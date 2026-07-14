package dev.xitee.sleeptimer.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.xitee.sleeptimer.core.data.model.TimerPhase
import dev.xitee.sleeptimer.core.data.repository.SettingsRepository
import dev.xitee.sleeptimer.core.data.repository.TimerRepository
import dev.xitee.sleeptimer.core.data.util.remainingMillisToDisplayMinutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps placed home-screen widgets in sync with the live timer state. Started once
 * from [dev.xitee.sleeptimer.SleepTimerApp]; the collector lives as long as the
 * process. That is exactly the window in which the widget can go stale: while a
 * timer is active the foreground service keeps the process alive, and once the
 * process dies the last render is guaranteed to be the idle state (drawn when the
 * timer ended). Preset edits made in the app are also picked up here.
 */
@Singleton
class TimerWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timerRepository: TimerRepository,
    private val settingsRepository: SettingsRepository,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    private data class WidgetUi(val phase: TimerPhase, val minutes: Int)

    fun start() {
        if (started) return
        started = true
        scope.launch {
            combine(timerRepository.timerState, settingsRepository.settings) { state, settings ->
                WidgetUi(
                    phase = state.phase,
                    minutes = when (state.phase) {
                        TimerPhase.IDLE -> settings.presetMinutes
                        else -> remainingMillisToDisplayMinutes(state.remainingMillis)
                    },
                )
            }
                // The timer state ticks every second but the displayed minutes only
                // change once a minute — don't re-push identical RemoteViews.
                .distinctUntilChanged()
                .collect { ui ->
                    SleepTimerWidgetRenderer.updateAllWidgets(context, ui.phase, ui.minutes)
                }
        }
    }
}
