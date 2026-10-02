package dev.xitee.sleeptimer.feature.timer.timer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.xitee.sleeptimer.feature.timer.R
import dev.xitee.sleeptimer.feature.timer.theme.appTheme

@Composable
fun TimeDisplay(
    totalMinutes: Int,
    modifier: Modifier = Modifier,
) {
    val theme = appTheme()
    val hours = totalMinutes / 60
    val remMin = totalMinutes % 60

    val big: String
    val small: String
    if (hours == 0) {
        big = remMin.toString()
        small = stringResource(
            if (remMin == 1) R.string.time_unit_minute else R.string.time_unit_minutes,
        )
    } else {
        // From 60 minutes on always show "H:MM" — a bare "1" for a full hour reads
        // like "1 minute" at a glance.
        big = "$hours:${remMin.toString().padStart(2, '0')}"
        small = stringResource(R.string.time_unit_hr_min)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = big,
            style = MaterialTheme.typography.displayLarge,
            color = theme.textPrimary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = small.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = theme.textDim,
        )
    }
}
