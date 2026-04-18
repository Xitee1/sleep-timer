package dev.mato.sleeptimer.core.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.mato.sleeptimer.core.data.model.TimerPhase
import dev.mato.sleeptimer.core.data.model.TimerState
import dev.mato.sleeptimer.core.data.model.UserSettings
import dev.mato.sleeptimer.core.data.repository.SettingsRepository
import dev.mato.sleeptimer.core.data.repository.TimerRepository
import dev.mato.sleeptimer.core.data.util.remainingMillisToDisplayMinutes
import dev.mato.sleeptimer.core.service.media.MediaVolumeController
import dev.mato.sleeptimer.core.service.notification.TimerNotificationManager
import dev.mato.sleeptimer.core.service.screen.ScreenLockHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SleepTimerService : Service() {

    @Inject lateinit var timerRepository: TimerRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var notificationManager: TimerNotificationManager
    @Inject lateinit var mediaVolumeController: MediaVolumeController
    @Inject lateinit var screenLockHelper: ScreenLockHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var countdownJob: Job? = null
    private var remainingMillis: Long = 0L
    private var totalDurationMillis: Long = 0L
    private var stepMinutes: Int = 5

    companion object {
        const val ACTION_START = "dev.mato.sleeptimer.action.START"
        const val ACTION_CANCEL = "dev.mato.sleeptimer.action.CANCEL"
        const val ACTION_ADD_MINUTES = "dev.mato.sleeptimer.action.ADD_MINUTES"
        const val ACTION_SUBTRACT_MINUTES = "dev.mato.sleeptimer.action.SUBTRACT_MINUTES"
        const val EXTRA_DURATION_MILLIS = "dev.mato.sleeptimer.extra.DURATION_MILLIS"
    }

    override fun onCreate() {
        super.onCreate()
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

        // Start countdown
        countdownJob = serviceScope.launch {
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

            // Timer expired — handle completion
            onTimerExpired()
        }
    }

    private fun addStep() {
        if (countdownJob?.isActive == true) {
            val stepMillis = stepMinutes * 60 * 1000L
            remainingMillis += stepMillis
            totalDurationMillis += stepMillis
            updateTimerState(TimerPhase.RUNNING)
            notificationManager.updateNotification(
                remainingMillisToDisplayMinutes(remainingMillis),
                stepMinutes,
            )
        }
    }

    private fun subtractStep() {
        if (countdownJob?.isActive == true) {
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
    }

    private fun cancelTimer() {
        countdownJob?.cancel()
        countdownJob = null

        // Restore volume if fading was in progress
        mediaVolumeController.restoreVolume()

        updateTimerState(TimerPhase.IDLE)
        notificationManager.cancelNotification()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onTimerExpired() {
        serviceScope.launch {
            val settings: UserSettings = settingsRepository.settings.first()

            if (settings.stopMediaPlayback) {
                updateTimerState(TimerPhase.FADING_OUT)
                notificationManager.updateNotification(0, stepMinutes)

                mediaVolumeController.fadeOutAndPause(settings.fadeOutDurationSeconds)
            }

            if (settings.screenOff) {
                screenLockHelper.lockScreen()
            }

            // Reset state before stopping foreground to avoid race with onDestroy
            timerRepository.updateState(TimerState())
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
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
