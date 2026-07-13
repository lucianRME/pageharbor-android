package org.synapseworks.pageharbor

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.synapseworks.pageharbor.ui.PageHarborApp

class HomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pageHarborTitleIsDisplayed() {
        composeTestRule.setContent {
            PageHarborApp()
        }

        composeTestRule.onNodeWithText("PageHarbor").assertIsDisplayed()
    }

    @Test
    fun scanDocumentButtonIsDisplayedAndEnabled() {
        composeTestRule.setContent {
            PageHarborApp()
        }

        composeTestRule.onNodeWithText("Scan document")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun clickingScanDocumentShowsComingNextSnackbar() {
        composeTestRule.setContent {
            PageHarborApp()
        }

        composeTestRule.onNodeWithText("Scan document").performClick()

        composeTestRule.onNodeWithText("Document scanning is coming next.")
            .assertIsDisplayed()
    }

    @Test
    fun clickingPrivacyActionShowsPrivacyDialog() {
        composeTestRule.setContent {
            PageHarborApp()
        }

        composeTestRule.onNodeWithText("How privacy works").performClick()

        composeTestRule.onNodeWithText("Documents are intended to be processed locally.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("PageHarbor does not operate cloud storage.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Users will choose where exported files are saved or shared.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("No analytics, advertising, or tracking are used.")
            .assertIsDisplayed()
    }

    @Test
    fun privacyDialogCanBeDismissed() {
        composeTestRule.setContent {
            PageHarborApp()
        }

        composeTestRule.onNodeWithText("How privacy works").performClick()
        composeTestRule.onNodeWithText("OK").performClick()

        composeTestRule.onAllNodesWithText("Documents are intended to be processed locally.")
            .assertCountEquals(0)
    }
}
