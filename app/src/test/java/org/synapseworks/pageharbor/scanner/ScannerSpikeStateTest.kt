package org.synapseworks.pageharbor.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerSpikeStateTest {
    @Test
    fun createScannerResultSummaryKeepsPdfAvailabilityAndPageCount() {
        val summary = createScannerResultSummary(jpegPageCount = 2, pdfPageCount = 2)

        assertEquals(2, summary.jpegPageCount)
        assertTrue(summary.hasPdf)
        assertEquals(2, summary.pdfPageCount)
    }

    @Test
    fun createScannerResultSummaryHandlesMissingPdf() {
        val summary = createScannerResultSummary(jpegPageCount = 1, pdfPageCount = null)

        assertEquals(1, summary.jpegPageCount)
        assertFalse(summary.hasPdf)
        assertNull(summary.pdfPageCount)
    }

    @Test
    fun createScannerResultSummaryCoercesNegativeJpegCountToZero() {
        val summary = createScannerResultSummary(jpegPageCount = -1, pdfPageCount = null)

        assertEquals(0, summary.jpegPageCount)
    }
}
