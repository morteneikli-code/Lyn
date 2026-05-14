package no.lyn.app.data

import androidx.room.TypeConverter
import no.lyn.app.SafetyLevel

class Converters {
    @TypeConverter fun fromSafetyLevel(level: SafetyLevel): String = level.name

    /**
     * Tolerant decoder: maps any stored string to a current SafetyLevel.
     * Older builds wrote tier names (EXTREME_DANGER, DANGER, CAUTION, LOW_RISK) that no longer
     * exist. We map them to the nearest current tier so historical rows still load.
     */
    @TypeConverter fun toSafetyLevel(name: String): SafetyLevel = when (name) {
        "EXTREME_DANGER" -> SafetyLevel.VERY_CLOSE
        "DANGER"         -> SafetyLevel.CLOSE
        "CAUTION"        -> SafetyLevel.NEAR
        "LOW_RISK"       -> SafetyLevel.DISTANT
        else -> runCatching { SafetyLevel.valueOf(name) }.getOrDefault(SafetyLevel.DISTANT)
    }
}
