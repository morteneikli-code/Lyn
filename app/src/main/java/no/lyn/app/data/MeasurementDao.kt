package no.lyn.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<Measurement>>

    @Insert
    suspend fun insert(measurement: Measurement)

    @Delete
    suspend fun delete(measurement: Measurement)

    @Query("DELETE FROM measurements")
    suspend fun deleteAll()
}
