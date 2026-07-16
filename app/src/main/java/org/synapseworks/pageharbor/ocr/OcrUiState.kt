package org.synapseworks.pageharbor.ocr

/** UI-safe, in-memory state for one explicitly requested OCR operation. */
sealed interface OcrUiState {
    data object Idle : OcrUiState
    data object Recognizing : OcrUiState
    data class Success(val result: OcrResult) : OcrUiState
    data class Error(val reason: OcrUiError) : OcrUiState
}

enum class OcrUiError {
    NO_PAGES,
    ALL_PAGES_FAILED,
    UNEXPECTED_FAILURE,
}

fun canStartOcr(state: OcrUiState): Boolean = state != OcrUiState.Recognizing

fun clearedOcrState(): OcrUiState = OcrUiState.Idle

fun ocrStateAfterResult(result: OcrResult): OcrUiState = when {
    result.pages.isEmpty() -> OcrUiState.Error(OcrUiError.NO_PAGES)
    result.pages.all { it.error != null } -> OcrUiState.Error(OcrUiError.ALL_PAGES_FAILED)
    else -> OcrUiState.Success(result)
}

fun OcrResult.textFoundPageCount(): Int = pages.count { it.text.isNotBlank() }

fun OcrResult.failedPageCount(): Int = pages.count { it.error != null }

/**
 * Produces the deterministic, ordered plain-text preview without document metadata.
 * Callers supply localized headings and empty-page wording for display.
 */
fun formatOcrPreview(
    result: OcrResult,
    pageHeading: (Int) -> String,
    emptyPageText: String,
): String = result.pages.joinToString(separator = "\n\n") { page ->
    "${pageHeading(page.pageIndex + 1)}\n${page.text.ifBlank { emptyPageText }}"
}
