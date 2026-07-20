package org.synapseworks.pageharbor.document.filename

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.synapseworks.pageharbor.document.classification.DocumentCategory
import org.synapseworks.pageharbor.document.classification.DocumentClassifier

class FilenameSuggestionEngineTest {
    private val engine = FilenameSuggestionEngine()

    @Test
    fun suggestsExpectedFilenameForEveryCategory() {
        assertSuggestion(DocumentCategory.INVOICE, "invoice.pdf")
        assertSuggestion(DocumentCategory.RECEIPT, "receipt.pdf")
        assertSuggestion(DocumentCategory.LETTER, "letter.pdf")
        assertSuggestion(DocumentCategory.FORM, "form.pdf")
        assertSuggestion(DocumentCategory.UNKNOWN, "document.pdf")
    }

    @Test
    fun categorySuggestionsContainOnlySafeAsciiPdfNames() {
        DocumentCategory.entries.forEach { category ->
            val filename = engine.suggest(category).filename

            assertTrue(filename.endsWith(".pdf"))
            assertFalse(filename.any { it.code < 0x20 })
            assertFalse(filename.any { it in setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|') })
        }
    }

    @Test
    fun repeatedCallsReturnTheSameSuggestion() {
        val first = engine.suggest(DocumentCategory.INVOICE)

        assertEquals(first, engine.suggest(DocumentCategory.INVOICE))
    }

    @Test
    fun unicodeOcrTextAffectsOnlyTheCategoryNotTheCategoryBasedFilename() {
        val category = DocumentClassifier().classify("FACTURĂ\nTVA").category

        assertSuggestion(category, "invoice.pdf")
    }

    @Test
    fun sanitizesInvalidFilenameCharactersWithoutPaths() {
        val filename = engine.sanitizeFilenameCandidate(" invoice / March:2026? \\\\ draft |.PDF ")

        assertEquals("invoice-March-2026-draft.pdf", filename)
        assertFalse(filename.any { it in setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|') })
    }

    @Test
    fun fallsBackWhenCandidateContainsOnlyInvalidCharacters() {
        assertEquals(
            "document.pdf",
            engine.sanitizeFilenameCandidate("/\\:*?\"<>|\u0000"),
        )
    }

    @Test
    fun boundsStemLengthWithoutBreakingThePdfExtension() {
        val filename = engine.sanitizeFilenameCandidate("ă".repeat(100))
        val stem = filename.removeSuffix(".pdf")

        assertTrue(stem.toByteArray(Charsets.UTF_8).size <= 80)
        assertTrue(filename.endsWith(".pdf"))
    }

    @Test
    fun normalizesExtensionToOneLowercasePdfSuffix() {
        assertEquals(
            "invoice.pdf",
            engine.sanitizeFilenameCandidate("invoice.PDF.pdf"),
        )
    }

    private fun assertSuggestion(category: DocumentCategory, expectedFilename: String) {
        assertEquals(
            FilenameSuggestion(filename = expectedFilename, category = category),
            engine.suggest(category),
        )
    }
}
