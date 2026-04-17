package dev.mato.sleeptimer.feature.timer.timer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mato.sleeptimer.feature.timer.R
import dev.mato.sleeptimer.feature.timer.timer.components.CircularDial
import dev.mato.sleeptimer.feature.timer.timer.components.PlayButton
import dev.mato.sleeptimer.feature.timer.timer.components.TimeDisplay
import dev.mato.sleeptimer.feature.timer.timer.components.rememberCircularDialState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val dialState = rememberCircularDialState()
    val context = LocalContext.current

    // Notification permission launcher for Android 13+
    // Timer starts after permission dialog resolves (regardless of result)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        viewModel.startTimer()
    }

    val isRunning = uiState is TimerUiState.Running || uiState is TimerUiState.FadingOut

    // Sync dial state with selected minutes when idle
    LaunchedEffect(uiState) {
        if (uiState is TimerUiState.Idle) {
            val idle = uiState as TimerUiState.Idle
            if (dialState.totalMinutes != idle.selectedMinutes) {
                dialState.setMinutes(idle.selectedMinutes)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Circular dial with time display overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularDial(
                    state = dialState,
                    isRunning = isRunning,
                    progress = when (val state = uiState) {
                        is TimerUiState.Running -> state.progress
                        else -> 0f
                    },
                    hapticEnabled = settings.hapticFeedbackEnabled,
                    onMinutesChanged = { viewModel.setMinutes(it) },
                    modifier = Modifier.fillMaxSize(),
                )

                // Time display in center of dial
                when (val state = uiState) {
                    is TimerUiState.Idle -> {
                        TimeDisplay(
                            minutes = state.selectedMinutes,
                            seconds = null,
                        )
                    }
                    is TimerUiState.Running -> {
                        TimeDisplay(
                            minutes = state.remainingMinutes,
                            seconds = state.remainingSeconds,
                        )
                    }
                    is TimerUiState.FadingOut -> {
                        TimeDisplay(
                            minutes = 0,
                            seconds = 0,
                        )
                    }
                }
            }

            // Play/Stop button
            PlayButton(
                isRunning = isRunning,
                onClick = {
                    if (isRunning) {
                        viewModel.stopTimer()
                    } else {
                        // Request notification permission if needed on Android 13+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // Timer starts in the launcher callback after permission resolves
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS,
                            )
                        } else {
                            viewModel.startTimer()
                        }
                    }
                },
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
