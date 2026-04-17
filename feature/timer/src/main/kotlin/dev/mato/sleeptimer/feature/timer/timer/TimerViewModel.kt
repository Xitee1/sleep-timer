package dev.mato.sleeptimer.feature.timer.timer

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mato.sleeptimer.core.data.model.TimerPhase
import dev.mato.sleeptimer.core.data.model.UserSettings
import dev.mato.sleeptimer.core.data.repository.SettingsRepository
import dev.mato.sleeptimer.core.data.repository.TimerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val timerRepository: TimerRepository,
    settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _selectedMinutes = MutableStateFlow(15)

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    val uiState: StateFlow<TimerUiState> = combine(
        timerRepository.timerState,
        _selectedMinutes,
    ) { timerState, selectedMin ->
        when (timerState.phase) {
            TimerPhase.IDLE, TimerPhase.FINISHED -> {
                TimerUiState.Idle(selectedMinutes = selectedMin)
            }
            TimerPhase.RUNNING -> {
                val remainingSeconds = (timerState.remainingMillis / 1000).toInt()
                val totalSeconds = (timerState.totalDurationMillis / 1000).toInt()
                val progress = if (totalSeconds > 0) {
                    remainingSeconds.toFloat() / totalSeconds.toFloat()
                } else {
                    0f
                }
                TimerUiState.Running(
                    totalMinutes = (totalSeconds / 60),
                    remainingMinutes = remainingSeconds / 60,
                    remainingSeconds = remainingSeconds % 60,
                    progress = progress,
                )
            }
            TimerPhase.FADING_OUT -> TimerUiState.FadingOut
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerUiState.Idle())

    fun setMinutes(minutes: Int) {
        _selectedMinutes.value = minutes.coerceIn(0, 180)
    }

    fun startTimer() {
        val minutes = _selectedMinutes.value
        if (minutes <= 0) return
        val durationMillis = minutes * 60 * 1000L
        val intent = Intent().apply {
            action = ACTION_START
            setClassName(context, SERVICE_CLASS)
            putExtra(EXTRA_DURATION_MILLIS, durationMillis)
        }
        context.startForegroundService(intent)
    }

    fun stopTimer() {
        val intent = Intent().apply {
            action = ACTION_CANCEL
            setClassName(context, SERVICE_CLASS)
        }
        context.startService(intent)
    }

    fun addFiveMinutes() {
        val intent = Intent().apply {
            action = ACTION_ADD_FIVE
            setClassName(context, SERVICE_CLASS)
        }
        context.startService(intent)
    }

    companion object {
        const val SERVICE_CLASS = "dev.mato.sleeptimer.core.service.SleepTimerService"
        const val ACTION_START = "dev.mato.sleeptimer.action.START"
        const val ACTION_CANCEL = "dev.mato.sleeptimer.action.CANCEL"
        const val ACTION_ADD_FIVE = "dev.mato.sleeptimer.action.ADD_FIVE"
        const val EXTRA_DURATION_MILLIS = "dev.mato.sleeptimer.extra.DURATION_MILLIS"
    }
}
