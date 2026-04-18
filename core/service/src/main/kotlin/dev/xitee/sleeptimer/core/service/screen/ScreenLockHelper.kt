package dev.xitee.sleeptimer.core.service.screen

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenLockHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val devicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val adminComponent = ComponentName(
        context,
        "dev.xitee.sleeptimer.receiver.SleepTimerDeviceAdminReceiver",
    )

    fun lockScreen(): Boolean {
        return if (devicePolicyManager.isAdminActive(adminComponent)) {
            devicePolicyManager.lockNow()
            true
        } else {
            false
        }
    }
}
