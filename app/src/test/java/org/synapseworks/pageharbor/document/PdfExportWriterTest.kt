package org.synapseworks.pageharbor.document

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfExportWriterTest {
    @Test
    fun copyPdfToDestinationCopiesBytes() {
        val sourceBytes = byteArrayOf(1, 2, 3, 4)
        val destination = ByteArrayOutputStream()

        val result = copyPdfToDestination(
            source = ByteArrayInputStream(sourceBytes),
            destination = destination,
        )

        assertEquals(PdfExportResult.Success, result)
        assertArrayEquals(sourceBytes, destination.toByteArray())
    }

    @Test
    fun copyPdfToDestinationHandlesMissingSource() {
        val result = copyPdfToDestination(
            source = null,
            destination = ByteArrayOutputStream(),
        )

        assertEquals(PdfExportResult.SourceMissing, result)
    }

    @Test
    fun copyPdfToDestinationHandlesUnavailableDestination() {
        val result = copyPdfToDestination(
            source = ByteArrayInputStream(byteArrayOf(1)),
            destination = null,
        )

        assertEquals(PdfExportResult.DestinationUnavailable, result)
    }

    @Test
    fun copyPdfToDestinationHandlesIoFailure() {
        val result = copyPdfToDestination(
            source = ByteArrayInputStream(byteArrayOf(1)),
            destination = FailingOutputStream(),
        )

        assertEquals(PdfExportResult.WriteFailed, result)
    }
}

private class FailingOutputStream : OutputStream() {
    override fun write(b: Int) {
        throw IOException("write failed")
    }
}
