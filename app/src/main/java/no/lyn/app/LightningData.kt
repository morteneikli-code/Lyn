package no.lyn.app

import androidx.compose.ui.graphics.Color
import no.lyn.app.ui.theme.*

data class SafetyInfo(
    val level: SafetyLevel,
    val title: String,
    val advice: String,
    val color: Color,
    val emoji: String,
)

enum class SafetyLevel { EXTREME_DANGER, DANGER, CAUTION, LOW_RISK }

enum class StormTrend { APPROACHING, RETREATING, STABLE, UNKNOWN }

/**
 * Derives trend from a list of distance-km values collected in one session.
 * Needs at least 2 measurements; uses the window of the last 3.
 * Threshold 0.3 km avoids noise from measurement imprecision.
 */
fun getStormTrend(distancesKm: List<Double>): StormTrend {
    if (distancesKm.size < 2) return StormTrend.UNKNOWN
    val window = distancesKm.takeLast(3)
    return when {
        window.last() - window.first() < -0.3 -> StormTrend.APPROACHING
        window.last() - window.first() >  0.3 -> StormTrend.RETREATING
        else                                   -> StormTrend.STABLE
    }
}

fun getSafetyInfo(distanceKm: Double): SafetyInfo = when {
    distanceKm < 3.0 -> SafetyInfo(
        level = SafetyLevel.EXTREME_DANGER,
        title = "Extreme Danger",
        advice = "Seek solid shelter IMMEDIATELY. Do NOT stand under trees, near water, or in open areas. Get inside a building or a hard-topped vehicle now.",
        color = DangerRed,
        emoji = "🔴",
    )
    distanceKm < 6.0 -> SafetyInfo(
        level = SafetyLevel.DANGER,
        title = "Danger — Seek Shelter",
        advice = "The storm is very close. Move inside immediately. Avoid contact with plumbing, electrical equipment, and windows.",
        color = DangerOrange,
        emoji = "🟠",
    )
    distanceKm < 10.0 -> SafetyInfo(
        level = SafetyLevel.CAUTION,
        title = "Caution — Be Prepared",
        advice = "Lightning can still reach you. Apply the 30/30 rule: if thunder follows lightning in under 30 s, seek shelter. Wait 30 min after the last strike.",
        color = CautionYellow,
        emoji = "🟡",
    )
    else -> SafetyInfo(
        level = SafetyLevel.LOW_RISK,
        title = "Low Risk — Stay Alert",
        advice = "The storm is relatively far away, but conditions can change quickly. Monitor the situation and be ready to seek shelter.",
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
