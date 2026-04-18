package dev.xitee.sleeptimer.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import dev.xitee.sleeptimer.feature.timer.about.AboutScreen
import dev.xitee.sleeptimer.feature.timer.settings.SettingsScreen
import dev.xitee.sleeptimer.feature.timer.settings.ThemePickerScreen
import dev.xitee.sleeptimer.feature.timer.timer.AppOrientationController
import dev.xitee.sleeptimer.feature.timer.timer.DeviceOrientation
import dev.xitee.sleeptimer.feature.timer.timer.TimerScreen
import dev.xitee.sleeptimer.feature.timer.timer.rememberDeviceOrientation

@Composable
fun SleepTimerNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isTimer = backStackEntry?.destination?.hasRoute<TimerRoute>() ?: true

    // Skip the cross-screen fade when the device is tilted: the orientation
    // flip that accompanies Timer ↔ Settings navigation combined with a fade
    // briefly shows Timer's counter-rotated content being rotated by the
    // window manager, appearing upside-down. In natural portrait there is no
    // orientation flip, so the fade is kept.
    val deviceOrientation by rememberDeviceOrientation()
    val animate = deviceOrientation == DeviceOrientation.PORTRAIT

    AppOrientationController(orientation = deviceOrientation, lockPortrait = isTimer)
    NavHost(
        navController = navController,
        startDestination = TimerRoute,
        enterTransition = { if (animate) fadeIn() else EnterTransition.None },
        exitTransition = { if (animate) fadeOut() else ExitTransition.None },
        popEnterTransition = { if (animate) fadeIn() else EnterTransition.None },
        popExitTransition = { if (animate) fadeOut() else ExitTransition.None },
    ) {
        composable<TimerRoute> {
            TimerScreen(
                onNavigateToSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToThemePicker = { navController.navigate(ThemePickerRoute) },
                onNavigateToAbout = { navController.navigate(AboutRoute) },
            )
        }
        composable<ThemePickerRoute> {
            ThemePickerScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable<AboutRoute> {
            AboutScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
