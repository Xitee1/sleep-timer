package dev.xitee.sleeptimer.feature.timer.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.xitee.sleeptimer.core.data.model.ThemeId
import dev.xitee.sleeptimer.feature.timer.R
import dev.xitee.sleeptimer.feature.timer.settings.components.SettingsTopBar
import dev.xitee.sleeptimer.feature.timer.settings.components.ThemePreviewIcon
import dev.xitee.sleeptimer.feature.timer.theme.AppTheme
import dev.xitee.sleeptimer.feature.timer.theme.AppThemes
import dev.xitee.sleeptimer.feature.timer.theme.LocalAppTheme
import dev.xitee.sleeptimer.feature.timer.theme.rememberAnimatedAppTheme
import dev.xitee.sleeptimer.feature.timer.timer.components.TimerBackground

@Composable
fun ThemePickerScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ready = uiState ?: return

    val animatedTheme = rememberAnimatedAppTheme(AppThemes.byId(ready.settings.theme))
    CompositionLocalProvider(LocalAppTheme provides animatedTheme) {
        ThemePickerContent(
            selected = ready.settings.theme,
            starsEnabled = ready.settings.starsEnabled,
            onBack = onBack,
            onSelect = { viewModel.updateTheme(it) },
        )
    }
}

@Composable
private fun ThemePickerContent(
    selected: ThemeId,
    starsEnabled: Boolean,
    onBack: () -> Unit,
    onSelect: (ThemeId) -> Unit,
) {
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
            SettingsTopBar(
                title = stringResource(R.string.category_appearance),
                onBack = onBack,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(AppThemes.All, key = { it.id }) { option ->
                    ThemeGridCard(
                        option = option,
                        isSelected = option.id == selected,
                        onClick = { onSelect(option.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeGridCard(
    option: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val theme = LocalAppTheme.current
    val borderColor = if (isSelected) option.accent else theme.stroke
    val labelColor = if (isSelected) option.accent else theme.textDim

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ThemePreviewIcon(option = option, size = 64.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(option.labelRes),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = labelColor,
        )
    }
}
