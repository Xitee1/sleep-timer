package dev.xitee.sleeptimer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import dev.xitee.sleeptimer.R
import dev.xitee.sleeptimer.core.data.model.TimerPhase
import dev.xitee.sleeptimer.core.service.SleepTimerService

/**
 * Builds and pushes the widget's [RemoteViews] for a given timer phase. The single
 * tap target toggles with the phase: idle starts the preset timer (via the
 * [SleepTimerWidgetProvider] trampoline), running/fading cancels the service.
 */
internal object SleepTimerWidgetRenderer {

    // Distinct from TimerNotificationManager's request codes (1-3) so the widget's
    // cancel PendingIntent never aliases a notification action.
    private const val REQUEST_CODE_START = 100
    private const val REQUEST_CODE_CANCEL = 101

    /** Renders [phase] into every placed widget instance; no-op when none exist. */
    fun updateAllWidgets(context: Context, phase: TimerPhase, minutes: Int) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, SleepTimerWidgetProvider::class.java),
        )
        if (ids.isEmpty()) return
        render(context, manager, ids, phase, minutes)
    }

    /**
     * @param minutes the preset minutes when idle, the remaining display minutes
     * while running; ignored during fade-out.
     */
    fun render(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        phase: TimerPhase,
        minutes: Int,
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_sleep_timer)
        when (phase) {
            TimerPhase.IDLE -> {
                views.setViewVisibility(R.id.widget_minutes, View.VISIBLE)
                views.setTextViewText(
                    R.id.widget_minutes,
                    context.getString(R.string.widget_minutes, minutes),
                )
                views.setTextViewText(
                    R.id.widget_label,
                    context.getString(R.string.widget_tap_to_start),
                )
                views.setOnClickPendingIntent(R.id.widget_root, startPendingIntent(context))
            }
            TimerPhase.RUNNING -> {
                views.setViewVisibility(R.id.widget_minutes, View.VISIBLE)
                views.setTextViewText(
                    R.id.widget_minutes,
                    context.getString(R.string.widget_minutes, minutes),
                )
                views.setTextViewText(
                    R.id.widget_label,
                    context.getString(R.string.widget_tap_to_cancel),
                )
                views.setOnClickPendingIntent(R.id.widget_root, cancelPendingIntent(context))
            }
            TimerPhase.FADING_OUT -> {
                views.setViewVisibility(R.id.widget_minutes, View.GONE)
                views.setTextViewText(
                    R.id.widget_label,
                    context.getString(R.string.widget_fading_out),
                )
                views.setOnClickPendingIntent(R.id.widget_root, cancelPendingIntent(context))
            }
        }
        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    private fun startPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, SleepTimerWidgetProvider::class.java)
            .setAction(SleepTimerWidgetProvider.ACTION_START_TIMER)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_START,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun cancelPendingIntent(context: Context): PendingIntent {
        // A stale tap after the timer already ended is harmless: the service's
        // no-active-countdown guard stops itself before startForeground is due.
        val intent = Intent().apply {
            action = SleepTimerService.ACTION_CANCEL
            setClassName(context, SleepTimerService::class.java.name)
        }
        return PendingIntent.getService(
            context,
            REQUEST_CODE_CANCEL,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
