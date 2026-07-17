package dev.xitee.sleeptimer.core.data.repository

import dev.xitee.sleeptimer.core.data.model.WidgetConfig
import kotlinx.coroutines.flow.Flow

/**
 * Per-widget-instance configuration, keyed by appWidgetId. Unlike [UserSettings]
 * (one value per setting), entries here come and go with the widgets themselves:
 * they are written by the widget configuration screen and must be removed via
 * [removeConfigs] when instances are deleted from the home screen — and moved via
 * [remapConfigs] when a backup/device-transfer restore reassigns their ids.
 */
interface WidgetConfigRepository {
    /** All stored configs by appWidgetId. Instances without an entry use [WidgetConfig] defaults. */
    val configs: Flow<Map<Int, WidgetConfig>>
    suspend fun getConfig(appWidgetId: Int): WidgetConfig
    suspend fun setConfig(appWidgetId: Int, config: WidgetConfig)
    suspend fun removeConfigs(appWidgetIds: List<Int>)

    /**
     * Moves each config entry from its old appWidgetId to the new one (map keyed
     * old id -> new id) in a single atomic write. Called from the provider's
     * `onRestored` after the host remaps ids on a backup/device-transfer restore, so
     * restored widgets keep their configuration and the old-id entries don't leak.
     */
    suspend fun remapConfigs(oldToNew: Map<Int, Int>)
}
