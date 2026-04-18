package dev.xitee.sleeptimer.core.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.xitee.sleeptimer.core.data.model.TimerPhase
import dev.xitee.sleeptimer.core.data.model.TimerState
import dev.xitee.sleeptimer.core.data.model.UserSettings
import dev.xitee.sleeptimer.core.data.repository.SettingsRepository
import dev.xitee.sleeptimer.core.data.repository.TimerRepository
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

@AndroidEntryPoint
class SleepTimerService : Service() {

    @Inject lateinit var timerRepository: TimerRepository
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
    private var remainingMillis: Long = 0L
    private var totalDurationMillis: Long = 0L
    private var stepMinutes: Int = 0

    companion object {
        const val ACTION_START = "dev.xitee.sleeptimer.action.START"
        const val ACTION_CANCEL = "dev.xitee.sleeptimer.action.CANCEL"
        const val ACTION_ADD_MINUTES = "dev.xitee.sleeptimer.action.ADD_MINUTES"
        const val ACTION_SUBTRACT_MINUTES = "dev.xitee.sleeptimer.action.SUBTRACT_MINUTES"
        const val EXTRA_DURATION_MILLIS = "dev.xitee.sleeptimer.extra.DURATION_MILLIS"
        private const val FADE_IN_SECONDS = 2
    }

    override fun onCreate() {
        super.onCreate()
        // Prime synchronously so the first notification uses the saved step,
        // not a default from before the settings flow has emitted.
        stepMinutes = runBlocking { settingsRepository.settings.first().stepMinutes }
        serviceScope.launch {
            settingsRepository.settings
                .map { it.stepMinutes }
                .collect { stepMinutes = it }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val durationMillis = intent.getLongExtra(EXTRA_DURATION_MILLIS, 0L)
                if (durationMillis > 0) {
                    startTimer(durationMillis)
                }
            }
            ACTION_ADD_MINUTES -> {
                addStep()
            }
            ACTION_SUBTRACT_MINUTES -> {
                subtractStep()
            }
            ACTION_CANCEL -> {
                cancelTimer()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer(durationMillis: Long) {
        // Cancel any existing countdown
        countdownJob?.cancel()

        totalDurationMillis = durationMillis
        remainingMillis = durationMillis

        // Create notification channel and start foreground
        notificationManager.createNotificationChannel()
        val notification = notificationManager.buildNotification(
            remainingMinutes = remainingMillisToDisplayMinutes(remainingMillis),
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
            runCountdownAndExpire()
        }
    }

    private suspend fun runCountdownAndExpire() {
        while (remainingMillis > 0) {
            delay(1000L)
            remainingMillis -= 1000L

            if (remainingMillis <= 0) {
                remainingMillis = 0
            }

            updateTimerState(TimerPhase.RUNNING)

            val remainingMinutes = remainingMillisToDisplayMinutes(remainingMillis)
            notificationManager.updateNotification(remainingMinutes, stepMinutes)
        }

        onTimerExpired()
    }

    private fun addStep() {
        when (timerRepository.timerState.value.phase) {
            TimerPhase.RUNNING -> {
                if (countdownJob?.isActive != true) return
                val stepMillis = stepMinutes * 60 * 1000L
                remainingMillis += stepMillis
                totalDurationMillis += stepMillis
                updateTimerState(TimerPhase.RUNNING)
                notificationManager.updateNotification(
                    remainingMillisToDisplayMinutes(remainingMillis),
                    stepMinutes,
                )
            }
            TimerPhase.FADING_OUT -> {
                // Replace countdownJob with the fade-in + restart so Cancel during
                // the 2 s fade-in window cancels this whole sequence, not just the
                // already-finished fade-out. The countdown and fade-in run in
                // parallel — the clock ticks from the moment the user presses +,
                // not after the fade-in completes.
                val oldJob = countdownJob ?: return
                val stepMillis = stepMinutes * 60 * 1000L
                countdownJob = serviceScope.launch {
                    oldJob.cancelAndJoin()
                    totalDurationMillis = stepMillis
                    remainingMillis = stepMillis
                    updateTimerState(TimerPhase.RUNNING)
                    notificationManager.updateNotification(
                        remainingMillisToDisplayMinutes(remainingMillis),
                        stepMinutes,
                    )
                    launch { mediaVolumeController.fadeInToOriginal(FADE_IN_SECONDS) }
                    runCountdownAndExpire()
                }
            }
            else -> {}
        }
    }

    private fun subtractStep() {
        if (timerRepository.timerState.value.phase != TimerPhase.RUNNING) return
        if (countdownJob?.isActive != true) return
        val stepMillis = stepMinutes * 60 * 1000L
        if (remainingMillis <= stepMillis) return
        remainingMillis -= stepMillis
        totalDurationMillis = (totalDurationMillis - stepMillis).coerceAtLeast(remainingMillis)
        updateTimerState(TimerPhase.RUNNING)
        notificationManager.updateNotification(
            remainingMillisToDisplayMinutes(remainingMillis),
            stepMinutes,
        )
    }

    private fun cancelTimer() {
        val job = countdownJob
        countdownJob = null
        serviceScope.launch {
            // Join the fade-out before restoring volume, otherwise the still-running
            // fade coroutine would overwrite the restored volume at its next step.
            job?.cancelAndJoin()
            mediaVolumeController.restoreVolume()
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
            val usedShizuku = if (settings.softScreenOff && shizukuManager.isReady()) {
                shizukuScreenOffHelper.turnOffScreen()
            } else {
                false
            }
            if (!usedShizuku) {
                // Hard-lock fallback: also the path when softScreenOff is off, or
                // when the Shizuku attempt failed. Forces credential on next unlock.
                screenLockHelper.lockScreen()
            }
        }

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
