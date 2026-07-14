package dev.xitee.sleeptimer.core.service.media

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.xitee.sleeptimer.core.data.model.MediaEndAction
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaVolumeController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var originalVolume: Int = -1

    suspend fun fadeOutAndEndPlayback(durationSeconds: Int, endAction: MediaEndAction) {
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        if (originalVolume <= 0 || durationSeconds <= 0) {
            endPlayback(endAction)
            return
        }

        val steps = originalVolume
        val intervalMs = (durationSeconds * 1000L) / steps

        for (i in originalVolume - 1 downTo 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, i, 0)
            delay(intervalMs)
        }

        endPlayback(endAction)
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

    private fun endPlayback(action: MediaEndAction) {
        val keyCode = when (action) {
            MediaEndAction.Pause -> KeyEvent.KEYCODE_MEDIA_PAUSE
            MediaEndAction.Stop -> KeyEvent.KEYCODE_MEDIA_STOP
        }
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    fun restoreVolume() {
        if (originalVolume >= 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
            originalVolume = -1
        }
    }
}
