package org.synapseworks.pageharbor.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DocumentPreviewDecodePolicyTest {
    @Test
    fun keepsSmallPreviewAtFullSize() {
        assertEquals(1, DocumentPreviewDecodePolicy.calculateInSampleSize(1_200, 900))
    }

    @Test
    fun boundsLargePreviewWithPowerOfTwoSampling() {
        assertEquals(2, DocumentPreviewDecodePolicy.calculateInSampleSize(3_200, 2_400))
    }

    @Test
    fun rejectsInvalidPreviewDimensions() {
        assertNull(DocumentPreviewDecodePolicy.calculateInSampleSize(0, 1_000))
    }
}
