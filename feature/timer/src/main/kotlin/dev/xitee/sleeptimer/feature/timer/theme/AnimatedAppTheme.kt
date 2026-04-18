package dev.xitee.sleeptimer.feature.timer.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

private const val THEME_TRANSITION_MS = 350

@Composable
fun rememberAnimatedAppTheme(target: AppTheme): AppTheme {
    val spec: AnimationSpec<Color> = tween(durationMillis = THEME_TRANSITION_MS)
    val bgTop by animateColorAsState(target.bgTop, spec, label = "bgTop")
    val bgMid by animateColorAsState(target.bgMid, spec, label = "bgMid")
    val bgBot by animateColorAsState(target.bgBot, spec, label = "bgBot")
    val bgSolid by animateColorAsState(target.bgSolid, spec, label = "bgSolid")
    val auroraColor by animateColorAsState(target.auroraColor, spec, label = "auroraColor")
    val starColor by animateColorAsState(target.starColor, spec, label = "starColor")
    val accent by animateColorAsState(target.accent, spec, label = "accent")
    val accentInk by animateColorAsState(target.accentInk, spec, label = "accentInk")
    val surface1 by animateColorAsState(target.surface1, spec, label = "surface1")
    val surface2 by animateColorAsState(target.surface2, spec, label = "surface2")
    val stroke by animateColorAsState(target.stroke, spec, label = "stroke")
    val strokeStrong by animateColorAsState(target.strokeStrong, spec, label = "strokeStrong")
    val textPrimary by animateColorAsState(target.textPrimary, spec, label = "textPrimary")
    val textDim by animateColorAsState(target.textDim, spec, label = "textDim")
    val textMuted by animateColorAsState(target.textMuted, spec, label = "textMuted")
    val textFaint by animateColorAsState(target.textFaint, spec, label = "textFaint")
    val dialPlateTop by animateColorAsState(target.dialPlateTop, spec, label = "dialPlateTop")
    val dialPlateBot by animateColorAsState(target.dialPlateBot, spec, label = "dialPlateBot")
    val dialWell by animateColorAsState(target.dialWell, spec, label = "dialWell")
    val dialTrack by animateColorAsState(target.dialTrack, spec, label = "dialTrack")
    val dialTickMajor by animateColorAsState(target.dialTickMajor, spec, label = "dialTickMajor")
    val dialTickMinor by animateColorAsState(target.dialTickMinor, spec, label = "dialTickMinor")
    val knobBody by animateColorAsState(target.knobBody, spec, label = "knobBody")
    val hourDotInactive by animateColorAsState(target.hourDotInactive, spec, label = "hourDotInactive")

    return target.copy(
        bgTop = bgTop,
        bgMid = bgMid,
        bgBot = bgBot,
        bgSolid = bgSolid,
        auroraColor = auroraColor,
        starColor = starColor,
        accent = accent,
        accentInk = accentInk,
        surface1 = surface1,
        surface2 = surface2,
        stroke = stroke,
        strokeStrong = strokeStrong,
        textPrimary = textPrimary,
        textDim = textDim,
        textMuted = textMuted,
        textFaint = textFaint,
        dialPlateTop = dialPlateTop,
        dialPlateBot = dialPlateBot,
        dialWell = dialWell,
        dialTrack = dialTrack,
        dialTickMajor = dialTickMajor,
        dialTickMinor = dialTickMinor,
        knobBody = knobBody,
        hourDotInactive = hourDotInactive,
    )
}
