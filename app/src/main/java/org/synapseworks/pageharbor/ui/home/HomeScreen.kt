package org.synapseworks.pageharbor.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.synapseworks.pageharbor.R
import org.synapseworks.pageharbor.document.PageExportState
import org.synapseworks.pageharbor.document.PdfSaveState
import org.synapseworks.pageharbor.document.PdfShareState
import org.synapseworks.pageharbor.scanner.ScannerSpikeState
import org.synapseworks.pageharbor.ocr.OcrUiError
import org.synapseworks.pageharbor.ocr.OcrUiState
import org.synapseworks.pageharbor.ocr.canStartOcr
import org.synapseworks.pageharbor.ocr.failedPageCount
import org.synapseworks.pageharbor.ocr.formatOcrPreview
import org.synapseworks.pageharbor.ocr.textFoundPageCount

@Composable
fun HomeScreen(
    snackbarHostState: SnackbarHostState,
    scannerSpikeState: ScannerSpikeState,
    pdfSaveState: PdfSaveState,
    pdfShareState: PdfShareState,
    pageExportState: PageExportState,
    ocrUiState: OcrUiState,
    showDevelopmentStatus: Boolean,
    versionName: String,
    versionCode: Int,
    gitRevision: String,
    showPrivacyInfo: Boolean,
    showAbout: Boolean,
    onScanDocument: () -> Unit,
    onSavePdf: () -> Unit,
    onSharePdf: () -> Unit,
    onExportPages: () -> Unit,
    onRecognizeText: () -> Unit,
    onViewRecognizedText: () -> Unit,
    onClearRecognizedText: () -> Unit,
    onPrivacyInfo: () -> Unit,
    onDismissPrivacyInfo: () -> Unit,
    onAbout: () -> Unit,
    onDismissAbout: () -> Unit,
    onViewSourceCode: () -> Unit,
    onClearScanResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    if (showDevelopmentStatus) {
                        Text(
                            modifier = Modifier.padding(top = 8.dp),
                            text = stringResource(R.string.home_status),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        modifier = Modifier.padding(top = 32.dp),
                        text = stringResource(R.string.home_headline),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = stringResource(R.string.home_supporting_text),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        modifier = Modifier.padding(top = 40.dp),
                        enabled = scannerSpikeState != ScannerSpikeState.Preparing,
                        onClick = onScanDocument,
                    ) {
                        Text(text = stringResource(R.string.home_scan_document))
                    }
                    when (scannerSpikeState) {
                        ScannerSpikeState.Preparing -> {
                            Text(
                                modifier = Modifier.padding(top = 16.dp),
                                text = stringResource(R.string.home_scan_preparing),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                            )
                        }

                        is ScannerSpikeState.ResultSummary -> {
                            ScanResultSummary(
                                modifier = Modifier.padding(top = 24.dp),
                                resultSummary = scannerSpikeState,
                                pdfSaveState = pdfSaveState,
                                pdfShareState = pdfShareState,
                                pageExportState = pageExportState,
                                ocrUiState = ocrUiState,
                                onSavePdf = onSavePdf,
                                onSharePdf = onSharePdf,
                                onExportPages = onExportPages,
                                onRecognizeText = onRecognizeText,
                                onViewRecognizedText = onViewRecognizedText,
                                onClearRecognizedText = onClearRecognizedText,
                                onClearScanResult = onClearScanResult,
                            )
                        }

                        ScannerSpikeState.Idle,
                        ScannerSpikeState.Cancelled,
                        ScannerSpikeState.Error,
                        -> Unit
                    }
                    TextButton(
                        modifier = Modifier.padding(top = 8.dp),
                        onClick = onPrivacyInfo,
                    ) {
                        Text(text = stringResource(R.string.home_privacy_action))
                    }
                    TextButton(
                        onClick = onAbout,
                    ) {
                        Text(text = stringResource(R.string.home_about_action))
                    }
                    Text(
                        modifier = Modifier.padding(top = 32.dp),
                        text = stringResource(R.string.home_footer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    if (showDevelopmentStatus) {
                        Text(
                            modifier = Modifier.padding(top = 16.dp),
                            text = stringResource(
                                R.string.home_debug_build_label,
                                versionName,
                                versionCode,
                                gitRevision,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }

    if (showPrivacyInfo) {
        PrivacyInfoDialog(onDismiss = onDismissPrivacyInfo)
    }

    if (showAbout) {
        AboutDialog(
            showDebugBuildInfo = showDevelopmentStatus,
            versionName = versionName,
            versionCode = versionCode,
            gitRevision = gitRevision,
            onViewSourceCode = onViewSourceCode,
            onDismiss = onDismissAbout,
        )
    }
}

@Composable
private fun ScanResultSummary(
    resultSummary: ScannerSpikeState.ResultSummary,
    pdfSaveState: PdfSaveState,
    pdfShareState: PdfShareState,
    pageExportState: PageExportState,
    ocrUiState: OcrUiState,
    onSavePdf: () -> Unit,
    onSharePdf: () -> Unit,
    onExportPages: () -> Unit,
    onRecognizeText: () -> Unit,
    onViewRecognizedText: () -> Unit,
    onClearRecognizedText: () -> Unit,
    onClearScanResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val saveInProgress = pdfSaveState == PdfSaveState.ChoosingDestination ||
        pdfSaveState == PdfSaveState.Saving
    val shareInProgress = pdfShareState == PdfShareState.Preparing
    val pageExportProgress = when (pageExportState) {
        is PageExportState.ChoosingDestination -> pageExportState
        is PageExportState.Exporting -> pageExportState
        else -> null
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.home_scan_result_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(
                R.string.home_scan_result_jpeg_pages,
                resultSummary.jpegPageCount,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (resultSummary.hasPdf) {
                stringResource(R.string.home_scan_result_pdf_returned)
            } else {
                stringResource(R.string.home_scan_result_pdf_not_returned)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        resultSummary.pdfPageCount?.let { pdfPageCount ->
            Text(
                text = stringResource(R.string.home_scan_result_pdf_pages, pdfPageCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = stringResource(R.string.home_scan_result_local_statement),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        if (resultSummary.hasPdf) {
            Button(
                modifier = Modifier.padding(top = 8.dp),
                enabled = !saveInProgress,
                onClick = onSavePdf,
            ) {
                Text(text = stringResource(R.string.pdf_save_action))
            }
        }
        if (resultSummary.hasPdf || resultSummary.jpegPageCount > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (resultSummary.hasPdf) {
                    OutlinedButton(
                        enabled = !shareInProgress,
                        onClick = onSharePdf,
                    ) {
                        Text(text = stringResource(R.string.pdf_share_action))
                    }
                }
                if (resultSummary.jpegPageCount > 0) {
                    OutlinedButton(
                        enabled = pageExportProgress == null,
                        onClick = onExportPages,
                    ) {
                        Text(text = stringResource(R.string.page_export_action))
                    }
                }
            }
        }
        if (saveInProgress) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(R.string.pdf_save_progress),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        if (shareInProgress) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(R.string.pdf_share_progress),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        if (pageExportProgress != null) {
            val pageNumber = when (pageExportProgress) {
                is PageExportState.ChoosingDestination -> pageExportProgress.pageNumber
                is PageExportState.Exporting -> pageExportProgress.pageNumber
                else -> error("Unexpected page export state")
            }
            val pageCount = when (pageExportProgress) {
                is PageExportState.ChoosingDestination -> pageExportProgress.pageCount
                is PageExportState.Exporting -> pageExportProgress.pageCount
                else -> error("Unexpected page export state")
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(
                        R.string.page_export_progress,
                        pageNumber,
                        pageCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        if (pdfSaveState == PdfSaveState.Saved) {
            Text(
                text = stringResource(R.string.pdf_save_success),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
        if (pageExportState is PageExportState.Completed) {
            Text(
                text = stringResource(R.string.page_export_success),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
        if (pageExportState is PageExportState.Cancelled) {
            Text(
                text = stringResource(R.string.page_export_cancelled),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (resultSummary.jpegPageCount > 0) {
            OutlinedButton(
                enabled = canStartOcr(ocrUiState),
                onClick = onRecognizeText,
            ) {
                Text(
                    text = stringResource(
                        if (ocrUiState is OcrUiState.Success) {
                            R.string.ocr_recognize_again_action
                        } else {
                            R.string.ocr_recognize_action
                        },
                    ),
                )
            }
        }
        OcrResultSection(
            state = ocrUiState,
            onViewRecognizedText = onViewRecognizedText,
            onClearRecognizedText = onClearRecognizedText,
        )
        OutlinedButton(
            modifier = Modifier.padding(top = 8.dp),
            onClick = onClearScanResult,
        ) {
            Text(text = stringResource(R.string.home_clear_scan_result))
        }
    }
}

@Composable
private fun OcrResultSection(
    state: OcrUiState,
    onViewRecognizedText: () -> Unit,
    onClearRecognizedText: () -> Unit,
) {
    when (state) {
        OcrUiState.Idle -> Unit

        OcrUiState.Recognizing -> {
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(R.string.ocr_recognizing_progress),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        is OcrUiState.Error -> {
            val message = when (state.reason) {
                OcrUiError.NO_PAGES -> stringResource(R.string.ocr_error_no_pages)
                OcrUiError.ALL_PAGES_FAILED -> stringResource(R.string.ocr_error_all_pages_failed)
                OcrUiError.UNEXPECTED_FAILURE -> stringResource(R.string.ocr_error_unexpected)
            }
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }

        is OcrUiState.Success -> {
            OutlinedButton(onClick = onViewRecognizedText) {
                Text(text = stringResource(R.string.ocr_view_action))
            }
        }
    }
}

@Composable
private fun PrivacyInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.home_privacy_dialog_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.home_privacy_dialog_local_processing))
                Text(text = stringResource(R.string.home_privacy_dialog_no_cloud))
                Text(text = stringResource(R.string.home_privacy_dialog_user_choice))
                Text(text = stringResource(R.string.home_privacy_dialog_no_tracking))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.home_privacy_dialog_dismiss))
            }
        }
    )
}

@Composable
private fun AboutDialog(
    showDebugBuildInfo: Boolean,
    versionName: String,
    versionCode: Int,
    gitRevision: String,
    onViewSourceCode: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.about_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = stringResource(R.string.about_tagline))
                Text(text = stringResource(R.string.about_version, versionName))
                Text(text = stringResource(R.string.about_build_number, versionCode))
                if (showDebugBuildInfo) {
                    Text(text = stringResource(R.string.about_git_revision, gitRevision))
                }
                Text(text = stringResource(R.string.about_developed_by))
                Text(text = stringResource(R.string.about_published_under))
                Text(text = stringResource(R.string.about_license))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.about_close))
            }
        },
        dismissButton = {
            TextButton(onClick = onViewSourceCode) {
                Text(text = stringResource(R.string.about_view_source))
            }
        },
    )
}
