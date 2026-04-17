package dev.mato.sleeptimer.core.service.media

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
