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
        val destination = CloseTrackingPdfOutputStream()

        val result = copyPdfToDestination(
            source = null,
            destination = destination,
        )

        assertEquals(PdfExportResult.SourceMissing, result)
        assertEquals(true, destination.wasClosed)
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

    @Test
    fun copyPdfToDestinationCopiesShortSourceReads() {
        val sourceBytes = byteArrayOf(1, 2, 3, 4, 5)
        val destination = ByteArrayOutputStream()

        val result = copyPdfToDestination(
            source = ShortReadInputStream(sourceBytes),
            destination = destination,
        )

        assertEquals(PdfExportResult.Success, result)
        assertArrayEquals(sourceBytes, destination.toByteArray())
    }
}

private class FailingOutputStream : OutputStream() {
    override fun write(b: Int) {
        throw IOException("write failed")
    }
}

private class CloseTrackingPdfOutputStream : ByteArrayOutputStream() {
    var wasClosed = false
        private set

    override fun close() {
        wasClosed = true
        super.close()
    }
}

private class ShortReadInputStream(private val bytes: ByteArray) : java.io.InputStream() {
    private var position = 0

    override fun read(): Int = if (position < bytes.size) bytes[position++].toInt() and 0xFF else -1

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (position >= bytes.size) return -1
        val count = minOf(2, length, bytes.size - position)
        bytes.copyInto(buffer, offset, position, position + count)
        position += count
        return count
    }
}
