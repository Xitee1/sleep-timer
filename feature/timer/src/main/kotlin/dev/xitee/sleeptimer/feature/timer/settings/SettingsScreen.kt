package dev.xitee.sleeptimer.feature.timer.settings

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuManager
import dev.xitee.sleeptimer.feature.timer.R
import dev.xitee.sleeptimer.feature.timer.settings.components.FadeOutSlider
import dev.xitee.sleeptimer.feature.timer.settings.components.ScreenLockMethodDialog
import dev.xitee.sleeptimer.feature.timer.settings.components.SettingsToggleRow
import dev.xitee.sleeptimer.feature.timer.settings.components.SettingsTopBar
import dev.xitee.sleeptimer.feature.timer.settings.components.ShizukuRequiredDialog
import dev.xitee.sleeptimer.feature.timer.settings.components.StepMinutesSlider
import dev.xitee.sleeptimer.feature.timer.settings.components.ThemeRow
import dev.xitee.sleeptimer.feature.timer.theme.AppThemes
import dev.xitee.sleeptimer.feature.timer.theme.LocalAppTheme
import dev.xitee.sleeptimer.feature.timer.theme.appTheme
import dev.xitee.sleeptimer.feature.timer.theme.rememberAnimatedAppTheme
import dev.xitee.sleeptimer.feature.timer.timer.components.TimerBackground

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToThemePicker: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ready = uiState ?: return

    val animatedTheme = rememberAnimatedAppTheme(AppThemes.byId(ready.settings.theme))
    CompositionLocalProvider(LocalAppTheme provides animatedTheme) {
        SettingsContent(
            uiState = ready,
            onBack = onBack,
            onNavigateToThemePicker = onNavigateToThemePicker,
            onNavigateToAbout = onNavigateToAbout,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onNavigateToThemePicker: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val screenDescription = stringResource(R.string.screen_description)
    val shizukuSoftScreenOffExplanation =
        stringResource(R.string.shizuku_feature_soft_screen_off)
    val shizukuWifiExplanation = stringResource(R.string.shizuku_feature_wifi)
    val shizukuBluetoothExplanation = stringResource(R.string.shizuku_feature_bluetooth)

    val deviceAdminLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.updateScreenOff(true)
            viewModel.updateSoftScreenOff(false)
        }
    }

    var shizukuDialogExplanation by remember { mutableStateOf<String?>(null) }
    var pendingShizukuToggle by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showMethodDialog by remember { mutableStateOf(false) }

    fun requestWithShizuku(explanation: String, enableAction: () -> Unit) {
        if (viewModel.isShizukuReady()) {
            enableAction()
        } else {
            shizukuDialogExplanation = explanation
            pendingShizukuToggle = enableAction
        }
    }

    if (shizukuDialogExplanation != null) {
        ShizukuRequiredDialog(
            state = uiState.shizukuState,
            featureExplanation = shizukuDialogExplanation!!,
            onRequestPermission = { viewModel.requestShizukuPermission() },
            onDismiss = {
                shizukuDialogExplanation = null
                pendingShizukuToggle = null
            },
        )
    }

    if (showMethodDialog) {
        ScreenLockMethodDialog(
            onHardLockSelected = {
                if (viewModel.isDeviceAdminActive()) {
                    viewModel.updateScreenOff(true)
                    viewModel.updateSoftScreenOff(false)
                } else {
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(
                            DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                            viewModel.getAdminComponent(),
                        )
                        putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            screenDescription,
                        )
                    }
                    deviceAdminLauncher.launch(intent)
                }
            },
            onSoftLockSelected = {
                requestWithShizuku(shizukuSoftScreenOffExplanation) {
                    viewModel.updateScreenOff(true)
                    viewModel.updateSoftScreenOff(true)
                }
            },
            onDismiss = { showMethodDialog = false },
        )
    }

    // Auto-complete the pending toggle if Shizuku transitions to Ready while dialog is open.
    LaunchedEffect(uiState.shizukuState, pendingShizukuToggle) {
        if (pendingShizukuToggle != null && uiState.shizukuState == ShizukuManager.State.Ready) {
            pendingShizukuToggle?.invoke()
            pendingShizukuToggle = null
            shizukuDialogExplanation = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshShizuku()
    }

    // Device admin can be revoked from system Settings without any broadcast we're
    // subscribed to — re-query on resume so the "Screen" row description reflects
    // the current admin state if the user came back from Settings → Device admin.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshDeviceAdminState()
    }

    TimerBackground(
        animating = false,
        starsEnabled = uiState.settings.starsEnabled,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            SettingsTopBar(
                title = stringResource(R.string.settings_title),
                onBack = onBack,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                SectionHeader(stringResource(R.string.category_appearance))
                ThemeRow(
                    selected = uiState.settings.theme,
                    onClick = onNavigateToThemePicker,
                )
                SettingsToggleRow(
                    icon = Icons.Default.AutoAwesome,
                    title = stringResource(R.string.stars_title),
                    description = if (AppThemes.byId(uiState.settings.theme).allowStars) {
                        stringResource(R.string.stars_description)
                    } else {
                        stringResource(R.string.stars_unavailable)
                    },
                    checked = uiState.settings.starsEnabled,
                    onCheckedChange = { viewModel.updateStarsEnabled(it) },
                    enabled = AppThemes.byId(uiState.settings.theme).allowStars,
                )
                SettingsToggleRow(
                    icon = Icons.Default.RocketLaunch,
                    title = stringResource(R.string.launch_animation_title),
                    description = stringResource(R.string.launch_animation_description),
                    checked = uiState.settings.launchAnimationEnabled,
                    onCheckedChange = { viewModel.updateLaunchAnimationEnabled(it) },
                )

                SectionHeader(stringResource(R.string.category_sleep_timer))
                SettingsToggleRow(
                    icon = Icons.Default.MusicOff,
                    title = stringResource(R.string.playback_title),
                    description = stringResource(R.string.playback_description),
                    checked = uiState.settings.stopMediaPlayback,
                    onCheckedChange = { viewModel.updateStopMediaPlayback(it) },
                )
                FadeOutSlider(
                    durationSeconds = uiState.settings.fadeOutDurationSeconds,
                    onDurationChanged = { viewModel.updateFadeOutDuration(it) },
                )
                StepMinutesSlider(
                    stepMinutes = uiState.settings.stepMinutes,
                    onStepChanged = { viewModel.updateStepMinutes(it) },
                )

                Spacer(modifier = Modifier.height(4.dp))

                SettingsToggleRow(
                    icon = Icons.Default.PhoneAndroid,
                    title = stringResource(R.string.screen_title),
                    description = when {
                        !uiState.settings.screenOff -> stringResource(R.string.screen_description)
                        uiState.settings.softScreenOff -> stringResource(R.string.screen_method_active_soft)
                        else -> stringResource(R.string.screen_method_active_hard)
                    },
                    checked = uiState.settings.screenOff,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            showMethodDialog = true
                        } else {
                            viewModel.updateScreenOff(false)
                        }
                    },
                )

                SettingsToggleRow(
                    icon = Icons.Default.Wifi,
                    title = stringResource(R.string.wifi_off_title),
                    description = stringResource(R.string.wifi_off_description),
                    checked = uiState.settings.turnOffWifi,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            requestWithShizuku(shizukuWifiExplanation) {
                                viewModel.updateTurnOffWifi(true)
                            }
                        } else {
                            viewModel.updateTurnOffWifi(false)
                        }
                    },
                )

                SettingsToggleRow(
                    icon = Icons.Default.Bluetooth,
                    title = stringResource(R.string.bluetooth_off_title),
                    description = stringResource(R.string.bluetooth_off_description),
                    checked = uiState.settings.turnOffBluetooth,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            requestWithShizuku(shizukuBluetoothExplanation) {
                                viewModel.updateTurnOffBluetooth(true)
                            }
                        } else {
                            viewModel.updateTurnOffBluetooth(false)
                        }
                    },
                )

                SectionHeader(stringResource(R.string.category_haptic))
                SettingsToggleRow(
                    icon = Icons.Default.Vibration,
                    title = stringResource(R.string.haptic_title),
                    description = stringResource(R.string.haptic_description),
                    checked = uiState.settings.hapticFeedbackEnabled,
                    onCheckedChange = { viewModel.updateHapticFeedback(it) },
                )

                SectionHeader(stringResource(R.string.category_about))
                SettingsNavigationRow(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.about_entry_title),
                    description = stringResource(R.string.about_entry_description),
                    onClick = onNavigateToAbout,
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    val theme = appTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(theme.surface1),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = theme.accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = theme.textPrimary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = theme.textDim,
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    val theme = appTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = theme.textMuted,
        )
    }
}
