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
 * Builds and pushes a single widget instance's [RemoteViews] for a given timer
 * phase. The tap target toggles with the phase: idle starts that instance's
 * configured duration (via the [SleepTimerWidgetProvider] trampoline),
 * running/fading cancels the service.
 */
internal object SleepTimerWidgetRenderer {

    // Distinct from TimerNotificationManager's request codes (1-3) so the widget's
    // cancel PendingIntent never aliases a notification action.
    private const val REQUEST_CODE_CANCEL = 101

    /** The currently placed instances of our widget. */
    fun widgetIds(context: Context, appWidgetManager: AppWidgetManager): IntArray =
        appWidgetManager.getAppWidgetIds(
            ComponentName(context, SleepTimerWidgetProvider::class.java),
        )

    /**
     * @param minutes the instance's configured start minutes when idle, the
     * remaining display minutes while running; ignored during fade-out.
     */
    fun render(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
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
                views.setOnClickPendingIntent(
                    R.id.widget_root,
                    startPendingIntent(context, appWidgetId),
                )
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
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun startPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, SleepTimerWidgetProvider::class.java)
            .setAction(SleepTimerWidgetProvider.ACTION_START_TIMER)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        // requestCode = appWidgetId: extras don't participate in PendingIntent
        // identity (Intent.filterEquals), so without a per-instance request code
        // every widget would share one PendingIntent and start the same duration.
        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun cancelPendingIntent(context: Context): PendingIntent {
        // A stale tap after the timer already ended is harmless: the service's
        // no-active-countdown guard stops itself before startForeground is due.
        return PendingIntent.getService(
            context,
            REQUEST_CODE_CANCEL,
            SleepTimerService.intent(context, SleepTimerService.ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
