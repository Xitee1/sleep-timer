# Play-Button Launch-Animation

**Status:** design approved, ready for implementation planning
**Date:** 2026-04-20

## Context

Der Play-Button ist aktuell ein statisches Play-Icon, das per Crossfade zum Stop-Icon wechselt, sobald der Timer läuft. Ein in Claude Design erstellter Prototyp (`docs/plans/` nicht committed; Referenz-HTML/JSX im Handoff-Bundle) zeigt eine Launch-Animation, bei der das Play-Icon beim Tap als „Rakete" Richtung Dial-Zentrum fliegt und dort auf das Knob-Element einschlägt — visuelles Storytelling für „Timer zündet".

Ziel dieses Specs: Die Prototyp-Animation in Jetpack Compose umsetzen, ohne die Funktion des Dials im Idle-Zustand (Knob ziehbar, Progress-Arc sichtbar) zu verändern.

## Scope

**In-Scope:**
- Eine Animationsvariante (Rocket — gerader Schuss), umgesetzt für Portrait **und** Landscape
- User-Toggle in Settings zum Deaktivieren
- Default-Wert des Toggles berücksichtigt Android System-Reduce-Motion einmalig beim ersten Start
- System-Reduce-Motion wird zur Laufzeit respektiert (überspringt Animation immer)
- Impact-Effekt auf Dial: Shockwave vom Knob, Knob-Pulse, Ring-Boost (wie Prototyp)

**Out-of-Scope:**
- Die Varianten „Arc toss" und „Warp" aus dem Prototyp
- Zusätzliche Haptics (Impact-Vibration) — der bestehende Tap-Haptic im PlayButton bleibt
- Unit-Tests (Projekt hat aktuell keine Tests; Animation ist reine UI-Choreographie)
- Tracking, ob User den Toggle je explizit gesetzt hat (System-Setting-Änderungen post-Install werden nicht auto-reflektiert)

## User Experience

### Animations-Phasen und Timing

Nach Tap auf den Play-Button durchläuft die Animation drei Phasen (1:1 Timings aus dem Prototyp). Nach der letzten Phase fällt der Controller zurück auf `Idle`; der danach sichtbare „Running"-Zustand wird vollständig über `uiState` und den bestehenden `PlayButton`-Crossfade gerendert — der Controller hält keinen eigenen Running-State.

| Phase    | Zeitspanne | Button                                          | Icon                                                                  | Dial                                   |
| -------- | ---------- | ----------------------------------------------- | --------------------------------------------------------------------- | -------------------------------------- |
| Crouch   | 0–140 ms   | scale 1.0 → 0.92                                | rotiert von 0° Richtung Dial, scale 1.0 → 0.9, bleibt im Button       | unverändert                            |
| Launch   | 140–560 ms | scale 0.92 → 1.0                                | wird Overlay, fliegt vom Button-Zentrum zum Dial-Zentrum, Trail hinter| unverändert                            |
| Impact   | 560–820 ms | scale 1.0 → 1.04 (Recoil-Puls, danach zurück)   | unsichtbar (in Dial „aufgegangen")                                    | Knob pulsiert, Ring-Glow verstärkt, 3× Shockwave-Ripple vom Knob |
| (Idle)   | 820 ms+    | Controller auf Idle zurückgesetzt; Button-Rendering läuft über `uiState` (Stop-Icon-Crossfade bei `isRunning == true`) |

### Icon-Rotation (orientation-agnostisch)

Das Play-Icon zeigt im Idle-State nach rechts (Standard-Material-Play-Arrow). Die Ziel-Rotation beim Crouch wird dynamisch berechnet als Winkel zwischen dem Vektor (Button-Zentrum → Dial-Zentrum) und der horizontalen Achse. In Portrait sind Button und Dial vertikal angeordnet, das Icon rotiert auf -90° (zeigt nach oben). In Landscape ist die Anordnung identisch im Physical-Space (die `Column` in `TimerScreen` ist nicht rotiert — nur die Dial-Inhalte selbst via `graphicsLayer`), der Winkel bleibt -90°.

Der Ansatz ist bewusst allgemein gehalten: sollte das Layout sich ändern (z.B. Button seitlich des Dials in einer zukünftigen Variante), bleibt die Animation korrekt, ohne Anpassung.

### Flug-Trajektorie

Linear: `buttonCenter + (dialCenter - buttonCenter) * t`, mit `t` per `cubic-bezier(.4,0,.2,1)` Easing-Kurve von 0 auf 1 über 420 ms. Icon schrumpft parallel von scale 1.1 → 0.2 (Perspektivillusion) und wird bei t≈0.85 unsichtbar (Impact-Einschlag).

### Impact-Zielpunkt

Rocket fliegt zum **Dial-Zentrum**. Der Impact-Effekt (Shockwave, Pulse) wird am **Knob** ausgelöst — narrative Idee: „Rocket schlägt im Dial-Zentrum ein, Energie reist zum Knob und entzündet den Timer". Knob-Position abhängig von aktueller `fraction = (selectedMinutes % 60) / 60`.

### Cancel-Verhalten

Der Button bleibt die gesamte Animation über tappbar. Tap während der Animation ruft `onToggle` auf, welches `viewModel.stopTimer()` triggert (Service-Cancel), sofern die Animation aktiv ist oder `uiState == Running`. Die Animation **läuft visuell weiter bis zum Ende** — es gibt keine Abbruch-Logik auf visueller Ebene. Nach Animation-Ende zeigt der Button Play (weil Timer gestoppt wurde), Dial-State reflektiert Idle.

### Reduce-Motion und Settings-Toggle

Zwei unabhängige Gates, beide müssen `true` liefern, damit die Animation spielt:

1. **System Reduce-Motion:** zur Laufzeit geprüft via `Settings.Global.getFloat(ANIMATOR_DURATION_SCALE)`. Android hat keine eigene API wie iOS' `UIAccessibility.isReduceMotionEnabled`; die „Remove animations"-Accessibility-Toggle in Android setzt intern die drei Animation-Scale-Settings (`ANIMATOR_DURATION_SCALE`, `TRANSITION_ANIMATION_SCALE`, `WINDOW_ANIMATION_SCALE`) auf `0f`. `ANIMATOR_DURATION_SCALE == 0f` deckt daher sowohl die Accessibility-Toggle als auch die Developer-Options-Einstellung ab.
2. **App-Setting `launchAnimationEnabled`:** in DataStore persistiert.

Wenn beide true: Animation spielt. Sonst: normaler bisheriger Crossfade Play→Stop ohne Flug.

**Default-Wert des App-Settings:** Beim ersten Laden (DataStore liefert kein Value zurück) wird der System-Reduce-Motion-Status einmalig gelesen und als `!systemReduceMotion` persistiert. User-Overrides gewinnen danach.

## Architecture

### Neue Dateien

- `feature/timer/src/main/kotlin/.../timer/components/LaunchAnimation.kt`
  - Enthält: `LaunchPhase` Enum (`Idle`, `Crouch`, `Launch`, `Impact`), `LaunchAnimationController` (Animatable-basierte State-Machine), `rememberLaunchAnimationController()`, `LaunchOverlay` Composable

**Warum Overlay statt Icon im Button zu animieren:** Das fliegende Icon muss über den Button hinaus bis zum Dial reisen. Als Child des Buttons würde es geclippt oder das Button-Layout müsste fix groß sein und den halben Screen reservieren. Ein Overlay in der Root-Box des `TimerScreen` hat den ganzen Screen als Canvas, ohne die anderen Komponenten zu beeinflussen.

### Geänderte Dateien

| Datei | Änderung |
|-------|----------|
| `core/data/.../model/UserSettings.kt` | Neues Feld `launchAnimationEnabled: Boolean = true` |
| `core/data/.../repository/SettingsRepositoryImpl.kt` | Neue `Preferences.Key<Boolean>`; `updateLaunchAnimationEnabled(Boolean)`; Default-Initialisierung liest einmalig `AccessibilityManager` beim ersten Mapping |
| `feature/timer/.../settings/SettingsScreen.kt` | Neuer `SettingsToggleRow` unter `starsEnabled`-Toggle |
| `feature/timer/.../settings/SettingsViewModel.kt` | `onLaunchAnimationEnabledChange(Boolean)`-Handler |
| `feature/timer/.../settings/SettingsUiState.kt` | Feld `launchAnimationEnabled: Boolean` |
| `feature/timer/.../timer/TimerScreen.kt` | Miss Button/Dial-Zentrum via `onGloballyPositioned`; Render `LaunchOverlay` im Root-Box; Trigger Phasen; leite `impactPulse` ans Dial; Reduce-Motion-Detection |
| `feature/timer/.../timer/components/CircularDial.kt` | Neuer Parameter `impactPulse: Float = 0f` (0..1); bei >0: Shockwave-Ripple vom Knob, Knob-Glow-Scale erhöht, Ring-Alpha-Boost |
| `feature/timer/.../timer/components/PlayButton.kt` | Neue Parameter `crouchProgress: Float = 0f`, `iconLaunching: Boolean = false`; bei crouchProgress>0: Button-Scale + Icon-Rotation/Scale; bei iconLaunching: Icon unsichtbar |
| `feature/timer/src/main/res/values/strings.xml` | Neuer String für Settings-Toggle |
| `feature/timer/src/main/res/values-de/strings.xml` | Deutsche Übersetzung |

### Controller-API (Vorschlag)

```kotlin
class LaunchAnimationController(private val scope: CoroutineScope) {
    var phase by mutableStateOf(LaunchPhase.Idle)
        private set
    val buttonScale = Animatable(1f)
    val iconRotationDeg = Animatable(0f)
    val iconTravel = Animatable(0f)        // 0 = Button, 1 = Dial
    val iconScale = Animatable(1f)
    val shockwave = Animatable(0f)          // 0 = hidden, 1 = fully expanded
    val knobPulse = Animatable(0f)
    val ringBoost = Animatable(0f)

    /**
     * Löst die Animation aus. Idempotent: wenn phase != Idle, no-op.
     * Durchläuft Crouch → Launch → Impact sequenziell. Nach Impact: phase = Idle,
     * alle Animatable-Werte auf Idle-Defaults (weiche Rückführung, nicht snap).
     */
    fun launch(targetAngleDeg: Float)

    /**
     * Bricht eine laufende Animation hart ab, snapt alle Werte auf Idle-Defaults.
     * Wird aufgerufen bei App-Background oder Composable-Dispose.
     */
    fun reset()
}
```

### Integration in TimerScreen

```kotlin
val density = LocalDensity.current
var buttonCenter by remember { mutableStateOf(Offset.Zero) }
var dialCenter by remember { mutableStateOf(Offset.Zero) }
val controller = rememberLaunchAnimationController()
val animationsEnabled = settings.launchAnimationEnabled && !isSystemReduceMotion()

// im Dial-Container:
Modifier.onGloballyPositioned { dialCenter = it.boundsInRoot().center }

// im Button:
Modifier.onGloballyPositioned { buttonCenter = it.boundsInRoot().center }

// onToggle:
if (isRunning || controller.phase != LaunchPhase.Idle) {
    viewModel.stopTimer()
} else {
    // Permission-Check wie bisher
    viewModel.startTimer()
    if (animationsEnabled) {
        val angle = angleBetween(buttonCenter, dialCenter)
        controller.launch(angle)
    }
}

// Overlay am Ende des Root-Box:
LaunchOverlay(
    controller = controller,
    buttonCenter = buttonCenter,
    dialCenter = dialCenter,
    themeAccent = appTheme().accent,
)
```

### Koordinaten-Modell

Alle Positionen werden via `layoutCoordinates.boundsInRoot().center` in **Root-Koordinaten** ermittelt. Das Overlay rendert im selben Root-Space. Keine Transformationen über Orientation-Rotation nötig, weil die Container (Button-Row, Dial-Box) physisch am gleichen Platz bleiben — nur der Dial-Inhalt wird via `graphicsLayer` rotiert, was die Container-Bounds nicht beeinflusst.

### Dial-Impact-Rendering

Im `CircularDial.kt` Canvas, nach dem bisherigen `drawKnob`-Call: wenn `impactPulse > 0f`:

- Knob-Aura-Radius animiert von 16dp auf ~60dp, alpha fade von 0.35 auf 0
- 3× konzentrische Ring-Ripples am Knob, je 900ms Dauer mit 0/110/220ms Offset, stroke-Width shrinkt von 6→0.5, alpha 0.9→0
- Ring-Hauptfarbe: `theme.accent`
- `ringFraction` Hauptarc bekommt +0.2 Alpha-Boost für ~400ms (macht den ganzen Ring kurz heller)

Der `impactPulse`-Parameter selbst ist ein Float 0..1, der von `TimerScreen` aus `controller.shockwave.value` (oder ähnlich) durchgereicht wird. Alternative: einzelne Parameter `shockwaveProgress: Float`, `knobPulseProgress: Float`, `ringBoost: Float` für mehr Kontrolle. Entscheidung beim Implementieren: erst mit einem gemeinsamen Float starten, nur splitten wenn Fine-Tuning es erfordert.

## Edge Cases

| Fall | Verhalten |
|------|-----------|
| Animation läuft, App geht in Background | `DisposableEffect`/Lifecycle-Observer ruft `controller.reset()` — Button zeigt den echten State |
| Screen-Rotation während Animation | Composable wird neu erstellt, neuer Controller gestartet, alter cancelled. Animation verloren (akzeptabel) |
| Theme-Wechsel während Animation | `theme.accent` wird zur Draw-Zeit abgegriffen, Farbwechsel live |
| Service startet nicht (Exception) | Animation spielt komplett durch; Button fällt auf Play-Icon zurück, da `uiState` nie Running wird |
| User dreht Dial-Knob direkt nach Tap | Dial-Gestures bleiben aktiv; Knob-Position beim Impact nimmt die aktuelle `fraction` (live berechnet) |
| Notification-Permission-Dialog (Android 13+, erster Start) | Dialog zuerst, keine Animation bei diesem Tap. Erst beim nächsten Start fliegt die Rakete |
| User tappt Play, dann vor Animations-Ende nochmals | Zweiter Tap triggert `stopTimer()`. Animation läuft visuell weiter. Button-Icon am Ende: Play |

## Testing

Keine automatisierten Tests geplant (Projekt hat aktuell keine Tests; Animation ist reine UI).

**Manuelle Verifikation (Debug-Build):**

- Portrait: Tap Play bei 15min → Rocket fliegt nach oben, Impact + Shockwave vom Knob oben am Dial
- Portrait bei 45min: Knob links am Dial → Rocket fliegt trotzdem ins Zentrum, Impact-Effekt am linken Knob
- Portrait bei 90min (Overflow-Ring): Ring komplett gefüllt + Knob an neuer Position — Impact dort
- Landscape: Rocket fliegt zum Dial-Zentrum im Physical-Space (aus User-Perspektive „zur Seite"), Impact korrekt
- Tap während Launch-Phase → Timer stoppt sofort, Animation läuft durch, Button zeigt Play
- Settings-Toggle off → Tap Play → kein Flug, nur Crossfade
- Settings-Toggle on + System-Reduce-Motion on → kein Flug
- Settings-Toggle on + System-Reduce-Motion off → Flug
- Settings-Screen zeigt den neuen Toggle unter „Sterne"
- Deutsche Übersetzung vorhanden
- Build: `./gradlew assembleDebug` und `./gradlew lint` müssen grün bleiben
- Release-Build: `./gradlew assembleRelease` (mit Fallback auf Debug-Signing ohne Keystore) muss grün bleiben

## Open Decisions

- Genaue Wording/Sublabel des Settings-Toggles — festlegen beim Implementieren (siehe Strings-Änderungen; kurze klare Formulierung auf DE+EN)
- Ob `impactPulse` in `CircularDial` als einzelner Float oder drei Parameter splitten — mit einem starten, nur splitten wenn nötig

## References

Die Design-Entscheidungen und Timings stammen aus dem Claude-Design-Handoff-Bundle (temporär unter `/tmp/sleep-timer-design/sleep-timer-start-button-animation/` während der Design-Session verfügbar; nicht im Repo committed). Die für die Implementierung relevanten Details sind in diesem Spec vollständig festgehalten — das Bundle ist keine verlässliche Langzeit-Referenz. Falls benötigt, ist der React-Prototyp aus dem Bundle mit Standard-Tools (React + Babel Standalone über CDN) ohne Build-Schritt im Browser ausführbar.
