package dev.mato.sleeptimer.feature.timer.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.mato.sleeptimer.feature.timer.R
import dev.mato.sleeptimer.feature.timer.theme.DesignTokens
import kotlin.math.roundToInt

@Composable
fun FadeOutSlider(
    durationSeconds: Int,
    onDurationChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sliderValue by remember(durationSeconds) { mutableFloatStateOf(durationSeconds.toFloat()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DesignTokens.Surface1)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.fade_out_title),
                style = MaterialTheme.typography.titleMedium,
                color = DesignTokens.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.fade_out_seconds, sliderValue.roundToInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = DesignTokens.Accent,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onDurationChanged(sliderValue.roundToInt()) },
            valueRange = 0f..120f,
            steps = 23,
            colors = SliderDefaults.colors(
                thumbColor = DesignTokens.Accent,
                activeTrackColor = DesignTokens.Accent,
                inactiveTrackColor = DesignTokens.Stroke,
                activeTickColor = DesignTokens.Accent,
                inactiveTickColor = DesignTokens.Stroke,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
