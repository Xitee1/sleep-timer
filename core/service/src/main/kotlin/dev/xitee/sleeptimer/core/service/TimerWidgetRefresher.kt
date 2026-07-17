package dev.xitee.sleeptimer.core.service

/**
 * Pushes a fresh render to the home-screen widgets after the timer reaches a terminal
 * state. Implemented in :app (which owns the AppWidget provider) and injected here so
 * the service can trigger the final idle draw itself.
 *
 * Why the service and not only the in-process observer: the app's `TimerWidgetUpdater`
 * collector renders on a background dispatcher, so the very last idle frame can be lost
 * if the process is reclaimed right after [SleepTimerService] calls `stopSelf`. Firing
 * a system-mediated refresh from the service instead keeps the process alive long
 * enough for that draw to land. A no-op when no widgets are placed.
 */
fun interface TimerWidgetRefresher {
    fun refresh()
}
