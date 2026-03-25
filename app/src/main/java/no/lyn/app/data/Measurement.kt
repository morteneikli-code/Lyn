package no.lyn.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import no.lyn.app.SafetyLevel

@Entity(tableName = "measurements")
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val seconds: Double,
    val distanceKm: Double,
    val safetyLevel: SafetyLevel,
)
