package org.synapseworks.pageharbor.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrBitmapDecodePolicyTest {
    @Test
    fun imageBelowBoundsUsesNoDownsampling() {
        assertEquals(1, OcrBitmapDecodePolicy.calculateInSampleSize(2_000, 1_500))
    }

    @Test
    fun veryWideImageIsSampled() {
        assertEquals(2, OcrBitmapDecodePolicy.calculateInSampleSize(5_600, 1_000))
    }

    @Test
    fun veryTallImageIsSampled() {
        assertEquals(2, OcrBitmapDecodePolicy.calculateInSampleSize(1_000, 5_600))
    }

    @Test
    fun highPixelImageIsSampledEvenWhenItsEdgesFit() {
        assertEquals(2, OcrBitmapDecodePolicy.calculateInSampleSize(2_700, 2_700))
    }

    @Test
    fun invalidDimensionsHaveNoDecodePolicy() {
        assertNull(OcrBitmapDecodePolicy.calculateInSampleSize(0, 1_000))
        assertNull(OcrBitmapDecodePolicy.calculateInSampleSize(1_000, 0))
        assertNull(OcrBitmapDecodePolicy.calculateInSampleSize(-1, 1_000))
    }

    @Test
    fun samplingIsAlwaysPositivePowerOfTwo() {
        val sampleSize = OcrBitmapDecodePolicy.calculateInSampleSize(64_000, 48_000)!!

        assertTrue(sampleSize > 0)
        assertEquals(0, sampleSize and (sampleSize - 1))
    }
}
