package org.synapseworks.pageharbor.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrOperationTrackerTest {
    @Test
    fun currentOperationAcceptsExactlyOneCompletion() {
        val tracker = OcrOperationTracker()
        val token = tracker.begin()

        assertEquals(OcrOperationTracker.CompletionClaim.CLAIMED, tracker.claimCompletion(token))
        assertEquals(OcrOperationTracker.CompletionClaim.DUPLICATE, tracker.claimCompletion(token))
    }

    @Test
    fun invalidatedOrReplacedOperationCannotPublishLateCompletion() {
        val tracker = OcrOperationTracker()
        val staleToken = tracker.begin()
        tracker.invalidate()

        assertEquals(OcrOperationTracker.CompletionClaim.SUPERSEDED, tracker.claimCompletion(staleToken))

        val replacementToken = tracker.begin()
        assertEquals(
            OcrOperationTracker.CompletionClaim.CLAIMED,
            tracker.claimCompletion(replacementToken),
        )
    }
}
