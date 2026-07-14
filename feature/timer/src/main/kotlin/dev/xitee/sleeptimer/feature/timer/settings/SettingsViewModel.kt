package dev.xitee.sleeptimer.feature.timer.settings

import android.content.ComponentName
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.xitee.sleeptimer.core.data.model.AutoRotateMode
import dev.xitee.sleeptimer.core.data.model.ScreenLockMethod
import dev.xitee.sleeptimer.core.data.model.ThemeId
import dev.xitee.sleeptimer.core.data.repository.SettingsRepository
import dev.xitee.sleeptimer.core.service.screen.AccessibilityLockHelper
import dev.xitee.sleeptimer.core.service.screen.ScreenLockHelper
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
    private val screenLockHelper: ScreenLockHelper,
    private val accessibilityLockHelper: AccessibilityLockHelper,
) : ViewModel() {

    // Tick to re-query isAdminActive / isServiceEnabled. Both grants can be revoked
    // from system Settings without any callback into the app, so nothing else drives
    // a refresh. Bumped from SettingsScreen on ON_RESUME so returning from the system
    // settings reflects the current state.
    private val permissionRefreshTicker = MutableStateFlow(0)

    val uiState: StateFlow<SettingsUiState?> =
        combine(
            settingsRepository.settings,
            shizukuManager.state,
            permissionRefreshTicker,
        ) { settings, shizukuState, _ ->
            SettingsUiState(
                settings = settings,
                shizukuState = shizukuState,
                isDeviceAdminActive = screenLockHelper.isAdminActive(),
                isAccessibilityLockEnabled = accessibilityLockHelper.isServiceEnabled(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun isDeviceAdminActive(): Boolean = screenLockHelper.isAdminActive()

    fun isAccessibilityLockEnabled(): Boolean = accessibilityLockHelper.isServiceEnabled()

    fun getAdminComponent(): ComponentName = screenLockHelper.adminComponent

    fun refreshShizuku() = shizukuManager.refresh()

    /** Triggers a re-read of the device-admin and accessibility-service grants. */
    fun refreshPermissionState() {
        permissionRefreshTicker.value = permissionRefreshTicker.value + 1
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

    fun updateScreenLockMethod(method: ScreenLockMethod) {
        viewModelScope.launch { settingsRepository.updateScreenLockMethod(method) }
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

    fun updateAutoRotateMode(mode: AutoRotateMode) {
        viewModelScope.launch { settingsRepository.updateAutoRotateMode(mode) }
    }

    fun updateStepMinutes(minutes: Int) {
        viewModelScope.launch { settingsRepository.updateStepMinutes(minutes) }
    }

    fun updateLaunchAnimationEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateLaunchAnimationEnabled(enabled) }
    }
}
