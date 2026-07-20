package org.synapseworks.pageharbor.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.synapseworks.pageharbor.R
import org.synapseworks.pageharbor.ocr.OcrUiState
import org.synapseworks.pageharbor.ocr.OcrUiError
import org.synapseworks.pageharbor.document.PageExportState
import org.synapseworks.pageharbor.document.PdfSaveState
import org.synapseworks.pageharbor.document.PdfShareState
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfSaveError
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfSaveState
import org.synapseworks.pageharbor.document.searchablepdf.isInProgress
import org.synapseworks.pageharbor.scanner.ScannerSpikeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    result: ScannerSpikeState.ResultSummary,
    pdfSaveState: PdfSaveState,
    pdfShareState: PdfShareState,
    pageExportState: PageExportState,
    ocrUiState: OcrUiState,
    searchablePdfSaveState: SearchablePdfSaveState,
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
    val saving = pdfSaveState == PdfSaveState.ChoosingDestination || pdfSaveState == PdfSaveState.Saving
    val sharing = pdfShareState == PdfShareState.Preparing
    val exporting = pageExportState is PageExportState.ChoosingDestination || pageExportState is PageExportState.Exporting
    val savingSearchablePdf = searchablePdfSaveState.isInProgress()
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.scan_result_title)) }, navigationIcon = {
        TextButton(onClick = onBack) { Text(stringResource(R.string.ocr_back_action)) }
    }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.scan_complete), style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            Text(stringResource(if (result.jpegPageCount == 1) R.string.scan_page_ready else R.string.scan_pages_ready, result.jpegPageCount))
            if (result.hasPdf) Button(modifier = Modifier.fillMaxWidth(), enabled = !saving, onClick = onSavePdf) { Text(stringResource(R.string.pdf_save_action)) }
            if (result.jpegPageCount > 0) OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !savingSearchablePdf, onClick = onSaveSearchablePdf) {
                Text(stringResource(R.string.searchable_pdf_save_action))
            }
            if (result.hasPdf) OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !sharing, onClick = onSharePdf) { Text(stringResource(R.string.pdf_share_action)) }
            if (result.jpegPageCount > 0) OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !exporting, onClick = onExportPages) { Text(stringResource(R.string.page_export_action)) }
            if (result.jpegPageCount > 0) OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = ocrUiState != OcrUiState.Recognizing, onClick = onRecognizeText) {
                Text(stringResource(if (ocrUiState is OcrUiState.Success) R.string.ocr_recognize_again_action else R.string.ocr_recognize_action))
            }
            if (ocrUiState is OcrUiState.Success) TextButton(onClick = onViewRecognizedText) { Text(stringResource(R.string.ocr_view_action)) }
            if (saving) Text(stringResource(R.string.pdf_save_progress))
            if (pdfSaveState == PdfSaveState.Saved) Text(stringResource(R.string.pdf_save_success))
            when (searchablePdfSaveState) {
                SearchablePdfSaveState.Preparing,
                SearchablePdfSaveState.ChoosingDestination,
                -> Text(stringResource(R.string.searchable_pdf_preparing_progress))

                SearchablePdfSaveState.Recognizing -> Text(stringResource(R.string.searchable_pdf_recognizing_progress))
                SearchablePdfSaveState.Generating -> Text(stringResource(R.string.searchable_pdf_generating_progress))
                SearchablePdfSaveState.Saving -> Text(stringResource(R.string.searchable_pdf_saving_progress))
                SearchablePdfSaveState.Saved -> Text(stringResource(R.string.searchable_pdf_save_success))
                SearchablePdfSaveState.Cancelled -> Text(stringResource(R.string.searchable_pdf_cancelled))
                SearchablePdfSaveState.Idle,
                -> Unit

                is SearchablePdfSaveState.Error -> Text(
                    stringResource(
                        when (searchablePdfSaveState.reason) {
                            SearchablePdfSaveError.NO_PAGES -> R.string.searchable_pdf_error_no_pages
                            SearchablePdfSaveError.PREPARATION_FAILED ->
                                R.string.searchable_pdf_error_preparation_failed

                            SearchablePdfSaveError.DESTINATION_UNAVAILABLE ->
                                R.string.searchable_pdf_error_destination_unavailable

                            SearchablePdfSaveError.WRITE_FAILED ->
                                R.string.searchable_pdf_error_write_failed
                        },
                    ),
                )
            }
            if (sharing) Text(stringResource(R.string.pdf_share_progress))
            if (pageExportState is PageExportState.Cancelled) Text(stringResource(R.string.page_export_cancelled))
            if (pageExportState is PageExportState.Completed) Text(stringResource(R.string.page_export_success))
            if (pageExportState is PageExportState.ChoosingDestination) Text(stringResource(R.string.page_export_progress, pageExportState.pageNumber, pageExportState.pageCount))
            if (pageExportState is PageExportState.Exporting) Text(stringResource(R.string.page_export_progress, pageExportState.pageNumber, pageExportState.pageCount))
            if (ocrUiState == OcrUiState.Recognizing) Text(stringResource(R.string.ocr_recognizing_progress))
            if (ocrUiState is OcrUiState.Error) Text(stringResource(when (ocrUiState.reason) {
                OcrUiError.NO_PAGES -> R.string.ocr_error_no_pages
                OcrUiError.ALL_PAGES_FAILED -> R.string.ocr_error_all_pages_failed
                OcrUiError.UNEXPECTED_FAILURE -> R.string.ocr_error_unexpected
            }))
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onScanAgain) { Text(stringResource(R.string.scan_again_action)) }
            TextButton(onClick = onDiscard) { Text(stringResource(R.string.home_clear_scan_result)) }
        }
    }
}
