package org.synapseworks.pageharbor

import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.synapseworks.pageharbor.document.searchablepdf.LocalSearchablePdfExportCoordinator
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfGenerationResult
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfGenerator
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfRequest
import org.synapseworks.pageharbor.document.searchablepdf.SearchablePdfSaveState
import org.synapseworks.pageharbor.ocr.OcrEngine
import org.synapseworks.pageharbor.ocr.OcrPage
import org.synapseworks.pageharbor.ocr.OcrPageResult
import org.synapseworks.pageharbor.ocr.OcrResult
import org.synapseworks.pageharbor.ocr.OcrUiState
import org.synapseworks.pageharbor.scanner.ScannerSpikeState
import org.synapseworks.pageharbor.ui.PageHarborScreen

/**
 * Exercises real Activity ownership with fakes paused at an explicit test-only gate. The gate
 * lives in androidTest, receives no document data, and has bounded waits only in test code.
 */
class ActiveOperationInterruptionTest {
    @Test
    fun pausedOcrCompletesOnceAfterBackgroundAndForeground() {
        val gate = ControllableOperationGate()
        val terminalState = CountDownLatch(1)
        val result = ocrResult("ocr-background-token")

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.replaceOperationsForTest(
                    ocrEngine = GatedOcrEngine(gate, result),
                    onOcrTerminalState = terminalState::countDown,
                )
                activity.restoreCompletedSessionForTest(
                    summary = scanSummary(),
                    pageUris = listOf(testPageUri()),
                )
                activity.recognizeTextForTest()
            }

            gate.awaitReached()
            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            gate.release()

            assertTrue("OCR completion was not delivered", terminalState.await(10, TimeUnit.SECONDS))
            scenario.onActivity { activity ->
                assertEquals(OcrUiState.Success(result), activity.ocrStateForTest())
                assertEquals(PageHarborScreen.ScanResult, activity.sessionScreenForTest())
            }
        }
    }

    @Test
    fun pausedOcrIsIgnoredAfterDiscardAndGateRelease() {
        val gate = ControllableOperationGate()
        val terminalStateCount = AtomicInteger(0)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.replaceOperationsForTest(
                    ocrEngine = GatedOcrEngine(gate, ocrResult("stale-ocr-token")),
                    onOcrTerminalState = { terminalStateCount.incrementAndGet() },
                )
                activity.restoreCompletedSessionForTest(scanSummary(), pageUris = listOf(testPageUri()))
                activity.recognizeTextForTest()
            }

            gate.awaitReached()
            scenario.onActivity { it.discardForTest() }
            gate.release()
            gate.awaitExited()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onActivity { activity ->
                assertEquals(PageHarborScreen.Home, activity.sessionScreenForTest())
                assertEquals(OcrUiState.Idle, activity.ocrStateForTest())
            }
            assertEquals(0, terminalStateCount.get())
        }
    }

    @Test
    fun pausedOcrRecreationResetsToScanResultAndIgnoresLateCompletion() {
        val gate = ControllableOperationGate()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.replaceOperationsForTest(ocrEngine = GatedOcrEngine(gate, ocrResult("rotation")))
                activity.restoreCompletedSessionForTest(scanSummary(), pageUris = listOf(testPageUri()))
                activity.recognizeTextForTest()
            }

            gate.awaitReached()
            scenario.recreate()
            gate.release()
            gate.awaitExited()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onActivity { activity ->
                assertEquals(PageHarborScreen.ScanResult, activity.sessionScreenForTest())
                assertEquals(OcrUiState.Idle, activity.ocrStateForTest())
            }
        }
    }

    @Test
    fun pausedSearchablePdfCompletesWithOneDestinationRequestAfterBackgroundAndForeground() {
        runBlocking {
        val gate = ControllableOperationGate()
        val destinationRequests = CountDownLatch(1)
        var requestCount = 0

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.replaceOperationsForTest(
                    searchablePdfExportCoordinator = gatedCoordinator(gate),
                    onSearchablePdfDestinationRequested = {
                        requestCount++
                        destinationRequests.countDown()
                    },
                )
                activity.restoreCompletedSessionForTest(
                    summary = scanSummary(),
                    ocrResult = ocrResult("searchable-background-token"),
                    pageUris = listOf(testPageUri()),
                )
                activity.saveSearchablePdfForTest()
            }

            gate.awaitReached()
            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            gate.release()

            assertTrue("Destination picker was not requested", destinationRequests.await(10, TimeUnit.SECONDS))
            scenario.onActivity { activity ->
                assertEquals(1, requestCount)
                assertEquals(SearchablePdfSaveState.ChoosingDestination, activity.searchablePdfStateForTest())
            }
        }
    }
    }

    @Test
    fun pausedSearchablePdfDiscardCleansPreparedOutputAndDoesNotRequestDestination() {
        runBlocking {
        val gate = ControllableOperationGate()
        val deleted = CountDownLatch(1)
        val destinationRequestCount = AtomicInteger(0)
        var generatedFile: File? = null

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.replaceOperationsForTest(
                    searchablePdfExportCoordinator = gatedCoordinator(
                        gate = gate,
                        onCreated = { generatedFile = it },
                        onDeleted = deleted::countDown,
                    ),
                    onSearchablePdfDestinationRequested = { destinationRequestCount.incrementAndGet() },
                )
                activity.restoreCompletedSessionForTest(
                    summary = scanSummary(),
                    ocrResult = ocrResult("searchable-stale-token"),
                    pageUris = listOf(testPageUri()),
                )
                activity.saveSearchablePdfForTest()
            }

            gate.awaitReached()
            scenario.onActivity { it.discardForTest() }
            gate.release()

            assertTrue("Prepared temporary output was not cleaned", deleted.await(10, TimeUnit.SECONDS))
            assertFalse(generatedFile?.exists() ?: true)
            assertEquals(0, destinationRequestCount.get())
            scenario.onActivity { activity ->
                assertEquals(PageHarborScreen.Home, activity.sessionScreenForTest())
                assertEquals(SearchablePdfSaveState.Idle, activity.searchablePdfStateForTest())
            }
        }
    }
    }

    private fun gatedCoordinator(
        gate: ControllableOperationGate,
        onCreated: (File) -> Unit = {},
        onDeleted: () -> Unit = {},
    ) = LocalSearchablePdfExportCoordinator(
        context = InstrumentationRegistry.getInstrumentation().targetContext,
        ocrEngine = object : OcrEngine {
            override fun recognize(pages: List<OcrPage>): OcrResult =
                error("Existing deterministic OCR must be supplied")
        },
        generator = GatedGenerator(gate, onCreated),
        deleteTemporaryFile = { file ->
            file.delete().also { onDeleted() }
        },
    )

    private fun scanSummary() = ScannerSpikeState.ResultSummary(
        jpegPageCount = 1,
        hasPdf = true,
        pdfPageCount = 1,
    )

    private fun testPageUri(): Uri = Uri.parse("content://org.synapseworks.pageharbor.test/page")

    private fun ocrResult(token: String) = OcrResult(listOf(OcrPageResult(pageIndex = 0, text = token)))

    private class GatedOcrEngine(
        private val gate: ControllableOperationGate,
        private val result: OcrResult,
    ) : OcrEngine {
        override fun recognize(pages: List<OcrPage>): OcrResult {
            gate.await()
            return result
        }
    }

    private class GatedGenerator(
        private val gate: ControllableOperationGate,
        private val onCreated: (File) -> Unit,
    ) : SearchablePdfGenerator {
        override suspend fun generate(request: SearchablePdfRequest): SearchablePdfGenerationResult =
            withContext(Dispatchers.IO) {
            request.outputFile.writeBytes(byteArrayOf(0x50, 0x44, 0x46))
            onCreated(request.outputFile)
            gate.await()
            SearchablePdfGenerationResult.Success(
                pageCount = request.pages.size,
                textLayerPageCount = 1,
            )
        }
    }
}

private class ControllableOperationGate {
    private val reached = CountDownLatch(1)
    private val released = CountDownLatch(1)
    private val exited = CountDownLatch(1)

    fun await() {
        reached.countDown()
        try {
            check(released.await(10, TimeUnit.SECONDS)) { "Test gate release timed out" }
        } finally {
            exited.countDown()
        }
    }

    fun awaitReached() {
        assertTrue("Operation did not reach the test gate", reached.await(10, TimeUnit.SECONDS))
    }

    fun release() = released.countDown()

    fun awaitExited() {
        assertTrue("Operation did not leave the test gate", exited.await(10, TimeUnit.SECONDS))
    }
}
