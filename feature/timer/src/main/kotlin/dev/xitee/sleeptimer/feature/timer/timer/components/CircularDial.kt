package dev.xitee.sleeptimer.feature.timer.timer.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import dev.xitee.sleeptimer.feature.timer.theme.AppTheme
import dev.xitee.sleeptimer.feature.timer.theme.appTheme
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularDial(
    state: CircularDialState,
    isRunning: Boolean,
    runningMinutes: Float,
    hapticEnabled: Boolean,
    onMinutesChanged: (Int) -> Unit,
    onMinutesCommitted: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = appTheme()
    val view = LocalView.current
    val tickHapticType = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        }
    }
    var lastReportedMinutes by remember { mutableStateOf(state.totalMinutes) }

    val targetMinutes: Float = if (isRunning) runningMinutes else state.totalMinutes.toFloat()
    val animatedMinutes = remember { Animatable(targetMinutes) }
    LaunchedEffect(targetMinutes, state.isDragging) {
        val delta = kotlin.math.abs(targetMinutes - animatedMinutes.value)
        val snap = state.isDragging || delta < 1f
        if (snap) {
            animatedMinutes.snapTo(targetMinutes)
        } else {
            animatedMinutes.animateTo(
                targetValue = targetMinutes,
                animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            )
        }
    }

    val displayMinutes: Float = animatedMinutes.value
    val minuteInRing = ((displayMinutes % 60f) + 60f) % 60f
    val ringFraction = minuteInRing / 60f
    val hoursComplete = (displayMinutes / 60f).toInt().coerceIn(0, 5)

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!isRunning) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    state.onDragStart(
                                        centerX = size.width / 2f,
                                        centerY = size.height / 2f,
                                        x = offset.x,
                                        y = offset.y,
                                    )
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    state.onDrag(
                                        centerX = size.width / 2f,
                                        centerY = size.height / 2f,
                                        x = change.position.x,
                                        y = change.position.y,
                                    )
                                    if (state.totalMinutes != lastReportedMinutes) {
                                        if (hapticEnabled) {
                                            view.performHapticFeedback(tickHapticType)
                                        }
                                        lastReportedMinutes = state.totalMinutes
                                        onMinutesChanged(state.totalMinutes)
                                    }
                                },
                                onDragEnd = {
                                    state.onDragEnd()
                                    // Persist only at drag end to avoid DataStore write flood
                                    // during the gesture (30+ writes/sec otherwise).
                                    onMinutesCommitted(state.totalMinutes)
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            val strokeWidth = 14.dp.toPx()
            val plateInset = 6.dp.toPx()
            val radius = (size.minDimension - strokeWidth - plateInset * 2f) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer glow
            val glowOuter = radius + strokeWidth * 0.5f + 8.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    0.65f to Color.Transparent,
                    1.00f to theme.accent.copy(alpha = if (isRunning) 0.28f else 0.14f),
                    center = center,
                    radius = glowOuter,
                ),
                radius = glowOuter,
                center = center,
            )

            // Plate
            drawCircle(
                brush = Brush.verticalGradient(
                    0.0f to theme.dialPlateTop,
                    1.0f to theme.dialPlateBot,
                    startY = center.y - radius,
                    endY = center.y + radius,
                ),
                radius = radius + strokeWidth * 0.5f,
                center = center,
            )
            drawCircle(
                color = theme.stroke,
                radius = radius + strokeWidth * 0.5f,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = theme.dialWell,
                radius = radius - strokeWidth * 0.5f - 6.dp.toPx(),
                center = center,
            )

            drawTicks(center, radius, strokeWidth, theme)

            drawCircle(
                color = theme.dialTrack,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            if (displayMinutes > 0f) {
                drawProgressArc(
                    center = center,
                    radius = radius,
                    strokeWidth = strokeWidth,
                    fraction = ringFraction,
                    overflowing = displayMinutes >= 60f,
                    trailColor = theme.accent.copy(alpha = 0.32f),
                    leadColor = theme.accent,
                )
            }

            drawHourDots(center, radius, hoursComplete, theme)

            drawKnob(
                center = center,
                radius = radius,
                fraction = ringFraction,
                theme = theme,
                dimmed = isRunning,
            )
        }
    }
}

private fun DrawScope.drawTicks(center: Offset, radius: Float, strokeWidth: Float, theme: AppTheme) {
    for (i in 0 until 60) {
        val big = i % 5 == 0
        val angle = (i / 60f) * (2f * Math.PI.toFloat()) - (Math.PI.toFloat() / 2f)
        val inner = radius - strokeWidth * 0.5f - (if (big) 10.dp.toPx() else 6.dp.toPx())
        val outer = radius - strokeWidth * 0.5f - 3.dp.toPx()
        val x1 = center.x + inner * cos(angle)
        val y1 = center.y + inner * sin(angle)
        val x2 = center.x + outer * cos(angle)
        val y2 = center.y + outer * sin(angle)
        drawLine(
            color = if (big) theme.dialTickMajor else theme.dialTickMinor,
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = if (big) 1.5.dp.toPx() else 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawProgressArc(
    center: Offset,
    radius: Float,
    strokeWidth: Float,
    fraction: Float,
    overflowing: Boolean,
    trailColor: Color,
    leadColor: Color,
) {
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = Size(radius * 2, radius * 2)
    val glowStrokeWidth = strokeWidth + 8.dp.toPx()
    val radToDeg = 180f / Math.PI.toFloat()
    val mainCapDeg = (strokeWidth * 0.5f) / radius * radToDeg

    val sweep = if (overflowing) 360f else 360f * fraction.coerceIn(0f, 1f)
    // Rotate the canvas so that the sweep gradient's 0° axis (east in the rotated frame)
    // aligns with the arc's start angle in the original frame — top when partial, handle
    // position when the ring has wrapped past 60 minutes.
    val rotationDegrees = if (overflowing) -90f + fraction * 360f else -90f
    val leadStop = if (overflowing) 1f else sweep / 360f

    // Main arc: inset by one cap on each side so the round caps land flush with the visible
    // span — starts exactly at top, ends exactly at handle.
    val mainStartOffset = if (overflowing) 0f else mainCapDeg
    val mainSweep = if (overflowing) 360f else (sweep - 2f * mainCapDeg).coerceAtLeast(0f)

    // The sweep gradient's stops determine color at every angle around the full circle,
    // including outside the arc's visible range. Without a snap-back, round caps extending
    // past the last stop sample the lead color (Android clamps to last stop), which paints
    // a bright blob where we expect a faded halo. Adding a quick trail-colored stop just
    // past `leadStop` keeps the glow's rounded extension soft.
    val snapStop = (leadStop + 0.001f).coerceAtMost(0.999f)
    val trailFaded = trailColor.copy(alpha = trailColor.alpha * 0.5f)
    val leadFaded = leadColor.copy(alpha = leadColor.alpha * 0.35f)

    val mainBrush = if (overflowing) {
        Brush.sweepGradient(
            0f to trailColor,
            1f to leadColor,
            center = center,
        )
    } else {
        Brush.sweepGradient(
            0f to trailColor,
            leadStop to leadColor,
            snapStop to trailColor,
            1f to trailColor,
            center = center,
        )
    }
    val glowBrush = if (overflowing) {
        Brush.sweepGradient(
            0f to trailFaded,
            1f to leadFaded,
            center = center,
        )
    } else {
        Brush.sweepGradient(
            0f to trailFaded,
            leadStop to leadFaded,
            snapStop to trailFaded,
            1f to trailFaded,
            center = center,
        )
    }

    // Glow shares the main arc's geometric start/sweep — only the stroke is wider. The
    // round cap then overhangs each end by the same radial amount it overhangs the sides,
    // so the shadow halo reads as uniform thickness all the way around.
    val glowStartOffset = mainStartOffset
    val glowSweep = mainSweep

    rotate(degrees = rotationDegrees, pivot = center) {
        if (glowSweep > 0f) {
            drawArc(
                brush = glowBrush,
                startAngle = glowStartOffset,
                sweepAngle = glowSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = glowStrokeWidth, cap = StrokeCap.Round),
            )
        }
        if (mainSweep > 0f) {
            drawArc(
                brush = mainBrush,
                startAngle = mainStartOffset,
                sweepAngle = mainSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}

private fun DrawScope.drawHourDots(center: Offset, radius: Float, hoursComplete: Int, theme: AppTheme) {
    val rDot = radius - 42.dp.toPx()
    for (i in 0 until 5) {
        val active = i < hoursComplete
        val angle = (i / 5f) * (2f * Math.PI.toFloat()) - (Math.PI.toFloat() / 2f)
        val x = center.x + rDot * cos(angle)
        val y = center.y + rDot * sin(angle)
        drawCircle(
            color = if (active) theme.accent else theme.hourDotInactive,
            radius = if (active) 3.dp.toPx() else 2.dp.toPx(),
            center = Offset(x, y),
        )
    }
}

private fun DrawScope.drawKnob(
    center: Offset,
    radius: Float,
    fraction: Float,
    theme: AppTheme,
    dimmed: Boolean,
) {
    val angle = fraction * (2f * Math.PI.toFloat()) - (Math.PI.toFloat() / 2f)
    val kx = center.x + radius * cos(angle)
    val ky = center.y + radius * sin(angle)

    val alpha = if (dimmed) 0.6f else 1f
    drawCircle(
        color = theme.accent.copy(alpha = 0.15f * alpha),
        radius = 16.dp.toPx(),
        center = Offset(kx, ky),
    )
    drawCircle(
        color = theme.accent.copy(alpha = 0.45f * alpha),
        radius = 12.dp.toPx(),
        center = Offset(kx, ky + 2.dp.toPx()),
    )
    drawCircle(
        color = theme.knobBody.copy(alpha = alpha),
        radius = 10.dp.toPx(),
        center = Offset(kx, ky),
    )
    drawCircle(
        color = theme.accent.copy(alpha = alpha),
        radius = 10.dp.toPx(),
        center = Offset(kx, ky),
        style = Stroke(width = 2.dp.toPx()),
    )
    drawCircle(
        color = theme.accent.copy(alpha = alpha),
        radius = 3.dp.toPx(),
        center = Offset(kx, ky),
    )
}
