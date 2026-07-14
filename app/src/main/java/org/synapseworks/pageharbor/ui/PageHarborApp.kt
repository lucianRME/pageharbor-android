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
import org.synapseworks.pageharbor.scanner.ScannerSpikeState
import org.synapseworks.pageharbor.ui.home.HomeScreen
import org.synapseworks.pageharbor.ui.theme.PageHarborTheme

@Composable
fun PageHarborApp(
    scannerSpikeState: ScannerSpikeState = ScannerSpikeState.Idle,
    onScanDocument: () -> Unit = {},
    onViewSourceCode: () -> Unit = {},
    onClearScanResult: () -> Unit = {},
) {
    PageHarborTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        var showPrivacyInfo by remember { mutableStateOf(false) }
        var showAbout by remember { mutableStateOf(false) }
        val scanCancelledMessage = stringResource(R.string.home_scan_cancelled)
        val scannerErrorMessage = stringResource(R.string.home_scanner_error)

        HomeScreen(
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
    }
}
