package dev.xitee.sleeptimer.feature.timer.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.xitee.sleeptimer.core.data.model.ThemeId
import dev.xitee.sleeptimer.feature.timer.theme.AppThemes
import dev.xitee.sleeptimer.feature.timer.theme.appTheme

@Composable
fun ThemeRow(
    selected: ThemeId,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = appTheme()
    val option = AppThemes.byId(selected)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemePreviewIcon(option = option, size = 40.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = stringResource(option.labelRes),
            style = MaterialTheme.typography.titleMedium,
            color = theme.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = theme.textMuted,
            modifier = Modifier.size(24.dp),
        )
    }
}
