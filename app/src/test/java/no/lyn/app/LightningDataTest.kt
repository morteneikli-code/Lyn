package no.lyn.app

import org.junit.Assert.assertEquals
import org.junit.Test

class LightningDataTest {

    // ─── secondsToKm ─────────────────────────────────────────────────────────────

    @Test
    fun `secondsToKm returns 0 for 0 seconds`() {
        assertEquals(0.0, secondsToKm(0.0), 0.0001)
    }

    @Test
    fun `secondsToKm returns 1 km at the speed-of-sound constant`() {
        // 2.915 s ≈ 1 km, since sound travels ~343 m/s
        assertEquals(1.0, secondsToKm(2.915), 0.0001)
    }

    @Test
    fun `secondsToKm scales linearly`() {
        assertEquals(2.0, secondsToKm(5.83), 0.0001)
        assertEquals(0.5, secondsToKm(1.4575), 0.0001)
    }

    // ─── getSafetyInfo — boundary behaviour ──────────────────────────────────────
    // Thresholds use strict `<`, so the boundary value belongs to the NEXT (safer) tier.

    @Test
    fun `getSafetyInfo at 0 km is extreme danger`() {
        assertEquals(SafetyLevel.EXTREME_DANGER, getSafetyInfo(0.0).level)
    }

    @Test
    fun `getSafetyInfo just below 3 km is extreme danger`() {
        assertEquals(SafetyLevel.EXTREME_DANGER, getSafetyInfo(2.99).level)
    }

    @Test
    fun `getSafetyInfo at exactly 3 km is danger`() {
        assertEquals(SafetyLevel.DANGER, getSafetyInfo(3.0).level)
    }

    @Test
    fun `getSafetyInfo just below 6 km is danger`() {
        assertEquals(SafetyLevel.DANGER, getSafetyInfo(5.99).level)
    }

    @Test
    fun `getSafetyInfo at exactly 6 km is caution`() {
        assertEquals(SafetyLevel.CAUTION, getSafetyInfo(6.0).level)
    }

    @Test
    fun `getSafetyInfo just below 10 km is caution`() {
        assertEquals(SafetyLevel.CAUTION, getSafetyInfo(9.99).level)
    }

    @Test
    fun `getSafetyInfo at exactly 10 km is low risk`() {
        assertEquals(SafetyLevel.LOW_RISK, getSafetyInfo(10.0).level)
    }

    @Test
    fun `getSafetyInfo far away is low risk`() {
        assertEquals(SafetyLevel.LOW_RISK, getSafetyInfo(100.0).level)
    }

    // ─── getStormTrend ───────────────────────────────────────────────────────────

    // Trend tests below assume the distance-aware noise floor:
    //   distance < 6 km → 1.0 km noise floor (lightning location is imprecise close-up)
    //   distance ≥ 6 km → 0.3 km noise floor

    @Test
    fun `getStormTrend with empty list is unknown`() {
        assertEquals(StormTrend.UNKNOWN, getStormTrend(emptyList()))
    }

    @Test
    fun `getStormTrend with one measurement is unknown`() {
        assertEquals(StormTrend.UNKNOWN, getStormTrend(listOf(5.0)))
    }

    @Test
    fun `getStormTrend approaching when distances shrink beyond threshold`() {
        // last - first across the window of 3 is -2.0, well below -0.3
        assertEquals(StormTrend.APPROACHING, getStormTrend(listOf(5.0, 4.0, 3.0)))
    }

    @Test
    fun `getStormTrend retreating when distances grow beyond threshold`() {
        assertEquals(StormTrend.RETREATING, getStormTrend(listOf(3.0, 4.0, 5.0)))
    }

    @Test
    fun `getStormTrend stable when change stays within plus or minus threshold`() {
        // -0.3 < change < +0.3 → STABLE (noise floor)
        assertEquals(StormTrend.STABLE, getStormTrend(listOf(5.0, 5.1, 5.0)))
        assertEquals(StormTrend.STABLE, getStormTrend(listOf(5.0, 4.9, 5.2)))
    }

    @Test
    fun `getStormTrend close-range minor change stays stable under raised noise floor`() {
        // At ~5 km we use the 1.0 km noise floor. A delta of -0.3 km is well inside that.
        // (Before the close-range adjustment, this would have been APPROACHING — the new
        // behaviour reflects that small swings close to the storm are usually measurement
        // noise, not a real approach.)
        assertEquals(StormTrend.STABLE, getStormTrend(listOf(5.0, 4.85, 4.7)))
    }

    // ─── getStormTrend — distance-aware noise floor ─────────────────────────────

    @Test
    fun `getStormTrend close-range needs a large delta to count as approaching`() {
        // 2.5 → 2.2 → 2.0: delta -0.5, well inside the 1.0 km noise floor → STABLE
        assertEquals(StormTrend.STABLE, getStormTrend(listOf(2.5, 2.2, 2.0)))
    }

    @Test
    fun `getStormTrend close-range honest approach beats the noise floor`() {
        // 5.5 → 3.5 → 1.5: delta -4.0, way past 1.0 km → APPROACHING
        assertEquals(StormTrend.APPROACHING, getStormTrend(listOf(5.5, 3.5, 1.5)))
    }

    @Test
    fun `getStormTrend close-range minor retreat is suppressed as stable`() {
        // 1.5 → 1.7 → 1.9: delta +0.4, inside 1.0 km noise → STABLE (was RETREATING under old logic)
        assertEquals(StormTrend.STABLE, getStormTrend(listOf(1.5, 1.7, 1.9)))
    }

    @Test
    fun `getStormTrend far-range remains sensitive to small changes`() {
        // 12 → 11.5 → 11.0: delta -1.0 with 0.3 noise floor → APPROACHING
        assertEquals(StormTrend.APPROACHING, getStormTrend(listOf(12.0, 11.5, 11.0)))
        // 11 → 11.5 → 12.0: delta +1.0 → RETREATING
        assertEquals(StormTrend.RETREATING, getStormTrend(listOf(11.0, 11.5, 12.0)))
    }

    @Test
    fun `getStormTrend close-far boundary at 6 km uses the far noise floor`() {
        // last = 6.0, NOT < 6 → far noise floor (0.3 km).
        // 6.5 → 6.2 → 6.0: delta -0.5 past 0.3 → APPROACHING
        assertEquals(StormTrend.APPROACHING, getStormTrend(listOf(6.5, 6.2, 6.0)))
    }

    @Test
    fun `getStormTrend just below 6 km uses the close noise floor`() {
        // last = 5.99 → close noise floor (1.0). Delta -0.5 is now just noise → STABLE.
        assertEquals(StormTrend.STABLE, getStormTrend(listOf(6.5, 6.2, 5.99)))
    }

    // ─── displayTrend ────────────────────────────────────────────────────────────
    // Decides what label the UI shows. RETREATING/STABLE collapse to STILL_CLOSE
    // when the storm is still in the danger zone — never give a green/yellow signal
    // when the safety card is still red.

    @Test
    fun `displayTrend passes APPROACHING through regardless of distance`() {
        // Warnings of worsening conditions are always honest — never suppressed.
        assertEquals(TrendDisplay.APPROACHING, displayTrend(StormTrend.APPROACHING, 1.0))
        assertEquals(TrendDisplay.APPROACHING, displayTrend(StormTrend.APPROACHING, 15.0))
    }

    @Test
    fun `displayTrend overrides RETREATING to STILL_CLOSE when in danger zone`() {
        assertEquals(TrendDisplay.STILL_CLOSE, displayTrend(StormTrend.RETREATING, 2.0))
        assertEquals(TrendDisplay.STILL_CLOSE, displayTrend(StormTrend.RETREATING, 5.99))
    }

    @Test
    fun `displayTrend overrides STABLE to STILL_CLOSE when in danger zone`() {
        assertEquals(TrendDisplay.STILL_CLOSE, displayTrend(StormTrend.STABLE, 1.5))
    }

    @Test
    fun `displayTrend shows RETREATING normally when out of danger zone`() {
        // Boundary uses strict `<`, so 6.0 is "far enough".
        assertEquals(TrendDisplay.RETREATING, displayTrend(StormTrend.RETREATING, 6.0))
        assertEquals(TrendDisplay.RETREATING, displayTrend(StormTrend.RETREATING, 15.0))
    }

    @Test
    fun `displayTrend shows STABLE normally when out of danger zone`() {
        assertEquals(TrendDisplay.STABLE, displayTrend(StormTrend.STABLE, 8.0))
    }

    @Test
    fun `displayTrend passes UNKNOWN through`() {
        // Even when no real data exists, distance shouldn't change classification.
        assertEquals(TrendDisplay.UNKNOWN, displayTrend(StormTrend.UNKNOWN, 0.0))
        assertEquals(TrendDisplay.UNKNOWN, displayTrend(StormTrend.UNKNOWN, 20.0))
    }

    @Test
    fun `getStormTrend uses only the last three measurements`() {
        // First two values (100, 50) would suggest retreating if included.
        // The window-of-3 logic should look only at 5.0 → 4.0 → 3.0, which is approaching.
        assertEquals(
            StormTrend.APPROACHING,
            getStormTrend(listOf(100.0, 50.0, 5.0, 4.0, 3.0)),
        )
    }

    @Test
    fun `getStormTrend with exactly two measurements still works`() {
        // takeLast(3) on a 2-element list returns the 2 elements.
        assertEquals(StormTrend.APPROACHING, getStormTrend(listOf(5.0, 3.0)))
        assertEquals(StormTrend.RETREATING, getStormTrend(listOf(3.0, 5.0)))
        assertEquals(StormTrend.STABLE, getStormTrend(listOf(5.0, 5.1)))
    }

    // ─── factForMeasurementCount ─────────────────────────────────────────────────

    @Test
    fun `factForMeasurementCount returns first fact for zero measurements`() {
        assertEquals(LIGHTNING_FACTS[0], factForMeasurementCount(0))
    }

    @Test
    fun `factForMeasurementCount advances with each measurement`() {
        assertEquals(LIGHTNING_FACTS[1], factForMeasurementCount(1))
        assertEquals(LIGHTNING_FACTS[2], factForMeasurementCount(2))
        assertEquals(LIGHTNING_FACTS[3], factForMeasurementCount(3))
    }

    @Test
    fun `factForMeasurementCount wraps around past the end of the list`() {
        val size = LIGHTNING_FACTS.size
        // After a full cycle, we're back to fact[0]; then fact[1], etc.
        assertEquals(LIGHTNING_FACTS[0], factForMeasurementCount(size))
        assertEquals(LIGHTNING_FACTS[1], factForMeasurementCount(size + 1))
        assertEquals(LIGHTNING_FACTS[0], factForMeasurementCount(size * 7))
    }

    @Test
    fun `factForMeasurementCount handles unexpected negative input without crashing`() {
        // Defensive: count comes from sessionDistances.size which should never be negative,
        // but Kotlin's `mod` always returns a non-negative result, so this stays safe.
        val result = factForMeasurementCount(-1)
        assertEquals(LIGHTNING_FACTS[LIGHTNING_FACTS.size - 1], result)
    }
}
