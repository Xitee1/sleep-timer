package dev.xitee.sleeptimer.feature.timer.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.xitee.sleeptimer.core.data.model.TrackEndMethod
import dev.xitee.sleeptimer.feature.timer.R

/**
 * [showDisableHint]: when the feature is already set up (long-press path), explain
 * that it is turned off via the slider — users looking to disable it land here first.
 */
@Composable
fun TrackEndMethodDialog(
    selected: TrackEndMethod,
    showDisableHint: Boolean,
    onNotificationAccessSelected: () -> Unit,
    onShizukuSelected: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
        title = { Text(stringResource(R.string.track_end_method_dialog_title)) },
        text = {
            Column {
                ModeOption(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.track_end_method_notification_title),
                    description = stringResource(R.string.track_end_method_notification_description),
                    isSelected = selected == TrackEndMethod.NotificationAccess,
                    onClick = { onNotificationAccessSelected(); onDismiss() },
                )
                Spacer(Modifier.height(8.dp))
                ModeOption(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.track_end_method_shizuku_title),
                    description = stringResource(R.string.track_end_method_shizuku_description),
                    isSelected = selected == TrackEndMethod.Shizuku,
                    onClick = { onShizukuSelected(); onDismiss() },
                )
                if (showDisableHint) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.track_end_disable_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
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
fun NotificationAccessRequiredDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
        title = { Text(stringResource(R.string.notification_access_dialog_title)) },
        text = { Text(stringResource(R.string.notification_access_body_required)) },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.accessibility_action_open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.shizuku_action_cancel))
            }
        },
    )
}
