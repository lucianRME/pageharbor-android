package org.synapseworks.pageharbor.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.synapseworks.pageharbor.BuildConfig
import org.synapseworks.pageharbor.R
import org.synapseworks.pageharbor.document.PdfExportResult
import org.synapseworks.pageharbor.document.PdfSaveState
import org.synapseworks.pageharbor.document.PdfShareError
import org.synapseworks.pageharbor.document.PdfShareState
import org.synapseworks.pageharbor.scanner.ScannerSpikeState
import org.synapseworks.pageharbor.ui.home.HomeScreen
import org.synapseworks.pageharbor.ui.theme.PageHarborTheme

@Composable
fun PageHarborApp(
    scannerSpikeState: ScannerSpikeState = ScannerSpikeState.Idle,
    pdfSaveState: PdfSaveState = PdfSaveState.Idle,
    pdfShareState: PdfShareState = PdfShareState.Idle,
    onScanDocument: () -> Unit = {},
    onSavePdf: () -> Unit = {},
    onSharePdf: () -> Unit = {},
    onViewSourceCode: () -> Unit = {},
    onClearScanResult: () -> Unit = {},
) {
    PageHarborTheme {
        val snackbarHostState = remember { SnackbarHostState() }
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

        HomeScreen(
            snackbarHostState = snackbarHostState,
            scannerSpikeState = scannerSpikeState,
            pdfSaveState = pdfSaveState,
            pdfShareState = pdfShareState,
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
    }
}
