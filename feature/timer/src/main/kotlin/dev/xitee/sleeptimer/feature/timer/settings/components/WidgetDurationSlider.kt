package dev.xitee.sleeptimer.feature.timer.settings.components

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
import dev.xitee.sleeptimer.feature.timer.R
import dev.xitee.sleeptimer.feature.timer.theme.appTheme
import kotlin.math.roundToInt

private const val MIN_MINUTES = 5
private const val MAX_MINUTES = 240
private const val STEP = 5

/** Fixed duration the home-screen widget starts with, in 5-minute steps. */
@Composable
fun WidgetDurationSlider(
    minutes: Int,
    onMinutesChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = appTheme()
    var sliderValue by remember(minutes) { mutableFloatStateOf(minutes.toFloat()) }

    // Snap to the slider grid so a value persisted outside it (e.g. a future
    // migration) still renders on a tick.
    fun snapped(): Int =
        ((sliderValue / STEP).roundToInt() * STEP).coerceIn(MIN_MINUTES, MAX_MINUTES)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(theme.surface1)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.widget_duration_title),
                style = MaterialTheme.typography.titleMedium,
                color = theme.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.widget_duration_value, snapped()),
                style = MaterialTheme.typography.bodyMedium,
                color = theme.accent,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onMinutesChanged(snapped()) },
            valueRange = MIN_MINUTES.toFloat()..MAX_MINUTES.toFloat(),
            steps = (MAX_MINUTES - MIN_MINUTES) / STEP - 1,
            colors = SliderDefaults.colors(
                thumbColor = theme.accent,
                activeTrackColor = theme.accent,
                inactiveTrackColor = theme.stroke,
                activeTickColor = theme.accent,
                inactiveTickColor = theme.stroke,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
