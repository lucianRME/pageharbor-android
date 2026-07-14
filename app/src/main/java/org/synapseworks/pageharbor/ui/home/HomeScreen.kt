package org.synapseworks.pageharbor.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.synapseworks.pageharbor.R
import org.synapseworks.pageharbor.scanner.ScannerSpikeState

@Composable
fun HomeScreen(
    snackbarHostState: SnackbarHostState,
    scannerSpikeState: ScannerSpikeState,
    showDevelopmentStatus: Boolean,
    versionName: String,
    versionCode: Int,
    gitRevision: String,
    showPrivacyInfo: Boolean,
    showAbout: Boolean,
    onScanDocument: () -> Unit,
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
    onClearScanResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        OutlinedButton(
            modifier = Modifier.padding(top = 8.dp),
            onClick = onClearScanResult,
        ) {
            Text(text = stringResource(R.string.home_clear_scan_result))
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
