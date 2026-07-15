package org.synapseworks.pageharbor.document

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PageExportWriterTest {
    @Test
    fun copyPageToDestinationCopiesBytesWithoutReencoding() {
        val sourceBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 0xFF.toByte(), 0xD9.toByte())
        val destination = ByteArrayOutputStream()

        val result = copyPageToDestination(
            source = ByteArrayInputStream(sourceBytes),
            destination = destination,
        )

        assertEquals(PageExportResult.Success, result)
        assertArrayEquals(sourceBytes, destination.toByteArray())
    }

    @Test
    fun copyPageToDestinationHandlesMissingSource() {
        val destination = CloseTrackingOutputStream()

        val result = copyPageToDestination(
            source = null,
            destination = destination,
        )

        assertEquals(PageExportResult.SourceMissing, result)
        assertEquals(true, destination.wasClosed)
    }

    @Test
    fun copyPageToDestinationHandlesUnavailableDestination() {
        val result = copyPageToDestination(
            source = ByteArrayInputStream(byteArrayOf(1)),
            destination = null,
        )

        assertEquals(PageExportResult.DestinationUnavailable, result)
    }

    @Test
    fun copyPageToDestinationHandlesIoFailure() {
        val result = copyPageToDestination(
            source = ByteArrayInputStream(byteArrayOf(1)),
            destination = FailingPageOutputStream(),
        )

        assertEquals(PageExportResult.WriteFailed, result)
    }
}

private class FailingPageOutputStream : OutputStream() {
    override fun write(b: Int) {
        throw IOException("write failed")
    }
}

private class CloseTrackingOutputStream : ByteArrayOutputStream() {
    var wasClosed = false
        private set

    override fun close() {
        wasClosed = true
        super.close()
    }
}
