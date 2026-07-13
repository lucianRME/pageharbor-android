package org.synapseworks.pageharbor.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import org.synapseworks.pageharbor.R
import org.synapseworks.pageharbor.ui.home.HomeScreen
import org.synapseworks.pageharbor.ui.theme.PageHarborTheme

@Composable
fun PageHarborApp() {
    PageHarborTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()
        var showPrivacyInfo by remember { mutableStateOf(false) }
        val scanComingNextMessage = stringResource(R.string.home_scan_coming_next)

        HomeScreen(
            snackbarHostState = snackbarHostState,
            showPrivacyInfo = showPrivacyInfo,
            onScanDocument = {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(scanComingNextMessage)
                }
            },
            onPrivacyInfo = {
                showPrivacyInfo = true
            },
            onDismissPrivacyInfo = {
                showPrivacyInfo = false
            },
        )
    }
}
