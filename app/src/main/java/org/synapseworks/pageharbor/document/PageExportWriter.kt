package org.synapseworks.pageharbor.document

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

private const val PageCopyBufferSize = 8 * 1024

sealed interface PageExportResult {
    data object Success : PageExportResult
    data object SourceMissing : PageExportResult
    data object DestinationUnavailable : PageExportResult
    data object WriteFailed : PageExportResult
}

fun copyPageToDestination(
    source: InputStream?,
    destination: OutputStream?,
): PageExportResult {
    if (source == null) {
        destination?.closeSafely()
        return PageExportResult.SourceMissing
    }
    if (destination == null) {
        source.closeSafely()
        return PageExportResult.DestinationUnavailable
    }

    return try {
        source.use { input ->
            destination.use { output ->
                input.copyTo(output, bufferSize = PageCopyBufferSize)
                output.flush()
            }
        }
        PageExportResult.Success
    } catch (_: IOException) {
        PageExportResult.WriteFailed
    } catch (_: SecurityException) {
        PageExportResult.WriteFailed
    }
}

private fun InputStream.closeSafely() {
    try {
        close()
    } catch (_: IOException) {
        // Nothing user-actionable, and document details must not be logged.
    }
}

private fun OutputStream.closeSafely() {
    try {
        close()
    } catch (_: IOException) {
        // Nothing user-actionable, and document details must not be logged.
    }
}
