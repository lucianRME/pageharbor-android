package org.synapseworks.pageharbor.ocr

/**
 * In-memory text recognized from an OCR request, kept in the same order as its input pages.
 *
 * Recognized text is sensitive document content and must not be logged or retained by default.
 */
data class OcrResult(
    val pages: List<OcrPageResult>,
) {
    val plainText: String
        get() = pages.joinToString(separator = "\n\n") { it.text }
}

data class OcrPageResult(
    val text: String,
)
