package dev.xitee.sleeptimer.core.data.util

import android.content.Context
import android.provider.Settings

/**
 * Gibt true zurück, wenn der Nutzer in den System-Einstellungen „Animationen entfernen"
 * aktiviert hat. Erkennung über `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`, was
 * von Accessibility-Settings und den Developer-Options identisch gesetzt wird.
 */
fun isSystemReduceMotionEnabled(context: Context): Boolean {
    val scale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    return scale == 0f
}
