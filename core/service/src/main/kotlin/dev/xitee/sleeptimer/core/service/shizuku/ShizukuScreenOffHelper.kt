package dev.xitee.sleeptimer.core.service.shizuku

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Soft screen-off via simulated power key press (KEYCODE_POWER = 26).
 * Keeps biometric unlock valid — unlike DevicePolicyManager.lockNow(),
 * which forces the next unlock to require credentials.
 */
@Singleton
class ShizukuScreenOffHelper @Inject constructor(
    private val shell: ShizukuShell,
) {
    suspend fun turnOffScreen(): Boolean = shell.exec("input", "keyevent", "26")
}
