# In-App Rotation Design

**Date:** 2026-04-18
**Status:** Approved, ready for implementation

## Problem

On device rotation the system recreates the Activity in landscape, which breaks the current TimerScreen layout — the dial overflows the screen. The layout is only designed for portrait.

## Goal

Keep the Activity locked to portrait and handle rotation *inside* Compose, rotating only text and icons while elements stay in place. The user (typically lying in bed) should be able to tilt the phone and still read the UI.

## Approach

### 1. Orientation detection & lock

- Lock the Activity to portrait via `android:screenOrientation="portrait"` on `<activity>` in `app/src/main/AndroidManifest.xml`. Compose always renders the portrait layout regardless of physical pose.
- Detect physical orientation with `OrientationEventListener`. Snap the continuous angle (0–359°) to four buckets — `PORTRAIT`, `LANDSCAPE_LEFT`, `PORTRAIT_REVERSED`, `LANDSCAPE_RIGHT` — with a ~30° hysteresis so small wobbles don't flip state.
- Expose the result as a Compose `State<Orientation>` via a helper composable `rememberDeviceOrientation()` using `DisposableEffect` to manage the listener lifecycle. Scope: feature-local (only `TimerScreen` uses it).
- All four orientations are supported, including 180° (reversed portrait).

### 2. Layout in rotated state

Only **two** layout states — portrait vs. rotated. The rotation *direction* (90° / 180° / 270°) only changes per-element rotation angles, not physical placement.

| Element | Portrait | Rotated (physical placement) | Rendering rotation |
|---|---|---|---|
| TopBar title | centered in TopBar | right edge of screen, under the settings icon, vertical | `+angle` |
| Settings icon | top-right corner | unchanged | `+angle` |
| Dial (incl. numbers) | center | unchanged | `+angle` |
| "Endet um" text | under dial, centered | under dial, centered (may shift slightly) | `+angle` |
| Minus / Play / Plus row | bottom row | unchanged | icons only get `+angle` |

`angle` is the counter-rotation of the device pose — e.g. device tilted 90° CW → content rotates –90° so it reads upright to the user.

**Title placement in rotated mode:** directly below the settings icon on the right edge, ~8dp gap, rendered as a vertical banner (height ≈ 120dp).

### 3. Animation

- Single `animateFloatAsState` in `TimerContent` computes `animatedAngle` from the target orientation angle, with `tween(350ms, FastOutSlowInEasing)`.
- The same `animatedAngle` is threaded down to every rotating element (dial, time display, "Endet um" text, button icons, title, settings icon). All rotate synchronously.
- Rotations are applied via `Modifier.graphicsLayer { rotationZ = animatedAngle }`, rotating around each element's center.
- **Shortest-path guard:** when the target angle jumps (e.g. 90° → –90°), remember the last angle and pick the "continue rotating same direction" equivalent so the animation doesn't reverse. E.g. from 180° the next target is expressed as 270° rather than –90°.
- **Title layout change (portrait ↔ rotated) — Crossfade:** render both positions simultaneously; fade the portrait title out and the edge title in over ~350ms, synchronised with the rotation animation. Simple and clean; the fade masks the positional discontinuity.
- No animation for the "Endet um" box position shift (spec says it may shift). Buttons don't move at all — only the icons inside rotate.

## Files expected to change

- `app/src/main/AndroidManifest.xml` — add `screenOrientation="portrait"`.
- `feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/timer/TimerScreen.kt` — rotated layout branch, animated angle threading.
- New: `feature/timer/src/main/kotlin/dev/xitee/sleeptimer/feature/timer/timer/DeviceOrientation.kt` — enum + `rememberDeviceOrientation()`.
- Button / icon components under `feature/timer/.../timer/components/` — accept an `iconRotation: Float` parameter.

## Out of scope

- Settings screen rotation (stays portrait-only, since the Activity is locked).
- About screen rotation (same).
