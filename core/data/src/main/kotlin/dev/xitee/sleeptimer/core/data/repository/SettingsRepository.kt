package dev.xitee.sleeptimer.core.data.repository

import dev.xitee.sleeptimer.core.data.model.ThemeId
import dev.xitee.sleeptimer.core.data.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun updateStopMediaPlayback(enabled: Boolean)
    suspend fun updateFadeOutDuration(seconds: Int)
    suspend fun updateScreenOff(enabled: Boolean)
    suspend fun updateSoftScreenOff(enabled: Boolean)
    suspend fun updateTurnOffWifi(enabled: Boolean)
    suspend fun updateTurnOffBluetooth(enabled: Boolean)
    suspend fun updateHapticFeedback(enabled: Boolean)
    suspend fun updateTheme(theme: ThemeId)
    suspend fun updateStarsEnabled(enabled: Boolean)
    suspend fun updateStepMinutes(minutes: Int)
}
