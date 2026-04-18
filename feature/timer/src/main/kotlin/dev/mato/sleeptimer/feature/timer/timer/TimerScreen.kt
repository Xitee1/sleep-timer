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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import dev.mato.sleeptimer.feature.timer.theme.AppThemes
import dev.mato.sleeptimer.feature.timer.theme.LocalAppTheme
import dev.mato.sleeptimer.feature.timer.theme.appTheme
import dev.mato.sleeptimer.feature.timer.timer.components.CircularDial
import dev.mato.sleeptimer.feature.timer.timer.components.PlayButton
import dev.mato.sleeptimer.feature.timer.timer.components.SecondaryRoundButton
import dev.mato.sleeptimer.feature.timer.timer.components.TimeDisplay
import dev.mato.sleeptimer.feature.timer.timer.components.TimerBackground
import dev.mato.sleeptimer.feature.timer.timer.components.rememberCircularDialState

@Composable
fun TimerScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalAppTheme provides AppThemes.byId(settings.theme)) {
        TimerContent(
            onNavigateToSettings = onNavigateToSettings,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun TimerContent(
    onNavigateToSettings: () -> Unit,
    viewModel: TimerViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val dialState = rememberCircularDialState()
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        viewModel.startTimer()
    }

    val isRunning = uiState is TimerUiState.Running || uiState is TimerUiState.FadingOut

    LaunchedEffect(uiState) {
        if (uiState is TimerUiState.Idle) {
            val idle = uiState as TimerUiState.Idle
            if (dialState.totalMinutes != idle.selectedMinutes) {
                dialState.setMinutes(idle.selectedMinutes)
            }
        }
    }

    val runningMinutes: Float = when (val s = uiState) {
        is TimerUiState.Running -> s.remainingMinutes + s.remainingSeconds / 60f
        is TimerUiState.FadingOut -> 0f
        else -> 0f
    }

    TimerBackground(
        animating = isRunning,
        starsEnabled = settings.starsEnabled,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HomeTopBar(onOpenSettings = onNavigateToSettings)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 56.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularDial(
                    state = dialState,
                    isRunning = isRunning,
                    runningMinutes = runningMinutes,
                    hapticEnabled = settings.hapticFeedbackEnabled,
                    onMinutesChanged = { viewModel.setMinutes(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .sizeIn(maxWidth = 320.dp, maxHeight = 320.dp),
                )

                when (val s = uiState) {
                    is TimerUiState.Idle -> TimeDisplay(totalMinutes = s.selectedMinutes)
                    is TimerUiState.Running -> TimeDisplay(
                        totalMinutes = s.remainingMinutes + if (s.remainingSeconds > 0) 1 else 0,
                    )
                    is TimerUiState.FadingOut -> TimeDisplay(totalMinutes = 0)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val runningRemainingSeconds: Int = when (val s = uiState) {
                is TimerUiState.Running -> s.remainingMinutes * 60 + s.remainingSeconds
                else -> 0
            }

            ActionRow(
                isRunning = isRunning,
                onToggle = {
                    if (isRunning) {
                        viewModel.stopTimer()
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.startTimer()
                    }
                },
                onMinusStep = {
                    if (isRunning) {
                        viewModel.subtractStep()
                    } else {
                        val step = settings.stepMinutes
                        val current = dialState.totalMinutes
                        val next = ((current - 1).coerceAtLeast(0) / step) * step
                        viewModel.setMinutes(next)
                    }
                },
                onPlusStep = {
                    if (isRunning) {
                        viewModel.addStep()
                    } else {
                        val step = settings.stepMinutes
                        val current = dialState.totalMinutes
                        val next = (current / step + 1) * step
                        viewModel.setMinutes(next.coerceAtMost(300))
                    }
                },
                isMinusEnabled = if (isRunning) {
                    runningRemainingSeconds > settings.stepMinutes * 60
                } else {
                    dialState.totalMinutes > 0
                },
                isPlusEnabled = !isRunning && dialState.totalMinutes < 300,
                plusStepVisibleWhileRunning = true,
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HomeTopBar(onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = appTheme().textPrimary,
            modifier = Modifier.align(Alignment.Center),
        )
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(44.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = appTheme().textPrimary,
            )
        }
    }
}

@Composable
private fun ActionRow(
    isRunning: Boolean,
    onToggle: () -> Unit,
    onMinusStep: () -> Unit,
    onPlusStep: () -> Unit,
    isMinusEnabled: Boolean,
    isPlusEnabled: Boolean,
    plusStepVisibleWhileRunning: Boolean,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SecondaryRoundButton(
            icon = Icons.Default.Remove,
            contentDescription = "Minus",
            onClick = onMinusStep,
            enabled = isMinusEnabled,
        )
        PlayButton(isRunning = isRunning, onClick = onToggle)
        SecondaryRoundButton(
            icon = Icons.Default.Add,
            contentDescription = "Plus",
            onClick = onPlusStep,
            enabled = if (isRunning) plusStepVisibleWhileRunning else isPlusEnabled,
        )
    }
}
