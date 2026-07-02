package dev.xitee.sleeptimer.feature.timer.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Provides [LocalAppTheme] and mirrors the theme into a Material color scheme so
 * Material components (dialogs, text buttons, sliders) match the selected app theme
 * instead of the static dark scheme from the app module. Also keeps the system bar
 * icon contrast in sync with the app theme rather than the system dark-mode setting.
 */
@Composable
fun ProvideAppTheme(theme: AppTheme, content: @Composable () -> Unit) {
    SyncSystemBarAppearance(isDark = theme.isDark)
    CompositionLocalProvider(LocalAppTheme provides theme) {
        MaterialTheme(
            colorScheme = theme.toMaterialColorScheme(),
            typography = MaterialTheme.typography,
            content = content,
        )
    }
}

private fun AppTheme.toMaterialColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    // AlertDialogs and menus draw on the surfaceContainer roles; derive a subtly
    // elevated container from the theme background instead of the Material baseline.
    val container = if (isDark) surface2.compositeOver(bgSolid) else Color(0xFFFAF8F3)
    return base.copy(
        primary = accent,
        onPrimary = accentInk,
        secondary = accent,
        onSecondary = accentInk,
        background = bgSolid,
        onBackground = textPrimary,
        surface = bgSolid,
        onSurface = textPrimary,
        surfaceVariant = container,
        onSurfaceVariant = textDim,
        outline = strokeStrong,
        surfaceTint = accent,
        surfaceContainerHigh = container,
        surfaceContainerHighest = container,
    )
}

@Composable
private fun SyncSystemBarAppearance(isDark: Boolean) {
    val view = LocalView.current
    LaunchedEffect(view, isDark) {
        val window = view.context.findActivity()?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !isDark
        insetsController.isAppearanceLightNavigationBars = !isDark
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
