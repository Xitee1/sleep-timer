package dev.xitee.sleeptimer.core.data.model

/** Upper bound for any timer duration — shared by the dial UI, the service, and the persisted preset. */
const val MAX_TIMER_MINUTES = 240

data class TimerState(
    val phase: TimerPhase = TimerPhase.IDLE,
    val totalDurationMillis: Long = 0L,
    val remainingMillis: Long = 0L,
)

enum class TimerPhase {
    IDLE,
    RUNNING,
    FADING_OUT,
}
