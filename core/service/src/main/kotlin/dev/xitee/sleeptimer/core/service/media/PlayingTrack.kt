package dev.xitee.sleeptimer.core.service.media

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock

/**
 * The song that is currently playing. [key] identifies it across polls, so a change
 * of song can be told apart from seeking within it. Parcelable because the Shizuku
 * path returns it across the user-service binder.
 */
class PlayingTrack(
    val key: String,
    val remainingMillis: Long,
) : Parcelable {

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(key)
        dest.writeLong(remainingMillis)
    }

    companion object CREATOR : Parcelable.Creator<PlayingTrack> {
        override fun createFromParcel(source: Parcel): PlayingTrack =
            PlayingTrack(key = source.readString().orEmpty(), remainingMillis = source.readLong())

        override fun newArray(size: Int): Array<PlayingTrack?> = arrayOfNulls(size)
    }
}

private const val KEY_SEPARATOR = "\u001F"

/**
 * The song in the highest-priority playing session, or null when nothing is playing
 * or that song's length or position is unknown (e.g. a livestream).
 *
 * Shared by both read paths — the app process (notification access) and the Shizuku
 * shell process — so it must not touch app infrastructure.
 */
internal fun List<MediaController>.playingTrack(): PlayingTrack? {
    // getActiveSessions orders by priority, so the first playing session is the one
    // the user hears (and the one a media-key pause reaches).
    val controller = firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        ?: return null
    val state = controller.playbackState ?: return null
    val metadata = controller.metadata ?: return null
    val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
    if (duration <= 0L || state.position == PlaybackState.PLAYBACK_POSITION_UNKNOWN) return null
    // The reported position is a snapshot from lastPositionUpdateTime; extrapolate
    // it on the same elapsedRealtime clock, scaled by the playback speed.
    val speed = state.playbackSpeed.takeIf { it > 0f } ?: 1f
    val elapsedSinceUpdate = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
    val position = state.position + (elapsedSinceUpdate * speed).toLong()
    // The app's media ID where it sets one, else title, artist and length. Prefixed
    // with the app, so another player taking over also counts as a different song.
    val mediaId = metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
    val trackId = mediaId?.takeIf { it.isNotEmpty() }
        ?: listOf(
            metadata.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            metadata.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty(),
            duration.toString(),
        ).joinToString(KEY_SEPARATOR)
    return PlayingTrack(
        key = controller.packageName + KEY_SEPARATOR + trackId,
        remainingMillis = ((duration - position) / speed).toLong().coerceAtLeast(0L),
    )
}
