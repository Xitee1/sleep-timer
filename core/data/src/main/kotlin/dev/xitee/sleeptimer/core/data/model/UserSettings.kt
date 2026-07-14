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
    val autoRotateMode: AutoRotateMode = AutoRotateMode.Default,
    val stepMinutes: Int = 5,
    val presetMinutes: Int = 15,
    val launchAnimationEnabled: Boolean = true,
    val widgetUseFixedDuration: Boolean = false,
    val widgetFixedMinutes: Int = 30,
) {
    /**
     * Minutes a home-screen-widget tap starts the timer with: the fixed widget
     * duration when configured, otherwise the last used time ([presetMinutes] is
     * updated on every committed dial value).
     */
    fun widgetStartMinutes(): Int =
        if (widgetUseFixedDuration) widgetFixedMinutes else presetMinutes
}
