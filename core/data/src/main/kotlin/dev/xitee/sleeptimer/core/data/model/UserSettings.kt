package dev.xitee.sleeptimer.core.data.model

/** Upper bound of the "let the song finish" slider; shared by the settings UI and the repository. */
const val TRACK_END_MAX_WAIT_MINUTES = 15

data class UserSettings(
    val stopMediaPlayback: Boolean = true,
    val fadeOutDurationSeconds: Int = 30,
    val trackEndMethod: TrackEndMethod = TrackEndMethod.Default,
    // Longest rest of a song the timer waits for before fading out; 0 = off.
    val trackEndMaxWaitMinutes: Int = 5,
    val screenOff: Boolean = false,
    val screenLockMethod: ScreenLockMethod = ScreenLockMethod.Default,
    val turnOffWifi: Boolean = false,
    val turnOffMobileData: Boolean = false,
    val turnOffBluetooth: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val theme: ThemeId = ThemeId.Default,
    val starsEnabled: Boolean = true,
    val autoRotateMode: AutoRotateMode = AutoRotateMode.Default,
    val stepMinutes: Int = 5,
    val presetMinutes: Int = 15,
    val launchAnimationEnabled: Boolean = true,
)
