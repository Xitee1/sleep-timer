package dev.xitee.sleeptimer.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.xitee.sleeptimer.core.data.model.TimerPhase
import dev.xitee.sleeptimer.core.data.model.WidgetConfig
import dev.xitee.sleeptimer.core.data.repository.SettingsRepository
import dev.xitee.sleeptimer.core.data.repository.TimerRepository
import dev.xitee.sleeptimer.core.data.repository.WidgetConfigRepository
import dev.xitee.sleeptimer.core.data.util.remainingMillisToDisplayMinutes
import dev.xitee.sleeptimer.core.service.SleepTimerService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Home-screen widget that starts the sleep timer with a single tap. Instances are
 * configured individually (see [SleepTimerWidgetConfigActivity]): each starts
 * either the last used time or its own fixed duration.
 *
 * The idle tap does not carry the duration in its PendingIntent — it broadcasts
 * [ACTION_START_TIMER] (with the tapped instance's appWidgetId) back to this
 * receiver, which resolves the duration from DataStore at tap time. Baking the
 * duration into the widget's PendingIntent would go stale whenever the settings
 * change without the widget being re-rendered.
 *
 * Starting the foreground service from here is allowed even though the app is in
 * the background: a widget tap is a documented exemption from the FGS
 * background-start restrictions (the same mechanism notification actions rely on).
 */
@AndroidEntryPoint
class SleepTimerWidgetProvider : AppWidgetProvider() {

    @Inject lateinit var timerRepository: TimerRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var widgetConfigRepository: WidgetConfigRepository

    override fun onReceive(context: Context, intent: Intent) {
        // Hilt injects the fields inside super.onReceive, which also dispatches
        // APPWIDGET_UPDATE to onUpdate — so injection must happen first.
        super.onReceive(context, intent)
        if (intent.action == ACTION_START_TIMER) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
            startConfiguredTimer(context, appWidgetId)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // After process death timerState is IDLE by construction (in-process flow,
        // and the foreground service keeps the process alive while a timer runs).
        val state = timerRepository.timerState.value
        if (state.phase == TimerPhase.IDLE) {
            val (presetMinutes, configs) = runBlocking {
                settingsRepository.settings.first().presetMinutes to
                    widgetConfigRepository.configs.first()
            }
            appWidgetIds.forEach { id ->
                val minutes = (configs[id] ?: WidgetConfig()).startMinutes(presetMinutes)
                SleepTimerWidgetRenderer.render(context, appWidgetManager, id, state.phase, minutes)
            }
        } else {
            val minutes = remainingMillisToDisplayMinutes(state.remainingMillis)
            appWidgetIds.forEach { id ->
                SleepTimerWidgetRenderer.render(context, appWidgetManager, id, state.phase, minutes)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // Removed instances must not leak their config entries in DataStore.
        runBlocking { widgetConfigRepository.removeConfigs(appWidgetIds.toList()) }
    }

    private fun startConfiguredTimer(context: Context, appWidgetId: Int) {
        // The rendered tap target can be momentarily stale (timer started from the
        // app right before the tap landed) — never restart a timer that is active.
        if (timerRepository.timerState.value.phase != TimerPhase.IDLE) return
        val startMinutes = runBlocking {
            val presetMinutes = settingsRepository.settings.first().presetMinutes
            widgetConfigRepository.getConfig(appWidgetId).startMinutes(presetMinutes)
        }
        val serviceIntent = Intent().apply {
            action = SleepTimerService.ACTION_START
            setClassName(context, SleepTimerService::class.java.name)
            putExtra(SleepTimerService.EXTRA_DURATION_MILLIS, startMinutes * 60_000L)
        }
        context.startForegroundService(serviceIntent)
    }

    companion object {
        const val ACTION_START_TIMER = "dev.xitee.sleeptimer.action.WIDGET_START_TIMER"
    }
}
