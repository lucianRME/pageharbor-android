package org.synapseworks.pageharbor.ocr

/**
 * Memory bounds for one OCR page bitmap.
 *
 * A 2,800 px long edge preserves legible document text while a 7 MP cap keeps an ARGB_8888
 * bitmap near 28 MB. JPEG decoding uses power-of-two sampling, so pages just over a bound can be
 * reduced more than the exact threshold; this is intentional to keep allocation predictable.
 */
internal object OcrBitmapDecodePolicy {
    const val MAX_DECODED_LONG_EDGE = 2_800
    const val MAX_DECODED_PIXEL_COUNT = 7_000_000L

    /** Returns a power-of-two sample size, or null when the decoded dimensions are invalid. */
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
        val sampledLongEdge = maxOf(sampledWidth, sampledHeight)
        val sampledPixelCount = sampledWidth.toLong() * sampledHeight.toLong()

        return sampledLongEdge > MAX_DECODED_LONG_EDGE ||
            sampledPixelCount > MAX_DECODED_PIXEL_COUNT
    }

    private fun ceilDivide(value: Int, divisor: Int): Int =
        ((value.toLong() + divisor - 1) / divisor).toInt()
}
