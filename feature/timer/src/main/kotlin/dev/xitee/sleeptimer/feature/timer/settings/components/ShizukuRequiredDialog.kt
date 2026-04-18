package dev.xitee.sleeptimer.feature.timer.settings.components

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuManager
import dev.xitee.sleeptimer.feature.timer.R

@Composable
fun ShizukuRequiredDialog(
    state: ShizukuManager.State,
    featureExplanation: String,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    val (bodyRes, confirmRes, onConfirm) = when (state) {
        ShizukuManager.State.NotInstalled -> Triple(
            R.string.shizuku_body_not_installed,
            R.string.shizuku_action_install,
            {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://shizuku.rikka.app/".toUri(),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                onDismiss()
            },
        )
        ShizukuManager.State.NotRunning -> Triple(
            R.string.shizuku_body_not_running,
            R.string.shizuku_action_open,
            {
                val pm = context.packageManager
                val intent = pm.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                intent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
                onDismiss()
            },
        )
        ShizukuManager.State.PermissionRequired -> Triple(
            R.string.shizuku_body_permission,
            R.string.shizuku_action_grant,
            { onRequestPermission(); onDismiss() },
        )
        ShizukuManager.State.Ready -> Triple(
            R.string.shizuku_body_ready,
            R.string.shizuku_action_ok,
            onDismiss,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shizuku_dialog_title)) },
        text = {
            Column {
                Text(featureExplanation)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(bodyRes))
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text(stringResource(confirmRes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.shizuku_action_cancel))
            }
        },
    )
}
