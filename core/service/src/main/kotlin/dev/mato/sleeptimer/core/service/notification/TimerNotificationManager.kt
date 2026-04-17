package dev.mato.sleeptimer.core.service.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mato.sleeptimer.core.service.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_ID = "sleep_timer"
        const val NOTIFICATION_ID = 1
        private const val ACTION_ADD_FIVE = "dev.mato.sleeptimer.action.ADD_FIVE"
        private const val ACTION_CANCEL = "dev.mato.sleeptimer.action.CANCEL"
        private const val SERVICE_CLASS = "dev.mato.sleeptimer.core.service.SleepTimerService"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun buildNotification(remainingMinutes: Int): Notification {
        val contentText = if (remainingMinutes > 0) {
            context.getString(R.string.notification_minutes_remaining, remainingMinutes)
        } else {
            context.getString(R.string.notification_less_than_minute)
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

        // +5 min action
        val addFiveIntent = Intent().apply {
            action = ACTION_ADD_FIVE
            setClassName(context, SERVICE_CLASS)
        }
        val addFivePendingIntent = PendingIntent.getService(
            context,
            1,
            addFiveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Cancel action
        val cancelIntent = Intent().apply {
            action = ACTION_CANCEL
            setClassName(context, SERVICE_CLASS)
        }
        val cancelPendingIntent = PendingIntent.getService(
            context,
            2,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.notification_content_title))
            .setContentText(contentText)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, context.getString(R.string.notification_action_add_five), addFivePendingIntent)
            .addAction(0, context.getString(R.string.notification_action_cancel), cancelPendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun updateNotification(remainingMinutes: Int) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(remainingMinutes))
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
