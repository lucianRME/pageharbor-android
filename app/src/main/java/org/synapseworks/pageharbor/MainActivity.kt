package org.synapseworks.pageharbor

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import org.synapseworks.pageharbor.scanner.ScannerSpikeState
import org.synapseworks.pageharbor.scanner.createScannerResultSummary
import org.synapseworks.pageharbor.ui.PageHarborApp

class MainActivity : ComponentActivity() {
    private var scannerSpikeState: ScannerSpikeState by mutableStateOf(ScannerSpikeState.Idle)

    private val scanLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
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
                createScannerResultSummary(
                    jpegPageCount = jpegPageCount,
                    pdfPageCount = pdfPageCount,
                )
            }
        }.getOrDefault(ScannerSpikeState.Error)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PageHarborApp(
                scannerSpikeState = scannerSpikeState,
                onScanDocument = ::launchDocumentScanner,
                onViewSourceCode = ::openSourceCode,
                onClearScanResult = {
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
