package dev.xitee.sleeptimer.feature.timer.settings

import android.app.admin.DevicePolicyManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuManager
import dev.xitee.sleeptimer.feature.timer.R
import dev.xitee.sleeptimer.feature.timer.settings.components.FadeOutSlider
import dev.xitee.sleeptimer.feature.timer.settings.components.SettingsToggleRow
import dev.xitee.sleeptimer.feature.timer.settings.components.ShizukuRequiredDialog
import dev.xitee.sleeptimer.feature.timer.settings.components.StepMinutesSlider
import dev.xitee.sleeptimer.feature.timer.settings.components.ThemeSelector
import dev.xitee.sleeptimer.feature.timer.theme.AppThemes
import dev.xitee.sleeptimer.feature.timer.theme.LocalAppTheme
import dev.xitee.sleeptimer.feature.timer.theme.appTheme
import dev.xitee.sleeptimer.feature.timer.timer.components.TimerBackground

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ready = uiState ?: return

    CompositionLocalProvider(LocalAppTheme provides AppThemes.byId(ready.settings.theme)) {
        SettingsContent(
            uiState = ready,
            onBack = onBack,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current

    val deviceAdminLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        // after return, state refresh is driven by the flow
    }

    var shizukuDialogExplanation by remember { mutableStateOf<String?>(null) }
    var pendingShizukuToggle by remember { mutableStateOf<(() -> Unit)?>(null) }

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
            SettingsTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                SectionHeader(stringResource(R.string.category_appearance))
                ThemeSelector(
                    selected = uiState.settings.theme,
                    onSelect = { viewModel.updateTheme(it) },
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                    description = if (uiState.isDeviceAdminEnabled || uiState.settings.softScreenOff) {
                        stringResource(R.string.screen_description)
                    } else {
                        stringResource(R.string.screen_admin_required)
                    },
                    checked = uiState.settings.screenOff,
                    onCheckedChange = { enabled ->
                        if (enabled && !uiState.settings.softScreenOff && !viewModel.isDeviceAdminActive()) {
                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(
                                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                                    viewModel.getAdminComponent(),
                                )
                                putExtra(
                                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                    context.getString(R.string.screen_description),
                                )
                            }
                            deviceAdminLauncher.launch(intent)
                        } else {
                            viewModel.updateScreenOff(enabled)
                        }
                    },
                )

                // Soft screen-off (Shizuku) — only relevant if parent toggle is on.
                if (uiState.settings.screenOff) {
                    SettingsToggleRow(
                        icon = Icons.Default.Nightlight,
                        title = stringResource(R.string.soft_screen_off_title),
                        description = stringResource(R.string.soft_screen_off_description),
                        checked = uiState.settings.softScreenOff,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                requestWithShizuku(context.getString(R.string.shizuku_feature_soft_screen_off)) {
                                    viewModel.updateSoftScreenOff(true)
                                }
                            } else {
                                viewModel.updateSoftScreenOff(false)
                            }
                        },
                    )
                }

                SettingsToggleRow(
                    icon = Icons.Default.Wifi,
                    title = stringResource(R.string.wifi_off_title),
                    description = stringResource(R.string.wifi_off_description),
                    checked = uiState.settings.turnOffWifi,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            requestWithShizuku(context.getString(R.string.shizuku_feature_wifi)) {
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
                            requestWithShizuku(context.getString(R.string.shizuku_feature_bluetooth)) {
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

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    val theme = appTheme()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = theme.textPrimary,
            )
        }
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            color = theme.textPrimary,
            modifier = Modifier.align(Alignment.Center),
        )
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
