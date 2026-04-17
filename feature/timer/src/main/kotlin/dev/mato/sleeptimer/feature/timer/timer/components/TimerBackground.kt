package dev.mato.sleeptimer.feature.timer.timer.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.mato.sleeptimer.feature.timer.theme.DesignTokens
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/**
 * Full-screen immersive background: vertical midnight gradient + two soft aurora blobs
 * + a starfield that drifts gently at idle and accelerates with a twinkle when
 * [animating] is true.
 */
@Composable
fun TimerBackground(
    animating: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    0.00f to DesignTokens.BgTop,
                    0.45f to DesignTokens.BgMid,
                    1.00f to DesignTokens.BgBot,
                ),
            )
            drawAuroraBlob(
                center = Offset(size.width * -0.10f, size.height * -0.05f),
                radius = size.minDimension * 0.55f,
                color = Color(0x40C9B8FF),
            )
            drawAuroraBlob(
                center = Offset(size.width * 1.05f, size.height * 1.00f),
                radius = size.minDimension * 0.70f,
                color = Color(0x33C9B8FF),
            )
        }

        StarField(animating = animating)

        content()
    }
}

private fun DrawScope.drawAuroraBlob(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius,
        ),
        center = center,
        radius = radius,
    )
}

/**
 * Starfield styled after tsparticles' "stars" preset: uniform small dots with per-star
 * linear velocity and screen wrap-around. Idle = slow drift, running = ~4× faster with
 * a subtle opacity twinkle. Both drive & twinkle ramp over ~5 s when [animating] flips.
 */
@Composable
private fun StarField(animating: Boolean) {
    val configuration = LocalConfiguration.current
    val screenMinDp = min(configuration.screenWidthDp, configuration.screenHeightDp)
    val sizeScale = (screenMinDp / 360f).coerceIn(0.9f, 1.4f)
    val starCount = (110 * sizeScale).toInt().coerceIn(90, 200)

    val seeds = remember(starCount) {
        List(starCount) { i ->
            val r = { k: Int ->
                val v = sin(i * 12.9898 + k * 78.233) * 43758.5453
                (v - floor(v)).toFloat()
            }
            StarSeed(
                initX = r(0),
                initY = r(1),
                opacity = 0.35f + r(2) * 0.55f,
                sizeMul = 0.8f + r(3) * 0.5f,
                // idle speed: 2..6 dp/s; with drive multiplier up to ~4× when running
                speedDp = 2f + r(4) * 4f,
                angle = r(5) * (2f * Math.PI.toFloat()),
                twinkleFreq = 0.8f + r(6) * 1.6f, // 0.8..2.4 Hz
                twinklePhase = r(7) * (2f * Math.PI.toFloat()),
            )
        }
    }

    // Current px positions; initialized lazily once we know the canvas size.
    val positions = remember(seeds) { FloatArray(seeds.size * 2) }
    var initialized by remember(seeds) { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(canvasSize, seeds) {
        if (canvasSize.width > 0 && canvasSize.height > 0 && !initialized) {
            for (i in seeds.indices) {
                positions[i * 2] = seeds[i].initX * canvasSize.width
                positions[i * 2 + 1] = seeds[i].initY * canvasSize.height
            }
            initialized = true
        }
    }

    // 1.0 idle, ~4.0 running — smoothly ramped.
    val drive = remember { Animatable(1f) }
    // 0 idle, 1 running — scales the twinkle depth.
    val twinkle = remember { Animatable(0f) }
    LaunchedEffect(animating) {
        val spec = tween<Float>(durationMillis = 5_000, easing = LinearEasing)
        drive.animateTo(if (animating) 4f else 1f, spec)
    }
    LaunchedEffect(animating) {
        val spec = tween<Float>(durationMillis = 5_000, easing = LinearEasing)
        twinkle.animateTo(if (animating) 1f else 0f, spec)
    }

    // Will hold px/dp conversion; computed inside Canvas, but we need it in the
    // frame loop too — simplest is to compute dp→px via a saved density-scale.
    var dpToPx by remember { mutableStateOf(1f) }

    var frameTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(initialized) {
        if (!initialized) return@LaunchedEffect
        var last = 0L
        while (true) {
            val now = withFrameNanos { it }
            if (last == 0L) {
                last = now
                continue
            }
            val dt = min(0.05f, (now - last) / 1_000_000_000f)
            last = now
            val w = canvasSize.width.toFloat()
            val h = canvasSize.height.toFloat()
            if (w > 0f && h > 0f) {
                val d = drive.value
                val px = dpToPx
                for (i in seeds.indices) {
                    val s = seeds[i]
                    val v = s.speedDp * px * d
                    var x = positions[i * 2] + cos(s.angle) * v * dt
                    var y = positions[i * 2 + 1] + sin(s.angle) * v * dt
                    if (x < 0f) x += w
                    if (x >= w) x -= w
                    if (y < 0f) y += h
                    if (y >= h) y -= h
                    positions[i * 2] = x
                    positions[i * 2 + 1] = y
                }
                frameTick = now
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        dpToPx = 1.dp.toPx()
        // Read frameTick so Compose redraws on every integration step.
        @Suppress("UNUSED_EXPRESSION") frameTick

        if (!initialized) return@Canvas

        val coreBase = 1.1.dp.toPx() * sizeScale
        val haloBase = 2.6.dp.toPx() * sizeScale
        val tw = twinkle.value
        val tSec = frameTick / 1_000_000_000f

        for (i in seeds.indices) {
            val s = seeds[i]
            val cx = positions[i * 2]
            val cy = positions[i * 2 + 1]
            val core = coreBase * s.sizeMul
            val halo = haloBase * s.sizeMul

            // Opacity twinkle: sin in 0..1, only "breathes" the star when running.
            val pulse = (sin(tSec * s.twinkleFreq + s.twinklePhase) * 0.5f + 0.5f)
            // idle: full opacity. running: varies from 0.4×..1× of base.
            val dim = 1f - tw * 0.6f * (1f - pulse)
            val alpha = (s.opacity * dim).coerceIn(0f, 1f)

            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.25f),
                radius = halo,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = core,
                center = Offset(cx, cy),
            )
        }
    }
}

private data class StarSeed(
    val initX: Float,
    val initY: Float,
    val opacity: Float,
    val sizeMul: Float,
    val speedDp: Float,
    val angle: Float,
    val twinkleFreq: Float,
    val twinklePhase: Float,
)
