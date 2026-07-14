package dev.xitee.sleeptimer.core.data.model

/**
 * Per-instance configuration of a home-screen widget, keyed by its appWidgetId in
 * [dev.xitee.sleeptimer.core.data.repository.WidgetConfigRepository]. Instances
 * without a stored entry use the defaults, so a widget is fully functional even
 * if its configuration step was skipped (Android 12+ `configuration_optional`).
 */
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
