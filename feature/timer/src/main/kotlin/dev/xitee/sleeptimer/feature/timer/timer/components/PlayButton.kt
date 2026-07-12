package dev.xitee.sleeptimer.feature.timer.timer.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.xitee.sleeptimer.feature.timer.R
import dev.xitee.sleeptimer.feature.timer.theme.appTheme

/**
 * Der Play/Stop-Button. Der Button zeichnet sein Icon selbst (inkl. Orientierungs-
 * Rotation und Play↔Stop-Crossfade). Nur während die Launch-Animation das Icon
 * fliegt, wird das Play-Icon hier per Alpha versteckt ([hidePlayIcon]) und vom
 * `LaunchOverlay` an seiner Stelle gerendert.
 *
 * @param isRunning Visueller Zustand (Stop-Icon + Shape-Morph). Läuft die Launch-
 *                  Animation, bleibt das bewusst false, bis sie fertig ist.
 * @param describesRunning Semantischer Zustand für Accessibility: was ein Tap JETZT
 *                         tut. Während der Animation stoppt ein Tap den bereits
 *                         laufenden Timer, obwohl visuell noch kein Stop-Icon zu
 *                         sehen ist — die Ansage muss der Aktion folgen.
 * @param buttonScale Als Lambda, gelesen im graphicsLayer-Block: Animations-Frames
 *                    sind so reine Layer-Updates ohne Recomposition.
 */
@Composable
fun PlayButton(
    isRunning: Boolean,
    hapticEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRotation: Float = 0f,
    buttonScale: () -> Float = { 1f },
    hidePlayIcon: Boolean = false,
    describesRunning: Boolean = isRunning,
) {
    val theme = appTheme()
    val view = LocalView.current

    val morph by animateFloatAsState(
        targetValue = if (isRunning) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "playMorph",
    )
    // 0 = circle (50% corners), 1 = rounded square (~28% corners)
    val cornerPercent = (50f - 22f * morph).toInt().coerceIn(28, 50)
    val shape = RoundedCornerShape(percent = cornerPercent)

    val shadowModifier = if (theme.hasGradient) {
        Modifier.shadow(
            elevation = 20.dp,
            shape = shape,
            ambientColor = theme.accent,
            spotColor = theme.accent,
        )
    } else {
        Modifier
    }

    val description = stringResource(
        if (describesRunning) R.string.stop_timer else R.string.start_timer,
    )

    Box(
        modifier = modifier
            .size(84.dp)
            .graphicsLayer {
                val scale = buttonScale()
                scaleX = scale
                scaleY = scale
            }
            .then(shadowModifier)
            .clip(shape)
            .clickable {
                if (hapticEnabled) {
                    view.performHapticFeedback(playStopHaptic)
                }
                onClick()
            }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(84.dp)) {
            val cornerRadiusPx = (cornerPercent / 100f) * size.minDimension
            val corner = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            drawRoundRect(
                color = theme.accent,
                cornerRadius = corner,
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (theme.isDark) 0.45f else 0.30f),
                    1f to Color.White.copy(alpha = 0f),
                    startY = 0f,
                    endY = size.height * 0.55f,
                ),
                cornerRadius = corner,
            )
        }
        Crossfade(
            targetState = isRunning,
            animationSpec = tween(durationMillis = 180),
            label = "playIcon",
        ) { running ->
            val icon: ImageVector = if (running) Icons.Default.Stop else Icons.Default.PlayArrow
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = theme.accentInk,
                modifier = Modifier
                    .size(34.dp)
                    .graphicsLayer {
                        rotationZ = iconRotation
                        // Während des Fluges rendert das LaunchOverlay das Play-Icon.
                        alpha = if (!running && hidePlayIcon) 0f else 1f
                    },
            )
        }
    }
}

@Composable
fun SecondaryRoundButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    hapticEnabled: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconRotation: Float = 0f,
) {
    val theme = appTheme()
    val view = LocalView.current
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled) {
                if (hapticEnabled) {
                    view.performHapticFeedback(stepHaptic)
                }
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(56.dp)) {
            drawCircle(color = theme.surface2)
            drawCircle(color = theme.stroke, style = Stroke(width = 1.dp.toPx()))
        }
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) theme.textPrimary else theme.textFaint,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer { rotationZ = iconRotation },
        )
    }
}

private val playStopHaptic: Int
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.CONFIRM
    } else {
        HapticFeedbackConstants.VIRTUAL_KEY
    }

private val stepHaptic: Int
    get() = HapticFeedbackConstants.CLOCK_TICK
