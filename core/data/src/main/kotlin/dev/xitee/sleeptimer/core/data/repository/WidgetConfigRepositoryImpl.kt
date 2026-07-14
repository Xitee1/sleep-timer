package dev.xitee.sleeptimer.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import dev.xitee.sleeptimer.core.data.model.MAX_TIMER_MINUTES
import dev.xitee.sleeptimer.core.data.model.WidgetConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
                USE_FIXED_REGEX.matchEntire(key.name)?.let { match ->
                    val id = match.groupValues[1].toIntOrNull() ?: return@forEach
                    result[id] = (result[id] ?: WidgetConfig())
                        .copy(useFixedDuration = value as Boolean)
                }
                FIXED_MINUTES_REGEX.matchEntire(key.name)?.let { match ->
                    val id = match.groupValues[1].toIntOrNull() ?: return@forEach
                    result[id] = (result[id] ?: WidgetConfig())
                        .copy(fixedMinutes = (value as Int).coerceIn(1, MAX_TIMER_MINUTES))
                }
            }
            result
        }

    override suspend fun getConfig(appWidgetId: Int): WidgetConfig {
        val prefs = dataStore.data
            .catch { error ->
                if (error is IOException) emit(emptyPreferences()) else throw error
            }
            .first()
        val d = WidgetConfig()
        return WidgetConfig(
            useFixedDuration = prefs[useFixedKey(appWidgetId)] ?: d.useFixedDuration,
            fixedMinutes = (prefs[fixedMinutesKey(appWidgetId)] ?: d.fixedMinutes)
                .coerceIn(1, MAX_TIMER_MINUTES),
        )
    }

    override suspend fun setConfig(appWidgetId: Int, config: WidgetConfig) {
        dataStore.edit { prefs ->
            prefs[useFixedKey(appWidgetId)] = config.useFixedDuration
            prefs[fixedMinutesKey(appWidgetId)] = config.fixedMinutes.coerceIn(1, MAX_TIMER_MINUTES)
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
