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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import dev.mato.sleeptimer.feature.timer.theme.DesignTokens
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/**
 * Full-screen immersive background: vertical midnight gradient + two soft aurora blobs
 * + a starfield that slowly drifts when [animating] is true.
 */
@Composable
fun TimerBackground(
    animating: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Base vertical gradient + aurora blobs in a single canvas pass.
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
 * Starfield with per-star random velocity. Ramps smoothly from rest to max drift speed
 * over ~5 s when [animating] turns on, and ramps back down when it turns off.
 * Star size and count scale with the display size.
 */
@Composable
private fun StarField(animating: Boolean) {
    val configuration = LocalConfiguration.current
    val screenMinDp = min(configuration.screenWidthDp, configuration.screenHeightDp)
    // 360 dp phone = 1x, tablets up to ~1.4x.
    val sizeScale = (screenMinDp / 360f).coerceIn(0.9f, 1.4f)
    // Dense starfield. ~90 on a standard phone, more on larger displays.
    val starCount = (90 * sizeScale).toInt().coerceIn(70, 160)

    // Each star: position (0..1), opacity, per-star size jitter.
    val seed = remember(starCount) {
        List(starCount) { i ->
            val r = { k: Int ->
                val v = sin(i * 12.9898 + k * 78.233) * 43758.5453
                (v - floor(v)).toFloat()
            }
            StarSeed(
                x = r(0),
                y = r(1),
                opacity = 0.30f + r(2) * 0.55f,
                sizeMul = 0.8f + r(3) * 0.5f,
            )
        }
    }

    // Stable per-star motion parameters.
    val params = remember(seed) {
        seed.mapIndexed { i, _ ->
            val r = { k: Int ->
                val v = sin((i + 500) * 12.9898 + k * 78.233) * 43758.5453
                (v - floor(v)).toFloat()
            }
            StarParams(
                speed = 4f + r(1) * 10f,
                angle = r(2) * (2f * Math.PI.toFloat()),
                wobbleFreq = 0.10f + r(3) * 0.30f,
                wobbleAmt = 0.60f + r(4) * 1.20f,
                phase = r(5) * 100f,
            )
        }
    }

    // Per-star integrated drift offsets (px).
    val positions = remember(seed) { FloatArray(seed.size * 2) }

    // 0..1 smoothly ramped drive amount.
    val ramp = remember { Animatable(0f) }
    LaunchedEffect(animating) {
        ramp.animateTo(
            targetValue = if (animating) 1f else 0f,
            animationSpec = tween(durationMillis = 5_000, easing = LinearEasing),
        )
    }

    var frameTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            val now = withFrameNanos { it }
            if (last == 0L) {
                last = now
                continue
            }
            val dt = min(0.05f, (now - last) / 1_000_000_000f)
            last = now
            val r = ramp.value
            val eased = r * r * (3f - 2f * r)
            if (eased > 0f) {
                val t = now / 1_000_000_000f
                for (i in seed.indices) {
                    val p = params[i]
                    val dir = p.angle + sin(t * p.wobbleFreq + p.phase) * p.wobbleAmt
                    positions[i * 2] += cos(dir) * p.speed * eased * dt
                    positions[i * 2 + 1] += sin(dir) * p.speed * eased * dt
                }
                frameTick = now
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        // Read frameTick so Compose redraws on every integration step.
        @Suppress("UNUSED_EXPRESSION") frameTick
        val coreBase = 1.1.dp.toPx() * sizeScale
        val haloBase = 2.6.dp.toPx() * sizeScale
        for (i in seed.indices) {
            val s = seed[i]
            val cx = s.x * size.width + positions[i * 2]
            val cy = s.y * size.height + positions[i * 2 + 1]
            val core = coreBase * s.sizeMul
            val halo = haloBase * s.sizeMul
            // faint glow halo — the "twinkle" edge
            drawCircle(
                color = Color.White.copy(alpha = s.opacity * 0.25f),
                radius = halo,
                center = Offset(cx, cy),
            )
            // bright pin-prick core
            drawCircle(
                color = Color.White.copy(alpha = s.opacity),
                radius = core,
                center = Offset(cx, cy),
            )
        }
    }
}

private data class StarSeed(
    val x: Float,
    val y: Float,
    val opacity: Float,
    val sizeMul: Float,
)

private data class StarParams(
    val speed: Float,
    val angle: Float,
    val wobbleFreq: Float,
    val wobbleAmt: Float,
    val phase: Float,
)
