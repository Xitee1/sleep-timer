package dev.xitee.sleeptimer.tile

import android.content.Intent
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quick Settings tile that starts the sleep timer with the last used duration —
 * `UserSettings.presetMinutes`, the value persisted on every dial commit — without
 * opening the app. While a timer is running the tile shows the remaining minutes
 * and a tap cancels it, mirroring the notification's Cancel action.
 */
@AndroidEntryPoint
class SleepTimerTileService : TileService() {

    @Inject lateinit var timerRepository: TimerRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listeningJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        // The tile binds into the app process, so this observes the same in-process
        // StateFlow the foreground service writes on every tick.
        listeningJob?.cancel()
        listeningJob = serviceScope.launch {
            combine(timerRepository.timerState, settingsRepository.settings) { timerState, settings ->
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
            // the countdown ended in the meantime, the service's stale-intent guard
            // turns this into a no-op stopSelf.
            else -> startService(timerServiceIntent(SleepTimerService.ACTION_CANCEL))
        }
    }

    private fun startTimerWithPreset() {
        // The preset needs a DataStore read. The tap keeps the app in a state that
        // may start a foreground service, and the read is local and fast, so doing
        // it inside that window is fine.
        serviceScope.launch {
            val minutes = settingsRepository.settings.first().presetMinutes
            val intent = timerServiceIntent(SleepTimerService.ACTION_START).apply {
                putExtra(SleepTimerService.EXTRA_DURATION_MILLIS, minutes * 60_000L)
            }
            startForegroundService(intent)
        }
    }

    private fun render(model: TileModel) {
        val tile = qsTile ?: return
        tile.state = if (model.phase == TimerPhase.IDLE) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when (model.phase) {
                TimerPhase.FADING_OUT -> getString(R.string.qs_tile_fading_out)
                // coerceAtLeast: remaining can round down to 0 in the brief window
                // between expiry and teardown; never show "0 min".
                else -> getString(R.string.qs_tile_minutes, model.minutes.coerceAtLeast(1))
            }
        }
        tile.updateTile()
    }

    private fun timerServiceIntent(actionName: String): Intent =
        Intent().apply {
            action = actionName
            setClassName(this@SleepTimerTileService, SleepTimerService::class.java.name)
        }

    private data class TileModel(val phase: TimerPhase, val minutes: Int)
}
