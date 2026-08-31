package org.synapseworks.pageharbor.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import org.synapseworks.pageharbor.ActiveScanPage
import org.synapseworks.pageharbor.MAX_SCAN_PAGES
import org.synapseworks.pageharbor.R
import org.synapseworks.pageharbor.document.PageExportState
import org.synapseworks.pageharbor.document.PdfSaveState
import org.synapseworks.pageharbor.document.PdfShareState
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfSaveState
import org.synapseworks.pageharbor.document.searchablepdf.isInProgress
import org.synapseworks.pageharbor.image.DocumentFilter
import org.synapseworks.pageharbor.ocr.OcrUiState
import org.synapseworks.pageharbor.scanner.ScannerSpikeState
import org.synapseworks.pageharbor.ui.theme.PageHarborLayout
import org.synapseworks.pageharbor.ui.theme.PageHarborSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    result: ScannerSpikeState.ResultSummary,
    snackbarHostState: SnackbarHostState,
    pdfSaveState: PdfSaveState,
    pdfShareState: PdfShareState,
    pageExportState: PageExportState,
    ocrUiState: OcrUiState,
    searchablePdfSaveState: SearchablePdfSaveState,
    scanPages: List<ActiveScanPage>,
    onPageFilterChange: (Long, DocumentFilter) -> Unit,
    onBack: () -> Unit,
    onSavePdf: () -> Unit,
    onSaveSearchablePdf: () -> Unit,
    onSharePdf: () -> Unit,
    onExportPages: () -> Unit,
    onRecognizeText: () -> Unit,
    onViewRecognizedText: () -> Unit,
    onScanAgain: () -> Unit,
    onDiscard: () -> Unit,
) {
    var selectedPageId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(scanPages) {
        if (scanPages.none { it.id == selectedPageId }) {
            selectedPageId = scanPages.firstOrNull()?.id
        }
    }
    val selectedPageIndex = scanPages.indexOfFirst { it.id == selectedPageId }
        .takeIf { it >= 0 } ?: 0
    val selectedPage = scanPages.getOrNull(selectedPageIndex)
    val displayedPageCount = scanPages.size.takeIf { it > 0 } ?: result.jpegPageCount
    val canAddPages = displayedPageCount < MAX_SCAN_PAGES
    val saving = pdfSaveState == PdfSaveState.ChoosingDestination ||
        pdfSaveState == PdfSaveState.Saving
    val sharing = pdfShareState == PdfShareState.Preparing
    val exporting = pageExportState is PageExportState.ChoosingDestination ||
        pageExportState is PageExportState.Exporting
    val savingSearchablePdf = searchablePdfSaveState.isInProgress()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        modifier = Modifier.semantics { heading() },
                        text = stringResource(R.string.scan_result_title),
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.ocr_back_action))
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = PageHarborLayout.expandedContentMaxWidth)
                    .fillMaxWidth()
                    .padding(
                        horizontal = PageHarborLayout.compactScreenHorizontalPadding,
                        vertical = PageHarborSpacing.large,
                    )
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PageHarborSpacing.medium),
            ) {
                ScanContext(result.jpegPageCount)

                selectedPage?.let { page ->
                    PageEditingSection(
                        page = page,
                        selectedPageIndex = selectedPageIndex,
                        pageCount = scanPages.size,
                        canAddPages = canAddPages,
                        onSelectedPageChange = { index -> selectedPageId = scanPages[index].id },
                        onPageFilterChange = onPageFilterChange,
                        onAddPages = onScanAgain,
                    )
                } ?: PageToolbar(
                    selectedPageIndex = null,
                    pageCount = displayedPageCount,
                    canAddPages = canAddPages,
                    onSelectedPageChange = {},
                    onAddPages = onScanAgain,
                )

                DocumentActionLayer(
                    hasPdf = result.hasPdf,
                    hasPages = result.jpegPageCount > 0,
                    saving = saving,
                    savingSearchablePdf = savingSearchablePdf,
                    sharing = sharing,
                    exporting = exporting,
                    ocrUiState = ocrUiState,
                    onSavePdf = onSavePdf,
                    onSaveSearchablePdf = onSaveSearchablePdf,
                    onSharePdf = onSharePdf,
                    onExportPages = onExportPages,
                    onRecognizeText = onRecognizeText,
                    onViewRecognizedText = onViewRecognizedText,
                )

                OperationStatus(
                    pdfSaveState = pdfSaveState,
                    searchablePdfSaveState = searchablePdfSaveState,
                    sharing = sharing,
                    pageExportState = pageExportState,
                    ocrUiState = ocrUiState,
                )

                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDiscard,
                ) {
                    Text(stringResource(R.string.home_clear_scan_result))
                }
            }
        }
    }
}

@Composable
private fun ScanContext(pageCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.semantics { heading() },
            text = stringResource(R.string.scan_complete),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(
                if (pageCount == 1) R.string.scan_page_ready else R.string.scan_pages_ready,
                pageCount,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PageEditingSection(
    page: ActiveScanPage,
    selectedPageIndex: Int,
    pageCount: Int,
    canAddPages: Boolean,
    onSelectedPageChange: (Int) -> Unit,
    onPageFilterChange: (Long, DocumentFilter) -> Unit,
    onAddPages: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PageHarborSpacing.medium)) {
        if (page.sourceUri != null) {
            FilteredDocumentPreview(
                request = FilteredPreviewRequest(
                    pageId = page.id,
                    sourceKey = page.sourceUri.toString(),
                    filter = page.filter,
                ),
                pageUri = page.sourceUri,
                pageNumber = selectedPageIndex + 1,
                pageCount = pageCount,
                minHeight = PageHarborLayout.editorDocumentPreviewMinHeight,
                maxHeight = PageHarborLayout.editorDocumentPreviewMaxHeight,
            )
        } else {
            Text(stringResource(R.string.scan_preview_unavailable))
        }
        PageToolbar(
            selectedPageIndex = selectedPageIndex,
            pageCount = pageCount,
            canAddPages = canAddPages,
            onSelectedPageChange = onSelectedPageChange,
            onAddPages = onAddPages,
        )
        FilterSelector(
            selectedFilter = page.filter,
            onFilterSelected = { onPageFilterChange(page.id, it) },
        )
    }
}

@Composable
private fun DocumentActionLayer(
    hasPdf: Boolean,
    hasPages: Boolean,
    saving: Boolean,
    savingSearchablePdf: Boolean,
    sharing: Boolean,
    exporting: Boolean,
    onSavePdf: () -> Unit,
    onSaveSearchablePdf: () -> Unit,
    onSharePdf: () -> Unit,
    onExportPages: () -> Unit,
    ocrUiState: OcrUiState,
    onRecognizeText: () -> Unit,
    onViewRecognizedText: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PageHarborSpacing.small)) {
        if (hasPdf) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
                onClick = onSavePdf,
            ) {
                Text(stringResource(R.string.pdf_save_action))
            }
        }
        if (hasPages) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PageHarborSpacing.small),
            ) {
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    enabled = ocrUiState != OcrUiState.Recognizing,
                    onClick = onRecognizeText,
                ) {
                    Text(
                        stringResource(
                            if (ocrUiState is OcrUiState.Success) {
                                R.string.ocr_recognize_again_action
                            } else {
                                R.string.ocr_recognize_action
                            },
                        ),
                    )
                }
                TextButton(
                    modifier = Modifier.weight(1f),
                    enabled = !savingSearchablePdf,
                    onClick = onSaveSearchablePdf,
                ) {
                    Text(stringResource(R.string.searchable_pdf_save_action))
                }
            }
        }
        if (ocrUiState is OcrUiState.Success) {
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onViewRecognizedText,
            ) {
                Text(stringResource(R.string.ocr_view_action))
            }
        }
        if (hasPdf || hasPages) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PageHarborSpacing.small),
            ) {
                if (hasPdf) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        enabled = !sharing,
                        onClick = onSharePdf,
                    ) {
                        Text(stringResource(R.string.pdf_share_action))
                    }
                }
                if (hasPages) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        enabled = !exporting,
                        onClick = onExportPages,
                    ) {
                        Text(stringResource(R.string.page_export_action))
                    }
                }
            }
        }
    }
}

@Composable
private fun PageToolbar(
    selectedPageIndex: Int?,
    pageCount: Int,
    canAddPages: Boolean,
    onSelectedPageChange: (Int) -> Unit,
    onAddPages: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(PageHarborSpacing.compact),
            verticalArrangement = Arrangement.spacedBy(PageHarborSpacing.extraSmall),
        ) {
            if (selectedPageIndex != null && pageCount > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        enabled = selectedPageIndex > 0,
                        onClick = { onSelectedPageChange(selectedPageIndex - 1) },
                    ) {
                        Text(stringResource(R.string.ocr_previous_page_action))
                    }
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        text = stringResource(
                            R.string.ocr_page_indicator,
                            selectedPageIndex + 1,
                            pageCount,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(
                        modifier = Modifier.weight(1f),
                        enabled = selectedPageIndex < pageCount - 1,
                        onClick = { onSelectedPageChange(selectedPageIndex + 1) },
                    ) {
                        Text(stringResource(R.string.ocr_next_page_action))
                    }
                }
            }
            TextButton(
                modifier = Modifier.align(Alignment.End),
                enabled = canAddPages,
                onClick = onAddPages,
            ) {
                Text(stringResource(R.string.scan_again_action))
            }
            if (!canAddPages) {
                Text(
                    text = stringResource(R.string.scan_page_limit_reached),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
@Composable
private fun OperationStatus(
    pdfSaveState: PdfSaveState,
    searchablePdfSaveState: SearchablePdfSaveState,
    sharing: Boolean,
    pageExportState: PageExportState,
    ocrUiState: OcrUiState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PageHarborSpacing.small)) {
        if (pdfSaveState == PdfSaveState.Saving) {
            InlineOperationStatus(R.string.pdf_save_progress)
        }
        when (searchablePdfSaveState) {
            SearchablePdfSaveState.Preparing -> {
                InlineOperationStatus(R.string.searchable_pdf_preparing_progress)
            }

            SearchablePdfSaveState.Recognizing -> {
                InlineOperationStatus(R.string.searchable_pdf_recognizing_progress)
            }

            SearchablePdfSaveState.Generating -> {
                InlineOperationStatus(R.string.searchable_pdf_generating_progress)
            }

            SearchablePdfSaveState.Saving -> {
                InlineOperationStatus(R.string.searchable_pdf_saving_progress)
            }

            SearchablePdfSaveState.Idle,
            SearchablePdfSaveState.ChoosingDestination,
            SearchablePdfSaveState.Saved,
            SearchablePdfSaveState.Cancelled,
            is SearchablePdfSaveState.Error,
            -> Unit
        }
        if (sharing) {
            InlineOperationStatus(R.string.pdf_share_progress)
        }
        when (pageExportState) {
            is PageExportState.Exporting -> InlineOperationStatus(
                R.string.page_export_progress,
                pageExportState.pageNumber,
                pageExportState.pageCount,
            )

            PageExportState.Idle,
            is PageExportState.ChoosingDestination,
            is PageExportState.Cancelled,
            is PageExportState.Completed,
            is PageExportState.Error,
            -> Unit
        }
        when (ocrUiState) {
            OcrUiState.Recognizing -> InlineOperationStatus(R.string.ocr_recognizing_progress)

            OcrUiState.Idle,
            is OcrUiState.Error,
            is OcrUiState.Success,
            -> Unit
        }
    }
}

@Composable
private fun FilterSelector(
    selectedFilter: DocumentFilter,
    onFilterSelected: (DocumentFilter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PageHarborSpacing.extraSmall)) {
        Text(
            text = stringResource(R.string.filter_selector_heading),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(PageHarborSpacing.small),
        ) {
            filterSelectorOptions.forEach { option ->
                val accessibleLabel = stringResource(option.contentDescriptionRes)
                FilterChip(
                    selected = option.filter == selectedFilter,
                    onClick = { onFilterSelected(option.filter) },
                    label = { Text(stringResource(option.labelRes)) },
                    modifier = Modifier.semantics {
                        contentDescription = accessibleLabel
                    },
                )
            }
        }
    }
}
