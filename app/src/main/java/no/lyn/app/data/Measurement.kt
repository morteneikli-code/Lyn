package no.lyn.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,    // Unix ms when measurement was saved
    val seconds: Double,    // Time between flash and thunder
    val distanceKm: Double, // Calculated distance
    val safetyLevel: String, // SafetyLevel.name
)
