package org.synapseworks.pageharbor.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OcrTextLayoutFormatterTest {
    @Test
    fun normalLineKeepsWordsSeparated() {
        val layout = layoutOf(
            blockOf(
                line(
                    text = "Invoice number 12345",
                    top = 10f,
                    elements = listOf(
                        word("Invoice", 10f, 45f),
                        word("number", 51f, 81f),
                        word("12345", 87f, 112f),
                    ),
                ),
            ),
        )

        assertEquals("Invoice number 12345", OcrTextLayoutFormatter.format(layout))
    }

    @Test
    fun multilineBlockUsesOneNewlinePerDetectedLine() {
        val layout = layoutOf(
            blockOf(
                line("Total EUR 24.50", top = 30f),
                line("Invoice number 12345", top = 10f),
            ),
        )

        assertEquals(
            "Invoice number 12345\nTotal EUR 24.50",
            OcrTextLayoutFormatter.format(layout),
        )
    }

    @Test
    fun significantlyIndentedLineGetsBoundedLeadingSpaces() {
        val layout = layoutOf(
            blockOf(
                line("Base", top = 10f, left = 10f, elements = listOf(word("Base", 10f, 30f))),
                line(
                    "Indented",
                    top = 30f,
                    left = 30f,
                    elements = listOf(word("Indented", 30f, 70f)),
                ),
            ),
        )

        assertEquals("Base\n    Indented", OcrTextLayoutFormatter.format(layout))
    }

    @Test
    fun separatedBlocksGetOneBlankLine() {
        val layout = layoutOf(
            blockOf(line("First paragraph", top = 10f)),
            blockOf(line("Second paragraph", top = 60f)),
        )

        assertEquals("First paragraph\n\nSecond paragraph", OcrTextLayoutFormatter.format(layout))
    }

    @Test
    fun linesAndWordsSortTopToBottomThenLeftToRight() {
        val layout = layoutOf(
            blockOf(
                line(
                    text = "Hello world",
                    top = 30f,
                    elements = listOf(word("world", 40f, 65f), word("Hello", 10f, 35f)),
                ),
                line("Earlier", top = 10f),
            ),
        )

        assertEquals("Earlier\nHello world", OcrTextLayoutFormatter.format(layout))
    }

    @Test
    fun smallCoordinateNoiseDoesNotCreateIndentation() {
        val layout = layoutOf(
            blockOf(
                line("One", top = 10f, left = 10f, elements = listOf(word("One", 10f, 25f))),
                line("Two", top = 30f, left = 12f, elements = listOf(word("Two", 12f, 27f))),
            ),
        )

        assertEquals("One\nTwo", OcrTextLayoutFormatter.format(layout))
    }

    @Test
    fun largeWordGapIsBoundedForLongLines() {
        val layout = layoutOf(
            blockOf(
                line(
                    text = "One Two",
                    top = 10f,
                    elements = listOf(word("One", 10f, 25f), word("Two", 100f, 115f)),
                ),
            ),
        )

        assertEquals("One    Two", OcrTextLayoutFormatter.format(layout))
    }

    @Test
    fun missingBoundsKeepStableSourceOrderAndPlainTextFallback() {
        val layout = OcrPageLayout(
            imageWidthPx = 100,
            imageHeightPx = 100,
            lines = listOf(
                OcrTextLine(text = "First", elements = listOf(OcrTextElement("First"))),
                OcrTextLine(text = "Second", elements = listOf(OcrTextElement("Second"))),
                OcrTextLine(text = "Fallback  text"),
            ),
        )

        assertEquals("First\nSecond\nFallback text", OcrTextLayoutFormatter.format(layout))
    }

    @Test
    fun emptyLayoutAndTextStayEmpty() {
        val page = OcrPageResult(
            pageIndex = 0,
            text = "",
            layout = OcrPageLayout(imageWidthPx = 100, imageHeightPx = 100, lines = emptyList()),
        )

        assertEquals("", page.displayText())
        assertNull(copyableOcrPreview(OcrResult(listOf(page)), { "Page $it" }, "Empty"))
    }

    @Test
    fun copyPreviewUsesTheSameFormattedPlainTextAsTheResultView() {
        val page = OcrPageResult(
            pageIndex = 0,
            text = "Flattened fallback",
            layout = layoutOf(
                blockOf(
                    line("Second", top = 30f),
                    line("First", top = 10f),
                ),
            ),
        )
        val result = OcrResult(listOf(page))

        assertEquals("First\nSecond", page.displayText())
        assertEquals("First\nSecond", copyableOcrPreview(result, { "Page $it" }, "Empty"))
    }

    @Test
    fun formattingTheSameLayoutTwiceHasTheSameOutput() {
        val layout = layoutOf(blockOf(line("Stable", top = 10f)))

        assertEquals(OcrTextLayoutFormatter.format(layout), OcrTextLayoutFormatter.format(layout))
    }

    private fun layoutOf(vararg blocks: OcrTextBlock): OcrPageLayout = OcrPageLayout(
        imageWidthPx = 200,
        imageHeightPx = 200,
        lines = blocks.flatMap { it.lines },
        blocks = blocks.toList(),
    )

    private fun blockOf(vararg lines: OcrTextLine): OcrTextBlock = OcrTextBlock(lines = lines.toList())

    private fun line(
        text: String,
        top: Float,
        left: Float = 10f,
        elements: List<OcrTextElement> = emptyList(),
    ): OcrTextLine = OcrTextLine(
        text = text,
        bounds = OcrTextBounds(left = left, top = top, right = left + 100f, bottom = top + 10f),
        elements = elements,
    )

    private fun word(text: String, left: Float, right: Float): OcrTextElement = OcrTextElement(
        text = text,
        bounds = OcrTextBounds(left = left, top = 0f, right = right, bottom = 10f),
    )
}
