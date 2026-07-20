package org.synapseworks.pageharbor.document.classification

import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentClassifierTest {
    private val classifier = DocumentClassifier()

    @Test
    fun classifiesEnglishInvoice() {
        assertClassification(
            text = "INVOICE\nVAT\nTotal due",
            category = DocumentCategory.INVOICE,
            confidence = DocumentClassificationConfidence.HIGH,
        )
    }

    @Test
    fun classifiesGermanInvoice() {
        assertClassification(
            text = "Rechnung\nMwSt\nGesamtbetrag",
            category = DocumentCategory.INVOICE,
            confidence = DocumentClassificationConfidence.HIGH,
        )
    }

    @Test
    fun classifiesRomanianInvoiceWithAndWithoutDiacritics() {
        assertClassification(
            text = "FACTURĂ\nTVA\nTotal de plată",
            category = DocumentCategory.INVOICE,
            confidence = DocumentClassificationConfidence.HIGH,
        )
        assertClassification(
            text = "Factura\nTVA",
            category = DocumentCategory.INVOICE,
            confidence = DocumentClassificationConfidence.MEDIUM,
        )
    }

    @Test
    fun classifiesReceiptExamplesAcrossSupportedLanguages() {
        assertClassification(
            text = "Receipt\nPurchase\nSubtotal",
            category = DocumentCategory.RECEIPT,
            confidence = DocumentClassificationConfidence.HIGH,
        )
        assertClassification(
            text = "Kassenbon\nSubtotal",
            category = DocumentCategory.RECEIPT,
            confidence = DocumentClassificationConfidence.MEDIUM,
        )
        assertClassification(
            text = "Bon fiscal\nSubtotal",
            category = DocumentCategory.RECEIPT,
            confidence = DocumentClassificationConfidence.MEDIUM,
        )
    }

    @Test
    fun classifiesLetterExamplesAcrossSupportedLanguages() {
        assertClassification(
            text = "Dear reader\nRegards",
            category = DocumentCategory.LETTER,
            confidence = DocumentClassificationConfidence.MEDIUM,
        )
        assertClassification(
            text = "Sehr geehrte Damen und Herren\nMit freundlichen Grüßen",
            category = DocumentCategory.LETTER,
            confidence = DocumentClassificationConfidence.MEDIUM,
        )
        assertClassification(
            text = "Stimate doamne\nCu stimă",
            category = DocumentCategory.LETTER,
            confidence = DocumentClassificationConfidence.MEDIUM,
        )
    }

    @Test
    fun classifiesFormExamplesAcrossSupportedLanguages() {
        assertClassification(
            text = "Application form\nPlease complete",
            category = DocumentCategory.FORM,
            confidence = DocumentClassificationConfidence.HIGH,
        )
        assertClassification(
            text = "Formular\nAntrag",
            category = DocumentCategory.FORM,
            confidence = DocumentClassificationConfidence.MEDIUM,
        )
        assertClassification(
            text = "Cerere\nFormular",
            category = DocumentCategory.FORM,
            confidence = DocumentClassificationConfidence.MEDIUM,
        )
    }

    @Test
    fun combinesSupportedLanguagesForOneCategory() {
        assertClassification(
            text = "Rechnung\nVAT\nTotal due",
            category = DocumentCategory.INVOICE,
            confidence = DocumentClassificationConfidence.HIGH,
        )
    }

    @Test
    fun returnsUnknownForUnrelatedText() {
        assertClassification(
            text = "Meeting notes for next week",
            category = DocumentCategory.UNKNOWN,
            confidence = DocumentClassificationConfidence.NONE,
        )
    }

    @Test
    fun returnsUnknownForEmptyOcrText() {
        assertClassification(
            text = "  \n\t ",
            category = DocumentCategory.UNKNOWN,
            confidence = DocumentClassificationConfidence.NONE,
        )
    }

    @Test
    fun returnsUnknownForOneWeakSignal() {
        assertClassification(
            text = "Bill",
            category = DocumentCategory.UNKNOWN,
            confidence = DocumentClassificationConfidence.LOW,
        )
    }

    @Test
    fun doesNotInflateConfidenceForRepeatedOrSynonymousSignals() {
        assertClassification(
            text = "Invoice bill invoice",
            category = DocumentCategory.UNKNOWN,
            confidence = DocumentClassificationConfidence.LOW,
        )
    }

    @Test
    fun returnsUnknownWhenCategoriesTie() {
        assertClassification(
            text = "Invoice VAT\nReceipt subtotal",
            category = DocumentCategory.UNKNOWN,
            confidence = DocumentClassificationConfidence.LOW,
        )
    }

    private fun assertClassification(
        text: String,
        category: DocumentCategory,
        confidence: DocumentClassificationConfidence,
    ) {
        assertEquals(
            ClassificationResult(category, confidence),
            classifier.classify(text),
        )
    }
}
