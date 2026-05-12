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
    fun `getStormTrend boundary at exactly -0_3 is stable`() {
        // Threshold uses strict `<`, so a delta of exactly -0.3 is NOT approaching.
        // Pin this so a future "<= -0.3" refactor surfaces here.
        assertEquals(StormTrend.STABLE, getStormTrend(listOf(5.0, 4.85, 4.7)))
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
}
