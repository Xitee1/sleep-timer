package dev.xitee.sleeptimer.core.service.shizuku

import android.content.Context
import android.media.session.MediaSessionManager
import android.util.Log
import dev.xitee.sleeptimer.core.service.media.playingTrackRemainingMillis
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Runs inside the process Shizuku spawns with shell (uid 2000) privileges, where no
 * Application or Hilt graph exists — don't reference Hilt or app singletons here.
 *
 * Shizuku instantiates this class by name: servers since API 13 prefer the
 * `(Context)` constructor and fall back to the no-arg one, so both must stay.
 */
class ShellUserService(private val context: Context?) : IShellUserService.Stub() {

    constructor() : this(null)

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

    override fun playingTrackRemainingMillis(): Long = try {
        // A null listener component makes the system check MEDIA_CONTENT_CONTROL,
        // which the shell uid holds — no notification access needed on this path.
        context?.getSystemService(MediaSessionManager::class.java)
            ?.getActiveSessions(null)
            ?.playingTrackRemainingMillis()
            ?: UNKNOWN_REMAINING
    } catch (t: Throwable) {
        Log.e(TAG, "reading the playing track failed", t)
        UNKNOWN_REMAINING
    }

    private companion object {
        const val TAG = "ShellUserService"
        const val EXIT_ERROR = -1
        const val EXIT_TIMEOUT = -2
        const val UNKNOWN_REMAINING = -1L
    }
}
