package dev.xitee.sleeptimer.core.data.model

/**
 * How the screen is turned off when the timer expires (only relevant while
 * [UserSettings.screenOff] is enabled).
 *
 * - [DeviceAdmin] — `DevicePolicyManager.lockNow()`; requires credential on next unlock.
 * - [Accessibility] — `GLOBAL_ACTION_LOCK_SCREEN` via the app's accessibility service;
 *   behaves like the power button, biometric unlock stays valid. API 28+.
 * - [Shizuku] — simulated power-key press through Shizuku; biometric unlock stays valid.
 */
enum class ScreenLockMethod {
    DeviceAdmin,
    Accessibility,
    Shizuku,
    ;

    companion object {
        val Default: ScreenLockMethod = DeviceAdmin

        fun fromStorage(value: String?): ScreenLockMethod =
            entries.firstOrNull { it.name == value } ?: Default
    }
}
