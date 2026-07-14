package dev.xitee.sleeptimer.feature.timer.settings.components

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.xitee.sleeptimer.core.data.model.MediaEndAction
import dev.xitee.sleeptimer.feature.timer.R
import dev.xitee.sleeptimer.feature.timer.theme.appTheme

/** Title string shown in the settings row's description slot for the selected action. */
@get:StringRes
val MediaEndAction.labelRes: Int
    get() = when (this) {
        MediaEndAction.Pause -> R.string.media_end_action_pause_title
        MediaEndAction.Stop -> R.string.media_end_action_stop_title
    }

@Composable
fun MediaEndActionDialog(
    selected: MediaEndAction,
    onSelect: (MediaEndAction) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.MusicOff, contentDescription = null) },
        title = { Text(stringResource(R.string.media_end_action_dialog_title)) },
        text = {
            Column {
                ActionOption(
                    icon = Icons.Default.Pause,
                    title = stringResource(R.string.media_end_action_pause_title),
                    description = stringResource(R.string.media_end_action_pause_description),
                    isSelected = selected == MediaEndAction.Pause,
                    onClick = { onSelect(MediaEndAction.Pause); onDismiss() },
                )
                Spacer(Modifier.height(8.dp))
                ActionOption(
                    icon = Icons.Default.Stop,
                    title = stringResource(R.string.media_end_action_stop_title),
                    description = stringResource(R.string.media_end_action_stop_description),
                    isSelected = selected == MediaEndAction.Stop,
                    onClick = { onSelect(MediaEndAction.Stop); onDismiss() },
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.shizuku_action_cancel))
            }
        },
    )
}

@Composable
private fun ActionOption(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val accent = appTheme().accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isSelected) accent else LocalContentColor.current,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) accent else LocalContentColor.current,
                fontWeight = if (isSelected) FontWeight.SemiBold else null,
            )
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
