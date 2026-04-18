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
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val shizukuManager: ShizukuManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val devicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(
        context,
        "dev.xitee.sleeptimer.receiver.SleepTimerDeviceAdminReceiver",
    )

    // Tick to re-query isAdminActive. Admin grants can be revoked from system Settings
    // without any callback into the app, so nothing else drives a refresh. Bumped from
    // SettingsScreen on ON_RESUME so returning from Settings → Security → Device admin
    // reflects the current state.
    private val adminRefreshTicker = MutableStateFlow(0)

    val uiState: StateFlow<SettingsUiState?> =
        combine(
            settingsRepository.settings,
            shizukuManager.state,
            adminRefreshTicker,
        ) { settings, shizukuState, _ ->
            SettingsUiState(
                settings = settings,
                isDeviceAdminEnabled = devicePolicyManager.isAdminActive(adminComponent),
                shizukuState = shizukuState,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun isDeviceAdminActive(): Boolean =
        devicePolicyManager.isAdminActive(adminComponent)

    fun getAdminComponent(): ComponentName = adminComponent

    fun refreshShizuku() = shizukuManager.refresh()

    /** Triggers a re-read of the device-admin active flag. */
    fun refreshDeviceAdminState() {
        adminRefreshTicker.value = adminRefreshTicker.value + 1
    }
    fun requestShizukuPermission() = shizukuManager.requestPermission()
    fun isShizukuReady(): Boolean = shizukuManager.isReady()

    fun updateStopMediaPlayback(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateStopMediaPlayback(enabled) }
    }

    fun updateFadeOutDuration(seconds: Int) {
        viewModelScope.launch { settingsRepository.updateFadeOutDuration(seconds) }
    }

    fun updateScreenOff(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateScreenOff(enabled) }
    }

    fun updateSoftScreenOff(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSoftScreenOff(enabled) }
    }

    fun updateTurnOffWifi(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateTurnOffWifi(enabled) }
    }

    fun updateTurnOffBluetooth(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateTurnOffBluetooth(enabled) }
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
