package id.cv.catvision.detector

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import id.cv.catvision.R
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.exp

/**
 * Wraps the bundled MobileNetV2 TFLite model and turns a single RGB frame
 * into a cat-detection result.
 */
class CatDetector(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var isInitialized = false

    /** Rata-rata bergerak probabilitas antar-frame (khusus mode kamera). */
    private var emaProbabilities: FloatArray? = null

    private val detectorExecutor: Executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Reused across frames; only touched on [detectorExecutor]. */
    private val inputBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(INPUT_SIZE * FLOAT_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    val isReady: Boolean
        get() = isInitialized

    data class DetectionResult(
        val label: String,
        val confidence: Float,
        val box: FloatArray,
        val topClasses: List<Pair<String, Float>>,
        val isCat: Boolean = true,
        val catScore: Float = 0f,
    )

    private val labels: List<String> by lazy {
        context.resources.getStringArray(R.array.imagenet_labels).toList()
    }

    /** Loads the model from assets into the cache directory and prepares the interpreter. */
    fun loadModel(onComplete: (Boolean) -> Unit = {}) {
        detectorExecutor.execute {
            if (interpreter != null && isInitialized) {
                mainHandler.post { onComplete(true) }
                return@execute
            }
            try {
                val modelFile = File(context.cacheDir, "mobilenet_v2.tflite")
                if (!modelFile.exists() || modelFile.length() == 0L) {
                    context.assets.open("mobilenet_v2.tflite").use { input ->
                        FileOutputStream(modelFile).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                            }
                        }
                    }
                }
                interpreter = Interpreter(
                    modelFile,
                    Interpreter.Options().apply {
                        setNumThreads(4)
                        setUseXNNPACK(true)
                    }
                )
                interpreter?.allocateTensors()
                isInitialized = true
                Log.d("CatDetector", "Model loaded: ${modelFile.length()} bytes")
                mainHandler.post { onComplete(true) }
            } catch (e: Exception) {
                Log.e("CatDetector", "Failed to initialize model", e)
                isInitialized = false
                mainHandler.post { onComplete(false) }
            }
        }
    }

    fun destroy() {
        detectorExecutor.execute {
            runCatching { interpreter?.close() }
            interpreter = null
            isInitialized = false
        }
    }

    /**
     * Runs inference on one packed RGB frame ([width] x [height] x 3 bytes).
     * The buffer must contain exactly 224 * 224 * 3 bytes.
     *
     * [augment] menambahkan inferensi versi cermin horizontal lalu merata-ratakan
     * probabilitasnya. Dipakai untuk foto galeri agar hasil lebih stabil;
     * untuk frame kamera dibiarkan mati supaya tetap ringan.
     */
    fun detectFrame(
        rgbBuffer: ByteBuffer,
        augment: Boolean = false,
        onResult: (DetectionResult?) -> Unit
    ) {
        detectorExecutor.execute {
            val current = interpreter
            if (current == null || !isInitialized) {
                mainHandler.post { onResult(null) }
                return@execute
            }
            try {
                val probabilities = runInference(current, rgbBuffer)
                val finalProbabilities = if (augment) {
                    val mirrored = mirroredRgb(rgbBuffer)
                    val mirroredProbabilities = runInference(current, mirrored)
                    FloatArray(NUM_CLASSES) { index ->
                        (probabilities[index] + mirroredProbabilities[index]) / 2f
                    }
                } else {
                    probabilities
                }
                val result = processOutput(finalProbabilities)
                mainHandler.post { onResult(result) }
            } catch (e: Throwable) {
                Log.w("CatDetector", "Frame preprocessing/inference failed", e)
                mainHandler.post { onResult(null) }
            }
        }
    }

    /**
     * Analisis frame kamera: menjalankan model pada dua skala (seluruh frame
     * dan crop tengah 62%), memilih yang paling kuat sinyal kucingnya, lalu
     * menghaluskan probabilitas antar-frame supaya label tidak berubah-ubah
     * secara acak.
     */
    fun detectSceneFrame(rgbBuffer: ByteBuffer, onResult: (DetectionResult?) -> Unit) {
        detectorExecutor.execute {
            val current = interpreter
            if (current == null || !isInitialized) {
                mainHandler.post { onResult(null) }
                return@execute
            }
            try {
                val full = runInference(current, rgbBuffer)
                val zoom = runInference(current, centerZoomRgb(rgbBuffer, 0.62f))
                val chosen = if (catMass(zoom) > catMass(full) * 1.10f) zoom else full
                val smoothed = smoothProbabilities(chosen)
                val result = processOutput(smoothed)
                mainHandler.post { onResult(result) }
            } catch (e: Throwable) {
                Log.w("CatDetector", "Scene inference failed", e)
                mainHandler.post { onResult(null) }
            }
        }
    }

    /** Menghapus riwayat penghalusan saat kamera dinyalakan ulang. */
    fun resetSmoothing() {
        detectorExecutor.execute { emaProbabilities = null }
    }

    private fun smoothProbabilities(probs: FloatArray): FloatArray {
        val previous = emaProbabilities
        val blended = if (previous == null) {
            probs.copyOf()
        } else {
            FloatArray(probs.size) { index ->
                previous[index] * (1f - EMA_ALPHA) + probs[index] * EMA_ALPHA
            }
        }
        emaProbabilities = blended
        return blended
    }

    /** Crop tengah [fraction] dari frame lalu diskalakan ke 224 x 224. */
    private fun centerZoomRgb(rgbBuffer: ByteBuffer, fraction: Float): ByteBuffer {
        val source = rgbBuffer.asReadOnlyBuffer()
        source.rewind()
        val crop = (TARGET_SIZE * fraction).toInt().coerceIn(16, TARGET_SIZE)
        val offset = (TARGET_SIZE - crop) / 2
        val out = ByteBuffer.allocate(INPUT_SIZE)
        for (y in 0 until TARGET_SIZE) {
            val sy = offset + ((y * crop) / TARGET_SIZE).coerceIn(0, crop - 1)
            for (x in 0 until TARGET_SIZE) {
                val sx = offset + ((x * crop) / TARGET_SIZE).coerceIn(0, crop - 1)
                val from = (sy * TARGET_SIZE + sx) * 3
                out.put(source.get(from))
                out.put(source.get(from + 1))
                out.put(source.get(from + 2))
            }
        }
        out.rewind()
        return out
    }

    private fun catMass(probs: FloatArray): Float {
        var sum = 0f
        for (index in catIndices) sum += probs[index]
        return sum
    }

    private val catIndices: IntArray by lazy {
        labels.indices.filter { isCatClass(labels[it]) }.toIntArray()
    }

    private fun runInference(interpreter: Interpreter, rgbBuffer: ByteBuffer): FloatArray {
        val pixels = rgbBuffer.asReadOnlyBuffer().order(ByteOrder.nativeOrder())
        inputBuffer.clear()
        for (i in 0 until INPUT_SIZE) {
            val channel = pixels.get(i).toInt() and 0xFF
            inputBuffer.put(channel / 255f)
        }
        inputBuffer.rewind()

        val output = Array(1) { FloatArray(NUM_CLASSES) }
        interpreter.run(inputBuffer, output)
        return normalizeScores(output[0])
    }

    /**
     * Sebagian file TFLite mengeluarkan logit, bukan probabilitas. Jika jumlah
     * seluruh skor tidak mendekati 1, terapkan softmax agar semua angka yang
     * ditampilkan tetap dalam rentang 0-100%.
     */
    private fun normalizeScores(scores: FloatArray): FloatArray {
        var sum = 0.0
        var outOfRange = false
        for (score in scores) {
            sum += score
            if (score < -0.001f || score > 1.001f) outOfRange = true
        }
        val looksLikeProbabilities = !outOfRange && sum in 0.85..1.15
        if (looksLikeProbabilities) return scores.copyOf()

        var max = Float.NEGATIVE_INFINITY
        for (score in scores) if (score > max) max = score
        val exps = DoubleArray(scores.size) { exp((scores[it] - max).toDouble()) }
        val total = exps.sum()
        if (total <= 0.0) return scores.copyOf()
        return FloatArray(scores.size) { (exps[it] / total).toFloat() }
    }

    /** Salinan frame yang dicerminkan kiri-kanan (224 x 224 x 3). */
    private fun mirroredRgb(rgbBuffer: ByteBuffer): ByteBuffer {
        val source = rgbBuffer.asReadOnlyBuffer()
        source.rewind()
        val out = ByteBuffer.allocate(INPUT_SIZE)
        for (y in 0 until TARGET_SIZE) {
            val rowStart = y * TARGET_SIZE
            for (x in 0 until TARGET_SIZE) {
                val from = (rowStart + (TARGET_SIZE - 1 - x)) * 3
                out.put(source.get(from))
                out.put(source.get(from + 1))
                out.put(source.get(from + 2))
            }
        }
        out.rewind()
        return out
    }

    private fun processOutput(probs: FloatArray): DetectionResult? {
        val ranked = probs.indices
            .map { it to probs[it] }
            .sortedByDescending { it.second }
            .take(15)

        if (ranked.isEmpty()) return null

        val topLabel = labelFor(ranked.first().first)
        val topScore = ranked.first().second
        val catMatches = ranked.filter { isCatClass(labelFor(it.first)) }
        val bestCat = catMatches.firstOrNull()
        val catSum = catMatches.take(8)
            .sumOf { it.second.toDouble() }
            .toFloat()

        val catEvidence = catSum >= CAT_SUM_MIN ||
            (bestCat?.second ?: 0f) >= CAT_SINGLE_MIN
        val topIsCat = isCatClass(topLabel)
        val nonCatDominant = !topIsCat &&
            topScore >= NON_CAT_MIN &&
            topScore > (bestCat?.second ?: 0f) * 3f

        val topClasses = ranked.take(5).map { (index, score) ->
            labelFor(index) to score
        }

        // Bukan kucing: objek lain mendominasi atau bukti kucing terlalu lemah.
        // Jika top-1 sendiri kelas kucing, tetap anggap kucing walau skornya kecil.
        if ((!catEvidence || nonCatDominant) && !topIsCat) {
            return if (topScore >= NON_CAT_MIN) {
                DetectionResult(
                    label = topLabel,
                    confidence = topScore,
                    box = floatArrayOf(0f, 0f, 1f, 1f),
                    topClasses = topClasses,
                    isCat = false,
                    catScore = catSum
                )
            } else {
                null
            }
        }

        val best = bestCat ?: ranked.first()
        return DetectionResult(
            label = labelFor(best.first),
            confidence = best.second,
            box = floatArrayOf(0f, 0f, 1f, 1f),
            topClasses = topClasses,
            isCat = true,
            catScore = catSum
        )
    }

    private fun isCatClass(name: String): Boolean =
        CAT_CLASSES.any { it.equals(name, ignoreCase = true) }

    private fun labelFor(index: Int): String = labels.getOrNull(index) ?: "unknown"

    companion object {
        private const val INPUT_SIZE = 224 * 224 * 3
        private const val TARGET_SIZE = 224
        private const val FLOAT_BYTES = 4
        private const val NUM_CLASSES = 1000

        private const val CAT_SINGLE_MIN = 0.12f
        private const val CAT_SUM_MIN = 0.25f
        private const val NON_CAT_MIN = 0.20f

        /** Bobot frame baru pada penghalusan; makin kecil makin stabil. */
        private const val EMA_ALPHA = 0.35f

        /** Kelas ImageNet yang benar-benar kucing (termasuk kucing besar). */
        private val CAT_CLASSES = setOf(
            "tabby", "tiger cat", "persian cat", "siamese cat", "egyptian cat",
            "cougar", "lynx", "leopard", "snow leopard", "jaguar", "lion",
            "tiger", "cheetah"
        )
    }
}
