package dev.mato.sleeptimer.feature.timer.timer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mato.sleeptimer.feature.timer.theme.DesignTokens

/**
 * Big center readout. Shows either a single number (e.g. "30") or a compound "h:mm" once
 * the timer crosses one hour. A small caption below describes the unit in quiet caps.
 */
@Composable
fun TimeDisplay(
    totalMinutes: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    val hours = totalMinutes / 60
    val remMin = totalMinutes % 60

    val big: String
    val small: String
    when {
        hours == 0 -> {
            big = remMin.toString()
            small = if (remMin == 1) "minute" else "minutes"
        }
        remMin == 0 -> {
            big = hours.toString()
            small = if (hours == 1) "hour" else "hours"
        }
        else -> {
            big = "$hours:${remMin.toString().padStart(2, '0')}"
            small = "hr · min"
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = big,
            style = MaterialTheme.typography.displayLarge,
            color = DesignTokens.TextPrimary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label.ifEmpty { small }.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = DesignTokens.TextDim,
        )
    }
}
