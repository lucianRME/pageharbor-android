package org.synapseworks.pageharbor.document.filename

import java.text.BreakIterator
import java.text.Normalizer
import java.util.Locale
import org.synapseworks.pageharbor.document.classification.DocumentCategory

/** A content-free, category-based filename suggestion for one future PDF export. */
data class FilenameSuggestion(
    val filename: String,
    val category: DocumentCategory,
)

/**
 * Produces deterministic, privacy-preserving PDF filenames from a broad document category.
 *
 * The public API intentionally accepts no OCR text, paths, destination URIs, or user identity.
 * It has no Android, storage, network, UI, or PDF dependency.
 */
class FilenameSuggestionEngine {
    fun suggest(category: DocumentCategory): FilenameSuggestion = FilenameSuggestion(
        filename = sanitizeFilenameCandidate(categoryBaseNames.getValue(category)),
        category = category,
    )

    /**
     * Internal preparation for future user-provided or localized labels. It is not connected to
     * export, OCR, or UI in this foundation.
     */
    internal fun sanitizeFilenameCandidate(candidate: String): String {
        val stem = normalizeStem(candidate)
            .takeIf { it.isNotEmpty() && it != "." && it != ".." }
            ?.let(::truncateStemToMaximumUtf8Bytes)
            ?.trim('-', '.')
            .orEmpty()

        return "${stem.ifEmpty { DefaultStem }}$PdfExtension"
    }

    private fun normalizeStem(candidate: String): String {
        val withoutPdfExtension = candidate.trim().removePdfExtensions()
        val normalized = Normalizer.normalize(withoutPdfExtension, Normalizer.Form.NFC)
        val safeCharacters = buildString(normalized.length) {
            normalized.codePoints().forEach { codePoint ->
                if (codePoint.isUnsafeFilenameCodePoint()) {
                    append(' ')
                } else {
                    appendCodePoint(codePoint)
                }
            }
        }

        return safeCharacters
            .replace(Whitespace, "-")
            .replace(RepeatedHyphens, "-")
            .trim('-', '.', ' ')
    }

    private fun truncateStemToMaximumUtf8Bytes(stem: String): String {
        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
        iterator.setText(stem)

        val bounded = StringBuilder()
        var start = iterator.first()
        var end = iterator.next()
        var byteCount = 0
        while (end != BreakIterator.DONE) {
            val cluster = stem.substring(start, end)
            val clusterBytes = cluster.toByteArray(Charsets.UTF_8).size
            if (byteCount + clusterBytes > MaximumStemUtf8Bytes) break
            bounded.append(cluster)
            byteCount += clusterBytes
            start = end
            end = iterator.next()
        }
        return bounded.toString()
    }

    private fun String.removePdfExtensions(): String {
        var stem = this
        while (stem.endsWith(PdfExtension, ignoreCase = true)) {
            stem = stem.dropLast(PdfExtension.length).trimEnd()
        }
        return stem
    }

    private fun Int.isUnsafeFilenameCodePoint(): Boolean =
        Character.isISOControl(this) || this in InvalidFilenameCodePoints

    private companion object {
        const val PdfExtension = ".pdf"
        const val DefaultStem = "document"
        const val MaximumStemUtf8Bytes = 80

        val Whitespace = Regex("\\s+")
        val RepeatedHyphens = Regex("-+")
        val InvalidFilenameCodePoints = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
            .map(Char::code)
            .toSet()

        val categoryBaseNames = mapOf(
            DocumentCategory.INVOICE to "invoice",
            DocumentCategory.RECEIPT to "receipt",
            DocumentCategory.LETTER to "letter",
            DocumentCategory.FORM to "form",
            DocumentCategory.UNKNOWN to DefaultStem,
        )
    }
}
