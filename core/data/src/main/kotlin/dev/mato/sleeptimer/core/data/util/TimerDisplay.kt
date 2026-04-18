package dev.mato.sleeptimer.core.data.util

/**
 * Converts a remaining duration to the minutes count shown to the user.
 *
 * Uses ceiling rounding: while any fractional minute is left, the displayed number
 * stays one higher — so a fresh 30-minute timer shows "30" for the first 60 seconds,
 * not "29" after the first tick.
 */
fun remainingMillisToDisplayMinutes(remainingMillis: Long): Int {
    if (remainingMillis <= 0L) return 0
    return ((remainingMillis + 59_999L) / 60_000L).toInt()
}
