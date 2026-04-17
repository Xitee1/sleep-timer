package dev.mato.sleeptimer.feature.timer.theme

import androidx.compose.ui.graphics.Color

// Midnight palette used for the immersive background gradient and glows.
object DesignTokens {
    val BgTop = Color(0xFF2A2550)
    val BgMid = Color(0xFF1A1830)
    val BgBot = Color(0xFF0F0D1F)

    val Accent = Color(0xFFC9B8FF)
    val AccentDeep = Color(0xFF6B5BD6)

    val Surface1 = Color(0x0AFFFFFF) // 4% white — chips, settings rows, knob halo
    val Surface2 = Color(0x10FFFFFF) // 6% white — secondary buttons
    val Stroke = Color(0x1AFFFFFF) // 10% white — thin borders
    val StrokeStrong = Color(0x26FFFFFF) // 15% white

    val TextPrimary = Color(0xFFFFFFFF)
    val TextDim = Color(0x8CFFFFFF) // 55%
    val TextMuted = Color(0x73FFFFFF) // 45%
    val TextFaint = Color(0x4DFFFFFF) // 30%
}
