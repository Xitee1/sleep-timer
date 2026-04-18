package dev.mato.sleeptimer.feature.timer.timer

sealed interface TimerUiState {

    data class Idle(
        val selectedMinutes: Int = 15,
    ) : TimerUiState

    data class Running(
        val totalMinutes: Int,
        val remainingMillis: Long,
        val progress: Float,
    ) : TimerUiState

    data object FadingOut : TimerUiState
}
