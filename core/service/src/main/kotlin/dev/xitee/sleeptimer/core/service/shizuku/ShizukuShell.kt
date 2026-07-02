package dev.xitee.sleeptimer.core.service.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Executes shell commands with shell-uid privileges through Shizuku's UserService
 * API — the supported replacement for the private `newProcess` AIDL, which rikka
 * has announced for removal. [ShellUserService] is spawned by Shizuku on first use
 * and stays bound for the app's lifetime; `daemon(false)` ties the spawned process
 * to ours, so it dies with the app.
 */
@Singleton
class ShizukuShell @Inject constructor(
    @ApplicationContext context: Context,
    private val shizukuManager: ShizukuManager,
) {

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, ShellUserService::class.java.name),
    )
        .daemon(false)
        .processNameSuffix("shell")
        .version(USER_SERVICE_VERSION)

    private val bindMutex = Mutex()

    @Volatile
    private var boundService: IShellUserService? = null

    /**
     * Runs a shell command via Shizuku. Returns true on exit code 0.
     * Safe to call when Shizuku is not ready — returns false silently.
     * The command is bounded by [EXEC_TIMEOUT_MS] inside the user service, so a
     * wedged command can't block timer teardown forever.
     */
    suspend fun exec(vararg args: String): Boolean = withContext(Dispatchers.IO) {
        if (!shizukuManager.isReady()) {
            Log.w(TAG, "exec called but Shizuku not ready: state=${shizukuManager.state.value}")
            return@withContext false
        }
        val service = try {
            withTimeout(BIND_TIMEOUT_MS) { obtainService() }
        } catch (ce: CancellationException) {
            if (ce is TimeoutCancellationException) {
                Log.w(TAG, "binding shell user service timed out after ${BIND_TIMEOUT_MS}ms")
                return@withContext false
            }
            throw ce
        } catch (t: Exception) {
            Log.e(TAG, "binding shell user service failed", t)
            return@withContext false
        }
        try {
            val exit = service.exec(args, EXEC_TIMEOUT_MS)
            if (exit != 0) {
                Log.w(TAG, "cmd=${args.joinToString(" ")} exit=$exit")
            }
            exit == 0
        } catch (t: Exception) {
            // The binder likely died with the user service process — drop the
            // cache so the next call rebinds.
            boundService = null
            Log.e(TAG, "exec failed: ${args.joinToString(" ")}", t)
            false
        }
    }

    private suspend fun obtainService(): IShellUserService {
        boundService?.let { if (it.asBinder().isBinderAlive) return it }
        return bindMutex.withLock {
            val cached = boundService
            if (cached != null && cached.asBinder().isBinderAlive) {
                cached
            } else {
                bindUserService().also { boundService = it }
            }
        }
    }

    private suspend fun bindUserService(): IShellUserService =
        suspendCancellableCoroutine { continuation ->
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    if (binder == null || !binder.pingBinder()) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException("invalid binder received for $name"),
                            )
                        }
                        return
                    }
                    val service = IShellUserService.Stub.asInterface(binder)
                    if (continuation.isActive) continuation.resume(service)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    boundService = null
                }
            }
            try {
                Shizuku.bindUserService(userServiceArgs, connection)
            } catch (t: Throwable) {
                if (continuation.isActive) continuation.resumeWithException(t)
            }
        }

    private companion object {
        const val TAG = "ShizukuShell"

        // Bump when ShellUserService's AIDL or behavior changes — Shizuku replaces
        // a running user service whose version differs.
        const val USER_SERVICE_VERSION = 1

        // The first bind spawns a new process via Shizuku; give it more headroom
        // than a plain command.
        const val BIND_TIMEOUT_MS = 10_000L

        // `svc wifi disable` etc. complete in milliseconds in practice; 5s is a generous
        // ceiling that's still short enough not to delay timer cancel perceptibly.
        const val EXEC_TIMEOUT_MS = 5_000L
    }
}
