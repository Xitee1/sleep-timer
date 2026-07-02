package dev.xitee.sleeptimer.core.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.xitee.sleeptimer.core.data.model.MAX_TIMER_MINUTES
import dev.xitee.sleeptimer.core.data.model.TimerPhase
import dev.xitee.sleeptimer.core.data.model.TimerState
import dev.xitee.sleeptimer.core.data.model.UserSettings
import dev.xitee.sleeptimer.core.data.repository.SettingsRepository
import dev.xitee.sleeptimer.core.data.repository.TimerRepositoryImpl
import dev.xitee.sleeptimer.core.data.util.remainingMillisToDisplayMinutes
import dev.xitee.sleeptimer.core.service.media.MediaVolumeController
import dev.xitee.sleeptimer.core.service.notification.TimerNotificationManager
import dev.xitee.sleeptimer.core.service.screen.ScreenLockHelper
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuBluetoothController
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuManager
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuScreenOffHelper
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuWifiController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

@AndroidEntryPoint
class SleepTimerService : Service() {

    @Inject lateinit var timerRepository: TimerRepositoryImpl
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var notificationManager: TimerNotificationManager
    @Inject lateinit var mediaVolumeController: MediaVolumeController
    @Inject lateinit var screenLockHelper: ScreenLockHelper
    @Inject lateinit var shizukuManager: ShizukuManager
    @Inject lateinit var shizukuScreenOffHelper: ShizukuScreenOffHelper
    @Inject lateinit var shizukuWifiController: ShizukuWifiController
    @Inject lateinit var shizukuBluetoothController: ShizukuBluetoothController

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var countdownJob: Job? = null

    // Deadline on the elapsedRealtime clock. delay() ticks are uptime-based and pause
    // while the CPU deep-sleeps, so the remaining time must be re-derived from a clock
    // that keeps counting through sleep — otherwise the countdown silently extends.
    private var deadlineElapsed: Long = 0L
    private var remainingMillis: Long = 0L
    private var totalDurationMillis: Long = 0L
    private var stepMinutes: Int = 0

    // Bumped by startTimer so an in-flight cancel teardown can tell whether a newer
    // timer took over while it was joining the old countdown job (see cancelTimer).
    private var timerGeneration: Int = 0

    // Last minutes value shown in the notification; notify() is skipped while the
    // displayed text would not change. Int.MIN_VALUE forces the next update through.
    private var lastNotifiedMinutes: Int = Int.MIN_VALUE

    private val powerManager by lazy { getSystemService(POWER_SERVICE) as PowerManager }

    companion object {
        const val ACTION_START = "dev.xitee.sleeptimer.action.START"
        const val ACTION_CANCEL = "dev.xitee.sleeptimer.action.CANCEL"
        const val ACTION_ADD_MINUTES = "dev.xitee.sleeptimer.action.ADD_MINUTES"
        const val ACTION_SUBTRACT_MINUTES = "dev.xitee.sleeptimer.action.SUBTRACT_MINUTES"
        const val ACTION_SET_MINUTES = "dev.xitee.sleeptimer.action.SET_MINUTES"
        const val EXTRA_DURATION_MILLIS = "dev.xitee.sleeptimer.extra.DURATION_MILLIS"
        const val EXTRA_MINUTES = "dev.xitee.sleeptimer.extra.MINUTES"
        private const val FADE_IN_SECONDS = 2
        private const val MAX_TIMER_MILLIS = MAX_TIMER_MINUTES * 60_000L
    }

    override fun onCreate() {
        super.onCreate()
        // Prime synchronously so the first notification uses the saved step,
        // not a default from before the settings flow has emitted.
        stepMinutes = runBlocking { settingsRepository.settings.first().stepMinutes }
        serviceScope.launch {
            settingsRepository.settings
                .map { it.stepMinutes }
                .collect { newStep ->
                    val changed = newStep != stepMinutes
                    stepMinutes = newStep
                    // Keep the +/− action labels current if the step is edited mid-timer.
                    if (changed && countdownJob?.isActive == true && lastNotifiedMinutes >= 0) {
                        notificationManager.updateNotification(lastNotifiedMinutes, stepMinutes)
                    }
                }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        // If the service was restarted by the OS (or by a stale PendingIntent from a
        // notification that survived process death) for any action other than START,
        // there is no countdown to modify. Skip straight to stopSelf — otherwise we
        // would never call startForeground within the 5-second window and crash with
        // ForegroundServiceDidNotStartInTimeException.
        if (action != ACTION_START && countdownJob?.isActive != true) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        when (action) {
            ACTION_START -> {
                val durationMillis = intent.getLongExtra(EXTRA_DURATION_MILLIS, 0L)
                if (durationMillis > 0) {
                    startTimer(durationMillis.coerceAtMost(MAX_TIMER_MILLIS))
                } else if (countdownJob?.isActive != true) {
                    // Invalid start with nothing running — same 5-second-window
                    // consideration as the stale-intent guard above.
                    stopSelf(startId)
                }
            }
            ACTION_ADD_MINUTES -> {
                addStep()
            }
            ACTION_SUBTRACT_MINUTES -> {
                subtractStep()
            }
            ACTION_SET_MINUTES -> {
                val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)
                if (minutes > 0) setRemainingMinutes(minutes)
            }
            ACTION_CANCEL -> {
                cancelTimer()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer(durationMillis: Long) {
        timerGeneration++
        val previousJob = countdownJob

        totalDurationMillis = durationMillis
        setRemaining(durationMillis)

        // Create notification channel and start foreground
        notificationManager.createNotificationChannel()
        lastNotifiedMinutes = remainingMillisToDisplayMinutes(remainingMillis)
        val notification = notificationManager.buildNotification(
            remainingMinutes = lastNotifiedMinutes,
            stepMinutes = stepMinutes,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                TimerNotificationManager.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(TimerNotificationManager.NOTIFICATION_ID, notification)
        }

        // Update repository
        updateTimerState(TimerPhase.RUNNING)

        // Start countdown. onTimerExpired runs inside this job so that cancelling
        // the job also cancels the fade-out — lets + and Cancel interrupt the fade.
        countdownJob = serviceScope.launch {
            // If the previous countdown was mid-fade, stop it and undo its volume
            // ramp before this timer takes over — otherwise media would stay quiet
            // and the faded level would be captured as the new "original" volume.
            previousJob?.cancelAndJoin()
            mediaVolumeController.restoreVolume()
            runCountdownAndExpire()
        }
    }

    /** Anchors [remainingMillis] to a fresh elapsedRealtime deadline. */
    private fun setRemaining(millis: Long) {
        remainingMillis = millis.coerceAtLeast(0L)
        deadlineElapsed = SystemClock.elapsedRealtime() + remainingMillis
    }

    private suspend fun runCountdownAndExpire() {
        while (true) {
            remainingMillis = (deadlineElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            updateTimerState(TimerPhase.RUNNING)
            notifyRemainingIfChanged()
            if (remainingMillis <= 0L) break
            delay(minOf(1_000L, remainingMillis))
        }
        onTimerExpired()
    }

    private fun notifyRemainingIfChanged() {
        val minutes = remainingMillisToDisplayMinutes(remainingMillis)
        if (minutes == lastNotifiedMinutes) return
        lastNotifiedMinutes = minutes
        notificationManager.updateNotification(minutes, stepMinutes)
    }

    private fun addStep() {
        when (timerRepository.timerState.value.phase) {
            TimerPhase.RUNNING -> {
                if (countdownJob?.isActive != true) return
                val stepMillis = stepMinutes * 60_000L
                setRemaining((remainingMillis + stepMillis).coerceAtMost(MAX_TIMER_MILLIS))
                totalDurationMillis = (totalDurationMillis + stepMillis).coerceAtMost(MAX_TIMER_MILLIS)
                updateTimerState(TimerPhase.RUNNING)
                notifyRemainingIfChanged()
            }
            TimerPhase.FADING_OUT -> {
                // Replace countdownJob with the fade-in + restart so Cancel during
                // the 2 s fade-in window cancels this whole sequence, not just the
                // already-finished fade-out. The countdown and fade-in run in
                // parallel — the clock ticks from the moment the user presses +,
                // not after the fade-in completes.
                val oldJob = countdownJob ?: return
                val stepMillis = (stepMinutes * 60_000L).coerceAtMost(MAX_TIMER_MILLIS)
                countdownJob = serviceScope.launch {
                    oldJob.cancelAndJoin()
                    totalDurationMillis = stepMillis
                    setRemaining(stepMillis)
                    updateTimerState(TimerPhase.RUNNING)
                    notifyRemainingIfChanged()
                    launch { mediaVolumeController.fadeInToOriginal(FADE_IN_SECONDS) }
                    runCountdownAndExpire()
                }
            }
            else -> {}
        }
    }

    private fun setRemainingMinutes(minutes: Int) {
        if (timerRepository.timerState.value.phase != TimerPhase.RUNNING) return
        if (countdownJob?.isActive != true) return
        setRemaining(minutes.coerceIn(1, MAX_TIMER_MINUTES) * 60_000L)
        totalDurationMillis = maxOf(totalDurationMillis, remainingMillis)
        updateTimerState(TimerPhase.RUNNING)
        notifyRemainingIfChanged()
    }

    private fun subtractStep() {
        if (timerRepository.timerState.value.phase != TimerPhase.RUNNING) return
        if (countdownJob?.isActive != true) return
        val stepMillis = stepMinutes * 60_000L
        if (remainingMillis <= stepMillis) return
        setRemaining(remainingMillis - stepMillis)
        totalDurationMillis = (totalDurationMillis - stepMillis).coerceAtLeast(remainingMillis)
        updateTimerState(TimerPhase.RUNNING)
        notifyRemainingIfChanged()
    }

    private fun cancelTimer() {
        val job = countdownJob
        countdownJob = null
        val generation = timerGeneration
        serviceScope.launch {
            // Join the fade-out before restoring volume, otherwise the still-running
            // fade coroutine would overwrite the restored volume at its next step.
            job?.cancelAndJoin()
            mediaVolumeController.restoreVolume()
            // A newer timer may have started while the join was in flight — its
            // foreground session must survive this teardown.
            if (timerGeneration != generation) return@launch
            updateTimerState(TimerPhase.IDLE)
            notificationManager.cancelNotification()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun onTimerExpired() {
        val settings: UserSettings = settingsRepository.settings.first()

        if (settings.stopMediaPlayback) {
            updateTimerState(TimerPhase.FADING_OUT)
            lastNotifiedMinutes = Int.MIN_VALUE
            notificationManager.updateNotification(0, stepMinutes, TimerPhase.FADING_OUT)
            mediaVolumeController.fadeOutAndPause(settings.fadeOutDurationSeconds)
        }

        if (settings.turnOffWifi && shizukuManager.isReady()) {
            shizukuWifiController.disableWifi()
        }

        if (settings.turnOffBluetooth && shizukuManager.isReady()) {
            shizukuBluetoothController.disableBluetooth()
        }

        if (settings.screenOff) {
            if (settings.softScreenOff && shizukuManager.isReady()) {
                // KEYCODE_POWER is a toggle: only send it while the screen is on,
                // otherwise "turn off the screen" would wake a sleeping device. If
                // the screen is already off there is nothing to do — falling back
                // to the hard lock here would defeat the user's choice to keep
                // biometric unlock working.
                if (powerManager.isInteractive && !shizukuScreenOffHelper.turnOffScreen()) {
                    screenLockHelper.lockScreen()
                }
            } else {
                // Hard-lock: also the path when softScreenOff is on but Shizuku is
                // unavailable. Forces credential on next unlock.
                screenLockHelper.lockScreen()
            }
        }

        // "+" during the fade replaces countdownJob with a successor that restarts
        // the countdown; if that happened, this coroutine must not tear down the
        // foreground session the successor is still using.
        if (countdownJob !== coroutineContext[Job]) return
        countdownJob = null

        // Reset state before stopping foreground to avoid race with onDestroy
        timerRepository.updateState(TimerState())
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateTimerState(phase: TimerPhase) {
        timerRepository.updateState(
            TimerState(
                phase = phase,
                totalDurationMillis = totalDurationMillis,
                remainingMillis = remainingMillis,
            ),
        )
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
