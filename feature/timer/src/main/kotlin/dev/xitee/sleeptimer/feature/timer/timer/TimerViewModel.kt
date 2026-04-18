package dev.xitee.sleeptimer.feature.timer.timer

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
import dev.xitee.sleeptimer.core.service.SleepTimerService
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
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _selectedMinutes = MutableStateFlow(15)

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

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

    fun setMinutes(minutes: Int) {
        val coerced = minutes.coerceIn(1, 300)
        _selectedMinutes.value = coerced
        val phase = timerRepository.timerState.value.phase
        if (phase == TimerPhase.IDLE || phase == TimerPhase.FINISHED) {
            viewModelScope.launch {
                settingsRepository.updatePresetMinutes(coerced)
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
