package dev.xitee.sleeptimer.core.service.shizuku

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuWifiController @Inject constructor(
    private val shell: ShizukuShell,
) {
    suspend fun disableWifi(): Boolean = shell.exec("svc", "wifi", "disable")
}
