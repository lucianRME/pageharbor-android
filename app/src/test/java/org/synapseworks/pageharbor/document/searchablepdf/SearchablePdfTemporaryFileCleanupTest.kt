package org.synapseworks.pageharbor.document.searchablepdf

import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchablePdfTemporaryFileCleanupTest {
    private lateinit var cacheDirectory: File
    private lateinit var searchablePdfDirectory: File
    private val nowMillis = 2 * 24L * 60L * 60L * 1000L

    @Before
    fun setUp() {
        cacheDirectory = Files.createTempDirectory("pageharbor-cache-").toFile()
        searchablePdfDirectory = File(cacheDirectory, "searchable-pdfs").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        cacheDirectory.deleteRecursively()
    }

    @Test
    fun removesOnlyStaleOwnedSearchablePdfAndVisualImageFiles() {
        val staleOwned = file("searchable-stale.pdf", ageMillis = 24L * 60L * 60L * 1000L)
        val staleVisual = file("searchable-visual-stale.jpg", ageMillis = 24L * 60L * 60L * 1000L)
        val freshOwned = file("searchable-fresh.pdf", ageMillis = 1L)
        val freshVisual = file("searchable-visual-fresh.jpg", ageMillis = 1L)
        val unrelatedPdf = file("other-document.pdf", ageMillis = 24L * 60L * 60L * 1000L)
        val nonPdf = file("searchable-note.txt", ageMillis = 24L * 60L * 60L * 1000L)
        val nestedDirectory = File(searchablePdfDirectory, "searchable-folder.pdf").apply { mkdirs() }

        deleteStaleSearchablePdfs(cacheDirectory, nowMillis)

        assertFalse(staleOwned.exists())
        assertFalse(staleVisual.exists())
        assertTrue(freshOwned.exists())
        assertTrue(freshVisual.exists())
        assertTrue(unrelatedPdf.exists())
        assertTrue(nonPdf.exists())
        assertTrue(nestedDirectory.exists())
    }

    @Test
    fun doesNotDeleteAFileWithFutureTimestamp() {
        val futureOwned = file("searchable-future.pdf", ageMillis = -1L)

        deleteStaleSearchablePdfs(cacheDirectory, nowMillis)

        assertTrue(futureOwned.exists())
    }

    private fun file(name: String, ageMillis: Long): File =
        File(searchablePdfDirectory, name).apply {
            writeText("test")
            setLastModified(nowMillis - ageMillis)
        }
}
