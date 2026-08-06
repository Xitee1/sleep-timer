package dev.xitee.sleeptimer.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.xitee.sleeptimer.core.data.model.TimerPhase
import dev.xitee.sleeptimer.core.data.model.startMinutesFor
import dev.xitee.sleeptimer.core.data.repository.SettingsRepository
import dev.xitee.sleeptimer.core.data.repository.TimerRepository
import dev.xitee.sleeptimer.core.data.repository.WidgetConfigRepository
import dev.xitee.sleeptimer.core.data.util.remainingMillisToDisplayMinutes
import dev.xitee.sleeptimer.core.service.SleepTimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Home-screen widget that starts the sleep timer with a single tap. Instances are
 * configured individually (see [SleepTimerWidgetConfigActivity]): each starts
 * either the last used time or its own fixed duration.
 *
 * The idle tap does not carry the duration in its PendingIntent — it broadcasts
 * [ACTION_START_TIMER] (with the tapped instance's appWidgetId) back to this
 * receiver, which resolves the duration from DataStore at tap time. Baking the
 * duration into the widget's PendingIntent would go stale whenever the settings
 * change without the widget being re-rendered.
 *
 * Starting the foreground service from here is allowed even though the app is in
 * the background: a widget tap is a documented exemption from the FGS
 * background-start restrictions (the same mechanism notification actions rely on).
 */
@AndroidEntryPoint
class SleepTimerWidgetProvider : AppWidgetProvider() {

    @Inject lateinit var timerRepository: TimerRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var widgetConfigRepository: WidgetConfigRepository

    override fun onReceive(context: Context, intent: Intent) {
        // Hilt injects the fields inside super.onReceive, which also dispatches
        // APPWIDGET_UPDATE to onUpdate — so injection must happen first.
        super.onReceive(context, intent)
        if (intent.action == ACTION_START_TIMER) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
            startConfiguredTimer(context, appWidgetId)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // After process death timerState is IDLE by construction (in-process flow,
        // and the foreground service keeps the process alive while a timer runs).
        val state = timerRepository.timerState.value
        if (state.phase != TimerPhase.IDLE) {
            // Remaining time is held in-process — render synchronously, no DataStore.
            val minutes = remainingMillisToDisplayMinutes(state.remainingMillis)
            appWidgetIds.forEach { id ->
                SleepTimerWidgetRenderer.render(context, appWidgetManager, id, state.phase, minutes)
            }
            return
        }
        // IDLE: resolving each instance's configured start minutes needs DataStore, so
        // do it off the main thread — goAsync keeps the process alive until finish().
        val pending = goAsync()
        asyncScope.launch {
            try {
                val presetMinutes = settingsRepository.settings.first().presetMinutes
                val configs = widgetConfigRepository.configs.first()
                // Re-read the phase after the async DataStore gap: a timer may have
                // started meanwhile. Render whatever the phase is *now* so a stale IDLE
                // frame can't clobber the live updater's fresher running render.
                val current = timerRepository.timerState.value
                appWidgetIds.forEach { id ->
                    val minutes = if (current.phase == TimerPhase.IDLE) {
                        configs.startMinutesFor(id, presetMinutes)
                    } else {
                        remainingMillisToDisplayMinutes(current.remainingMillis)
                    }
                    SleepTimerWidgetRenderer.render(context, appWidgetManager, id, current.phase, minutes)
                }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        // A backup/device-transfer restore reassigns appWidgetIds; move each instance's
        // config from its old id to the new one so restored widgets keep their
        // fixed-duration setting and no orphan entries are left behind. goAsync keeps
        // the process alive for the write.
        val remap = oldWidgetIds.zip(newWidgetIds).toMap()
        if (remap.isEmpty()) return
        val pending = goAsync()
        asyncScope.launch {
            try {
                widgetConfigRepository.remapConfigs(remap)
            } finally {
                pending.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // Removed instances must not leak their config entries in DataStore. goAsync
        // keeps the process alive for the write without blocking the main thread on it.
        val pending = goAsync()
        asyncScope.launch {
            try {
                widgetConfigRepository.removeConfigs(appWidgetIds.toList())
            } finally {
                pending.finish()
            }
        }
    }

    private fun startConfiguredTimer(context: Context, appWidgetId: Int) {
        // The rendered tap target can be momentarily stale (timer started from the
        // app right before the tap landed) — never restart a timer that is active.
        if (timerRepository.timerState.value.phase != TimerPhase.IDLE) return
        // A rapid second tap would also pass the IDLE check above while the first
        // start is still resolving its duration off-thread (phase not yet RUNNING).
        // Gate so only one start is in flight; reset once it has been dispatched.
        if (!starting.compareAndSet(false, true)) return
        // Resolve the duration off the main thread. goAsync keeps the broadcast (and
        // with it the widget-tap FGS background-start exemption) alive until the
        // service is started a few ms later.
        val pending = goAsync()
        asyncScope.launch {
            try {
                val config = widgetConfigRepository.getConfig(appWidgetId)
                // A fixed-duration widget ignores the preset, so skip that read entirely.
                val startMinutes = if (config.useFixedDuration) {
                    config.fixedMinutes
                } else {
                    config.startMinutes(settingsRepository.settings.first().presetMinutes)
                }
                // start() owns the FGS dispatch and swallows the denied-start case
                // (background-restricted app), so the tap degrades to a logged
                // no-op instead of crashing.
                SleepTimerService.start(context, startMinutes * 60_000L)
            } finally {
                starting.set(false)
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_START_TIMER = "dev.xitee.sleeptimer.action.WIDGET_START_TIMER"

        // Process-lifetime scope for the short DataStore reads/writes the receiver
        // callbacks hand off via goAsync(); SupervisorJob so one failure can't cancel it.
        private val asyncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        // Guards against a double-tap issuing two concurrent ACTION_START intents while
        // the first tap is still resolving its duration off-thread. Static because a
        // BroadcastReceiver instance is transient (one per delivered broadcast).
        private val starting = AtomicBoolean(false)
    }
}
