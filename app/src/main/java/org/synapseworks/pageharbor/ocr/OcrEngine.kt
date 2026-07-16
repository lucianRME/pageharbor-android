package org.synapseworks.pageharbor.ocr

import java.io.InputStream

/**
 * Recognizes text from JPEG pages supplied by the active scan session.
 *
 * Implementations must run off the main thread, keep document contents local, and close every
 * stream they open from [OcrPage].
 */
interface OcrEngine {
    fun recognize(pages: List<OcrPage>): OcrResult
}

/**
 * An opaque, session-scoped JPEG source.
 *
 * This boundary deliberately does not expose a URI, path, or scanner-specific type. Callers own
 * the underlying document resource; an engine owns each stream it opens.
 */
fun interface OcrPage {
    fun openJpegStream(): InputStream
}
