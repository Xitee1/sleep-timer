package dev.xitee.sleeptimer.feature.timer.timer.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.xitee.sleeptimer.feature.timer.theme.appTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
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
     *
     * Die Phasen schalten weiter, sobald ihre Animationen fertig sind — bewusst kein
     * wall-clock `delay()`: Animatable respektiert den System-Animator-Duration-Scale
     * (MotionDurationScale), `delay()` nicht; bei z.B. 2x würde die State-Machine sonst
     * mitten im Flug auf Impact springen. So leben die Dauern außerdem nur in den Specs.
     *
     * @param startIconRotationDeg Ausgangswinkel des Icons — die Orientierungs-Rotation
     *                             des Button-Icons, damit die Übergabe vom `PlayButton`
     *                             ans Overlay auch in Landscape nahtlos ist.
     * @param targetIconRotationDeg Grad, auf den das Play-Icon während Crouch rotieren
     *                              soll (der Winkel zum Dial-Zentrum; siehe TimerScreen).
     */
    fun launch(startIconRotationDeg: Float, targetIconRotationDeg: Float) {
        if (phase != LaunchPhase.Idle) return
        // Synchron setzen, nicht erst im Job: der Idle-Guard oben wirkt damit auch
        // gegen einen zweiten Tap im selben Frame (Job-Start ist dispatched).
        phase = LaunchPhase.Crouch
        val previousJob = currentJob
        currentJob = scope.launch {
            // Ein evtl. noch ausstehender reset()-Snap darf nicht in diese Choreographie
            // hineinschneiden — erst abwarten, dann von sauberen Idle-Werten starten.
            previousJob?.cancelAndJoin()
            snapToIdleValues()
            iconRotationDeg.snapTo(startIconRotationDeg)

            // Phase 1: Crouch (140ms)
            val crouchSpec = tween<Float>(140, easing = FastOutSlowInEasing)
            coroutineScope {
                launch { buttonScale.animateTo(0.92f, crouchSpec) }
                launch { iconRotationDeg.animateTo(targetIconRotationDeg, crouchSpec) }
                launch { iconScale.animateTo(0.9f, crouchSpec) }
            }

            // Phase 2: Launch (420ms) — drei Segmente mit ease-in-out pro Segment.
            // Waypoints: Travel 0 → 0.28 (30%) → 0.84 (80%) → 1.0 (100%),
            // Scale 0.9 → 1.1 → 0.9 → 0.5. An den Segmentgrenzen fällt die Velocity
            // nahezu auf Null (ease-out des einen, ease-in des nächsten) — das
            // erzeugt die charakteristischen „Hang"-Momente.
            phase = LaunchPhase.Launch
            coroutineScope {
                launch { buttonScale.animateTo(1f, tween(180)) }
                launch {
                    iconTravel.animateTo(0.28f, tween(126, easing = FastOutSlowInEasing))
                    iconTravel.animateTo(0.84f, tween(210, easing = FastOutSlowInEasing))
                    iconTravel.animateTo(1f, tween(84, easing = FastOutSlowInEasing))
                }
                launch {
                    iconScale.animateTo(1.1f, tween(126, easing = FastOutSlowInEasing))
                    iconScale.animateTo(0.9f, tween(210, easing = FastOutSlowInEasing))
                    iconScale.animateTo(0.5f, tween(84, easing = FastOutSlowInEasing))
                }
            }

            // Phase 3: Impact (600ms). Länger als der Prototyp-Impact (260ms)
            // weil wir die Shockwave-Expansion in derselben Pulse-Kurve steuern — dort
            // nutzt der Prototyp separate 900ms CSS-Animations die die Phase überdauern.
            phase = LaunchPhase.Impact
            val impactSpec = tween<Float>(600, easing = CubicBezierEasing(0.12f, 0.85f, 0.3f, 1f))
            coroutineScope {
                launch {
                    buttonScale.animateTo(
                        1.04f,
                        tween(130, easing = CubicBezierEasing(0.2f, 1.8f, 0.4f, 1f)),
                    )
                    buttonScale.animateTo(1f, tween(170))
                }
                launch { impactPulse.animateTo(1f, impactSpec) }
            }

            // Zurück auf Idle (snap, nicht animiert, weil nächstes Frame den echten Running-State hat).
            snapToIdleValues()
            phase = LaunchPhase.Idle
        }
    }

    /**
     * Bricht eine laufende Animation ab und snapt alle Werte auf Idle-Defaults zurück.
     * `phase` wird synchron zurückgesetzt; die Werte snappen in einem Job, den ein
     * nachfolgendes launch() via cancelAndJoin abwartet — der Snap kann also nie in
     * eine neue Choreographie hineinschneiden.
     */
    fun reset() {
        val previousJob = currentJob
        currentJob = scope.launch {
            previousJob?.cancelAndJoin()
            snapToIdleValues()
        }
        phase = LaunchPhase.Idle
    }

    private suspend fun snapToIdleValues() {
        buttonScale.snapTo(1f)
        iconRotationDeg.snapTo(0f)
        iconTravel.snapTo(0f)
        iconScale.snapTo(1f)
        impactPulse.snapTo(0f)
    }
}

@Composable
fun rememberLaunchAnimationController(): LaunchAnimationController {
    val scope = rememberCoroutineScope()
    return remember(scope) { LaunchAnimationController(scope) }
}

/**
 * Overlay, das das fliegende Play-Icon während Crouch + Launch rendert: es reist vom
 * Button zur Dial-Mitte mit einem weichen Accent-Glow zur Sichtbarkeit vor dem dunklen
 * Hintergrund. Im Idle (und während der Timer läuft) zeichnet der `PlayButton` sein
 * Icon selbst — das Overlay übernimmt nur für die Flug-Phasen, in denen der Button
 * sein Play-Icon per Alpha versteckt. Im Impact ist das Icon „eingeschlagen" und
 * unsichtbar (Dial-Effekte übernehmen).
 *
 * Alle Animatable-Reads passieren in graphicsLayer-/Draw-Lambdas, damit jeder
 * Animations-Frame nur ein Layer-Update bzw. Redraw ist, keine Recomposition.
 */
@Composable
fun LaunchOverlay(
    controller: LaunchAnimationController,
    buttonCenter: Offset,
    dialCenter: Offset,
) {
    val phase = controller.phase
    if (phase != LaunchPhase.Crouch && phase != LaunchPhase.Launch) return
    // Beide Positionen sind vor controller.launch() gegated (siehe onToggle) —
    // dieser Guard ist nur ein Sicherheitsnetz.
    if (buttonCenter == Offset.Zero || dialCenter == Offset.Zero) return

    val theme = appTheme()

    // Trail: leuchtende Bahn vom Button bis zur aktuellen Icon-Position. Nur während
    // des Flugs gerendert. Das ist das wesentliche „Wow"-Element — ohne Trail wirkt
    // das fliegende Icon wie ein simples Schieben; mit Trail wird daraus ein Rocket-
    // Launch mit Abgasspur. Width konstant, Alpha-Kurve wie im Prototyp: steigt bis
    // ~40% Flugzeit auf Peak, fadet in den letzten 60% aus.
    if (phase == LaunchPhase.Launch) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val travel = controller.iconTravel.value
            if (travel <= 0.02f) return@Canvas
            val trailAlpha = when {
                travel < 0.4f -> travel / 0.4f
                else -> (1f - (travel - 0.4f) / 0.6f).coerceAtLeast(0f)
            }
            if (trailAlpha <= 0f) return@Canvas
            val trailHead = lerp(buttonCenter, dialCenter, travel)
            drawLine(
                brush = Brush.linearGradient(
                    0f to Color.Transparent,
                    0.25f to theme.accent.copy(alpha = 0.35f * trailAlpha),
                    0.75f to theme.accent.copy(alpha = 1f * trailAlpha),
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

    // Icon + runder Accent-Glow direkt dahinter. Container ist 60dp, Icon 34dp.
    Box(
        modifier = Modifier
            .size(60.dp)
            .graphicsLayer {
                val travel = controller.iconTravel.value
                val current = lerp(buttonCenter, dialCenter, travel)
                val halfPx = 30.dp.toPx()
                translationX = current.x - halfPx
                translationY = current.y - halfPx
                rotationZ = controller.iconRotationDeg.value
                scaleX = controller.iconScale.value
                scaleY = controller.iconScale.value
                // Icon fadet über die letzten 20% des Fluges aus — matched den Prototyp.
                alpha = if (travel < 0.8f) 1f else (1f - (travel - 0.8f) / 0.2f).coerceIn(0f, 1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(60.dp)) {
            // Glow um das Icon herum — wächst mit Entfernung vom Button.
            val glowAlpha = controller.iconTravel.value.coerceIn(0f, 1f) * 0.9f
            if (glowAlpha <= 0f) return@Canvas
            drawCircle(
                brush = Brush.radialGradient(
                    0f to theme.accent.copy(alpha = glowAlpha),
                    1f to theme.accent.copy(alpha = 0f),
                    radius = size.minDimension / 2f,
                ),
                radius = size.minDimension / 2f,
            )
        }
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = theme.accentInk,
            modifier = Modifier.size(34.dp),
        )
    }
}
