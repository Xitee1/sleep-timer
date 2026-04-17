package dev.mato.sleeptimer.feature.timer.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.mato.sleeptimer.core.data.model.ThemeId

/**
 * Full set of visual tokens used throughout the timer screens. Themes either render a
 * vertical gradient with aurora blobs (the immersive themes) or a single solid surface
 * (the minimal Basic theme).
 */
data class AppTheme(
    val id: ThemeId,
    val label: String,

    // Background
    val hasGradient: Boolean,
    val bgTop: Color,
    val bgMid: Color,
    val bgBot: Color,
    val bgSolid: Color,

    // Aurora / stars
    val auroraColor: Color,
    val starColor: Color,
    val allowStars: Boolean,

    // Accent
    val accent: Color,
    val accentInk: Color, // contrast ink drawn on top of `accent` (button icon, knob dot)

    // Surfaces & strokes (over background)
    val surface1: Color,
    val surface2: Color,
    val stroke: Color,
    val strokeStrong: Color,

    // Text
    val textPrimary: Color,
    val textDim: Color,
    val textMuted: Color,
    val textFaint: Color,

    // Dial parts
    val dialPlateTop: Color,
    val dialPlateBot: Color,
    val dialWell: Color,
    val dialTrack: Color,
    val dialTickMajor: Color,
    val dialTickMinor: Color,
    val knobBody: Color,
    val hourDotInactive: Color,

    val isDark: Boolean,
) {
    companion object
}

val LocalAppTheme = staticCompositionLocalOf { AppThemes.Midnight }

/** Shortcut: `appTheme()` == `LocalAppTheme.current`. */
@Composable
@ReadOnlyComposable
fun appTheme(): AppTheme = LocalAppTheme.current
