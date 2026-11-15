package id.cv.catvision.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import id.cv.catvision.breed.BreedEstimator
import id.cv.catvision.breed.BreedMatch
import id.cv.catvision.detector.CatDetector
import id.cv.catvision.model.CatVisionDatabase
import id.cv.catvision.model.DetectionEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class HistoryItem(
    val id: Long,
    val timestamp: Long,
    val label: String,
    val confidence: Float,
    val photoPath: String? = null,
)

sealed class UiStatus {
    data object Idle : UiStatus()
    data object Running : UiStatus()
    data object Paused : UiStatus()
    data class CatDetected(
        val label: String,
        val confidence: Float,
        val topClasses: List<Pair<String, Float>>,
        val breedMatches: List<BreedMatch> = emptyList(),
        val features: BreedEstimator.Features? = null,
        val modelLabel: String? = null,
    ) : UiStatus()
    data class NotACat(
        val label: String,
        val confidence: Float,
    ) : UiStatus()
    data class Error(val msg: String) : UiStatus()
}

data class CameraOptions(
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val isTorchOn: Boolean = false,
    val hasFlash: Boolean = false,
    val zoomRatio: Float = 1f,
    val maxZoom: Float = 1f,
    val isFocusing: Boolean = false,
    val focusPoint: Pair<Float, Float>? = null,
)

/** Result of analysing a picture picked from the gallery. */
data class ImportedResult(
    val imagePath: String,
    val label: String?,
    val confidence: Float,
    val topClasses: List<Pair<String, Float>>,
    val isCat: Boolean = true,
    val modelLabel: String? = null,
    val breedMatches: List<BreedMatch> = emptyList(),
    val features: BreedEstimator.Features? = null,
)

data class UiState(
    val status: UiStatus = UiStatus.Idle,
    val isCameraReady: Boolean = false,
    val isCameraStarting: Boolean = false,
    val isModelReady: Boolean = false,
    val history: List<HistoryItem> = emptyList(),
    val isRunning: Boolean = false,
    val lastPhotoPath: String? = null,
    val showHistory: Boolean = false,
    val showPhoto: Boolean = false,
    val showSettings: Boolean = false,
    val showBreeds: Boolean = false,
    val detections: Int = 0,
    val isAnalyzingImage: Boolean = false,
    val importedResult: ImportedResult? = null,
    val importError: String? = null,
)

class CatVisionViewModel(
    private val context: Context,
    private val database: CatVisionDatabase
) : ViewModel() {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _cameraOptions = MutableStateFlow(CameraOptions())
    val cameraOptions: StateFlow<CameraOptions> = _cameraOptions.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val detector = CatDetector(context)
    private val dao = database.detectionDao()
    private val cameraExecutor: Executor = Executors.newSingleThreadExecutor()
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var previewView: PreviewView? = null
    private var isBound = false
    private var isBinding = false
    private var pendingStart = false

    private val isProcessing = AtomicBoolean(false)
    private var lastFrameAt = 0L
    private var lastInsertAt = 0L
    private var lastDetectedAt = 0L
    private var focusJob: Job? = null

    /**
     * Status "kucing / bukan kucing" yang sudah stabil. Perubahan hanya
     * diterapkan setelah kandidat baru bertahan beberapa saat, supaya kartu
     * hasil tidak berkedip saat model ragu satu-dua frame.
     */
    private var stableIsCat: Boolean? = null
    private var pendingIsCat: Boolean? = null
    private var pendingVerdictSince = 0L

    /** Nama objek non-kucing yang sudah stabil, plus kandidat yang sedang diuji. */
    private var stableObjectLabel: String? = null
    private var pendingObjectLabel: String? = null
    private var pendingObjectSince = 0L

    private val frameBuffer: ByteBuffer = ByteBuffer.allocate(TARGET_SIZE * TARGET_SIZE * 3)

    private val zoomObserver = Observer<ZoomState> { state ->
        _cameraOptions.update {
            it.copy(zoomRatio = state.zoomRatio, maxZoom = state.maxZoomRatio)
        }
    }

    private val torchObserver = Observer<Int> { state ->
        _cameraOptions.update { it.copy(isTorchOn = state == TorchState.ON) }
    }

    init {
        detector.loadModel { ok ->
            _uiState.update { it.copy(isModelReady = ok) }
            if (!ok) {
                _uiState.update { it.copy(status = UiStatus.Error("Gagal memuat model AI")) }
            }
        }
        loadHistory()
    }

    private fun loadSettings(): AppSettings = AppSettings(
        autoStartCamera = prefs.getBoolean(KEY_AUTO_START, false),
        hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, true),
        saveDetections = prefs.getBoolean(KEY_SAVE_DETECTIONS, true),
        detectionThreshold = prefs.getFloat(KEY_THRESHOLD, 0.35f),
    )

    fun setAutoStartCamera(enabled: Boolean) {
        if (_settings.value.autoStartCamera == enabled) return
        prefs.edit().putBoolean(KEY_AUTO_START, enabled).apply()
        _settings.update { it.copy(autoStartCamera = enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        if (_settings.value.hapticsEnabled == enabled) return
        prefs.edit().putBoolean(KEY_HAPTICS, enabled).apply()
        _settings.update { it.copy(hapticsEnabled = enabled) }
    }

    fun setSaveDetections(enabled: Boolean) {
        if (_settings.value.saveDetections == enabled) return
        prefs.edit().putBoolean(KEY_SAVE_DETECTIONS, enabled).apply()
        _settings.update { it.copy(saveDetections = enabled) }
    }

    fun setDetectionThreshold(value: Float) {
        val clamped = value.coerceIn(0.20f, 0.80f)
        prefs.edit().putFloat(KEY_THRESHOLD, clamped).apply()
        _settings.update { it.copy(detectionThreshold = clamped) }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            dao.getAll().collect { entries ->
                _uiState.update { state ->
                    state.copy(history = entries.take(HISTORY_LIMIT).map { e ->
                        HistoryItem(e.id, e.timestamp, e.label, e.confidence, e.photoPath)
                    })
                }
            }
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    /** Call once the PreviewView is available. */
    fun setPreviewView(pv: PreviewView?) {
        previewView = pv
        // Manual mode by default: only auto-start when the user enabled it.
        if (pv != null && _settings.value.autoStartCamera && hasCameraPermission()) {
            startCamera()
        }
    }

    fun startCamera() {
        if (isBound) return
        if (isBinding) {
            pendingStart = true
            return
        }

        val lifecycleOwner = context as? LifecycleOwner
        if (lifecycleOwner == null) {
            showError("Aktivitas tidak mendukung Lifecycle")
            return
        }
        val pv = previewView ?: return

        if (!hasCameraPermission()) {
            showError("Izin kamera belum diberikan")
            return
        }

        isBinding = true
        _uiState.update {
            it.copy(
                isCameraStarting = true,
                status = if (it.status is UiStatus.Error) UiStatus.Idle else it.status
            )
        }

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                cameraProvider = provider
                provider.unbindAll()
                detachCameraObservers()

                val selector = CameraSelector.Builder()
                    .requireLensFacing(_cameraOptions.value.lensFacing)
                    .build()

                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(pv.surfaceProvider) }

                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 640))
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    analysis,
                    capture
                )

                imageAnalysis = analysis
                imageCapture = capture
                isBound = true
                isBinding = false
                isProcessing.set(false)
                detector.resetSmoothing()
                resetVerdictTracking()
                attachCameraObservers(lifecycleOwner)
                _uiState.update {
                    it.copy(
                        status = UiStatus.Running,
                        isCameraReady = true,
                        isCameraStarting = false,
                        isRunning = true
                    )
                }
                runAnalysisLoop(analysis)
                if (pendingStart) {
                    pendingStart = false
                    startCamera()
                }
            } catch (e: Exception) {
                isBinding = false
                isProcessing.set(false)
                Log.e("CatVisionViewModel", "Camera bind failed", e)
                _uiState.update { it.copy(isCameraStarting = false, isCameraReady = false) }
                showError(e.message ?: "Gagal membuka kamera")
                if (pendingStart) {
                    pendingStart = false
                    startCamera()
                }
            }
        }, mainExecutor)
    }

    private fun attachCameraObservers(lifecycleOwner: LifecycleOwner) {
        val info = camera?.cameraInfo ?: return
        info.zoomState.observe(lifecycleOwner, zoomObserver)
        info.torchState.observe(lifecycleOwner, torchObserver)
        _cameraOptions.update {
            it.copy(
                hasFlash = info.hasFlashUnit(),
                maxZoom = info.zoomState.value?.maxZoomRatio ?: 1f
            )
        }
    }

    private fun detachCameraObservers() {
        val info = camera?.cameraInfo ?: return
        info.zoomState.removeObserver(zoomObserver)
        info.torchState.removeObserver(torchObserver)
    }

    private fun runAnalysisLoop(analysis: ImageAnalysis) {
        analysis.setAnalyzer(cameraExecutor) { image ->
            val now = SystemClock.elapsedRealtime()
            if (!isProcessing.compareAndSet(false, true)) {
                image.close()
                return@setAnalyzer
            }
            if (now - lastFrameAt < FRAME_INTERVAL_MS) {
                isProcessing.set(false)
                image.close()
                return@setAnalyzer
            }
            try {
                lastFrameAt = now
                convertYUV420ToRgb888(image, frameBuffer)
                val features = BreedEstimator.analyze(frameBuffer, TARGET_SIZE, TARGET_SIZE)
                detector.detectSceneFrame(frameBuffer) { result ->
                    handleDetection(result, features)
                    isProcessing.set(false)
                }
            } catch (e: Exception) {
                Log.w("CatVisionViewModel", "Skipping malformed frame", e)
                isProcessing.set(false)
            } finally {
                image.close()
            }
        }
    }

    private fun handleDetection(
        result: CatDetector.DetectionResult?,
        features: BreedEstimator.Features
    ) {
        val now = System.currentTimeMillis()
        if (result == null) {
            if (now - lastDetectedAt < DETECTION_GRACE_MS) return
            resetVerdictTracking()
            _uiState.update { state ->
                if (state.status is UiStatus.CatDetected || state.status is UiStatus.NotACat) {
                    state.copy(status = UiStatus.Running)
                } else {
                    state
                }
            }
            return
        }

        lastDetectedAt = now

        if (!result.isCat) {
            // Hysteresis: kartu "kucing" hanya boleh berubah jika objek lain
            // cukup yakin terdeteksi, bukan karena satu frame yang meleset.
            if (stableIsCat == true && result.confidence < NONCAT_SWITCH_MIN) return
            if (!commitVerdict(false, now)) return
            if (!commitObjectLabel(result.label, now)) return
            val displayLabel = stableObjectLabel ?: result.label
            val displayConfidence = result.topClasses
                .firstOrNull { it.first == displayLabel }?.second
                ?: (_uiState.value.status as? UiStatus.NotACat)?.confidence
                ?: result.confidence
            _uiState.update {
                it.copy(
                    status = UiStatus.NotACat(displayLabel, displayConfidence),
                    isCameraReady = true,
                    isRunning = true
                )
            }
            return
        }

        // Sebaliknya, jangan langsung pindah ke "kucing" kalau bukti kucing
        // masih lemah (mis. pola di layar yang mirip bulu kucing).
        if (stableIsCat == false && result.catScore < CAT_SWITCH_MIN) return
        if (!commitVerdict(true, now)) return
        stableObjectLabel = null
        pendingObjectLabel = null

        val matches = BreedEstimator.estimate(features, result.topClasses)
        val top = matches.firstOrNull()
        val threshold = _settings.value.detectionThreshold
        val confident = top != null && top.confidence >= threshold
        val label = if (confident) top!!.breed.name else "Kucing"
        val confidence = if (confident) top!!.confidence else result.confidence

        _uiState.update {
            it.copy(
                status = UiStatus.CatDetected(
                    label = label,
                    confidence = confidence,
                    topClasses = result.topClasses,
                    breedMatches = matches.take(3),
                    features = features,
                    modelLabel = result.label
                ),
                isCameraReady = true,
                isRunning = true,
                detections = it.detections + 1
            )
        }

        if (now - lastInsertAt >= MIN_INSERT_INTERVAL_MS && _settings.value.saveDetections) {
            lastInsertAt = now
            insertEntry(
                DetectionEntry(
                    timestamp = now,
                    label = label,
                    confidence = confidence,
                    photoPath = null,
                    width = TARGET_SIZE,
                    height = TARGET_SIZE
                )
            )
        }
    }

    /**
     * Menahan perpindahan status kucing/bukan-kucing sampai kandidat terbaca
     * konsisten selama [VERDICT_STABLE_MS]. Mengembalikan true bila tampilan
     * boleh diganti.
     */
    private fun commitVerdict(isCat: Boolean, now: Long): Boolean {
        val stable = stableIsCat
        if (stable == null || stable == isCat) {
            stableIsCat = isCat
            pendingIsCat = null
            return true
        }
        if (pendingIsCat != isCat) {
            pendingIsCat = isCat
            pendingVerdictSince = now
            return false
        }
        if (now - pendingVerdictSince < VERDICT_STABLE_MS) return false
        stableIsCat = isCat
        pendingIsCat = null
        return true
    }

    private fun resetVerdictTracking() {
        stableIsCat = null
        pendingIsCat = null
        pendingVerdictSince = 0L
        stableObjectLabel = null
        pendingObjectLabel = null
        pendingObjectSince = 0L
    }

    /**
     * Menahan pergantian nama objek non-kucing sampai label baru terbaca
     * konsisten; mencegah teks seperti "laptop" berubah jadi "iPod" sesaat.
     */
    private fun commitObjectLabel(label: String, now: Long): Boolean {
        val stable = stableObjectLabel
        if (stable == null || stable == label) {
            stableObjectLabel = label
            pendingObjectLabel = null
            return true
        }
        if (pendingObjectLabel != label) {
            pendingObjectLabel = label
            pendingObjectSince = now
            return false
        }
        if (now - pendingObjectSince < OBJECT_LABEL_STABLE_MS) return false
        stableObjectLabel = label
        pendingObjectLabel = null
        return true
    }

    fun stopCamera() {
        detachCameraObservers()
        imageAnalysis?.clearAnalyzer()
        cameraProvider?.unbindAll()
        camera = null
        imageAnalysis = null
        imageCapture = null
        isBound = false
        isBinding = false
        isProcessing.set(false)
        detector.resetSmoothing()
        resetVerdictTracking()
        _cameraOptions.update { it.copy(isTorchOn = false, isFocusing = false, focusPoint = null) }
        _uiState.update {
            it.copy(
                status = UiStatus.Paused,
                isCameraReady = false,
                isCameraStarting = false,
                isRunning = false
            )
        }
    }

    /** Switches between the back and front camera without leaving the screen. */
    fun switchCamera() {
        if (!isBound) return
        val next = if (_cameraOptions.value.lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        isBound = false
        isBinding = false
        detachCameraObservers()
        imageAnalysis?.clearAnalyzer()
        cameraProvider?.unbindAll()
        camera = null
        imageAnalysis = null
        imageCapture = null
        isProcessing.set(false)
        _cameraOptions.update {
            it.copy(
                lensFacing = next,
                isTorchOn = false,
                zoomRatio = 1f,
                isFocusing = false,
                focusPoint = null
            )
        }
        _uiState.update { it.copy(status = UiStatus.Running, isCameraReady = false) }
        startCamera()
    }

    fun toggleTorch() {
        val cam = camera ?: return
        val options = _cameraOptions.value
        if (!options.hasFlash || options.lensFacing == CameraSelector.LENS_FACING_FRONT) {
            return
        }
        val next = !options.isTorchOn
        cam.cameraControl.enableTorch(next)
        _cameraOptions.update { it.copy(isTorchOn = next) }
    }

    fun setZoom(ratio: Float) {
        val cam = camera ?: return
        val max = _cameraOptions.value.maxZoom.coerceAtLeast(1f)
        cam.cameraControl.setZoomRatio(ratio.coerceIn(1f, max))
    }

    fun zoomBy(factor: Float) {
        setZoom(_cameraOptions.value.zoomRatio * factor)
    }

    /** [nx]/[ny] are normalized coordinates (0..1) inside the preview. */
    fun focusAt(nx: Float, ny: Float) {
        val cam = camera ?: return
        val pv = previewView ?: return
        if (pv.width == 0 || pv.height == 0) return

        val point = pv.meteringPointFactory.createPoint(
            nx.coerceIn(0f, 1f) * pv.width,
            ny.coerceIn(0f, 1f) * pv.height
        )
        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()

        cam.cameraControl.startFocusAndMetering(action)
        _cameraOptions.update {
            it.copy(isFocusing = true, focusPoint = nx to ny)
        }
        focusJob?.cancel()
        focusJob = viewModelScope.launch {
            delay(900)
            _cameraOptions.update { it.copy(isFocusing = false) }
        }
    }

    fun takePhoto() {
        val capture = imageCapture
        if (capture == null) {
            showError("Kamera belum siap")
            return
        }
        val dir = photosDir()
        if (!dir.exists()) dir.mkdirs()
        val photoFile = File(dir, "cat_${System.currentTimeMillis()}.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            options,
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val status = _uiState.value.status
                    val entry = if (status is UiStatus.CatDetected) {
                        DetectionEntry(
                            label = status.label,
                            confidence = status.confidence,
                            photoPath = photoFile.absolutePath,
                            width = TARGET_SIZE,
                            height = TARGET_SIZE
                        )
                    } else {
                        DetectionEntry(
                            label = "Foto",
                            confidence = 0f,
                            photoPath = photoFile.absolutePath
                        )
                    }
                    insertEntry(entry)
                    _uiState.update {
                        it.copy(lastPhotoPath = photoFile.absolutePath)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CatVisionViewModel", "takePicture failed", exception)
                    showError(exception.message ?: "Gagal mengambil foto")
                }
            }
        )
    }

    /**
     * Analyses a picture picked from the gallery / file picker. The image is
     * copied into app storage, scaled to the model input size and classified.
     */
    fun analyzeImage(uri: Uri) {
        if (_uiState.value.isAnalyzingImage) return
        if (!detector.isReady) {
            _uiState.update {
                it.copy(importError = "Model AI masih dimuat. Coba lagi sebentar.")
            }
            return
        }

        _uiState.update {
            it.copy(
                isAnalyzingImage = true,
                importedResult = null,
                importError = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            var target: File? = null
            try {
                val dir = File(context.filesDir, "imports")
                if (!dir.exists()) dir.mkdirs()
                target = File(dir, "import_${System.currentTimeMillis()}.jpg")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IOException("Tidak bisa membaca file")

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                var sample = 1
                val longest = maxOf(bounds.outWidth, bounds.outHeight)
                while (longest / (sample * 2) >= 640) {
                    sample *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOptions)
                } ?: throw IOException("Gagal membaca gambar")

                val square = centerCropSquare(bitmap)
                val scaled = Bitmap.createScaledBitmap(square, TARGET_SIZE, TARGET_SIZE, true)
                if (scaled !== bitmap) bitmap.recycle()
                if (square !== bitmap && square !== scaled) square.recycle()
                val pixels = IntArray(TARGET_SIZE * TARGET_SIZE)
                scaled.getPixels(pixels, 0, TARGET_SIZE, 0, 0, TARGET_SIZE, TARGET_SIZE)
                scaled.recycle()

                val buffer = ByteBuffer.allocate(TARGET_SIZE * TARGET_SIZE * 3)
                for (pixel in pixels) {
                    buffer.put(((pixel shr 16) and 0xFF).toByte())
                    buffer.put(((pixel shr 8) and 0xFF).toByte())
                    buffer.put((pixel and 0xFF).toByte())
                }
                buffer.rewind()

                val features = BreedEstimator.analyze(buffer, TARGET_SIZE, TARGET_SIZE)
                val savedPath = target.absolutePath
                withContext(Dispatchers.Main) {
                    detector.detectFrame(buffer, augment = true) { result ->
                        val isCat = result?.isCat == true
                        val matches = if (isCat) {
                            BreedEstimator.estimate(features, result?.topClasses ?: emptyList())
                        } else {
                            emptyList()
                        }
                        val top = matches.firstOrNull()
                        val confident = top != null &&
                            top.confidence >= _settings.value.detectionThreshold
                        val label = when {
                            !isCat -> null
                            confident -> top!!.breed.name
                            else -> "Kucing"
                        }
                        val confidence = when {
                            !isCat -> result?.confidence ?: 0f
                            confident -> top!!.confidence
                            else -> result?.confidence ?: 0f
                        }

                        if (isCat && _settings.value.saveDetections) {
                            insertEntry(
                                DetectionEntry(
                                    label = label ?: result?.label ?: "Kucing",
                                    confidence = confidence,
                                    photoPath = savedPath,
                                    width = TARGET_SIZE,
                                    height = TARGET_SIZE
                                )
                            )
                        }

                        _uiState.update {
                            it.copy(
                                isAnalyzingImage = false,
                                importedResult = ImportedResult(
                                    imagePath = savedPath,
                                    label = label,
                                    confidence = confidence,
                                    topClasses = result?.topClasses ?: emptyList(),
                                    isCat = isCat,
                                    modelLabel = result?.label,
                                    breedMatches = matches.take(3),
                                    features = features
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CatVisionViewModel", "analyzeImage failed", e)
                target?.delete()
                _uiState.update {
                    it.copy(
                        isAnalyzingImage = false,
                        importError = e.message ?: "Gagal menganalisis gambar"
                    )
                }
            }
        }
    }

    fun closeImportedResult() {
        _uiState.update { it.copy(importedResult = null, importError = null, isAnalyzingImage = false) }
    }

    fun insertEntry(entry: DetectionEntry) {
        viewModelScope.launch {
            try {
                dao.insert(entry)
            } catch (e: Exception) {
                Log.e("CatVisionViewModel", "insertEntry failed", e)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                dao.clearAll()
                photosDir().listFiles()?.forEach { it.delete() }
                File(context.filesDir, "imports").listFiles()?.forEach { it.delete() }
                _uiState.update { it.copy(lastPhotoPath = null) }
            } catch (e: Exception) {
                Log.e("CatVisionViewModel", "clearHistory failed", e)
            }
        }
    }

    fun showError(msg: String) {
        _uiState.update { it.copy(status = UiStatus.Error(msg)) }
    }

    fun openHistory() {
        _uiState.update { it.copy(showHistory = true) }
    }

    fun closeHistory() {
        _uiState.update { it.copy(showHistory = false) }
    }

    fun openSettings() {
        _uiState.update { it.copy(showSettings = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(showSettings = false) }
    }

    fun openBreeds() {
        _uiState.update { it.copy(showBreeds = true) }
    }

    fun closeBreeds() {
        _uiState.update { it.copy(showBreeds = false) }
    }

    fun openPhoto() {
        if (_uiState.value.lastPhotoPath != null) {
            _uiState.update { it.copy(showPhoto = true) }
        }
    }

    fun closePhoto() {
        _uiState.update { it.copy(showPhoto = false) }
    }

    override fun onCleared() {
        focusJob?.cancel()
        detachCameraObservers()
        imageAnalysis?.clearAnalyzer()
        cameraProvider?.unbindAll()
        detector.destroy()
        super.onCleared()
    }

    private fun photosDir(): File = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir,
        "CatVision"
    )

    /** Crop tengah persegi supaya rasio foto tidak terdistorsi saat diskalakan. */
    private fun centerCropSquare(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        if (size <= 0 || (bitmap.width == size && bitmap.height == size)) return bitmap
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }

    /**
     * Converts a YUV_420_888 [ImageProxy] into a packed RGB888 buffer scaled to
     * [TARGET_SIZE] x [TARGET_SIZE], the input size expected by the model.
     */
    private fun convertYUV420ToRgb888(image: ImageProxy, out: ByteBuffer) {
        val planes = image.planes
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        val yBuffer: ByteBuffer = yPlane.buffer
        val uBuffer: ByteBuffer = uPlane.buffer
        val vBuffer: ByteBuffer = vPlane.buffer

        val width = image.width
        val height = image.height
        val yBase = yBuffer.position()
        val uBase = uBuffer.position()
        val vBase = vBuffer.position()

        out.clear()

        // Center-crop ke persegi dulu: model tidak perlu melihat seluruh frame
        // portrait yang terdistorsi. Crop dibuat genap agar UV sampling tepat.
        val side = minOf(width, height).and(-2).coerceAtLeast(2)
        val cropX = ((width - side) / 2).and(-2)
        val cropY = ((height - side) / 2).and(-2)
        val xStep = side.toFloat() / TARGET_SIZE
        val yStep = side.toFloat() / TARGET_SIZE

        for (ty in 0 until TARGET_SIZE) {
            val sy = cropY + (ty * yStep).toInt().coerceIn(0, side - 1)
            for (tx in 0 until TARGET_SIZE) {
                val sx = cropX + (tx * xStep).toInt().coerceIn(0, side - 1)

                val yIndex = yBase + sy * yPlane.rowStride + sx * yPlane.pixelStride
                val uvX = sx / 2
                val uvY = sy / 2
                val uIndex = uBase + uvY * uPlane.rowStride + uvX * uPlane.pixelStride
                val vIndex = vBase + uvY * vPlane.rowStride + uvX * vPlane.pixelStride

                val yy = yBuffer.get(yIndex).toInt() and 0xFF
                val uu = uBuffer.get(uIndex).toInt() and 0xFF
                val vv = vBuffer.get(vIndex).toInt() and 0xFF

                val r = (yy + 1.402f * (vv - 128)).toInt().coerceIn(0, 255)
                val g = (yy - 0.344136f * (uu - 128) - 0.714136f * (vv - 128))
                    .toInt().coerceIn(0, 255)
                val b = (yy + 1.772f * (uu - 128)).toInt().coerceIn(0, 255)

                out.put(r.toByte())
                out.put(g.toByte())
                out.put(b.toByte())
            }
        }
        out.rewind()
    }

    companion object {
        private const val PREFS_NAME = "catvision_settings"
        private const val KEY_AUTO_START = "auto_start_camera"
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_SAVE_DETECTIONS = "save_detections"
        private const val KEY_THRESHOLD = "detection_threshold"

        private const val TARGET_SIZE = 224
        private const val FRAME_INTERVAL_MS = 260L
        private const val MIN_INSERT_INTERVAL_MS = 2_000L
        private const val DETECTION_GRACE_MS = 1_800L

        /** Lama kandidat kucing/bukan-kucing harus konsisten sebelum tampil. */
        private const val VERDICT_STABLE_MS = 900L

        /** Ambang bukti kucing untuk berpindah dari kartu "bukan kucing". */
        private const val CAT_SWITCH_MIN = 0.30f

        /** Ambang objek lain untuk berpindah dari kartu "kucing". */
        private const val NONCAT_SWITCH_MIN = 0.25f

        /** Lama nama objek non-kucing harus konsisten sebelum ditampilkan. */
        private const val OBJECT_LABEL_STABLE_MS = 1_500L

        /** Keyakinan minimum untuk mengganti nama objek non-kucing. */
        private const val NONCAT_LABEL_MIN = 0.25f
        private const val HISTORY_LIMIT = 100

        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(CatVisionViewModel::class.java)) {
                        return CatVisionViewModel(
                            context,
                            CatVisionDatabase.getInstance(appContext)
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
