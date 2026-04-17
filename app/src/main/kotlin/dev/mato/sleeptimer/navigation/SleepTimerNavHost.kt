package dev.mato.sleeptimer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.mato.sleeptimer.feature.timer.settings.SettingsScreen
import dev.mato.sleeptimer.feature.timer.timer.TimerScreen

@Composable
fun SleepTimerNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = TimerRoute,
    ) {
        composable<TimerRoute> {
            TimerScreen(
                onNavigateToSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
