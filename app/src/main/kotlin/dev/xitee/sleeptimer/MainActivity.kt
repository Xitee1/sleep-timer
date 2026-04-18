package dev.xitee.sleeptimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.xitee.sleeptimer.navigation.SleepTimerNavHost
import dev.xitee.sleeptimer.theme.SleepTimerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SleepTimerTheme {
                SleepTimerNavHost()
            }
        }
    }
}
