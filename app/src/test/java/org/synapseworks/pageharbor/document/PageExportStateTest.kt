package org.synapseworks.pageharbor.document

import org.junit.Assert.assertEquals
import org.junit.Test

class PageExportStateTest {
    @Test
    fun startPageExportSelectsTheFirstPage() {
        assertEquals(
            PageExportState.ChoosingDestination(pageNumber = 1, pageCount = 3),
            startPageExport(pageCount = 3),
        )
    }

    @Test
    fun startPageExportRejectsMissingPages() {
        assertEquals(
            PageExportState.Error(PageExportResult.SourceMissing),
            startPageExport(pageCount = 0),
        )
    }

    @Test
    fun successfulPageAdvancesToTheNextPicker() {
        assertEquals(
            PageExportState.ChoosingDestination(pageNumber = 2, pageCount = 3),
            pageExportStateAfterSuccess(pageNumber = 1, pageCount = 3),
        )
    }

    @Test
    fun successfulFinalPageCompletesExport() {
        assertEquals(
            PageExportState.Completed(pageCount = 3),
            pageExportStateAfterSuccess(pageNumber = 3, pageCount = 3),
        )
    }

    @Test
    fun cancellationReportsOnlyPreviouslyExportedPages() {
        assertEquals(
            PageExportState.Cancelled(exportedPageCount = 1),
            pageExportStateAfterCancellation(pageNumber = 2),
        )
    }
}
