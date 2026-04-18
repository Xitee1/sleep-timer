package dev.xitee.sleeptimer.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.xitee.sleeptimer.core.data.model.ThemeId
import dev.xitee.sleeptimer.core.data.model.UserSettings
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
        val SOFT_SCREEN_OFF = booleanPreferencesKey("soft_screen_off")
        val TURN_OFF_WIFI = booleanPreferencesKey("turn_off_wifi")
        val TURN_OFF_BLUETOOTH = booleanPreferencesKey("turn_off_bluetooth")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val THEME = stringPreferencesKey("theme")
        val STARS_ENABLED = booleanPreferencesKey("stars_enabled")
        val STEP_MINUTES = intPreferencesKey("step_minutes")
        val PRESET_MINUTES = intPreferencesKey("preset_minutes")
    }

    override val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            stopMediaPlayback = prefs[STOP_MEDIA] ?: true,
            fadeOutDurationSeconds = prefs[FADE_OUT_DURATION] ?: 30,
            screenOff = prefs[SCREEN_OFF] ?: false,
            softScreenOff = prefs[SOFT_SCREEN_OFF] ?: false,
            turnOffWifi = prefs[TURN_OFF_WIFI] ?: false,
            turnOffBluetooth = prefs[TURN_OFF_BLUETOOTH] ?: false,
            hapticFeedbackEnabled = prefs[HAPTIC_FEEDBACK] ?: true,
            theme = ThemeId.fromStorage(prefs[THEME]),
            starsEnabled = prefs[STARS_ENABLED] ?: true,
            stepMinutes = prefs[STEP_MINUTES] ?: 5,
            presetMinutes = prefs[PRESET_MINUTES] ?: 15,
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

    override suspend fun updateSoftScreenOff(enabled: Boolean) {
        dataStore.edit { it[SOFT_SCREEN_OFF] = enabled }
    }

    override suspend fun updateTurnOffWifi(enabled: Boolean) {
        dataStore.edit { it[TURN_OFF_WIFI] = enabled }
    }

    override suspend fun updateTurnOffBluetooth(enabled: Boolean) {
        dataStore.edit { it[TURN_OFF_BLUETOOTH] = enabled }
    }

    override suspend fun updateHapticFeedback(enabled: Boolean) {
        dataStore.edit { it[HAPTIC_FEEDBACK] = enabled }
    }

    override suspend fun updateTheme(theme: ThemeId) {
        dataStore.edit { it[THEME] = theme.name }
    }

    override suspend fun updateStarsEnabled(enabled: Boolean) {
        dataStore.edit { it[STARS_ENABLED] = enabled }
    }

    override suspend fun updateStepMinutes(minutes: Int) {
        dataStore.edit { it[STEP_MINUTES] = minutes.coerceIn(1, 30) }
    }

    override suspend fun updatePresetMinutes(minutes: Int) {
        dataStore.edit { it[PRESET_MINUTES] = minutes.coerceIn(1, 300) }
    }
}
