package dev.xitee.sleeptimer.core.service.shake

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Detects a deliberate shake from the accelerometer and reports one shake per gesture.
 *
 * A sample counts as accelerating when the total acceleration clearly exceeds gravity.
 * A shake is a window of at least [MIN_SPAN_NANOS] in which most samples accelerate, so
 * single bumps (putting the phone down, turning over in bed) don't count. After a shake,
 * motion is ignored for [COOLDOWN_NANOS]: a shake that lasts a little longer must not
 * extend the timer twice.
 *
 * Uses the non-wake-up accelerometer: with the screen off, events only arrive while
 * something keeps the CPU awake (e.g. media playback). Callbacks run on the main thread.
 */
class ShakeDetector @Inject constructor(
    @ApplicationContext context: Context,
) : SensorEventListener {

    private class Sample(val timestampNanos: Long, val accelerating: Boolean)

    private val sensorManager: SensorManager? = context.getSystemService(SensorManager::class.java)
    private val samples = ArrayDeque<Sample>()
    private var acceleratingCount = 0
    private var cooldownUntilNanos = Long.MIN_VALUE
    private var onShake: (() -> Unit)? = null

    /** Starts listening. No-op while already listening or without an accelerometer. */
    fun start(onShake: () -> Unit) {
        if (this.onShake != null) return
        val manager = sensorManager ?: return
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        this.onShake = onShake
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    /** Stops listening. No-op while not listening. */
    fun stop() {
        if (onShake == null) return
        sensorManager?.unregisterListener(this)
        onShake = null
        clearSamples()
        cooldownUntilNanos = Long.MIN_VALUE
    }

    override fun onSensorChanged(event: SensorEvent) {
        val now = event.timestamp
        if (now < cooldownUntilNanos) return
        val (x, y, z) = event.values
        addSample(now, accelerating = x * x + y * y + z * z > THRESHOLD_SQUARED)
        if (!isShaking()) return
        clearSamples()
        cooldownUntilNanos = now + COOLDOWN_NANOS
        onShake?.invoke()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun addSample(timestampNanos: Long, accelerating: Boolean) {
        while (samples.isNotEmpty() && timestampNanos - samples.first().timestampNanos > WINDOW_NANOS) {
            if (samples.removeFirst().accelerating) acceleratingCount--
        }
        samples.addLast(Sample(timestampNanos, accelerating))
        if (accelerating) acceleratingCount++
    }

    private fun isShaking(): Boolean {
        if (samples.size < MIN_SAMPLES) return false
        val span = samples.last().timestampNanos - samples.first().timestampNanos
        return span >= MIN_SPAN_NANOS && acceleratingCount * 4 >= samples.size * 3
    }

    private fun clearSamples() {
        samples.clear()
        acceleratingCount = 0
    }

    private companion object {
        // Total acceleration (gravity included) a sample must exceed, in m/s². Gravity
        // alone is ~9.8. 11 is Seismic's light level: the phone is shaken half asleep
        // in bed, so a moderate shake must be enough.
        const val THRESHOLD = 11f
        const val THRESHOLD_SQUARED = THRESHOLD * THRESHOLD

        // Sliding window that is evaluated, and the span of it a shake must fill.
        const val WINDOW_NANOS = 500_000_000L
        const val MIN_SPAN_NANOS = 250_000_000L

        // Guards against a sparse window, e.g. two samples around a delivery gap.
        const val MIN_SAMPLES = 5

        const val COOLDOWN_NANOS = 1_500_000_000L
    }
}
