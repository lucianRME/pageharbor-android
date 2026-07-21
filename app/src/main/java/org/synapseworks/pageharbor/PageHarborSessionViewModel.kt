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
import org.synapseworks.pageharbor.ui.PageHarborScreen

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
    var searchablePdfSaveState: SearchablePdfSaveState by mutableStateOf(SearchablePdfSaveState.Idle)

    fun replaceScan(
        scannerState: ScannerSpikeState.ResultSummary,
        scannedPdfUri: Uri?,
        scannedPageUris: List<Uri>,
    ) {
        this.scannedPdfUri = scannedPdfUri
        this.scannedPageUris = scannedPageUris
        this.scannerState = scannerState
        ocrUiState = OcrUiState.Idle
        resetTransientState()
        screen = PageHarborScreen.ScanResult
    }

    fun clearScan() {
        screen = PageHarborScreen.Home
        scannerState = ScannerSpikeState.Idle
        scannedPdfUri = null
        scannedPageUris = emptyList()
        ocrUiState = OcrUiState.Idle
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
}
