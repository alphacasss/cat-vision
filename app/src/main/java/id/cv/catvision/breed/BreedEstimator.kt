package id.cv.catvision.breed

import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/** Hasil pencocokan satu breed beserta alasannya. */
data class BreedMatch(
    val breed: CatBreed,
    val confidence: Float,
    val reasons: List<String>,
)

/**
 * Estimasi breed berbasis ciri visual yang bisa diukur dari foto:
 * warna bulu dominan, pola (solid/tabby/bicolor/colorpoint/calico/tortie),
 * kontras wajah-badan, dan tekstur/belang. Ditambah hint dari model
 * ImageNet (Persian, Siamese, Egyptian, tabby, tiger cat).
 *
 * Catatan: telinga terlipat/melengkung dan panjang kaki belum bisa
 * dideteksi tanpa model khusus, jadi trait tersebut hanya ada di database.
 */
object BreedEstimator {

    data class Features(
        val white: Float,
        val black: Float,
        val blueGray: Float,
        val orange: Float,
        val cream: Float,
        val brown: Float,
        val golden: Float,
        val silver: Float,
        val lilac: Float,
        val brightness: Float,
        val saturation: Float,
        val stripeScore: Float,
        val colorpointScore: Float,
        val pattern: CoatPattern,
        val topColors: List<Pair<CoatColor, Float>>,
    ) {
        val colorSummary: String
            get() = if (topColors.isEmpty()) {
                "tidak jelas"
            } else {
                topColors.take(2).joinToString(" + ") { it.first.label.lowercase() }
            }
    }

    private data class Scored(
        val breed: CatBreed,
        val score: Float,
        val reasons: List<String>,
    )

    private data class RegionStats(
        val meanV: Float,
        val darkFraction: Float,
    )

    /**
     * Membaca buffer RGB888 (224 x 224 x 3) dan mengekstrak ciri visual.
     * Hanya area tengah yang dipakai supaya latar belakang tidak mendominasi.
     */
    fun analyze(buffer: ByteBuffer, width: Int, height: Int): Features {
        val data = buffer.asReadOnlyBuffer()
        data.rewind()

        val x0 = (width * 0.18f).toInt().coerceIn(0, width - 1)
        val x1 = (width * 0.82f).toInt().coerceIn(x0 + 1, width)
        val y0 = (height * 0.14f).toInt().coerceIn(0, height - 1)
        val y1 = (height * 0.90f).toInt().coerceIn(y0 + 1, height)

        val step = max(1, min(width, height) / 64)
        val gridW = max(1, (x1 - x0) / step)
        val gridH = max(1, (y1 - y0) / step)
        val lumGrid = FloatArray(gridW * gridH)

        val counts = HashMap<CoatColor, Int>()
        var totalV = 0f
        var totalS = 0f
        var samples = 0
        var stripeDiff = 0.0
        var stripeCount = 0

        var gy = 0
        var y = y0
        while (y < y1 && gy < gridH) {
            var gx = 0
            var x = x0
            while (x < x1 && gx < gridW) {
                val index = (y * width + x) * 3
                if (index + 2 < data.limit()) {
                    val r = data.get(index).toInt() and 0xFF
                    val g = data.get(index + 1).toInt() and 0xFF
                    val b = data.get(index + 2).toInt() and 0xFF

                    val color = classifyColor(r, g, b)
                    counts[color] = (counts[color] ?: 0) + 1

                    val rf = r / 255f
                    val gf = g / 255f
                    val bf = b / 255f
                    val mx = max(rf, max(gf, bf))
                    val mn = min(rf, min(gf, bf))
                    val v = mx
                    val s = if (mx <= 0.0001f) 0f else (mx - mn) / mx
                    totalV += v
                    totalS += s

                    val lum = 0.2126f * rf + 0.7152f * gf + 0.0722f * bf
                    lumGrid[gy * gridW + gx] = lum

                    // Kontras lokal: indikator pola tabby/belang.
                    if (gx > 0) {
                        stripeDiff += abs(lum - lumGrid[gy * gridW + gx - 1]).toDouble()
                        stripeCount++
                    }
                    if (gy > 0) {
                        stripeDiff += abs(lum - lumGrid[(gy - 1) * gridW + gx]).toDouble()
                        stripeCount++
                    }
                    samples++
                }
                x += step
                gx++
            }
            y += step
            gy++
        }

        if (samples == 0) {
            return emptyFeatures()
        }

        fun fraction(color: CoatColor): Float =
            (counts[color] ?: 0).toFloat() / samples

        val white = fraction(CoatColor.WHITE)
        val black = fraction(CoatColor.BLACK)
        val blueGray = fraction(CoatColor.BLUE)
        val orange = fraction(CoatColor.ORANGE)
        val cream = fraction(CoatColor.CREAM)
        val brown = fraction(CoatColor.BROWN)
        val golden = fraction(CoatColor.GOLDEN)
        val silver = fraction(CoatColor.SILVER)
        val lilac = fraction(CoatColor.LILAC)

        val stripeScore = if (stripeCount == 0) 0f else (stripeDiff / stripeCount).toFloat()

        val face = regionStats(data, width, height, 0.30f, 0.16f, 0.70f, 0.46f)
        val body = regionStats(data, width, height, 0.32f, 0.55f, 0.68f, 0.88f)
        val delta = (body.meanV - face.meanV).coerceAtLeast(0f)
        val darkDelta = (face.darkFraction - body.darkFraction).coerceAtLeast(0f)
        var colorpoint = (delta / 0.30f).coerceIn(0f, 1f) * 0.65f +
            (darkDelta / 0.35f).coerceIn(0f, 1f) * 0.35f
        if (body.meanV < 0.40f) colorpoint *= 0.35f

        val pattern = when {
            orange > 0.07f && black > 0.07f && white > 0.06f -> CoatPattern.CALICO
            orange > 0.06f && black > 0.06f -> CoatPattern.TORTIE
            colorpoint >= 0.45f -> CoatPattern.COLORPOINT
            white > 0.20f && (black + blueGray + orange + brown) > 0.20f -> CoatPattern.BICOLOR
            stripeScore >= 0.065f -> CoatPattern.TABBY
            else -> CoatPattern.SOLID
        }

        val topColors = listOf(
            CoatColor.WHITE to white,
            CoatColor.BLACK to black,
            CoatColor.BLUE to blueGray,
            CoatColor.ORANGE to orange,
            CoatColor.CREAM to cream,
            CoatColor.BROWN to brown,
            CoatColor.GOLDEN to golden,
            CoatColor.SILVER to silver,
            CoatColor.LILAC to lilac,
        )
            .filter { it.second >= 0.03f }
            .sortedByDescending { it.second }
            .take(4)

        return Features(
            white = white,
            black = black,
            blueGray = blueGray,
            orange = orange,
            cream = cream,
            brown = brown,
            golden = golden,
            silver = silver,
            lilac = lilac,
            brightness = totalV / samples,
            saturation = totalS / samples,
            stripeScore = stripeScore,
            colorpointScore = colorpoint,
            pattern = pattern,
            topColors = topColors,
        )
    }

    /**
     * Mengurutkan breed yang paling cocok. [modelTop] adalah hasil ImageNet
     * (label + probabilitas) yang dipakai sebagai petunjuk tambahan.
     */
    fun estimate(
        features: Features,
        modelTop: List<Pair<String, Float>> = emptyList(),
    ): List<BreedMatch> {
        val hints = modelTop
            .take(8)
            .map { it.first.lowercase() to it.second }

        val scored = CatBreeds.all.map { breed ->
            var score = 0.35f + breed.popularity * 0.018f
            if (breed.contest) score += 0.04f

            score += if (breed.colors.contains(CoatColor.ANY)) {
                0.45f
            } else {
                features.topColors.sumOf { (color, fraction) ->
                    val weight = if (breed.colors.contains(color)) 1.15 else -0.35
                    (weight * fraction).toDouble()
                }.toFloat().coerceIn(-0.60f, 1.10f)
            }

            score += when {
                breed.patterns.contains(CoatPattern.ANY) -> 0.25f
                breed.patterns.contains(features.pattern) -> 0.75f
                features.pattern == CoatPattern.TABBY &&
                    breed.patterns.contains(CoatPattern.SPOTTED) -> 0.55f
                features.pattern == CoatPattern.BICOLOR &&
                    breed.patterns.contains(CoatPattern.CALICO) -> 0.35f
                features.pattern == CoatPattern.SOLID &&
                    breed.patterns.contains(CoatPattern.BICOLOR) -> 0.10f
                else -> -0.25f
            }

            score += if (breed.colorpoint) {
                if (features.colorpointScore >= 0.45f) {
                    0.65f + features.colorpointScore * 0.30f
                } else {
                    -0.45f
                }
            } else {
                if (features.colorpointScore >= 0.55f) -0.50f else 0.22f
            }

            score += signatureBonus(breed, features)

            var hintReason: String? = null
            for ((label, probability) in hints) {
                if (label.length < 3) continue
                val hit = breed.modelHints.any { hint ->
                    val normalized = hint.lowercase()
                    label.contains(normalized) || normalized.contains(label)
                }
                if (hit) {
                    score += 0.35f + probability * 1.4f
                    hintReason = "Model AI mengenali \"$label\""
                    break
                }
            }

            Scored(breed, score, buildReasons(features, hintReason))
        }

        val maxScore = scored.maxOf { it.score }
        val candidates = scored
            .sortedByDescending { it.score }
            .take(8)
        val maxCandidate = candidates.first().score
        val exps = candidates.map { exp((it.score - maxCandidate) * 2.0) }
        val total = exps.sum()
        if (total <= 0.0) return emptyList()

        return candidates.indices
            .map { index ->
                BreedMatch(
                    breed = candidates[index].breed,
                    confidence = (exps[index] / total).toFloat().coerceIn(0.01f, 0.94f),
                    reasons = candidates[index].reasons,
                )
            }
            .sortedByDescending { it.confidence }
            .take(5)
    }

    /**
     * Bonus untuk kombinasi warna + pola yang menjadi ciri khas breed tertentu.
     * Membantu memisahkan breed yang sama-sama punya warna dasar serupa.
     */
    private fun signatureBonus(breed: CatBreed, f: Features): Float = when (breed.id) {
        "british-shorthair" ->
            if (f.blueGray >= 0.18f && f.pattern == CoatPattern.SOLID) 0.45f else 0f
        "russian-blue", "chartreux", "korat" ->
            if (f.blueGray >= 0.25f && f.pattern == CoatPattern.SOLID) 0.30f else 0f
        "american-shorthair" ->
            if (f.silver >= 0.15f && f.pattern == CoatPattern.TABBY) 0.40f else 0f
        "bengal", "toyger", "savannah", "egyptian-mau" ->
            if (f.pattern == CoatPattern.TABBY && (f.golden + f.brown) >= 0.18f) 0.30f else 0f
        "siamese", "tonkinese" ->
            if (f.colorpointScore >= 0.50f && f.cream >= 0.12f) 0.25f else 0f
        "ragdoll", "birman" ->
            if (f.colorpointScore >= 0.50f && f.white >= 0.10f) 0.25f else 0f
        "himalayan" ->
            if (f.colorpointScore >= 0.50f && (f.cream + f.white) >= 0.20f) 0.25f else 0f
        "persian" ->
            if (f.pattern == CoatPattern.SOLID && (f.white + f.cream) >= 0.25f) 0.25f else 0f
        "maine-coon" ->
            if (f.pattern == CoatPattern.TABBY && f.brown >= 0.12f) 0.20f else 0f
        "sphynx" ->
            if (f.brightness > 0.55f && f.saturation < 0.25f) 0.15f else 0f
        "bombay" ->
            if (f.black >= 0.35f) 0.30f else 0f
        "khao-manee" ->
            if (f.white >= 0.55f) 0.25f else 0f
        "turkish-van" ->
            if (f.white >= 0.45f && (f.orange + f.cream) >= 0.10f) 0.25f else 0f
        else -> 0f
    }

    private fun buildReasons(features: Features, hint: String?): List<String> {
        val reasons = mutableListOf<String>()
        features.topColors.take(2).forEach { (color, fraction) ->
            if (fraction >= 0.10f) {
                reasons.add("Warna ${color.label.lowercase()} dominan")
            }
        }
        if (features.pattern != CoatPattern.SOLID) {
            reasons.add("Pola ${features.pattern.label.lowercase()}")
        }
        if (features.colorpointScore >= 0.45f) {
            reasons.add("Ciri colorpoint terlihat")
        }
        hint?.let { reasons.add(it) }
        return reasons.take(3)
    }

    private fun emptyFeatures(): Features = Features(
        white = 0f,
        black = 0f,
        blueGray = 0f,
        orange = 0f,
        cream = 0f,
        brown = 0f,
        golden = 0f,
        silver = 0f,
        lilac = 0f,
        brightness = 0f,
        saturation = 0f,
        stripeScore = 0f,
        colorpointScore = 0f,
        pattern = CoatPattern.SOLID,
        topColors = emptyList(),
    )

    private fun regionStats(
        data: ByteBuffer,
        width: Int,
        height: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ): RegionStats {
        val x0 = (width * left).toInt().coerceIn(0, width - 1)
        val x1 = (width * right).toInt().coerceIn(x0 + 1, width)
        val y0 = (height * top).toInt().coerceIn(0, height - 1)
        val y1 = (height * bottom).toInt().coerceIn(y0 + 1, height)
        val step = max(2, min(width, height) / 56)

        var sumV = 0f
        var dark = 0
        var count = 0
        var y = y0
        while (y < y1) {
            var x = x0
            while (x < x1) {
                val index = (y * width + x) * 3
                if (index + 2 < data.limit()) {
                    val r = data.get(index).toInt() and 0xFF
                    val g = data.get(index + 1).toInt() and 0xFF
                    val b = data.get(index + 2).toInt() and 0xFF
                    val v = max(r, max(g, b)) / 255f
                    sumV += v
                    if (v < 0.30f) dark++
                    count++
                }
                x += step
            }
            y += step
        }
        if (count == 0) return RegionStats(0f, 0f)
        return RegionStats(sumV / count, dark.toFloat() / count)
    }

    private fun classifyColor(r: Int, g: Int, b: Int): CoatColor {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val mx = max(rf, max(gf, bf))
        val mn = min(rf, min(gf, bf))
        val delta = mx - mn
        val v = mx
        val s = if (mx <= 0.0001f) 0f else delta / mx

        var h = 0f
        if (delta > 0.0001f) {
            h = when (mx) {
                rf -> 60f * (((gf - bf) / delta) % 6f)
                gf -> 60f * (((bf - rf) / delta) + 2f)
                else -> 60f * (((rf - gf) / delta) + 4f)
            }
            if (h < 0f) h += 360f
        }

        return when {
            v < 0.15f -> CoatColor.BLACK
            s < 0.10f && v > 0.72f -> CoatColor.WHITE
            s < 0.16f && v in 0.30f..0.72f -> CoatColor.SILVER
            v < 0.30f && s > 0.20f && h in 10f..55f -> CoatColor.BROWN
            v < 0.30f -> CoatColor.BLACK
            h in 185f..270f && s in 0.08f..0.45f && v in 0.28f..0.80f -> CoatColor.BLUE
            h in 10f..45f && s > 0.45f && v > 0.45f -> CoatColor.ORANGE
            h in 10f..50f && s in 0.15f..0.42f && v > 0.68f -> CoatColor.CREAM
            h in 45f..75f && s > 0.22f && v in 0.45f..0.85f -> CoatColor.GOLDEN
            h in 20f..60f && s in 0.25f..0.75f && v in 0.32f..0.68f -> CoatColor.BROWN
            h in 255f..315f && s in 0.08f..0.40f && v > 0.42f -> CoatColor.LILAC
            v > 0.65f && s < 0.22f -> CoatColor.SILVER
            s < 0.18f && v > 0.72f -> CoatColor.WHITE
            s < 0.22f -> CoatColor.SILVER
            v > 0.70f -> CoatColor.CREAM
            else -> CoatColor.BROWN
        }
    }
}
