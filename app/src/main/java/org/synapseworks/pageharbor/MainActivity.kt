package org.synapseworks.pageharbor

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.synapseworks.pageharbor.document.PageExportResult
import org.synapseworks.pageharbor.document.PageExportState
import org.synapseworks.pageharbor.document.PdfExportResult
import org.synapseworks.pageharbor.document.PdfSaveState
import org.synapseworks.pageharbor.document.PdfShareError
import org.synapseworks.pageharbor.document.PdfShareIntentResult
import org.synapseworks.pageharbor.document.PdfSharePreparationResult
import org.synapseworks.pageharbor.document.PdfShareState
import org.synapseworks.pageharbor.document.copyPageToDestination
import org.synapseworks.pageharbor.document.copyPdfToDestination
import org.synapseworks.pageharbor.document.createPdfShareIntent
import org.synapseworks.pageharbor.document.deleteStaleSharedPdfs
import org.synapseworks.pageharbor.document.pageExportStateAfterCancellation
import org.synapseworks.pageharbor.document.pageExportStateAfterSuccess
import org.synapseworks.pageharbor.document.preparePdfForSharing
import org.synapseworks.pageharbor.document.startPageExport
import org.synapseworks.pageharbor.document.searchablepdf.LocalSearchablePdfExportCoordinator
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfExportError
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfExportProgressListener
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfExportRequest
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfExportResult
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfPreparedExport
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfSaveError
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfSaveState
import org.synapseworks.pageharbor.document.searchablepdf.isInProgress
import org.synapseworks.pageharbor.document.searchablepdf.searchablePdfSaveStateForProgress
import org.synapseworks.pageharbor.scanner.ScannerSpikeState
import org.synapseworks.pageharbor.scanner.createScannerResultSummary
import org.synapseworks.pageharbor.ui.PageHarborApp
import org.synapseworks.pageharbor.ocr.MlKitOcrEngine
import org.synapseworks.pageharbor.ocr.OcrEngine
import org.synapseworks.pageharbor.ocr.OcrPage
import org.synapseworks.pageharbor.ocr.OcrUiError
import org.synapseworks.pageharbor.ocr.OcrUiState
import org.synapseworks.pageharbor.ocr.canStartOcr
import org.synapseworks.pageharbor.ocr.clearedOcrState
import org.synapseworks.pageharbor.ocr.ocrStateAfterResult

class MainActivity : ComponentActivity() {
    private var scannerSpikeState: ScannerSpikeState by mutableStateOf(ScannerSpikeState.Idle)
    private var pdfSaveState: PdfSaveState by mutableStateOf(PdfSaveState.Idle)
    private var pdfShareState: PdfShareState by mutableStateOf(PdfShareState.Idle)
    private var pageExportState: PageExportState by mutableStateOf(PageExportState.Idle)
    private var ocrUiState: OcrUiState by mutableStateOf(OcrUiState.Idle)
    private var searchablePdfSaveState: SearchablePdfSaveState by mutableStateOf(
        SearchablePdfSaveState.Idle,
    )
    private var scannedPdfUri: Uri? = null
    private var scannedPageUris: List<Uri> = emptyList()
    private val ocrEngine: OcrEngine = MlKitOcrEngine()
    private var ocrJob: Job? = null
    private val searchablePdfExportCoordinator by lazy {
        LocalSearchablePdfExportCoordinator(this, ocrEngine)
    }
    private var searchablePdfPreparedExport: SearchablePdfPreparedExport.Ready? = null
    private var searchablePdfExportJob: Job? = null

    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        clearRecognizedText()
        scannedPdfUri = null
        scannedPageUris = emptyList()
        pdfSaveState = PdfSaveState.Idle
        pdfShareState = PdfShareState.Idle
        pageExportState = PageExportState.Idle
        clearSearchablePdfSave()

        if (result.resultCode == Activity.RESULT_CANCELED) {
            scannerSpikeState = ScannerSpikeState.Cancelled
            return@registerForActivityResult
        }

        if (result.resultCode != Activity.RESULT_OK) {
            scannerSpikeState = ScannerSpikeState.Error
            return@registerForActivityResult
        }

        scannerSpikeState = runCatching {
            val scannerResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val jpegPageCount = scannerResult?.pages?.size ?: 0
            val pdfPageCount = scannerResult?.pdf?.pageCount
            if (scannerResult == null || (jpegPageCount == 0 && pdfPageCount == null)) {
                ScannerSpikeState.Error
            } else {
                scannedPdfUri = scannerResult.pdf?.uri
                scannedPageUris = scannerResult.pages.orEmpty().map { page -> page.imageUri }
                createScannerResultSummary(
                    jpegPageCount = jpegPageCount,
                    pdfPageCount = pdfPageCount,
                )
            }
        }.getOrDefault(ScannerSpikeState.Error)
    }

    private val createPdfDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { destinationUri ->
        if (destinationUri == null) {
            pdfSaveState = PdfSaveState.Idle
            return@registerForActivityResult
        }

        savePdfToDestination(destinationUri)
    }

    private val createPageDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("image/jpeg"),
    ) { destinationUri ->
        val currentState = pageExportState as? PageExportState.ChoosingDestination
            ?: return@registerForActivityResult

        if (destinationUri == null) {
            pageExportState = pageExportStateAfterCancellation(currentState.pageNumber)
            return@registerForActivityResult
        }

        exportPageToDestination(currentState, destinationUri)
    }

    private val createSearchablePdfDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { destinationUri ->
        val preparedExport = searchablePdfPreparedExport
        if (destinationUri == null) {
            preparedExport?.let(searchablePdfExportCoordinator::discardPreparedExport)
            searchablePdfPreparedExport = null
            searchablePdfSaveState = if (preparedExport == null) {
                SearchablePdfSaveState.Idle
            } else {
                SearchablePdfSaveState.Cancelled
            }
            return@registerForActivityResult
        }
        if (preparedExport == null) {
            searchablePdfSaveState = SearchablePdfSaveState.Error(
                SearchablePdfSaveError.PREPARATION_FAILED,
            )
            return@registerForActivityResult
        }

        searchablePdfSaveState = SearchablePdfSaveState.Saving
        searchablePdfExportJob = lifecycleScope.launch {
            val result = searchablePdfExportCoordinator.writePreparedExport(preparedExport, destinationUri)
            searchablePdfPreparedExport = null
            searchablePdfSaveState = when (result) {
                SearchablePdfExportResult.Success -> SearchablePdfSaveState.Saved
                is SearchablePdfExportResult.Failure -> SearchablePdfSaveState.Error(
                    when (result.reason) {
                        SearchablePdfExportError.PREPARED_EXPORT_UNAVAILABLE ->
                            SearchablePdfSaveError.PREPARATION_FAILED

                        SearchablePdfExportError.DESTINATION_UNAVAILABLE ->
                            SearchablePdfSaveError.DESTINATION_UNAVAILABLE

                        SearchablePdfExportError.WRITE_FAILED -> SearchablePdfSaveError.WRITE_FAILED
                    },
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch(Dispatchers.IO) {
            deleteStaleSharedPdfs(cacheDir)
        }
        setContent {
            PageHarborApp(
                scannerSpikeState = scannerSpikeState,
                pdfSaveState = pdfSaveState,
                pdfShareState = pdfShareState,
                pageExportState = pageExportState,
                ocrUiState = ocrUiState,
                searchablePdfSaveState = searchablePdfSaveState,
                onScanDocument = ::launchDocumentScanner,
                onSavePdf = ::choosePdfDestination,
                onSaveSearchablePdf = ::saveSearchablePdf,
                onSharePdf = ::sharePdf,
                onExportPages = ::exportPages,
                onRecognizeText = ::recognizeText,
                onClearRecognizedText = ::clearRecognizedText,
                onViewSourceCode = ::openSourceCode,
                onClearScanResult = {
                    clearRecognizedText()
                    scannedPdfUri = null
                    scannedPageUris = emptyList()
                    pdfSaveState = PdfSaveState.Idle
                    pdfShareState = PdfShareState.Idle
                    pageExportState = PageExportState.Idle
                    clearSearchablePdfSave()
                    scannerSpikeState = ScannerSpikeState.Idle
                },
            )
        }
    }

    private fun launchDocumentScanner() {
        if (scannerSpikeState == ScannerSpikeState.Preparing) return

        clearSearchablePdfSave()
        clearRecognizedText()
        scannerSpikeState = ScannerSpikeState.Preparing

        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE_WITH_FILTER)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
            )
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .build()

        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                scannerSpikeState = ScannerSpikeState.Idle
                val request = IntentSenderRequest.Builder(intentSender).build()
                scanLauncher.launch(request)
            }
            .addOnFailureListener {
                scannerSpikeState = ScannerSpikeState.Error
            }
    }

    private fun recognizeText() {
        if (!canStartOcr(ocrUiState)) return

        val pages = scannedPageUris.map { pageUri ->
            OcrPage {
                contentResolver.openInputStream(pageUri) ?: throw FileNotFoundException()
            }
        }
        if (pages.isEmpty()) {
            ocrUiState = OcrUiState.Error(OcrUiError.NO_PAGES)
            return
        }

        ocrUiState = OcrUiState.Recognizing
        ocrJob = lifecycleScope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    ocrEngine.recognize(pages)
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                return@launch
            } catch (_: Exception) {
                ocrUiState = OcrUiState.Error(OcrUiError.UNEXPECTED_FAILURE)
                return@launch
            }
            ocrUiState = ocrStateAfterResult(result)
        }
    }

    private fun clearRecognizedText() {
        ocrJob?.cancel()
        ocrJob = null
        ocrUiState = clearedOcrState()
    }

    private fun choosePdfDestination() {
        if (pdfSaveState == PdfSaveState.ChoosingDestination || pdfSaveState == PdfSaveState.Saving) {
            return
        }

        if (scannedPdfUri == null) {
            pdfSaveState = PdfSaveState.Error(PdfExportResult.SourceMissing)
            return
        }

        pdfSaveState = PdfSaveState.ChoosingDestination
        createPdfDocumentLauncher.launch(getString(R.string.pdf_default_filename))
    }

    private fun saveSearchablePdf() {
        if (searchablePdfSaveState.isInProgress()) return
        if (scannedPageUris.isEmpty()) {
            searchablePdfSaveState = SearchablePdfSaveState.Error(SearchablePdfSaveError.NO_PAGES)
            return
        }

        searchablePdfPreparedExport?.let(searchablePdfExportCoordinator::discardPreparedExport)
        searchablePdfPreparedExport = null
        searchablePdfSaveState = SearchablePdfSaveState.Preparing
        val existingOcrResult = (ocrUiState as? OcrUiState.Success)?.result
        searchablePdfExportJob = lifecycleScope.launch {
            val preparedExport = searchablePdfExportCoordinator.prepare(
                SearchablePdfExportRequest(
                    pageUris = scannedPageUris,
                    ocrResult = existingOcrResult,
                    progressListener = SearchablePdfExportProgressListener { progress ->
                        runOnUiThread {
                            searchablePdfSaveState = searchablePdfSaveStateForProgress(progress)
                        }
                    },
                ),
            )
            when (preparedExport) {
                is SearchablePdfPreparedExport.Ready -> {
                    searchablePdfPreparedExport = preparedExport
                    searchablePdfSaveState = SearchablePdfSaveState.ChoosingDestination
                    try {
                        createSearchablePdfDocumentLauncher.launch(
                            preparedExport.filenameSuggestion.filename,
                        )
                    } catch (_: ActivityNotFoundException) {
                        searchablePdfExportCoordinator.discardPreparedExport(preparedExport)
                        searchablePdfPreparedExport = null
                        searchablePdfSaveState = SearchablePdfSaveState.Error(
                            SearchablePdfSaveError.DESTINATION_UNAVAILABLE,
                        )
                    } catch (_: RuntimeException) {
                        searchablePdfExportCoordinator.discardPreparedExport(preparedExport)
                        searchablePdfPreparedExport = null
                        searchablePdfSaveState = SearchablePdfSaveState.Error(
                            SearchablePdfSaveError.DESTINATION_UNAVAILABLE,
                        )
                    }
                }

                is SearchablePdfPreparedExport.Failure -> {
                    searchablePdfSaveState = SearchablePdfSaveState.Error(
                        when (preparedExport.reason) {
                            org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfPreparationError.NO_PAGES ->
                                SearchablePdfSaveError.NO_PAGES

                            org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfPreparationError.OCR_FAILED,
                            org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfPreparationError.OCR_RESULT_MISMATCH,
                            org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfPreparationError.TEMPORARY_STORAGE_UNAVAILABLE,
                            org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfPreparationError.GENERATION_FAILED,
                            -> SearchablePdfSaveError.PREPARATION_FAILED
                        },
                    )
                }
            }
        }
    }

    private fun clearSearchablePdfSave() {
        searchablePdfExportJob?.cancel()
        searchablePdfExportJob = null
        searchablePdfPreparedExport?.let(searchablePdfExportCoordinator::discardPreparedExport)
        searchablePdfPreparedExport = null
        searchablePdfSaveState = SearchablePdfSaveState.Idle
    }

    private fun sharePdf() {
        if (pdfShareState == PdfShareState.Preparing) return

        val pdfUri = scannedPdfUri
        pdfShareState = PdfShareState.Preparing
        lifecycleScope.launch {
            val preparationResult = withContext(Dispatchers.IO) {
                preparePdfForSharing(this@MainActivity, pdfUri)
            }
            when (preparationResult) {
                is PdfSharePreparationResult.Ready -> launchPdfShare(preparationResult.uri)

                PdfSharePreparationResult.SourceMissing -> {
                    pdfShareState = PdfShareState.Error(PdfShareError.NoPdfAvailable)
                }

                PdfSharePreparationResult.Failed -> {
                    pdfShareState = PdfShareState.Error(PdfShareError.UnexpectedFailure)
                }
            }
        }
    }

    private fun launchPdfShare(pdfUri: Uri) {
        when (val result = createPdfShareIntent(pdfUri)) {
            is PdfShareIntentResult.Success -> {
                try {
                    val chooser = Intent.createChooser(
                        result.intent,
                        getString(R.string.pdf_share_chooser_title),
                    )
                    startActivity(chooser)
                    pdfShareState = PdfShareState.Idle
                } catch (_: ActivityNotFoundException) {
                    pdfShareState = PdfShareState.Error(PdfShareError.ShareTargetUnavailable)
                } catch (_: SecurityException) {
                    pdfShareState = PdfShareState.Error(PdfShareError.UnexpectedFailure)
                } catch (_: IllegalArgumentException) {
                    pdfShareState = PdfShareState.Error(PdfShareError.UnexpectedFailure)
                } catch (_: RuntimeException) {
                    pdfShareState = PdfShareState.Error(PdfShareError.UnexpectedFailure)
                }
            }

            PdfShareIntentResult.NoPdfAvailable -> {
                pdfShareState = PdfShareState.Error(PdfShareError.NoPdfAvailable)
            }

            PdfShareIntentResult.InvalidUri -> {
                pdfShareState = PdfShareState.Error(PdfShareError.InvalidUri)
            }
        }
    }

    private fun exportPages() {
        if (pageExportState is PageExportState.ChoosingDestination ||
            pageExportState is PageExportState.Exporting
        ) {
            return
        }

        pageExportState = startPageExport(scannedPageUris.size)
        val initialState = pageExportState as? PageExportState.ChoosingDestination ?: return
        launchPageDestination(initialState)
    }

    private fun launchPageDestination(state: PageExportState.ChoosingDestination) {
        try {
            createPageDocumentLauncher.launch(
                getString(R.string.page_export_default_filename, state.pageNumber),
            )
        } catch (_: ActivityNotFoundException) {
            pageExportState = PageExportState.Error(PageExportResult.DestinationUnavailable)
        } catch (_: RuntimeException) {
            pageExportState = PageExportState.Error(PageExportResult.DestinationUnavailable)
        }
    }

    private fun exportPageToDestination(
        state: PageExportState.ChoosingDestination,
        destinationUri: Uri,
    ) {
        val sourceUri = scannedPageUris.getOrNull(state.pageNumber - 1)
        if (sourceUri == null) {
            pageExportState = PageExportState.Error(PageExportResult.SourceMissing)
            return
        }

        pageExportState = PageExportState.Exporting(
            pageNumber = state.pageNumber,
            pageCount = state.pageCount,
        )
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                copyScannedPage(sourceUri, destinationUri)
            }
            if (result != PageExportResult.Success) {
                pageExportState = PageExportState.Error(result)
                return@launch
            }

            pageExportState = pageExportStateAfterSuccess(
                pageNumber = state.pageNumber,
                pageCount = state.pageCount,
            )
            (pageExportState as? PageExportState.ChoosingDestination)?.let(::launchPageDestination)
        }
    }

    private fun savePdfToDestination(destinationUri: Uri) {
        val sourceUri = scannedPdfUri
        if (sourceUri == null) {
            pdfSaveState = PdfSaveState.Error(PdfExportResult.SourceMissing)
            return
        }

        pdfSaveState = PdfSaveState.Saving
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                copyScannedPdf(sourceUri, destinationUri)
            }
            pdfSaveState = when (result) {
                PdfExportResult.Success -> PdfSaveState.Saved
                PdfExportResult.SourceMissing,
                PdfExportResult.DestinationUnavailable,
                PdfExportResult.WriteFailed,
                -> PdfSaveState.Error(result)
            }
        }
    }

    private fun copyScannedPdf(sourceUri: Uri, destinationUri: Uri): PdfExportResult {
        val source = try {
            contentResolver.openInputStream(sourceUri)
        } catch (_: FileNotFoundException) {
            null
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            return PdfExportResult.SourceMissing
        }

        val destination = try {
            contentResolver.openOutputStream(destinationUri)
        } catch (_: FileNotFoundException) {
            source.closeSafely()
            return PdfExportResult.DestinationUnavailable
        } catch (_: IOException) {
            source.closeSafely()
            return PdfExportResult.DestinationUnavailable
        } catch (_: SecurityException) {
            source.closeSafely()
            return PdfExportResult.DestinationUnavailable
        }

        return copyPdfToDestination(source, destination)
    }

    private fun copyScannedPage(sourceUri: Uri, destinationUri: Uri): PageExportResult {
        val source = try {
            contentResolver.openInputStream(sourceUri)
        } catch (_: FileNotFoundException) {
            null
        } catch (_: IOException) {
            return PageExportResult.WriteFailed
        } catch (_: SecurityException) {
            return PageExportResult.SourceMissing
        }

        val destination = try {
            contentResolver.openOutputStream(destinationUri)
        } catch (_: FileNotFoundException) {
            source.closeSafely()
            return PageExportResult.DestinationUnavailable
        } catch (_: IOException) {
            source.closeSafely()
            return PageExportResult.DestinationUnavailable
        } catch (_: SecurityException) {
            source.closeSafely()
            return PageExportResult.DestinationUnavailable
        }

        return copyPageToDestination(source, destination)
    }

    private fun InputStream?.closeSafely() {
        try {
            this?.close()
        } catch (_: IOException) {
            // Nothing user-actionable, and paths or document details must not be logged.
        }
    }

    override fun onDestroy() {
        clearSearchablePdfSave()
        super.onDestroy()
    }

    private fun openSourceCode() {
        val sourceIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(getString(R.string.source_code_url)),
        )

        try {
            startActivity(sourceIntent)
        } catch (_: ActivityNotFoundException) {
            // No browser is available. Keep the app stable and avoid logging local state.
        }
    }
}
