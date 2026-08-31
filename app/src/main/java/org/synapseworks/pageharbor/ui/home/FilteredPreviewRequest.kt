package org.synapseworks.pageharbor.ui.home

import org.synapseworks.pageharbor.image.DocumentFilter

/** Identifies one transient Scan Result preview request. */
internal data class FilteredPreviewRequest(
    val pageId: Long,
    val sourceKey: String,
    val filter: DocumentFilter,
)

/** A completed request may update the UI only when it still matches the selected page and filter. */
internal fun FilteredPreviewRequest.isCurrentFor(currentRequest: FilteredPreviewRequest): Boolean =
    this == currentRequest
