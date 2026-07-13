package org.synapseworks.pageharbor.scanner

sealed interface ScannerSpikeState {
    data object Idle : ScannerSpikeState
    data object Preparing : ScannerSpikeState
    data class ResultSummary(
        val jpegPageCount: Int,
        val hasPdf: Boolean,
        val pdfPageCount: Int?,
    ) : ScannerSpikeState
    data object Cancelled : ScannerSpikeState
    data object Error : ScannerSpikeState
}

fun createScannerResultSummary(
    jpegPageCount: Int,
    pdfPageCount: Int?,
): ScannerSpikeState.ResultSummary =
    ScannerSpikeState.ResultSummary(
        jpegPageCount = jpegPageCount.coerceAtLeast(0),
        hasPdf = pdfPageCount != null,
        pdfPageCount = pdfPageCount,
    )
