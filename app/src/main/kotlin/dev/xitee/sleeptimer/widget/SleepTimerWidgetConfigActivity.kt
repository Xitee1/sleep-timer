package dev.xitee.sleeptimer.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.xitee.sleeptimer.feature.timer.widgetconfig.WidgetConfigScreen
import dev.xitee.sleeptimer.theme.SleepTimerTheme

/**
 * Launched by the widget host when a widget is placed (and via long-press → edit
 * on Android 12+, see `reconfigurable` in the provider info). Thin Compose host:
 * the actual screen and persistence live in :feature:timer / :core:data.
 */
@AndroidEntryPoint
class SleepTimerWidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Canceled until the user explicitly saves: backing out of a first-time
        // placement makes the host discard the half-configured widget.
        setResult(RESULT_CANCELED, resultIntent(appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // This activity is exported (some launchers start it directly), so reject a
        // launch for an id that isn't one of our own placed widgets — otherwise
        // another app could invoke it with a foreign/bogus appWidgetId and persist an
        // orphan config that onDeleted would never reclaim. The host always binds the
        // id to us before launching config, so a legitimate launch passes this check.
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
        if (info?.provider != ComponentName(this, SleepTimerWidgetProvider::class.java)) {
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            SleepTimerTheme {
                WidgetConfigScreen(
                    onSaved = {
                        setResult(RESULT_OK, resultIntent(appWidgetId))
                        finish()
                    },
                    onCancel = { finish() },
                )
            }
        }
    }

    private fun resultIntent(appWidgetId: Int): Intent =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}
