package org.synapseworks.pageharbor.document

sealed interface PdfSaveState {
    data object Idle : PdfSaveState
    data object ChoosingDestination : PdfSaveState
    data object Saving : PdfSaveState
    data object Saved : PdfSaveState
    data class Error(val result: PdfExportResult) : PdfSaveState
}
