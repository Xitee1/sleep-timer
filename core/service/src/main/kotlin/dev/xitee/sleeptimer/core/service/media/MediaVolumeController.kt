package dev.xitee.sleeptimer.core.service.media

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
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
        restoreVolume()
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
}
