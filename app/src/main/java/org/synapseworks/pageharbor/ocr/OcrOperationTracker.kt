package org.synapseworks.pageharbor.ocr

/**
 * In-memory ownership token for one Activity-owned OCR operation.
 *
 * OCR engines can be backed by services that do not stop immediately when their caller is
 * cancelled. A token makes a late completion harmless after Discard, a replacement scan, or an
 * Activity recreation without retaining any document data or progress across process death.
 */
class OcrOperationTracker {
    private var currentToken = 0L
    private var completedToken: Long? = null

    fun begin(): Long {
        currentToken++
        completedToken = null
        return currentToken
    }

    fun invalidate() {
        currentToken++
        completedToken = null
    }

    /** Claims the sole completion that is allowed to publish OCR UI state. */
    fun claimCompletion(token: Long): CompletionClaim = when {
        token != currentToken -> CompletionClaim.SUPERSEDED
        completedToken == token -> CompletionClaim.DUPLICATE
        else -> {
            completedToken = token
            CompletionClaim.CLAIMED
        }
    }

    enum class CompletionClaim {
        CLAIMED,
        DUPLICATE,
        SUPERSEDED,
    }
}
