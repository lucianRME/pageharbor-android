package org.synapseworks.pageharbor.document

sealed interface PageExportState {
    data object Idle : PageExportState
    data class ChoosingDestination(
        val pageNumber: Int,
        val pageCount: Int,
    ) : PageExportState

    data class Exporting(
        val pageNumber: Int,
        val pageCount: Int,
    ) : PageExportState

    data class Completed(val pageCount: Int) : PageExportState
    data class Cancelled(val exportedPageCount: Int) : PageExportState
    data class Error(val result: PageExportResult) : PageExportState
}

fun startPageExport(pageCount: Int): PageExportState = if (pageCount > 0) {
    PageExportState.ChoosingDestination(pageNumber = 1, pageCount = pageCount)
} else {
    PageExportState.Error(PageExportResult.SourceMissing)
}

fun pageExportStateAfterCancellation(pageNumber: Int): PageExportState =
    PageExportState.Cancelled(exportedPageCount = (pageNumber - 1).coerceAtLeast(0))

fun pageExportStateAfterSuccess(
    pageNumber: Int,
    pageCount: Int,
): PageExportState = if (pageNumber < pageCount) {
    PageExportState.ChoosingDestination(
        pageNumber = pageNumber + 1,
        pageCount = pageCount,
    )
} else {
    PageExportState.Completed(pageCount = pageCount)
}
