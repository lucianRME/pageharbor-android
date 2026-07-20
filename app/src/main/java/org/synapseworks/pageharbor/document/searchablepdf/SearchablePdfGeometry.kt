package org.synapseworks.pageharbor.document.searchablepdf

import org.synapseworks.pageharbor.ocr.OcrTextBounds

data class PdfPageSize(
    val widthPoints: Float,
    val heightPoints: Float,
) {
    init {
        require(widthPoints.isFinite() && widthPoints > 0f)
        require(heightPoints.isFinite() && heightPoints > 0f)
    }
}

data class PdfTextBounds(
    val left: Float,
    val bottom: Float,
    val right: Float,
    val top: Float,
)

/**
 * Converts an upright, top-left-origin OCR rectangle to PDF's bottom-left-origin page space.
 * Boxes are clipped instead of rejected: OCR commonly reaches one or two pixels past an edge.
 */
fun mapOcrBoundsToPdf(
    bounds: OcrTextBounds,
    sourceWidthPx: Int,
    sourceHeightPx: Int,
    pageSize: PdfPageSize,
): PdfTextBounds? {
    require(sourceWidthPx > 0) { "sourceWidthPx must be positive" }
    require(sourceHeightPx > 0) { "sourceHeightPx must be positive" }

    val clippedLeft = bounds.left.coerceIn(0f, sourceWidthPx.toFloat())
    val clippedTop = bounds.top.coerceIn(0f, sourceHeightPx.toFloat())
    val clippedRight = bounds.right.coerceIn(0f, sourceWidthPx.toFloat())
    val clippedBottom = bounds.bottom.coerceIn(0f, sourceHeightPx.toFloat())
    if (clippedRight <= clippedLeft || clippedBottom <= clippedTop) return null

    val scaleX = pageSize.widthPoints / sourceWidthPx
    val scaleY = pageSize.heightPoints / sourceHeightPx
    return PdfTextBounds(
        left = clippedLeft * scaleX,
        bottom = pageSize.heightPoints - (clippedBottom * scaleY),
        right = clippedRight * scaleX,
        top = pageSize.heightPoints - (clippedTop * scaleY),
    )
}
