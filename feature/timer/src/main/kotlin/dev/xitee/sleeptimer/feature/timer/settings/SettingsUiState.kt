package dev.xitee.sleeptimer.feature.timer.settings

import dev.xitee.sleeptimer.core.data.model.UserSettings
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuManager

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val shizukuState: ShizukuManager.State = ShizukuManager.State.NotInstalled,
)
