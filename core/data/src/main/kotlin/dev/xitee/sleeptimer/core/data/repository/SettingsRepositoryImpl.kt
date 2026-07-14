package dev.xitee.sleeptimer.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.xitee.sleeptimer.core.data.model.AutoRotateMode
import dev.xitee.sleeptimer.core.data.model.MAX_TIMER_MINUTES
import dev.xitee.sleeptimer.core.data.model.ThemeId
import dev.xitee.sleeptimer.core.data.model.UserSettings
import dev.xitee.sleeptimer.core.data.util.isSystemReduceMotionEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException
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
        val AUTO_ROTATE_MODE = stringPreferencesKey("auto_rotate_mode")
        val STEP_MINUTES = intPreferencesKey("step_minutes")
        val PRESET_MINUTES = intPreferencesKey("preset_minutes")
        val LAUNCH_ANIMATION_ENABLED = booleanPreferencesKey("launch_animation_enabled")
        val WIDGET_USE_FIXED_DURATION = booleanPreferencesKey("widget_use_fixed_duration")
        val WIDGET_FIXED_MINUTES = intPreferencesKey("widget_fixed_minutes")
    }

    // Einmaliger Init-Scope fürs Seeding. IO-Dispatcher ist angemessen für DataStore-Zugriffe.
    private val initScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Seed-on-first-install: solange der Key noch nie geschrieben wurde, wird
        // launchAnimationEnabled einmalig aus der System-Reduce-Motion-Präferenz
        // abgeleitet. Key-Absenz statt separatem "seeded"-Flag: ein User-Toggle, das
        // zuerst landet, kann so nie überschrieben werden. Der Read vor dem edit spart
        // die Write-Transaktion bei jedem weiteren Prozess-Start. Spätere System-
        // Änderungen werden bewusst nicht reflektiert (siehe Spec, Out-of-Scope).
        initScope.launch {
            try {
                if (LAUNCH_ANIMATION_ENABLED !in dataStore.data.first()) {
                    dataStore.edit { prefs ->
                        if (LAUNCH_ANIMATION_ENABLED !in prefs) {
                            prefs[LAUNCH_ANIMATION_ENABLED] = !isSystemReduceMotionEnabled(context)
                        }
                    }
                }
            } catch (_: IOException) {
                // Seeding ist best-effort: ohne Seed greift der UserSettings-Default.
                // Ein I/O-Fehler darf den Prozess nicht crashen (Read-Pfad unten
                // behandelt IOException genauso).
            }
        }
    }

    override val settings: Flow<UserSettings> = dataStore.data
        .catch { error ->
            // A failed read must not crash collectors — SleepTimerService reads this
            // flow with runBlocking in onCreate. Fall back to defaults.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
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
            autoRotateMode = AutoRotateMode.fromStorage(prefs[AUTO_ROTATE_MODE]),
            stepMinutes = prefs[STEP_MINUTES] ?: d.stepMinutes,
            // Clamp on read too: values persisted before the cap changed must not
            // leak an out-of-range preset into the dial.
            presetMinutes = (prefs[PRESET_MINUTES] ?: d.presetMinutes).coerceIn(1, MAX_TIMER_MINUTES),
            launchAnimationEnabled = prefs[LAUNCH_ANIMATION_ENABLED] ?: d.launchAnimationEnabled,
            widgetUseFixedDuration = prefs[WIDGET_USE_FIXED_DURATION] ?: d.widgetUseFixedDuration,
            widgetFixedMinutes = (prefs[WIDGET_FIXED_MINUTES] ?: d.widgetFixedMinutes)
                .coerceIn(1, MAX_TIMER_MINUTES),
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

    override suspend fun updateAutoRotateMode(mode: AutoRotateMode) {
        dataStore.edit { it[AUTO_ROTATE_MODE] = mode.name }
    }

    override suspend fun updateStepMinutes(minutes: Int) {
        dataStore.edit { it[STEP_MINUTES] = minutes.coerceIn(1, 30) }
    }

    override suspend fun updatePresetMinutes(minutes: Int) {
        dataStore.edit { it[PRESET_MINUTES] = minutes.coerceIn(1, MAX_TIMER_MINUTES) }
    }

    override suspend fun updateLaunchAnimationEnabled(enabled: Boolean) {
        dataStore.edit { it[LAUNCH_ANIMATION_ENABLED] = enabled }
    }

    override suspend fun updateWidgetUseFixedDuration(enabled: Boolean) {
        dataStore.edit { it[WIDGET_USE_FIXED_DURATION] = enabled }
    }

    override suspend fun updateWidgetFixedMinutes(minutes: Int) {
        dataStore.edit { it[WIDGET_FIXED_MINUTES] = minutes.coerceIn(1, MAX_TIMER_MINUTES) }
    }
}
