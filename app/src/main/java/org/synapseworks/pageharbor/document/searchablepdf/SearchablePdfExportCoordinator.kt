package org.synapseworks.pageharbor.document.searchablepdf

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.synapseworks.pageharbor.document.classification.DocumentClassifier
import org.synapseworks.pageharbor.document.filename.FilenameSuggestion
import org.synapseworks.pageharbor.document.filename.FilenameSuggestionEngine
import org.synapseworks.pageharbor.ocr.OcrEngine
import org.synapseworks.pageharbor.ocr.OcrPage
import org.synapseworks.pageharbor.ocr.OcrResult

/** Coordinates local OCR, searchable-PDF preparation, and a caller-selected SAF destination. */
interface SearchablePdfExportCoordinator {
    suspend fun prepare(request: SearchablePdfExportRequest): SearchablePdfPreparedExport

    suspend fun writePreparedExport(
        preparedExport: SearchablePdfPreparedExport,
        destinationUri: Uri,
    ): SearchablePdfExportResult

    /** Deletes an unused prepared PDF, for example when the user cancels destination selection. */
    fun discardPreparedExport(preparedExport: SearchablePdfPreparedExport)
}

/** Ordered scanner JPEG sources, optional existing OCR, and UI-agnostic progress notifications. */
data class SearchablePdfExportRequest(
    val pageUris: List<Uri>,
    val ocrResult: OcrResult? = null,
    val progressListener: SearchablePdfExportProgressListener = SearchablePdfExportProgressListener {},
)

fun interface SearchablePdfExportProgressListener {
    fun onProgress(progress: SearchablePdfExportProgress)
}

sealed interface SearchablePdfExportProgress {
    data object Recognizing : SearchablePdfExportProgress
    data object Generating : SearchablePdfExportProgress
    data object Writing : SearchablePdfExportProgress
}

sealed interface SearchablePdfPreparedExport {
    class Ready internal constructor(
        internal val temporaryFile: File,
        internal val progressListener: SearchablePdfExportProgressListener,
        val pageCount: Int,
        val textLayerPageCount: Int,
        /** Safe, category-only title for the immediately following user-controlled SAF save. */
        val filenameSuggestion: FilenameSuggestion,
    ) : SearchablePdfPreparedExport

    data class Failure(val reason: SearchablePdfPreparationError) : SearchablePdfPreparedExport
}

enum class SearchablePdfPreparationError {
    NO_PAGES,
    OCR_FAILED,
    OCR_RESULT_MISMATCH,
    TEMPORARY_STORAGE_UNAVAILABLE,
    GENERATION_FAILED,
}

sealed interface SearchablePdfExportResult {
    data object Success : SearchablePdfExportResult
    data class Failure(val reason: SearchablePdfExportError) : SearchablePdfExportResult
}

enum class SearchablePdfExportError {
    PREPARED_EXPORT_UNAVAILABLE,
    DESTINATION_UNAVAILABLE,
    WRITE_FAILED,
}

/**
 * Local implementation. It keeps generated PDFs in private cache only until they are copied to a
 * destination selected through SAF, discarded, or cancelled.
 */
class LocalSearchablePdfExportCoordinator(
    context: Context,
    private val ocrEngine: OcrEngine,
    private val generator: SearchablePdfGenerator = PdfBoxSearchablePdfGenerator(context),
    private val documentClassifier: DocumentClassifier = DocumentClassifier(),
    private val filenameSuggestionEngine: FilenameSuggestionEngine = FilenameSuggestionEngine(),
    /** Kept injectable only for deterministic provider-boundary tests; production uses SAF. */
    private val openDestinationOutputStream: (Uri) -> OutputStream? = { uri ->
        context.contentResolver.openOutputStream(uri)
    },
    /** Kept injectable only for deterministic private-cache cleanup tests. */
    private val deleteTemporaryFile: (File) -> Boolean = File::delete,
) : SearchablePdfExportCoordinator {
    private val applicationContext: Context = context.applicationContext ?: context

    override suspend fun prepare(request: SearchablePdfExportRequest): SearchablePdfPreparedExport {
        if (request.pageUris.isEmpty()) {
            return SearchablePdfPreparedExport.Failure(SearchablePdfPreparationError.NO_PAGES)
        }

        val ocrResult = request.ocrResult ?: recognize(request)
            ?: return SearchablePdfPreparedExport.Failure(SearchablePdfPreparationError.OCR_FAILED)
        val orderedOcrPages = orderOcrPages(ocrResult, request.pageUris.size)
            ?: return SearchablePdfPreparedExport.Failure(SearchablePdfPreparationError.OCR_RESULT_MISMATCH)
        val filenameSuggestion = filenameSuggestionEngine.suggest(
            documentClassifier.classify(ocrResult.plainText).category,
        )
        val temporaryFile = createTemporaryPdf()
            ?: return SearchablePdfPreparedExport.Failure(
                SearchablePdfPreparationError.TEMPORARY_STORAGE_UNAVAILABLE,
            )

        return try {
            coroutineContext.ensureActive()
            reportProgress(request.progressListener, SearchablePdfExportProgress.Generating)
            when (
                val generated = generator.generate(
                    SearchablePdfRequest(
                        pages = request.pageUris.mapIndexed { index, uri ->
                            SearchablePdfPage(
                                openJpegStream = {
                                    applicationContext.contentResolver.openInputStream(uri)
                                        ?: throw FileNotFoundException()
                                },
                                ocrResult = orderedOcrPages[index],
                            )
                        },
                        outputFile = temporaryFile,
                    ),
                )
            ) {
                is SearchablePdfGenerationResult.Success -> {
                    if (!temporaryFile.isFile || temporaryFile.length() == 0L) {
                        deleteTemporaryPdf(temporaryFile)
                        SearchablePdfPreparedExport.Failure(
                            SearchablePdfPreparationError.GENERATION_FAILED,
                        )
                    } else {
                        SearchablePdfPreparedExport.Ready(
                            temporaryFile = temporaryFile,
                            progressListener = request.progressListener,
                            pageCount = generated.pageCount,
                            textLayerPageCount = generated.textLayerPageCount,
                            filenameSuggestion = filenameSuggestion,
                        )
                    }
                }

                is SearchablePdfGenerationResult.Failure -> {
                    deleteTemporaryPdf(temporaryFile)
                    SearchablePdfPreparedExport.Failure(SearchablePdfPreparationError.GENERATION_FAILED)
                }
            }
        } catch (error: CancellationException) {
            deleteTemporaryPdf(temporaryFile)
            throw error
        } catch (_: Exception) {
            deleteTemporaryPdf(temporaryFile)
            SearchablePdfPreparedExport.Failure(SearchablePdfPreparationError.GENERATION_FAILED)
        }
    }

    override suspend fun writePreparedExport(
        preparedExport: SearchablePdfPreparedExport,
        destinationUri: Uri,
    ): SearchablePdfExportResult = withContext(Dispatchers.IO) {
        val ready = preparedExport as? SearchablePdfPreparedExport.Ready
            ?: return@withContext SearchablePdfExportResult.Failure(
                SearchablePdfExportError.PREPARED_EXPORT_UNAVAILABLE,
            )
        if (!ready.temporaryFile.isFile) {
            deleteTemporaryPdf(ready.temporaryFile)
            return@withContext SearchablePdfExportResult.Failure(
                SearchablePdfExportError.PREPARED_EXPORT_UNAVAILABLE,
            )
        }

        try {
            coroutineContext.ensureActive()
            reportProgress(ready.progressListener, SearchablePdfExportProgress.Writing)
            val destination = try {
                openDestinationOutputStream(destinationUri)
            } catch (_: FileNotFoundException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: SecurityException) {
                null
            } catch (_: IOException) {
                null
            } catch (_: RuntimeException) {
                null
            }
            if (destination == null) {
                return@withContext SearchablePdfExportResult.Failure(
                    SearchablePdfExportError.DESTINATION_UNAVAILABLE,
                )
            }

            ready.temporaryFile.inputStream().use { input ->
                destination.use { output ->
                    val buffer = ByteArray(CopyBufferSize)
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                    output.flush()
                }
            }
            SearchablePdfExportResult.Success
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            SearchablePdfExportResult.Failure(SearchablePdfExportError.WRITE_FAILED)
        } catch (_: SecurityException) {
            SearchablePdfExportResult.Failure(SearchablePdfExportError.WRITE_FAILED)
        } catch (_: RuntimeException) {
            SearchablePdfExportResult.Failure(SearchablePdfExportError.WRITE_FAILED)
        } finally {
            deleteTemporaryPdf(ready.temporaryFile)
        }
    }

    override fun discardPreparedExport(preparedExport: SearchablePdfPreparedExport) {
        (preparedExport as? SearchablePdfPreparedExport.Ready)?.let { ready ->
            deleteTemporaryPdf(ready.temporaryFile)
        }
    }

    private suspend fun recognize(request: SearchablePdfExportRequest): OcrResult? {
        reportProgress(request.progressListener, SearchablePdfExportProgress.Recognizing)
        return try {
            withContext(Dispatchers.IO) {
                coroutineContext.ensureActive()
                ocrEngine.recognize(
                    request.pageUris.map { uri ->
                        OcrPage {
                            applicationContext.contentResolver.openInputStream(uri)
                                ?: throw FileNotFoundException()
                        }
                    },
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }

    private fun orderOcrPages(ocrResult: OcrResult, pageCount: Int) =
        ocrResult.pages
            .takeIf { pages -> pages.size == pageCount }
            ?.sortedBy { page -> page.pageIndex }
            ?.takeIf { pages -> pages.indices.all { index -> pages[index].pageIndex == index } }

    private fun createTemporaryPdf(): File? {
        val directory = File(applicationContext.cacheDir, TemporaryPdfDirectory)
        return try {
            if (!directory.exists() && !directory.mkdirs()) return null
            File.createTempFile(TemporaryPdfPrefix, ".pdf", directory)
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    private fun reportProgress(
        listener: SearchablePdfExportProgressListener,
        progress: SearchablePdfExportProgress,
    ) {
        try {
            listener.onProgress(progress)
        } catch (_: RuntimeException) {
            // Progress observation cannot change document handling or expose sensitive details.
        }
    }

    private fun deleteTemporaryPdf(file: File) {
        if (file.isFile) {
            try {
                deleteTemporaryFile(file)
            } catch (_: SecurityException) {
                // Private-cache cleanup is best-effort and must not expose document details.
            }
        }
    }

    private companion object {
        const val TemporaryPdfDirectory = "searchable-pdfs"
        const val TemporaryPdfPrefix = "searchable-"
        const val CopyBufferSize = 8 * 1024
    }
}
