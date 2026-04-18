package dev.xitee.sleeptimer.feature.timer.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.xitee.sleeptimer.core.data.model.ThemeId
import dev.xitee.sleeptimer.feature.timer.theme.AppTheme
import dev.xitee.sleeptimer.feature.timer.theme.AppThemes
import dev.xitee.sleeptimer.feature.timer.theme.appTheme

/**
 * Horizontal row with one card per available theme. Selected card gets an accent border
 * and tinted label.
 */
@Composable
fun ThemeSelector(
    selected: ThemeId,
    onSelect: (ThemeId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (option in AppThemes.All) {
            ThemeCard(
                option = option,
                isSelected = option.id == selected,
                onClick = { onSelect(option.id) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ThemeCard(
    option: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentTheme = appTheme()
    val borderColor = if (isSelected) option.accent else currentTheme.stroke
    val labelColor = if (isSelected) option.accent else currentTheme.textDim

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ThemePreview(option)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(option.labelRes),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = labelColor,
        )
    }
}

@Composable
private fun ThemePreview(option: AppTheme) {
    Canvas(modifier = Modifier.size(36.dp)) {
        val r = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        // Body
        if (option.hasGradient) {
            drawCircle(
                brush = Brush.verticalGradient(
                    0f to option.bgTop,
                    0.5f to option.bgMid,
                    1f to option.bgBot,
                    startY = 0f,
                    endY = size.height,
                ),
                radius = r,
                center = center,
            )
        } else {
            drawCircle(color = option.bgSolid, radius = r, center = center)
        }
        // Hairline border
        drawCircle(
            color = Color.White.copy(alpha = if (option.isDark) 0.15f else 0.25f),
            radius = r,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
        // Accent dot (top)
        drawCircle(
            color = option.accent,
            radius = 3.dp.toPx(),
            center = Offset(center.x, center.y - r + 7.dp.toPx()),
        )
        // Accent "C" ring – approximated with an arc-like stroke
        drawArc(
            color = option.accent,
            startAngle = 35f,
            sweepAngle = 290f,
            useCenter = false,
            topLeft = Offset(center.x - r + 6.dp.toPx(), center.y - r + 6.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(
                r * 2f - 12.dp.toPx(),
                r * 2f - 12.dp.toPx(),
            ),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

