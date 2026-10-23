package com.wecker.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wecker.app.data.SettingsRepository
import com.wecker.app.data.ThemeMode
import com.wecker.app.ui.AlarmEditScreen
import com.wecker.app.ui.AlarmListScreen
import com.wecker.app.ui.BarcodeLearnScreen
import com.wecker.app.ui.theme.WeckerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings = remember { SettingsRepository(applicationContext) }
            val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val colorScheme by settings.colorScheme.collectAsState(initial = 0)
            WeckerTheme(themeMode = themeMode, colorScheme = colorScheme) {
                NotificationPermissionRequest()
                WeckerNavHost()
            }
        }
    }
}

/** POST_NOTIFICATIONS ist ab Android 13 eine Runtime-Berechtigung. */
@Composable
private fun NotificationPermissionRequest() {
    if (Build.VERSION.SDK_INT < 33) return
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    LaunchedEffect(Unit) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
}

@Composable
private fun WeckerNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "list",
        // Sanfte Folien- und Blende-Übergänge zwischen den Screens.
        enterTransition = {
            slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(300))
        },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(300)) },
        popExitTransition = {
            slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(200))
        }
    ) {
        composable("list") {
            AlarmListScreen(onEditAlarm = { id -> navController.navigate("edit/$id") })
        }
        composable(
            route = "edit/{alarmId}",
            arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
        ) { entry ->
            val alarmId = entry.arguments?.getLong("alarmId") ?: 0L
            val learnedBarcode by entry.savedStateHandle
                .getStateFlow<String?>("barcode", null)
                .collectAsState()
            AlarmEditScreen(
                alarmId = alarmId,
                learnedBarcode = learnedBarcode,
                onBarcodeConsumed = { entry.savedStateHandle.remove<String>("barcode") },
                onLearnBarcode = { navController.navigate("barcodeLearn") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("barcodeLearn") {
            BarcodeLearnScreen(onScanned = { value ->
                navController.previousBackStackEntry?.savedStateHandle?.set("barcode", value)
                navController.popBackStack()
            })
        }
    }
}
