package org.synapseworks.pageharbor.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.synapseworks.pageharbor.R
import org.synapseworks.pageharbor.ocr.OcrResult
import org.synapseworks.pageharbor.ocr.copyableOcrPreview
import org.synapseworks.pageharbor.ocr.failedPageCount
import org.synapseworks.pageharbor.ocr.formatOcrPreview
import org.synapseworks.pageharbor.ocr.textFoundPageCount

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun OcrResultScreen(
    result: OcrResult,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRecognizeAgain: () -> Unit,
    onClearRecognizedText: () -> Unit,
    onCopyText: (String) -> Unit,
) {
    val textFoundPageCount = result.textFoundPageCount()
    val context = LocalContext.current
    val preview = formatOcrPreview(
        result = result,
        pageHeading = { context.getString(R.string.ocr_preview_page_heading, it) },
        emptyPageText = stringResource(R.string.ocr_preview_empty_page),
    )
    val copyPayload = copyableOcrPreview(
        result = result,
        pageHeading = { context.getString(R.string.ocr_preview_page_heading, it) },
        emptyPageText = stringResource(R.string.ocr_preview_empty_page),
    )
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        modifier = Modifier.semantics { heading() },
                        text = stringResource(R.string.ocr_result_heading),
                    )
                },
                navigationIcon = {
                    TextButton(
                        modifier = Modifier.semantics {
                            contentDescription = context.getString(R.string.ocr_back_content_description)
                        },
                        onClick = onBack,
                    ) { Text(stringResource(R.string.ocr_back_action)) }
                },
            )
        },
    ) { innerPadding ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (textFoundPageCount == 0) {
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.ocr_no_text),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = if (result.pages.size == 1) {
                        stringResource(R.string.ocr_single_page_summary)
                    } else {
                        stringResource(R.string.ocr_page_summary, textFoundPageCount, result.pages.size)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (result.failedPageCount() > 0) {
                    Text(
                        text = stringResource(R.string.ocr_partial_failure_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                OcrActions(
                    copyPayload = copyPayload,
                    onCopyText = onCopyText,
                    onRecognizeAgain = onRecognizeAgain,
                    onClearRecognizedText = onClearRecognizedText,
                )
                SelectionContainer {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        text = preview,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            if (textFoundPageCount == 0) {
                OcrActions(
                    copyPayload = null,
                    onCopyText = onCopyText,
                    onRecognizeAgain = onRecognizeAgain,
                    onClearRecognizedText = onClearRecognizedText,
                )
            }
        }
    }
}

@Composable
private fun OcrActions(
    copyPayload: String?,
    onCopyText: (String) -> Unit,
    onRecognizeAgain: () -> Unit,
    onClearRecognizedText: () -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        if (copyPayload != null) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onCopyText(copyPayload) },
            ) {
                Text(stringResource(R.string.ocr_copy_action))
            }
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onRecognizeAgain,
        ) {
            Text(stringResource(R.string.ocr_recognize_again_action))
        }
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClearRecognizedText,
        ) {
            Text(stringResource(R.string.ocr_clear_action))
        }
    }
}
