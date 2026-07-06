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
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenRotation
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
import dev.xitee.sleeptimer.core.data.model.AutoRotateMode
import dev.xitee.sleeptimer.feature.timer.R
import dev.xitee.sleeptimer.feature.timer.theme.appTheme

/** Title string shown in the settings row's description slot for the selected mode. */
@get:StringRes
val AutoRotateMode.labelRes: Int
    get() = when (this) {
        AutoRotateMode.System -> R.string.auto_rotate_system_title
        AutoRotateMode.Always -> R.string.auto_rotate_always_title
        AutoRotateMode.Portrait -> R.string.auto_rotate_portrait_title
    }

@Composable
fun AutoRotateModeDialog(
    selected: AutoRotateMode,
    onSelect: (AutoRotateMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.ScreenRotation, contentDescription = null) },
        title = { Text(stringResource(R.string.auto_rotate_dialog_title)) },
        text = {
            Column {
                ModeOption(
                    icon = Icons.Default.PhoneAndroid,
                    title = stringResource(R.string.auto_rotate_system_title),
                    description = stringResource(R.string.auto_rotate_system_description),
                    isSelected = selected == AutoRotateMode.System,
                    onClick = { onSelect(AutoRotateMode.System); onDismiss() },
                )
                Spacer(Modifier.height(8.dp))
                ModeOption(
                    icon = Icons.Default.ScreenRotation,
                    title = stringResource(R.string.auto_rotate_always_title),
                    description = stringResource(R.string.auto_rotate_always_description),
                    isSelected = selected == AutoRotateMode.Always,
                    onClick = { onSelect(AutoRotateMode.Always); onDismiss() },
                )
                Spacer(Modifier.height(8.dp))
                ModeOption(
                    icon = Icons.Default.ScreenLockPortrait,
                    title = stringResource(R.string.auto_rotate_portrait_title),
                    description = stringResource(R.string.auto_rotate_portrait_description),
                    isSelected = selected == AutoRotateMode.Portrait,
                    onClick = { onSelect(AutoRotateMode.Portrait); onDismiss() },
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
private fun ModeOption(
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
