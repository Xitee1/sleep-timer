package dev.xitee.sleeptimer.feature.timer.timer

import android.content.Context
import android.content.pm.ActivityInfo
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.OrientationEventListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.xitee.sleeptimer.core.data.model.AutoRotateMode

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

fun DeviceOrientation.toActivityInfoOrientation(): Int = when (this) {
    DeviceOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    DeviceOrientation.LANDSCAPE_LEFT -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
    DeviceOrientation.PORTRAIT_REVERSED -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
    DeviceOrientation.LANDSCAPE_RIGHT -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
}

@Composable
fun rememberDeviceOrientation(mode: AutoRotateMode): State<DeviceOrientation> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(DeviceOrientation.PORTRAIT) }

    // Resolve whether rotation is allowed. In System mode this follows the OS-wide
    // auto-rotate flag; the ContentObserver inside rememberSystemAutoRotate only exists
    // in the composition while this branch is active, so it self-disposes on mode change.
    val enabled = when (mode) {
        AutoRotateMode.System -> rememberSystemAutoRotate().value
        AutoRotateMode.Always -> true
        AutoRotateMode.Portrait -> false
    }

    DisposableEffect(enabled) {
        if (!enabled) {
            // Auto-rotate is off: pin to portrait and never register the sensor so
            // AppOrientationController locks the window and the timer content stops
            // counter-rotating, at no battery cost.
            state.value = DeviceOrientation.PORTRAIT
            return@DisposableEffect onDispose {}
        }
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

// Tracks the system-wide auto-rotate flag (Settings > Display > Auto-rotate, or the
// quick-settings tile). Readable and observable without any permission. The observer is
// process-scoped, so flips made while the app is backgrounded still land before the user
// returns; the synchronous initial read means there is no wrong-value first frame.
@Composable
private fun rememberSystemAutoRotate(): State<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(readSystemAutoRotate(context)) }

    DisposableEffect(context) {
        state.value = readSystemAutoRotate(context)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                state.value = readSystemAutoRotate(context)
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
            false,
            observer,
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }

    return state
}

private fun readSystemAutoRotate(context: Context): Boolean =
    Settings.System.getInt(
        context.contentResolver,
        Settings.System.ACCELEROMETER_ROTATION,
        0,
    ) == 1

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
