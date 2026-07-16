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
                OcrPageResult(text = "First page"),
                OcrPageResult(text = "Second page"),
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
                OcrPageResult(text = "One"),
                OcrPageResult(text = "Two"),
            ),
        )
        val engine = FakeOcrEngine(expected)

        val actual = engine.recognize(listOf(firstPage, secondPage))

        assertSame(expected, actual)
        assertEquals(listOf(firstPage, secondPage), engine.receivedPages)
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
