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
