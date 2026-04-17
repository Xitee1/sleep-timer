package dev.mato.sleeptimer.feature.timer.timer.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.atan2
import kotlin.math.roundToInt

class CircularDialState {
    var angleDegrees by mutableFloatStateOf(90f)
        private set
    var revolutions by mutableIntStateOf(0)
        private set
    var totalMinutes by mutableIntStateOf(15)
        private set

    private var previousAngle = 90f
    private var isDragging = false

    val maxRevolutions = 3
    private val maxMinutes = maxRevolutions * 60

    fun onDragStart(centerX: Float, centerY: Float, x: Float, y: Float) {
        isDragging = true
        previousAngle = computeAngle(centerX, centerY, x, y)
    }

    fun onDrag(centerX: Float, centerY: Float, x: Float, y: Float) {
        if (!isDragging) return

        val newAngle = computeAngle(centerX, centerY, x, y)
        val rawDelta = newAngle - previousAngle

        // Detect revolution crossing (passing through 0/360 boundary)
        // Raw delta > 180 means the angle jumped backwards across 0° (counterclockwise wrap)
        // Raw delta < -180 means the angle jumped forwards across 360° (clockwise wrap)
        if (rawDelta > 180f) {
            // Crossed boundary counterclockwise — decrease revolution
            if (revolutions > 0) {
                revolutions--
            }
        } else if (rawDelta < -180f) {
            // Crossed boundary clockwise — increase revolution
            if (revolutions < maxRevolutions) {
                revolutions++
            }
        }

        angleDegrees = newAngle
        previousAngle = newAngle
        recalculateMinutes()
    }

    fun onDragEnd() {
        isDragging = false
        // Snap to nearest minute
        val minuteInRevolution = ((angleDegrees / 360f) * 60f).roundToInt().coerceIn(0, 59)
        angleDegrees = (minuteInRevolution / 60f) * 360f
        recalculateMinutes()
    }

    fun setMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(0, maxMinutes)
        revolutions = clamped / 60
        val remainder = clamped % 60
        angleDegrees = (remainder / 60f) * 360f
        totalMinutes = clamped
    }

    private fun recalculateMinutes() {
        val minuteInRevolution = ((angleDegrees / 360f) * 60f).roundToInt().coerceIn(0, 59)
        totalMinutes = (revolutions * 60 + minuteInRevolution).coerceIn(0, maxMinutes)
    }

    private fun computeAngle(centerX: Float, centerY: Float, x: Float, y: Float): Float {
        // Angle in degrees, 0 = top (12 o'clock), clockwise
        val dx = x - centerX
        val dy = y - centerY
        val radians = atan2(dx.toDouble(), -dy.toDouble())
        val degrees = Math.toDegrees(radians).toFloat()
        return ((degrees % 360f) + 360f) % 360f
    }

}

@Composable
fun rememberCircularDialState(): CircularDialState {
    return remember { CircularDialState() }
}
