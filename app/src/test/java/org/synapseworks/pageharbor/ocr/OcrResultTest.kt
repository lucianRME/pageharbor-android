package org.synapseworks.pageharbor.ocr

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class OcrResultTest {
    @Test
    fun plainTextCombinesRecognizedPagesInOrder() {
        val result = OcrResult(
            pages = listOf(
                OcrPageResult(pageIndex = 0, text = "First page"),
                OcrPageResult(pageIndex = 1, text = "Second page"),
            ),
        )

        assertEquals("First page\n\nSecond page", result.plainText)
    }

    @Test
    fun emptyResultHasEmptyPlainText() {
        val result = OcrResult(pages = emptyList())

        assertEquals("", result.plainText)
    }

    @Test
    fun fakeEngineFulfillsTheOcrEngineContract() {
        val firstPage = OcrPage { ByteArrayInputStream(byteArrayOf(1)) }
        val secondPage = OcrPage { ByteArrayInputStream(byteArrayOf(2)) }
        val expected = OcrResult(
            pages = listOf(
                OcrPageResult(pageIndex = 0, text = "One"),
                OcrPageResult(pageIndex = 1, text = "Two"),
            ),
        )
        val engine = FakeOcrEngine(expected)

        val actual = engine.recognize(listOf(firstPage, secondPage))

        assertSame(expected, actual)
        assertEquals(listOf(firstPage, secondPage), engine.receivedPages)
    }

    @Test
    fun failedPageRetainsItsIndexAndDoesNotRemoveEmptyPages() {
        val result = OcrResult(
            pages = listOf(
                OcrPageResult(pageIndex = 0, text = "First"),
                OcrPageResult(pageIndex = 1, text = ""),
                OcrPageResult(
                    pageIndex = 2,
                    text = "",
                    error = OcrPageError.IMAGE_UNREADABLE,
                ),
            ),
        )

        assertEquals(listOf(0, 1, 2), result.pages.map { it.pageIndex })
        assertEquals("First\n\n\n\n", result.plainText)
        assertEquals(OcrPageError.IMAGE_UNREADABLE, result.pages[2].error)
    }

    @Test
    fun layoutIsOptionalAndDoesNotChangeExistingPlainTextBehavior() {
        val result = OcrResult(
            pages = listOf(
                OcrPageResult(pageIndex = 0, text = "Existing text"),
            ),
        )

        assertEquals("Existing text", result.plainText)
        assertEquals(null, result.pages.single().layout)
    }

    @Test
    fun layoutPreservesOrderedUnicodeLinesWithoutPdfSpecificData() {
        val layout = OcrPageLayout(
            imageWidthPx = 1_200,
            imageHeightPx = 1_600,
            rotationDegrees = 90,
            lines = listOf(
                OcrTextLine(
                    text = "Română: ă â î ș ț",
                    bounds = OcrTextBounds(left = 12f, top = 24f, right = 300f, bottom = 56f),
                    confidence = 0.98f,
                ),
                OcrTextLine(
                    text = "Deutsch: ä ö ü ß",
                    bounds = OcrTextBounds(left = 12f, top = 70f, right = 280f, bottom = 102f),
                ),
            ),
        )

        assertEquals(90, layout.rotationDegrees)
        assertEquals(
            listOf("Română: ă â î ș ț", "Deutsch: ä ö ü ß"),
            layout.lines.map { it.text },
        )
        assertEquals(0.98f, layout.lines.first().confidence)
    }

    private class FakeOcrEngine(
        private val result: OcrResult,
    ) : OcrEngine {
        var receivedPages: List<OcrPage>? = null
            private set

        override fun recognize(pages: List<OcrPage>): OcrResult {
            receivedPages = pages
            return result
        }
    }
}
