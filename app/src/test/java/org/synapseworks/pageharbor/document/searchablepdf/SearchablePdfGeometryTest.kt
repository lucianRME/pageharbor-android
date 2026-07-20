package org.synapseworks.pageharbor.document.searchablepdf

import org.synapseworks.pageharbor.ocr.OcrPageLayout
import org.synapseworks.pageharbor.ocr.OcrTextBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchablePdfGeometryTest {
    private val page = PdfPageSize(widthPoints = 612f, heightPoints = 792f)

    @Test
    fun mapsTopLeftOcrBoundsToBottomLeftPdfBounds() {
        val result = mapOcrBoundsToPdf(
            bounds = OcrTextBounds(left = 100f, top = 200f, right = 300f, bottom = 260f),
            sourceWidthPx = 1_200,
            sourceHeightPx = 1_600,
            pageSize = page,
        )!!

        assertEquals(51f, result.left, 0.001f)
        assertEquals(663.3f, result.bottom, 0.001f)
        assertEquals(153f, result.right, 0.001f)
        assertEquals(693f, result.top, 0.001f)
    }

    @Test
    fun scalesLandscapeSourceIndependentlyOnEachAxis() {
        val result = mapOcrBoundsToPdf(
            bounds = OcrTextBounds(left = 800f, top = 100f, right = 1_200f, bottom = 200f),
            sourceWidthPx = 1_600,
            sourceHeightPx = 900,
            pageSize = PdfPageSize(widthPoints = 1_000f, heightPoints = 500f),
        )!!

        assertEquals(500f, result.left, 0.001f)
        assertEquals(388.8889f, result.bottom, 0.001f)
        assertEquals(750f, result.right, 0.001f)
        assertEquals(444.4444f, result.top, 0.001f)
    }

    @Test
    fun clipsSmallOcrOverflowAtPageEdges() {
        val result = mapOcrBoundsToPdf(
            bounds = OcrTextBounds(left = -2f, top = 1_590f, right = 1_205f, bottom = 1_605f),
            sourceWidthPx = 1_200,
            sourceHeightPx = 1_600,
            pageSize = page,
        )!!

        assertEquals(0f, result.left, 0.001f)
        assertEquals(0f, result.bottom, 0.001f)
        assertEquals(612f, result.right, 0.001f)
        assertEquals(4.95f, result.top, 0.001f)
    }

    @Test
    fun clipsBoxesNearEveryImageEdge() {
        val sourceWidth = 100
        val sourceHeight = 200
        val edgeBoxes = listOf(
            OcrTextBounds(left = -1f, top = 40f, right = 10f, bottom = 50f),
            OcrTextBounds(left = 90f, top = 40f, right = 101f, bottom = 50f),
            OcrTextBounds(left = 40f, top = -1f, right = 50f, bottom = 10f),
            OcrTextBounds(left = 40f, top = 190f, right = 50f, bottom = 201f),
        )

        val results = edgeBoxes.map {
            mapOcrBoundsToPdf(
                bounds = it,
                sourceWidthPx = sourceWidth,
                sourceHeightPx = sourceHeight,
                pageSize = PdfPageSize(widthPoints = 100f, heightPoints = 200f),
            )!!
        }

        assertEquals(0f, results[0].left, 0.001f)
        assertEquals(100f, results[1].right, 0.001f)
        assertEquals(200f, results[2].top, 0.001f)
        assertEquals(0f, results[3].bottom, 0.001f)
    }

    @Test
    fun ignoresABoxThatIsEmptyAfterClipping() {
        assertNull(
            mapOcrBoundsToPdf(
                bounds = OcrTextBounds(left = -50f, top = 10f, right = -1f, bottom = 20f),
                sourceWidthPx = 100,
                sourceHeightPx = 100,
                pageSize = page,
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonRightAngleRotation() {
        OcrPageLayout(imageWidthPx = 100, imageHeightPx = 100, rotationDegrees = 45, lines = emptyList())
    }

    @Test
    fun layoutAcceptsTheNormalizedRightAngleRotationContract() {
        assertEquals(
            listOf(0, 90, 180, 270),
            listOf(0, 90, 180, 270).map { rotation ->
                OcrPageLayout(
                    imageWidthPx = 100,
                    imageHeightPx = 200,
                    rotationDegrees = rotation,
                    lines = emptyList(),
                ).rotationDegrees
            },
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFiniteBounds() {
        OcrTextBounds(left = Float.NaN, top = 0f, right = 1f, bottom = 1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroPdfPageWidth() {
        PdfPageSize(widthPoints = 0f, heightPoints = 100f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroPdfPageHeight() {
        PdfPageSize(widthPoints = 100f, heightPoints = 0f)
    }
}
