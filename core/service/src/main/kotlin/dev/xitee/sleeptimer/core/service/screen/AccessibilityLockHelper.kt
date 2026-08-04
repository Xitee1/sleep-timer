package dev.xitee.sleeptimer.core.service.screen

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Screen lock via [LockAccessibilityService] (`GLOBAL_ACTION_LOCK_SCREEN`).
 * Behaves like a power-button lock — biometric unlock stays valid — without
 * needing Shizuku. Requires API 28+ and the user enabling the service in the
 * system accessibility settings.
 */
@Singleton
class AccessibilityLockHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val serviceComponent = ComponentName(context, LockAccessibilityService::class.java)

    /** Whether the user has enabled the lock service in the accessibility settings. */
    fun isServiceEnabled(): Boolean {
        if (!isSupported) return false
        // Settings.Secure instead of LockAccessibilityService.instance: the instance
        // only proves the service is currently bound, while the settings row is the
        // authoritative grant — and it is queryable even right after the user returns
        // from the settings screen, before the system finished binding.
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabledServices.split(':')
            .any { ComponentName.unflattenFromString(it) == serviceComponent }
    }

    fun lockScreen(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return LockAccessibilityService.instance
            ?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) == true
    }

    companion object {
        /** GLOBAL_ACTION_LOCK_SCREEN exists since API 28 (minSdk is 26). */
        val isSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }
}
