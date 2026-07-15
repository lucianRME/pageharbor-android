package org.synapseworks.pageharbor.document

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri

sealed interface PdfShareIntentResult {
    data class Success(val intent: Intent) : PdfShareIntentResult
    data object NoPdfAvailable : PdfShareIntentResult
    data object InvalidUri : PdfShareIntentResult
}

fun createPdfShareIntent(pdfUri: Uri?): PdfShareIntentResult {
    if (pdfUri == null) return PdfShareIntentResult.NoPdfAvailable
    if (pdfUri.scheme != ContentResolver.SCHEME_CONTENT) return PdfShareIntentResult.InvalidUri

    val intent = Intent(Intent.ACTION_SEND)
        .setType("application/pdf")
        .putExtra(Intent.EXTRA_STREAM, pdfUri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    return PdfShareIntentResult.Success(intent)
}
