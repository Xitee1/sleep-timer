package dev.xitee.sleeptimer.core.service.shake

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Confirms a shake-to-extend by vibration. Independent of the in-app haptic feedback
 * setting: with the screen off it is the only sign that the timer was extended.
 */
class ShakeVibrator @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    /** Two short pulses, distinct from a notification buzz. */
    fun vibrateExtended() {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        vibrator.vibrate(VibrationEffect.createWaveform(EXTENDED_TIMINGS, NO_REPEAT))
    }

    private companion object {
        // Off/on pairs in ms: wait 0, buzz 60, pause 100, buzz 60.
        val EXTENDED_TIMINGS = longArrayOf(0, 60, 100, 60)
        const val NO_REPEAT = -1
    }
}
