package dev.xitee.sleeptimer.feature.timer.timer.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
    // Icon-Scale während des Fluges (1.1 → 0.2 für Perspektivillusion).
    val iconScale = Animatable(1f)
    // Crouch-Intensität 0..1, steuert Button-Schrumpfung im PlayButton.
    val crouchProgress = Animatable(0f)
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
            launch { crouchProgress.animateTo(1f, crouchSpec) }
            delay(140)

            // Phase 2: Launch (140–560ms)
            phase = LaunchPhase.Launch
            val launchEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
            val launchSpec = tween<Float>(420, easing = launchEasing)
            launch { buttonScale.animateTo(1f, tween(180)) }
            launch { iconTravel.animateTo(1f, launchSpec) }
            // Icon scale: 1.0 → 1.1 → 0.2 across the flight (rough approximation using two segments).
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
            crouchProgress.snapTo(0f)
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

@Composable
fun LaunchOverlay(
    controller: LaunchAnimationController,
    buttonCenter: Offset,
    dialCenter: Offset,
    accentColor: Color,
) {
    // Nur während der Flug-Phase rendern. In Crouch ist das Icon noch im Button,
    // in Impact ist es „eingeschlagen" (und die Dial-Effekte übernehmen).
    if (controller.phase != LaunchPhase.Launch) return
    // Warten bis beide Positionen gemessen wurden — sonst würde das Icon in Frame 1
    // kurz bei (0,0) aufblitzen.
    if (buttonCenter == Offset.Zero || dialCenter == Offset.Zero) return

    val travel = controller.iconTravel.value
    val iconScaleValue = controller.iconScale.value
    // Fade-out gegen Ende des Fluges, damit der Impact „nahtlos" übernimmt.
    val alphaValue =
        if (travel < 0.85f) 1f else (1f - (travel - 0.85f) / 0.15f).coerceIn(0f, 1f)

    val currentX = buttonCenter.x + (dialCenter.x - buttonCenter.x) * travel
    val currentY = buttonCenter.y + (dialCenter.y - buttonCenter.y) * travel

    Icon(
        imageVector = Icons.Default.PlayArrow,
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier
            .size(34.dp)
            .graphicsLayer {
                val halfPx = 17.dp.toPx() // Icon ist 34dp; Center-Offset ist die Hälfte.
                translationX = currentX - halfPx
                translationY = currentY - halfPx
                rotationZ = controller.iconRotationDeg.value
                scaleX = iconScaleValue
                scaleY = iconScaleValue
                alpha = alphaValue
            },
    )
}
