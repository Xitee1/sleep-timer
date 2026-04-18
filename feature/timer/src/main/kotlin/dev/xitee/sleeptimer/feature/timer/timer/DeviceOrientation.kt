package dev.xitee.sleeptimer.feature.timer.timer

import android.view.OrientationEventListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

enum class DeviceOrientation(val degrees: Int) {
    PORTRAIT(0),
    LANDSCAPE_LEFT(90),
    PORTRAIT_REVERSED(180),
    LANDSCAPE_RIGHT(270),
}

fun DeviceOrientation.counterRotationDegrees(): Float = when (this) {
    DeviceOrientation.PORTRAIT -> 0f
    DeviceOrientation.LANDSCAPE_LEFT -> -90f
    DeviceOrientation.PORTRAIT_REVERSED -> 180f
    DeviceOrientation.LANDSCAPE_RIGHT -> 90f
}

@Composable
fun rememberDeviceOrientation(): State<DeviceOrientation> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(DeviceOrientation.PORTRAIT) }

    DisposableEffect(Unit) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val next = snapToOrientation(orientation, state.value)
                if (next != state.value) {
                    state.value = next
                }
            }
        }
        if (listener.canDetectOrientation()) {
            listener.enable()
        }
        onDispose { listener.disable() }
    }

    return state
}

// Hysteresis: stick with the current bucket until the device pose is more than
// 60° away from its centre — 15° past the natural 45° boundary — so small wobbles
// don't flip state.
private fun snapToOrientation(degrees: Int, current: DeviceOrientation): DeviceOrientation {
    val normalized = ((degrees % 360) + 360) % 360
    if (shortestAngularDistance(normalized, current.degrees) <= 60) return current
    return DeviceOrientation.entries.minBy { shortestAngularDistance(normalized, it.degrees) }
}

private fun shortestAngularDistance(a: Int, b: Int): Int {
    val diff = kotlin.math.abs(a - b)
    return kotlin.math.min(diff, 360 - diff)
}
