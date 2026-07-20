package org.synapseworks.pageharbor.document.classification

import java.text.Normalizer
import java.util.Locale

/** Broad, non-sensitive categories inferred from active-session OCR text. */
enum class DocumentCategory {
    INVOICE,
    RECEIPT,
    LETTER,
    FORM,
    UNKNOWN,
}

/**
 * Rule-based confidence, not a statistical probability.
 *
 * [LOW] is retained only to describe an inconclusive OCR match. It never produces a category
 * other than [DocumentCategory.UNKNOWN].
 */
enum class DocumentClassificationConfidence {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
}

/** A content-free result from one local, in-memory classification attempt. */
data class ClassificationResult(
    val category: DocumentCategory,
    val confidence: DocumentClassificationConfidence,
)

/**
 * Deterministically classifies active-session OCR text using only immutable local rules.
 *
 * This component does not retain, log, expose, or persist OCR text. It deliberately has no
 * Android, storage, network, UI, or PDF dependency.
 */
class DocumentClassifier {
    fun classify(ocrText: String): ClassificationResult {
        val normalizedText = normalizeForMatching(ocrText)
        if (normalizedText.isEmpty()) {
            return ClassificationResult(DocumentCategory.UNKNOWN, DocumentClassificationConfidence.NONE)
        }

        val scores = rules.groupBy { it.category }
            .mapValues { (_, categoryRules) -> categoryRules.count { it.matches(normalizedText) } }
        val highestScore = scores.values.maxOrNull() ?: 0
        if (highestScore == 0) {
            return ClassificationResult(DocumentCategory.UNKNOWN, DocumentClassificationConfidence.NONE)
        }
        if (highestScore < MinimumCategorySignals) {
            return ClassificationResult(DocumentCategory.UNKNOWN, DocumentClassificationConfidence.LOW)
        }

        val winners = scores.filterValues { it == highestScore }.keys
        if (winners.size != 1) {
            return ClassificationResult(DocumentCategory.UNKNOWN, DocumentClassificationConfidence.LOW)
        }

        return ClassificationResult(
            category = winners.single(),
            confidence = if (highestScore >= HighConfidenceSignals) {
                DocumentClassificationConfidence.HIGH
            } else {
                DocumentClassificationConfidence.MEDIUM
            },
        )
    }

    private fun normalizeForMatching(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .replace("ß", "ss")
        .let { normalized ->
            Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replace(CombiningMarks, "")
        }
        .replace(Whitespace, " ")
        .trim()

    private data class Rule(
        val category: DocumentCategory,
        val phrases: Set<String>,
    ) {
        private val matchPatterns = phrases.map { phrase ->
            Regex("(?<![\\p{L}\\p{N}])${Regex.escape(phrase)}(?![\\p{L}\\p{N}])")
        }

        fun matches(text: String): Boolean = matchPatterns.any { it.containsMatchIn(text) }
    }

    private companion object {
        const val MinimumCategorySignals = 2
        const val HighConfidenceSignals = 3

        val CombiningMarks = Regex("\\p{M}+")
        val Whitespace = Regex("\\s+")

        /*
         * Phrases are stored in the same folded form produced by normalizeForMatching. Each
         * rule is one evidence type, so synonyms and repeated OCR text cannot raise confidence
         * or change precedence.
         */
        val rules = listOf(
            Rule(DocumentCategory.INVOICE, setOf("invoice", "bill", "rechnung", "factura")),
            Rule(DocumentCategory.INVOICE, setOf("vat", "tax", "mwst", "steuer", "tva")),
            Rule(DocumentCategory.INVOICE, setOf("total due", "gesamtbetrag", "total de plata")),

            Rule(DocumentCategory.RECEIPT, setOf("receipt", "kassenbon", "quittung", "bon", "chitanta")),
            Rule(DocumentCategory.RECEIPT, setOf("purchase")),
            Rule(DocumentCategory.RECEIPT, setOf("subtotal")),

            Rule(DocumentCategory.LETTER, setOf("dear", "sehr geehrte", "stimate")),
            Rule(DocumentCategory.LETTER, setOf("sincerely", "regards", "mit freundlichen grussen", "cu stima")),

            Rule(DocumentCategory.FORM, setOf("form", "formular")),
            Rule(DocumentCategory.FORM, setOf("application", "antrag", "cerere")),
            Rule(DocumentCategory.FORM, setOf("please complete")),
        )
    }
}
