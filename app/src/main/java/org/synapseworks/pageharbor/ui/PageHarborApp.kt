package org.synapseworks.pageharbor.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import org.synapseworks.pageharbor.BuildConfig
import org.synapseworks.pageharbor.R
import org.synapseworks.pageharbor.document.PageExportResult
import org.synapseworks.pageharbor.document.PageExportState
import org.synapseworks.pageharbor.document.PdfExportResult
import org.synapseworks.pageharbor.document.PdfSaveState
import org.synapseworks.pageharbor.document.PdfShareError
import org.synapseworks.pageharbor.document.PdfShareState
import org.synapseworks.pageharbor.scanner.ScannerSpikeState
import org.synapseworks.pageharbor.ocr.OcrUiState
import org.synapseworks.pageharbor.ui.home.HomeScreen
import org.synapseworks.pageharbor.ui.home.OcrResultScreen
import org.synapseworks.pageharbor.ui.theme.PageHarborTheme

@Composable
fun PageHarborApp(
    scannerSpikeState: ScannerSpikeState = ScannerSpikeState.Idle,
    pdfSaveState: PdfSaveState = PdfSaveState.Idle,
    pdfShareState: PdfShareState = PdfShareState.Idle,
    pageExportState: PageExportState = PageExportState.Idle,
    ocrUiState: OcrUiState = OcrUiState.Idle,
    onScanDocument: () -> Unit = {},
    onSavePdf: () -> Unit = {},
    onSharePdf: () -> Unit = {},
    onExportPages: () -> Unit = {},
    onRecognizeText: () -> Unit = {},
    onClearRecognizedText: () -> Unit = {},
    onCopyRecognizedText: ((String) -> Unit)? = null,
    onViewSourceCode: () -> Unit = {},
    onClearScanResult: () -> Unit = {},
) {
    PageHarborTheme {
        var showOcrResult by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        val copiedMessage = stringResource(R.string.ocr_copied_message)
        var showPrivacyInfo by remember { mutableStateOf(false) }
        var showAbout by remember { mutableStateOf(false) }
        val scanCancelledMessage = stringResource(R.string.home_scan_cancelled)
        val scannerErrorMessage = stringResource(R.string.home_scanner_error)
        val pdfSourceMissingMessage = stringResource(R.string.pdf_save_source_missing)
        val pdfDestinationUnavailableMessage = stringResource(R.string.pdf_save_destination_unavailable)
        val pdfWriteFailedMessage = stringResource(R.string.pdf_save_failed)
        val pdfShareNoPdfMessage = stringResource(R.string.pdf_share_no_pdf)
        val pdfShareTargetUnavailableMessage = stringResource(R.string.pdf_share_target_unavailable)
        val pdfShareInvalidUriMessage = stringResource(R.string.pdf_share_invalid_uri)
        val pdfShareFailedMessage = stringResource(R.string.pdf_share_failed)
        val pageExportSourceMissingMessage = stringResource(R.string.page_export_source_missing)
        val pageExportDestinationUnavailableMessage =
            stringResource(R.string.page_export_destination_unavailable)
        val pageExportFailedMessage = stringResource(R.string.page_export_failed)

        if (showOcrResult && ocrUiState is OcrUiState.Success) {
            OcrResultScreen(
                result = ocrUiState.result,
                snackbarHostState = snackbarHostState,
                onBack = { showOcrResult = false },
                onRecognizeAgain = {
                    showOcrResult = false
                    onRecognizeText()
                },
                onClearRecognizedText = {
                    showOcrResult = false
                    onClearRecognizedText()
                },
                onCopyText = { text ->
                    val copied = if (onCopyRecognizedText != null) {
                        onCopyRecognizedText(text)
                        true
                    } else {
                        copyPlainTextToClipboard(
                            context = context,
                            label = context.getString(R.string.ocr_result_heading),
                            text = text,
                        )
                    }
                    if (copied) {
                        coroutineScope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                    }
                },
            )
        } else HomeScreen(
            snackbarHostState = snackbarHostState,
            scannerSpikeState = scannerSpikeState,
            pdfSaveState = pdfSaveState,
            pdfShareState = pdfShareState,
            pageExportState = pageExportState,
            ocrUiState = ocrUiState,
            showDevelopmentStatus = BuildConfig.DEBUG,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            gitRevision = BuildConfig.GIT_REVISION,
            showPrivacyInfo = showPrivacyInfo,
            showAbout = showAbout,
            onScanDocument = {
                onScanDocument()
            },
            onSavePdf = onSavePdf,
            onSharePdf = onSharePdf,
            onExportPages = onExportPages,
            onRecognizeText = onRecognizeText,
            onViewRecognizedText = { showOcrResult = true },
            onClearRecognizedText = {
                showOcrResult = false
                onClearRecognizedText()
            },
            onPrivacyInfo = {
                showPrivacyInfo = true
            },
            onDismissPrivacyInfo = {
                showPrivacyInfo = false
            },
            onAbout = {
                showAbout = true
            },
            onDismissAbout = {
                showAbout = false
            },
            onViewSourceCode = onViewSourceCode,
            onClearScanResult = onClearScanResult,
        )

        LaunchedEffect(scannerSpikeState) {
            when (scannerSpikeState) {
                ScannerSpikeState.Cancelled -> {
                    snackbarHostState.showSnackbar(scanCancelledMessage)
                }

                ScannerSpikeState.Error -> {
                    snackbarHostState.showSnackbar(scannerErrorMessage)
                }

                ScannerSpikeState.Idle,
                ScannerSpikeState.Preparing,
                is ScannerSpikeState.ResultSummary,
                -> Unit
            }
        }

        LaunchedEffect(pdfSaveState) {
            val message = when (pdfSaveState) {
                is PdfSaveState.Error -> when (pdfSaveState.result) {
                    PdfExportResult.SourceMissing -> pdfSourceMissingMessage
                    PdfExportResult.DestinationUnavailable -> pdfDestinationUnavailableMessage
                    PdfExportResult.WriteFailed -> pdfWriteFailedMessage
                    PdfExportResult.Success -> null
                }

                PdfSaveState.Idle,
                PdfSaveState.ChoosingDestination,
                PdfSaveState.Saving,
                PdfSaveState.Saved,
                -> null
            }

            if (message != null) {
                snackbarHostState.showSnackbar(message)
            }
        }

        LaunchedEffect(pdfShareState) {
            val message = when (pdfShareState) {
                is PdfShareState.Error -> when (pdfShareState.result) {
                    PdfShareError.NoPdfAvailable -> pdfShareNoPdfMessage
                    PdfShareError.ShareTargetUnavailable -> pdfShareTargetUnavailableMessage
                    PdfShareError.InvalidUri -> pdfShareInvalidUriMessage
                    PdfShareError.UnexpectedFailure -> pdfShareFailedMessage
                }

                PdfShareState.Idle,
                PdfShareState.Preparing,
                -> null
            }

            if (message != null) {
                snackbarHostState.showSnackbar(message)
            }
        }

        LaunchedEffect(pageExportState) {
            val message = when (pageExportState) {
                is PageExportState.Error -> when (pageExportState.result) {
                    PageExportResult.SourceMissing -> pageExportSourceMissingMessage
                    PageExportResult.DestinationUnavailable ->
                        pageExportDestinationUnavailableMessage
                    PageExportResult.WriteFailed -> pageExportFailedMessage
                    PageExportResult.Success -> null
                }

                PageExportState.Idle,
                is PageExportState.ChoosingDestination,
                is PageExportState.Exporting,
                is PageExportState.Completed,
                is PageExportState.Cancelled,
                -> null
            }

            if (message != null) {
                snackbarHostState.showSnackbar(message)
            }
        }
    }
}
