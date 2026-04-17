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

    val maxRevolutions = 5 // 5 hours
    private val maxMinutes = maxRevolutions * 60

    fun onDragStart(centerX: Float, centerY: Float, x: Float, y: Float) {
        isDragging = true
        previousAngle = computeAngle(centerX, centerY, x, y)
    }

    fun onDrag(centerX: Float, centerY: Float, x: Float, y: Float) {
        if (!isDragging) return

        val newAngle = computeAngle(centerX, centerY, x, y)
        val rawDelta = newAngle - previousAngle

        if (rawDelta > 180f) {
            if (revolutions > 0) revolutions--
        } else if (rawDelta < -180f) {
            if (revolutions < maxRevolutions) revolutions++
        }

        angleDegrees = newAngle
        previousAngle = newAngle
        recalculateMinutes()
    }

    fun onDragEnd() {
        isDragging = false
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
