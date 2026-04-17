package dev.mato.sleeptimer.core.data.repository

import dev.mato.sleeptimer.core.data.model.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerRepositoryImpl @Inject constructor() : TimerRepository {

    private val _timerState = MutableStateFlow(TimerState())

    override val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    override fun updateState(state: TimerState) {
        _timerState.value = state
    }
}
