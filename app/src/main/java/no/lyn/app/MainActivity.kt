package no.lyn.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
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

sealed class Screen(val route: String, val labelRes: Int, val icon: ImageVector) {
    object Timer   : Screen("timer",   R.string.nav_timer,   Icons.Filled.ElectricBolt)
    object History : Screen("history", R.string.nav_history, Icons.Filled.History)
}

val screens = listOf(Screen.Timer, Screen.History)

/** Threshold for firing a "still close" notification (VERY_CLOSE and below — 2 km). */
private const val ALERT_DISTANCE_THRESHOLD_KM = VERY_CLOSE_THRESHOLD_KM

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)
        enableEdgeToEdge()
        setContent {
            LynTheme {
                // Disclaimer is a one-time gate. We read the initial state from prefs,
                // then track acceptance in memory so the next composition flips smoothly
                // without a full activity recreate.
                val context = LocalContext.current
                var accepted by remember {
                    mutableStateOf(DisclaimerPrefs.isAccepted(context))
                }
                if (!accepted) {
                    DisclaimerScreen(onAccept = {
                        DisclaimerPrefs.accept(context)
                        accepted = true
                    })
                } else {
                    LynApp(database = (application as LynApplication).database)
                }
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
    val context = LocalContext.current

    // Request notification permission on Android 13+
    // Launcher must be registered unconditionally — Compose rules forbid conditional remember calls.
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result ignored — app works fine without it */ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Timer state hoisted here so it survives tab navigation
    var flashTime by rememberSaveable { mutableStateOf<Long?>(null) }
    var elapsedSeconds by rememberSaveable { mutableStateOf<Double?>(null) }
    // Session distances reset when the app restarts; kept across tab switches
    var sessionDistances by remember { mutableStateOf(listOf<Double>()) }

    Scaffold(
        containerColor = StormBlack,
        bottomBar = {
            NavigationBar(containerColor = StormDeep, tonalElevation = 0.dp) {
                screens.forEach { screen ->
                    val selected = currentDestination
                        ?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(stringResource(screen.labelRes)) },
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
                    flashTime = flashTime,
                    elapsedSeconds = elapsedSeconds,
                    sessionDistances = sessionDistances,
                    onTap = {
                        val now = System.currentTimeMillis()
                        if (flashTime == null) {
                            flashTime = now
                            elapsedSeconds = null
                        } else {
                            val secs = (now - flashTime!!) / 1000.0
                            elapsedSeconds = secs
                            flashTime = null
                            scope.launch {
                                val dist = secondsToKm(secs)
                                sessionDistances = sessionDistances + dist
                                database.measurementDao().insert(
                                    Measurement(
                                        timestamp = now,
                                        seconds = secs,
                                        distanceKm = dist,
                                        safetyLevel = getSafetyInfo(dist).level,
                                    )
                                )
                                LynWidget.onMeasurementSaved(context, dist)
                                if (dist < ALERT_DISTANCE_THRESHOLD_KM) {
                                    NotificationHelper.notifyNearbyStrike(context, dist)
                                }
                            }
                        }
                    },
                    onReset = {
                        flashTime = null
                        elapsedSeconds = null
                        sessionDistances = emptyList()
                    },
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(database = database)
            }
        }
    }
}
