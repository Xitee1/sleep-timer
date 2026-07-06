package dev.xitee.sleeptimer.core.service.screen

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Marks the [ComponentName] of the app's DeviceAdminReceiver. Provided by the app
 * module — the only module that can reference the receiver class directly, so a
 * rename there can't silently break the string-based lookups here.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DeviceAdminComponent

@Singleton
class ScreenLockHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    @DeviceAdminComponent val adminComponent: ComponentName,
) {
    private val devicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun isAdminActive(): Boolean = devicePolicyManager.isAdminActive(adminComponent)

    fun lockScreen(): Boolean {
        return if (isAdminActive()) {
            devicePolicyManager.lockNow()
            true
        } else {
            false
        }
    }
}
