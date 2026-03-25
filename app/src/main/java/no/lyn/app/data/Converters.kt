package no.lyn.app.data

import androidx.room.TypeConverter
import no.lyn.app.SafetyLevel

class Converters {
    @TypeConverter fun fromSafetyLevel(level: SafetyLevel): String = level.name
    @TypeConverter fun toSafetyLevel(name: String): SafetyLevel = SafetyLevel.valueOf(name)
}
