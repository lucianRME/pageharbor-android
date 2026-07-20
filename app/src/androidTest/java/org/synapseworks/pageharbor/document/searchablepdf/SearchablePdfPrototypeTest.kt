package org.synapseworks.pageharbor.document.searchablepdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream.AppendMode
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.state.RenderingMode
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local, test-only proof that the Android PDFBox port can retain an image and an invisible Unicode
 * text layer. It uses a test-APK cache file only and deletes it after verification.
 */
class SearchablePdfPrototypeTest {
    @Test
    fun writesAndExtractsInvisibleRomanianAndGermanText() {
        // The generator loads the same PdfBox-Android font asset from the production APK.
        val context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            override fun getApplicationContext(): Context = this
        }
        PDFBoxResourceLoader.init(context)
        val source = fixtureImage()
        val expectedText = listOf("Română: ă â î ș ț", "Deutsch: ä ö ü ß")
        val outputFile = File.createTempFile(
            "searchable-pdf-prototype-",
            ".pdf",
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
        )
        try {
            PDDocument().use { document ->
                val page = PDPage(PDRectangle(source.width.toFloat(), source.height.toFloat()))
                document.addPage(page)
                val font = embeddedLiberationSans(document, context)
                PDPageContentStream(document, page).use { content ->
                    content.drawImage(LosslessFactory.createFromImage(document, source), 0f, 0f)
                }
                PDPageContentStream(document, page, AppendMode.APPEND, true, true).use { content ->
                    content.setRenderingMode(RenderingMode.NEITHER)
                    expectedText.forEachIndexed { index, text ->
                        content.beginText()
                        content.setFont(font, 24f)
                        content.newLineAtOffset(32f, source.height - 56f - (index * 42f))
                        content.showText(text)
                        content.endText()
                    }
                }
                outputFile.outputStream().use(document::save)
            }

            PDDocument.load(outputFile).use { document ->
                assertEquals(expectedText.joinToString(separator = "\n"), PDFTextStripper().getText(document).trim())
                assertEquals(1, document.numberOfPages)
                assertTrue(document.getPage(0).resources.xObjectNames.iterator().hasNext())
            }

            assertTrue(outputFile.length() > 0L)
        } finally {
            check(outputFile.delete()) { "Prototype temporary PDF must be deleted" }
        }
    }

    private fun fixtureImage(): Bitmap = Bitmap.createBitmap(400, 560, Bitmap.Config.ARGB_8888).also { bitmap ->
        Canvas(bitmap).apply {
            drawColor(Color.rgb(245, 247, 250))
            drawRect(12f, 12f, 388f, 92f, Paint().apply { color = Color.rgb(36, 95, 199) })
            drawText(
                "Local image fixture",
                32f,
                60f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(23, 32, 51)
                    textSize = 24f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                },
            )
            Paint().apply {
                color = Color.rgb(160, 170, 184)
                strokeWidth = 2f
                drawLine(32f, 150f, 368f, 150f, this)
                drawLine(32f, 190f, 332f, 190f, this)
                drawLine(32f, 230f, 372f, 230f, this)
                drawLine(32f, 420f, 292f, 420f, this)
            }
        }
    }

    private fun embeddedLiberationSans(document: PDDocument, context: Context): PDType0Font {
        val fontStream = context.assets.open(
            "com/tom_roush/pdfbox/resources/ttf/LiberationSans-Regular.ttf",
        )
        return fontStream.use { PDType0Font.load(document, it, true) }
    }
}
