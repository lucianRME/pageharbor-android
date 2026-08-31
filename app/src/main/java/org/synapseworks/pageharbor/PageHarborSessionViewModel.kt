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
import org.synapseworks.pageharbor.image.DocumentFilter
import org.synapseworks.pageharbor.ocr.OcrUiState
import org.synapseworks.pageharbor.scanner.ScannerSpikeState
import org.synapseworks.pageharbor.scanner.createScannerResultSummary
import org.synapseworks.pageharbor.ui.PageHarborScreen

/** The maximum number of pages retained by one active, in-memory scan session. */
internal const val MAX_SCAN_PAGES = 20

/** Whether the external scanner is acquiring a first scan or pages for the active scan. */
internal enum class ScannerRequestMode {
    INITIAL_SCAN,
    ADD_PAGES,
}

/** Stable, active-session-only identity and non-destructive filter choice for one source URI. */
data class ActiveScanPage(
    val id: Long,
    val sourceUri: Uri?,
    val filter: DocumentFilter = DocumentFilter.ORIGINAL,
)

/**
 * Retains only the current in-memory scan session while Android recreates MainActivity.
 * It deliberately has no saved-state handle: process-death recovery is unsupported.
 */
class PageHarborSessionViewModel : ViewModel() {
    var screen: PageHarborScreen by mutableStateOf(PageHarborScreen.Home)
    var scannerState: ScannerSpikeState by mutableStateOf(ScannerSpikeState.Idle)
    var scannedPdfUri: Uri? by mutableStateOf(null)
    var scanPages: List<ActiveScanPage> by mutableStateOf(emptyList())
        private set
    @Suppress("UNCHECKED_CAST")
    val scannedPageUris: List<Uri>
        get() = scanPages.map(ActiveScanPage::sourceUri) as List<Uri>
    var pdfSaveState: PdfSaveState by mutableStateOf(PdfSaveState.Idle)
    var pdfShareState: PdfShareState by mutableStateOf(PdfShareState.Idle)
    var pageExportState: PageExportState by mutableStateOf(PageExportState.Idle)
    var ocrUiState: OcrUiState by mutableStateOf(OcrUiState.Idle)
    var ocrSelectedPageIndex: Int by mutableStateOf(0)
    var searchablePdfSaveState: SearchablePdfSaveState by mutableStateOf(SearchablePdfSaveState.Idle)
    private var activeScannerRequest: ScannerRequestMode? = null
    private var nextPageId = 0L

    /**
     * Starts one external scanner request without clearing a completed session pre-emptively.
     * A second request is ignored until the pending request returns or fails to start.
     */
    internal fun beginScannerRequest(): ScannerRequestMode? {
        if (activeScannerRequest != null) return null
        if (scannerState is ScannerSpikeState.ResultSummary && remainingPageCapacity() == 0) {
            return null
        }

        return if (scannerState is ScannerSpikeState.ResultSummary) {
            ScannerRequestMode.ADD_PAGES
        } else {
            scannerState = ScannerSpikeState.Preparing
            ScannerRequestMode.INITIAL_SCAN
        }.also { activeScannerRequest = it }
    }

    /** The capacity that can safely be requested from the next scanner invocation. */
    internal fun remainingPageCapacity(): Int =
        (MAX_SCAN_PAGES - activePageCount()).coerceAtLeast(0)

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
        val acceptedPageUris = scannedPageUris.take(MAX_SCAN_PAGES)
        val scannerExceededPageLimit = scannerState.jpegPageCount > MAX_SCAN_PAGES ||
            scannedPageUris.size > MAX_SCAN_PAGES
        this.scannedPdfUri = if (scannerExceededPageLimit) null else scannedPdfUri
        scanPages = newPages(acceptedPageUris)
        this.scannerState = if (scannerExceededPageLimit) {
            createScannerResultSummary(
                jpegPageCount = acceptedPageUris.size,
                pdfPageCount = null,
            )
        } else {
            scannerState
        }
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
        scanPages = emptyList()
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
        addedPageUris: List<Uri>,
    ) {
        val acceptedPageUris = addedPageUris.take(remainingPageCapacity())
        if (acceptedPageUris.isEmpty()) {
            screen = PageHarborScreen.ScanResult
            return
        }

        scanPages = scanPages + newPages(acceptedPageUris)
        scannerState = createScannerResultSummary(
            jpegPageCount = existingSummary.jpegPageCount + acceptedPageUris.size,
            pdfPageCount = existingSummary.pdfPageCount,
        )
        ocrUiState = OcrUiState.Idle
        ocrSelectedPageIndex = 0
        resetTransientState()
        screen = PageHarborScreen.ScanResult
    }

    fun setPageFilter(pageId: Long, filter: DocumentFilter): Boolean {
        val index = scanPages.indexOfFirst { it.id == pageId }
        if (index < 0) return false
        scanPages = scanPages.toMutableList().apply {
            this[index] = this[index].copy(filter = filter)
        }
        return true
    }

    /** Keeps each filter with its stable page identity; invalid order requests are ignored safely. */
    fun reorderPages(pageIds: List<Long>): Boolean {
        if (pageIds.size != scanPages.size || pageIds.toSet().size != scanPages.size) return false
        val pagesById = scanPages.associateBy(ActiveScanPage::id)
        val reordered = pageIds.map { pagesById[it] ?: return false }
        scanPages = reordered
        return true
    }

    private fun newPages(uris: List<Uri?>): List<ActiveScanPage> = uris.map { uri ->
        ActiveScanPage(id = nextPageId++, sourceUri = uri)
    }

    /** Use the larger representation defensively while an external scanner result is pending. */
    private fun activePageCount(): Int = maxOf(
        scanPages.size,
        (scannerState as? ScannerSpikeState.ResultSummary)?.jpegPageCount ?: 0,
    )
}
