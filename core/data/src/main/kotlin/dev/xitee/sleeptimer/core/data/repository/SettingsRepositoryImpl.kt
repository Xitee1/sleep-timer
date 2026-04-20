package dev.xitee.sleeptimer.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.xitee.sleeptimer.core.data.model.ThemeId
import dev.xitee.sleeptimer.core.data.model.UserSettings
import dev.xitee.sleeptimer.core.data.util.isSystemReduceMotionEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context,
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
        val LAUNCH_ANIMATION_ENABLED = booleanPreferencesKey("launch_animation_enabled")
        val LAUNCH_ANIMATION_SEEDED = booleanPreferencesKey("launch_animation_seeded")
    }

    // Einmaliger Init-Scope. IO-Dispatcher ist angemessen für DataStore-Writes,
    // SupervisorJob verhindert dass eine Child-Exception weitere Writes stoppt.
    private val initScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Seed-on-first-install: ist der „seeded"-Flag nicht gesetzt, wird das
        // launchAnimationEnabled-Feld einmalig basierend auf der System-Reduce-Motion-
        // Präferenz persistiert. Danach gewinnen User-Overrides. Spätere System-Änderungen
        // werden bewusst nicht reflektiert (siehe Spec, Out-of-Scope).
        initScope.launch {
            dataStore.edit { prefs ->
                if (prefs[LAUNCH_ANIMATION_SEEDED] != true) {
                    prefs[LAUNCH_ANIMATION_ENABLED] = !isSystemReduceMotionEnabled(context)
                    prefs[LAUNCH_ANIMATION_SEEDED] = true
                }
            }
        }
    }

    override val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        // Single source of truth: defaults come from UserSettings(), so adding a new
        // field only requires updating the data class.
        val d = UserSettings()
        UserSettings(
            stopMediaPlayback = prefs[STOP_MEDIA] ?: d.stopMediaPlayback,
            fadeOutDurationSeconds = prefs[FADE_OUT_DURATION] ?: d.fadeOutDurationSeconds,
            screenOff = prefs[SCREEN_OFF] ?: d.screenOff,
            softScreenOff = prefs[SOFT_SCREEN_OFF] ?: d.softScreenOff,
            turnOffWifi = prefs[TURN_OFF_WIFI] ?: d.turnOffWifi,
            turnOffBluetooth = prefs[TURN_OFF_BLUETOOTH] ?: d.turnOffBluetooth,
            hapticFeedbackEnabled = prefs[HAPTIC_FEEDBACK] ?: d.hapticFeedbackEnabled,
            theme = ThemeId.fromStorage(prefs[THEME]),
            starsEnabled = prefs[STARS_ENABLED] ?: d.starsEnabled,
            stepMinutes = prefs[STEP_MINUTES] ?: d.stepMinutes,
            presetMinutes = prefs[PRESET_MINUTES] ?: d.presetMinutes,
            launchAnimationEnabled = prefs[LAUNCH_ANIMATION_ENABLED] ?: d.launchAnimationEnabled,
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

    override suspend fun updateLaunchAnimationEnabled(enabled: Boolean) {
        dataStore.edit { it[LAUNCH_ANIMATION_ENABLED] = enabled }
    }
}
