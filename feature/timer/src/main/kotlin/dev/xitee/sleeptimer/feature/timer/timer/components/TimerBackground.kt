package dev.xitee.sleeptimer.feature.timer.timer.components

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.xitee.sleeptimer.feature.timer.theme.AppTheme
import dev.xitee.sleeptimer.feature.timer.theme.appTheme
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/**
 * Full-screen immersive background. Either a vertical gradient + soft aurora blobs
 * (immersive themes) or a plain solid surface (Basic theme). Optionally overlays a
 * drifting starfield when [starsEnabled] is true and the current theme allows it.
 */
@Composable
fun TimerBackground(
    animating: Boolean,
    starsEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val theme = appTheme()
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    0.00f to theme.bgTop,
                    0.45f to theme.bgMid,
                    1.00f to theme.bgBot,
                ),
            )
            if (theme.auroraColor.alpha > 0f) {
                drawAuroraBlob(
                    center = Offset(size.width * -0.10f, size.height * -0.05f),
                    radius = size.minDimension * 0.55f,
                    color = theme.auroraColor,
                )
                drawAuroraBlob(
                    center = Offset(size.width * 1.05f, size.height * 1.00f),
                    radius = size.minDimension * 0.70f,
                    color = theme.auroraColor.copy(alpha = theme.auroraColor.alpha * 0.8f),
                )
            }
        }

        if (starsEnabled && theme.allowStars) {
            StarField(animating = animating, theme = theme)
        }

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
 * linear velocity and screen wrap-around. Idle drifts barely; running accelerates ×2.5
 * and adds a per-star opacity twinkle. Ramps over ~5 s.
 */
@Composable
private fun StarField(animating: Boolean, theme: AppTheme) {
    val configuration = LocalConfiguration.current
    val screenMinDp = min(configuration.screenWidthDp, configuration.screenHeightDp)
    val sizeScale = (screenMinDp / 360f).coerceIn(0.9f, 1.4f)
    val starCount = (50 * sizeScale).toInt().coerceIn(40, 90)

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
                speedDp = 0.3f + r(4) * 0.7f,
                angle = r(5) * (2f * Math.PI.toFloat()),
                twinkleFreq = 0.8f + r(6) * 1.6f,
                twinklePhase = r(7) * (2f * Math.PI.toFloat()),
            )
        }
    }

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

    val drive = remember { Animatable(1f) }
    val twinkle = remember { Animatable(0f) }
    LaunchedEffect(animating) {
        val spec = tween<Float>(durationMillis = 5_000, easing = LinearEasing)
        drive.animateTo(if (animating) 2.5f else 1f, spec)
    }
    LaunchedEffect(animating) {
        val spec = tween<Float>(durationMillis = 5_000, easing = LinearEasing)
        twinkle.animateTo(if (animating) 1f else 0f, spec)
    }

    // Read density once from the composable body — previous impl wrote this from inside
    // a Canvas draw lambda while the LaunchedEffect below was reading it, which is the
    // classic "state read during draw" Compose anti-pattern and can cause invalidation loops.
    val dpToPx = with(LocalDensity.current) { 1.dp.toPx() }

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
        @Suppress("UNUSED_EXPRESSION") frameTick

        if (!initialized) return@Canvas

        val coreBase = 0.7.dp.toPx() * sizeScale
        val haloBase = 1.6.dp.toPx() * sizeScale
        val tw = twinkle.value
        val tSec = frameTick / 1_000_000_000f
        val star = theme.starColor

        for (i in seeds.indices) {
            val s = seeds[i]
            val cx = positions[i * 2]
            val cy = positions[i * 2 + 1]
            val core = coreBase * s.sizeMul
            val halo = haloBase * s.sizeMul

            val pulse = (sin(tSec * s.twinkleFreq + s.twinklePhase) * 0.5f + 0.5f)
            val dim = 1f - tw * 0.6f * (1f - pulse)
            val alpha = (s.opacity * dim).coerceIn(0f, 1f)

            drawCircle(
                color = star.copy(alpha = alpha * 0.25f),
                radius = halo,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = star.copy(alpha = alpha),
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
