package dev.xitee.sleeptimer.feature.timer.timer.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.StrokeCap
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

            // Phase 2: Launch (140–560ms, 420ms) — 1:1 nach Prototyp. Scale-Kurve
            // 1.0 → 1.1 (30%) → 0.9 (80%) → 0.5 (100%): kurzer Windup beim Abheben,
            // dann perspektivische Verkleinerung zur Dial-Mitte. Travel-Kurve kommt
            // aus der Material-Easing (langsamer Start, schnelle Mitte, weiches Ende).
            phase = LaunchPhase.Launch
            val launchEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
            launch { buttonScale.animateTo(1f, tween(180)) }
            launch { iconTravel.animateTo(1f, tween(420, easing = launchEasing)) }
            launch {
                // Start ist 0.9 (vom Crouch). 3-stufig matched die Prototyp-Keyframes.
                iconScale.animateTo(1.1f, tween(126, easing = launchEasing))
                iconScale.animateTo(0.9f, tween(210, easing = launchEasing))
                iconScale.animateTo(0.5f, tween(84, easing = launchEasing))
            }
            delay(420)

            // Phase 3: Impact (560–1160ms, 600ms). Länger als der Prototyp-Impact (260ms)
            // weil wir die Shockwave-Expansion in derselben Pulse-Kurve steuern — dort
            // nutzt der Prototyp separate 900ms CSS-Animations die die Phase überdauern.
            phase = LaunchPhase.Impact
            val impactEasing = CubicBezierEasing(0.12f, 0.85f, 0.3f, 1f)
            val impactSpec = tween<Float>(600, easing = impactEasing)
            launch {
                buttonScale.animateTo(
                    1.04f,
                    tween(130, easing = CubicBezierEasing(0.2f, 1.8f, 0.4f, 1f)),
                )
                buttonScale.animateTo(1f, tween(170))
            }
            launch { impactPulse.animateTo(1f, impactSpec) }
            delay(600)

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
    // Icon fadet über die letzten 20% des Fluges aus — matched den Prototyp.
    val alphaValue =
        if (travel < 0.8f) 1f else (1f - (travel - 0.8f) / 0.2f).coerceIn(0f, 1f)
    // Glow um das Icon herum — wächst mit Entfernung vom Button.
    val glowAlpha = travel.coerceIn(0f, 1f) * 0.9f

    val currentX = buttonCenter.x + (dialCenter.x - buttonCenter.x) * travel
    val currentY = buttonCenter.y + (dialCenter.y - buttonCenter.y) * travel

    // Trail: leuchtende Bahn vom Button bis zur aktuellen Icon-Position. Nur während
    // des Flugs gerendert. Das ist das wesentliche „Wow"-Element — ohne Trail wirkt
    // das fliegende Icon wie ein simples Schieben; mit Trail wird daraus ein Rocket-
    // Launch mit Abgasspur. Width konstant, Alpha-Kurve wie im Prototyp: steigt bis
    // ~40% Flugzeit auf Peak, fadet in den letzten 60% aus.
    if (phase == LaunchPhase.Launch && travel > 0.02f && dialCenter != Offset.Zero) {
        val trailAlpha = when {
            travel < 0.4f -> travel / 0.4f
            else -> (1f - (travel - 0.4f) / 0.6f).coerceAtLeast(0f)
        }
        if (trailAlpha > 0f) {
            val trailHead = Offset(currentX, currentY)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    brush = Brush.linearGradient(
                        0f to Color.Transparent,
                        0.25f to glowColor.copy(alpha = 0.35f * trailAlpha),
                        0.75f to glowColor.copy(alpha = 1f * trailAlpha),
                        1f to Color.White.copy(alpha = trailAlpha),
                        start = buttonCenter,
                        end = trailHead,
                    ),
                    start = buttonCenter,
                    end = trailHead,
                    strokeWidth = 10.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }

    // Icon + runder Accent-Glow direkt dahinter. Container ist 60dp, Icon 34dp.
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
