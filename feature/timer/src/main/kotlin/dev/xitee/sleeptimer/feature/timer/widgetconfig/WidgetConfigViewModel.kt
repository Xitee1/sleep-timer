package dev.xitee.sleeptimer.feature.timer.widgetconfig

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.xitee.sleeptimer.core.data.model.UserSettings
import dev.xitee.sleeptimer.core.data.model.WidgetConfig
import dev.xitee.sleeptimer.core.data.repository.SettingsRepository
import dev.xitee.sleeptimer.core.data.repository.WidgetConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WidgetConfigViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val widgetConfigRepository: WidgetConfigRepository,
    settingsRepository: SettingsRepository,
    @WidgetProviderComponent private val widgetProviderComponent: ComponentName,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // The host activity's intent extras are this ViewModel's default arguments.
    val appWidgetId: Int =
        savedStateHandle[AppWidgetManager.EXTRA_APPWIDGET_ID]
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

    /** Only used to theme the config screen like the rest of the app. */
    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    // null until the stored config (or its defaults) has loaded — the screen
    // renders nothing before that instead of flashing default values.
    private val _config = MutableStateFlow<WidgetConfig?>(null)
    val config: StateFlow<WidgetConfig?> = _config.asStateFlow()

    init {
        viewModelScope.launch {
            _config.value = widgetConfigRepository.getConfig(appWidgetId)
        }
    }

    fun setUseFixedDuration(enabled: Boolean) {
        _config.update { it?.copy(useFixedDuration = enabled) }
    }

    fun setFixedMinutes(minutes: Int) {
        _config.update { it?.copy(fixedMinutes = minutes) }
    }

    fun save(onSaved: () -> Unit) {
        val config = _config.value ?: return
        viewModelScope.launch {
            widgetConfigRepository.setConfig(appWidgetId, config)
            // With a configure activity declared, the host skips the initial
            // APPWIDGET_UPDATE on placement — request this instance's first draw
            // explicitly. (Also covers a no-op save: DataStore dedupes unchanged
            // writes, so the live updater would not re-render on its own.)
            context.sendBroadcast(
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                    component = widgetProviderComponent
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                },
            )
            onSaved()
        }
    }
}
