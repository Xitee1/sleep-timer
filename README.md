# SleepTimer

A minimal, privacy-respecting sleep timer for Android. Starts a countdown that
stops media playback, fades audio out, and optionally locks the screen when it
ends — so you can fall asleep without your phone draining the battery or
blaring podcasts at 3 AM.

No accounts, no ads, no trackers, no network access.

## Features

- Simple countdown timer in minutes, with `+5 min` and cancel actions directly
  from the notification
- Stops audio and video playback across apps at the end of the timer
- Configurable fade-out duration before playback stops
- Optional screen lock when the timer ends (requires Device Admin permission)
- Haptic feedback for timer controls (can be disabled)
- Foreground service keeps the timer reliable even when the app is in the
  background
- English and German translations

## Screenshots

_Coming soon._

## Permissions

| Permission | Why it is needed |
|---|---|
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Keeps the countdown running reliably in the background so it fires on time |
| `POST_NOTIFICATIONS` | Shows the running timer with quick actions (+5 min, cancel) |
| `BIND_DEVICE_ADMIN` (optional, `force-lock` only) | Required by Android to lock the screen when the timer ends — only requested if you enable the "Lock screen" setting |

The app does not request the `INTERNET` permission. It cannot connect to any
network.

## Privacy

- No analytics, crash reporting, or telemetry
- No third-party SDKs (no Firebase, no Google Play Services, no ads)
- All settings are stored locally on your device using Jetpack DataStore
- No data leaves your device

## Installation

### F-Droid

_Submission pending._

### GitHub Releases

Signed APKs are published on the [Releases page](https://github.com/Xitee1/sleep-timer/releases).

## Building from source

Requirements:

- JDK 17
- Android SDK with platform 35 (`compileSdk`)
- `minSdk` is 26 (Android 8.0)

```sh
./gradlew assembleRelease
```

The build is fully reproducible from this repository using a 100% FLOSS
toolchain — no proprietary dependencies, no binary blobs.

### Project structure

- `app/` — Android application module, entry points, DI wiring
- `core/data/` — settings and timer state (DataStore + repository pattern)
- `core/service/` — foreground service managing the countdown and playback control
- `feature/timer/` — Jetpack Compose UI for the timer and settings screens

Written in Kotlin with Jetpack Compose, Hilt (DI) and kotlinx.serialization.

## Contributing

Issues and pull requests are welcome at
<https://github.com/Xitee1/sleep-timer>.

## License

SleepTimer is free software licensed under the
**GNU General Public License, version 3 or later**. See [LICENSE](LICENSE) for
the full text.

```
Copyright (C) 2026 Xitee

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
```
