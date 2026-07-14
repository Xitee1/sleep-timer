package dev.xitee.sleeptimer.core.service.power

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Queries and requests the battery-optimization exemption. Doze/App-Standby can
 * defer or kill the foreground service on aggressive OEM builds, so the timer may
 * silently stop mid-countdown; the exemption is optional because stock Android
 * usually keeps a foreground service alive without it.
 */
@Singleton
class BatteryOptimizationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val powerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun isIgnoringBatteryOptimizations(): Boolean =
        powerManager.isIgnoringBatteryOptimizations(context.packageName)

    /**
     * System dialog asking to exempt this app. Play policy discourages this action
     * (the BatteryLife lint check), but the app is distributed via F-Droid/GitHub
     * and the request is only ever triggered from an opt-in settings row.
     */
    @SuppressLint("BatteryLife")
    fun requestExemptionIntent(): Intent = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        "package:${context.packageName}".toUri(),
    )

    /**
     * The system's battery-optimization list. The exemption can't be revoked
     * programmatically, so turning the setting off sends the user here.
     */
    fun batteryOptimizationSettingsIntent(): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
}
