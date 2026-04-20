package dev.xitee.sleeptimer.feature.timer.timer.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class LaunchPhase { Idle, Crouch, Launch, Impact }

/**
 * Orchestriert die Rocket-Launch-Animation in einer Coroutine. Hält alle Animatable-Werte
 * als Public-Properties, damit `TimerScreen` und `LaunchOverlay` sie lesen können.
 *
 * Der Controller weiß nichts vom Service oder Timer-State — er spielt nur die visuelle
 * Choreographie ab.
 */
class LaunchAnimationController(private val scope: CoroutineScope) {
    var phase by mutableStateOf(LaunchPhase.Idle)
        private set

    // 1.0 im Idle, 0.92 auf dem Höhepunkt des Crouch, 1.04 beim Impact-Recoil.
    val buttonScale = Animatable(1f)
    // Absoluter Winkel des Play-Icons in Grad.
    val iconRotationDeg = Animatable(0f)
    // Fortschritt der Icon-Reise: 0 = Button-Center, 1 = Dial-Center.
    val iconTravel = Animatable(0f)
    // Icon-Scale: 1.0 idle, 0.9 crouch (komprimiert), 1.1 → 0.2 während des Fluges.
    val iconScale = Animatable(1f)
    // Impact-Pulse 0..1, wird ans Dial weitergereicht.
    val impactPulse = Animatable(0f)

    private var currentJob: Job? = null

    /**
     * Startet die Animations-Choreographie. Idempotent: wenn bereits nicht-Idle, no-op.
     * @param targetIconRotationDeg Grad, auf den das Play-Icon während Crouch rotieren soll
     *                              (meist der Winkel zum Dial-Zentrum; siehe TimerScreen).
     */
    fun launch(targetIconRotationDeg: Float) {
        if (phase != LaunchPhase.Idle) return
        currentJob = scope.launch {
            // Phase 1: Crouch (0–140ms)
            phase = LaunchPhase.Crouch
            val crouchEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
            val crouchSpec = tween<Float>(140, easing = crouchEasing)
            launch { buttonScale.animateTo(0.92f, crouchSpec) }
            launch { iconRotationDeg.animateTo(targetIconRotationDeg, crouchSpec) }
            launch { iconScale.animateTo(0.9f, crouchSpec) }
            delay(140)

            // Phase 2: Launch (140–560ms)
            phase = LaunchPhase.Launch
            val launchEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
            val launchSpec = tween<Float>(420, easing = launchEasing)
            launch { buttonScale.animateTo(1f, tween(180)) }
            launch { iconTravel.animateTo(1f, launchSpec) }
            // Icon scale: 0.9 → 1.1 → 0.2 across the flight.
            launch {
                iconScale.animateTo(1.1f, tween(120, easing = launchEasing))
                iconScale.animateTo(0.2f, tween(300, easing = launchEasing))
            }
            delay(420)

            // Phase 3: Impact (560–820ms)
            phase = LaunchPhase.Impact
            val impactEasing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)
            val impactSpec = tween<Float>(260, easing = impactEasing)
            launch {
                buttonScale.animateTo(
                    1.04f,
                    tween(130, easing = CubicBezierEasing(0.2f, 1.8f, 0.4f, 1f)),
                )
                buttonScale.animateTo(1f, tween(130))
            }
            launch { impactPulse.animateTo(1f, impactSpec) }
            delay(260)

            // Zurück auf Idle (snap, nicht animiert, weil nächstes Frame den echten Running-State hat).
            reset()
        }
    }

    /**
     * Bricht eine laufende Animation ab und snapt alle Werte auf Idle-Defaults zurück.
     */
    fun reset() {
        currentJob?.cancel()
        currentJob = null
        scope.launch {
            buttonScale.snapTo(1f)
            iconRotationDeg.snapTo(0f)
            iconTravel.snapTo(0f)
            iconScale.snapTo(1f)
            impactPulse.snapTo(0f)
        }
        phase = LaunchPhase.Idle
    }
}

@Composable
fun rememberLaunchAnimationController(): LaunchAnimationController {
    val scope = rememberCoroutineScope()
    return remember(scope) { LaunchAnimationController(scope) }
}

/**
 * Overlay, das das Play-Icon durchgängig rendert: im Idle sitzt es zentriert auf dem
 * Button (dunkles `iconTint` auf Accent-Background), während des Fluges reist es zur
 * Dial-Mitte mit einem weichen Accent-Glow zur Sichtbarkeit vor dem dunklen Hintergrund.
 * Im Impact ist es „eingeschlagen" und nicht mehr sichtbar (Dial-Effekte übernehmen).
 * Während der Timer läuft, übernimmt das Stop-Icon im `PlayButton` via Crossfade.
 */
@Composable
fun LaunchOverlay(
    controller: LaunchAnimationController,
    isRunning: Boolean,
    buttonCenter: Offset,
    dialCenter: Offset,
    iconTint: Color,
    glowColor: Color,
) {
    val phase = controller.phase
    val shouldRender = when (phase) {
        LaunchPhase.Impact -> false
        LaunchPhase.Idle -> !isRunning
        LaunchPhase.Crouch, LaunchPhase.Launch -> true
    }
    if (!shouldRender) return
    // Brauchen mindestens Button-Position; Dial-Position ist nur relevant sobald
    // travel > 0 (wird in onToggle vor controller.launch() gegated).
    if (buttonCenter == Offset.Zero) return

    val travel = controller.iconTravel.value
    val iconScaleValue = controller.iconScale.value
    // Fade-out kurz vor dem Impact, damit der Übergang zum Dial-Effekt weich wirkt.
    val alphaValue =
        if (travel < 0.85f) 1f else (1f - (travel - 0.85f) / 0.15f).coerceIn(0f, 1f)
    // Glow wächst proportional zur Entfernung vom Button: auf dem Button fast unsichtbar,
    // im freien Flug deutlich sichtbar.
    val glowAlpha = travel.coerceIn(0f, 1f) * 0.9f

    val currentX = buttonCenter.x + (dialCenter.x - buttonCenter.x) * travel
    val currentY = buttonCenter.y + (dialCenter.y - buttonCenter.y) * travel

    // Container-Box ist 60dp (Glow-Radius). Icon ist 34dp zentriert darin.
    Box(
        modifier = Modifier
            .size(60.dp)
            .graphicsLayer {
                val halfPx = 30.dp.toPx()
                translationX = currentX - halfPx
                translationY = currentY - halfPx
                rotationZ = controller.iconRotationDeg.value
                scaleX = iconScaleValue
                scaleY = iconScaleValue
                alpha = alphaValue
            },
        contentAlignment = Alignment.Center,
    ) {
        if (glowAlpha > 0f) {
            Canvas(modifier = Modifier.size(60.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to glowColor.copy(alpha = glowAlpha),
                        1f to glowColor.copy(alpha = 0f),
                        radius = size.minDimension / 2f,
                    ),
                    radius = size.minDimension / 2f,
                )
            }
        }
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(34.dp),
        )
    }
}
