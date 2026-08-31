package org.synapseworks.pageharbor.document

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileNotFoundException
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.synapseworks.pageharbor.document.searchablepdf.PdfBoxSearchablePdfGenerator
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfGenerationResult
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfPage
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfRequest
import org.synapseworks.pageharbor.image.DocumentFilter

private const val NormalPdfDirectory = "normal-pdfs"
private const val NormalPdfPrefix = "normal-"
private const val NormalPdfVisualPrefix = "normal-visual-"
private const val NormalPdfMaxAgeMillis = 24L * 60L * 60L * 1000L

sealed interface NormalPdfRecompositionResult {
    data class Ready(val file: File) : NormalPdfRecompositionResult
    data object SourceMissing : NormalPdfRecompositionResult
    data object Failed : NormalPdfRecompositionResult
}

/**
 * Produces one private, image-only PDF from the current effective page visuals. Every source is
 * handled sequentially; filtered temporary JPEGs delete themselves when PdfBox closes each stream.
 */
suspend fun recomposeNormalPdf(
    context: Context,
    pages: List<NormalPdfPage>,
): NormalPdfRecompositionResult = withContext(Dispatchers.IO) {
    if (pages.isEmpty() || pages.any { it.sourceUri == null }) {
        return@withContext NormalPdfRecompositionResult.SourceMissing
    }
    val outputFile = createTemporaryNormalPdf(context.cacheDir)
        ?: return@withContext NormalPdfRecompositionResult.Failed
    val streamProvider = NormalPdfVisualStreamProvider(context)

    try {
        when (
            PdfBoxSearchablePdfGenerator(context).generate(
                SearchablePdfRequest(
                    pages = pages.map { page ->
                        SearchablePdfPage(
                            openJpegStream = { streamProvider.open(page) },
                            // This is deliberately a normal PDF, with no OCR/searchable layer.
                            ocrResult = null,
                        )
                    },
                    outputFile = outputFile,
                ),
            )
        ) {
            is SearchablePdfGenerationResult.Success -> {
                if (outputFile.isFile && outputFile.length() > 0L) {
                    NormalPdfRecompositionResult.Ready(outputFile)
                } else {
                    outputFile.deleteSafely()
                    NormalPdfRecompositionResult.Failed
                }
            }

            is SearchablePdfGenerationResult.Failure -> {
                outputFile.deleteSafely()
                NormalPdfRecompositionResult.Failed
            }
        }
    } catch (error: CancellationException) {
        outputFile.deleteSafely()
        throw error
    } catch (_: Exception) {
        outputFile.deleteSafely()
        NormalPdfRecompositionResult.Failed
    }
}

fun deleteNormalPdfRecomposition(file: File) {
    file.deleteSafely()
}

fun deleteStaleNormalPdfs(
    cacheDirectory: File,
    nowMillis: Long = System.currentTimeMillis(),
) {
    File(cacheDirectory, NormalPdfDirectory).listFiles()?.forEach { file ->
        if (
            file.isFile &&
            (
                (file.name.startsWith(NormalPdfPrefix) && file.name.endsWith(".pdf")) ||
                    (file.name.startsWith(NormalPdfVisualPrefix) && file.name.endsWith(".jpg"))
            ) &&
            nowMillis - file.lastModified() >= NormalPdfMaxAgeMillis
        ) {
            file.deleteSafely()
        }
    }
}

private class NormalPdfVisualStreamProvider(private val context: Context) {
    fun open(page: NormalPdfPage): InputStream = when (page.filter) {
        DocumentFilter.ORIGINAL -> openOriginal(page.sourceUri)
        else -> openFiltered(page.sourceUri, page.filter)
    }

    private fun openOriginal(sourceUri: Uri?): InputStream =
        sourceUri?.let { context.contentResolver.openInputStream(it) } ?: throw FileNotFoundException()

    private fun openFiltered(sourceUri: Uri?, filter: DocumentFilter): InputStream {
        val uri = sourceUri ?: throw FileNotFoundException()
        val temporaryImage = createTemporaryNormalVisual(context.cacheDir) ?: throw FileNotFoundException()
        val destination = try {
            temporaryImage.outputStream()
        } catch (_: IOException) {
            temporaryImage.deleteSafely()
            throw FileNotFoundException()
        } catch (_: SecurityException) {
            temporaryImage.deleteSafely()
            throw FileNotFoundException()
        }
        val result = writeFilteredJpegToDestination(
            openSource = { context.contentResolver.openInputStream(uri) },
            destination = destination,
            filter = filter,
        )
        if (result != PageExportResult.Success) {
            temporaryImage.deleteSafely()
            throw FileNotFoundException()
        }
        return try {
            DeleteOnCloseInputStream(temporaryImage.inputStream(), temporaryImage)
        } catch (_: IOException) {
            temporaryImage.deleteSafely()
            throw FileNotFoundException()
        }
    }
}

private class DeleteOnCloseInputStream(
    input: InputStream,
    private val temporaryImage: File,
) : FilterInputStream(input) {
    override fun close() {
        try {
            super.close()
        } finally {
            temporaryImage.deleteSafely()
        }
    }
}

private fun createTemporaryNormalPdf(cacheDirectory: File): File? =
    createTemporaryFile(cacheDirectory, NormalPdfPrefix, ".pdf")

private fun createTemporaryNormalVisual(cacheDirectory: File): File? =
    createTemporaryFile(cacheDirectory, NormalPdfVisualPrefix, ".jpg")

private fun createTemporaryFile(cacheDirectory: File, prefix: String, suffix: String): File? {
    val directory = File(cacheDirectory, NormalPdfDirectory)
    return try {
        if (!directory.exists() && !directory.mkdirs()) return null
        File.createTempFile(prefix, suffix, directory)
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }
}

private fun File.deleteSafely() {
    try {
        delete()
    } catch (_: SecurityException) {
        // Private-cache cleanup is best-effort and must not expose document details.
    }
}
