package org.synapseworks.pageharbor.ui

import androidx.compose.runtime.Composable
import org.synapseworks.pageharbor.ui.home.HomeScreen
import org.synapseworks.pageharbor.ui.theme.PageHarborTheme

@Composable
fun PageHarborApp() {
    PageHarborTheme {
        HomeScreen()
    }
}
