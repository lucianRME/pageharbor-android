package org.synapseworks.pageharbor.document.searchablepdf

import android.net.Uri
import org.synapseworks.pageharbor.image.DocumentFilter

/** One original OCR source and its independent, non-destructive visual PDF selection. */
data class SearchablePdfVisualPage(
    val pageId: Long,
    val originalUri: Uri,
    val filter: DocumentFilter = DocumentFilter.ORIGINAL,
)

sealed interface SearchablePdfVisualPlan {
    val pageId: Long
    val ocrSource: SearchablePdfOcrSource

    /** Preserves the existing direct JPEG embedding path for the visual PDF page. */
    data class Original(override val pageId: Long) : SearchablePdfVisualPlan {
        override val ocrSource = SearchablePdfOcrSource.ORIGINAL
    }

    /** Produces one full-resolution temporary JPEG for visual embedding only. */
    data class Filtered(
        override val pageId: Long,
        val filter: DocumentFilter,
    ) : SearchablePdfVisualPlan {
        override val ocrSource = SearchablePdfOcrSource.ORIGINAL
    }
}

/** OCR recognition and its geometry always come from the original scanner image. */
enum class SearchablePdfOcrSource { ORIGINAL }

internal fun searchablePdfVisualPlan(
    pageId: Long,
    filter: DocumentFilter,
): SearchablePdfVisualPlan = when (filter) {
    DocumentFilter.ORIGINAL -> SearchablePdfVisualPlan.Original(pageId)
    else -> SearchablePdfVisualPlan.Filtered(pageId, filter)
}

internal fun searchablePdfVisualPlan(page: SearchablePdfVisualPage): SearchablePdfVisualPlan =
    searchablePdfVisualPlan(page.pageId, page.filter)
