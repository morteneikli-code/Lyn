package no.lyn.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import no.lyn.app.R
import no.lyn.app.data.LightningStrike
import no.lyn.app.ui.theme.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapScreen(vm: MapViewModel = viewModel()) {
    val context = LocalContext.current
    val strikes by vm.strikes.collectAsState()
    val isConnected by vm.isConnected.collectAsState()

    // ---- Location permission ----
    var hasLocation by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocation = granted }

    // ---- Build OSMDroid MapView once ----
    val strikeOverlay = remember { LightningStrikeOverlay() }
    // Keep a ref so the FAB can re-center at any time
    val locationOverlay = remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(4.5)
            controller.setCenter(GeoPoint(55.0, 15.0)) // fallback until GPS fix
            overlays.add(strikeOverlay)
            clipToOutline = true
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocation) {
            permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(strikes) {
        strikeOverlay.setStrikes(strikes)
        mapView.invalidate()
        // Feed user position into VM so it can check for nearby strikes
        locationOverlay.value?.myLocation?.let { loc ->
            vm.updateUserLocation(loc.latitude, loc.longitude)
            vm.checkNearbyStrikes(context, strikes)
        }
    }

    LaunchedEffect(hasLocation) {
        if (hasLocation) {
            val locOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
            locOverlay.enableMyLocation()
            // runOnFirstFix callback runs on a background thread
            locOverlay.runOnFirstFix {
                mapView.post {
                    mapView.controller.animateTo(locOverlay.myLocation)
                    mapView.controller.setZoom(8.0)
                }
            }
            mapView.overlays.removeAll { it is MyLocationNewOverlay }
            mapView.overlays.add(locOverlay)
            locationOverlay.value = locOverlay
            mapView.invalidate()
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE  -> mapView.onPause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // The map
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
        )

        // Top status bar
        MapStatusBar(
            strikes = strikes,
            isConnected = isConnected,
            onReconnect = { vm.reconnect() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
        )

        // Bottom legend + location button
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StrikeLegend()
            FloatingActionButton(
                onClick = {
                    if (hasLocation) {
                        val loc = locationOverlay.value?.myLocation
                        if (loc != null) {
                            mapView.controller.animateTo(loc)
                            mapView.controller.setZoom(8.0)
                        }
                    } else {
                        permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                containerColor = StormCard,
                contentColor = LightningYellow,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "My location", modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun MapStatusBar(
    strikes: List<LightningStrike>,
    isConnected: Boolean,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recent = remember(strikes) {
        val cutoff = System.currentTimeMillis() - 10 * 60_000L
        strikes.count { it.timeMs > cutoff }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StormCard.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, if (isConnected) LightningYellow.copy(alpha = 0.4f) else DangerRed.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ColorDot(color = if (isConnected) SafeGreen else DangerRed)
            Icon(Icons.Filled.ElectricBolt, null, tint = LightningYellow, modifier = Modifier.size(16.dp))
            Text(
                text = if (isConnected) stringResource(R.string.map_status_connected, recent, strikes.size)
                       else stringResource(R.string.map_status_disconnected),
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
            )
            AnimatedVisibility(visible = !isConnected) {
                IconButton(onClick = onReconnect, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.SignalWifiOff, "Reconnect", tint = DangerOrange, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StrikeLegend() {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = StormCard.copy(alpha = 0.90f)),
        border = BorderStroke(1.dp, StormBorder),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LegendRow(Color(0xFFFFD60A), "< 5 min")
            LegendRow(Color(0xFFFF8C00), "5 – 15 min")
            LegendRow(Color(0xFFFF4444), "15 – 30 min")
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ColorDot(color = color)
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = TextSecondary, fontSize = 11.sp)
    }
}
