package dev.xitee.sleeptimer.core.service.shizuku

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuMobileDataController @Inject constructor(
    private val shell: ShizukuShell,
) {
    suspend fun disableMobileData(): Boolean = shell.exec("svc", "data", "disable")
}
