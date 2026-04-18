package dev.xitee.sleeptimer.feature.timer.theme

import androidx.compose.ui.graphics.Color
import dev.xitee.sleeptimer.core.data.model.ThemeId
import dev.xitee.sleeptimer.feature.timer.R

object AppThemes {

    val Midnight: AppTheme = darkThemeOf(
        id = ThemeId.Midnight,
        labelRes = R.string.theme_midnight,
        bgTop = Color(0xFF2A2550),
        bgMid = Color(0xFF1A1830),
        bgBot = Color(0xFF0F0D1F),
        accent = Color(0xFFC9B8FF),
    )

    val Ocean: AppTheme = darkThemeOf(
        id = ThemeId.Ocean,
        labelRes = R.string.theme_ocean,
        bgTop = Color(0xFF0F3A55),
        bgMid = Color(0xFF0A2438),
        bgBot = Color(0xFF05121E),
        accent = Color(0xFF7DD3FC),
    )

    val Ember: AppTheme = darkThemeOf(
        id = ThemeId.Ember,
        labelRes = R.string.theme_ember,
        bgTop = Color(0xFF3A1810),
        bgMid = Color(0xFF22110A),
        bgBot = Color(0xFF0E0705),
        accent = Color(0xFFFCB07D),
    )

    val Light: AppTheme = AppTheme(
        id = ThemeId.Light,
        labelRes = R.string.theme_light,
        hasGradient = true,
        bgTop = Color(0xFFF4F1EC),
        bgMid = Color(0xFFEAE4DA),
        bgBot = Color(0xFFDFD7C9),
        bgSolid = Color(0xFFECE7DE),
        auroraColor = Color(0x306B5BD6),
        starColor = Color(0x00000000), // no stars on light surface
        allowStars = false,
        accent = Color(0xFF6B5BD6),
        accentInk = Color(0xFFFFFFFF),
        surface1 = Color(0x0A1A1830),
        surface2 = Color(0x141A1830),
        stroke = Color(0x1F1A1830),
        strokeStrong = Color(0x331A1830),
        textPrimary = Color(0xFF1A1830),
        textDim = Color(0x8C1A1830),
        textMuted = Color(0x731A1830),
        textFaint = Color(0x4D1A1830),
        dialPlateTop = Color(0x141A1830),
        dialPlateBot = Color(0x081A1830),
        dialWell = Color(0x08000000),
        dialTrack = Color(0x1F1A1830),
        dialTickMajor = Color(0x591A1830),
        dialTickMinor = Color(0x1F1A1830),
        knobBody = Color(0xFFFFFFFF),
        hourDotInactive = Color(0x2E1A1830),
        isDark = false,
    )

    val Basic: AppTheme = darkThemeOf(
        id = ThemeId.Basic,
        labelRes = R.string.theme_basic,
        bgTop = Color(0xFF1B1B1F),
        bgMid = Color(0xFF1B1B1F),
        bgBot = Color(0xFF1B1B1F),
        accent = Color(0xFFBFC2FF),
    ).copy(
        hasGradient = false,
        // Basic keeps a muted aurora-free look; stars can still be toggled.
        auroraColor = Color.Transparent,
    )

    val All: List<AppTheme> = listOf(Midnight, Ocean, Ember, Light, Basic)

    fun byId(id: ThemeId): AppTheme = when (id) {
        ThemeId.Midnight -> Midnight
        ThemeId.Ocean -> Ocean
        ThemeId.Ember -> Ember
        ThemeId.Light -> Light
        ThemeId.Basic -> Basic
    }
}

/** Factory for dark-on-gradient themes that only differ in their 3 gradient stops + accent. */
private fun darkThemeOf(
    id: ThemeId,
    labelRes: Int,
    bgTop: Color,
    bgMid: Color,
    bgBot: Color,
    accent: Color,
): AppTheme = AppTheme(
    id = id,
    labelRes = labelRes,
    hasGradient = true,
    bgTop = bgTop,
    bgMid = bgMid,
    bgBot = bgBot,
    bgSolid = bgMid,
    auroraColor = accent.copy(alpha = 0.25f),
    starColor = Color(0xFFFFFFFF),
    allowStars = true,
    accent = accent,
    accentInk = bgMid,
    surface1 = Color(0x0AFFFFFF),
    surface2 = Color(0x10FFFFFF),
    stroke = Color(0x1AFFFFFF),
    strokeStrong = Color(0x26FFFFFF),
    textPrimary = Color(0xFFFFFFFF),
    textDim = Color(0x8CFFFFFF),
    textMuted = Color(0x73FFFFFF),
    textFaint = Color(0x4DFFFFFF),
    dialPlateTop = Color(0x0FFFFFFF),
    dialPlateBot = Color(0x04FFFFFF),
    dialWell = Color(0x40000000),
    dialTrack = Color(0x0FFFFFFF),
    dialTickMajor = Color(0x59FFFFFF),
    dialTickMinor = Color(0x1FFFFFFF),
    knobBody = Color(0xFFFFFFFF),
    hourDotInactive = Color(0x2EFFFFFF),
    isDark = true,
)
