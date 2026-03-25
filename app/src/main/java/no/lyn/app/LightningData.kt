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

data class LightningFact(val title: String, val body: String, val icon: String)

val LIGHTNING_FACTS = listOf(
    LightningFact(
        "100 Strikes Per Second",
        "Earth is hit by lightning roughly 100 times every second — that's about 8 million strikes per day worldwide.",
        "⚡",
    ),
    LightningFact(
        "Hotter Than the Sun",
        "A single lightning bolt can heat the surrounding air to 30,000 °C — five times hotter than the surface of the sun.",
        "🌡️",
    ),
    LightningFact(
        "Razor-Thin but Kilometres Long",
        "A lightning channel is only 2–3 cm wide, yet can stretch 3–5 km or more through the sky.",
        "📏",
    ),
    LightningFact(
        "The 30/30 Rule",
        "If thunder follows a flash in under 30 seconds, seek shelter. Wait 30 minutes after the last clap before going back outside.",
        "⏱️",
    ),
    LightningFact(
        "Cars Are Surprisingly Safe",
        "A hard-topped car is a good shelter — not because of the rubber tyres, but because the metal body conducts electricity around the passengers.",
        "🚗",
    ),
    LightningFact(
        "Same Place Twice? Absolutely",
        "The Empire State Building is struck by lightning about 20–25 times per year. Tall structures get hit repeatedly.",
        "🗼",
    ),
    LightningFact(
        "Lightning Creates Ozone",
        "The massive energy of a lightning bolt splits nitrogen and oxygen molecules, which then recombine to form ozone (O₃).",
        "🌿",
    ),
    LightningFact(
        "\"Heat Lightning\" Is a Myth",
        "There is no such thing as heat lightning. What you see is regular lightning from a storm too far away to hear its thunder.",
        "🌅",
    ),
    LightningFact(
        "Upward Lightning Exists",
        "Lightning doesn't only travel downward. Upward lightning, where the bolt travels from the ground up to a cloud, is common from tall structures.",
        "⬆️",
    ),
    LightningFact(
        "Rubber Shoes Don't Help",
        "Rubber-soled shoes provide essentially zero protection against lightning. The billions of volts involved make a few mm of rubber irrelevant.",
        "👟",
    ),
)
