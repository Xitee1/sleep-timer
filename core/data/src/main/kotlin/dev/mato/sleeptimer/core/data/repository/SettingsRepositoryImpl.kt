package dev.mato.sleeptimer.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import dev.mato.sleeptimer.core.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private companion object {
        val STOP_MEDIA = booleanPreferencesKey("stop_media_playback")
        val FADE_OUT_DURATION = intPreferencesKey("fade_out_duration_seconds")
        val SCREEN_OFF = booleanPreferencesKey("screen_off")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
    }

    override val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            stopMediaPlayback = prefs[STOP_MEDIA] ?: true,
            fadeOutDurationSeconds = prefs[FADE_OUT_DURATION] ?: 30,
            screenOff = prefs[SCREEN_OFF] ?: false,
            notificationEnabled = prefs[NOTIFICATION_ENABLED] ?: true,
            hapticFeedbackEnabled = prefs[HAPTIC_FEEDBACK] ?: true,
        )
    }

    override suspend fun updateStopMediaPlayback(enabled: Boolean) {
        dataStore.edit { it[STOP_MEDIA] = enabled }
    }

    override suspend fun updateFadeOutDuration(seconds: Int) {
        dataStore.edit { it[FADE_OUT_DURATION] = seconds }
    }

    override suspend fun updateScreenOff(enabled: Boolean) {
        dataStore.edit { it[SCREEN_OFF] = enabled }
    }

    override suspend fun updateNotificationEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATION_ENABLED] = enabled }
    }

    override suspend fun updateHapticFeedback(enabled: Boolean) {
        dataStore.edit { it[HAPTIC_FEEDBACK] = enabled }
    }
}
