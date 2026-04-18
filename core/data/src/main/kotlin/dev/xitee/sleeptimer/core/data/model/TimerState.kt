package dev.xitee.sleeptimer.core.data.model

data class TimerState(
    val phase: TimerPhase = TimerPhase.IDLE,
    val totalDurationMillis: Long = 0L,
    val remainingMillis: Long = 0L,
)

enum class TimerPhase {
    IDLE,
    RUNNING,
    FADING_OUT,
    FINISHED,
}
