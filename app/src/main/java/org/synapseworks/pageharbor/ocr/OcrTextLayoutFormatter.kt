package org.synapseworks.pageharbor.ocr

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Produces selectable plain text for the OCR result view while retaining the most useful parts of
 * ML Kit's layout: line order, likely paragraph blocks, indentation, and unusually wide word
 * gaps. This is intentionally conservative; it does not attempt table or column reconstruction.
 */
internal object OcrTextLayoutFormatter {
    private const val MaxIndentSpaces = 8
    private const val MaxWordGapSpaces = 4
    private val whitespace = "\\s+".toRegex()

    fun format(layout: OcrPageLayout): String {
        val blocks = layout.blocks.takeIf { it.isNotEmpty() }
            ?: listOf(OcrTextBlock(lines = layout.lines))
        val orderedBlocks = orderByPosition(blocks) { it.positionBounds() }
        val characterWidth = medianCharacterWidth(orderedBlocks)
        val baselineLeft = orderedBlocks.flatMap { it.lines }
            .mapNotNull { it.bounds?.left }
            .minOrNull()
        val typicalLineHeight = median(
            orderedBlocks.flatMap { it.lines }
                .mapNotNull { line -> line.bounds?.let { it.bottom - it.top } }
                .filter { it > 0f },
        )

        val renderedBlocks = orderedBlocks.mapNotNull { block ->
            orderByPosition(block.lines) { it.bounds }
                .mapNotNull { line ->
                    formatLine(line, baselineLeft, characterWidth)?.takeIf { it.isNotBlank() }
                }
                .joinToString(separator = "\n")
                .takeIf { it.isNotBlank() }
                ?.let { block to it }
        }
        if (renderedBlocks.isEmpty()) return ""

        return buildString {
            renderedBlocks.forEachIndexed { index, (block, rendered) ->
                if (index > 0) {
                    val previousBlock = renderedBlocks[index - 1].first
                    append(
                        if (isParagraphGap(previousBlock, block, typicalLineHeight)) "\n\n" else "\n",
                    )
                }
                append(rendered)
            }
        }.trimEnd()
    }

    private fun formatLine(
        line: OcrTextLine,
        baselineLeft: Float?,
        characterWidth: Float?,
    ): String? {
        val content = formatLineContent(line, characterWidth) ?: return null
        val indentation = indentationFor(line, baselineLeft, characterWidth)
        return " ".repeat(indentation) + content
    }

    private fun formatLineContent(line: OcrTextLine, characterWidth: Float?): String? {
        val words = line.elements
            .mapNotNull { element ->
                element.text.trim().takeIf { it.isNotEmpty() }?.let { IndexedElement(element, it) }
            }
        if (words.isEmpty()) {
            return line.text.trim().replace(whitespace, " ").takeIf { it.isNotEmpty() }
        }

        val orderedWords = orderByPosition(words) { it.element.bounds }
        return buildString {
            orderedWords.forEachIndexed { index, word ->
                if (index > 0) {
                    val previous = orderedWords[index - 1]
                    append(" ".repeat(wordGap(previous.element, word.element, characterWidth)))
                }
                append(word.text)
            }
        }
    }

    private fun wordGap(
        previous: OcrTextElement,
        current: OcrTextElement,
        fallbackCharacterWidth: Float?,
    ): Int {
        val previousBounds = previous.bounds ?: return 1
        val currentBounds = current.bounds ?: return 1
        val width = elementCharacterWidth(previous) ?: elementCharacterWidth(current)
            ?: fallbackCharacterWidth
            ?: return 1
        if (width <= 0f) return 1
        return ((currentBounds.left - previousBounds.right) / width)
            .roundToInt()
            .coerceIn(1, MaxWordGapSpaces)
    }

    private fun indentationFor(
        line: OcrTextLine,
        baselineLeft: Float?,
        characterWidth: Float?,
    ): Int {
        val left = line.bounds?.left ?: return 0
        val baseline = baselineLeft ?: return 0
        val width = characterWidth ?: return 0
        val offset = left - baseline
        if (offset < width * 0.75f) return 0
        return (offset / width).roundToInt().coerceIn(0, MaxIndentSpaces)
    }

    private fun medianCharacterWidth(blocks: List<OcrTextBlock>): Float? {
        val fromElements = blocks.flatMap { it.lines }
            .flatMap { it.elements }
            .mapNotNull(::elementCharacterWidth)
        if (fromElements.isNotEmpty()) return median(fromElements)

        return median(
            blocks.flatMap { it.lines }.mapNotNull { line ->
                val bounds = line.bounds ?: return@mapNotNull null
                val characterCount = line.text.count { !it.isWhitespace() }
                ((bounds.right - bounds.left) / characterCount).takeIf { characterCount > 0 && it > 0f }
            },
        )
    }

    private fun elementCharacterWidth(element: OcrTextElement): Float? {
        val bounds = element.bounds ?: return null
        val characterCount = element.text.count { !it.isWhitespace() }
        return ((bounds.right - bounds.left) / characterCount)
            .takeIf { characterCount > 0 && it > 0f }
    }

    private fun isParagraphGap(
        previous: OcrTextBlock,
        current: OcrTextBlock,
        typicalLineHeight: Float?,
    ): Boolean {
        val previousBounds = previous.positionBounds() ?: return false
        val currentBounds = current.positionBounds() ?: return false
        val threshold = max((typicalLineHeight ?: 0f) * 1.5f, 12f)
        return currentBounds.top - previousBounds.bottom >= threshold
    }

    private fun OcrTextBlock.positionBounds(): OcrTextBounds? = bounds ?: lines
        .mapNotNull { it.bounds }
        .takeIf { it.isNotEmpty() }
        ?.let { lineBounds ->
            OcrTextBounds(
                left = lineBounds.minOf { it.left },
                top = lineBounds.minOf { it.top },
                right = lineBounds.maxOf { it.right },
                bottom = lineBounds.maxOf { it.bottom },
            )
        }

    private fun <T> orderByPosition(
        values: List<T>,
        bounds: (T) -> OcrTextBounds?,
    ): List<T> = values.withIndex()
        .sortedWith { first, second ->
            val firstBounds = bounds(first.value)
            val secondBounds = bounds(second.value)
            when {
                firstBounds != null && secondBounds != null ->
                    compareValues(firstBounds.top, secondBounds.top)
                        .takeIf { it != 0 }
                        ?: compareValues(firstBounds.left, secondBounds.left)
                            .takeIf { it != 0 }
                        ?: first.index.compareTo(second.index)
                firstBounds != null -> -1
                secondBounds != null -> 1
                else -> first.index.compareTo(second.index)
            }
        }
        .map { it.value }

    private fun median(values: List<Float>): Float? = values.sorted().let { sorted ->
        when {
            sorted.isEmpty() -> null
            sorted.size % 2 == 1 -> sorted[sorted.size / 2]
            else -> (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2f
        }
    }

    private data class IndexedElement(
        val element: OcrTextElement,
        val text: String,
    )
}

/** Uses geometry when it is available, while preserving the existing flattened-text fallback. */
internal fun OcrPageResult.displayText(): String = layout
    ?.let(OcrTextLayoutFormatter::format)
    ?.takeIf { it.isNotBlank() }
    ?: text
