package dev.xitee.sleeptimer.core.service.media

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.SystemClock

/**
 * Remaining playback time of the song in the highest-priority playing session, or
 * null when nothing is playing or that song has no known length (e.g. a livestream).
 *
 * Shared by both read paths — the app process (notification access) and the Shizuku
 * shell process — so it must not touch app infrastructure.
 */
internal fun List<MediaController>.playingTrackRemainingMillis(): Long? {
    // getActiveSessions orders by priority, so the first playing session is the one
    // the user hears (and the one a media-key pause reaches).
    val controller = firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        ?: return null
    val state = controller.playbackState ?: return null
    val duration = controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
    if (duration <= 0L) return null
    // The reported position is a snapshot from lastPositionUpdateTime; extrapolate
    // it on the same elapsedRealtime clock, scaled by the playback speed.
    val speed = state.playbackSpeed.takeIf { it > 0f } ?: 1f
    val elapsedSinceUpdate = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
    val position = state.position + (elapsedSinceUpdate * speed).toLong()
    return ((duration - position) / speed).toLong().coerceAtLeast(0L)
}
