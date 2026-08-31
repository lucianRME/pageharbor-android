package org.synapseworks.pageharbor.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.synapseworks.pageharbor.ActiveScanPage
import org.synapseworks.pageharbor.image.DocumentFilter

class NormalPdfExportPlanTest {
    @Test
    fun allOriginalPagesUseTheDirectScannerPdfPlan() {
        val plan = normalPdfExportPlan(
            scannedPdfUri = null,
            scanPages = listOf(page(1L), page(2L)),
        )

        assertEquals(NormalPdfExportPlan.DirectScannerPdf(sourceUri = null), plan)
    }

    @Test
    fun oneFilteredPageSelectsRecomposition() {
        val plan = normalPdfExportPlan(null, listOf(page(1L, DocumentFilter.GRAYSCALE)))

        assertEquals(
            NormalPdfExportPlan.RecomposeFromPages(
                listOf(NormalPdfPage(1L, null, DocumentFilter.GRAYSCALE)),
            ),
            plan,
        )
    }

    @Test
    fun mixedAndAllFilteredPagesSelectRecompositionWithTheirOwnFilters() {
        val mixed = normalPdfExportPlan(
            null,
            listOf(
                page(1L),
                page(2L, DocumentFilter.GRAYSCALE),
                page(3L, DocumentFilter.BLACK_AND_WHITE),
            ),
        )
        val allFiltered = normalPdfExportPlan(
            null,
            listOf(page(4L, DocumentFilter.AUTO_ENHANCE), page(5L, DocumentFilter.HIGH_CONTRAST)),
        )

        assertEquals(
            NormalPdfExportPlan.RecomposeFromPages(
                listOf(
                    NormalPdfPage(1L, null, DocumentFilter.ORIGINAL),
                    NormalPdfPage(2L, null, DocumentFilter.GRAYSCALE),
                    NormalPdfPage(3L, null, DocumentFilter.BLACK_AND_WHITE),
                ),
            ),
            mixed,
        )
        assertTrue(allFiltered is NormalPdfExportPlan.RecomposeFromPages)
    }

    @Test
    fun reorderedPagesKeepTheCorrectVisualFilterAndOrder() {
        val reordered = listOf(
            page(3L, DocumentFilter.BLACK_AND_WHITE),
            page(1L),
            page(2L, DocumentFilter.GRAYSCALE),
        )

        assertEquals(
            NormalPdfExportPlan.RecomposeFromPages(
                listOf(
                    NormalPdfPage(3L, null, DocumentFilter.BLACK_AND_WHITE),
                    NormalPdfPage(1L, null, DocumentFilter.ORIGINAL),
                    NormalPdfPage(2L, null, DocumentFilter.GRAYSCALE),
                ),
            ),
            normalPdfExportPlan(null, reordered),
        )
    }

    @Test
    fun revertingTheLastFilteredPageReturnsToTheDirectPath() {
        assertTrue(
            normalPdfExportPlan(null, listOf(page(1L, DocumentFilter.HIGH_CONTRAST)))
                is NormalPdfExportPlan.RecomposeFromPages,
        )

        assertEquals(
            NormalPdfExportPlan.DirectScannerPdf(null),
            normalPdfExportPlan(null, listOf(page(1L))),
        )
    }

    @Test
    fun addedOriginalPageDoesNotChangeTheCorrectSourceDecision() {
        assertEquals(
            NormalPdfExportPlan.DirectScannerPdf(null),
            normalPdfExportPlan(null, listOf(page(1L), page(2L))),
        )
        assertTrue(
            normalPdfExportPlan(
                null,
                listOf(page(1L, DocumentFilter.GRAYSCALE), page(2L)),
            ) is NormalPdfExportPlan.RecomposeFromPages,
        )
    }

    @Test
    fun saveAndShareReceiveTheSamePlanForTheSameActiveSession() {
        val pages = listOf(page(1L), page(2L, DocumentFilter.HIGH_CONTRAST))

        assertEquals(normalPdfExportPlan(null, pages), normalPdfExportPlan(null, pages))
    }

    @Test
    fun planningDoesNotMutateTheSessionPageSourcesOrFilters() {
        val pages = listOf(page(1L, DocumentFilter.GRAYSCALE), page(2L))
        val before = pages.toList()

        normalPdfExportPlan(null, pages)

        assertEquals(before, pages)
    }

    private fun page(
        id: Long,
        filter: DocumentFilter = DocumentFilter.ORIGINAL,
    ): ActiveScanPage = ActiveScanPage(id = id, sourceUri = null, filter = filter)
}
