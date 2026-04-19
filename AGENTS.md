# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & test commands

Toolchain: JDK 17, Android SDK platform 36 (`compileSdk`/`targetSdk`), `minSdk = 26`, AGP 9.1.1, Kotlin 2.3.20, Compose BoM 2026.03.01. Hilt + KSP for DI.

- `./gradlew assembleDebug` — build the debug APK (what CI uses to verify)
- `./gradlew assembleRelease` — build release; signs with the `release` config only when `SIGNING_KEYSTORE_PATH` is set in the environment (with `SIGNING_KEYSTORE_PASSWORD` / `SIGNING_KEY_ALIAS` / `SIGNING_KEY_PASSWORD`), else falls back to debug signing so local builds succeed without secrets
- `./gradlew lint` — Android lint across all modules (required to pass in CI)
- `./gradlew testDebugUnitTest` — unit tests (currently no test sources exist; CI runs the task anyway to catch new ones)
- Single-module variants: `./gradlew :feature:timer:lintDebug`, `./gradlew :core:service:testDebugUnitTest`, etc.

`versionCode` in `app/build.gradle.kts` follows the schema `major*100_000 + minor*1_000 + patch*10`; the last digit is reserved for hotfixes. Bump both `versionCode` and `versionName` together. Release tags named `v*` trigger `.github/workflows/release.yml`, which builds a signed APK and publishes a GitHub Release.

## Module architecture

Four Gradle modules with a strict one-way dependency flow:

```
app ──▶ feature:timer ──▶ core:service ──▶ core:data
                    └───────────────────▶ core:data
```

- **`app/`** — `@HiltAndroidApp` (`SleepTimerApp`), `MainActivity` (edge-to-edge Compose host), `SleepTimerNavHost` (type-safe Navigation-Compose routes in `navigation/Routes.kt`), and `SleepTimerDeviceAdminReceiver` for the hard-lock path. The `ShizukuProvider` is declared here in `AndroidManifest.xml`; `shizuku-provider` is only on `:app`'s classpath so lint can resolve it.
- **`feature:timer/`** — all Compose UI: `timer/` (dial, starfield, `TimerViewModel`, `AppOrientationController`), `settings/`, `theme/` (light/dark + six palettes in `AppThemes.kt`, animated transitions in `AnimatedAppTheme.kt`), `about/`. ViewModels use `@HiltViewModel` and dispatch to the service via `Intent`s (see below).
- **`core:service/`** — the `SleepTimerService` foreground service (the runtime "source of truth" while a timer is active), `TimerNotificationManager` (notification with +/−/cancel actions), `MediaVolumeController` (fade-out / fade-in), `ScreenLockHelper` (Device-Admin `lockNow`), and `shizuku/` (Shizuku state machine + Wi-Fi, Bluetooth, soft screen-off controllers).
- **`core:data/`** — `UserSettings` + `TimerState`/`TimerPhase` models, `SettingsRepository` (Jetpack DataStore Preferences, single `settings` file), `TimerRepository` (in-process `StateFlow<TimerState>` — process state, **not** persisted), and Hilt `DataModule`. Repositories are bound as `@Singleton` via `@Binds`.

All modules apply the Kotlin compiler flag `-Xannotation-default-target=param-property` (needed for Hilt/Compose annotation targeting under Kotlin 2.x).

## The timer runtime contract

UI never mutates timer state directly — it sends intents to `SleepTimerService`. The action names in `SleepTimerService.Companion` are the public contract:

- `ACTION_START` + `EXTRA_DURATION_MILLIS` → begins countdown, calls `startForeground` with `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`
- `ACTION_ADD_MINUTES` / `ACTION_SUBTRACT_MINUTES` → use the current `stepMinutes` from settings
- `ACTION_SET_MINUTES` + `EXTRA_MINUTES` → absolute set (dial-commit while running)
- `ACTION_CANCEL` → cancels countdown + in-flight fade, restores volume, stops the service

The service writes to `TimerRepositoryImpl` on every tick; the UI observes `timerRepository.timerState` to render. `UserSettings.stepMinutes` is primed synchronously in `onCreate` via `runBlocking` so the very first notification uses the persisted step value, not the `UserSettings()` default.

Two non-obvious behaviors worth preserving when editing the service:

1. **Restart-resilience**: if `onStartCommand` is invoked by the OS (or by a stale `PendingIntent` from a surviving notification) with any action other than `ACTION_START` while `countdownJob == null`, the service calls `stopSelf` **before** returning. Skipping this branch will crash with `ForegroundServiceDidNotStartInTimeException` because `startForeground` is never called within the 5-second window.
2. **Add-during-fade**: when the user taps "+" while the timer is in `FADING_OUT`, the new countdown job wraps `oldJob.cancelAndJoin()` so a subsequent Cancel can interrupt the fade-in + restart sequence. The countdown and fade-in run in parallel — the clock starts from the tap, not from the end of the fade-in.

## Shizuku integration

Shizuku is optional. `ShizukuManager` models a four-state machine (`NotInstalled` / `NotRunning` / `PermissionRequired` / `Ready`) driven by `Shizuku.OnBinderReceivedListener` etc. Use `awaitInitialState(timeoutMs)` (not `isReady()`) for startup checks — a cold `pingBinder()` race otherwise reports `NotRunning` even when Shizuku is up. All three opt-in features (Wi-Fi off, Bluetooth off, soft screen-off) go through Shizuku because modern Android blocks direct toggling; the hard-lock path uses Device Admin and is independent of Shizuku. The `<queries>` block in `app/src/main/AndroidManifest.xml` is required on targetSdk 30+ to see the Shizuku package.

## Privacy/scope constraints (hard rules)

- **No `INTERNET` permission.** The app must never make a network call. Do not add analytics, crash reporting, Firebase, Play Services, ads, or any third-party SDK that opens a socket.
- **All settings live in DataStore Preferences** (`UserSettings`). Add new settings by extending `UserSettings`, mapping a new `Preferences.Key` in `SettingsRepositoryImpl`, and exposing an `updateX` method.
- Strings are localized — any user-visible string added to `values/strings.xml` must also be translated in `values-de/strings.xml`.

## Planning docs

`docs/plans/` holds dated design notes for in-flight or completed features (e.g. Shizuku integration, in-app rotation). Read the relevant plan before making large changes in those areas; the reasoning often isn't repeated in code comments.
