package id.cv.catvision.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionDao {
    @Insert
    suspend fun insert(entry: DetectionEntry)

    @Query("SELECT * FROM detections ORDER BY timestamp DESC")
    fun getAll(): Flow<List<DetectionEntry>>

    @Query("DELETE FROM detections")
    suspend fun clearAll()
}
