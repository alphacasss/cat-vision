package id.cv.catvision.ui

/** User preferences persisted in SharedPreferences. */
data class AppSettings(
    val autoStartCamera: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val saveDetections: Boolean = true,
    val detectionThreshold: Float = 0.35f,
)
