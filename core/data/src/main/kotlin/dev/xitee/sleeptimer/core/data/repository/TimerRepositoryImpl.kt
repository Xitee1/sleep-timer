package dev.xitee.sleeptimer.core.data.repository

import dev.xitee.sleeptimer.core.data.model.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerRepositoryImpl @Inject constructor() : TimerRepository {

    private val _timerState = MutableStateFlow(TimerState())

    override val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    /**
     * Mutation intentionally lives on the concrete class, not the interface, so
     * read-only consumers (ViewModels) can't write. Only injected where mutation
     * is legitimate (currently only SleepTimerService).
     */
    fun updateState(state: TimerState) {
        _timerState.value = state
    }
}
