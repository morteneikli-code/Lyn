package no.lyn.app.ui

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import no.lyn.app.DayCategory
import no.lyn.app.R
import no.lyn.app.categorizeDay
import no.lyn.app.data.AppDatabase
import no.lyn.app.data.Measurement
import no.lyn.app.getSafetyInfo
import no.lyn.app.groupMeasurementsByDay
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
                text = stringResource(R.string.nav_history),
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

            val context = LocalContext.current
            // Measurements are already sorted newest-first by DAO; group preserves that order.
            val groups = remember(measurements) { groupMeasurementsByDay(measurements) }
            val latestDayKey = groups.firstOrNull()?.first
            // Track which day-keys are expanded. Latest day starts expanded by default.
            val expanded = rememberSaveable(
                saver = androidx.compose.runtime.saveable.listSaver(
                    save = { it.toList() },
                    restore = { it.toMutableStateList() },
                ),
            ) { mutableStateListOf<String>().also { if (latestDayKey != null) it.add(latestDayKey) } }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                groups.forEach { (dayKey, dayMeasurements) ->
                    val isExpanded = dayKey in expanded
                    item(key = "header_$dayKey") {
                        DayHeader(
                            label = formatDayLabel(dayMeasurements.first().timestamp, context),
                            count = dayMeasurements.size,
                            expanded = isExpanded,
                            onToggle = {
                                if (isExpanded) expanded.remove(dayKey) else expanded.add(dayKey)
                            },
                        )
                    }
                    if (isExpanded) {
                        items(dayMeasurements, key = { it.id }) { measurement ->
                            MeasurementItem(
                                measurement = measurement,
                                onDelete = { scope.launch { dao.delete(measurement) } },
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = StormCard,
            title = { Text(stringResource(R.string.history_clear_title), color = TextPrimary) },
            text = { Text(stringResource(R.string.history_clear_body), color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.deleteAll() }
                    showClearDialog = false
                }) { Text(stringResource(R.string.history_clear), color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.history_cancel), color = TextSecondary) }
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
            SummaryStat("${measurements.size}", stringResource(R.string.history_count))
            VerticalDivider(modifier = Modifier.height(40.dp), color = StormBorder)
            SummaryStat("%.1f km".format(avgDist), stringResource(R.string.history_avg))
            VerticalDivider(modifier = Modifier.height(40.dp), color = StormBorder)
            SummaryStat("%.1f km".format(closest), stringResource(R.string.history_closest))
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
    val context = LocalContext.current

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
                    text = formatRelativeTime(measurement.timestamp, context),
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
            Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Text(
                stringResource(R.string.history_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = StormBorder,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DayHeader(label: String, count: Int, expanded: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = LightningYellow,
            modifier = Modifier
                .size(20.dp)
                .rotate(rotation),
        )
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            pluralStringResource(R.plurals.day_measurement_count, count, count),
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
        )
    }
}

private fun formatDayLabel(timestamp: Long, context: Context): String =
    when (categorizeDay(timestamp)) {
        DayCategory.TODAY     -> context.getString(R.string.day_today)
        DayCategory.YESTERDAY -> context.getString(R.string.day_yesterday)
        DayCategory.OTHER     -> SimpleDateFormat("EEEE d. MMMM", Locale.getDefault())
            .format(Date(timestamp))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

private fun formatRelativeTime(timestamp: Long, context: Context): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000          -> context.getString(R.string.time_just_now)
        diff < 3_600_000       -> context.getString(R.string.time_minutes_ago, diff / 60_000)
        diff < 86_400_000      -> context.getString(R.string.time_hours_ago, diff / 3_600_000)
        diff < 7 * 86_400_000L -> context.getString(R.string.time_days_ago, diff / 86_400_000)
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
