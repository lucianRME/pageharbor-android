package org.synapseworks.pageharbor

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.synapseworks.pageharbor.document.PageExportState
import org.synapseworks.pageharbor.document.PdfSaveState
import org.synapseworks.pageharbor.document.PdfShareState
import org.synapseworks.pageharbor.scanner.ScannerSpikeState
import org.synapseworks.pageharbor.ocr.OcrPageError
import org.synapseworks.pageharbor.ocr.OcrPageResult
import org.synapseworks.pageharbor.ocr.OcrResult
import org.synapseworks.pageharbor.ocr.OcrUiState
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
    fun privacyAndAboutActionsAreDisplayed() {
        composeTestRule.setContent {
            PageHarborApp()
        }

        composeTestRule.onNodeWithText("How privacy works").assertIsDisplayed()
        composeTestRule.onNodeWithText("About PageHarbor").assertIsDisplayed()
    }

    @Test
    fun debugBuildLabelIsDisplayed() {
        composeTestRule.setContent {
            PageHarborApp()
        }

        composeTestRule.onNodeWithText(
            "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · ${BuildConfig.GIT_REVISION}",
        ).assertIsDisplayed()
        assertTrue(BuildConfig.GIT_REVISION.isNotBlank())
    }

    @Test
    fun clickingScanDocumentInvokesCallback() {
        var scanClickCount = 0

        composeTestRule.setContent {
            PageHarborApp(
                onScanDocument = {
                    scanClickCount += 1
                },
            )
        }

        composeTestRule.onNodeWithText("Scan document").performClick()

        assertEquals(1, scanClickCount)
    }

    @Test
    fun preparingStateDisablesScanActionAndShowsProgress() {
        composeTestRule.setContent {
            PageHarborApp(scannerSpikeState = ScannerSpikeState.Preparing)
        }

        composeTestRule.onNodeWithText("Scan document")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("Preparing scanner…")
            .assertIsDisplayed()
    }

    @Test
    fun successfulSummaryDisplaysPageCountAndPdfAvailability() {
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = ScannerSpikeState.ResultSummary(
                    jpegPageCount = 3,
                    hasPdf = true,
                    pdfPageCount = 3,
                ),
            )
        }

        composeTestRule.onNodeWithText("Scanned pages: 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("PDF result: returned").assertIsDisplayed()
        composeTestRule.onNodeWithText("PDF pages: 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scanner result was received locally.").assertIsDisplayed()
    }

    @Test
    fun savePdfButtonDoesNotAppearWhenPdfIsMissing() {
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = ScannerSpikeState.ResultSummary(
                    jpegPageCount = 1,
                    hasPdf = false,
                    pdfPageCount = null,
                ),
            )
        }

        composeTestRule.onAllNodesWithText("Save PDF").assertCountEquals(0)
    }

    @Test
    fun sharePdfButtonDoesNotAppearWhenPdfIsMissing() {
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = ScannerSpikeState.ResultSummary(
                    jpegPageCount = 1,
                    hasPdf = false,
                    pdfPageCount = null,
                ),
            )
        }

        composeTestRule.onAllNodesWithText("Share PDF").assertCountEquals(0)
    }

    @Test
    fun pdfActionsAppearWhenPdfExists() {
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = ScannerSpikeState.ResultSummary(
                    jpegPageCount = 1,
                    hasPdf = true,
                    pdfPageCount = 1,
                ),
            )
        }

        composeTestRule.onNodeWithText("Save PDF")
            .assertIsDisplayed()
            .assertIsEnabled()
        composeTestRule.onNodeWithText("Share PDF")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun exportPagesButtonDoesNotAppearWhenPagesAreMissing() {
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = ScannerSpikeState.ResultSummary(
                    jpegPageCount = 0,
                    hasPdf = true,
                    pdfPageCount = 1,
                ),
            )
        }

        composeTestRule.onAllNodesWithText("Export Pages").assertCountEquals(0)
    }

    @Test
    fun exportPagesButtonAppearsWhenPagesExist() {
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = ScannerSpikeState.ResultSummary(
                    jpegPageCount = 2,
                    hasPdf = false,
                    pdfPageCount = null,
                ),
            )
        }

        composeTestRule.onNodeWithText("Export Pages")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun clickingSavePdfInvokesCallback() {
        var saveClickCount = 0

        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = ScannerSpikeState.ResultSummary(
                    jpegPageCount = 1,
                    hasPdf = true,
                    pdfPageCount = 1,
                ),
                onSavePdf = {
                    saveClickCount += 1
                },
            )
        }

        composeTestRule.onNodeWithText("Save PDF").performClick()

        assertEquals(1, saveClickCount)
    }

    @Test
    fun clickingSharePdfInvokesCallback() {
        var shareClickCount = 0

        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = ScannerSpikeState.ResultSummary(
                    jpegPageCount = 1,
                    hasPdf = true,
                    pdfPageCount = 1,
                ),
                onSharePdf = {
                    shareClickCount += 1
                },
            )
        }

        composeTestRule.onNodeWithText("Share PDF").performClick()

        assertEquals(1, shareClickCount)
    }

    @Test
    fun clickingExportPagesInvokesCallback() {
        var exportClickCount = 0

        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = ScannerSpikeState.ResultSummary(
                    jpegPageCount = 2,
                    hasPdf = true,
                    pdfPageCount = 2,
                ),
                onExportPages = {
                    exportClickCount += 1
                },
            )
        }

        composeTestRule.onNodeWithText("Export Pages").performClick()

        assertEquals(1, exportClickCount)
    }

    @Test
    fun savingStateDisablesRepeatedClicksAndShowsProgress() {
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = ScannerSpikeState.ResultSummary(
                    jpegPageCount = 1,
                    hasPdf = true,
                    pdfPageCount = 1,
                ),
                pdfSaveState = PdfSaveState.Saving,
            )
        }

        composeTestRule.onNodeWithText("Save PDF")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("Share PDF")
            .assertIsDisplayed()
            .assertIsEnabled()
        composeTestRule.onNodeWithText("Saving PDF…").assertIsDisplayed()
    }

    @Test
    fun destinationPickerStateDisablesRepeatedClicks() {
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = ScannerSpikeState.ResultSummary(
                    jpegPageCount = 1,
                    hasPdf = true,
                    pdfPageCount = 1,
                ),
                pdfSaveState = PdfSaveState.ChoosingDestination,
            )
        }

        composeTestRule.onNodeWithText("Save PDF")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun sharePreparingStateDisablesRepeatedShareClicksAndKeepsSaveEnabled() {
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = ScannerSpikeState.ResultSummary(
                    jpegPageCount = 1,
                    hasPdf = true,
                    pdfPageCount = 1,
                ),
                pdfShareState = PdfShareState.Preparing,
            )
        }

        composeTestRule.onNodeWithText("Share PDF")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("Save PDF")
            .assertIsDisplayed()
            .assertIsEnabled()
        composeTestRule.onNodeWithText("Preparing share…").assertIsDisplayed()
    }

    @Test
    fun pageExportingStateDisablesRepeatedClicksAndShowsProgress() {
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = ScannerSpikeState.ResultSummary(
                    jpegPageCount = 3,
                    hasPdf = true,
                    pdfPageCount = 3,
                ),
                pageExportState = PageExportState.Exporting(
                    pageNumber = 2,
                    pageCount = 3,
                ),
            )
        }

        composeTestRule.onNodeWithText("Export Pages")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("Exporting page 2 of 3…").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save PDF").assertIsEnabled()
        composeTestRule.onNodeWithText("Share PDF").assertIsEnabled()
    }

    @Test
    fun pageDestinationPickerStateDisablesRepeatedClicksAndShowsProgress() {
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = ScannerSpikeState.ResultSummary(
                    jpegPageCount = 2,
                    hasPdf = false,
                    pdfPageCount = null,
                ),
                pageExportState = PageExportState.ChoosingDestination(
                    pageNumber = 1,
                    pageCount = 2,
                ),
            )
        }

        composeTestRule.onNodeWithText("Export Pages")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("Exporting page 1 of 2…").assertIsDisplayed()
    }

    @Test
    fun successMessageAppearsAfterPdfSave() {
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = ScannerSpikeState.ResultSummary(
                    jpegPageCount = 1,
                    hasPdf = true,
                    pdfPageCount = 1,
                ),
                pdfSaveState = PdfSaveState.Saved,
            )
        }

        composeTestRule.onNodeWithText("✓ PDF saved successfully").assertIsDisplayed()
    }

    @Test
    fun successMessageAppearsAfterPageExport() {
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = ScannerSpikeState.ResultSummary(
                    jpegPageCount = 2,
                    hasPdf = false,
                    pdfPageCount = null,
                ),
                pageExportState = PageExportState.Completed(pageCount = 2),
            )
        }

        composeTestRule.onNodeWithText("✓ Pages exported successfully").assertIsDisplayed()
    }

    @Test
    fun cancellationMessageAppearsAndExportCanBeRetried() {
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = ScannerSpikeState.ResultSummary(
                    jpegPageCount = 3,
                    hasPdf = false,
                    pdfPageCount = null,
                ),
                pageExportState = PageExportState.Cancelled(exportedPageCount = 1),
            )
        }

        composeTestRule.onNodeWithText("Page export cancelled.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Export Pages")
            .assertIsDisplayed()
            .assertIsEnabled()
        composeTestRule.onAllNodesWithText("Exporting page 2 of 3…").assertCountEquals(0)
    }

    @Test
    fun cancelledDestinationSelectionReturnsToSaveReadyState() {
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = ScannerSpikeState.ResultSummary(
                    jpegPageCount = 1,
                    hasPdf = true,
                    pdfPageCount = 1,
                ),
                pdfSaveState = PdfSaveState.Idle,
            )
        }

        composeTestRule.onNodeWithText("Save PDF")
            .assertIsDisplayed()
            .assertIsEnabled()
        composeTestRule.onAllNodesWithText("Saving PDF…").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("✓ PDF saved successfully").assertCountEquals(0)
    }

    @Test
    fun clearScanResultInvokesCallback() {
        var clearClickCount = 0

        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = ScannerSpikeState.ResultSummary(
                    jpegPageCount = 1,
                    hasPdf = false,
                    pdfPageCount = null,
                ),
                onClearScanResult = {
                    clearClickCount += 1
                },
            )
        }

        composeTestRule.onNodeWithText("Clear scan result").performClick()

        assertEquals(1, clearClickCount)
    }

    @Test
    fun cancellationFeedbackIsDisplayed() {
        composeTestRule.setContent {
            PageHarborApp(scannerSpikeState = ScannerSpikeState.Cancelled)
        }

        composeTestRule.onNodeWithText("Scan cancelled.").assertIsDisplayed()
    }

    @Test
    fun errorFeedbackIsDisplayed() {
        composeTestRule.setContent {
            PageHarborApp(scannerSpikeState = ScannerSpikeState.Error)
        }

        composeTestRule.onNodeWithText("Document scanner could not be opened.")
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

    @Test
    fun clickingAboutShowsAppAndAttributionInformation() {
        composeTestRule.setContent {
            PageHarborApp()
        }

        composeTestRule.onNodeWithText("About PageHarbor").performClick()

        composeTestRule.onNodeWithText("Private document scanner for Android")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Version: ${BuildConfig.VERSION_NAME}")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Build: ${BuildConfig.VERSION_CODE}")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Git revision: ${BuildConfig.GIT_REVISION}")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Developed by Lucian Irimie")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Published under SynapseWorks")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Open source under Apache License 2.0")
            .assertIsDisplayed()
    }

    @Test
    fun aboutDialogCanBeDismissed() {
        composeTestRule.setContent {
            PageHarborApp()
        }

        composeTestRule.onNodeWithText("About PageHarbor").performClick()
        composeTestRule.onNodeWithText("Close").performClick()

        composeTestRule.onAllNodesWithText("Private document scanner for Android")
            .assertCountEquals(0)
    }

    @Test
    fun viewSourceCodeInvokesCallback() {
        var viewSourceClickCount = 0

        composeTestRule.setContent {
            PageHarborApp(
                onViewSourceCode = {
                    viewSourceClickCount += 1
                },
            )
        }

        composeTestRule.onNodeWithText("About PageHarbor").performClick()
        composeTestRule.onNodeWithText("View source code").performClick()

        assertEquals(1, viewSourceClickCount)
    }

    @Test
    fun recognizeTextDoesNotAppearWhenJpegPagesAreMissing() {
        composeTestRule.setContent {
            PageHarborApp(scannerSpikeState = scanSummary(jpegPageCount = 0))
        }
        composeTestRule.onAllNodesWithText("Recognize Text").assertCountEquals(0)
    }

    @Test
    fun recognizeTextAppearsWhenJpegPagesExist() {
        composeTestRule.setContent {
            PageHarborApp(scannerSpikeState = scanSummary(jpegPageCount = 1))
        }
        composeTestRule.onNodeWithText("Recognize Text").assertIsDisplayed()
    }

    @Test
    fun recognizeTextInvokesCallback() {
        var callCount = 0
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = scanSummary(jpegPageCount = 1),
                onRecognizeText = { callCount += 1 },
            )
        }

        composeTestRule.onNodeWithText("Recognize Text").performClick()

        assertEquals(1, callCount)
    }

    @Test
    fun recognizingStateDisablesOnlyRepeatedOcrAndShowsProgress() {
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = scanSummary(jpegPageCount = 1),
                ocrUiState = OcrUiState.Recognizing,
            )
        }

        composeTestRule.onNodeWithText("Recognize Text").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Recognizing text…").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Save PDF").assertIsEnabled()
        composeTestRule.onNodeWithText("Share PDF").assertIsEnabled()
        composeTestRule.onNodeWithText("Export Pages").assertIsEnabled()
    }

    @Test
    fun ocrSuccessDisplaysOrderedSelectablePreview() {
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = scanSummary(jpegPageCount = 2),
                ocrUiState = OcrUiState.Success(
                    OcrResult(
                        listOf(
                            OcrPageResult(pageIndex = 0, text = "First page text"),
                            OcrPageResult(pageIndex = 1, text = "Second page text"),
                        ),
                    ),
                ),
            )
        }

        composeTestRule.onNodeWithText("Recognized text").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Text found on 2 of 2 pages").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Page 1\nFirst page text\n\nPage 2\nSecond page text")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun emptyOcrSuccessDisplaysSafeEmptyMessage() {
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = scanSummary(jpegPageCount = 1),
                ocrUiState = OcrUiState.Success(
                    OcrResult(listOf(OcrPageResult(pageIndex = 0, text = ""))),
                ),
            )
        }

        composeTestRule.onNodeWithText("No text was recognized in this scan.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun partialOcrSuccessKeepsTextAndShowsSafeWarning() {
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = scanSummary(jpegPageCount = 2),
                ocrUiState = OcrUiState.Success(
                    OcrResult(
                        listOf(
                            OcrPageResult(pageIndex = 0, text = "Readable page"),
                            OcrPageResult(
                                pageIndex = 1,
                                text = "",
                                error = OcrPageError.IMAGE_UNREADABLE,
                            ),
                        ),
                    ),
                ),
            )
        }

        composeTestRule.onNodeWithText("Readable page", substring = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Some pages could not be read.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun clearRecognizedTextPreservesExistingExportActions() {
        var clearCallCount = 0
        composeTestRule.setContent {
            PageHarborApp(
                scannerSpikeState = scanSummary(jpegPageCount = 1),
                ocrUiState = OcrUiState.Success(
                    OcrResult(listOf(OcrPageResult(pageIndex = 0, text = "Recognized"))),
                ),
                onClearRecognizedText = { clearCallCount += 1 },
            )
        }

        composeTestRule.onNodeWithText("Clear recognized text").performScrollTo().performClick()

        assertEquals(1, clearCallCount)
        composeTestRule.onNodeWithText("Save PDF").assertIsEnabled()
        composeTestRule.onNodeWithText("Share PDF").assertIsEnabled()
        composeTestRule.onNodeWithText("Export Pages").assertIsEnabled()
    }

    private fun scanSummary(jpegPageCount: Int) = ScannerSpikeState.ResultSummary(
        jpegPageCount = jpegPageCount,
        hasPdf = true,
        pdfPageCount = 1,
    )
}
