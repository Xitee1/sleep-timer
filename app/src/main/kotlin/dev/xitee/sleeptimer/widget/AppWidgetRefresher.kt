package dev.xitee.sleeptimer.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.xitee.sleeptimer.core.service.TimerWidgetRefresher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [TimerWidgetRefresher] implementation: broadcasts an explicit APPWIDGET_UPDATE to our
 * own provider. [SleepTimerWidgetProvider.onUpdate] then re-reads the (now idle) timer
 * state off the main thread via `goAsync` and redraws each instance. Delivering it as a
 * broadcast — rather than rendering inline — keeps the process alive for the redraw even
 * while the service is stopping, and avoids a main-thread DataStore read.
 */
@Singleton
class AppWidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
) : TimerWidgetRefresher {

    override fun refresh() {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, SleepTimerWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return
        context.sendBroadcast(
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                setComponent(component)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            },
        )
    }
}
