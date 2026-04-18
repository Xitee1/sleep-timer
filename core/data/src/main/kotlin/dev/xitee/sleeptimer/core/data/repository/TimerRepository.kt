package dev.xitee.sleeptimer.core.data.repository

import dev.xitee.sleeptimer.core.data.model.TimerState
import kotlinx.coroutines.flow.StateFlow

/**
 * Read-only view of the timer state, consumed by ViewModels and anywhere else
 * that should not mutate state. Mutation lives on [TimerRepositoryImpl] directly
 * and is only injected where mutation is intended (currently only the service).
 */
interface TimerRepository {
    val timerState: StateFlow<TimerState>
}
