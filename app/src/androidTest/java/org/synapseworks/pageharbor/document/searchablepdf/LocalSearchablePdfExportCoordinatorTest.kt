package org.synapseworks.pageharbor.document.searchablepdf

import android.content.Context
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.synapseworks.pageharbor.ocr.OcrEngine
import org.synapseworks.pageharbor.ocr.OcrPage
import org.synapseworks.pageharbor.ocr.OcrPageLayout
import org.synapseworks.pageharbor.ocr.OcrPageResult
import org.synapseworks.pageharbor.ocr.OcrResult
import org.synapseworks.pageharbor.ocr.OcrTextBounds
import org.synapseworks.pageharbor.ocr.OcrTextLine

class LocalSearchablePdfExportCoordinatorTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun runsOcrGeneratesPdfWritesContentDestinationAndDeletesTemporaryFile() = runBlocking {
        val source = sharedFile("source.jpg")
        val destination = sharedFile("destination.pdf")
        val progress = mutableListOf<SearchablePdfExportProgress>()
        writeJpeg(source)
        val originalImageBytes = source.readBytes()
        val ocrEngine = RecordingOcrEngine(pageResult())
        val coordinator = LocalSearchablePdfExportCoordinator(
            context = context,
            ocrEngine = ocrEngine,
        )

        try {
            val prepared = coordinator.prepare(
                SearchablePdfExportRequest(
                    pageUris = listOf(fileUri(source)),
                    progressListener = SearchablePdfExportProgressListener(progress::add),
                ),
            )

            assertTrue(prepared is SearchablePdfPreparedExport.Ready)
            prepared as SearchablePdfPreparedExport.Ready
            assertEquals(1, ocrEngine.requestCount)
            assertTrue(prepared.temporaryFile.exists())
            assertEquals(
                listOf(
                    SearchablePdfExportProgress.Recognizing,
                    SearchablePdfExportProgress.Generating,
                ),
                progress,
            )

            val result = coordinator.writePreparedExport(prepared, fileUri(destination))

            assertEquals(SearchablePdfExportResult.Success, result)
            assertFalse(prepared.temporaryFile.exists())
            assertArrayEquals(originalImageBytes, source.readBytes())
            assertEquals(
                listOf(
                    SearchablePdfExportProgress.Recognizing,
                    SearchablePdfExportProgress.Generating,
                    SearchablePdfExportProgress.Writing,
                ),
                progress,
            )
            PDDocument.load(destination).use { document ->
                assertEquals(1, document.numberOfPages)
                assertEquals("English: searchable text", PDFTextStripper().getText(document).trim())
            }
        } finally {
            source.delete()
            destination.delete()
        }
    }

    @Test
    fun usesSuppliedOcrAndDiscardDeletesUnusedPreparedPdf() = runBlocking {
        val source = sharedFile("supplied-source.jpg").apply { writeText("jpeg") }
        val generator = RecordingGenerator()
        val coordinator = LocalSearchablePdfExportCoordinator(
            context = context,
            ocrEngine = FailingOcrEngine,
            generator = generator,
        )

        try {
            val prepared = coordinator.prepare(
                SearchablePdfExportRequest(
                    pageUris = listOf(fileUri(source)),
                    ocrResult = OcrResult(listOf(pageResult())),
                ),
            )

            assertTrue(prepared is SearchablePdfPreparedExport.Ready)
            prepared as SearchablePdfPreparedExport.Ready
            assertEquals(1, generator.requestCount)
            assertTrue(prepared.temporaryFile.exists())

            coordinator.discardPreparedExport(prepared)

            assertFalse(prepared.temporaryFile.exists())
        } finally {
            source.delete()
        }
    }

    @Test
    fun preparesCategoryBasedFilenameSuggestions() = runBlocking {
        val source = sharedFile("filename-source.jpg").apply { writeText("jpeg") }

        try {
            assertPreparedFilename(source, "Invoice\nVAT", "invoice.pdf")
            assertPreparedFilename(source, "Receipt\nSubtotal", "receipt.pdf")
            assertPreparedFilename(source, "Dear reader\nRegards", "letter.pdf")
            assertPreparedFilename(source, "Application form\nPlease complete", "form.pdf")
            assertPreparedFilename(source, "Unrelated scan text", "document.pdf")
            assertPreparedFilename(source, "", "document.pdf")
        } finally {
            source.delete()
        }
    }

    @Test
    fun createDocumentReceivesSuggestionAndReturnsUserEditedDestinationUri() {
        val contract = ActivityResultContracts.CreateDocument("application/pdf")
        val intent = contract.createIntent(context, "invoice.pdf")
        val userEditedDestination = Uri.parse(
            "content://org.synapseworks.pageharbor.test/user-edited-name.pdf",
        )

        assertEquals(Intent.ACTION_CREATE_DOCUMENT, intent.action)
        assertEquals("invoice.pdf", intent.getStringExtra(Intent.EXTRA_TITLE))
        assertEquals(
            userEditedDestination,
            contract.parseResult(Activity.RESULT_OK, Intent().setData(userEditedDestination)),
        )
    }

    @Test
    fun deletesTemporaryPdfWhenDestinationCannotBeOpened() = runBlocking {
        val source = sharedFile("destination-failure-source.jpg").apply { writeText("jpeg") }
        val generator = RecordingGenerator()
        val coordinator = LocalSearchablePdfExportCoordinator(
            context = context,
            ocrEngine = FailingOcrEngine,
            generator = generator,
        )

        try {
            val prepared = coordinator.prepare(
                SearchablePdfExportRequest(
                    pageUris = listOf(fileUri(source)),
                    ocrResult = OcrResult(listOf(pageResult())),
                ),
            ) as SearchablePdfPreparedExport.Ready

            val result = coordinator.writePreparedExport(
                prepared,
                android.net.Uri.parse("content://missing.destination/searchable.pdf"),
            )

            assertEquals(
                SearchablePdfExportResult.Failure(
                    SearchablePdfExportError.DESTINATION_UNAVAILABLE,
                ),
                result,
            )
            assertFalse(prepared.temporaryFile.exists())
        } finally {
            source.delete()
        }
    }

    @Test
    fun deletesTemporaryPdfWhenGenerationIsCancelled() = runBlocking {
        val source = sharedFile("cancelled-source.jpg").apply { writeText("jpeg") }
        val generator = CancellingGenerator()
        val coordinator = LocalSearchablePdfExportCoordinator(
            context = context,
            ocrEngine = FailingOcrEngine,
            generator = generator,
        )

        try {
            val failure = runCatching {
                coordinator.prepare(
                    SearchablePdfExportRequest(
                        pageUris = listOf(fileUri(source)),
                        ocrResult = OcrResult(listOf(pageResult())),
                    ),
                )
            }.exceptionOrNull()

            assertTrue(failure is CancellationException)
            assertTrue(generator.outputFile != null)
            assertFalse(generator.outputFile!!.exists())
        } finally {
            source.delete()
        }
    }

    private suspend fun assertPreparedFilename(source: File, text: String, expectedFilename: String) {
        val coordinator = LocalSearchablePdfExportCoordinator(
            context = context,
            ocrEngine = FailingOcrEngine,
            generator = RecordingGenerator(),
        )
        val prepared = coordinator.prepare(
            SearchablePdfExportRequest(
                pageUris = listOf(fileUri(source)),
                ocrResult = OcrResult(listOf(pageResult(text = text))),
            ),
        ) as SearchablePdfPreparedExport.Ready

        try {
            assertEquals(expectedFilename, prepared.filenameSuggestion.filename)
        } finally {
            coordinator.discardPreparedExport(prepared)
        }
    }

    private fun pageResult(text: String = "English: searchable text"): OcrPageResult = OcrPageResult(
        pageIndex = 0,
        text = text,
        layout = OcrPageLayout(
            imageWidthPx = 240,
            imageHeightPx = 320,
            lines = listOf(
                OcrTextLine(
                    text = text,
                    bounds = OcrTextBounds(left = 16f, top = 20f, right = 224f, bottom = 48f),
                ),
            ),
        ),
    )

    private fun writeJpeg(file: File) {
        val bitmap = Bitmap.createBitmap(240, 320, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.rgb(220, 235, 250))
            FileOutputStream(file).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output))
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun sharedFile(name: String): File {
        val directory = File(context.cacheDir, "shared-pdfs").apply { mkdirs() }
        return File(directory, "searchable-coordinator-$name")
    }

    private fun fileUri(file: File) = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )

    private class RecordingOcrEngine(private val result: OcrPageResult) : OcrEngine {
        var requestCount = 0

        override fun recognize(pages: List<OcrPage>): OcrResult {
            requestCount++
            return OcrResult(listOf(result))
        }
    }

    private class RecordingGenerator : SearchablePdfGenerator {
        var requestCount = 0

        override suspend fun generate(request: SearchablePdfRequest): SearchablePdfGenerationResult {
            requestCount++
            request.outputFile.writeText("prepared searchable pdf")
            return SearchablePdfGenerationResult.Success(pageCount = 1, textLayerPageCount = 1)
        }
    }

    private class CancellingGenerator : SearchablePdfGenerator {
        var outputFile: File? = null

        override suspend fun generate(request: SearchablePdfRequest): SearchablePdfGenerationResult {
            outputFile = request.outputFile
            request.outputFile.writeText("partial")
            throw CancellationException()
        }
    }

    private data object FailingOcrEngine : OcrEngine {
        override fun recognize(pages: List<OcrPage>): OcrResult = error("OCR must not run")
    }
}
