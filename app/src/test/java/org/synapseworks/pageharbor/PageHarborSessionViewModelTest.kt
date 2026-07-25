package org.synapseworks.pageharbor

import org.junit.Assert.assertEquals
import org.junit.Test
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfSaveState
import org.synapseworks.pageharbor.ocr.OcrPageResult
import org.synapseworks.pageharbor.ocr.OcrResult
import org.synapseworks.pageharbor.ocr.OcrUiState
import org.synapseworks.pageharbor.scanner.ScannerSpikeState
import org.synapseworks.pageharbor.ui.PageHarborScreen

class PageHarborSessionViewModelTest {
    @Test
    fun completedScanIsRetainedOnTheScanResultScreen() {
        val session = PageHarborSessionViewModel()
        val summary = scanSummary(pageCount = 2)

        session.replaceScan(summary, scannedPdfUri = null, scannedPageUris = emptyList())

        assertEquals(PageHarborScreen.ScanResult, session.screen)
        assertEquals(summary, session.scannerState)
    }

    @Test
    fun completedOcrResultRemainsAvailableAfterRecreationReset() {
        val session = PageHarborSessionViewModel()
        val result = OcrResult(listOf(OcrPageResult(pageIndex = 0, text = "Retained test text")))
        session.replaceScan(scanSummary(pageCount = 1), scannedPdfUri = null, scannedPageUris = emptyList())
        session.ocrUiState = OcrUiState.Success(result)
        session.screen = PageHarborScreen.OcrResult

        session.resetTransientStateForRecreation()

        assertEquals(PageHarborScreen.OcrResult, session.screen)
        assertEquals(OcrUiState.Success(result), session.ocrUiState)
    }

    @Test
    fun newScanReplacesOldOcrStateAndRestoresScanResult() {
        val session = PageHarborSessionViewModel()
        session.replaceScan(scanSummary(pageCount = 1), scannedPdfUri = null, scannedPageUris = emptyList())
        session.ocrUiState = OcrUiState.Success(
            OcrResult(listOf(OcrPageResult(pageIndex = 0, text = "Old text"))),
        )
        val replacement = scanSummary(pageCount = 3)

        session.replaceScan(replacement, scannedPdfUri = null, scannedPageUris = emptyList())

        assertEquals(PageHarborScreen.ScanResult, session.screen)
        assertEquals(replacement, session.scannerState)
        assertEquals(OcrUiState.Idle, session.ocrUiState)
        assertEquals(0, session.ocrSelectedPageIndex)
    }

    @Test
    fun discardClearsTheInMemorySession() {
        val session = PageHarborSessionViewModel()
        session.replaceScan(scanSummary(pageCount = 1), scannedPdfUri = null, scannedPageUris = emptyList())
        session.ocrUiState = OcrUiState.Success(
            OcrResult(listOf(OcrPageResult(pageIndex = 0, text = "Discarded test text"))),
        )

        session.clearScan()

        assertEquals(PageHarborScreen.Home, session.screen)
        assertEquals(ScannerSpikeState.Idle, session.scannerState)
        assertEquals(emptyList<Any>(), session.scannedPageUris)
        assertEquals(null, session.scannedPdfUri)
        assertEquals(OcrUiState.Idle, session.ocrUiState)
        assertEquals(0, session.ocrSelectedPageIndex)
    }

    @Test
    fun activeOperationStateResetsWithoutDiscardingTheCompletedScan() {
        val session = PageHarborSessionViewModel()
        val summary = scanSummary(pageCount = 2)
        session.replaceScan(summary, scannedPdfUri = null, scannedPageUris = emptyList())
        session.ocrUiState = OcrUiState.Recognizing
        session.searchablePdfSaveState = SearchablePdfSaveState.Generating

        session.resetTransientStateForRecreation()

        assertEquals(PageHarborScreen.ScanResult, session.screen)
        assertEquals(summary, session.scannerState)
        assertEquals(OcrUiState.Idle, session.ocrUiState)
        assertEquals(SearchablePdfSaveState.Idle, session.searchablePdfSaveState)
    }

    @Test
    fun selectedOcrPageSurvivesRecreationButNewScanResetsIt() {
        val session = PageHarborSessionViewModel()
        session.replaceScan(scanSummary(pageCount = 3), scannedPdfUri = null, scannedPageUris = emptyList())
        session.ocrUiState = OcrUiState.Success(
            OcrResult(
                listOf(
                    OcrPageResult(pageIndex = 0, text = "First"),
                    OcrPageResult(pageIndex = 1, text = "Second"),
                    OcrPageResult(pageIndex = 2, text = "Third"),
                ),
            ),
        )
        session.screen = PageHarborScreen.OcrResult
        session.ocrSelectedPageIndex = 2

        session.resetTransientStateForRecreation()

        assertEquals(PageHarborScreen.OcrResult, session.screen)
        assertEquals(2, session.ocrSelectedPageIndex)

        session.replaceScan(scanSummary(pageCount = 1), scannedPdfUri = null, scannedPageUris = emptyList())

        assertEquals(0, session.ocrSelectedPageIndex)
    }

    private fun scanSummary(pageCount: Int) = ScannerSpikeState.ResultSummary(
        jpegPageCount = pageCount,
        hasPdf = true,
        pdfPageCount = pageCount,
    )
}
