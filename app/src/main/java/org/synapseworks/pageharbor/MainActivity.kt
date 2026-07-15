package org.synapseworks.pageharbor

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.synapseworks.pageharbor.document.PdfExportResult
import org.synapseworks.pageharbor.document.PdfSaveState
import org.synapseworks.pageharbor.document.PdfShareError
import org.synapseworks.pageharbor.document.PdfShareIntentResult
import org.synapseworks.pageharbor.document.PdfShareState
import org.synapseworks.pageharbor.document.copyPdfToDestination
import org.synapseworks.pageharbor.document.createPdfShareIntent
import org.synapseworks.pageharbor.scanner.ScannerSpikeState
import org.synapseworks.pageharbor.scanner.createScannerResultSummary
import org.synapseworks.pageharbor.ui.PageHarborApp

class MainActivity : ComponentActivity() {
    private var scannerSpikeState: ScannerSpikeState by mutableStateOf(ScannerSpikeState.Idle)
    private var pdfSaveState: PdfSaveState by mutableStateOf(PdfSaveState.Idle)
    private var pdfShareState: PdfShareState by mutableStateOf(PdfShareState.Idle)
    private var scannedPdfUri: Uri? = null

    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        scannedPdfUri = null
        pdfSaveState = PdfSaveState.Idle
        pdfShareState = PdfShareState.Idle

        if (result.resultCode == Activity.RESULT_CANCELED) {
            scannerSpikeState = ScannerSpikeState.Cancelled
            return@registerForActivityResult
        }

        if (result.resultCode != Activity.RESULT_OK) {
            scannerSpikeState = ScannerSpikeState.Error
            return@registerForActivityResult
        }

        scannerSpikeState = runCatching {
            val scannerResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val jpegPageCount = scannerResult?.pages?.size ?: 0
            val pdfPageCount = scannerResult?.pdf?.pageCount
            if (scannerResult == null || (jpegPageCount == 0 && pdfPageCount == null)) {
                ScannerSpikeState.Error
            } else {
                scannedPdfUri = scannerResult.pdf?.uri
                createScannerResultSummary(
                    jpegPageCount = jpegPageCount,
                    pdfPageCount = pdfPageCount,
                )
            }
        }.getOrDefault(ScannerSpikeState.Error)
    }

    private val createPdfDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { destinationUri ->
        if (destinationUri == null) {
            pdfSaveState = PdfSaveState.Idle
            return@registerForActivityResult
        }

        savePdfToDestination(destinationUri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PageHarborApp(
                scannerSpikeState = scannerSpikeState,
                pdfSaveState = pdfSaveState,
                pdfShareState = pdfShareState,
                onScanDocument = ::launchDocumentScanner,
                onSavePdf = ::choosePdfDestination,
                onSharePdf = ::sharePdf,
                onViewSourceCode = ::openSourceCode,
                onClearScanResult = {
                    scannedPdfUri = null
                    pdfSaveState = PdfSaveState.Idle
                    pdfShareState = PdfShareState.Idle
                    scannerSpikeState = ScannerSpikeState.Idle
                },
            )
        }
    }

    private fun launchDocumentScanner() {
        if (scannerSpikeState == ScannerSpikeState.Preparing) return

        scannerSpikeState = ScannerSpikeState.Preparing

        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE_WITH_FILTER)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
            )
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .build()

        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                scannerSpikeState = ScannerSpikeState.Idle
                val request = IntentSenderRequest.Builder(intentSender).build()
                scanLauncher.launch(request)
            }
            .addOnFailureListener {
                scannerSpikeState = ScannerSpikeState.Error
            }
    }

    private fun choosePdfDestination() {
        if (pdfSaveState == PdfSaveState.ChoosingDestination || pdfSaveState == PdfSaveState.Saving) {
            return
        }

        if (scannedPdfUri == null) {
            pdfSaveState = PdfSaveState.Error(PdfExportResult.SourceMissing)
            return
        }

        pdfSaveState = PdfSaveState.ChoosingDestination
        createPdfDocumentLauncher.launch(getString(R.string.pdf_default_filename))
    }

    private fun sharePdf() {
        if (pdfShareState == PdfShareState.Preparing) return

        pdfShareState = PdfShareState.Preparing
        when (val result = createPdfShareIntent(scannedPdfUri)) {
            is PdfShareIntentResult.Success -> {
                try {
                    val chooser = Intent.createChooser(
                        result.intent,
                        getString(R.string.pdf_share_chooser_title),
                    )
                    startActivity(chooser)
                    pdfShareState = PdfShareState.Idle
                } catch (_: ActivityNotFoundException) {
                    pdfShareState = PdfShareState.Error(PdfShareError.ShareTargetUnavailable)
                } catch (_: RuntimeException) {
                    pdfShareState = PdfShareState.Error(PdfShareError.UnexpectedFailure)
                }
            }

            PdfShareIntentResult.NoPdfAvailable -> {
                pdfShareState = PdfShareState.Error(PdfShareError.NoPdfAvailable)
            }

            PdfShareIntentResult.InvalidUri -> {
                pdfShareState = PdfShareState.Error(PdfShareError.InvalidUri)
            }
        }
    }

    private fun savePdfToDestination(destinationUri: Uri) {
        val sourceUri = scannedPdfUri
        if (sourceUri == null) {
            pdfSaveState = PdfSaveState.Error(PdfExportResult.SourceMissing)
            return
        }

        pdfSaveState = PdfSaveState.Saving
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                copyScannedPdf(sourceUri, destinationUri)
            }
            pdfSaveState = when (result) {
                PdfExportResult.Success -> PdfSaveState.Saved
                PdfExportResult.SourceMissing,
                PdfExportResult.DestinationUnavailable,
                PdfExportResult.WriteFailed,
                -> PdfSaveState.Error(result)
            }
        }
    }

    private fun copyScannedPdf(sourceUri: Uri, destinationUri: Uri): PdfExportResult {
        val source = try {
            contentResolver.openInputStream(sourceUri)
        } catch (_: FileNotFoundException) {
            null
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            return PdfExportResult.SourceMissing
        }

        val destination = try {
            contentResolver.openOutputStream(destinationUri)
        } catch (_: FileNotFoundException) {
            source.closeSafely()
            return PdfExportResult.DestinationUnavailable
        } catch (_: IOException) {
            source.closeSafely()
            return PdfExportResult.DestinationUnavailable
        } catch (_: SecurityException) {
            source.closeSafely()
            return PdfExportResult.DestinationUnavailable
        }

        return copyPdfToDestination(source, destination)
    }

    private fun InputStream?.closeSafely() {
        try {
            this?.close()
        } catch (_: IOException) {
            // Nothing user-actionable, and paths or document details must not be logged.
        }
    }

    private fun openSourceCode() {
        val sourceIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(getString(R.string.source_code_url)),
        )

        try {
            startActivity(sourceIntent)
        } catch (_: ActivityNotFoundException) {
            // No browser is available. Keep the app stable and avoid logging local state.
        }
    }
}
