package dev.mato.sleeptimer.feature.timer.timer.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularDial(
    state: CircularDialState,
    isRunning: Boolean,
    progress: Float,
    hapticEnabled: Boolean,
    onMinutesChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryVariant = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val thumbColor = MaterialTheme.colorScheme.onSurface
    val hapticFeedback = LocalHapticFeedback.current

    var lastReportedMinutes by remember { mutableStateOf(state.totalMinutes) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(24.dp),
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
            val strokeWidth = 16.dp.toPx()
            val thumbRadius = 14.dp.toPx()
            val radius = (size.minDimension - strokeWidth - thumbRadius * 2) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Draw background track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            if (isRunning) {
                // When running, show progress arc
                drawProgressArc(center, radius, strokeWidth, progress, primary, primaryVariant)
            } else {
                // When idle, show selected time arcs
                drawSelectionArcs(
                    center = center,
                    radius = radius,
                    strokeWidth = strokeWidth,
                    state = state,
                    primary = primary,
                    primaryVariant = primaryVariant,
                    trackColor = trackColor,
                )

                // Draw thumb
                val thumbAngleRad = Math.toRadians((state.angleDegrees - 90.0)).toFloat()
                val thumbX = center.x + radius * cos(thumbAngleRad)
                val thumbY = center.y + radius * sin(thumbAngleRad)

                // Thumb shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.2f),
                    radius = thumbRadius + 2.dp.toPx(),
                    center = Offset(thumbX + 1.dp.toPx(), thumbY + 2.dp.toPx()),
                )
                // Thumb
                drawCircle(
                    color = thumbColor,
                    radius = thumbRadius,
                    center = Offset(thumbX, thumbY),
                )
            }
        }
    }
}

private fun DrawScope.drawProgressArc(
    center: Offset,
    radius: Float,
    strokeWidth: Float,
    progress: Float,
    primary: Color,
    primaryVariant: Color,
) {
    val sweepAngle = 360f * progress
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)

    drawArc(
        brush = Brush.sweepGradient(
            colors = listOf(primaryVariant, primary, primaryVariant),
            center = center,
        ),
        startAngle = -90f,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawSelectionArcs(
    center: Offset,
    radius: Float,
    strokeWidth: Float,
    state: CircularDialState,
    primary: Color,
    primaryVariant: Color,
    trackColor: Color,
) {
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)

    // Draw completed revolutions as full circles with decreasing opacity
    for (rev in 0 until state.revolutions) {
        val alpha = 0.3f + (rev.toFloat() / state.maxRevolutions) * 0.3f
        drawArc(
            color = primaryVariant.copy(alpha = alpha),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }

    // Draw current revolution arc
    val currentSweep = state.angleDegrees
    if (currentSweep > 0f) {
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(primaryVariant, primary),
                center = center,
            ),
            startAngle = -90f,
            sweepAngle = currentSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }

    // Draw tick marks every 5 minutes
    for (i in 0 until 12) {
        val tickAngle = (i * 30f) - 90f
        val tickAngleRad = Math.toRadians(tickAngle.toDouble()).toFloat()
        val innerRadius = radius - strokeWidth / 2f - 4.dp.toPx()
        val outerRadius = radius - strokeWidth / 2f + 4.dp.toPx()
        val startPoint = Offset(
            center.x + innerRadius * cos(tickAngleRad),
            center.y + innerRadius * sin(tickAngleRad),
        )
        val endPoint = Offset(
            center.x + outerRadius * cos(tickAngleRad),
            center.y + outerRadius * sin(tickAngleRad),
        )
        drawLine(
            color = trackColor.copy(alpha = 0.5f),
            start = startPoint,
            end = endPoint,
            strokeWidth = 2.dp.toPx(),
        )
    }
}
