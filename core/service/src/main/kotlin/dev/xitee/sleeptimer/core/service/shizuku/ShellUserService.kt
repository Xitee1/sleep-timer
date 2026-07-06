package dev.xitee.sleeptimer.core.service.shizuku

import android.util.Log
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Runs inside the process Shizuku spawns with shell (uid 2000) privileges.
 * Instantiated there by name via its no-arg constructor — don't add constructor
 * parameters or reference Hilt/app infrastructure from this class.
 */
class ShellUserService : IShellUserService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(command: Array<String>, timeoutMillis: Long): Int = try {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        if (process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
            process.exitValue()
        } else {
            process.destroyForcibly()
            Log.w(TAG, "cmd=${command.joinToString(" ")} timed out after ${timeoutMillis}ms")
            EXIT_TIMEOUT
        }
    } catch (t: Throwable) {
        Log.e(TAG, "exec failed: ${command.joinToString(" ")}", t)
        EXIT_ERROR
    }

    private companion object {
        const val TAG = "ShellUserService"
        const val EXIT_ERROR = -1
        const val EXIT_TIMEOUT = -2
    }
}
