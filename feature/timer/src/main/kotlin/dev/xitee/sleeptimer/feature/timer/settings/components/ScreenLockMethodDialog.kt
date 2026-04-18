package dev.xitee.sleeptimer.feature.timer.settings.components

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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.xitee.sleeptimer.feature.timer.R

@Composable
fun ScreenLockMethodDialog(
    onHardLockSelected: () -> Unit,
    onSoftLockSelected: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
        title = { Text(stringResource(R.string.screen_method_dialog_title)) },
        text = {
            Column {
                MethodOption(
                    icon = Icons.Default.AdminPanelSettings,
                    title = stringResource(R.string.screen_method_hard_title),
                    description = stringResource(R.string.screen_method_hard_description),
                    onClick = { onHardLockSelected(); onDismiss() },
                )
                Spacer(Modifier.height(8.dp))
                MethodOption(
                    icon = Icons.Default.Nightlight,
                    title = stringResource(R.string.screen_method_soft_title),
                    description = stringResource(R.string.screen_method_soft_description),
                    onClick = { onSoftLockSelected(); onDismiss() },
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
private fun MethodOption(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
