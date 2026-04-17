package dev.mato.sleeptimer.feature.timer.settings

import android.app.admin.DevicePolicyManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mato.sleeptimer.feature.timer.R
import dev.mato.sleeptimer.feature.timer.settings.components.FadeOutSlider
import dev.mato.sleeptimer.feature.timer.settings.components.SettingsToggleRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val deviceAdminLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        // After returning from device admin enrollment, the state will update
        // via the uiState flow when it re-checks isAdminActive
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            // Category: Sleep Timer
            CategoryHeader(text = stringResource(R.string.category_sleep_timer))

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
                modifier = Modifier.padding(start = 40.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleRow(
                icon = Icons.Default.PhoneAndroid,
                title = stringResource(R.string.screen_title),
                description = if (uiState.isDeviceAdminEnabled) {
                    stringResource(R.string.screen_description)
                } else {
                    stringResource(R.string.screen_admin_required)
                },
                checked = uiState.settings.screenOff,
                onCheckedChange = { enabled ->
                    if (enabled && !viewModel.isDeviceAdminActive()) {
                        // Launch device admin enrollment
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

            // Category: Notification
            CategoryHeader(text = stringResource(R.string.category_notification))

            SettingsToggleRow(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.notification_title),
                description = stringResource(R.string.notification_description),
                checked = uiState.settings.notificationEnabled,
                onCheckedChange = { viewModel.updateNotificationEnabled(it) },
            )

            // Category: Haptic Feedback
            CategoryHeader(text = stringResource(R.string.category_haptic))

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

@Composable
private fun CategoryHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 56.dp, top = 24.dp, bottom = 8.dp),
    )
}
