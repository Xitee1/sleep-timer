package dev.xitee.sleeptimer.core.service.shizuku

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuShell @Inject constructor(
    private val shizukuManager: ShizukuManager,
) {

    /**
     * Runs a shell command via Shizuku. Returns true on exit code 0.
     * Safe to call when Shizuku is not ready — returns false silently.
     */
    suspend fun exec(vararg args: String): Boolean = withContext(Dispatchers.IO) {
        if (!shizukuManager.isReady()) {
            Log.w(TAG, "exec called but Shizuku not ready: state=${shizukuManager.state.value}")
            return@withContext false
        }
        try {
            val binder = Shizuku.getBinder() ?: return@withContext false
            val service = IShizukuService.Stub.asInterface(binder)
            val remote = service.newProcess(args, null, null)
            val exit = remote.waitFor()
            if (exit != 0) {
                Log.w(TAG, "cmd=${args.joinToString(" ")} exit=$exit")
            }
            exit == 0
        } catch (t: Exception) {
            Log.e(TAG, "exec failed: ${args.joinToString(" ")}", t)
            false
        }
    }

    private companion object {
        const val TAG = "ShizukuShell"
    }
}
