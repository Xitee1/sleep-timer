package dev.xitee.sleeptimer.feature.timer.timer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.xitee.sleeptimer.core.data.model.TimerPhase
import dev.xitee.sleeptimer.core.data.model.UserSettings
import dev.xitee.sleeptimer.core.data.repository.SettingsRepository
import dev.xitee.sleeptimer.core.data.repository.TimerRepository
import dev.xitee.sleeptimer.core.data.util.remainingMillisToDisplayMinutes
import dev.xitee.sleeptimer.core.service.SleepTimerService
import dev.xitee.sleeptimer.core.service.screen.ScreenLockHelper
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val timerRepository: TimerRepository,
    private val settingsRepository: SettingsRepository,
    private val shizukuManager: ShizukuManager,
    private val screenLockHelper: ScreenLockHelper,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _selectedMinutes = MutableStateFlow(15)

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    val shizukuState: StateFlow<ShizukuManager.State> = shizukuManager.state

    // Guards the startup permission dialog so it fires once per process lifetime.
    // Survives navigation (ViewModel is scoped to the Timer nav entry).
    private var startupCheckDone = false

    init {
        viewModelScope.launch {
            _selectedMinutes.value = settingsRepository.settings.first().presetMinutes
        }
    }

    val uiState: StateFlow<TimerUiState> = combine(
        timerRepository.timerState,
        _selectedMinutes,
    ) { timerState, selectedMin ->
        when (timerState.phase) {
            TimerPhase.IDLE, TimerPhase.FINISHED -> {
                TimerUiState.Idle(selectedMinutes = selectedMin)
            }
            TimerPhase.RUNNING -> {
                val totalSeconds = (timerState.totalDurationMillis / 1000).toInt()
                val progress = if (timerState.totalDurationMillis > 0L) {
                    timerState.remainingMillis.toFloat() / timerState.totalDurationMillis.toFloat()
                } else {
                    0f
                }
                TimerUiState.Running(
                    totalMinutes = (totalSeconds / 60),
                    remainingMillis = timerState.remainingMillis,
                    progress = progress,
                )
            }
            TimerPhase.FADING_OUT -> TimerUiState.FadingOut
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerUiState.Idle())

    fun getAdminComponent(): ComponentName = screenLockHelper.adminComponent

    fun refreshShizuku() = shizukuManager.refresh()
    fun requestShizukuPermission() = shizukuManager.requestPermission()

    suspend fun computeStartupPermissionCheck(): StartupPermissionCheck? {
        if (startupCheckDone) return null
        startupCheckDone = true
        shizukuManager.refresh()
        val s = settingsRepository.settings.first()
        val shizukuReady = shizukuManager.isReady()
        val adminActive = screenLockHelper.isAdminActive()
        val adminMissing = s.screenOff && !s.softScreenOff && !adminActive
        val shizukuFeatures = buildList {
            if (s.screenOff && s.softScreenOff && !shizukuReady) add(ShizukuFeature.SCREEN_OFF)
            if (s.turnOffWifi && !shizukuReady) add(ShizukuFeature.WIFI)
            if (s.turnOffBluetooth && !shizukuReady) add(ShizukuFeature.BLUETOOTH)
        }
        return StartupPermissionCheck(adminMissing, shizukuFeatures)
    }

    /**
     * Updates the live UI minutes without persisting. Called continuously during a
     * dial drag — persisting every tick would flood DataStore with ~30 writes per drag.
     * Ignored while a timer is running: during-drag previews are driven by dialState
     * in the UI, and updating `_selectedMinutes` would silently shift the idle preset
     * once the running session ends.
     */
    fun setMinutes(minutes: Int) {
        val phase = timerRepository.timerState.value.phase
        if (phase != TimerPhase.IDLE && phase != TimerPhase.FINISHED) return
        _selectedMinutes.value = minutes.coerceIn(1, 300)
    }

    /**
     * Applies a committed minutes value. When idle, persists it as the preset. When a
     * timer is running, dispatches to the service to adjust remaining time.
     */
    fun commitMinutes(minutes: Int) {
        val coerced = minutes.coerceIn(1, 300)
        val state = timerRepository.timerState.value
        when (state.phase) {
            TimerPhase.RUNNING -> {
                if (remainingMillisToDisplayMinutes(state.remainingMillis) == coerced) return
                val intent = serviceIntent(SleepTimerService.ACTION_SET_MINUTES).apply {
                    putExtra(SleepTimerService.EXTRA_MINUTES, coerced)
                }
                context.startService(intent)
            }
            else -> {
                _selectedMinutes.value = coerced
                viewModelScope.launch {
                    settingsRepository.updatePresetMinutes(coerced)
                }
            }
        }
    }

    fun startTimer() {
        val minutes = _selectedMinutes.value
        if (minutes <= 0) return
        val durationMillis = minutes * 60 * 1000L
        val intent = serviceIntent(SleepTimerService.ACTION_START).apply {
            putExtra(SleepTimerService.EXTRA_DURATION_MILLIS, durationMillis)
        }
        context.startForegroundService(intent)
    }

    fun stopTimer() {
        context.startService(serviceIntent(SleepTimerService.ACTION_CANCEL))
    }

    fun addStep() {
        context.startService(serviceIntent(SleepTimerService.ACTION_ADD_MINUTES))
    }

    fun subtractStep() {
        context.startService(serviceIntent(SleepTimerService.ACTION_SUBTRACT_MINUTES))
    }

    private fun serviceIntent(actionName: String): Intent =
        Intent().apply {
            action = actionName
            setClassName(context, SleepTimerService::class.java.name)
        }
}

data class StartupPermissionCheck(
    val adminMissing: Boolean,
    val shizukuMissingFeatures: List<ShizukuFeature>,
)

enum class ShizukuFeature { SCREEN_OFF, WIFI, BLUETOOTH }
