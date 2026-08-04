package dev.xitee.sleeptimer.feature.timer.settings.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.xitee.sleeptimer.feature.timer.R

@Composable
fun AccessibilityRequiredDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    @StringRes dismissLabelRes: Int = R.string.shizuku_action_cancel,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Accessibility, contentDescription = null) },
        title = { Text(stringResource(R.string.accessibility_dialog_title)) },
        text = { Text(stringResource(R.string.accessibility_body_required)) },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.accessibility_action_open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(dismissLabelRes))
            }
        },
    )
}
