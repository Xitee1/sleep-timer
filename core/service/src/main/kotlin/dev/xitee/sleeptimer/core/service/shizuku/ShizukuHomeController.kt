package dev.xitee.sleeptimer.core.service.shizuku

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Returns to the home screen via a simulated home key press (KEYCODE_HOME = 3).
 * Goes through Shizuku because Android 10+ blocks background activity launches,
 * so a plain ACTION_MAIN/CATEGORY_HOME startActivity from the service would be
 * silently dropped while another app is in the foreground.
 */
@Singleton
class ShizukuHomeController @Inject constructor(
    private val shell: ShizukuShell,
) {
    suspend fun goHome(): Boolean = shell.exec("input", "keyevent", "3")
}
