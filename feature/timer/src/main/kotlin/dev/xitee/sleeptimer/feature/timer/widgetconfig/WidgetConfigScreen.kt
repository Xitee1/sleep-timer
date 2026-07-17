package dev.xitee.sleeptimer.feature.timer.widgetconfig

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xitee.sleeptimer.feature.timer.R
import dev.xitee.sleeptimer.feature.timer.settings.components.SettingsToggleRow
import dev.xitee.sleeptimer.feature.timer.settings.components.SettingsTopBar
import dev.xitee.sleeptimer.feature.timer.theme.AppThemes
import dev.xitee.sleeptimer.feature.timer.theme.ProvideAppTheme
import dev.xitee.sleeptimer.feature.timer.theme.appTheme
import dev.xitee.sleeptimer.feature.timer.theme.rememberAnimatedAppTheme
import dev.xitee.sleeptimer.feature.timer.timer.components.CircularDial
import dev.xitee.sleeptimer.feature.timer.timer.components.TimeDisplay
import dev.xitee.sleeptimer.feature.timer.timer.components.TimerBackground
import dev.xitee.sleeptimer.feature.timer.timer.components.rememberCircularDialState

/**
 * Configuration screen for a single home-screen widget instance, hosted by the
 * widget config activity in :app. [onSaved] fires after the config has been
 * persisted and the widget redrawn; [onCancel] on back navigation (the host
 * keeps its RESULT_CANCELED default, so first-time placement is aborted).
 */
@Composable
fun WidgetConfigScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    viewModel: WidgetConfigViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    // Wait for both the stored settings (for the theme) and this widget's config to
    // load before drawing anything — otherwise the screen would flash the default
    // palette/values before the real ones arrive.
    val loadedSettings = settings ?: return
    val ready = config ?: return

    val dialState = rememberCircularDialState()
    // Seed the dial from the stored config (and no-op after commits, which write
    // the dial's own value back) — same one-way sync TimerScreen uses.
    LaunchedEffect(ready.fixedMinutes) {
        if (dialState.totalMinutes != ready.fixedMinutes) {
            dialState.setMinutes(ready.fixedMinutes)
        }
    }

    val animatedTheme = rememberAnimatedAppTheme(AppThemes.byId(loadedSettings.theme))
    ProvideAppTheme(animatedTheme) {
        val theme = appTheme()
        TimerBackground(
            animating = false,
            starsEnabled = loadedSettings.starsEnabled,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
            ) {
                SettingsTopBar(
                    title = stringResource(R.string.widget_config_title),
                    onBack = onCancel,
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = stringResource(R.string.widget_config_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.textDim,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )

                    SettingsToggleRow(
                        icon = Icons.Default.Widgets,
                        title = stringResource(R.string.widget_fixed_duration_title),
                        description = if (ready.useFixedDuration) {
                            stringResource(R.string.widget_fixed_duration_on)
                        } else {
                            stringResource(R.string.widget_fixed_duration_off)
                        },
                        checked = ready.useFixedDuration,
                        onCheckedChange = { viewModel.setUseFixedDuration(it) },
                    )
                    if (ready.useFixedDuration) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp, vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularDial(
                                state = dialState,
                                isRunning = false,
                                runningMinutes = 0f,
                                hapticEnabled = loadedSettings.hapticFeedbackEnabled,
                                // Live drag preview comes straight from dialState
                                // (TimeDisplay below); only the committed value is
                                // pushed into the config being edited.
                                onMinutesChanged = {},
                                onMinutesCommitted = { viewModel.setFixedMinutes(it) },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            TimeDisplay(totalMinutes = dialState.totalMinutes)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.save(onSaved) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.accent,
                            contentColor = theme.accentInk,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    ) {
                        Text(text = stringResource(R.string.widget_config_save))
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
