package no.lyn.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import no.lyn.app.data.AppDatabase
import no.lyn.app.data.Measurement
import no.lyn.app.ui.*
import no.lyn.app.ui.theme.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Timer   : Screen("timer",   "Timer",   Icons.Filled.ElectricBolt)
    object History : Screen("history", "History", Icons.Filled.History)
    object Map     : Screen("map",     "Live Map",Icons.Filled.Map)
}

val screens = listOf(Screen.Timer, Screen.History, Screen.Map)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LynTheme {
                LynApp(database = (application as LynApplication).database)
            }
        }
    }
}

@Composable
fun LynApp(database: AppDatabase) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination
    val scope = rememberCoroutineScope()

    // Timer state hoisted here so it survives tab navigation
    var flashTime by rememberSaveable { mutableStateOf<Long?>(null) }
    var elapsedSeconds by rememberSaveable { mutableStateOf<Double?>(null) }

    Scaffold(
        containerColor = StormBlack,
        bottomBar = {
            NavigationBar(containerColor = StormDeep, tonalElevation = 0.dp) {
                screens.forEach { screen ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == screen.route } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LightningYellow,
                            selectedTextColor = LightningYellow,
                            indicatorColor = StormCard,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Timer.route,
            modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding())
                .background(StormBlack),
        ) {
            composable(Screen.Timer.route) {
                TimerScreen(
                    database = database,
                    flashTime = flashTime,
                    elapsedSeconds = elapsedSeconds,
                    onTap = {
                        val now = System.currentTimeMillis()
                        if (flashTime == null) {
                            flashTime = now
                            elapsedSeconds = null
                        } else {
                            val secs = (now - flashTime!!) / 1000.0
                            elapsedSeconds = secs
                            flashTime = null
                            // Persist to history
                            scope.launch {
                                val dist = secondsToKm(secs)
                                database.measurementDao().insert(
                                    Measurement(
                                        timestamp = now,
                                        seconds = secs,
                                        distanceKm = dist,
                                        safetyLevel = getSafetyInfo(dist).level.name,
                                    )
                                )
                            }
                        }
                    },
                    onReset = {
                        flashTime = null
                        elapsedSeconds = null
                    },
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(database = database)
            }
            composable(Screen.Map.route) {
                MapScreen()
            }
        }
    }
}
