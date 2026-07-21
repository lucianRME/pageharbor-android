package org.synapseworks.pageharbor.document.searchablepdf

/**
 * In-memory ownership token for one Activity-owned searchable-PDF operation.
 *
 * A token has one terminal preparation completion. This makes duplicate callbacks and callbacks
 * from a superseded Activity operation harmless without retaining document data in this model.
 */
class SearchablePdfOperationTracker {
    private var currentToken = 0L
    private var completedToken: Long? = null

    fun begin(): Long {
        currentToken++
        completedToken = null
        return currentToken
    }

    fun invalidate() {
        ++currentToken
        completedToken = null
    }

    fun isCurrent(token: Long): Boolean = token == currentToken

    /** True only while [token] owns an operation that has not completed preparation. */
    fun acceptsProgress(token: Long): Boolean = isCurrent(token) && completedToken != token

    /** Claims the one preparation completion that may publish UI state for [token]. */
    fun claimCompletion(token: Long): CompletionClaim = when {
        token != currentToken -> CompletionClaim.SUPERSEDED
        completedToken == token -> CompletionClaim.DUPLICATE
        else -> {
            completedToken = token
            CompletionClaim.CLAIMED
        }
    }

    enum class CompletionClaim {
        /** This is the one current completion permitted to publish prepared output. */
        CLAIMED,
        /** A repeated callback owns nothing and must leave the active prepared output untouched. */
        DUPLICATE,
        /** An old callback must discard only the output it produced. */
        SUPERSEDED,
    }
}
