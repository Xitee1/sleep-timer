package dev.xitee.sleeptimer.core.service.media

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaVolumeController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var originalVolume: Int = -1

    suspend fun fadeOutAndPause(durationSeconds: Int) {
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        if (originalVolume <= 0 || durationSeconds <= 0) {
            pauseMedia()
            return
        }

        val steps = originalVolume
        val intervalMs = (durationSeconds * 1000L) / steps

        for (i in originalVolume - 1 downTo 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, i, 0)
            delay(intervalMs)
        }

        pauseMedia()
        awaitPlaybackStopped()
        restoreVolume()
    }

    /**
     * The pause key is handled asynchronously, and many players ramp down on pause
     * themselves — restoring the volume right after dispatching it plays that tail at
     * full volume. Wait until the music stream goes quiet (bounded, in case the player
     * ignores the key), then let the audio buffer drain.
     */
    private suspend fun awaitPlaybackStopped() {
        withTimeoutOrNull(PAUSE_TIMEOUT_MILLIS) {
            while (audioManager.isMusicActive) delay(PAUSE_POLL_MILLIS)
        }
        delay(PAUSE_SETTLE_MILLIS)
    }

    suspend fun fadeInToOriginal(durationSeconds: Int) {
        val target = originalVolume
        if (target < 0) return
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (current >= target || durationSeconds <= 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
            originalVolume = -1
            return
        }
        val steps = target - current
        val intervalMs = (durationSeconds * 1000L) / steps
        for (i in current + 1..target) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, i, 0)
            delay(intervalMs)
        }
        originalVolume = -1
    }

    fun pauseMedia() {
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE)
        audioManager.dispatchMediaKeyEvent(downEvent)
        audioManager.dispatchMediaKeyEvent(upEvent)
    }

    fun restoreVolume() {
        if (originalVolume >= 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
            originalVolume = -1
        }
    }

    private companion object {
        const val PAUSE_TIMEOUT_MILLIS = 3_000L
        const val PAUSE_POLL_MILLIS = 100L
        const val PAUSE_SETTLE_MILLIS = 500L
    }
}
