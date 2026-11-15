package id.cv.catvision.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detections")
data class DetectionEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val label: String,
    val confidence: Float,
    val photoPath: String? = null,
    val width: Int = 0,
    val height: Int = 0,
)
