package dev.xitee.sleeptimer.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import dev.xitee.sleeptimer.core.data.model.WidgetConfig
import dev.xitee.sleeptimer.core.data.model.clampFixedWidgetMinutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetConfigRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : WidgetConfigRepository {

    private companion object {
        // Keys are dynamic (one pair per placed widget), so the regexes are the
        // inverse mapping used to rebuild the id->config map from the store.
        fun useFixedKey(appWidgetId: Int) =
            booleanPreferencesKey("widget_${appWidgetId}_use_fixed_duration")

        fun fixedMinutesKey(appWidgetId: Int) =
            intPreferencesKey("widget_${appWidgetId}_fixed_minutes")

        val USE_FIXED_REGEX = Regex("^widget_(\\d+)_use_fixed_duration$")
        val FIXED_MINUTES_REGEX = Regex("^widget_(\\d+)_fixed_minutes$")
    }

    override val configs: Flow<Map<Int, WidgetConfig>> = dataStore.data
        .catch { error ->
            // Same degrade-to-defaults contract as SettingsRepositoryImpl: a failed
            // read must never crash a collector.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            val result = mutableMapOf<Int, WidgetConfig>()
            prefs.asMap().forEach { (key, value) ->
                // Safe casts (not `as`): a wrong-typed value skips that key instead of
                // throwing a ClassCastException here, which — running after the .catch
                // above — would escape the degrade-to-defaults contract and kill the
                // collector.
                USE_FIXED_REGEX.matchEntire(key.name)?.let { match ->
                    val id = match.groupValues[1].toIntOrNull() ?: return@let
                    val enabled = value as? Boolean ?: return@let
                    result[id] = (result[id] ?: WidgetConfig())
                        .copy(useFixedDuration = enabled)
                }
                FIXED_MINUTES_REGEX.matchEntire(key.name)?.let { match ->
                    val id = match.groupValues[1].toIntOrNull() ?: return@let
                    val minutes = value as? Int ?: return@let
                    result[id] = (result[id] ?: WidgetConfig())
                        .copy(fixedMinutes = clampFixedWidgetMinutes(minutes))
                }
            }
            result
        }
        // The DataStore file is shared with UserSettings, so `data` re-emits on every
        // unrelated settings write; only propagate downstream when the parsed configs
        // actually change, sparing the always-on TimerWidgetUpdater a redundant re-render.
        .distinctUntilChanged()

    override suspend fun getConfig(appWidgetId: Int): WidgetConfig {
        val prefs = dataStore.data
            .catch { error ->
                if (error is IOException) emit(emptyPreferences()) else throw error
            }
            .first()
        val d = WidgetConfig()
        return WidgetConfig(
            useFixedDuration = prefs[useFixedKey(appWidgetId)] ?: d.useFixedDuration,
            fixedMinutes = clampFixedWidgetMinutes(prefs[fixedMinutesKey(appWidgetId)] ?: d.fixedMinutes),
        )
    }

    override suspend fun setConfig(appWidgetId: Int, config: WidgetConfig) {
        dataStore.edit { prefs ->
            prefs[useFixedKey(appWidgetId)] = config.useFixedDuration
            prefs[fixedMinutesKey(appWidgetId)] = clampFixedWidgetMinutes(config.fixedMinutes)
        }
    }

    override suspend fun removeConfigs(appWidgetIds: List<Int>) {
        if (appWidgetIds.isEmpty()) return
        dataStore.edit { prefs ->
            appWidgetIds.forEach { id ->
                prefs.remove(useFixedKey(id))
                prefs.remove(fixedMinutesKey(id))
            }
        }
    }
}
