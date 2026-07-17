package dev.xitee.sleeptimer.core.data.model

/**
 * Per-instance configuration of a home-screen widget, keyed by its appWidgetId in
 * [dev.xitee.sleeptimer.core.data.repository.WidgetConfigRepository]. Instances
 * without a stored entry use the defaults, so a widget is fully functional even
 * if its configuration step was skipped (Android 12+ `configuration_optional`).
 */
/**
 * Clamps a widget's fixed-duration value to the range the dial and the service accept.
 * Single source for the bound so the repository's read, write, and map-rebuild paths
 * can't drift out of sync with each other.
 */
fun clampFixedWidgetMinutes(minutes: Int): Int = minutes.coerceIn(1, MAX_TIMER_MINUTES)

data class WidgetConfig(
    val useFixedDuration: Boolean = false,
    val fixedMinutes: Int = 30,
) {
    /**
     * Minutes a tap on this widget starts the timer with: the widget's fixed
     * duration when configured, otherwise the last used time ([presetMinutes] is
     * updated on every committed dial value).
     */
    fun startMinutes(presetMinutes: Int): Int =
        if (useFixedDuration) fixedMinutes else presetMinutes
}

/**
 * Start minutes for the widget instance [appWidgetId], resolved from this map of
 * stored configs (falling back to [WidgetConfig] defaults for an instance without an
 * entry). Single source of truth for the "an idle widget shows exactly what a tap
 * will start" invariant — the provider's render and the live updater both resolve
 * through here, and the tap trampoline through [WidgetConfig.startMinutes] directly.
 */
fun Map<Int, WidgetConfig>.startMinutesFor(appWidgetId: Int, presetMinutes: Int): Int =
    (this[appWidgetId] ?: WidgetConfig()).startMinutes(presetMinutes)
