package org.synapseworks.pageharbor.document

import android.content.ContentResolver
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PdfSharePreparerTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val shareDirectory = File(context.cacheDir, "shared-pdfs")

    @After
    fun cleanUp() {
        shareDirectory.listFiles()?.forEach { it.delete() }
        shareDirectory.delete()
        File(context.cacheDir, TestSourceName).delete()
    }

    @Test
    fun nonContentPdfIsCopiedToReadableContentUri() {
        val expectedBytes = "%PDF-1.4 test document".toByteArray()
        val source = File(context.cacheDir, TestSourceName).apply { writeBytes(expectedBytes) }

        val result = preparePdfForSharing(context, Uri.fromFile(source))

        assertTrue(result is PdfSharePreparationResult.Ready)
        val preparedUri = (result as PdfSharePreparationResult.Ready).uri
        assertEquals(ContentResolver.SCHEME_CONTENT, preparedUri.scheme)
        val actualBytes = context.contentResolver.openInputStream(preparedUri)?.use { it.readBytes() }
        assertArrayEquals(expectedBytes, actualBytes)
    }

    @Test
    fun contentPdfIsReturnedWithoutCreatingCopy() {
        val sourceUri = Uri.parse("content://org.synapseworks.pageharbor.test/document.pdf")
        val filesBefore = shareDirectory.listFiles()?.map { it.name }.orEmpty()

        val result = preparePdfForSharing(context, sourceUri)

        assertEquals(PdfSharePreparationResult.Ready(sourceUri), result)
        assertEquals(filesBefore, shareDirectory.listFiles()?.map { it.name }.orEmpty())
    }

    @Test
    fun missingPdfReturnsSourceMissing() {
        assertSame(
            PdfSharePreparationResult.SourceMissing,
            preparePdfForSharing(context, null),
        )
    }

    @Test
    fun staleCleanupKeepsFreshFilesAndDeletesExpiredFiles() {
        val now = 100_000_000L
        val fresh = createSharedPdf("fresh.pdf", now - 1_000L)
        val stale = createSharedPdf("stale.pdf", now - 24L * 60L * 60L * 1000L)

        deleteStaleSharedPdfs(context.cacheDir, now)

        assertTrue(fresh.exists())
        assertFalse(stale.exists())
    }

    private fun createSharedPdf(name: String, modifiedAt: Long): File {
        shareDirectory.mkdirs()
        return File(shareDirectory, name).apply {
            writeText("test")
            assertTrue(setLastModified(modifiedAt))
        }
    }

    private companion object {
        const val TestSourceName = "share-preparer-source.pdf"
    }
}
