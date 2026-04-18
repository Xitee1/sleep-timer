package dev.xitee.sleeptimer.feature.timer.about

import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xitee.sleeptimer.feature.timer.R
import dev.xitee.sleeptimer.feature.timer.theme.AppThemes
import dev.xitee.sleeptimer.feature.timer.theme.LocalAppTheme
import dev.xitee.sleeptimer.feature.timer.theme.appTheme
import dev.xitee.sleeptimer.feature.timer.timer.components.TimerBackground

private const val REPO_URL = "https://github.com/Xitee1/sleep-timer"
private const val DONATE_URL = "https://github.com/Xitee1/Xitee1/blob/main/donate.md"

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalAppTheme provides AppThemes.byId(settings.theme)) {
        AboutContent(
            starsEnabled = settings.starsEnabled,
            onBack = onBack,
        )
    }
}

@Composable
private fun AboutContent(
    starsEnabled: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }
    }

    val openUrl: (String) -> Unit = { url ->
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    TimerBackground(
        animating = false,
        starsEnabled = starsEnabled,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            AboutTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                SectionHeader(stringResource(R.string.category_about))

                AboutRow(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.about_version_title),
                    value = versionName,
                )
                AboutRow(
                    icon = Icons.Default.Person,
                    title = stringResource(R.string.about_author_title),
                    value = stringResource(R.string.about_author_value),
                )
                AboutRow(
                    icon = Icons.Default.Code,
                    title = stringResource(R.string.about_repo_title),
                    value = stringResource(R.string.about_repo_value),
                    onClick = { openUrl(REPO_URL) },
                )
                AboutRow(
                    icon = Icons.Default.FavoriteBorder,
                    title = stringResource(R.string.about_donate_title),
                    value = stringResource(R.string.about_donate_description),
                    onClick = { openUrl(DONATE_URL) },
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AboutTopBar(onBack: () -> Unit) {
    val theme = appTheme()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = theme.textPrimary,
            )
        }
        Text(
            text = stringResource(R.string.about_title),
            style = MaterialTheme.typography.titleLarge,
            color = theme.textPrimary,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    val theme = appTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = theme.textMuted,
        )
    }
}

@Composable
private fun AboutRow(
    icon: ImageVector,
    title: String,
    value: String?,
    onClick: (() -> Unit)? = null,
) {
    val theme = appTheme()
    val rowModifier = Modifier
        .fillMaxWidth()
        .let { if (onClick != null) it.clickable { onClick() } else it }
        .padding(horizontal = 20.dp, vertical = 14.dp)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(theme.surface1),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = theme.accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = theme.textPrimary,
            )
            if (!value.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = theme.textDim,
                )
            }
        }
        if (onClick != null) {
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = theme.textMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

