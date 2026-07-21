package org.synapseworks.pageharbor.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.isSystemInDarkTheme
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
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfSaveError
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfSaveState
import org.synapseworks.pageharbor.scanner.ScannerSpikeState
import org.synapseworks.pageharbor.ocr.OcrUiState
import org.synapseworks.pageharbor.ui.home.HomeScreen
import org.synapseworks.pageharbor.ui.home.OcrResultScreen
import org.synapseworks.pageharbor.ui.home.ScanResultScreen
import org.synapseworks.pageharbor.ui.theme.PageHarborTheme

@Composable
fun PageHarborApp(
    screen: PageHarborScreen = PageHarborScreen.Home,
    onScreenChange: (PageHarborScreen) -> Unit = {},
    autoNavigateToScanResult: Boolean = true,
    scannerSpikeState: ScannerSpikeState = ScannerSpikeState.Idle,
    pdfSaveState: PdfSaveState = PdfSaveState.Idle,
    pdfShareState: PdfShareState = PdfShareState.Idle,
    pageExportState: PageExportState = PageExportState.Idle,
    ocrUiState: OcrUiState = OcrUiState.Idle,
    searchablePdfSaveState: SearchablePdfSaveState = SearchablePdfSaveState.Idle,
    onScanDocument: () -> Unit = {},
    onSavePdf: () -> Unit = {},
    onSaveSearchablePdf: () -> Unit = {},
    onSharePdf: () -> Unit = {},
    onExportPages: () -> Unit = {},
    onRecognizeText: () -> Unit = {},
    onClearRecognizedText: () -> Unit = {},
    onCopyRecognizedText: ((String) -> Unit)? = null,
    onViewSourceCode: () -> Unit = {},
    onClearScanResult: () -> Unit = {},
    darkTheme: Boolean = isSystemInDarkTheme(),
) {
    PageHarborTheme(darkTheme = darkTheme) {
        var currentScreen by remember { mutableStateOf(screen) }
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
        val searchablePdfNoPagesMessage = stringResource(R.string.searchable_pdf_error_no_pages)
        val searchablePdfPreparationFailedMessage = stringResource(R.string.searchable_pdf_error_preparation_failed)
        val searchablePdfDestinationUnavailableMessage =
            stringResource(R.string.searchable_pdf_error_destination_unavailable)
        val searchablePdfWriteFailedMessage = stringResource(R.string.searchable_pdf_error_write_failed)
        val pdfShareNoPdfMessage = stringResource(R.string.pdf_share_no_pdf)
        val pdfShareTargetUnavailableMessage = stringResource(R.string.pdf_share_target_unavailable)
        val pdfShareInvalidUriMessage = stringResource(R.string.pdf_share_invalid_uri)
        val pdfShareFailedMessage = stringResource(R.string.pdf_share_failed)
        val pageExportSourceMissingMessage = stringResource(R.string.page_export_source_missing)
        val pageExportDestinationUnavailableMessage =
            stringResource(R.string.page_export_destination_unavailable)
        val pageExportFailedMessage = stringResource(R.string.page_export_failed)

        fun navigateTo(target: PageHarborScreen) {
            currentScreen = target
            onScreenChange(target)
        }

        LaunchedEffect(screen) {
            currentScreen = screen
        }

        when {
        currentScreen == PageHarborScreen.OcrResult && ocrUiState is OcrUiState.Success -> {
            OcrResultScreen(
                result = ocrUiState.result,
                snackbarHostState = snackbarHostState,
                onBack = { navigateTo(PageHarborScreen.ScanResult) },
                onRecognizeAgain = {
                    navigateTo(PageHarborScreen.ScanResult)
                    onRecognizeText()
                },
                onClearRecognizedText = {
                    navigateTo(PageHarborScreen.ScanResult)
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
        }
        currentScreen == PageHarborScreen.ScanResult &&
            scannerSpikeState is ScannerSpikeState.ResultSummary -> ScanResultScreen(
            result = scannerSpikeState,
            pdfSaveState = pdfSaveState,
            pdfShareState = pdfShareState,
            pageExportState = pageExportState,
            ocrUiState = ocrUiState,
            searchablePdfSaveState = searchablePdfSaveState,
            onBack = { navigateTo(PageHarborScreen.Home) },
            onSavePdf = onSavePdf,
            onSaveSearchablePdf = onSaveSearchablePdf,
            onSharePdf = onSharePdf,
            onExportPages = onExportPages,
            onRecognizeText = onRecognizeText,
            onViewRecognizedText = { navigateTo(PageHarborScreen.OcrResult) },
            onScanAgain = onScanDocument,
            onDiscard = { navigateTo(PageHarborScreen.Home); onClearScanResult() },
        )
        else -> HomeScreen(
            snackbarHostState = snackbarHostState,
            scannerSpikeState = scannerSpikeState,
            showDevelopmentStatus = BuildConfig.DEBUG,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            gitRevision = BuildConfig.GIT_REVISION,
            showPrivacyInfo = showPrivacyInfo,
            showAbout = showAbout,
            onScanDocument = {
                onScanDocument()
            },
            onViewScanResult = { navigateTo(PageHarborScreen.ScanResult) },
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
        )
        }

        LaunchedEffect(scannerSpikeState) {
            when (scannerSpikeState) {
                is ScannerSpikeState.ResultSummary -> {
                    if (autoNavigateToScanResult) navigateTo(PageHarborScreen.ScanResult)
                }
                ScannerSpikeState.Cancelled -> {
                    snackbarHostState.showSnackbar(scanCancelledMessage)
                }

                ScannerSpikeState.Error -> {
                    snackbarHostState.showSnackbar(scannerErrorMessage)
                }

                ScannerSpikeState.Idle,
                ScannerSpikeState.Preparing,
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

        LaunchedEffect(searchablePdfSaveState) {
            val message = when (searchablePdfSaveState) {
                is SearchablePdfSaveState.Error -> when (searchablePdfSaveState.reason) {
                    SearchablePdfSaveError.NO_PAGES -> searchablePdfNoPagesMessage
                    SearchablePdfSaveError.PREPARATION_FAILED -> searchablePdfPreparationFailedMessage
                    SearchablePdfSaveError.DESTINATION_UNAVAILABLE -> searchablePdfDestinationUnavailableMessage
                    SearchablePdfSaveError.WRITE_FAILED -> searchablePdfWriteFailedMessage
                }

                SearchablePdfSaveState.Idle,
                SearchablePdfSaveState.Preparing,
                SearchablePdfSaveState.Recognizing,
                SearchablePdfSaveState.Generating,
                SearchablePdfSaveState.ChoosingDestination,
                SearchablePdfSaveState.Saving,
                SearchablePdfSaveState.Saved,
                SearchablePdfSaveState.Cancelled,
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
