package org.synapseworks.pageharbor

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfSaveState
import org.synapseworks.pageharbor.ocr.OcrPageResult
import org.synapseworks.pageharbor.ocr.OcrResult
import org.synapseworks.pageharbor.scanner.ScannerSpikeState
import org.synapseworks.pageharbor.ui.PageHarborScreen

class MainActivityLifecycleTest {
    @Test
    fun recreationAndDestructionLeaveTheActivityInASupportedStableState() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
            }
            scenario.moveToState(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun scanResultAndItsActionsRemainAvailableAfterRecreation() {
        val summary = scanSummary(pageCount = 2)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.restoreCompletedSessionForTest(summary)
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                assertEquals(PageHarborScreen.ScanResult, activity.sessionScreenForTest())
                assertEquals(summary, activity.sessionSummaryForTest())
            }
        }
    }

    @Test
    fun completedOcrResultRemainsOnItsScreenAfterRecreation() {
        val summary = scanSummary(pageCount = 1)
        val ocrResult = OcrResult(listOf(OcrPageResult(pageIndex = 0, text = "Deterministic test text")))

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.restoreCompletedSessionForTest(
                    summary = summary,
                    ocrResult = ocrResult,
                    screen = PageHarborScreen.OcrResult,
                )
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                assertEquals(PageHarborScreen.OcrResult, activity.sessionScreenForTest())
                assertEquals(summary, activity.sessionSummaryForTest())
            }
        }
    }

    @Test
    fun activeSearchablePdfStateResetsToRetryableScanResultAfterRecreation() {
        val summary = scanSummary(pageCount = 1)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.restoreCompletedSessionForTest(
                    summary = summary,
                    searchablePdfSaveState = SearchablePdfSaveState.Generating,
                )
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                assertEquals(PageHarborScreen.ScanResult, activity.sessionScreenForTest())
                assertEquals(summary, activity.sessionSummaryForTest())
                assertEquals(SearchablePdfSaveState.Idle, activity.searchablePdfStateForTest())
            }
        }
    }

    @Test
    fun discardAfterRecreationClearsTheRetainedSession() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.restoreCompletedSessionForTest(scanSummary(pageCount = 1))
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                activity.discardForTest()
                assertEquals(PageHarborScreen.Home, activity.sessionScreenForTest())
                assertEquals(ScannerSpikeState.Idle, activity.sessionSummaryForTest())
            }
        }
    }

    @Test
    fun freshScanAfterRecreationReplacesTheOldRetainedSummary() {
        val initial = scanSummary(pageCount = 1)
        val replacement = scanSummary(pageCount = 3)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.restoreCompletedSessionForTest(initial)
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                activity.restoreCompletedSessionForTest(replacement)
                assertEquals(replacement, activity.sessionSummaryForTest())
                assertEquals(PageHarborScreen.ScanResult, activity.sessionScreenForTest())
            }
        }
    }

    private fun scanSummary(pageCount: Int) = ScannerSpikeState.ResultSummary(
        jpegPageCount = pageCount,
        hasPdf = true,
        pdfPageCount = pageCount,
    )
}
