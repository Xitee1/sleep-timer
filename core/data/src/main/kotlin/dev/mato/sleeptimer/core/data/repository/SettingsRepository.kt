package dev.mato.sleeptimer.core.data.repository

import dev.mato.sleeptimer.core.data.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun updateStopMediaPlayback(enabled: Boolean)
    suspend fun updateFadeOutDuration(seconds: Int)
    suspend fun updateScreenOff(enabled: Boolean)
    suspend fun updateNotificationEnabled(enabled: Boolean)
    suspend fun updateHapticFeedback(enabled: Boolean)
}
