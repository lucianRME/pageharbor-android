package org.synapseworks.pageharbor.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * On-device OCR implementation backed by ML Kit's bundled Latin recognizer.
 *
 * This synchronous adapter must be called off the main thread, as required by [OcrEngine]. It
 * opens and processes exactly one session page at a time. ML Kit types and failures remain inside
 * this class; callers receive only PageHarbor's in-memory OCR models.
 */
class MlKitOcrEngine : OcrEngine {
    override fun recognize(pages: List<OcrPage>): OcrResult {
        val recognizer = try {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        } catch (_: Exception) {
            return OcrResult(
                pages = pages.mapIndexed { pageIndex, _ ->
                    OcrPageResult(
                        pageIndex = pageIndex,
                        text = "",
                        error = OcrPageError.RECOGNITION_FAILED,
                    )
                },
            )
        }

        return try {
            OcrResult(
                pages = pages.mapIndexed { pageIndex, page ->
                    recognizePage(recognizer, pageIndex, page)
                },
            )
        } finally {
            recognizer.close()
        }
    }

    private fun recognizePage(
        recognizer: com.google.mlkit.vision.text.TextRecognizer,
        pageIndex: Int,
        page: OcrPage,
    ): OcrPageResult {
        val bitmap = decodeBoundedBitmap(page)
        if (bitmap == null) {
            return OcrPageResult(pageIndex = pageIndex, text = "", error = OcrPageError.IMAGE_UNREADABLE)
        }

        return try {
            val result = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)))
            OcrPageResult(
                pageIndex = pageIndex,
                text = result.text,
                layout = OcrPageLayout(
                    imageWidthPx = bitmap.width,
                    imageHeightPx = bitmap.height,
                    // InputImage receives an upright bitmap with zero rotation. Keeping this
                    // explicit prevents a future raw-camera input from silently changing the
                    // searchable-PDF coordinate contract.
                    rotationDegrees = 0,
                    lines = result.textBlocks.flatMap { block ->
                        block.lines.mapNotNull { line ->
                            val bounds = line.boundingBox ?: return@mapNotNull null
                            OcrTextLine(
                                text = line.text,
                                bounds = OcrTextBounds(
                                    left = bounds.left.toFloat(),
                                    top = bounds.top.toFloat(),
                                    right = bounds.right.toFloat(),
                                    bottom = bounds.bottom.toFloat(),
                                ),
                                confidence = null,
                            )
                        }
                    },
                ),
            )
        } catch (_: Exception) {
            OcrPageResult(pageIndex = pageIndex, text = "", error = OcrPageError.RECOGNITION_FAILED)
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Reads JPEG bounds before allocation, then decodes with the policy's power-of-two sample.
     * ARGB_8888 is retained for ML Kit input fidelity; the 2,800 px / 7 MP bounds cap one bitmap
     * at roughly 28 MB. OOM is caught only around the allocating decode operation.
     */
    private fun decodeBoundedBitmap(page: OcrPage): Bitmap? {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        try {
            page.openJpegStream().use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
        } catch (_: Exception) {
            return null
        }

        val sampleSize = OcrBitmapDecodePolicy.calculateInSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
        ) ?: return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return try {
            page.openJpegStream().use { stream ->
                try {
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                } catch (_: OutOfMemoryError) {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
