package org.synapseworks.pageharbor.document

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import org.synapseworks.pageharbor.image.ArgbImage
import org.synapseworks.pageharbor.image.DocumentFilter
import org.synapseworks.pageharbor.image.DocumentImageFilterEngine

private const val FilteredJpegQuality = 100

/**
 * Decodes one source at its full encoded resolution, applies one non-destructive filter, and
 * writes a new JPEG. It processes one page at a time; no preview bitmap is used or retained.
 */
fun writeFilteredJpegToDestination(
    openSource: () -> InputStream?,
    destination: OutputStream?,
    filter: DocumentFilter,
): PageExportResult {
    if (destination == null) return PageExportResult.DestinationUnavailable
    if (filter == DocumentFilter.ORIGINAL) {
        destination.closeSafely()
        return PageExportResult.WriteFailed
    }

    var decoded: Bitmap? = null
    var oriented: Bitmap? = null
    var filtered: Bitmap? = null
    return try {
        val orientation = readOrientation(openSource)
        decoded = decodeFullResolution(openSource) ?: run {
            destination.closeSafely()
            return PageExportResult.SourceMissing
        }
        oriented = orient(decoded, orientation)
        filtered = filterBitmap(oriented, filter)
        destination.use { output ->
            if (filtered.compress(Bitmap.CompressFormat.JPEG, FilteredJpegQuality, output)) {
                output.flush()
                PageExportResult.Success
            } else {
                PageExportResult.WriteFailed
            }
        }
    } catch (_: IOException) {
        destination.closeSafely()
        PageExportResult.WriteFailed
    } catch (_: SecurityException) {
        destination.closeSafely()
        PageExportResult.WriteFailed
    } catch (_: IllegalArgumentException) {
        destination.closeSafely()
        PageExportResult.WriteFailed
    } catch (_: IllegalStateException) {
        destination.closeSafely()
        PageExportResult.WriteFailed
    } finally {
        filtered?.takeIf { it !== oriented }?.recycle()
        oriented?.takeIf { it !== decoded }?.recycle()
        decoded?.recycle()
    }
}

/** Exposed for JVM tests to prove the export path delegates to the shared Slice 1 engine. */
internal fun applyExportFilter(source: ArgbImage, filter: DocumentFilter): ArgbImage =
    DocumentImageFilterEngine.apply(source, filter)

private fun readOrientation(openSource: () -> InputStream?): Int = runCatching {
    openSource()?.use { source ->
        ExifInterface(source).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    } ?: ExifInterface.ORIENTATION_NORMAL
}.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

private fun decodeFullResolution(openSource: () -> InputStream?): Bitmap? =
    openSource()?.use { source ->
        BitmapFactory.decodeStream(
            source,
            null,
            BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        )
    }

private fun orient(source: Bitmap, orientation: Int): Bitmap {
    val matrix = when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Matrix().apply { setScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_180 -> Matrix().apply { setRotate(180f) }
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> Matrix().apply { setScale(1f, -1f) }
        ExifInterface.ORIENTATION_TRANSPOSE -> Matrix().apply {
            setRotate(90f)
            postScale(-1f, 1f)
        }

        ExifInterface.ORIENTATION_ROTATE_90 -> Matrix().apply { setRotate(90f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> Matrix().apply {
            setRotate(-90f)
            postScale(-1f, 1f)
        }

        ExifInterface.ORIENTATION_ROTATE_270 -> Matrix().apply { setRotate(-90f) }
        else -> return source
    }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

private fun filterBitmap(source: Bitmap, filter: DocumentFilter): Bitmap {
    val pixels = IntArray(source.width * source.height)
    source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
    val output = applyExportFilter(ArgbImage(source.width, source.height, pixels), filter)
    return Bitmap.createBitmap(output.pixels, output.width, output.height, Bitmap.Config.ARGB_8888)
}

private fun OutputStream.closeSafely() {
    try {
        close()
    } catch (_: IOException) {
        // Nothing user-actionable, and document details must not be logged.
    }
}
