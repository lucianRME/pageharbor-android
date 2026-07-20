package org.synapseworks.pageharbor.ocr

/**
 * Engine-neutral geometry retained only for the active OCR session.
 *
 * All values are in the visually upright OCR bitmap's top-left-origin pixel coordinate system.
 * This model deliberately contains no ML Kit types, URIs, paths, or document metadata.
 */
data class OcrTextBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    init {
        require(left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite())
        require(right >= left) { "right must not precede left" }
        require(bottom >= top) { "bottom must not precede top" }
    }
}

data class OcrTextLine(
    val text: String,
    val bounds: OcrTextBounds,
    val confidence: Float? = null,
) {
    init {
        require(confidence == null || (confidence.isFinite() && confidence in 0f..1f))
    }
}

data class OcrPageLayout(
    val imageWidthPx: Int,
    val imageHeightPx: Int,
    val rotationDegrees: Int = 0,
    val lines: List<OcrTextLine>,
) {
    init {
        require(imageWidthPx > 0) { "imageWidthPx must be positive" }
        require(imageHeightPx > 0) { "imageHeightPx must be positive" }
        require(rotationDegrees in setOf(0, 90, 180, 270)) {
            "rotationDegrees must be a right angle"
        }
    }
}
