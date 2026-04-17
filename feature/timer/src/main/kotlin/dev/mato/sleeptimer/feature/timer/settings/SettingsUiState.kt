package dev.mato.sleeptimer.feature.timer.settings

import dev.mato.sleeptimer.core.data.model.UserSettings

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val isDeviceAdminEnabled: Boolean = false,
)
