package dev.xitee.sleeptimer.feature.timer.timer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

// Owns the Activity's requestedOrientation for the whole session. Driven by the
// caller's already-tracked DeviceOrientation so there is no disposal gap when the
// lock is released — the pose is known at all times, avoiding Android's own sensor
// debounce on the handoff.
@Composable
fun AppOrientationController(orientation: DeviceOrientation, lockPortrait: Boolean) {
    val context = LocalContext.current

    LaunchedEffect(lockPortrait, orientation) {
        val activity = context.findActivity() ?: return@LaunchedEffect
        activity.requestedOrientation = if (lockPortrait) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            orientation.toActivityInfoOrientation()
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
