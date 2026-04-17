package dev.mato.sleeptimer.feature.timer.timer.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.mato.sleeptimer.feature.timer.R
import dev.mato.sleeptimer.feature.timer.theme.DesignTokens

/**
 * Big primary round button (84 dp) filled with the accent colour, with a warm colored
 * glow and an inner highlight. Icon tint = midnight ink.
 */
@Composable
fun PlayButton(
    isRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon: ImageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow
    val desc = stringResource(if (isRunning) R.string.stop_timer else R.string.start_timer)
    RoundAccentButton(icon = icon, contentDescription = desc, onClick = onClick, modifier = modifier)
}

@Composable
fun RoundAccentButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(84.dp)
            .shadow(
                elevation = 20.dp,
                shape = CircleShape,
                ambientColor = DesignTokens.Accent,
                spotColor = DesignTokens.Accent,
            )
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(84.dp)) {
            drawCircle(color = DesignTokens.Accent)
            drawCircle(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.45f),
                    1f to Color.White.copy(alpha = 0f),
                    startY = 0f,
                    endY = size.height * 0.55f,
                ),
                radius = size.minDimension / 2f - 1.dp.toPx(),
                center = Offset(size.width / 2f, size.height / 2f),
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = DesignTokens.BgMid,
            modifier = Modifier.size(34.dp),
        )
    }
}

/**
 * Secondary round button (56 dp) — soft surface with a thin border.
 */
@Composable
fun SecondaryRoundButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(56.dp)) {
            drawCircle(color = DesignTokens.Surface2)
            drawCircle(color = DesignTokens.Stroke, style = Stroke(width = 1.dp.toPx()))
        }
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(22.dp),
        )
    }
}
