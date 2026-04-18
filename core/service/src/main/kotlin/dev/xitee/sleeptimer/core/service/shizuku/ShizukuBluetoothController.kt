package dev.xitee.sleeptimer.core.service.shizuku

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuBluetoothController @Inject constructor(
    private val shell: ShizukuShell,
) {
    suspend fun disableBluetooth(): Boolean = shell.exec("svc", "bluetooth", "disable")
}
