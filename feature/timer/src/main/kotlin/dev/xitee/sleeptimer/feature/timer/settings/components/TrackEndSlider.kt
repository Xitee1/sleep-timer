package dev.xitee.sleeptimer.feature.timer.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import dev.xitee.sleeptimer.core.data.model.TRACK_END_MAX_WAIT_MINUTES
import dev.xitee.sleeptimer.feature.timer.R
import dev.xitee.sleeptimer.feature.timer.theme.appTheme
import kotlin.math.roundToInt

/**
 * "Let the song finish" slider: 0 = off, otherwise the longest rest of a song the
 * timer waits for. Greyed out while no read method is usable — a tap then opens the
 * method choice; a long-press always does, to switch methods later.
 */
@Composable
fun TrackEndSlider(
    maxWaitMinutes: Int,
    available: Boolean,
    description: String,
    onMaxWaitChanged: (Int) -> Unit,
    onChooseMethod: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = appTheme()
    var sliderValue by remember(maxWaitMinutes) { mutableFloatStateOf(maxWaitMinutes.toFloat()) }
    val minutes = sliderValue.roundToInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = { if (!available) onChooseMethod() },
                onLongClick = onChooseMethod,
                onLongClickLabel = stringResource(R.string.track_end_change_method),
            )
            .background(theme.surface1)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.track_end_title),
                style = MaterialTheme.typography.titleMedium,
                color = if (available) theme.textPrimary else theme.textFaint,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (minutes == 0) {
                    stringResource(R.string.track_end_off)
                } else {
                    stringResource(R.string.track_end_minutes, minutes)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (available) theme.accent else theme.textFaint,
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = if (available) theme.textDim else theme.textFaint,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onMaxWaitChanged(sliderValue.roundToInt()) },
            enabled = available,
            valueRange = 0f..TRACK_END_MAX_WAIT_MINUTES.toFloat(),
            steps = TRACK_END_MAX_WAIT_MINUTES - 1,
            colors = SliderDefaults.colors(
                thumbColor = theme.accent,
                activeTrackColor = theme.accent,
                inactiveTrackColor = theme.stroke,
                activeTickColor = theme.accent,
                inactiveTickColor = theme.stroke,
                disabledThumbColor = theme.textFaint,
                disabledActiveTrackColor = theme.textFaint,
                disabledInactiveTrackColor = theme.stroke,
                disabledActiveTickColor = theme.textFaint,
                disabledInactiveTickColor = theme.stroke,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
