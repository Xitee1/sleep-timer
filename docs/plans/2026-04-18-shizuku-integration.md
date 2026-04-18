# Shizuku Integration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add Shizuku as an optional privileged-shell backend so the sleep timer can (a) turn off the screen softly (biometric unlock still works) and (b) disable Wi-Fi and Bluetooth on timer expiration — while keeping the existing Device-Admin-based hard lock as fallback and requiring no Shizuku setup for users who do not enable these features.

**Architecture:**
- New `core/service/.../shizuku/` package containing `ShizukuManager` (state + permission), `ShizukuShell` (command execution via `Shizuku.newProcess`), and three thin action helpers (`ShizukuScreenOffHelper`, `ShizukuWifiController`, `ShizukuBluetoothController`).
- `SleepTimerService.onTimerExpired()` chooses between Shizuku and existing helpers based on settings; silently falls back if Shizuku is unavailable (matches the existing "fail silent" policy for screen lock).
- Settings UI gates each Shizuku-requiring toggle behind a dialog that is only shown when the user tries to enable the feature — no permission prompt at app startup.

**Tech Stack:**
- `dev.rikka.shizuku:api` + `dev.rikka.shizuku:provider` (13.1.5)
- Shell commands: `input keyevent 26` (power key), `svc wifi disable`, `svc bluetooth disable`
- No new test framework — project has no test infra; verification is manual on device (documented per task).

**Branching:** This work should live on a dedicated branch (e.g. `feat/shizuku-integration`), not on `fix/localize-hardcoded-strings` where we currently are. The executor must create and switch to the new branch in Task 0.

---

## A note on TDD

The writing-plans skill emphasizes TDD, but this project currently has **zero tests and no test infrastructure** — bootstrapping a full Android test stack (Robolectric/instrumented/Hilt test rules) is out of scope for this feature. Every task below ends with explicit **manual verification** steps on a real device, plus a `./gradlew :module:assembleDebug` build check. If the user later wants automated tests, we add them in a separate PR.

---

## Task 0: Create feature branch

**Why:** Current branch `fix/localize-hardcoded-strings` is unrelated; don't mix features.

**Step 1:** Check current state.
```bash
git -C /home/mato/projects/sleep-timer status
git -C /home/mato/projects/sleep-timer log --oneline main..HEAD
```
Expected: clean working tree, `e8d61ef Localize hardcoded UI strings` ahead of main.

**Step 2:** Create branch from current tip (keeps localization commit as base; it will be merged first or rebased later).
```bash
git -C /home/mato/projects/sleep-timer checkout -b feat/shizuku-integration
```

**Step 3:** Confirm.
```bash
git -C /home/mato/projects/sleep-timer branch --show-current
```
Expected: `feat/shizuku-integration`

---

## Task 1: Add Shizuku dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `core/service/build.gradle.kts`

**Step 1:** Add version + library entries to `gradle/libs.versions.toml`.

In `[versions]` section append:
```toml
shizuku = "13.1.5"
```

In `[libraries]` section append:
```toml
shizuku-api = { group = "dev.rikka.shizuku", name = "api", version.ref = "shizuku" }
shizuku-provider = { group = "dev.rikka.shizuku", name = "provider", version.ref = "shizuku" }
```

**Step 2:** Add dependencies to `core/service/build.gradle.kts` in the `dependencies { }` block:
```kotlin
implementation(libs.shizuku.api)
implementation(libs.shizuku.provider)
```

**Step 3:** Sync + build.
```bash
cd /home/mato/projects/sleep-timer && ./gradlew :core:service:assembleDebug
```
Expected: `BUILD SUCCESSFUL`. If Maven Central resolution fails, check `settings.gradle.kts` has `mavenCentral()` (it should — standard AGP bootstrap).

**Step 4:** Commit.
```bash
git add gradle/libs.versions.toml core/service/build.gradle.kts
git commit -m "build: add Shizuku api + provider dependencies"
```

---

## Task 2: Declare ShizukuProvider in manifest

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

**Step 1:** Inside `<application>` block (after the existing `<receiver>`), add:
```xml
<provider
    android:name="rikka.shizuku.ShizukuProvider"
    android:authorities="${applicationId}.shizuku"
    android:multiprocess="false"
    android:enabled="true"
    android:exported="true"
    android:permission="android.permission.INTERACT_ACROSS_USERS_FULL" />
```

**Why this matters:** Without the provider, Shizuku can't deliver the binder to our process. The `${applicationId}` placeholder resolves to the Gradle `applicationId` at build time.

**Step 2:** Build app module.
```bash
cd /home/mato/projects/sleep-timer && ./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

**Step 3:** Commit.
```bash
git add app/src/main/AndroidManifest.xml
git commit -m "build: declare ShizukuProvider in manifest"
```

---

## Task 3: Extend UserSettings with new fields

**Files:**
- Modify: `core/data/src/main/kotlin/dev/xitee/sleeptimer/core/data/model/UserSettings.kt`
- Modify: `core/data/src/main/kotlin/dev/xitee/sleeptimer/core/data/repository/SettingsRepository.kt`
- Modify: `core/data/src/main/kotlin/dev/xitee/sleeptimer/core/data/repository/SettingsRepositoryImpl.kt`

**Step 1:** Update `UserSettings`:
```kotlin
data class UserSettings(
    val stopMediaPlayback: Boolean = true,
    val fadeOutDurationSeconds: Int = 30,
    val screenOff: Boolean = false,
    val softScreenOff: Boolean = false,      // NEW — when true, use Shizuku power-key simulation instead of DevicePolicyManager.lockNow()
    val turnOffWifi: Boolean = false,        // NEW
    val turnOffBluetooth: Boolean = false,   // NEW
    val hapticFeedbackEnabled: Boolean = true,
    val theme: ThemeId = ThemeId.Default,
    val starsEnabled: Boolean = true,
    val stepMinutes: Int = 5,
)
```

**Step 2:** Add to `SettingsRepository` interface:
```kotlin
suspend fun updateSoftScreenOff(enabled: Boolean)
suspend fun updateTurnOffWifi(enabled: Boolean)
suspend fun updateTurnOffBluetooth(enabled: Boolean)
```

**Step 3:** Add to `SettingsRepositoryImpl`:

In `companion object`:
```kotlin
val SOFT_SCREEN_OFF = booleanPreferencesKey("soft_screen_off")
val TURN_OFF_WIFI = booleanPreferencesKey("turn_off_wifi")
val TURN_OFF_BLUETOOTH = booleanPreferencesKey("turn_off_bluetooth")
```

In the `settings` flow mapping, add:
```kotlin
softScreenOff = prefs[SOFT_SCREEN_OFF] ?: false,
turnOffWifi = prefs[TURN_OFF_WIFI] ?: false,
turnOffBluetooth = prefs[TURN_OFF_BLUETOOTH] ?: false,
```

Add three methods:
```kotlin
override suspend fun updateSoftScreenOff(enabled: Boolean) {
    dataStore.edit { it[SOFT_SCREEN_OFF] = enabled }
}

override suspend fun updateTurnOffWifi(enabled: Boolean) {
    dataStore.edit { it[TURN_OFF_WIFI] = enabled }
}

override suspend fun updateTurnOffBluetooth(enabled: Boolean) {
    dataStore.edit { it[TURN_OFF_BLUETOOTH] = enabled }
}
```

**Step 4:** Build.
```bash
cd /home/mato/projects/sleep-timer && ./gradlew :core:data:assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

**Step 5:** Commit.
```bash
git add core/data
git commit -m "feat(data): add shizuku-related settings fields"
```

---

## Task 4: ShizukuManager — state + permission

**Files:**
- Create: `core/service/src/main/kotlin/dev/xitee/sleeptimer/core/service/shizuku/ShizukuManager.kt`

**Step 1:** Create the file:

```kotlin
package dev.xitee.sleeptimer.core.service.shizuku

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    enum class State {
        NotInstalled,        // Shizuku app package not present
        NotRunning,          // Installed but binder not alive — user must start the service in the Shizuku app
        PermissionRequired,  // Running but our app has not been granted access yet
        Ready,               // All good — we can execute commands
    }

    private val _state = MutableStateFlow(computeState())
    val state: StateFlow<State> = _state.asStateFlow()

    private val binderReceived = Shizuku.OnBinderReceivedListener { refresh() }
    private val binderDead = Shizuku.OnBinderDeadListener { refresh() }
    private val permissionResult = Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionResult)
    }

    fun refresh() {
        _state.value = computeState()
    }

    fun isReady(): Boolean = _state.value == State.Ready

    fun requestPermission() {
        if (computeState() != State.PermissionRequired) return
        if (Shizuku.isPreV11()) return
        Shizuku.requestPermission(REQUEST_CODE)
    }

    private fun computeState(): State {
        if (!isShizukuInstalled()) return State.NotInstalled
        val running = try { Shizuku.pingBinder() } catch (_: Throwable) { false }
        if (!running) return State.NotRunning
        if (Shizuku.isPreV11()) return State.PermissionRequired
        val granted = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) { false }
        return if (granted) State.Ready else State.PermissionRequired
    }

    private fun isShizukuInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    companion object {
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val REQUEST_CODE = 9871
    }
}
```

**Why this design:**
- Singleton so the binder listeners register exactly once for the app lifetime.
- `StateFlow` so the UI can reactively show dialog content.
- `refresh()` is public so the UI can force a recompute after e.g. returning from a "Install Shizuku" deep-link.
- Silent catch on `Shizuku.pingBinder()` — in some edge cases it throws before the provider has initialized.

**Step 2:** Build.
```bash
cd /home/mato/projects/sleep-timer && ./gradlew :core:service:assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

**Step 3:** Commit.
```bash
git add core/service/src/main/kotlin/dev/xitee/sleeptimer/core/service/shizuku/ShizukuManager.kt
git commit -m "feat(service): add ShizukuManager for state + permission"
```

---

## Task 5: ShizukuShell — command execution

**Files:**
- Create: `core/service/src/main/kotlin/dev/xitee/sleeptimer/core/service/shizuku/ShizukuShell.kt`

**Step 1:** Create the file:

```kotlin
package dev.xitee.sleeptimer.core.service.shizuku

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuShell @Inject constructor(
    private val shizukuManager: ShizukuManager,
) {

    /**
     * Runs a shell command via Shizuku. Returns true on exit code 0.
     * Safe to call when Shizuku is not ready — returns false silently.
     */
    suspend fun exec(vararg args: String): Boolean = withContext(Dispatchers.IO) {
        if (!shizukuManager.isReady()) {
            Log.w(TAG, "exec called but Shizuku not ready: state=${shizukuManager.state.value}")
            return@withContext false
        }
        try {
            // Shizuku.newProcess is @hide + deprecated in newer Shizuku API but is still the
            // documented path for running shell commands as shell UID. See docs.shizuku.rikka.app.
            @Suppress("DEPRECATION")
            val process = Shizuku.newProcess(args, null, null)
            val exit = process.waitFor()
            if (exit != 0) {
                Log.w(TAG, "cmd=${args.joinToString(" ")} exit=$exit")
            }
            exit == 0
        } catch (t: Throwable) {
            Log.e(TAG, "exec failed: ${args.joinToString(" ")}", t)
            false
        }
    }

    private companion object {
        const val TAG = "ShizukuShell"
    }
}
```

**Why:**
- `Dispatchers.IO` because `waitFor()` blocks.
- Return Boolean so callers can short-circuit. They never see exceptions — matches the "silent failure" pattern established by `ScreenLockHelper`.
- Suppressed deprecation: `Shizuku.newProcess` is the canonical shell-exec path in Shizuku; the deprecation is an API-cleanliness flag, not a functional removal. If it eventually goes away we switch to `ShizukuBinderWrapper` + system services.

**Step 2:** Build.
```bash
cd /home/mato/projects/sleep-timer && ./gradlew :core:service:assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

**Step 3:** Commit.
```bash
git add core/service/src/main/kotlin/dev/xitee/sleeptimer/core/service/shizuku/ShizukuShell.kt
git commit -m "feat(service): add ShizukuShell for privileged command exec"
```

---

## Task 6: Action helpers — screen, wifi, bluetooth

**Files:**
- Create: `core/service/src/main/kotlin/dev/xitee/sleeptimer/core/service/shizuku/ShizukuScreenOffHelper.kt`
- Create: `core/service/src/main/kotlin/dev/xitee/sleeptimer/core/service/shizuku/ShizukuWifiController.kt`
- Create: `core/service/src/main/kotlin/dev/xitee/sleeptimer/core/service/shizuku/ShizukuBluetoothController.kt`

**Step 1:** Screen off helper:

```kotlin
package dev.xitee.sleeptimer.core.service.shizuku

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Soft screen-off via simulated power key press (KEYCODE_POWER = 26).
 * Keeps biometric unlock valid — unlike DevicePolicyManager.lockNow(),
 * which forces the next unlock to require credentials.
 */
@Singleton
class ShizukuScreenOffHelper @Inject constructor(
    private val shell: ShizukuShell,
) {
    suspend fun turnOffScreen(): Boolean = shell.exec("input", "keyevent", "26")
}
```

**Step 2:** Wi-Fi controller:

```kotlin
package dev.xitee.sleeptimer.core.service.shizuku

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuWifiController @Inject constructor(
    private val shell: ShizukuShell,
) {
    suspend fun disableWifi(): Boolean = shell.exec("svc", "wifi", "disable")
}
```

**Step 3:** Bluetooth controller:

```kotlin
package dev.xitee.sleeptimer.core.service.shizuku

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuBluetoothController @Inject constructor(
    private val shell: ShizukuShell,
) {
    suspend fun disableBluetooth(): Boolean = shell.exec("svc", "bluetooth", "disable")
}
```

**Step 4:** Build.
```bash
cd /home/mato/projects/sleep-timer && ./gradlew :core:service:assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

**Step 5:** Commit.
```bash
git add core/service/src/main/kotlin/dev/xitee/sleeptimer/core/service/shizuku
git commit -m "feat(service): add screen/wifi/bluetooth Shizuku action helpers"
```

---

## Task 7: Wire into SleepTimerService.onTimerExpired()

**Files:**
- Modify: `core/service/src/main/kotlin/dev/xitee/sleeptimer/core/service/SleepTimerService.kt`

**Step 1:** Add injected fields (after existing `@Inject` fields, around line 38):

```kotlin
@Inject lateinit var shizukuManager: ShizukuManager
@Inject lateinit var shizukuScreenOffHelper: ShizukuScreenOffHelper
@Inject lateinit var shizukuWifiController: ShizukuWifiController
@Inject lateinit var shizukuBluetoothController: ShizukuBluetoothController
```

Plus imports:
```kotlin
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuBluetoothController
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuManager
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuScreenOffHelper
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuWifiController
```

**Step 2:** Replace `onTimerExpired()` body (lines 178-198) with:

```kotlin
private fun onTimerExpired() {
    serviceScope.launch {
        val settings: UserSettings = settingsRepository.settings.first()

        if (settings.stopMediaPlayback) {
            updateTimerState(TimerPhase.FADING_OUT)
            notificationManager.updateNotification(0, stepMinutes)
            mediaVolumeController.fadeOutAndPause(settings.fadeOutDurationSeconds)
        }

        if (settings.turnOffWifi && shizukuManager.isReady()) {
            shizukuWifiController.disableWifi()
        }

        if (settings.turnOffBluetooth && shizukuManager.isReady()) {
            shizukuBluetoothController.disableBluetooth()
        }

        if (settings.screenOff) {
            val usedShizuku = if (settings.softScreenOff && shizukuManager.isReady()) {
                shizukuScreenOffHelper.turnOffScreen()
            } else false
            if (!usedShizuku) {
                // Fallback to DevicePolicyManager.lockNow(). Also the path when the user
                // explicitly chose hard-lock (softScreenOff = false).
                screenLockHelper.lockScreen()
            }
        }

        timerRepository.updateState(TimerState())
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}
```

**Why this order:** Wi-Fi and Bluetooth run **before** screen-off, because:
1. Disabling Wi-Fi while the screen is already off can race with doze/idle state transitions on some OEMs.
2. If screen-off (via power key) fails due to an edge case, the connectivity actions still complete.

**Step 3:** Build + install on device.
```bash
cd /home/mato/projects/sleep-timer && ./gradlew :app:assembleDebug && ./gradlew :app:installDebug
```
Expected: `BUILD SUCCESSFUL`, `Installed on 1 device`.

**Step 4:** Commit.
```bash
git add core/service/src/main/kotlin/dev/xitee/sleeptimer/core/service/SleepTimerService.kt
git commit -m "feat(service): wire shizuku actions into timer expiration"
```

---

## Task 8: Shizuku explainer dialog composable

**Files:**
- Create: `feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/settings/components/ShizukuRequiredDialog.kt`

**Step 1:** Create composable that adapts its content to `ShizukuManager.State`:

```kotlin
package dev.xitee.sleeptimer.feature.timer.settings.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuManager
import dev.xitee.sleeptimer.feature.timer.R

@Composable
fun ShizukuRequiredDialog(
    state: ShizukuManager.State,
    featureExplanation: String,   // e.g. "This feature turns off Wi-Fi via Shizuku."
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    val (bodyRes, confirmRes, onConfirm) = when (state) {
        ShizukuManager.State.NotInstalled -> Triple(
            R.string.shizuku_body_not_installed,
            R.string.shizuku_action_install,
            {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://shizuku.rikka.app/".toUri(),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                onDismiss()
            },
        )
        ShizukuManager.State.NotRunning -> Triple(
            R.string.shizuku_body_not_running,
            R.string.shizuku_action_open,
            {
                val pm = context.packageManager
                val intent = pm.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                intent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
                onDismiss()
            },
        )
        ShizukuManager.State.PermissionRequired -> Triple(
            R.string.shizuku_body_permission,
            R.string.shizuku_action_grant,
            { onRequestPermission(); onDismiss() },
        )
        ShizukuManager.State.Ready -> Triple(
            R.string.shizuku_body_ready, // unreachable in practice — caller only shows dialog when not Ready
            R.string.shizuku_action_ok,
            onDismiss,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shizuku_dialog_title)) },
        text = {
            Column {
                Text(featureExplanation)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(bodyRes))
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text(stringResource(confirmRes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.shizuku_action_cancel))
            }
        },
    )
}
```

Missing import: add `import androidx.compose.ui.Modifier` if not inferred.

**Step 2:** Add strings to `feature/timer/src/main/res/values/strings.xml` (end of file, before `</resources>`):

```xml
<!-- Shizuku dialog -->
<string name="shizuku_dialog_title">Shizuku required</string>
<string name="shizuku_body_not_installed">Shizuku is not installed. Tap below to learn how to install and set it up.</string>
<string name="shizuku_body_not_running">Shizuku is installed but not running. Open the Shizuku app and start the service.</string>
<string name="shizuku_body_permission">SleepTimer needs permission to use Shizuku.</string>
<string name="shizuku_body_ready">Shizuku is ready.</string>
<string name="shizuku_action_install">Install guide</string>
<string name="shizuku_action_open">Open Shizuku</string>
<string name="shizuku_action_grant">Grant permission</string>
<string name="shizuku_action_ok">OK</string>
<string name="shizuku_action_cancel">Cancel</string>

<!-- Shizuku feature explanations (per-toggle) -->
<string name="shizuku_feature_soft_screen_off">Soft screen-off simulates a power-button press via Shizuku, so biometric unlock keeps working.</string>
<string name="shizuku_feature_wifi">Turning off Wi-Fi after the timer ends requires Shizuku because modern Android versions block apps from toggling Wi-Fi directly.</string>
<string name="shizuku_feature_bluetooth">Turning off Bluetooth after the timer ends requires Shizuku because modern Android versions block apps from toggling Bluetooth directly.</string>
```

**Step 3:** Mirror to `feature/timer/src/main/res/values-de/strings.xml`:

```xml
<!-- Shizuku Dialog -->
<string name="shizuku_dialog_title">Shizuku erforderlich</string>
<string name="shizuku_body_not_installed">Shizuku ist nicht installiert. Tippe unten, um eine Installations- und Einrichtungsanleitung zu öffnen.</string>
<string name="shizuku_body_not_running">Shizuku ist installiert, aber nicht aktiv. Öffne die Shizuku-App und starte den Dienst.</string>
<string name="shizuku_body_permission">SleepTimer benötigt die Berechtigung, Shizuku zu verwenden.</string>
<string name="shizuku_body_ready">Shizuku ist bereit.</string>
<string name="shizuku_action_install">Anleitung öffnen</string>
<string name="shizuku_action_open">Shizuku öffnen</string>
<string name="shizuku_action_grant">Berechtigung erteilen</string>
<string name="shizuku_action_ok">OK</string>
<string name="shizuku_action_cancel">Abbrechen</string>

<!-- Shizuku-Feature-Erklärungen (pro Schalter) -->
<string name="shizuku_feature_soft_screen_off">Sanftes Bildschirmabschalten simuliert einen Druck der Power-Taste via Shizuku, sodass die biometrische Entsperrung weiterhin funktioniert.</string>
<string name="shizuku_feature_wifi">WLAN nach Timer-Ende auszuschalten erfordert Shizuku, da moderne Android-Versionen Apps das direkte Umschalten von WLAN verbieten.</string>
<string name="shizuku_feature_bluetooth">Bluetooth nach Timer-Ende auszuschalten erfordert Shizuku, da moderne Android-Versionen Apps das direkte Umschalten von Bluetooth verbieten.</string>
```

**Step 4:** `feature/timer` needs access to `ShizukuManager` — check that `feature/timer/build.gradle.kts` depends on `core:service`.

```bash
grep -n "core:service" /home/mato/projects/sleep-timer/feature/timer/build.gradle.kts
```
If there is no match, add `implementation(project(":core:service"))` to the `dependencies { }` block of that file. This is expected — historically `feature:timer` only depended on `core:data`.

**Step 5:** Build.
```bash
cd /home/mato/projects/sleep-timer && ./gradlew :feature:timer:assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

**Step 6:** Commit.
```bash
git add feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/settings/components/ShizukuRequiredDialog.kt \
        feature/timer/src/main/res/values/strings.xml \
        feature/timer/src/main/res/values-de/strings.xml \
        feature/timer/build.gradle.kts
git commit -m "feat(settings): add Shizuku-required dialog + strings"
```

---

## Task 9: Expose Shizuku state in SettingsViewModel + UiState

**Files:**
- Modify: `feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/settings/SettingsUiState.kt`
- Modify: `feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/settings/SettingsViewModel.kt`

**Step 1:** Update `SettingsUiState` to carry the Shizuku state:

```kotlin
package dev.xitee.sleeptimer.feature.timer.settings

import dev.xitee.sleeptimer.core.data.model.UserSettings
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuManager

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val isDeviceAdminEnabled: Boolean = false,
    val shizukuState: ShizukuManager.State = ShizukuManager.State.NotInstalled,
)
```

**Step 2:** Update `SettingsViewModel`:

Inject `ShizukuManager` and combine its state with the settings flow. Add three new update methods.

Replace the class body accordingly — new version:

```kotlin
package dev.xitee.sleeptimer.feature.timer.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.xitee.sleeptimer.core.data.model.ThemeId
import dev.xitee.sleeptimer.core.data.repository.SettingsRepository
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val shizukuManager: ShizukuManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val devicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(
        context,
        "dev.xitee.sleeptimer.receiver.SleepTimerDeviceAdminReceiver",
    )

    val uiState: StateFlow<SettingsUiState?> =
        combine(settingsRepository.settings, shizukuManager.state) { settings, shizukuState ->
            SettingsUiState(
                settings = settings,
                isDeviceAdminEnabled = devicePolicyManager.isAdminActive(adminComponent),
                shizukuState = shizukuState,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun isDeviceAdminActive(): Boolean =
        devicePolicyManager.isAdminActive(adminComponent)

    fun getAdminComponent(): ComponentName = adminComponent

    fun refreshShizuku() = shizukuManager.refresh()
    fun requestShizukuPermission() = shizukuManager.requestPermission()
    fun isShizukuReady(): Boolean = shizukuManager.isReady()

    fun updateStopMediaPlayback(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateStopMediaPlayback(enabled) }
    }

    fun updateFadeOutDuration(seconds: Int) {
        viewModelScope.launch { settingsRepository.updateFadeOutDuration(seconds) }
    }

    fun updateScreenOff(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateScreenOff(enabled) }
    }

    fun updateSoftScreenOff(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSoftScreenOff(enabled) }
    }

    fun updateTurnOffWifi(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateTurnOffWifi(enabled) }
    }

    fun updateTurnOffBluetooth(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateTurnOffBluetooth(enabled) }
    }

    fun updateHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateHapticFeedback(enabled) }
    }

    fun updateTheme(theme: ThemeId) {
        viewModelScope.launch { settingsRepository.updateTheme(theme) }
    }

    fun updateStarsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateStarsEnabled(enabled) }
    }

    fun updateStepMinutes(minutes: Int) {
        viewModelScope.launch { settingsRepository.updateStepMinutes(minutes) }
    }
}
```

**Why `combine`:** Re-emits the UI state whenever either settings or Shizuku state changes, so e.g. granting permission in the Shizuku app immediately enables the toggle in our settings screen.

**Why `refreshShizuku()`:** Called when the settings screen resumes — if the user went to the Shizuku app and started the service, the next `onResume` should recompute state (the binder listener covers most cases, but `ACTION_VIEW` deep-link returns might miss the event on some OEMs).

**Step 3:** Build.
```bash
cd /home/mato/projects/sleep-timer && ./gradlew :feature:timer:assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

**Step 4:** Commit.
```bash
git add feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/settings
git commit -m "feat(settings): expose Shizuku state in SettingsViewModel"
```

---

## Task 10: Settings UI — three new toggles with Shizuku gating

**Files:**
- Modify: `feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/settings/SettingsScreen.kt`
- Modify: `feature/timer/src/main/res/values/strings.xml`
- Modify: `feature/timer/src/main/res/values-de/strings.xml`

**Step 1:** Add strings to `values/strings.xml`:

```xml
<!-- Shizuku feature toggles -->
<string name="soft_screen_off_title">Soft screen-off</string>
<string name="soft_screen_off_description">Use power-button simulation via Shizuku — biometric unlock keeps working</string>
<string name="wifi_off_title">Turn off Wi-Fi</string>
<string name="wifi_off_description">Disable Wi-Fi when timer ends (requires Shizuku)</string>
<string name="bluetooth_off_title">Turn off Bluetooth</string>
<string name="bluetooth_off_description">Disable Bluetooth when timer ends (requires Shizuku)</string>
```

And to `values-de/strings.xml`:

```xml
<!-- Shizuku-Funktionen -->
<string name="soft_screen_off_title">Sanftes Bildschirmabschalten</string>
<string name="soft_screen_off_description">Power-Taste simulieren (via Shizuku) — biometrische Entsperrung bleibt nutzbar</string>
<string name="wifi_off_title">WLAN ausschalten</string>
<string name="wifi_off_description">WLAN deaktivieren wenn Timer endet (benötigt Shizuku)</string>
<string name="bluetooth_off_title">Bluetooth ausschalten</string>
<string name="bluetooth_off_description">Bluetooth deaktivieren wenn Timer endet (benötigt Shizuku)</string>
```

**Step 2:** Update `SettingsScreen.kt`. Add these imports:

```kotlin
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.xitee.sleeptimer.core.service.shizuku.ShizukuManager
import dev.xitee.sleeptimer.feature.timer.settings.components.ShizukuRequiredDialog
```

Inside `SettingsContent` (`@Composable private fun SettingsContent(...)`), above `TimerBackground(...)`, add dialog state management:

```kotlin
var shizukuDialogExplanation by remember { mutableStateOf<String?>(null) }
var pendingShizukuToggle by remember { mutableStateOf<(() -> Unit)?>(null) }

fun requestWithShizuku(explanation: String, enableAction: () -> Unit) {
    if (viewModel.isShizukuReady()) {
        enableAction()
    } else {
        shizukuDialogExplanation = explanation
        pendingShizukuToggle = enableAction
    }
}

if (shizukuDialogExplanation != null) {
    ShizukuRequiredDialog(
        state = uiState.shizukuState,
        featureExplanation = shizukuDialogExplanation!!,
        onRequestPermission = { viewModel.requestShizukuPermission() },
        onDismiss = {
            shizukuDialogExplanation = null
            pendingShizukuToggle = null
        },
    )
}

// Auto-complete the pending toggle if Shizuku just became Ready while dialog was open.
// (User clicked "Grant permission", Shizuku dialog appeared, they accepted, binder listener fired.)
if (pendingShizukuToggle != null && uiState.shizukuState == ShizukuManager.State.Ready) {
    pendingShizukuToggle?.invoke()
    pendingShizukuToggle = null
    shizukuDialogExplanation = null
}
```

**Step 3:** Modify the existing screen-off toggle block (currently lines 138-164) to add a nested soft-screen-off sub-toggle. Replace that block with:

```kotlin
SettingsToggleRow(
    icon = Icons.Default.PhoneAndroid,
    title = stringResource(R.string.screen_title),
    description = if (uiState.isDeviceAdminEnabled || uiState.settings.softScreenOff) {
        stringResource(R.string.screen_description)
    } else {
        stringResource(R.string.screen_admin_required)
    },
    checked = uiState.settings.screenOff,
    onCheckedChange = { enabled ->
        if (enabled && !uiState.settings.softScreenOff && !viewModel.isDeviceAdminActive()) {
            // Only the hard-lock path needs device admin.
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    viewModel.getAdminComponent(),
                )
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    context.getString(R.string.screen_description),
                )
            }
            deviceAdminLauncher.launch(intent)
        } else {
            viewModel.updateScreenOff(enabled)
        }
    },
)

// Soft screen-off (Shizuku) — only relevant if parent toggle is on.
if (uiState.settings.screenOff) {
    SettingsToggleRow(
        icon = Icons.Default.Nightlight,
        title = stringResource(R.string.soft_screen_off_title),
        description = stringResource(R.string.soft_screen_off_description),
        checked = uiState.settings.softScreenOff,
        onCheckedChange = { enabled ->
            if (enabled) {
                requestWithShizuku(context.getString(R.string.shizuku_feature_soft_screen_off)) {
                    viewModel.updateSoftScreenOff(true)
                }
            } else {
                viewModel.updateSoftScreenOff(false)
            }
        },
    )
}

SettingsToggleRow(
    icon = Icons.Default.Wifi,
    title = stringResource(R.string.wifi_off_title),
    description = stringResource(R.string.wifi_off_description),
    checked = uiState.settings.turnOffWifi,
    onCheckedChange = { enabled ->
        if (enabled) {
            requestWithShizuku(context.getString(R.string.shizuku_feature_wifi)) {
                viewModel.updateTurnOffWifi(true)
            }
        } else {
            viewModel.updateTurnOffWifi(false)
        }
    },
)

SettingsToggleRow(
    icon = Icons.Default.Bluetooth,
    title = stringResource(R.string.bluetooth_off_title),
    description = stringResource(R.string.bluetooth_off_description),
    checked = uiState.settings.turnOffBluetooth,
    onCheckedChange = { enabled ->
        if (enabled) {
            requestWithShizuku(context.getString(R.string.shizuku_feature_bluetooth)) {
                viewModel.updateTurnOffBluetooth(true)
            }
        } else {
            viewModel.updateTurnOffBluetooth(false)
        }
    },
)
```

**Step 4:** Ensure `feature/timer/build.gradle.kts` has `material-icons-extended` (needed for `Wifi`, `Bluetooth`, `Nightlight`):

```bash
grep -n material-icons-extended /home/mato/projects/sleep-timer/feature/timer/build.gradle.kts
```

Expected: a match. If not, add `implementation(libs.androidx.compose.material.icons.extended)` to that file's `dependencies {}` block. (It's present in `libs.versions.toml` already per Task 0 exploration.)

**Step 5:** Add `LaunchedEffect` to refresh Shizuku state when the user returns from the Shizuku app — find `SettingsContent` entry and add near the top of the composable body:

```kotlin
androidx.compose.runtime.LaunchedEffect(Unit) {
    viewModel.refreshShizuku()
}
```

(Optional — the binder listener usually covers this. Include only if manual verification shows stale state after returning from Shizuku.)

**Step 6:** Build.
```bash
cd /home/mato/projects/sleep-timer && ./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

**Step 7:** Commit.
```bash
git add feature/timer
git commit -m "feat(settings): add soft-lock, wifi-off, bluetooth-off toggles with Shizuku gating"
```

---

## Task 11: Manual verification on device

**Why this is a full task:** No automated test infrastructure exists — device verification **is** the test suite.

**Prereq:** Physical device or emulator with Shizuku installed. Have the Shizuku app ready, connected via Wireless Debugging or rooted.

**Setup:**
```bash
cd /home/mato/projects/sleep-timer && ./gradlew :app:installDebug
```

**Test 1: App works without Shizuku at all**
1. Uninstall Shizuku from device (if present): `adb shell pm uninstall moe.shizuku.privileged.api`
2. Open SleepTimer.
3. Open settings. **Expect:** No permission prompt. All toggles visible, the three Shizuku-gated toggles are off and interactive.
4. Toggle "Turn off Wi-Fi" → **Expect:** Dialog appears with "Shizuku is not installed" body and "Install guide" action. Cancel.
5. Toggle "Screen lock" ON → **Expect:** Device Admin intent launches (existing behavior, unchanged).
6. Start a short timer (1 min), wait for it to complete with playback running. **Expect:** Audio stops with fade-out; screen locks hard (credential required on next unlock). No crashes, no Shizuku errors in logcat.

**Test 2: Shizuku installed but not running**
1. `adb install` the Shizuku APK but do NOT start the service.
2. Toggle "Turn off Bluetooth" → **Expect:** Dialog with "Shizuku installed but not running" and "Open Shizuku" button.

**Test 3: Shizuku running, permission flow**
1. Start Shizuku service (Wireless Debugging pairing or root).
2. Toggle "Turn off Wi-Fi" → **Expect:** Dialog with "SleepTimer needs permission".
3. Tap "Grant permission" → **Expect:** Shizuku's system dialog opens, accept.
4. **Expect:** Dialog dismisses, toggle becomes ON. (The auto-complete from Task 10, Step 2 handles this.)

**Test 4: Soft screen-off keeps biometrics valid**
1. With Shizuku ready: toggle "Screen lock" ON, then toggle "Soft screen-off" ON (permission already granted).
2. Start a 1-minute timer with audio playing.
3. Wait for expiration. **Expect:** Audio fades, screen turns off.
4. Power-button wake. **Expect:** Fingerprint/face unlock available — not forced to enter PIN. This is the primary success criterion for this feature.

**Test 5: Wi-Fi and Bluetooth actually toggle**
1. Enable Wi-Fi and Bluetooth in Quick Settings.
2. Toggle both new options in SleepTimer.
3. Start 1-minute timer, wait. **Expect:** Wi-Fi and Bluetooth both turn off after timer ends.

**Test 6: Logcat sanity**
```bash
adb logcat -s ShizukuShell SleepTimerService
```
Expected: no `exec failed` errors when Shizuku is ready. Optional log lines when not ready, warning level.

**Test 7: Hard-lock fallback still works when soft-lock was requested but Shizuku later revoked**
1. Enable soft screen-off + permission.
2. Revoke Shizuku permission in Shizuku app.
3. Run timer. **Expect:** Service detects Shizuku not ready, falls back to `lockScreen()` (hard lock). No crash.

**Step 2:** If any test fails, fix the relevant task's code and repeat that test.

**Step 3:** Final commit (if test-driven fixes were needed — otherwise skip).

**Step 4:** Clean up before PR:
```bash
git -C /home/mato/projects/sleep-timer log --oneline main..HEAD
```
Confirm the commit sequence is clean and each commit tells a story.

---

## Task 12: Open PR

**Step 1:**
```bash
git -C /home/mato/projects/sleep-timer push -u origin feat/shizuku-integration
gh pr create --title "Add Shizuku integration for soft lock, Wi-Fi, Bluetooth" --body "..."
```

Body should include:
- Summary of new features
- The tradeoff: Shizuku is optional; app works fully without it
- Manual test matrix covered
- Screenshots or screen recording of the new settings + dialog

---

## Summary — what this plan produces

- **New UX:** Three toggles in Settings — soft screen-off (nested under the existing screen-off toggle), Wi-Fi off, Bluetooth off — each gated by an explainer dialog shown only when the user actively enables the feature.
- **No startup prompts:** Shizuku permission is requested only when the user tries to enable a Shizuku-requiring toggle, with an in-app dialog explaining why.
- **Graceful fallback:** Screen-off falls back to the existing Device Admin hard lock when soft-lock is unchecked or Shizuku is unavailable.
- **Code locations:** Backend in `core/service/.../shizuku/`, UI plumbing in `feature/timer/.../settings/`, settings persistence extended in `core/data/.../`.
- **Risk surface:** Silent-failure policy on every Shizuku call — matches the existing code's posture. Logcat warnings are the only diagnostic signal; that's acceptable for v1.
