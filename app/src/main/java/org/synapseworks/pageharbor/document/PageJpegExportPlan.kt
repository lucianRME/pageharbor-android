package org.synapseworks.pageharbor.document

import org.synapseworks.pageharbor.ActiveScanPage
import org.synapseworks.pageharbor.image.DocumentFilter

/** Chooses the export operation without mutating the source scan or its session-only filter state. */
sealed interface PageJpegExportPlan {
    val pageId: Long

    /** Preserves the original scanner JPEG bytes exactly. */
    data class DirectCopy(override val pageId: Long) : PageJpegExportPlan

    /** Decodes the original at full resolution and creates a new filtered JPEG. */
    data class Filtered(
        override val pageId: Long,
        val filter: DocumentFilter,
    ) : PageJpegExportPlan
}

fun pageJpegExportPlan(page: ActiveScanPage): PageJpegExportPlan =
    pageJpegExportPlan(page.id, page.filter)

internal fun pageJpegExportPlan(pageId: Long, filter: DocumentFilter): PageJpegExportPlan = when (filter) {
    DocumentFilter.ORIGINAL -> PageJpegExportPlan.DirectCopy(pageId)
    else -> PageJpegExportPlan.Filtered(pageId, filter)
}
