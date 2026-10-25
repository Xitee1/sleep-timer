package dev.xitee.sleeptimer.core.service.media

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaSessionManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.xitee.sleeptimer.core.data.model.TrackEndMethod
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuShell
import javax.inject.Inject
import javax.inject.Singleton

/** Reads the remaining time of the playing song through the chosen [TrackEndMethod]. */
@Singleton
class PlayingTrackReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shizukuShell: ShizukuShell,
) {
    val listenerComponent = ComponentName(context, MediaNotificationListener::class.java)

    private val mediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    /** Whether the user has granted notification access to [MediaNotificationListener]. */
    fun isNotificationAccessGranted(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /**
     * Remaining time of the playing song in milliseconds, or null when it can't be
     * determined: nothing playing, unknown length, or the method's access is missing.
     */
    suspend fun remainingMillis(method: TrackEndMethod): Long? = when (method) {
        TrackEndMethod.None -> null
        TrackEndMethod.NotificationAccess -> remainingViaNotificationAccess()
        TrackEndMethod.Shizuku -> shizukuShell.playingTrackRemainingMillis()
    }

    private fun remainingViaNotificationAccess(): Long? {
        if (!isNotificationAccessGranted()) return null
        return try {
            mediaSessionManager.getActiveSessions(listenerComponent).playingTrackRemainingMillis()
        } catch (e: SecurityException) {
            // Access revoked between the check and the call.
            Log.w(TAG, "getActiveSessions denied", e)
            null
        }
    }

    private companion object {
        const val TAG = "PlayingTrackReader"
    }
}
