package dev.xitee.sleeptimer.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MidnightColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = MidnightBgMid,
    primaryContainer = AccentDeep,
    onPrimaryContainer = TextPrimary,
    secondary = Accent,
    onSecondary = MidnightBgMid,
    background = MidnightBgMid,
    onBackground = TextPrimary,
    surface = MidnightBgMid,
    onSurface = TextPrimary,
    surfaceVariant = Surface1,
    onSurfaceVariant = TextDim,
    outline = SurfaceStroke,
)

@Composable
fun SleepTimerTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = MidnightColorScheme,
        typography = Typography,
        content = content,
    )
}
