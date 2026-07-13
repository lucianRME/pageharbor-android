package org.synapseworks.pageharbor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.synapseworks.pageharbor.ui.PageHarborApp

class HomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pageHarborTitleAndScanButtonAreDisplayed() {
        composeTestRule.setContent {
            PageHarborApp()
        }

        composeTestRule.onNodeWithText("PageHarbor").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scan document").assertIsDisplayed()
    }
}
