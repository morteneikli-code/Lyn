package no.lyn.app

import no.lyn.app.data.Measurement
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Pure helpers for grouping measurements by calendar day on the History screen.
 *
 * Kept Android-free so they can be unit-tested without an emulator.
 * Time-zone is explicit — never rely on the JVM default in shared logic.
 */

enum class DayCategory { TODAY, YESTERDAY, OTHER }

/** Stable yyyy-MM-dd key for grouping. Uses given zone so tests are deterministic. */
fun dayKey(timestampMillis: Long, zone: TimeZone = TimeZone.getDefault()): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }
    return fmt.format(Date(timestampMillis))
}

/**
 * Classify a timestamp relative to "now" in the given zone.
 * TODAY = same calendar day as now. YESTERDAY = the calendar day before. OTHER = anything else.
 */
fun categorizeDay(
    timestampMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault(),
): DayCategory {
    val today = Calendar.getInstance(zone).apply { timeInMillis = nowMillis }
    val that = Calendar.getInstance(zone).apply { timeInMillis = timestampMillis }
    if (today.sameDayAs(that)) return DayCategory.TODAY
    today.add(Calendar.DAY_OF_YEAR, -1)
    if (today.sameDayAs(that)) return DayCategory.YESTERDAY
    return DayCategory.OTHER
}

private fun Calendar.sameDayAs(other: Calendar): Boolean =
    get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
        get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)

/**
 * Group measurements by calendar day, preserving input order.
 * Input is expected newest-first (matches the DAO query order); output groups follow the same order.
 */
fun groupMeasurementsByDay(
    measurements: List<Measurement>,
    zone: TimeZone = TimeZone.getDefault(),
): List<Pair<String, List<Measurement>>> {
    val grouped = LinkedHashMap<String, MutableList<Measurement>>()
    for (m in measurements) {
        val key = dayKey(m.timestamp, zone)
        grouped.getOrPut(key) { mutableListOf() }.add(m)
    }
    return grouped.map { it.key to it.value }
}
