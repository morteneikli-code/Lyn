package no.lyn.app

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import no.lyn.app.ui.theme.*

data class SafetyInfo(
    val level: SafetyLevel,
    @StringRes val titleRes: Int,
    @StringRes val adviceRes: Int,
    val color: Color,
    val emoji: String,
)

/**
 * Five tiers grounded in real strike-physics and authoritative guidance.
 *
 * The split between OVERHEAD and VERY_CLOSE is the only place we change indoor advice:
 * close lightning makes plumbing and corded landline phones the realistic indoor hazards.
 * Regular grounded electronics are not meaningfully more dangerous to unplug at 1 km than
 * at 5 km — the "don't unplug during a storm" myth is conservative public messaging, not
 * physics. So we only flag the genuinely risky items, and only when proximity warrants it.
 */
enum class SafetyLevel { OVERHEAD, VERY_CLOSE, CLOSE, NEAR, DISTANT }

// Tier thresholds in km — single source of truth.
const val OVERHEAD_THRESHOLD_KM: Double = 1.0
const val VERY_CLOSE_THRESHOLD_KM: Double = 2.0
const val CLOSE_THRESHOLD_KM: Double = 6.0
const val NEAR_THRESHOLD_KM: Double = 10.0

enum class StormTrend { APPROACHING, RETREATING, STABLE, UNKNOWN }

/**
 * What the UI should display for the trend card.
 *
 * STILL_CLOSE overrides RETREATING/STABLE when the storm is still in the danger zone —
 * a small distance change at 2 km is within measurement noise, and reading "retreating"
 * as "safe" can be deadly. APPROACHING is never overridden: a warning that things are
 * getting worse is always honest.
 */
enum class TrendDisplay { APPROACHING, RETREATING, STABLE, STILL_CLOSE, UNKNOWN }

/** Below this distance, the storm is considered "close" — noise floor rises, trend gets overridden. */
const val TREND_CLOSE_THRESHOLD_KM: Double = 6.0
/** Noise floor when storm is far: small distance changes are still meaningful. */
const val TREND_NOISE_FAR_KM: Double = 0.3
/** Noise floor when storm is close: Blitzortung accuracy + sound-speed variance demand a larger delta. */
const val TREND_NOISE_CLOSE_KM: Double = 1.0

/**
 * Derives trend from a list of distance-km values collected in one session.
 * Needs at least 2 measurements; uses the window of the last 3.
 *
 * The noise floor scales with distance: 0.3 km when far (>6 km), 1.0 km when close.
 * Lightning location is inherently imprecise — at 2 km, a 0.3 km swing is well within
 * Blitzortung's typical error, so calling it "retreating" would be over-claiming.
 */
fun getStormTrend(distancesKm: List<Double>): StormTrend {
    if (distancesKm.size < 2) return StormTrend.UNKNOWN
    val window = distancesKm.takeLast(3)
    val delta = window.last() - window.first()
    val noiseFloor = if (window.last() < TREND_CLOSE_THRESHOLD_KM) TREND_NOISE_CLOSE_KM else TREND_NOISE_FAR_KM
    return when {
        delta < -noiseFloor -> StormTrend.APPROACHING
        delta >  noiseFloor -> StormTrend.RETREATING
        else                -> StormTrend.STABLE
    }
}

/**
 * Resolves the trend card label given raw trend and the latest measured distance.
 * Pure decision logic so the UI doesn't decide safety messaging itself.
 *
 * RETREATING/STABLE collapse to STILL_CLOSE when last distance is in the danger zone:
 * we never want a green/yellow signal to contradict a red safety card.
 */
fun displayTrend(trend: StormTrend, lastDistanceKm: Double): TrendDisplay = when (trend) {
    StormTrend.UNKNOWN     -> TrendDisplay.UNKNOWN
    StormTrend.APPROACHING -> TrendDisplay.APPROACHING
    StormTrend.RETREATING,
    StormTrend.STABLE      ->
        if (lastDistanceKm < TREND_CLOSE_THRESHOLD_KM) TrendDisplay.STILL_CLOSE
        else if (trend == StormTrend.RETREATING) TrendDisplay.RETREATING
        else TrendDisplay.STABLE
}

fun getSafetyInfo(distanceKm: Double): SafetyInfo = when {
    distanceKm < OVERHEAD_THRESHOLD_KM -> SafetyInfo(
        level = SafetyLevel.OVERHEAD,
        titleRes = R.string.safety_overhead_title,
        adviceRes = R.string.safety_overhead_advice,
        color = CrimsonRed,
        emoji = "🔴",
    )
    distanceKm < VERY_CLOSE_THRESHOLD_KM -> SafetyInfo(
        level = SafetyLevel.VERY_CLOSE,
        titleRes = R.string.safety_very_close_title,
        adviceRes = R.string.safety_very_close_advice,
        color = DangerRed,
        emoji = "🔴",
    )
    distanceKm < CLOSE_THRESHOLD_KM -> SafetyInfo(
        level = SafetyLevel.CLOSE,
        titleRes = R.string.safety_close_title,
        adviceRes = R.string.safety_close_advice,
        color = DangerOrange,
        emoji = "🟠",
    )
    distanceKm < NEAR_THRESHOLD_KM -> SafetyInfo(
        level = SafetyLevel.NEAR,
        titleRes = R.string.safety_near_title,
        adviceRes = R.string.safety_near_advice,
        color = CautionYellow,
        emoji = "🟡",
    )
    else -> SafetyInfo(
        level = SafetyLevel.DISTANT,
        titleRes = R.string.safety_distant_title,
        adviceRes = R.string.safety_distant_advice,
        color = SafeGreen,
        emoji = "🟢",
    )
}

/** Speed of sound ≈ 343 m/s → 1 km per ~2.915 s */
fun secondsToKm(seconds: Double): Double = seconds / 2.915

data class LightningFact(val titleRes: Int, val bodyRes: Int, val icon: String)

/**
 * Picks the fact to display given how many measurements have been made this session.
 * Cycles through LIGHTNING_FACTS so a new fact appears after each measurement,
 * and wraps back to the first fact once the list is exhausted.
 *
 * Returns null if the fact list is empty (defensive — currently never the case).
 */
fun factForMeasurementCount(count: Int): LightningFact? {
    if (LIGHTNING_FACTS.isEmpty()) return null
    val normalized = count.mod(LIGHTNING_FACTS.size) // mod is always non-negative in Kotlin
    return LIGHTNING_FACTS[normalized]
}

val LIGHTNING_FACTS = listOf(
    LightningFact(R.string.fact_strikes_per_second_title, R.string.fact_strikes_per_second_body, "⚡"),
    LightningFact(R.string.fact_hotter_than_sun_title,    R.string.fact_hotter_than_sun_body,    "🌡️"),
    LightningFact(R.string.fact_thin_but_long_title,      R.string.fact_thin_but_long_body,      "📏"),
    LightningFact(R.string.fact_thirty_thirty_title,      R.string.fact_thirty_thirty_body,      "⏱️"),
    LightningFact(R.string.fact_cars_safe_title,          R.string.fact_cars_safe_body,          "🚗"),
    LightningFact(R.string.fact_same_place_title,         R.string.fact_same_place_body,         "🗼"),
    LightningFact(R.string.fact_ozone_title,              R.string.fact_ozone_body,              "🌿"),
    LightningFact(R.string.fact_heat_lightning_title,     R.string.fact_heat_lightning_body,     "🌅"),
    LightningFact(R.string.fact_upward_title,             R.string.fact_upward_body,             "⬆️"),
    LightningFact(R.string.fact_rubber_shoes_title,       R.string.fact_rubber_shoes_body,       "👟"),
)
