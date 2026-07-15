package org.synapseworks.pageharbor.document

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfShareIntentFactoryTest {
    @Test
    fun createPdfShareIntentBuildsPdfSendIntentWithReadGrant() {
        val pdfUri = Uri.parse("content://org.synapseworks.pageharbor.test/document.pdf")

        val result = createPdfShareIntent(pdfUri)

        assertTrue(result is PdfShareIntentResult.Success)
        val intent = (result as PdfShareIntentResult.Success).intent
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("application/pdf", intent.type)
        assertEquals(
            pdfUri,
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java),
        )
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test
    fun createPdfShareIntentRejectsMissingPdf() {
        val result = createPdfShareIntent(null)

        assertSame(PdfShareIntentResult.NoPdfAvailable, result)
    }

    @Test
    fun createPdfShareIntentRejectsNonContentUri() {
        val result = createPdfShareIntent(Uri.parse("file:///tmp/document.pdf"))

        assertSame(PdfShareIntentResult.InvalidUri, result)
    }
}
