package dev.mato.sleeptimer.core.data.model

data class UserSettings(
    val stopMediaPlayback: Boolean = true,
    val fadeOutDurationSeconds: Int = 30,
    val screenOff: Boolean = false,
    val notificationEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val theme: ThemeId = ThemeId.Default,
    val starsEnabled: Boolean = true,
)
