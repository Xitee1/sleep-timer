package dev.xitee.sleeptimer.feature.timer.timer

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xitee.sleeptimer.core.data.util.remainingMillisToDisplayMinutes
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuManager
import dev.xitee.sleeptimer.feature.timer.R
import dev.xitee.sleeptimer.feature.timer.settings.components.DeviceAdminRequiredDialog
import dev.xitee.sleeptimer.feature.timer.settings.components.ShizukuRequiredDialog
import dev.xitee.sleeptimer.feature.timer.theme.AppThemes
import dev.xitee.sleeptimer.feature.timer.theme.LocalAppTheme
import dev.xitee.sleeptimer.feature.timer.theme.appTheme
import dev.xitee.sleeptimer.feature.timer.theme.rememberAnimatedAppTheme
import dev.xitee.sleeptimer.feature.timer.timer.components.CircularDial
import dev.xitee.sleeptimer.feature.timer.timer.components.PlayButton
import dev.xitee.sleeptimer.feature.timer.timer.components.SecondaryRoundButton
import dev.xitee.sleeptimer.feature.timer.timer.components.TimeDisplay
import dev.xitee.sleeptimer.feature.timer.timer.components.TimerBackground
import dev.xitee.sleeptimer.feature.timer.timer.components.rememberCircularDialState

private const val ROTATION_DURATION_MS = 350

@Composable
fun TimerScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val animatedTheme = rememberAnimatedAppTheme(AppThemes.byId(settings.theme))
    CompositionLocalProvider(LocalAppTheme provides animatedTheme) {
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

    val orientation by rememberDeviceOrientation()
    val isLandscape = orientation == DeviceOrientation.LANDSCAPE_LEFT ||
        orientation == DeviceOrientation.LANDSCAPE_RIGHT
    val animatedAngle = animatedRotationAngle(orientation)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        viewModel.startTimer()
    }

    var showAdminStartupDialog by remember { mutableStateOf(false) }
    var shizukuStartupFeatures by remember { mutableStateOf<List<ShizukuFeature>>(emptyList()) }
    val shizukuState by viewModel.shizukuState.collectAsStateWithLifecycle()

    val adminStartupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { _ ->
        showAdminStartupDialog = false
    }

    LaunchedEffect(Unit) {
        viewModel.computeStartupPermissionCheck()?.let { check ->
            showAdminStartupDialog = check.adminMissing
            shizukuStartupFeatures = check.shizukuMissingFeatures
        }
    }

    // Auto-close the Shizuku startup dialog once permission is granted.
    LaunchedEffect(shizukuState) {
        if (shizukuStartupFeatures.isNotEmpty() && shizukuState == ShizukuManager.State.Ready) {
            shizukuStartupFeatures = emptyList()
        }
    }

    if (showAdminStartupDialog) {
        DeviceAdminRequiredDialog(
            onRequestPermission = {
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
                adminStartupLauncher.launch(intent)
            },
            onDismiss = { showAdminStartupDialog = false },
        )
    }

    if (shizukuStartupFeatures.isNotEmpty()) {
        val explanations = shizukuStartupFeatures.map { feature ->
            when (feature) {
                ShizukuFeature.SCREEN_OFF -> stringResource(R.string.shizuku_feature_label_display)
                ShizukuFeature.WIFI -> stringResource(R.string.shizuku_feature_label_wifi)
                ShizukuFeature.BLUETOOTH -> stringResource(R.string.shizuku_feature_label_bluetooth)
            }
        }
        ShizukuRequiredDialog(
            state = shizukuState,
            featureExplanations = explanations,
            introText = stringResource(R.string.shizuku_startup_intro),
            onRequestPermission = { viewModel.requestShizukuPermission() },
            onDismiss = { shizukuStartupFeatures = emptyList() },
        )
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
        is TimerUiState.Running -> s.remainingMillis / 60_000f
        is TimerUiState.FadingOut -> 0f
        else -> 0f
    }

    val displayMinutes: Int = if (dialState.isDragging) {
        dialState.totalMinutes
    } else when (val s = uiState) {
        is TimerUiState.Idle -> s.selectedMinutes
        is TimerUiState.Running -> remainingMillisToDisplayMinutes(s.remainingMillis)
        is TimerUiState.FadingOut -> 0
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
            HomeTopBar(
                onOpenSettings = onNavigateToSettings,
                iconRotation = animatedAngle,
                showInlineTitle = !isLandscape,
            )

            val nowMillis by produceState(initialValue = System.currentTimeMillis()) {
                while (true) {
                    value = System.currentTimeMillis()
                    kotlinx.coroutines.delay(10_000L)
                }
            }
            val endMillis: Long? = if (uiState is TimerUiState.FadingOut || displayMinutes <= 0) {
                null
            } else {
                nowMillis + displayMinutes * 60_000L
            }
            val timeFormatter = remember(context) {
                android.text.format.DateFormat.getTimeFormat(context)
            }
            val endetText: String? = endMillis?.let {
                stringResource(R.string.ends_at_time, timeFormatter.format(java.util.Date(it)))
            }
            val endetTextStyle = MaterialTheme.typography.bodyLarge
            val textMeasurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val endetTextSize = remember(endetText, endetTextStyle, textMeasurer) {
                if (endetText.isNullOrEmpty()) {
                    androidx.compose.ui.unit.IntSize.Zero
                } else {
                    textMeasurer.measure(endetText, endetTextStyle).size
                }
            }
            val endetTextWidthDp = with(density) { endetTextSize.width.toDp() }
            val endetTextHeightDp = with(density) { endetTextSize.height.toDp() }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                val dialToEndetGap = 20.dp
                val maxDialSize = 360.dp

                // The rotated text's axis-aligned bounding box changes size with the
                // rotation angle. Driving the endet slot height from the same
                // `animatedAngle` that rotates the text guarantees the slot is always
                // exactly big enough to hold it — no frame during the transition can
                // see the text bleed into the dial above. When the flex area is
                // short, the dial shrinks to make room.
                val angleRad = animatedAngle * (Math.PI.toFloat() / 180f)
                val sinAbs = kotlin.math.abs(kotlin.math.sin(angleRad))
                val cosAbs = kotlin.math.abs(kotlin.math.cos(angleRad))
                val endetBoxHeight = (endetTextWidthDp * sinAbs + endetTextHeightDp * cosAbs)
                    .coerceAtLeast(24.dp)
                val availableForDialHeight = (maxHeight - dialToEndetGap - endetBoxHeight)
                    .coerceAtLeast(0.dp)
                val dialSize = minOf(maxWidth, availableForDialHeight, maxDialSize)

                // At landscape, place the slot's centre at the midpoint between the
                // dial's bottom and the flex area's bottom (= button row), so the
                // text reads as equidistant from dial and buttons instead of hugging
                // the dial. Since the Column natural-centres the (dial + gap + slot)
                // group, the slot's natural centre sits gap + slotHeight/2 below the
                // dial — derive the extra downward offset needed to land on the
                // midpoint. `sinAbs` eases the offset in alongside the rotation.
                val landscapeSlotOffset =
                    ((maxHeight - dialSize - dialToEndetGap * 3 - endetBoxHeight) / 4f)
                        .coerceAtLeast(0.dp)
                val slotOffset = landscapeSlotOffset * sinAbs

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(dialSize)
                            .graphicsLayer { rotationZ = animatedAngle },
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularDial(
                            state = dialState,
                            isRunning = isRunning,
                            runningMinutes = runningMinutes,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                            onMinutesChanged = viewModel::setMinutes,
                            onMinutesCommitted = viewModel::commitMinutes,
                            modifier = Modifier.fillMaxSize(),
                        )

                        TimeDisplay(totalMinutes = displayMinutes)
                    }

                    Spacer(modifier = Modifier.height(dialToEndetGap))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(endetBoxHeight)
                            .offset(y = slotOffset),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (endetText != null) {
                            Text(
                                text = endetText,
                                style = endetTextStyle,
                                color = appTheme().textDim,
                                modifier = Modifier.graphicsLayer { rotationZ = animatedAngle },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val runningRemainingSeconds: Int = when (val s = uiState) {
                is TimerUiState.Running -> (s.remainingMillis / 1000L).toInt()
                else -> 0
            }

            ActionRow(
                isRunning = isRunning,
                hapticEnabled = settings.hapticFeedbackEnabled,
                iconRotation = animatedAngle,
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
                        viewModel.commitMinutes(next)
                    }
                },
                onPlusStep = {
                    if (isRunning) {
                        viewModel.addStep()
                    } else {
                        val step = settings.stepMinutes
                        val current = dialState.totalMinutes
                        val next = (current / step + 1) * step
                        viewModel.commitMinutes(next.coerceAtMost(300))
                    }
                },
                isMinusEnabled = if (isRunning) {
                    runningRemainingSeconds > settings.stepMinutes * 60
                } else {
                    dialState.totalMinutes > 1
                },
                isPlusEnabled = !isRunning && dialState.totalMinutes < 300,
                plusStepVisibleWhileRunning = true,
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Landscape title: appears on the right edge, immediately below the settings
        // icon. The container is 44dp wide (matches settings icon) and right-padded
        // by 8dp (matches TopBar's horizontal padding), so the title's centre sits on
        // the same device x as the settings icon — i.e. inline with it from the user's
        // tilted perspective. wrapContentSize(unbounded = true) lets the text measure
        // its natural width instead of being clipped to the 44dp container.
        AnimatedVisibility(
            visible = isLandscape,
            enter = fadeIn(animationSpec = tween(ROTATION_DURATION_MS)),
            exit = fadeOut(animationSpec = tween(ROTATION_DURATION_MS)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(top = 56.dp, end = 8.dp)
                .width(44.dp)
                .height(160.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = appTheme().textPrimary,
                    softWrap = false,
                    modifier = Modifier
                        .wrapContentSize(unbounded = true)
                        .graphicsLayer { rotationZ = animatedAngle },
                )
            }
        }
    }
}

@Composable
private fun animatedRotationAngle(orientation: DeviceOrientation): Float {
    // Shortest-path guard: when the bucket jumps, pick the equivalent target (±360°)
    // closest to the previous value so the animation continues rotating rather than
    // reversing through 0°.
    val continuousTarget = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(orientation) {
        val raw = orientation.counterRotationDegrees()
        val last = continuousTarget.floatValue
        val candidates = listOf(raw, raw + 360f, raw - 360f)
        continuousTarget.floatValue = candidates.minBy { kotlin.math.abs(it - last) }
    }
    val animated by animateFloatAsState(
        targetValue = continuousTarget.floatValue,
        animationSpec = tween(ROTATION_DURATION_MS, easing = FastOutSlowInEasing),
        label = "rotationAngle",
    )
    return animated
}

@Composable
private fun HomeTopBar(
    onOpenSettings: () -> Unit,
    iconRotation: Float,
    showInlineTitle: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
    ) {
        AnimatedVisibility(
            visible = showInlineTitle,
            enter = fadeIn(animationSpec = tween(ROTATION_DURATION_MS)),
            exit = fadeOut(animationSpec = tween(ROTATION_DURATION_MS)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = appTheme().textPrimary,
                modifier = Modifier.graphicsLayer { rotationZ = iconRotation },
            )
        }
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
                modifier = Modifier.graphicsLayer { rotationZ = iconRotation },
            )
        }
    }
}

@Composable
private fun ActionRow(
    isRunning: Boolean,
    hapticEnabled: Boolean,
    iconRotation: Float,
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
            contentDescription = stringResource(R.string.cd_step_minus),
            onClick = onMinusStep,
            hapticEnabled = hapticEnabled,
            enabled = isMinusEnabled,
            iconRotation = iconRotation,
        )
        PlayButton(
            isRunning = isRunning,
            hapticEnabled = hapticEnabled,
            onClick = onToggle,
            iconRotation = iconRotation,
        )
        SecondaryRoundButton(
            icon = Icons.Default.Add,
            contentDescription = stringResource(R.string.cd_step_plus),
            onClick = onPlusStep,
            hapticEnabled = hapticEnabled,
            enabled = if (isRunning) plusStepVisibleWhileRunning else isPlusEnabled,
            iconRotation = iconRotation,
        )
    }
}
