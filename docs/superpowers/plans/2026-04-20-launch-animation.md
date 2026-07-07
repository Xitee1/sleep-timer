# Play-Button Launch-Animation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eine Rocket-Launch-Animation auf den Play-Button packen: Icon dreht sich Richtung Dial, fliegt zum Dial-Zentrum, schlägt ein, Knob pulsiert und Shockwave rippelt — gesteuert durch ein neues Settings-Toggle und gegated durch Androids System-Reduce-Motion.

**Architecture:** Ein neuer `LaunchAnimationController` (Animatable-basierte State-Machine in einer Coroutine) orchestriert die Phasen Crouch → Launch → Impact sequenziell. Ein `LaunchOverlay` Composable rendert das fliegende Icon im Root-Koordinatenraum des `TimerScreen`. `PlayButton` und `CircularDial` bekommen neue optionale Parameter, werden aber nicht anderweitig umgebaut — die Komponenten bleiben dumb, die Orchestrierung sitzt in `TimerScreen`.

**Tech Stack:** Kotlin, Jetpack Compose, `androidx.compose.animation.core.Animatable`, `Modifier.onGloballyPositioned`, DataStore Preferences, Hilt.

**Spec reference:** `docs/superpowers/specs/2026-04-20-launch-animation-design.md`

**Testing approach:** Dieses Projekt hat keine Tests — weder Unit- noch Instrumentation-Tests. Jeder Task endet mit **manueller Verifikation** (Build + ggf. visuelle Prüfung auf einem Debug-Build) und einem optionalen Commit. Der Executor entscheidet pro Task, ob committet wird; CLAUDE.md schreibt Commits nicht generell vor, die Anweisungen hier sind Vorschläge.

**Build commands for reference:**
- Vollbuild: `./gradlew assembleDebug`
- Modul-Lint: `./gradlew :feature:timer:lintDebug`, `./gradlew :core:data:lintDebug`
- Kompletter Lint: `./gradlew lint`
- Release-Sanity: `./gradlew assembleRelease` (fällt ohne Keystore auf Debug-Signing zurück)

---

## File Structure

**Neue Dateien:**
- `core/data/src/main/kotlin/dev/xitee/sleeptimer/core/data/util/ReduceMotion.kt` — Utility zum Lesen des Android-System-Reduce-Motion-Flags
- `feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/timer/components/LaunchAnimation.kt` — `LaunchPhase`, `LaunchAnimationController`, `rememberLaunchAnimationController`, `LaunchOverlay`

**Geänderte Dateien:**
- `core/data/src/main/kotlin/dev/xitee/sleeptimer/core/data/model/UserSettings.kt` — neues Feld
- `core/data/src/main/kotlin/dev/xitee/sleeptimer/core/data/repository/SettingsRepository.kt` — neue `update`-Methode
- `core/data/src/main/kotlin/dev/xitee/sleeptimer/core/data/repository/SettingsRepositoryImpl.kt` — Key, Seeding, Flow-Mapping, Update
- `feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/settings/SettingsViewModel.kt` — Handler
- `feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/settings/SettingsScreen.kt` — Toggle-Row
- `feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/timer/components/PlayButton.kt` — neue Parameter
- `feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/timer/components/CircularDial.kt` — neuer Parameter + Impact-Rendering
- `feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/timer/TimerScreen.kt` — Controller, Position-Tracking, Trigger, Overlay
- `feature/timer/src/main/res/values/strings.xml` — neue Strings
- `feature/timer/src/main/res/values-de/strings.xml` — deutsche Übersetzung

---

## Task 1: Reduce-Motion-Utility anlegen

**Files:**
- Create: `core/data/src/main/kotlin/dev/xitee/sleeptimer/core/data/util/ReduceMotion.kt`

- [ ] **Step 1: Datei erstellen**

```kotlin
package dev.xitee.sleeptimer.core.data.util

import android.content.Context
import android.provider.Settings

/**
 * Gibt true zurück, wenn der Nutzer in den System-Einstellungen „Animationen entfernen"
 * aktiviert hat. Erkennung über `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`, was
 * von Accessibility-Settings und den Developer-Options identisch gesetzt wird.
 */
fun isSystemReduceMotionEnabled(context: Context): Boolean {
    val scale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )
    return scale == 0f
}
```

- [ ] **Step 2: Build verifizieren**

Run: `./gradlew :core:data:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add core/data/src/main/kotlin/dev/xitee/sleeptimer/core/data/util/ReduceMotion.kt
git commit -m "feat(core-data): add system reduce-motion detection utility"
```

---

## Task 2: `launchAnimationEnabled`-Feld in `UserSettings`

**Files:**
- Modify: `core/data/src/main/kotlin/dev/xitee/sleeptimer/core/data/model/UserSettings.kt`

- [ ] **Step 1: Feld hinzufügen**

Ersetze den gesamten Inhalt der Datei durch:

```kotlin
package dev.xitee.sleeptimer.core.data.model

data class UserSettings(
    val stopMediaPlayback: Boolean = true,
    val fadeOutDurationSeconds: Int = 30,
    val screenOff: Boolean = false,
    val softScreenOff: Boolean = false,
    val turnOffWifi: Boolean = false,
    val turnOffBluetooth: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val theme: ThemeId = ThemeId.Default,
    val starsEnabled: Boolean = true,
    val stepMinutes: Int = 5,
    val presetMinutes: Int = 15,
    val launchAnimationEnabled: Boolean = true,
)
```

- [ ] **Step 2: Build verifizieren**

Run: `./gradlew :core:data:assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 3: `SettingsRepository`-Interface erweitern

**Files:**
- Modify: `core/data/src/main/kotlin/dev/xitee/sleeptimer/core/data/repository/SettingsRepository.kt`

- [ ] **Step 1: Neue Methode zum Interface hinzufügen**

Ersetze den gesamten Inhalt durch:

```kotlin
package dev.xitee.sleeptimer.core.data.repository

import dev.xitee.sleeptimer.core.data.model.ThemeId
import dev.xitee.sleeptimer.core.data.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun updateStopMediaPlayback(enabled: Boolean)
    suspend fun updateFadeOutDuration(seconds: Int)
    suspend fun updateScreenOff(enabled: Boolean)
    suspend fun updateSoftScreenOff(enabled: Boolean)
    suspend fun updateTurnOffWifi(enabled: Boolean)
    suspend fun updateTurnOffBluetooth(enabled: Boolean)
    suspend fun updateHapticFeedback(enabled: Boolean)
    suspend fun updateTheme(theme: ThemeId)
    suspend fun updateStarsEnabled(enabled: Boolean)
    suspend fun updateStepMinutes(minutes: Int)
    suspend fun updatePresetMinutes(minutes: Int)
    suspend fun updateLaunchAnimationEnabled(enabled: Boolean)
}
```

- [ ] **Step 2: Build fails (Impl implementiert neue Methode noch nicht)**

Run: `./gradlew :core:data:assembleDebug`
Expected: FAIL — `Class SettingsRepositoryImpl is not abstract and does not implement abstract member public abstract suspend fun updateLaunchAnimationEnabled(...)`

Dies bestätigt, dass das Interface korrekt erweitert wurde. Task 4 behebt den Build.

---

## Task 4: `SettingsRepositoryImpl` erweitern (Key, Seeding, Flow, Update)

**Files:**
- Modify: `core/data/src/main/kotlin/dev/xitee/sleeptimer/core/data/repository/SettingsRepositoryImpl.kt`

**Kontext:** Das Setting soll beim allerersten App-Start einmalig basierend auf dem System-Reduce-Motion-Flag persistiert werden. Dafür gibt es einen zweiten „seeded"-Boolean-Key, der nach dem ersten Seeding auf `true` gesetzt wird. Ohne diesen Flag würden Änderungen des System-Settings nach dem Install das App-Setting bei jedem Read neu berechnen.

- [ ] **Step 1: Impl komplett ersetzen**

Ersetze den gesamten Inhalt durch:

```kotlin
package dev.xitee.sleeptimer.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.xitee.sleeptimer.core.data.model.ThemeId
import dev.xitee.sleeptimer.core.data.model.UserSettings
import dev.xitee.sleeptimer.core.data.util.isSystemReduceMotionEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private companion object {
        val STOP_MEDIA = booleanPreferencesKey("stop_media_playback")
        val FADE_OUT_DURATION = intPreferencesKey("fade_out_duration_seconds")
        val SCREEN_OFF = booleanPreferencesKey("screen_off")
        val SOFT_SCREEN_OFF = booleanPreferencesKey("soft_screen_off")
        val TURN_OFF_WIFI = booleanPreferencesKey("turn_off_wifi")
        val TURN_OFF_BLUETOOTH = booleanPreferencesKey("turn_off_bluetooth")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val THEME = stringPreferencesKey("theme")
        val STARS_ENABLED = booleanPreferencesKey("stars_enabled")
        val STEP_MINUTES = intPreferencesKey("step_minutes")
        val PRESET_MINUTES = intPreferencesKey("preset_minutes")
        val LAUNCH_ANIMATION_ENABLED = booleanPreferencesKey("launch_animation_enabled")
        val LAUNCH_ANIMATION_SEEDED = booleanPreferencesKey("launch_animation_seeded")
    }

    // Einmaliger Init-Scope. IO-Dispatcher ist angemessen für DataStore-Writes,
    // SupervisorJob verhindert dass eine Child-Exception weitere Writes stoppt.
    private val initScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Seed-on-first-install: ist der „seeded"-Flag nicht gesetzt, wird das
        // launchAnimationEnabled-Feld einmalig basierend auf der System-Reduce-Motion-
        // Präferenz persistiert. Danach gewinnen User-Overrides. Spätere System-Änderungen
        // werden bewusst nicht reflektiert (siehe Spec, Out-of-Scope).
        initScope.launch {
            dataStore.edit { prefs ->
                if (prefs[LAUNCH_ANIMATION_SEEDED] != true) {
                    prefs[LAUNCH_ANIMATION_ENABLED] = !isSystemReduceMotionEnabled(context)
                    prefs[LAUNCH_ANIMATION_SEEDED] = true
                }
            }
        }
    }

    override val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        // Single source of truth: defaults come from UserSettings(), so adding a new
        // field only requires updating the data class.
        val d = UserSettings()
        UserSettings(
            stopMediaPlayback = prefs[STOP_MEDIA] ?: d.stopMediaPlayback,
            fadeOutDurationSeconds = prefs[FADE_OUT_DURATION] ?: d.fadeOutDurationSeconds,
            screenOff = prefs[SCREEN_OFF] ?: d.screenOff,
            softScreenOff = prefs[SOFT_SCREEN_OFF] ?: d.softScreenOff,
            turnOffWifi = prefs[TURN_OFF_WIFI] ?: d.turnOffWifi,
            turnOffBluetooth = prefs[TURN_OFF_BLUETOOTH] ?: d.turnOffBluetooth,
            hapticFeedbackEnabled = prefs[HAPTIC_FEEDBACK] ?: d.hapticFeedbackEnabled,
            theme = ThemeId.fromStorage(prefs[THEME]),
            starsEnabled = prefs[STARS_ENABLED] ?: d.starsEnabled,
            stepMinutes = prefs[STEP_MINUTES] ?: d.stepMinutes,
            presetMinutes = prefs[PRESET_MINUTES] ?: d.presetMinutes,
            launchAnimationEnabled = prefs[LAUNCH_ANIMATION_ENABLED] ?: d.launchAnimationEnabled,
        )
    }

    override suspend fun updateStopMediaPlayback(enabled: Boolean) {
        dataStore.edit { it[STOP_MEDIA] = enabled }
    }

    override suspend fun updateFadeOutDuration(seconds: Int) {
        dataStore.edit { it[FADE_OUT_DURATION] = seconds }
    }

    override suspend fun updateScreenOff(enabled: Boolean) {
        dataStore.edit { it[SCREEN_OFF] = enabled }
    }

    override suspend fun updateSoftScreenOff(enabled: Boolean) {
        dataStore.edit { it[SOFT_SCREEN_OFF] = enabled }
    }

    override suspend fun updateTurnOffWifi(enabled: Boolean) {
        dataStore.edit { it[TURN_OFF_WIFI] = enabled }
    }

    override suspend fun updateTurnOffBluetooth(enabled: Boolean) {
        dataStore.edit { it[TURN_OFF_BLUETOOTH] = enabled }
    }

    override suspend fun updateHapticFeedback(enabled: Boolean) {
        dataStore.edit { it[HAPTIC_FEEDBACK] = enabled }
    }

    override suspend fun updateTheme(theme: ThemeId) {
        dataStore.edit { it[THEME] = theme.name }
    }

    override suspend fun updateStarsEnabled(enabled: Boolean) {
        dataStore.edit { it[STARS_ENABLED] = enabled }
    }

    override suspend fun updateStepMinutes(minutes: Int) {
        dataStore.edit { it[STEP_MINUTES] = minutes.coerceIn(1, 30) }
    }

    override suspend fun updatePresetMinutes(minutes: Int) {
        dataStore.edit { it[PRESET_MINUTES] = minutes.coerceIn(1, 300) }
    }

    override suspend fun updateLaunchAnimationEnabled(enabled: Boolean) {
        dataStore.edit { it[LAUNCH_ANIMATION_ENABLED] = enabled }
    }
}
```

- [ ] **Step 2: Build verifizieren**

Run: `./gradlew :core:data:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Lint verifizieren**

Run: `./gradlew :core:data:lintDebug`
Expected: BUILD SUCCESSFUL, keine neuen Warnings

- [ ] **Step 4: Commit**

```bash
git add core/data/src/main/kotlin/dev/xitee/sleeptimer/core/data/model/UserSettings.kt \
        core/data/src/main/kotlin/dev/xitee/sleeptimer/core/data/repository/SettingsRepository.kt \
        core/data/src/main/kotlin/dev/xitee/sleeptimer/core/data/repository/SettingsRepositoryImpl.kt
git commit -m "feat(core-data): add launchAnimationEnabled setting with reduce-motion seed"
```

---

## Task 5: `SettingsViewModel`-Handler anlegen

**Files:**
- Modify: `feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/settings/SettingsViewModel.kt`

- [ ] **Step 1: Neue Methode am Ende der Klasse hinzufügen**

Füge direkt vor der schließenden Klammer der Klasse (nach `updateStepMinutes`) hinzu:

```kotlin
    fun updateLaunchAnimationEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateLaunchAnimationEnabled(enabled) }
    }
```

- [ ] **Step 2: Build verifizieren**

Run: `./gradlew :feature:timer:assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 6: Strings für das neue Toggle (EN + DE)

**Files:**
- Modify: `feature/timer/src/main/res/values/strings.xml`
- Modify: `feature/timer/src/main/res/values-de/strings.xml`

- [ ] **Step 1: Englische Strings hinzufügen**

Füge in `feature/timer/src/main/res/values/strings.xml` direkt nach der Zeile `<string name="stars_description">Drifting starfield behind the dial</string>` hinzu:

```xml
    <string name="launch_animation_title">Launch animation</string>
    <string name="launch_animation_description">Rocket-style play button animation when starting the timer</string>
```

- [ ] **Step 2: Deutsche Strings hinzufügen**

Füge in `feature/timer/src/main/res/values-de/strings.xml` direkt nach der Zeile `<string name="stars_description">Driftendes Sternenfeld hinter dem Dial</string>` hinzu:

```xml
    <string name="launch_animation_title">Launch-Animation</string>
    <string name="launch_animation_description">Raketenartige Animation des Play-Buttons beim Timer-Start</string>
```

- [ ] **Step 3: Build verifizieren**

Run: `./gradlew :feature:timer:assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 7: Toggle-Row in `SettingsScreen` einhängen

**Files:**
- Modify: `feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/settings/SettingsScreen.kt`

- [ ] **Step 1: Import hinzufügen**

Stelle sicher dass `import androidx.compose.material.icons.filled.RocketLaunch` am Anfang der Datei vorhanden ist (im Import-Block neben den anderen `material.icons.filled.*`-Imports).

- [ ] **Step 2: Toggle-Row einfügen**

In `SettingsScreen.kt`, direkt nach dem bestehenden `SettingsToggleRow` für `starsEnabled` (Zeilen ~215-226) und vor dem `SectionHeader(stringResource(R.string.category_sleep_timer))` (Zeile ~228) einfügen:

```kotlin
                SettingsToggleRow(
                    icon = Icons.Default.RocketLaunch,
                    title = stringResource(R.string.launch_animation_title),
                    description = stringResource(R.string.launch_animation_description),
                    checked = uiState.settings.launchAnimationEnabled,
                    onCheckedChange = { viewModel.updateLaunchAnimationEnabled(it) },
                )
```

- [ ] **Step 3: Build verifizieren**

Run: `./gradlew :feature:timer:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Manuelle Verifikation**

Install Debug-APK: `./gradlew :app:installDebug`
Öffne App → Settings → neues Toggle „Launch-Animation" erscheint unter „Sterne".
Toggle an/aus, App kill & neu öffnen → Wert ist persistiert.
Bei frischer Installation mit aktivem System-Reduce-Motion: Toggle erscheint als *aus*. Ohne Reduce-Motion: Toggle erscheint als *an*.

- [ ] **Step 5: Commit**

```bash
git add feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/settings/SettingsViewModel.kt \
        feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/settings/SettingsScreen.kt \
        feature/timer/src/main/res/values/strings.xml \
        feature/timer/src/main/res/values-de/strings.xml
git commit -m "feat(settings): add launch animation toggle"
```

---

## Task 8: `PlayButton` um `crouchProgress` + `iconLaunching` erweitern

**Files:**
- Modify: `feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/timer/components/PlayButton.kt`

**Kontext:** Der `PlayButton` bleibt funktional identisch, wenn die neuen Parameter ihre Default-Werte haben (`crouchProgress = 0f`, `iconLaunching = false`). Bei `crouchProgress > 0`: Button schrumpft leicht, Icon rotiert auf `targetIconRotationDeg` und verkleinert sich. Bei `iconLaunching = true`: das Play-Icon ist komplett transparent (weil das Overlay es gerade rendert).

- [ ] **Step 1: Signatur erweitern und Body anpassen**

Ersetze die gesamte Composable `PlayButton` (Zeilen 37-115) durch:

```kotlin
@Composable
fun PlayButton(
    isRunning: Boolean,
    hapticEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRotation: Float = 0f,
    crouchProgress: Float = 0f,
    iconLaunching: Boolean = false,
    targetIconRotationDeg: Float = 0f,
) {
    val theme = appTheme()
    val view = LocalView.current

    val morph by animateFloatAsState(
        targetValue = if (isRunning) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "playMorph",
    )
    // 0 = circle (50% corners), 1 = rounded square (~28% corners)
    val cornerPercent = (50f - 22f * morph).toInt().coerceIn(28, 50)
    val shape = RoundedCornerShape(percent = cornerPercent)

    val shadowModifier = if (theme.hasGradient) {
        Modifier.shadow(
            elevation = 20.dp,
            shape = shape,
            ambientColor = theme.accent,
            spotColor = theme.accent,
        )
    } else {
        Modifier
    }

    // Linear interpolate button scale: 1.0 at crouchProgress=0, 0.92 at crouchProgress=1
    val buttonScale = 1f - 0.08f * crouchProgress.coerceIn(0f, 1f)
    // Icon rotation during crouch: 0° → targetIconRotationDeg as crouchProgress goes 0→1.
    // `iconRotation` (orientation-based) is added on top so landscape still rotates the
    // idle icon correctly.
    val crouchRotation = targetIconRotationDeg * crouchProgress.coerceIn(0f, 1f)
    // Icon scale during crouch: 1.0 → 0.9
    val crouchIconScale = 1f - 0.1f * crouchProgress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .size(84.dp)
            .graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
            }
            .then(shadowModifier)
            .clip(shape)
            .clickable {
                if (hapticEnabled) {
                    view.performHapticFeedback(playStopHaptic)
                }
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(84.dp)) {
            val cornerRadiusPx = (cornerPercent / 100f) * size.minDimension
            val corner = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            drawRoundRect(
                color = theme.accent,
                cornerRadius = corner,
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (theme.isDark) 0.45f else 0.30f),
                    1f to Color.White.copy(alpha = 0f),
                    startY = 0f,
                    endY = size.height * 0.55f,
                ),
                cornerRadius = corner,
            )
        }
        Crossfade(
            targetState = isRunning,
            animationSpec = tween(durationMillis = 180),
            label = "playIcon",
        ) { running ->
            val icon: ImageVector = if (running) Icons.Default.Stop else Icons.Default.PlayArrow
            val desc = stringResource(if (running) R.string.stop_timer else R.string.start_timer)
            // Play icon hides while the launch overlay is flying.
            val iconAlpha = if (!running && iconLaunching) 0f else 1f
            Icon(
                imageVector = icon,
                contentDescription = desc,
                tint = theme.accentInk,
                modifier = Modifier
                    .size(34.dp)
                    .graphicsLayer {
                        rotationZ = iconRotation + (if (!running) crouchRotation else 0f)
                        scaleX = if (!running) crouchIconScale else 1f
                        scaleY = if (!running) crouchIconScale else 1f
                        alpha = iconAlpha
                    },
            )
        }
    }
}
```

- [ ] **Step 2: Build verifizieren**

Run: `./gradlew :feature:timer:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Manuelle Verifikation: Button unverändert ohne Parameter**

Install Debug-APK. App öffnen. Play-Button tippen → Crossfade zu Stop (bisheriges Verhalten). Zurück zu Idle per Stop. Keine visuelle Regression gegenüber vor der Änderung.

- [ ] **Step 4: Commit**

```bash
git add feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/timer/components/PlayButton.kt
git commit -m "feat(timer-ui): extend PlayButton with crouch + launching parameters"
```

---

## Task 9: `CircularDial` um `impactPulse` erweitern

**Files:**
- Modify: `feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/timer/components/CircularDial.kt`

**Kontext:** Ein einziger `impactPulse: Float` Parameter (0..1) steuert alle drei Impact-Effekte (Shockwave, Knob-Aura, Ring-Boost). Der Controller wird später Werte zwischen 0 und 1 für die ~260ms-Impact-Phase einspeisen. Bei `impactPulse == 0f` ist das Dial-Rendering bit-identisch zum bisherigen Zustand.

- [ ] **Step 1: Parameter zur Signatur hinzufügen**

Ersetze die Signatur (Zeilen 37-46):

```kotlin
@Composable
fun CircularDial(
    state: CircularDialState,
    isRunning: Boolean,
    runningMinutes: Float,
    hapticEnabled: Boolean,
    onMinutesChanged: (Int) -> Unit,
    onMinutesCommitted: (Int) -> Unit,
    modifier: Modifier = Modifier,
    impactPulse: Float = 0f,
) {
```

- [ ] **Step 2: Impact-Rendering nach `drawKnob` einbauen**

Im `Canvas`-Block, direkt nach dem `drawKnob(...)`-Call (Zeile ~196-202), füge hinzu:

```kotlin
            if (impactPulse > 0f) {
                drawImpactEffects(
                    center = center,
                    ringRadius = radius,
                    strokeWidth = strokeWidth,
                    knobFraction = ringFraction,
                    pulse = impactPulse.coerceIn(0f, 1f),
                    accent = theme.accent,
                )
            }
```

- [ ] **Step 3: Neue Draw-Funktion am Dateiende einfügen**

Füge am Ende der Datei (nach `drawKnob`) diese neue `private fun` hinzu:

```kotlin
private fun DrawScope.drawImpactEffects(
    center: Offset,
    ringRadius: Float,
    strokeWidth: Float,
    knobFraction: Float,
    pulse: Float,
    accent: androidx.compose.ui.graphics.Color,
) {
    // Knob-Position reproducing drawKnob's math.
    val angle = knobFraction * (2f * Math.PI.toFloat()) - (Math.PI.toFloat() / 2f)
    val kx = center.x + ringRadius * cos(angle)
    val ky = center.y + ringRadius * sin(angle)

    // 1) Knob aura bloom — radius expands, alpha fades.
    val auraRadius = 16.dp.toPx() + (60.dp.toPx() - 16.dp.toPx()) * pulse
    val auraAlpha = (1f - pulse).coerceAtLeast(0f) * 0.55f
    drawCircle(
        color = accent.copy(alpha = auraAlpha),
        radius = auraRadius,
        center = Offset(kx, ky),
    )

    // 2) Three concentric shockwave ripples, phase-shifted.
    // Each ripple has its own normalized lifetime within the impact pulse.
    val rippleOffsets = floatArrayOf(0f, 0.12f, 0.24f)
    for (offset in rippleOffsets) {
        val local = ((pulse - offset) / (1f - offset)).coerceIn(0f, 1f)
        if (local <= 0f) continue
        val rippleRadius = 10.dp.toPx() + (70.dp.toPx() - 10.dp.toPx()) * local
        val rippleStroke = (6.dp.toPx() - 5.5f.dp.toPx() * local).coerceAtLeast(0.5f.dp.toPx())
        val rippleAlpha = (1f - local) * 0.9f
        drawCircle(
            color = accent.copy(alpha = rippleAlpha),
            radius = rippleRadius,
            center = Offset(kx, ky),
            style = Stroke(width = rippleStroke),
        )
    }

    // 3) Ring-Alpha-Boost: bright flash layered over the existing progress arc.
    // Eases in quickly (0→0.3) and fades over the rest of the pulse.
    val boostAlpha = when {
        pulse < 0.3f -> pulse / 0.3f
        else -> (1f - pulse) / 0.7f
    }.coerceIn(0f, 1f) * 0.4f
    if (boostAlpha > 0f) {
        drawCircle(
            color = accent.copy(alpha = boostAlpha),
            radius = ringRadius,
            center = center,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}
```

- [ ] **Step 4: Build verifizieren**

Run: `./gradlew :feature:timer:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Manuelle Verifikation**

App öffnen: Dial sieht exakt wie vorher aus (impactPulse default 0).
Zur Verifikation des neuen Codes: temporär in `TimerScreen.kt` das Argument `impactPulse = 0.5f` an `CircularDial(...)` übergeben. Neu bauen → Dial zeigt ein „eingefrorenes" Impact-Bild (mittlere Shockwave, Knob-Aura). **Änderung danach zurücknehmen.**

- [ ] **Step 6: Commit**

```bash
git add feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/timer/components/CircularDial.kt
git commit -m "feat(timer-ui): add impact pulse rendering to CircularDial"
```

---

## Task 10: `LaunchAnimation.kt` — Phase + Controller-Skelett

**Files:**
- Create: `feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/timer/components/LaunchAnimation.kt`

- [ ] **Step 1: Datei mit Phase-Enum + Controller anlegen**

```kotlin
package dev.xitee.sleeptimer.feature.timer.timer.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
            // Parallel animieren
            launch { buttonScale.animateTo(0.92f, crouchSpec) }
            launch { iconRotationDeg.animateTo(targetIconRotationDeg, crouchSpec) }
            launch { crouchProgress.animateTo(1f, crouchSpec) }
            kotlinx.coroutines.delay(140)

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
            kotlinx.coroutines.delay(420)

            // Phase 3: Impact (560–820ms)
            phase = LaunchPhase.Impact
            val impactEasing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)
            val impactSpec = tween<Float>(260, easing = impactEasing)
            launch {
                buttonScale.animateTo(1.04f, tween(130, easing = CubicBezierEasing(0.2f, 1.8f, 0.4f, 1f)))
                buttonScale.animateTo(1f, tween(130))
            }
            launch { impactPulse.animateTo(1f, impactSpec) }
            kotlinx.coroutines.delay(260)

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
```

- [ ] **Step 2: Build verifizieren**

Run: `./gradlew :feature:timer:assembleDebug`
Expected: BUILD SUCCESSFUL

---

## Task 11: `LaunchOverlay` Composable in derselben Datei

**Files:**
- Modify: `feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/timer/components/LaunchAnimation.kt`

**Kontext:** Die Overlay-Composable liest Button- und Dial-Center (in Root-Koordinaten) und rendert während der `Launch`-Phase das fliegende Play-Icon. Platzierung: als Sibling zum Main-`Column` innerhalb der `TimerBackground`-Box — da die Background-Box vollflächig bei `(0,0)` im Root startet, mappen Root-Koordinaten 1:1 auf die Overlay-Child-Position.

Während `Crouch` ist das Icon noch im Button (dort animiert), während `Impact` ist es „eingeschlagen" und komplett weg. Das Overlay rendert also ausschließlich in der `Launch`-Phase.

- [ ] **Step 1: Imports am Dateianfang ergänzen**

Im Import-Block von `LaunchAnimation.kt` hinzufügen (neben den bestehenden Animatable/Coroutine-Imports):

```kotlin
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
```

- [ ] **Step 2: Overlay-Composable am Dateiende ergänzen**

Füge am Ende von `LaunchAnimation.kt` hinzu:

```kotlin
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
```

**Hinweis zur Positionierung:** Das `Icon` wird vom Parent (die `TimerBackground`-Box in `TimerScreen`) bei `(0, 0)` platziert. `graphicsLayer { translationX, translationY }` verschiebt es dann nach der Layoutphase um die gewünschten Pixel. Innerhalb des `graphicsLayer`-Blocks ist `this` ein `GraphicsLayerScope`, der von `Density` erbt — deshalb funktioniert `17.dp.toPx()` direkt.

- [ ] **Step 3: Build verifizieren**

Run: `./gradlew :feature:timer:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/timer/components/LaunchAnimation.kt
git commit -m "feat(timer-ui): add LaunchAnimationController and overlay composable"
```

---

## Task 12: Alles in `TimerScreen` verdrahten

**Files:**
- Modify: `feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/timer/TimerScreen.kt`

**Kontext:** Das ist der Task wo es zum ersten Mal visuell etwas gibt. Wir verknüpfen:
1. Button-Position messen (`onGloballyPositioned`)
2. Dial-Position messen (`onGloballyPositioned`)
3. Controller in Composable erzeugen
4. Trigger-Logik im `onToggle`: bei Idle + Animation-enabled → `controller.launch(angle)`
5. `LaunchOverlay` im Root-Box rendern
6. `impactPulse` an `CircularDial` durchreichen
7. Neue PlayButton-Parameter übergeben
8. Reduce-Motion-Check zur Laufzeit
9. Lifecycle-Reset bei Background

- [ ] **Step 1: Imports ergänzen**

Im Import-Block von `TimerScreen.kt` hinzufügen (alphabetisch einsortieren):

```kotlin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.xitee.sleeptimer.core.data.util.isSystemReduceMotionEnabled
import dev.xitee.sleeptimer.feature.timer.timer.components.LaunchOverlay
import dev.xitee.sleeptimer.feature.timer.timer.components.LaunchPhase
import dev.xitee.sleeptimer.feature.timer.timer.components.rememberLaunchAnimationController
import kotlin.math.atan2
```

- [ ] **Step 2: Controller-State + Positions-State in `TimerContent` hinzufügen**

Direkt nach den vorhandenen `val orientation by rememberDeviceOrientation()`-Zeilen (Zeile ~104):

```kotlin
    val context = LocalContext.current
    val launchController = rememberLaunchAnimationController()
    var buttonCenter by remember { mutableStateOf(Offset.Zero) }
    var dialCenter by remember { mutableStateOf(Offset.Zero) }
    val animationEnabled = settings.launchAnimationEnabled &&
        !isSystemReduceMotionEnabled(context)

    // Snap zurück auf Idle, wenn die App in den Hintergrund geht.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        launchController.reset()
    }
```

(`context` existiert bereits weiter oben — prüfen und nicht doppelt deklarieren.)

- [ ] **Step 3: Dial-Position messen**

Im `Box` direkt um den `CircularDial` (aktuell Zeile ~289):

```kotlin
                    Box(
                        modifier = Modifier
                            .size(dialSize)
                            .graphicsLayer { rotationZ = animatedAngle }
                            .onGloballyPositioned { coords ->
                                dialCenter = coords.boundsInRoot().center
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularDial(
                            state = dialState,
                            isRunning = isRunning,
                            runningMinutes = runningMinutes,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                            onMinutesChanged = viewModel::setMinutes,
                            onMinutesCommitted = viewModel::commitMinutes,
                            impactPulse = launchController.impactPulse.value,
                            modifier = Modifier.fillMaxSize(),
                        )
                        ...
```

- [ ] **Step 4: Button-Position messen + neue Parameter übergeben**

In der `ActionRow`-Composable (Zeile ~482), und der `PlayButton`-Instanziierung darin (Zeile ~509): die `ActionRow`-Signatur muss erweitert werden, damit sie die Launch-Animation-Parameter durchreichen kann. Ersetze die komplette `ActionRow`-Composable (Zeilen 482-524) durch:

```kotlin
@Composable
private fun ActionRow(
    isRunning: Boolean,
    hapticEnabled: Boolean,
    iconRotation: Float,
    onToggle: () -> Unit,
    onMinusStep: () -> Unit,
    onPlusStep: () -> Unit,
    isMinusEnabled: Boolean,
    isPlusEnabled: Boolean,
    plusStepVisibleWhileRunning: Boolean,
    crouchProgress: Float,
    iconLaunching: Boolean,
    targetIconRotationDeg: Float,
    buttonScale: Float,
    onButtonPositioned: (Offset) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SecondaryRoundButton(
            icon = Icons.Default.Remove,
            contentDescription = stringResource(R.string.cd_step_minus),
            onClick = onMinusStep,
            hapticEnabled = hapticEnabled,
            enabled = isMinusEnabled,
            iconRotation = iconRotation,
        )
        PlayButton(
            isRunning = isRunning,
            hapticEnabled = hapticEnabled,
            onClick = onToggle,
            iconRotation = iconRotation,
            crouchProgress = crouchProgress,
            iconLaunching = iconLaunching,
            targetIconRotationDeg = targetIconRotationDeg,
            buttonScale = buttonScale,
            modifier = Modifier.onGloballyPositioned { coords ->
                onButtonPositioned(coords.boundsInRoot().center)
            },
        )
        SecondaryRoundButton(
            icon = Icons.Default.Add,
            contentDescription = stringResource(R.string.cd_step_plus),
            onClick = onPlusStep,
            hapticEnabled = hapticEnabled,
            enabled = if (isRunning) plusStepVisibleWhileRunning else isPlusEnabled,
            iconRotation = iconRotation,
        )
    }
}
```

- [ ] **Step 5: `ActionRow`-Aufruf in `TimerContent` anpassen**

In `TimerContent`, beim `ActionRow`-Call (aktuell Zeile ~336–381): neue Parameter übergeben und `onToggle` ausbauen für Animation-Trigger. Der Block `val runningRemainingSeconds: Int = when (val s = uiState) { ... }` bleibt direkt **vor** der neuen `ActionRow(...)`-Call stehen (unverändert) — die neuen `val launchPhase`/`val targetIconAngleDeg`-Deklarationen dann zwischen `runningRemainingSeconds` und `ActionRow(...)` einfügen.

Alte `ActionRow(...)`-Zeilen ~336–381 ersetzen durch:

```kotlin
            val launchPhase = launchController.phase
            val iconLaunching = launchPhase == LaunchPhase.Launch ||
                launchPhase == LaunchPhase.Impact
            // Ziel-Rotation des Icons relativ zur X-Achse (Icon zeigt standardmäßig rechts).
            val targetIconAngleDeg = remember(buttonCenter, dialCenter) {
                if (buttonCenter == Offset.Zero || dialCenter == Offset.Zero) 0f
                else {
                    val dx = dialCenter.x - buttonCenter.x
                    val dy = dialCenter.y - buttonCenter.y
                    Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
                }
            }

            ActionRow(
                isRunning = isRunning,
                hapticEnabled = settings.hapticFeedbackEnabled,
                iconRotation = animatedAngle,
                onToggle = {
                    val animating = launchPhase != LaunchPhase.Idle
                    if (isRunning || animating) {
                        viewModel.stopTimer()
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.startTimer()
                        if (animationEnabled &&
                            buttonCenter != Offset.Zero &&
                            dialCenter != Offset.Zero
                        ) {
                            launchController.launch(targetIconAngleDeg)
                        }
                    }
                },
                onMinusStep = {
                    if (isRunning) {
                        viewModel.subtractStep()
                    } else {
                        val step = settings.stepMinutes
                        val current = dialState.totalMinutes
                        val next = ((current - 1).coerceAtLeast(0) / step) * step
                        viewModel.commitMinutes(next)
                    }
                },
                onPlusStep = {
                    if (isRunning) {
                        viewModel.addStep()
                    } else {
                        val step = settings.stepMinutes
                        val current = dialState.totalMinutes
                        val next = (current / step + 1) * step
                        viewModel.commitMinutes(next.coerceAtMost(300))
                    }
                },
                isMinusEnabled = if (isRunning) {
                    runningRemainingSeconds > settings.stepMinutes * 60
                } else {
                    dialState.totalMinutes > 1
                },
                isPlusEnabled = !isRunning && dialState.totalMinutes < 300,
                plusStepVisibleWhileRunning = true,
                crouchProgress = launchController.crouchProgress.value,
                iconLaunching = iconLaunching,
                targetIconRotationDeg = targetIconAngleDeg,
                buttonScale = launchController.buttonScale.value,
                onButtonPositioned = { buttonCenter = it },
            )
```

- [ ] **Step 6: `LaunchOverlay` am Ende der Root-`TimerBackground`-Box einbauen**

In `TimerContent`, innerhalb des `TimerBackground { ... }`-Blocks, *nach* dem Landscape-Title-`AnimatedVisibility` (das heute der letzte Block ist, Zeilen ~392-417), ergänze:

```kotlin
        LaunchOverlay(
            controller = launchController,
            buttonCenter = buttonCenter,
            dialCenter = dialCenter,
            accentColor = appTheme().accent,
        )
```

- [ ] **Step 7: Build verifizieren**

Run: `./gradlew :feature:timer:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Lint verifizieren**

Run: `./gradlew :feature:timer:lintDebug`
Expected: BUILD SUCCESSFUL, keine neuen Warnings

- [ ] **Step 9: Manuelle Verifikation (Kernfunktion)**

Install Debug-APK. App öffnen:
- Settings-Toggle „Launch-Animation" aktivieren (falls System Reduce-Motion aus ist, ist es schon an)
- Zurück zum Timer-Screen, 15 min eingestellt
- Play-Button tippen → Button drückt kurz rein (Crouch), Play-Icon rotiert nach oben und fliegt zum Dial-Zentrum, am Knob (oben) entsteht ein Ring-Pulse + Shockwave, Button zeigt danach Stop-Icon
- Stop drücken → Timer stoppt

- [ ] **Step 10: Commit**

```bash
git add feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/timer/TimerScreen.kt
git commit -m "feat(timer-ui): wire launch animation into TimerScreen"
```

---

## Task 13: Regressions- und Orientation-Tests

**Files:** keine Änderungen, rein manuelle Verifikation. Falls Bugs auftauchen, fix als separater Commit.

- [ ] **Step 1: Build-Sanity**

```
./gradlew assembleDebug
./gradlew lint
./gradlew assembleRelease
```
Alle: BUILD SUCCESSFUL.

- [ ] **Step 2: Test-Matrix abarbeiten**

Debug-APK installieren. Folgende Fälle durchgehen und bei Abweichungen notieren:

| # | Szenario | Erwartet |
|---|----------|----------|
| 1 | Portrait, 15 min, Tap Play | Rocket fliegt nach oben, Impact am Knob (12-Uhr-Position) |
| 2 | Portrait, 45 min, Tap Play | Rocket fliegt nach oben, Impact am Knob (9-Uhr-Position, links) |
| 3 | Portrait, 90 min, Tap Play | Ring overflow-gefüllt, Knob bei 12-Uhr (30 min modulo 60), Impact dort |
| 4 | Landscape (Gerät rotieren), 15 min, Tap Play | Rocket fliegt auf der physischen Achse Button→Dial (also „nach oben" auf dem Bildschirm, aus User-Perspektive seitlich), Impact am Knob, den das rotierte Dial zeigt |
| 5 | Portrait, Tap Play, dann 300ms später Tap nochmal | Timer stoppt sofort, Animation läuft sichtbar bis Ende, Button zeigt danach Play |
| 6 | Settings-Toggle off → Tap Play | Kein Rocket-Flug, direkter Crossfade Play→Stop |
| 7 | Settings-Toggle on, System-Reduce-Motion on → Tap Play | Kein Rocket-Flug (Runtime-Gate greift) |
| 8 | Frischinstall mit System-Reduce-Motion on | Settings-Toggle erscheint als *aus* (seed-Default) |
| 9 | Frischinstall ohne Reduce-Motion | Settings-Toggle erscheint als *an* |
| 10 | Play drücken, App in Background (Home-Button) während Animation, wieder öffnen | Controller resettet, Button spiegelt echten Timer-State (Running falls Service gestartet hat) |
| 11 | Play drücken + sofort Gerät drehen | Composable neu, neue Positions-Messung, keine Crashes; Animation beim nächsten Tap wieder ok |
| 12 | Erster App-Start mit Android 13+ ohne Notification-Permission | Permission-Dialog erscheint *vor* der Animation; keine Rocket beim ersten Tap |
| 13 | Settings-Screen: deutsche Sprache | Toggle-Label „Launch-Animation" mit Beschreibung „Raketenartige Animation..." |
| 14 | Settings-Screen: englische Sprache | Toggle-Label „Launch animation" mit Beschreibung „Rocket-style..." |

- [ ] **Step 3: Bei Fund: Fix als separater Commit**

Für jeden Bug: minimaler Fix, manuelle Re-Verifikation, commit mit Message `fix(timer-ui): <beschreibung>`.

---

## Task 14: Final-Commit falls gewünscht

**Kontext:** Der Spec und dieses Plan-Dokument sind bisher nicht committet. Falls gewünscht, jetzt mitnehmen.

- [ ] **Step 1: Status checken**

Run: `git status`
Expected: eventuell `docs/superpowers/specs/2026-04-20-launch-animation-design.md` + `docs/superpowers/plans/2026-04-20-launch-animation.md` als untracked.

- [ ] **Step 2: Design-Doku einchecken**

```bash
git add docs/superpowers/specs/2026-04-20-launch-animation-design.md \
        docs/superpowers/plans/2026-04-20-launch-animation.md
git commit -m "docs: add launch animation design spec and implementation plan"
```

---

## Open follow-ups

- Wenn `impactPulse` als einzelner Float sich beim Feintuning als zu undifferenziert herausstellt: auf drei Parameter (`shockwaveProgress`, `knobPulseProgress`, `ringBoost`) splitten. Bis dahin YAGNI.
- Wenn die Rocket in Landscape aus User-Perspektive zu kurios aussieht: in einem Follow-up-Spec die Option B/C aus der Brainstorming-Session (Rotated-Frame-Animation) neu bewerten.
