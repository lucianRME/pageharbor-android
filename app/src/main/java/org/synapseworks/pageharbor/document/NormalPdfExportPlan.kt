package org.synapseworks.pageharbor.document

import android.net.Uri
import org.synapseworks.pageharbor.ActiveScanPage
import org.synapseworks.pageharbor.image.DocumentFilter

/** One source page for a normal image-only PDF recomposition. */
data class NormalPdfPage(
    val pageId: Long,
    val sourceUri: Uri?,
    val filter: DocumentFilter,
)

/** The single source decision shared by normal PDF save and share. */
sealed interface NormalPdfExportPlan {
    /** Existing scanner PDF, retained without decoding or recomposition. */
    data class DirectScannerPdf(val sourceUri: Uri?) : NormalPdfExportPlan

    /** Ordered page images that must be rendered into a new normal image-only PDF. */
    data class RecomposeFromPages(val pages: List<NormalPdfPage>) : NormalPdfExportPlan
}

fun normalPdfExportPlan(
    scannedPdfUri: Uri?,
    scanPages: List<ActiveScanPage>,
): NormalPdfExportPlan = if (scanPages.all { it.filter == DocumentFilter.ORIGINAL }) {
    NormalPdfExportPlan.DirectScannerPdf(scannedPdfUri)
} else {
    NormalPdfExportPlan.RecomposeFromPages(
        scanPages.map { page ->
            NormalPdfPage(page.id, page.sourceUri, page.filter)
        },
    )
}
