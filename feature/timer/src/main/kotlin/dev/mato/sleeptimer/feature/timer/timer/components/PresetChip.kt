package dev.mato.sleeptimer.feature.timer.timer.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.mato.sleeptimer.feature.timer.theme.DesignTokens

data class Preset(val minutes: Int, val label: String)

@Composable
fun PresetRow(
    active: Int,
    presets: List<Preset>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        for (preset in presets) {
            PresetChip(
                label = preset.label,
                selected = active == preset.minutes,
                onClick = { onSelect(preset.minutes) },
            )
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) DesignTokens.Accent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f)
    val border = if (selected) DesignTokens.Accent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f)
    val textColor = if (selected) DesignTokens.Accent else Color.White.copy(alpha = 0.75f)

    Surface(
        color = bg,
        shape = RoundedCornerShape(100),
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .border(1.dp, border, RoundedCornerShape(100))
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
        )
    }
}
