package dev.xitee.sleeptimer.core.service.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.xitee.sleeptimer.core.data.model.TimerPhase
import dev.xitee.sleeptimer.core.service.R
import dev.xitee.sleeptimer.core.service.SleepTimerService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_ID = "sleep_timer_v2"
        private const val LEGACY_CHANNEL_ID = "sleep_timer"
        const val NOTIFICATION_ID = 1
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun buildNotification(
        remainingMinutes: Int,
        stepMinutes: Int,
        phase: TimerPhase = TimerPhase.RUNNING,
    ): Notification {
        val contentText = when {
            phase == TimerPhase.FADING_OUT -> context.getString(R.string.notification_fading_out)
            remainingMinutes > 0 -> context.resources.getQuantityString(
                R.plurals.notification_minutes_remaining,
                remainingMinutes,
                remainingMinutes,
            )
            else -> context.getString(R.string.notification_less_than_minute)
        }

        // Content intent to open the app
        val contentIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.let { intent ->
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }

        val subtractPendingIntent = servicePendingIntent(SleepTimerService.ACTION_SUBTRACT_MINUTES, requestCode = 1)
        val addPendingIntent = servicePendingIntent(SleepTimerService.ACTION_ADD_MINUTES, requestCode = 2)
        val cancelPendingIntent = servicePendingIntent(SleepTimerService.ACTION_CANCEL, requestCode = 3)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.notification_content_title))
            .setContentText(contentText)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(
                0,
                context.getString(R.string.notification_action_subtract_minutes, stepMinutes),
                subtractPendingIntent,
            )
            .addAction(
                0,
                context.getString(R.string.notification_action_add_minutes, stepMinutes),
                addPendingIntent,
            )
            .addAction(0, context.getString(R.string.notification_action_cancel), cancelPendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun servicePendingIntent(actionName: String, requestCode: Int): PendingIntent {
        val intent = Intent().apply {
            action = actionName
            setClassName(context, SleepTimerService::class.java.name)
        }
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun updateNotification(
        remainingMinutes: Int,
        stepMinutes: Int,
        phase: TimerPhase = TimerPhase.RUNNING,
    ) {
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(remainingMinutes, stepMinutes, phase),
        )
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
