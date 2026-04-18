package dev.xitee.sleeptimer.feature.timer.timer.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
    var isDragging by mutableStateOf(false)
        private set

    private var previousAngle = 90f
    private var cumulativeDegrees = 0f

    val maxRevolutions = 5 // 5 hours
    private val minMinutes = 1
    private val maxMinutes = maxRevolutions * 60
    private val minDegrees = (minMinutes / 60f) * 360f
    private val maxDegrees = maxRevolutions * 360f

    init {
        // Sync cumulative with default totalMinutes (15)
        cumulativeDegrees = (totalMinutes / 60f) * 360f
        revolutions = totalMinutes / 60
        angleDegrees = cumulativeDegrees - revolutions * 360f
    }

    fun onDragStart(centerX: Float, centerY: Float, x: Float, y: Float) {
        isDragging = true
        previousAngle = computeAngle(centerX, centerY, x, y)
        cumulativeDegrees = (revolutions * 360f + angleDegrees).coerceIn(minDegrees, maxDegrees)
    }

    fun onDrag(centerX: Float, centerY: Float, x: Float, y: Float) {
        if (!isDragging) return

        val newAngle = computeAngle(centerX, centerY, x, y)
        var delta = newAngle - previousAngle
        if (delta > 180f) delta -= 360f
        else if (delta < -180f) delta += 360f

        cumulativeDegrees = (cumulativeDegrees + delta).coerceIn(minDegrees, maxDegrees)
        previousAngle = newAngle

        revolutions = (cumulativeDegrees / 360f).toInt().coerceAtMost(maxRevolutions)
        angleDegrees = cumulativeDegrees - revolutions * 360f
        totalMinutes = ((cumulativeDegrees / 360f) * 60f).roundToInt().coerceIn(minMinutes, maxMinutes)
    }

    fun onDragEnd() {
        isDragging = false
        setMinutes(totalMinutes)
    }

    fun setMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(minMinutes, maxMinutes)
        totalMinutes = clamped
        cumulativeDegrees = (clamped / 60f) * 360f
        revolutions = clamped / 60
        val remainder = clamped % 60
        angleDegrees = (remainder / 60f) * 360f
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
