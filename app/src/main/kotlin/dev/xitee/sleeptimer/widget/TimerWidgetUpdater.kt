package dev.xitee.sleeptimer.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.xitee.sleeptimer.core.data.model.TimerPhase
import dev.xitee.sleeptimer.core.data.model.WidgetConfig
import dev.xitee.sleeptimer.core.data.model.startMinutesFor
import dev.xitee.sleeptimer.core.data.repository.SettingsRepository
import dev.xitee.sleeptimer.core.data.repository.TimerRepository
import dev.xitee.sleeptimer.core.data.repository.WidgetConfigRepository
import dev.xitee.sleeptimer.core.data.util.remainingMillisToDisplayMinutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps placed home-screen widgets in sync with the live timer state. Started once
 * from [dev.xitee.sleeptimer.SleepTimerApp]; the collector lives as long as the
 * process. That is exactly the window in which the widget can go stale: while a
 * timer is active the foreground service keeps the process alive. When the timer
 * ends the service also fires a terminal refresh (see
 * [dev.xitee.sleeptimer.core.service.TimerWidgetRefresher]) so the last shown state
 * is idle even if this collector's process is reclaimed before it renders. Edits to
 * the preset or to per-widget configs made in the app are also picked up here.
 */
@Singleton
class TimerWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timerRepository: TimerRepository,
    private val settingsRepository: SettingsRepository,
    private val widgetConfigRepository: WidgetConfigRepository,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    private data class TimerSignal(
        val phase: TimerPhase,
        val remainingDisplayMinutes: Int,
    )

    private data class RenderPlan(
        val phase: TimerPhase,
        val remainingDisplayMinutes: Int,
        val presetMinutes: Int,
        val configs: Map<Int, WidgetConfig>,
    )

    fun start() {
        if (started) return
        started = true
        scope.launch {
            // The timer state ticks every second, but the widget only ever shows the
            // phase and the display minutes — collapse to those up front so the combine
            // and RenderPlan below run once a minute, not once a second (and not at all
            // per-second for a user with no widget placed).
            val timerSignal = timerRepository.timerState
                .map { TimerSignal(it.phase, remainingMillisToDisplayMinutes(it.remainingMillis)) }
                .distinctUntilChanged()

            combine(
                timerSignal,
                settingsRepository.settings,
                widgetConfigRepository.configs,
            ) { signal, settings, configs ->
                RenderPlan(
                    phase = signal.phase,
                    remainingDisplayMinutes = signal.remainingDisplayMinutes,
                    presetMinutes = settings.presetMinutes,
                    configs = configs,
                )
            }
                .distinctUntilChanged()
                .collect { plan ->
                    val manager = AppWidgetManager.getInstance(context)
                    val ids = SleepTimerWidgetRenderer.widgetIds(context, manager)
                    if (ids.isEmpty()) return@collect
                    ids.forEach { id ->
                        val minutes = if (plan.phase == TimerPhase.IDLE) {
                            plan.configs.startMinutesFor(id, plan.presetMinutes)
                        } else {
                            plan.remainingDisplayMinutes
                        }
                        SleepTimerWidgetRenderer.render(context, manager, id, plan.phase, minutes)
                    }
                }
        }
    }
}
