package org.synapseworks.pageharbor.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrUiStateTest {
    @Test
    fun previewFormatsPagesInOrderIncludingEmptyPages() {
        val result = OcrResult(
            pages = listOf(
                OcrPageResult(pageIndex = 0, text = "First"),
                OcrPageResult(pageIndex = 1, text = ""),
            ),
        )

        assertEquals(
            "Page 1\nFirst\n\nPage 2\nEmpty",
            formatOcrPreview(result, pageHeading = { "Page $it" }, emptyPageText = "Empty"),
        )
    }

    @Test
    fun singlePagePreviewOmitsPageHeadingAndTrailingSeparator() {
        val result = OcrResult(listOf(OcrPageResult(pageIndex = 0, text = "One page")))

        assertEquals(
            "One page",
            formatOcrPreview(result, pageHeading = { "Page $it" }, emptyPageText = "Empty"),
        )
    }

    @Test
    fun allEmptyPagesRemainSuccessfulWithZeroTextFound() {
        val result = OcrResult(
            pages = listOf(
                OcrPageResult(pageIndex = 0, text = ""),
                OcrPageResult(pageIndex = 1, text = ""),
            ),
        )

        assertEquals(OcrUiState.Success(result), ocrStateAfterResult(result))
        assertEquals(0, result.textFoundPageCount())
    }

    @Test
    fun partialFailureKeepsSuccessfulPages() {
        val result = OcrResult(
            pages = listOf(
                OcrPageResult(pageIndex = 0, text = "Readable"),
                OcrPageResult(pageIndex = 1, text = "", error = OcrPageError.IMAGE_UNREADABLE),
            ),
        )

        assertEquals(OcrUiState.Success(result), ocrStateAfterResult(result))
        assertEquals(1, result.failedPageCount())
        assertEquals(1, result.textFoundPageCount())
    }

    @Test
    fun allPageFailuresBecomeSafeError() {
        val result = OcrResult(
            pages = listOf(
                OcrPageResult(pageIndex = 0, text = "", error = OcrPageError.RECOGNITION_FAILED),
            ),
        )

        assertEquals(
            OcrUiState.Error(OcrUiError.ALL_PAGES_FAILED),
            ocrStateAfterResult(result),
        )
    }

    @Test
    fun noPagesBecomesSafeError() {
        assertEquals(
            OcrUiState.Error(OcrUiError.NO_PAGES),
            ocrStateAfterResult(OcrResult(emptyList())),
        )
    }

    @Test
    fun recognizingStatePreventsDuplicateLaunchesAndIdleAllowsNewWork() {
        assertFalse(canStartOcr(OcrUiState.Recognizing))
        assertTrue(canStartOcr(OcrUiState.Idle))
        assertTrue(canStartOcr(OcrUiState.Success(OcrResult(emptyList()))))
    }

    @Test
    fun clearingOrReceivingANewScanReturnsToIdle() {
        val success = OcrUiState.Success(
            OcrResult(listOf(OcrPageResult(pageIndex = 0, text = "Previous scan"))),
        )

        assertEquals(OcrUiState.Idle, clearedOcrState())
        assertEquals(OcrUiState.Idle, clearedOcrState())
        assertTrue(success != clearedOcrState())
    }
}
