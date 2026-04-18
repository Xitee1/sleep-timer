package dev.xitee.sleeptimer.feature.timer.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.xitee.sleeptimer.core.data.model.ThemeId
import dev.xitee.sleeptimer.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val devicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(
        context,
        "dev.xitee.sleeptimer.receiver.SleepTimerDeviceAdminReceiver",
    )

    val uiState: StateFlow<SettingsUiState?> = settingsRepository.settings
        .map { settings ->
            SettingsUiState(
                settings = settings,
                isDeviceAdminEnabled = devicePolicyManager.isAdminActive(adminComponent),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun isDeviceAdminActive(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }

    fun getAdminComponent(): ComponentName = adminComponent

    fun updateStopMediaPlayback(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateStopMediaPlayback(enabled) }
    }

    fun updateFadeOutDuration(seconds: Int) {
        viewModelScope.launch { settingsRepository.updateFadeOutDuration(seconds) }
    }

    fun updateScreenOff(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateScreenOff(enabled) }
    }

    fun updateHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateHapticFeedback(enabled) }
    }

    fun updateTheme(theme: ThemeId) {
        viewModelScope.launch { settingsRepository.updateTheme(theme) }
    }

    fun updateStarsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateStarsEnabled(enabled) }
    }

    fun updateStepMinutes(minutes: Int) {
        viewModelScope.launch { settingsRepository.updateStepMinutes(minutes) }
    }
}
