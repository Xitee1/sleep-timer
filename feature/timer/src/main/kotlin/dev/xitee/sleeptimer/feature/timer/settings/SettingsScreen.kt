package dev.xitee.sleeptimer.feature.timer.settings

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuManager
import dev.xitee.sleeptimer.core.data.model.AutoRotateMode
import dev.xitee.sleeptimer.core.data.model.ScreenLockMethod
import dev.xitee.sleeptimer.core.data.model.TrackEndMethod
import dev.xitee.sleeptimer.feature.timer.R
import dev.xitee.sleeptimer.feature.timer.settings.components.AccessibilityRequiredDialog
import dev.xitee.sleeptimer.feature.timer.settings.components.AutoRotateModeDialog
import dev.xitee.sleeptimer.feature.timer.settings.components.FadeOutSlider
import dev.xitee.sleeptimer.feature.timer.settings.components.NotificationAccessRequiredDialog
import dev.xitee.sleeptimer.feature.timer.settings.components.ScreenLockMethodDialog
import dev.xitee.sleeptimer.feature.timer.settings.components.labelRes
import dev.xitee.sleeptimer.feature.timer.settings.components.SettingsToggleRow
import dev.xitee.sleeptimer.feature.timer.settings.components.SettingsTopBar
import dev.xitee.sleeptimer.feature.timer.settings.components.ShizukuRequiredDialog
import dev.xitee.sleeptimer.feature.timer.settings.components.StepMinutesSlider
import dev.xitee.sleeptimer.feature.timer.settings.components.ThemeRow
import dev.xitee.sleeptimer.feature.timer.settings.components.TrackEndMethodDialog
import dev.xitee.sleeptimer.feature.timer.settings.components.TrackEndSlider
import dev.xitee.sleeptimer.feature.timer.theme.AppThemes
import dev.xitee.sleeptimer.feature.timer.theme.ProvideAppTheme
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
    ProvideAppTheme(animatedTheme) {
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
    val shizukuMobileDataExplanation = stringResource(R.string.shizuku_feature_mobile_data)
    val shizukuTrackEndExplanation = stringResource(R.string.shizuku_feature_track_end)

    val context = LocalContext.current

    val deviceAdminLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.updateScreenOff(true)
            viewModel.updateScreenLockMethod(ScreenLockMethod.DeviceAdmin)
        }
    }

    var shizukuDialogExplanation by remember { mutableStateOf<String?>(null) }
    var pendingShizukuToggle by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showMethodDialog by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    // Set when the user was sent to the accessibility settings to enable the lock
    // service; resolved on ON_RESUME once we can re-query the grant.
    var pendingAccessibilityEnable by remember { mutableStateOf(false) }
    var showAutoRotateDialog by remember { mutableStateOf(false) }
    var showTrackEndMethodDialog by remember { mutableStateOf(false) }
    var showNotificationAccessDialog by remember { mutableStateOf(false) }
    // Same ON_RESUME hand-off as pendingAccessibilityEnable, for notification access.
    var pendingNotificationAccessEnable by remember { mutableStateOf(false) }

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
            onDeviceAdminSelected = {
                if (viewModel.isDeviceAdminActive()) {
                    viewModel.updateScreenOff(true)
                    viewModel.updateScreenLockMethod(ScreenLockMethod.DeviceAdmin)
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
            onAccessibilitySelected = {
                if (viewModel.isAccessibilityLockEnabled()) {
                    viewModel.updateScreenOff(true)
                    viewModel.updateScreenLockMethod(ScreenLockMethod.Accessibility)
                } else {
                    showAccessibilityDialog = true
                }
            },
            onShizukuSelected = {
                requestWithShizuku(shizukuSoftScreenOffExplanation) {
                    viewModel.updateScreenOff(true)
                    viewModel.updateScreenLockMethod(ScreenLockMethod.Shizuku)
                }
            },
            onDismiss = { showMethodDialog = false },
        )
    }

    if (showAccessibilityDialog) {
        AccessibilityRequiredDialog(
            onOpenSettings = {
                // No activity result to wait for — the grant is re-checked on ON_RESUME.
                pendingAccessibilityEnable = true
                showAccessibilityDialog = false
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            onDismiss = { showAccessibilityDialog = false },
        )
    }

    if (showTrackEndMethodDialog) {
        TrackEndMethodDialog(
            selected = uiState.settings.trackEndMethod,
            showDisableHint = uiState.settings.trackEndMethod != TrackEndMethod.None,
            onNotificationAccessSelected = {
                if (viewModel.isNotificationAccessGranted()) {
                    viewModel.updateTrackEndMethod(TrackEndMethod.NotificationAccess)
                } else {
                    showNotificationAccessDialog = true
                }
            },
            onShizukuSelected = {
                requestWithShizuku(shizukuTrackEndExplanation) {
                    viewModel.updateTrackEndMethod(TrackEndMethod.Shizuku)
                }
            },
            onDismiss = { showTrackEndMethodDialog = false },
        )
    }

    if (showNotificationAccessDialog) {
        NotificationAccessRequiredDialog(
            onOpenSettings = {
                // No activity result to wait for — the grant is re-checked on ON_RESUME.
                pendingNotificationAccessEnable = true
                showNotificationAccessDialog = false
                openNotificationListenerSettings(context, viewModel.getNotificationListenerComponent())
            },
            onDismiss = { showNotificationAccessDialog = false },
        )
    }

    if (showAutoRotateDialog) {
        AutoRotateModeDialog(
            selected = uiState.settings.autoRotateMode,
            onSelect = { viewModel.updateAutoRotateMode(it) },
            onDismiss = { showAutoRotateDialog = false },
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

    // Device admin and the accessibility service can be revoked from system Settings
    // without any broadcast we're subscribed to — re-query on resume so the "Screen"
    // row description reflects the current state when the user comes back. This is
    // also where a pending accessibility-method selection completes: if the user
    // enabled the service while away, commit the choice they made in the dialog.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermissionState()
        if (pendingAccessibilityEnable) {
            pendingAccessibilityEnable = false
            if (viewModel.isAccessibilityLockEnabled()) {
                viewModel.updateScreenOff(true)
                viewModel.updateScreenLockMethod(ScreenLockMethod.Accessibility)
            }
        }
        if (pendingNotificationAccessEnable) {
            pendingNotificationAccessEnable = false
            if (viewModel.isNotificationAccessGranted()) {
                viewModel.updateTrackEndMethod(TrackEndMethod.NotificationAccess)
            }
        }
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
                SettingsNavigationRow(
                    icon = Icons.Default.ScreenRotation,
                    title = stringResource(R.string.auto_rotate_title),
                    description = stringResource(uiState.settings.autoRotateMode.labelRes),
                    onClick = { showAutoRotateDialog = true },
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
                val trackEndMethod = uiState.settings.trackEndMethod
                val trackEndAvailable = when (trackEndMethod) {
                    TrackEndMethod.None -> false
                    TrackEndMethod.NotificationAccess -> uiState.isNotificationAccessGranted
                    TrackEndMethod.Shizuku -> uiState.shizukuState == ShizukuManager.State.Ready
                }
                TrackEndSlider(
                    maxWaitMinutes = uiState.settings.trackEndMaxWaitMinutes,
                    available = trackEndAvailable,
                    description = stringResource(
                        when {
                            trackEndAvailable -> R.string.track_end_hold_to_configure
                            trackEndMethod == TrackEndMethod.NotificationAccess ->
                                R.string.track_end_notification_access_missing
                            trackEndMethod == TrackEndMethod.Shizuku ->
                                R.string.track_end_shizuku_missing
                            else -> R.string.track_end_choose_method
                        },
                    ),
                    onMaxWaitChanged = { viewModel.updateTrackEndMaxWaitMinutes(it) },
                    onChooseMethod = { showTrackEndMethodDialog = true },
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
                        uiState.settings.screenLockMethod == ScreenLockMethod.Shizuku ->
                            stringResource(R.string.screen_method_active_soft)
                        uiState.settings.screenLockMethod == ScreenLockMethod.Accessibility ->
                            if (uiState.isAccessibilityLockEnabled) {
                                stringResource(R.string.screen_method_active_accessibility)
                            } else {
                                // Service was disabled in system settings after being
                                // chosen — locking falls back to the hard lock (if
                                // granted) until re-enabled.
                                stringResource(R.string.screen_method_accessibility_revoked)
                            }
                        uiState.isDeviceAdminActive -> stringResource(R.string.screen_method_active_hard)
                        // Hard-lock is configured but the admin grant was revoked in
                        // system settings — locking will silently no-op until re-granted.
                        else -> stringResource(R.string.screen_method_hard_revoked)
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

                SettingsToggleRow(
                    icon = Icons.Default.SignalCellularAlt,
                    title = stringResource(R.string.mobile_data_off_title),
                    description = stringResource(R.string.mobile_data_off_description),
                    checked = uiState.settings.turnOffMobileData,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            requestWithShizuku(shizukuMobileDataExplanation) {
                                viewModel.updateTurnOffMobileData(true)
                            }
                        } else {
                            viewModel.updateTurnOffMobileData(false)
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

/**
 * Opens the notification-access page — directly on our listener's toggle where the
 * platform supports it (API 30+), otherwise (or if an OEM lacks that screen) the list.
 */
private fun openNotificationListenerSettings(context: Context, listener: ComponentName) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                listener.flattenToString(),
            )
        try {
            context.startActivity(detail)
            return
        } catch (_: ActivityNotFoundException) {
            // Fall through to the generic list below.
        }
    }
    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
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
