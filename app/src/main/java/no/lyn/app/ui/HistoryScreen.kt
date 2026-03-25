package no.lyn.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import no.lyn.app.data.AppDatabase
import no.lyn.app.data.Measurement
import no.lyn.app.getSafetyInfo
import no.lyn.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(database: AppDatabase) {
    val dao = database.measurementDao()
    val measurements by dao.getAllFlow().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StormBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "History",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            if (measurements.isNotEmpty()) {
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(Icons.Filled.DeleteOutline, "Clear all", tint = TextSecondary)
                }
            }
        }

        if (measurements.isEmpty()) {
            EmptyHistoryState()
        } else {
            // Summary row
            SummaryRow(measurements)
            Spacer(Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(measurements, key = { it.id }) { measurement ->
                    MeasurementItem(
                        measurement = measurement,
                        onDelete = { scope.launch { dao.delete(measurement) } },
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = StormCard,
            title = { Text("Clear all history?", color = TextPrimary) },
            text = { Text("This cannot be undone.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.deleteAll() }
                    showClearDialog = false
                }) { Text("Clear", color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
        )
    }
}

@Composable
private fun SummaryRow(measurements: List<Measurement>) {
    val avgDist = measurements.map { it.distanceKm }.average()
    val closest = measurements.minOf { it.distanceKm }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StormCard),
        border = BorderStroke(1.dp, StormBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SummaryStat("${measurements.size}", "Measurements")
            VerticalDivider(modifier = Modifier.height(40.dp), color = StormBorder)
            SummaryStat("%.1f km".format(avgDist), "Avg distance")
            VerticalDivider(modifier = Modifier.height(40.dp), color = StormBorder)
            SummaryStat("%.1f km".format(closest), "Closest")
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = LightningYellow, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
    }
}

@Composable
private fun MeasurementItem(measurement: Measurement, onDelete: () -> Unit) {
    val safetyColor = getSafetyInfo(measurement.distanceKm).color

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StormCard),
        border = BorderStroke(1.dp, safetyColor.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColorDot(color = safetyColor, size = 10.dp)

            // Data
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "%.1f s".format(measurement.seconds),
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricBlue,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(Icons.Filled.ElectricBolt, null, tint = LightningYellow, modifier = Modifier.size(14.dp))
                    Text(
                        text = "%.1f km".format(measurement.distanceKm),
                        style = MaterialTheme.typography.titleMedium,
                        color = LightningYellow,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = formatRelativeTime(measurement.timestamp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }

            // Delete
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.DeleteOutline, "Delete", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.HistoryToggleOff, null, tint = StormBorder, modifier = Modifier.size(64.dp))
            Text("No measurements yet", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Text(
                "Tap the lightning button on the\nTimer tab to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = StormBorder,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000        -> "Just now"
        diff < 3_600_000     -> "${diff / 60_000} min ago"
        diff < 86_400_000    -> "${diff / 3_600_000}h ago"
        diff < 7 * 86_400_000L -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
