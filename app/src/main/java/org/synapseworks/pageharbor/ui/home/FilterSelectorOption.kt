package org.synapseworks.pageharbor.ui.home

import androidx.annotation.StringRes
import org.synapseworks.pageharbor.R
import org.synapseworks.pageharbor.image.DocumentFilter

internal data class FilterSelectorOption(
    val filter: DocumentFilter,
    @param:StringRes val labelRes: Int,
    @param:StringRes val contentDescriptionRes: Int,
)

internal val filterSelectorOptions = listOf(
    FilterSelectorOption(DocumentFilter.ORIGINAL, R.string.filter_original, R.string.filter_original),
    FilterSelectorOption(DocumentFilter.AUTO_ENHANCE, R.string.filter_enhance, R.string.filter_enhance),
    FilterSelectorOption(DocumentFilter.GRAYSCALE, R.string.filter_grayscale, R.string.filter_grayscale),
    FilterSelectorOption(
        DocumentFilter.BLACK_AND_WHITE,
        R.string.filter_black_and_white_short,
        R.string.filter_black_and_white,
    ),
    FilterSelectorOption(
        DocumentFilter.HIGH_CONTRAST,
        R.string.filter_high_contrast,
        R.string.filter_high_contrast,
    ),
)
