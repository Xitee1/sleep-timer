package dev.mato.sleeptimer.feature.timer.timer.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import dev.mato.sleeptimer.feature.timer.theme.DesignTokens
import kotlin.math.cos
import kotlin.math.sin

/**
 * The tactile sleep-timer dial. Drag anywhere inside to set the time; each full revolution
 * adds one hour (up to 5 h). Minute ticks on the outer ring, filled hour dots on the inner
 * ring, lavender progress arc with glow, and a halo'd knob.
 */
@Composable
fun CircularDial(
    state: CircularDialState,
    isRunning: Boolean,
    runningMinutes: Float,
    hapticEnabled: Boolean,
    onMinutesChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var lastReportedMinutes by remember { mutableStateOf(state.totalMinutes) }

    // Source of truth for what the ring/knob should show.
    val displayMinutes: Float = if (isRunning) runningMinutes else state.totalMinutes.toFloat()
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
                                            hapticFeedback.performHapticFeedback(
                                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove,
                                            )
                                        }
                                        lastReportedMinutes = state.totalMinutes
                                        onMinutesChanged(state.totalMinutes)
                                    }
                                },
                                onDragEnd = {
                                    state.onDragEnd()
                                    onMinutesChanged(state.totalMinutes)
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

            // Outer glow (fades a bit while idle; softer when running)
            val glowOuter = radius + strokeWidth * 0.5f + 8.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    0.65f to Color.Transparent,
                    1.00f to DesignTokens.Accent.copy(alpha = if (isRunning) 0.28f else 0.14f),
                    center = center,
                    radius = glowOuter,
                ),
                radius = glowOuter,
                center = center,
            )

            // Dial plate: faint top-to-bottom white gradient + inner well
            drawCircle(
                brush = Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.06f),
                    1.0f to Color.White.copy(alpha = 0.015f),
                    startY = center.y - radius,
                    endY = center.y + radius,
                ),
                radius = radius + strokeWidth * 0.5f,
                center = center,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = radius + strokeWidth * 0.5f,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.25f),
                radius = radius - strokeWidth * 0.5f - 6.dp.toPx(),
                center = center,
            )

            // Tick marks (60, with every 5th accented)
            drawTicks(center, radius, strokeWidth)

            // Background ring
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Progress arc (current revolution 0..1)
            if (ringFraction > 0f) {
                drawProgressArc(
                    center = center,
                    radius = radius,
                    strokeWidth = strokeWidth,
                    fraction = ringFraction,
                    accent = DesignTokens.Accent,
                )
            }

            // Inner hour dots
            drawHourDots(center, radius, hoursComplete)

            // Knob
            drawKnob(
                center = center,
                radius = radius,
                fraction = ringFraction,
                accent = DesignTokens.Accent,
                dimmed = isRunning,
            )
        }
    }
}

private fun DrawScope.drawTicks(center: Offset, radius: Float, strokeWidth: Float) {
    for (i in 0 until 60) {
        val big = i % 5 == 0
        val angle = (i / 60f) * (2f * Math.PI.toFloat()) - (Math.PI.toFloat() / 2f)
        val inner = radius - strokeWidth * 0.5f - (if (big) 10.dp.toPx() else 6.dp.toPx())
        val outer = radius - strokeWidth * 0.5f - (if (big) 3.dp.toPx() else 3.dp.toPx())
        val x1 = center.x + inner * cos(angle)
        val y1 = center.y + inner * sin(angle)
        val x2 = center.x + outer * cos(angle)
        val y2 = center.y + outer * sin(angle)
        drawLine(
            color = if (big) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
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
    accent: Color,
) {
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = Size(radius * 2, radius * 2)
    val sweep = 360f * fraction.coerceIn(0f, 1f)

    // Glow underlay — widened stroke with low alpha
    drawArc(
        color = accent.copy(alpha = 0.35f),
        startAngle = -90f,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth + 8.dp.toPx(), cap = StrokeCap.Round),
    )
    drawArc(
        color = accent,
        startAngle = -90f,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawHourDots(center: Offset, radius: Float, hoursComplete: Int) {
    val rDot = radius - 42.dp.toPx()
    for (i in 0 until 5) {
        val active = i < hoursComplete
        val angle = (i / 5f) * (2f * Math.PI.toFloat()) - (Math.PI.toFloat() / 2f)
        val x = center.x + rDot * cos(angle)
        val y = center.y + rDot * sin(angle)
        drawCircle(
            color = if (active) DesignTokens.Accent else Color.White.copy(alpha = 0.18f),
            radius = if (active) 3.dp.toPx() else 2.dp.toPx(),
            center = Offset(x, y),
        )
    }
}

private fun DrawScope.drawKnob(
    center: Offset,
    radius: Float,
    fraction: Float,
    accent: Color,
    dimmed: Boolean,
) {
    val angle = fraction * (2f * Math.PI.toFloat()) - (Math.PI.toFloat() / 2f)
    val kx = center.x + radius * cos(angle)
    val ky = center.y + radius * sin(angle)

    val alpha = if (dimmed) 0.6f else 1f
    // halo
    drawCircle(
        color = accent.copy(alpha = 0.15f * alpha),
        radius = 16.dp.toPx(),
        center = Offset(kx, ky),
    )
    // drop shadow
    drawCircle(
        color = accent.copy(alpha = 0.45f * alpha),
        radius = 12.dp.toPx(),
        center = Offset(kx, ky + 2.dp.toPx()),
    )
    // body
    drawCircle(
        color = Color.White.copy(alpha = alpha),
        radius = 10.dp.toPx(),
        center = Offset(kx, ky),
    )
    // accent ring
    drawCircle(
        color = accent.copy(alpha = alpha),
        radius = 10.dp.toPx(),
        center = Offset(kx, ky),
        style = Stroke(width = 2.dp.toPx()),
    )
    // center dot
    drawCircle(
        color = accent.copy(alpha = alpha),
        radius = 3.dp.toPx(),
        center = Offset(kx, ky),
    )
}

