package dev.xitee.sleeptimer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.xitee.sleeptimer.widget.TimerWidgetUpdater
import javax.inject.Inject

@HiltAndroidApp
class SleepTimerApp : Application() {

    @Inject lateinit var timerWidgetUpdater: TimerWidgetUpdater

    override fun onCreate() {
        super.onCreate()
        // Whoever wakes the process (activity, timer service, widget tap) also
        // brings the home-screen widgets in sync from here on.
        timerWidgetUpdater.start()
    }
}
