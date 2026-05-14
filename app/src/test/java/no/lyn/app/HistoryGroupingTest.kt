package no.lyn.app

import no.lyn.app.data.Measurement
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class HistoryGroupingTest {

    // Use Oslo so tests behave like the target users; fixed zone keeps results deterministic.
    private val oslo: TimeZone = TimeZone.getTimeZone("Europe/Oslo")

    /** Parse an ISO-like local time in Oslo to epoch millis. */
    private fun osloTime(iso: String): Long {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { timeZone = oslo }
        return fmt.parse(iso)!!.time
    }

    private fun m(id: Long, iso: String): Measurement = Measurement(
        id = id,
        timestamp = osloTime(iso),
        seconds = 1.0,
        distanceKm = 1.0,
        safetyLevel = SafetyLevel.LOW_RISK,
    )

    // ─── dayKey ──────────────────────────────────────────────────────────────────

    @Test
    fun `dayKey is stable for two timestamps on the same Oslo day`() {
        val morning = osloTime("2026-05-14 06:00:00")
        val evening = osloTime("2026-05-14 23:30:00")
        assertEquals(dayKey(morning, oslo), dayKey(evening, oslo))
        assertEquals("2026-05-14", dayKey(morning, oslo))
    }

    @Test
    fun `dayKey rolls over at local midnight`() {
        val justBefore = osloTime("2026-05-14 23:59:59")
        val justAfter  = osloTime("2026-05-15 00:00:01")
        assertEquals("2026-05-14", dayKey(justBefore, oslo))
        assertEquals("2026-05-15", dayKey(justAfter, oslo))
    }

    @Test
    fun `dayKey can differ between zones for the same instant`() {
        // 23:30 in Oslo on May 14 is still the 14th there, but already May 14 evening
        // becomes May 15 early morning in Tokyo (Oslo + 7-8h depending on DST).
        val instant = osloTime("2026-05-14 23:30:00")
        val tokyo = TimeZone.getTimeZone("Asia/Tokyo")
        assertEquals("2026-05-14", dayKey(instant, oslo))
        assertEquals("2026-05-15", dayKey(instant, tokyo))
    }

    // ─── categorizeDay ──────────────────────────────────────────────────────────

    @Test
    fun `categorizeDay returns TODAY for same calendar day`() {
        val now = osloTime("2026-05-14 14:00:00")
        val sameDay = osloTime("2026-05-14 06:00:00")
        assertEquals(DayCategory.TODAY, categorizeDay(sameDay, now, oslo))
    }

    @Test
    fun `categorizeDay returns YESTERDAY for the previous calendar day`() {
        val now = osloTime("2026-05-14 14:00:00")
        val yesterday = osloTime("2026-05-13 22:00:00")
        assertEquals(DayCategory.YESTERDAY, categorizeDay(yesterday, now, oslo))
    }

    @Test
    fun `categorizeDay returns OTHER for two days ago`() {
        val now = osloTime("2026-05-14 14:00:00")
        val twoDaysAgo = osloTime("2026-05-12 14:00:00")
        assertEquals(DayCategory.OTHER, categorizeDay(twoDaysAgo, now, oslo))
    }

    @Test
    fun `categorizeDay treats just-before-midnight today as TODAY`() {
        val now = osloTime("2026-05-14 23:59:59")
        val earlierToday = osloTime("2026-05-14 00:00:01")
        assertEquals(DayCategory.TODAY, categorizeDay(earlierToday, now, oslo))
    }

    @Test
    fun `categorizeDay handles month boundary as YESTERDAY`() {
        // Yesterday-detection must not break across month rollover.
        val now = osloTime("2026-06-01 10:00:00")
        val yesterday = osloTime("2026-05-31 23:00:00")
        assertEquals(DayCategory.YESTERDAY, categorizeDay(yesterday, now, oslo))
    }

    @Test
    fun `categorizeDay handles year boundary as YESTERDAY`() {
        val now = osloTime("2027-01-01 02:00:00")
        val yesterday = osloTime("2026-12-31 23:00:00")
        assertEquals(DayCategory.YESTERDAY, categorizeDay(yesterday, now, oslo))
    }

    // ─── groupMeasurementsByDay ─────────────────────────────────────────────────

    @Test
    fun `groupMeasurementsByDay returns empty list for empty input`() {
        assertEquals(emptyList<Pair<String, List<Measurement>>>(), groupMeasurementsByDay(emptyList(), oslo))
    }

    @Test
    fun `groupMeasurementsByDay keeps measurements on the same day together`() {
        val a = m(1, "2026-05-14 22:00:00")
        val b = m(2, "2026-05-14 14:00:00")
        val c = m(3, "2026-05-14 09:00:00")
        val groups = groupMeasurementsByDay(listOf(a, b, c), oslo)
        assertEquals(1, groups.size)
        assertEquals("2026-05-14", groups[0].first)
        assertEquals(listOf(a, b, c), groups[0].second)
    }

    @Test
    fun `groupMeasurementsByDay preserves newest-first order across days`() {
        // DAO returns newest first; output order should mirror that for the UI.
        val today1 = m(1, "2026-05-14 22:00:00")
        val today2 = m(2, "2026-05-14 09:00:00")
        val yest = m(3, "2026-05-13 18:00:00")
        val older = m(4, "2026-05-10 12:00:00")

        val groups = groupMeasurementsByDay(listOf(today1, today2, yest, older), oslo)

        assertEquals(listOf("2026-05-14", "2026-05-13", "2026-05-10"), groups.map { it.first })
        assertEquals(listOf(today1, today2), groups[0].second)
        assertEquals(listOf(yest), groups[1].second)
        assertEquals(listOf(older), groups[2].second)
    }

    @Test
    fun `groupMeasurementsByDay splits across local midnight`() {
        val beforeMidnight = m(1, "2026-05-14 23:59:00")
        val afterMidnight  = m(2, "2026-05-15 00:01:00")
        // Input order: newest first (afterMidnight), then beforeMidnight.
        val groups = groupMeasurementsByDay(listOf(afterMidnight, beforeMidnight), oslo)
        assertEquals(2, groups.size)
        assertEquals("2026-05-15", groups[0].first)
        assertEquals("2026-05-14", groups[1].first)
    }
}
