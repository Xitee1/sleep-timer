package dev.xitee.sleeptimer.core.service.shizuku

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
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
     * Bounded by [EXEC_TIMEOUT_MS]; a wedged Shizuku binder can't block cancel forever.
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
            try {
                val exit = withTimeout(EXEC_TIMEOUT_MS) { remote.waitFor() }
                if (exit != 0) {
                    Log.w(TAG, "cmd=${args.joinToString(" ")} exit=$exit")
                }
                exit == 0
            } finally {
                try { remote.destroy() } catch (_: Exception) { /* best-effort cleanup */ }
            }
        } catch (ce: CancellationException) {
            // Also covers TimeoutCancellationException (a CancellationException subclass):
            // treat a timeout as a silent failure rather than propagating cancellation.
            if (ce is TimeoutCancellationException) {
                Log.w(TAG, "cmd=${args.joinToString(" ")} timed out after ${EXEC_TIMEOUT_MS}ms")
                return@withContext false
            }
            throw ce
        } catch (t: Exception) {
            Log.e(TAG, "exec failed: ${args.joinToString(" ")}", t)
            false
        }
    }

    private companion object {
        const val TAG = "ShizukuShell"
        // `svc wifi disable` etc. complete in milliseconds in practice; 5s is a generous
        // ceiling that's still short enough not to delay timer cancel perceptibly.
        const val EXEC_TIMEOUT_MS = 5_000L
    }
}
