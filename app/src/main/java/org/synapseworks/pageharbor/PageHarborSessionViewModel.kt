package org.synapseworks.pageharbor

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.synapseworks.pageharbor.document.PageExportState
import org.synapseworks.pageharbor.document.PdfSaveState
import org.synapseworks.pageharbor.document.PdfShareState
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfSaveState
import org.synapseworks.pageharbor.ocr.OcrUiState
import org.synapseworks.pageharbor.scanner.ScannerSpikeState
import org.synapseworks.pageharbor.scanner.createScannerResultSummary
import org.synapseworks.pageharbor.ui.PageHarborScreen

/** Whether the external scanner is acquiring a first scan or pages for the active scan. */
internal enum class ScannerRequestMode {
    INITIAL_SCAN,
    ADD_PAGES,
}

/**
 * Retains only the current in-memory scan session while Android recreates MainActivity.
 * It deliberately has no saved-state handle: process-death recovery is unsupported.
 */
class PageHarborSessionViewModel : ViewModel() {
    var screen: PageHarborScreen by mutableStateOf(PageHarborScreen.Home)
    var scannerState: ScannerSpikeState by mutableStateOf(ScannerSpikeState.Idle)
    var scannedPdfUri: Uri? by mutableStateOf(null)
    var scannedPageUris: List<Uri> by mutableStateOf(emptyList())
    var pdfSaveState: PdfSaveState by mutableStateOf(PdfSaveState.Idle)
    var pdfShareState: PdfShareState by mutableStateOf(PdfShareState.Idle)
    var pageExportState: PageExportState by mutableStateOf(PageExportState.Idle)
    var ocrUiState: OcrUiState by mutableStateOf(OcrUiState.Idle)
    var ocrSelectedPageIndex: Int by mutableStateOf(0)
    var searchablePdfSaveState: SearchablePdfSaveState by mutableStateOf(SearchablePdfSaveState.Idle)
    private var activeScannerRequest: ScannerRequestMode? = null

    /**
     * Starts one external scanner request without clearing a completed session pre-emptively.
     * A second request is ignored until the pending request returns or fails to start.
     */
    internal fun beginScannerRequest(): ScannerRequestMode? {
        if (activeScannerRequest != null) return null

        return if (scannerState is ScannerSpikeState.ResultSummary) {
            ScannerRequestMode.ADD_PAGES
        } else {
            scannerState = ScannerSpikeState.Preparing
            ScannerRequestMode.INITIAL_SCAN
        }.also { activeScannerRequest = it }
    }

    /** A cancelled add-pages request is intentionally a no-op for the active scan session. */
    fun cancelScannerRequest() {
        when (activeScannerRequest) {
            ScannerRequestMode.INITIAL_SCAN -> scannerState = ScannerSpikeState.Cancelled
            ScannerRequestMode.ADD_PAGES -> screen = PageHarborScreen.ScanResult
            null,
            -> Unit
        }
        activeScannerRequest = null
    }

    /** A failed add-pages request is intentionally a no-op for the active scan session. */
    fun failScannerRequest() {
        when (activeScannerRequest) {
            ScannerRequestMode.INITIAL_SCAN -> scannerState = ScannerSpikeState.Error
            ScannerRequestMode.ADD_PAGES -> screen = PageHarborScreen.ScanResult
            null,
            -> Unit
        }
        activeScannerRequest = null
    }

    /** A successful activity result without scanner content cannot replace an active scan. */
    fun completeScannerRequestWithoutResult() {
        when (activeScannerRequest) {
            ScannerRequestMode.INITIAL_SCAN -> scannerState = ScannerSpikeState.Error
            ScannerRequestMode.ADD_PAGES -> screen = PageHarborScreen.ScanResult
            null,
            -> Unit
        }
        activeScannerRequest = null
    }

    /**
     * Applies a successful scanner result according to the request that launched it. Scanner PDF
     * output belongs only to that individual scanner session, so an add-pages result retains the
     * existing PDF source rather than incorrectly replacing it with a PDF for only the new pages.
     */
    fun completeScannerRequest(
        scannerState: ScannerSpikeState.ResultSummary,
        scannedPdfUri: Uri?,
        scannedPageUris: List<Uri>,
    ) {
        val request = activeScannerRequest
        activeScannerRequest = null
        val existingSummary = this.scannerState as? ScannerSpikeState.ResultSummary
        if (request == ScannerRequestMode.ADD_PAGES && existingSummary != null) {
            appendScan(
                existingSummary = existingSummary,
                addedSummary = scannerState,
                addedPageUris = scannedPageUris,
            )
        } else {
            replaceScan(scannerState, scannedPdfUri, scannedPageUris)
        }
    }

    /** Returns from OCR only when the active scan is still available. */
    fun returnToScanResult() {
        if (scannerState is ScannerSpikeState.ResultSummary) {
            screen = PageHarborScreen.ScanResult
        }
    }

    fun replaceScan(
        scannerState: ScannerSpikeState.ResultSummary,
        scannedPdfUri: Uri?,
        scannedPageUris: List<Uri>,
    ) {
        this.scannedPdfUri = scannedPdfUri
        this.scannedPageUris = scannedPageUris
        this.scannerState = scannerState
        ocrUiState = OcrUiState.Idle
        ocrSelectedPageIndex = 0
        resetTransientState()
        screen = PageHarborScreen.ScanResult
    }

    fun clearScan() {
        activeScannerRequest = null
        screen = PageHarborScreen.Home
        scannerState = ScannerSpikeState.Idle
        scannedPdfUri = null
        scannedPageUris = emptyList()
        ocrUiState = OcrUiState.Idle
        ocrSelectedPageIndex = 0
        resetTransientState()
    }

    /** Active work is Activity-owned and is cancelled by the Activity; completed data remains. */
    fun resetTransientStateForRecreation() {
        if (ocrUiState == OcrUiState.Recognizing) {
            ocrUiState = OcrUiState.Idle
        }
        resetTransientState()
        screen = when {
            screen == PageHarborScreen.OcrResult && ocrUiState is OcrUiState.Success -> {
                PageHarborScreen.OcrResult
            }

            scannerState is ScannerSpikeState.ResultSummary -> PageHarborScreen.ScanResult
            else -> PageHarborScreen.Home
        }
    }

    private fun resetTransientState() {
        pdfSaveState = PdfSaveState.Idle
        pdfShareState = PdfShareState.Idle
        pageExportState = PageExportState.Idle
        searchablePdfSaveState = SearchablePdfSaveState.Idle
    }

    private fun appendScan(
        existingSummary: ScannerSpikeState.ResultSummary,
        addedSummary: ScannerSpikeState.ResultSummary,
        addedPageUris: List<Uri>,
    ) {
        if (addedPageUris.isEmpty()) {
            screen = PageHarborScreen.ScanResult
            return
        }

        scannedPageUris = scannedPageUris + addedPageUris
        scannerState = createScannerResultSummary(
            jpegPageCount = existingSummary.jpegPageCount + addedSummary.jpegPageCount,
            pdfPageCount = existingSummary.pdfPageCount,
        )
        ocrUiState = OcrUiState.Idle
        ocrSelectedPageIndex = 0
        resetTransientState()
        screen = PageHarborScreen.ScanResult
    }
}
