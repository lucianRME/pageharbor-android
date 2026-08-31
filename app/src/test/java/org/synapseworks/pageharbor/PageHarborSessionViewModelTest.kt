package org.synapseworks.pageharbor

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfSaveState
import org.synapseworks.pageharbor.image.DocumentFilter
import org.synapseworks.pageharbor.ocr.OcrPageResult
import org.synapseworks.pageharbor.ocr.OcrResult
import org.synapseworks.pageharbor.ocr.OcrUiState
import org.synapseworks.pageharbor.scanner.ScannerSpikeState
import org.synapseworks.pageharbor.ui.PageHarborScreen

class PageHarborSessionViewModelTest {
    @Test
    fun newPagesDefaultToOriginalAndChangingOneDoesNotAffectAnother() {
        val session = completedSession("first", "second")
        val first = session.scanPages[0]
        val second = session.scanPages[1]

        assertEquals(DocumentFilter.ORIGINAL, first.filter)
        assertEquals(DocumentFilter.ORIGINAL, second.filter)
        assertEquals(true, session.setPageFilter(first.id, DocumentFilter.GRAYSCALE))
        assertEquals(DocumentFilter.GRAYSCALE, session.scanPages[0].filter)
        assertEquals(DocumentFilter.ORIGINAL, session.scanPages[1].filter)
    }

    @Test
    fun reorderKeepsTheFilterWithItsStablePageIdentity() {
        val session = completedSession("first", "second", "third")
        val first = session.scanPages[0]
        val second = session.scanPages[1]
        val third = session.scanPages[2]
        session.setPageFilter(second.id, DocumentFilter.BLACK_AND_WHITE)

        assertEquals(true, session.reorderPages(listOf(third.id, second.id, first.id)))
        assertEquals(listOf(third.id, second.id, first.id), session.scanPages.map(ActiveScanPage::id))
        assertEquals(DocumentFilter.BLACK_AND_WHITE, session.scanPages[1].filter)
    }

    @Test
    fun addedPagesDefaultToOriginalWithoutChangingExistingSelections() {
        val session = completedSession("first")
        val existing = session.scanPages.single()
        session.setPageFilter(existing.id, DocumentFilter.HIGH_CONTRAST)
        session.beginScannerRequest()
        session.completeScannerRequest(scanSummary(pageCount = 1), null, pages("second"))

        assertEquals(listOf(DocumentFilter.HIGH_CONTRAST, DocumentFilter.ORIGINAL),
            session.scanPages.map(ActiveScanPage::filter))
    }

    @Test
    fun revertingAndInvalidPageIdentifiersAreSafe() {
        val session = completedSession("first")
        val page = session.scanPages.single()
        session.setPageFilter(page.id, DocumentFilter.AUTO_ENHANCE)

        assertEquals(true, session.setPageFilter(page.id, DocumentFilter.ORIGINAL))
        assertEquals(DocumentFilter.ORIGINAL, session.scanPages.single().filter)
        assertEquals(false, session.setPageFilter(Long.MAX_VALUE, DocumentFilter.GRAYSCALE))
        assertEquals(false, session.reorderPages(emptyList()))
    }

    @Test
    fun recreationRetainsActivePageFilters() {
        val session = completedSession("first")
        val page = session.scanPages.single()
        session.setPageFilter(page.id, DocumentFilter.BLACK_AND_WHITE)

        session.resetTransientStateForRecreation()

        assertEquals(DocumentFilter.BLACK_AND_WHITE, session.scanPages.single().filter)
    }
    @Test
    fun returningFromOcrKeepsTheActiveScanAndItsOrderedPages() {
        val session = completedSession("first", "second")
        session.ocrUiState = OcrUiState.Success(
            OcrResult(listOf(OcrPageResult(pageIndex = 0, text = "Recognized"))),
        )
        session.screen = PageHarborScreen.OcrResult

        session.returnToScanResult()

        assertEquals(PageHarborScreen.ScanResult, session.screen)
        assertEquals(scanSummary(pageCount = 2), session.scannerState)
        assertEquals(pages("first", "second"), session.scannedPageUris)
    }

    @Test
    fun cancelledAddPagesRequestKeepsTheActiveScanUnchanged() {
        val session = completedSession("first", "second")
        session.screen = PageHarborScreen.Home

        assertEquals(ScannerRequestMode.ADD_PAGES, session.beginScannerRequest())
        session.cancelScannerRequest()

        assertEquals(PageHarborScreen.ScanResult, session.screen)
        assertEquals(scanSummary(pageCount = 2), session.scannerState)
        assertEquals(pages("first", "second"), session.scannedPageUris)
    }

    @Test
    fun successfulAddPagesRequestAppendsPagesInOrder() {
        val session = completedSession("first", "second")

        assertEquals(ScannerRequestMode.ADD_PAGES, session.beginScannerRequest())
        session.completeScannerRequest(
            scannerState = scanSummary(pageCount = 2),
            scannedPdfUri = null,
            scannedPageUris = pages("third", "fourth"),
        )

        assertEquals(PageHarborScreen.ScanResult, session.screen)
        assertEquals(scanSummary(pageCount = 4, pdfPageCount = 2), session.scannerState)
        assertEquals(pages("first", "second", "third", "fourth"), session.scannedPageUris)
    }

    @Test
    fun cancelledInitialScanDoesNotCreateAnEmptyScanResult() {
        val session = PageHarborSessionViewModel()

        assertEquals(ScannerRequestMode.INITIAL_SCAN, session.beginScannerRequest())
        session.cancelScannerRequest()

        assertEquals(PageHarborScreen.Home, session.screen)
        assertEquals(ScannerSpikeState.Cancelled, session.scannerState)
        assertEquals(emptyList<Uri>(), session.scannedPageUris)
    }

    @Test
    fun repeatedAddPagesCancellationNeverMutatesTheActiveScan() {
        val session = completedSession("first", "second")

        repeat(2) {
            assertEquals(ScannerRequestMode.ADD_PAGES, session.beginScannerRequest())
            session.cancelScannerRequest()
        }

        assertEquals(scanSummary(pageCount = 2), session.scannerState)
        assertEquals(pages("first", "second"), session.scannedPageUris)
    }

    @Test
    fun addPagesRequestWithoutScannerContentKeepsTheActiveScanUnchanged() {
        val session = completedSession("first", "second")

        assertEquals(ScannerRequestMode.ADD_PAGES, session.beginScannerRequest())
        session.completeScannerRequestWithoutResult()

        assertEquals(PageHarborScreen.ScanResult, session.screen)
        assertEquals(scanSummary(pageCount = 2), session.scannerState)
        assertEquals(pages("first", "second"), session.scannedPageUris)
    }

    @Test
    fun returningFromOcrLeavesTheScanUsableForAnotherOcrResult() {
        val session = completedSession("first")
        session.ocrUiState = OcrUiState.Success(
            OcrResult(listOf(OcrPageResult(pageIndex = 0, text = "Recognized"))),
        )
        session.screen = PageHarborScreen.OcrResult

        session.returnToScanResult()
        session.screen = PageHarborScreen.OcrResult

        assertEquals(PageHarborScreen.OcrResult, session.screen)
        assertEquals(pages("first"), session.scannedPageUris)
        assertEquals(scanSummary(pageCount = 1), session.scannerState)
    }

    @Test
    fun addPagesErrorDoesNotDestroyTheActiveScan() {
        val session = completedSession("first", "second")

        assertEquals(ScannerRequestMode.ADD_PAGES, session.beginScannerRequest())
        session.failScannerRequest()

        assertEquals(PageHarborScreen.ScanResult, session.screen)
        assertEquals(scanSummary(pageCount = 2), session.scannerState)
        assertEquals(pages("first", "second"), session.scannedPageUris)
    }

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

    private fun scanSummary(
        pageCount: Int,
        pdfPageCount: Int? = pageCount,
    ) = ScannerSpikeState.ResultSummary(
        jpegPageCount = pageCount,
        hasPdf = pdfPageCount != null,
        pdfPageCount = pdfPageCount,
    )

    private fun completedSession(vararg pageTokens: String): PageHarborSessionViewModel =
        PageHarborSessionViewModel().also { session ->
            session.replaceScan(
                scannerState = scanSummary(pageTokens.size),
                scannedPdfUri = null,
                scannedPageUris = pages(*pageTokens),
            )
        }

    @Suppress("UNCHECKED_CAST")
    private fun pages(vararg tokens: String): List<Uri> = List(tokens.size) { null } as List<Uri>
}
