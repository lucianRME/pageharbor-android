package org.synapseworks.pageharbor.image

import kotlin.math.roundToInt

/**
 * A synchronous, Android-independent ARGB filter engine. The caller owns [ArgbImage.pixels].
 * [DocumentFilter.ORIGINAL] returns the same instance; every other filter creates a new array and
 * never mutates the source image. Callers choose the appropriate background dispatcher later.
 */
object DocumentImageFilterEngine {
    private const val RedLuminanceWeight = 0.2126f
    private const val GreenLuminanceWeight = 0.7152f
    private const val BlueLuminanceWeight = 0.0722f
    private const val HighContrastFactor = 1.55f
    private const val AutoBlackPointPercentile = 0.04f
    private const val AutoWhitePointPercentile = 0.96f

    fun apply(source: ArgbImage, filter: DocumentFilter): ArgbImage = when (filter) {
        DocumentFilter.ORIGINAL -> source
        DocumentFilter.GRAYSCALE -> transform(source) { alpha, red, green, blue ->
            val luminance = luminance(red, green, blue)
            argb(alpha, luminance, luminance, luminance)
        }
        DocumentFilter.HIGH_CONTRAST -> transform(source) { alpha, red, green, blue ->
            argb(alpha, contrast(red), contrast(green), contrast(blue))
        }
        DocumentFilter.AUTO_ENHANCE -> autoEnhance(source)
        DocumentFilter.BLACK_AND_WHITE -> blackAndWhite(source)
    }

    private fun autoEnhance(source: ArgbImage): ArgbImage {
        val histogram = luminanceHistogram(source)
        val blackPoint = percentile(histogram, source.pixels.size, AutoBlackPointPercentile)
        val whitePoint = percentile(histogram, source.pixels.size, AutoWhitePointPercentile)
        if (whitePoint <= blackPoint) return apply(source, DocumentFilter.HIGH_CONTRAST)
        return transform(source) { alpha, red, green, blue ->
            argb(
                alpha,
                stretch(red, blackPoint, whitePoint),
                stretch(green, blackPoint, whitePoint),
                stretch(blue, blackPoint, whitePoint),
            )
        }
    }

    private fun blackAndWhite(source: ArgbImage): ArgbImage {
        val threshold = otsuThreshold(luminanceHistogram(source), source.pixels.size)
        return transform(source) { alpha, red, green, blue ->
            val value = if (luminance(red, green, blue) <= threshold) 0 else 255
            argb(alpha, value, value, value)
        }
    }

    private fun luminanceHistogram(source: ArgbImage): IntArray = IntArray(256).also { histogram ->
        source.pixels.forEach { pixel ->
            histogram[luminance(red(pixel), green(pixel), blue(pixel))]++
        }
    }

    private fun percentile(histogram: IntArray, total: Int, percentile: Float): Int {
        val target = (total * percentile).roundToInt().coerceIn(0, total - 1)
        var count = 0
        histogram.forEachIndexed { value, frequency ->
            count += frequency
            if (count > target) return value
        }
        return 255
    }

    private fun otsuThreshold(histogram: IntArray, total: Int): Int {
        var totalLuminance = 0L
        histogram.forEachIndexed { luminance, count -> totalLuminance += luminance.toLong() * count }
        var backgroundCount = 0
        var backgroundLuminance = 0L
        var bestThreshold = 127
        var bestVariance = -1.0
        histogram.forEachIndexed { value, count ->
            backgroundCount += count
            if (backgroundCount == 0) return@forEachIndexed
            val foregroundCount = total - backgroundCount
            if (foregroundCount == 0) return@forEachIndexed
            backgroundLuminance += value.toLong() * count
            val backgroundMean = backgroundLuminance.toDouble() / backgroundCount
            val foregroundMean = (totalLuminance - backgroundLuminance).toDouble() / foregroundCount
            val variance = backgroundCount.toDouble() * foregroundCount *
                (backgroundMean - foregroundMean) * (backgroundMean - foregroundMean)
            if (variance > bestVariance) {
                bestVariance = variance
                bestThreshold = value
            }
        }
        return bestThreshold
    }

    private inline fun transform(
        source: ArgbImage,
        pixel: (alpha: Int, red: Int, green: Int, blue: Int) -> Int,
    ): ArgbImage = ArgbImage(
        width = source.width,
        height = source.height,
        pixels = IntArray(source.pixels.size) { index ->
            val value = source.pixels[index]
            pixel(alpha(value), red(value), green(value), blue(value))
        },
    )

    private fun stretch(value: Int, low: Int, high: Int): Int =
        (((value - low) * 255f) / (high - low)).roundToInt().coerceIn(0, 255)

    private fun contrast(value: Int): Int =
        ((value - 128) * HighContrastFactor + 128).roundToInt().coerceIn(0, 255)

    private fun luminance(red: Int, green: Int, blue: Int): Int =
        (red * RedLuminanceWeight + green * GreenLuminanceWeight + blue * BlueLuminanceWeight)
            .roundToInt()
            .coerceIn(0, 255)

    private fun alpha(pixel: Int): Int = pixel ushr 24 and 0xff
    private fun red(pixel: Int): Int = pixel ushr 16 and 0xff
    private fun green(pixel: Int): Int = pixel ushr 8 and 0xff
    private fun blue(pixel: Int): Int = pixel and 0xff
    private fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int =
        alpha shl 24 or (red shl 16) or (green shl 8) or blue
}

data class ArgbImage(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
) {
    init {
        require(isSupportedDimensions(width, height))
        require(pixels.size.toLong() == width.toLong() * height)
    }

    companion object {
        fun isSupportedDimensions(width: Int, height: Int): Boolean =
            width > 0 && height > 0 && width.toLong() * height <= Int.MAX_VALUE
    }
}
