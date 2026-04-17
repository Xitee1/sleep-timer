package dev.mato.sleeptimer.core.data.repository

import dev.mato.sleeptimer.core.data.model.TimerState
import kotlinx.coroutines.flow.StateFlow

interface TimerRepository {
    val timerState: StateFlow<TimerState>
    fun updateState(state: TimerState)
}
