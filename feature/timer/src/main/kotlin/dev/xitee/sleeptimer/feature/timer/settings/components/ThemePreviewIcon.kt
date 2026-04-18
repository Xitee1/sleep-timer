package dev.xitee.sleeptimer.feature.timer.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.xitee.sleeptimer.feature.timer.theme.AppTheme

@Composable
fun ThemePreviewIcon(
    option: AppTheme,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        if (option.hasGradient) {
            drawCircle(
                brush = Brush.verticalGradient(
                    0f to option.bgTop,
                    0.5f to option.bgMid,
                    1f to option.bgBot,
                    startY = 0f,
                    endY = this.size.height,
                ),
                radius = r,
                center = center,
            )
        } else {
            drawCircle(color = option.bgSolid, radius = r, center = center)
        }
        drawCircle(
            color = Color.White.copy(alpha = if (option.isDark) 0.15f else 0.25f),
            radius = r,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
        val inset = (size.toPx() * (6f / 36f))
        drawCircle(
            color = option.accent,
            radius = 3.dp.toPx() * (size.toPx() / 36.dp.toPx()),
            center = Offset(center.x, center.y - r + inset + 1.dp.toPx()),
        )
        drawArc(
            color = option.accent,
            startAngle = 35f,
            sweepAngle = 290f,
            useCenter = false,
            topLeft = Offset(center.x - r + inset, center.y - r + inset),
            size = Size(r * 2f - inset * 2f, r * 2f - inset * 2f),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}
