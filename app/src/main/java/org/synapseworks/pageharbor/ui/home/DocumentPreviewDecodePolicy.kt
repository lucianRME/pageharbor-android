package org.synapseworks.pageharbor.ui.home

/**
 * Bounds one transient OCR Result preview bitmap. The source scan is never modified or cached.
 */
internal object DocumentPreviewDecodePolicy {
    private const val MAX_LONG_EDGE = 1_600
    private const val MAX_PIXEL_COUNT = 2_500_000L

    fun calculateInSampleSize(width: Int, height: Int): Int? {
        if (width <= 0 || height <= 0) return null

        var sampleSize = 1
        while (exceedsBounds(width, height, sampleSize)) {
            if (sampleSize > Int.MAX_VALUE / 2) return null
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun exceedsBounds(width: Int, height: Int, sampleSize: Int): Boolean {
        val sampledWidth = ceilDivide(width, sampleSize)
        val sampledHeight = ceilDivide(height, sampleSize)
        return maxOf(sampledWidth, sampledHeight) > MAX_LONG_EDGE ||
            sampledWidth.toLong() * sampledHeight > MAX_PIXEL_COUNT
    }

    private fun ceilDivide(value: Int, divisor: Int): Int =
        ((value.toLong() + divisor - 1) / divisor).toInt()
}
