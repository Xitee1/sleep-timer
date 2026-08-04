package dev.xitee.sleeptimer.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dagger.hilt.android.AndroidEntryPoint
import dev.xitee.sleeptimer.R
import dev.xitee.sleeptimer.core.data.model.TimerPhase
import dev.xitee.sleeptimer.core.data.repository.SettingsRepository
import dev.xitee.sleeptimer.core.data.repository.TimerRepository
import dev.xitee.sleeptimer.core.data.util.remainingMillisToDisplayMinutes
import dev.xitee.sleeptimer.core.service.SleepTimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quick Settings tile that starts the sleep timer with the preset duration —
 * `UserSettings.presetMinutes`, the value committed on the dial while idle —
 * without opening the app. While a timer is running the tile shows the remaining
 * minutes and a tap cancels it, mirroring the notification's Cancel action.
 */
@AndroidEntryPoint
class SleepTimerTileService : TileService() {

    @Inject lateinit var timerRepository: TimerRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listeningJob: Job? = null

    // Last preset delivered by the collector. onClick starts the service
    // synchronously from this cache: suspending on a DataStore read in the click
    // path would race both the tile's own destruction (SystemUI unbinds right
    // after the panel collapses, cancelling serviceScope) and the tap-granted
    // FGS start exemption. Main-thread only, like all TileService callbacks.
    private var cachedPresetMinutes: Int? = null

    // True from dispatching ACTION_START until the service publishes a non-IDLE
    // phase. A rapid second tap in that window would still read IDLE and either
    // double-start or instantly cancel the timer; swallow it instead.
    private var startInFlight = false

    override fun onStartListening() {
        super.onStartListening()
        startInFlight = false
        // The tile binds into the app process, so this observes the same in-process
        // StateFlow the foreground service writes on every tick.
        listeningJob?.cancel()
        listeningJob = serviceScope.launch {
            combine(timerRepository.timerState, settingsRepository.settings) { timerState, settings ->
                cachedPresetMinutes = settings.presetMinutes
                TileModel(
                    phase = timerState.phase,
                    minutes = when (timerState.phase) {
                        TimerPhase.IDLE -> settings.presetMinutes
                        else -> remainingMillisToDisplayMinutes(timerState.remainingMillis)
                    },
                )
            }
                // remainingMillis changes every second but the displayed minutes only
                // change once a minute — skip the redundant updateTile() calls.
                .distinctUntilChanged()
                .collect(::render)
        }
    }

    override fun onStopListening() {
        listeningJob?.cancel()
        listeningJob = null
        super.onStopListening()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        when (timerRepository.timerState.value.phase) {
            TimerPhase.IDLE -> startTimerWithPreset()
            // RUNNING / FADING_OUT: cancel restores volume and stops the service; if
            // the countdown ended in the meantime, the fresh instance's stale-intent
            // guard stops it again without touching timer state.
            else -> SleepTimerService.cancel(this)
        }
    }

    private fun startTimerWithPreset() {
        if (startInFlight) return
        // Not collected yet (a tap within the first frames of a cold panel open,
        // before the tile has rendered a subtitle) — drop the tap rather than
        // start a duration the user never saw.
        val minutes = cachedPresetMinutes ?: return
        // start() swallows the denied-start case (background-restricted app), so a
        // refused tap is a logged no-op instead of a crash.
        startInFlight = SleepTimerService.start(this, minutes * 60_000L)
    }

    private fun render(model: TileModel) {
        if (model.phase != TimerPhase.IDLE) startInFlight = false
        val tile = qsTile ?: return
        tile.state = if (model.phase == TimerPhase.IDLE) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        val statusText = when (model.phase) {
            TimerPhase.FADING_OUT -> getString(R.string.qs_tile_fading_out)
            // coerceAtLeast: the helper rounds up, but the service publishes one
            // final RUNNING tick with remainingMillis == 0 before the phase flips —
            // never show "0 min".
            else -> getString(R.string.widget_minutes, model.minutes.coerceAtLeast(1))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = statusText
        } else {
            // No subtitle before Q — fold the status into the label while a timer
            // is active so API 26-28 still see the remaining time, and restore the
            // plain name when idle (the label is also the tile's name in the QS
            // edit panel).
            tile.label = if (model.phase == TimerPhase.IDLE) {
                getString(R.string.app_name)
            } else {
                statusText
            }
        }
        tile.updateTile()
    }

    private data class TileModel(val phase: TimerPhase, val minutes: Int)
}
