package org.synapseworks.pageharbor.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.synapseworks.pageharbor.R
import org.synapseworks.pageharbor.ocr.OcrPageResult
import org.synapseworks.pageharbor.ocr.OcrResult
import org.synapseworks.pageharbor.ocr.copyableOcrPreview
import org.synapseworks.pageharbor.ocr.displayText
import org.synapseworks.pageharbor.ocr.failedPageCount
import org.synapseworks.pageharbor.ocr.textFoundPageCount

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun OcrResultScreen(
    result: OcrResult,
    pageUris: List<Uri>,
    selectedPageIndex: Int,
    onSelectedPageChange: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRecognizeAgain: () -> Unit,
    onClearRecognizedText: () -> Unit,
    onCopyText: (String) -> Unit,
) {
    val pageCount = result.pages.size
    val selectedIndex = selectedPageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    val selectedPage = result.pages.getOrNull(selectedIndex)
    val textFoundPageCount = result.textFoundPageCount()
    val context = LocalContext.current
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 24.dp,
                vertical = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                OcrSummary(
                    pageCount = pageCount,
                    textFoundPageCount = textFoundPageCount,
                    failedPageCount = result.failedPageCount(),
                )
            }

            if (pageCount > 1) {
                item {
                    OcrPageNavigation(
                        selectedPageIndex = selectedIndex,
                        pageCount = pageCount,
                        onSelectedPageChange = onSelectedPageChange,
                    )
                }
            }

            selectedPage?.let { page ->
                item {
                    ScannedDocumentPreview(
                        pageUri = pageUris.getOrNull(selectedIndex),
                        pageNumber = selectedIndex + 1,
                        pageCount = pageCount,
                    )
                }
                item {
                    OcrPageText(page)
                }
            }

            item {
                OcrActions(
                    copyPayload = copyPayload,
                    onCopyText = onCopyText,
                    onRecognizeAgain = onRecognizeAgain,
                    onClearRecognizedText = onClearRecognizedText,
                )
            }
        }
    }
}

@Composable
private fun OcrSummary(
    pageCount: Int,
    textFoundPageCount: Int,
    failedPageCount: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (textFoundPageCount == 0) {
                stringResource(R.string.ocr_no_text)
            } else if (pageCount == 1) {
                stringResource(R.string.ocr_single_page_summary)
            } else {
                stringResource(R.string.ocr_page_summary, textFoundPageCount, pageCount)
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        if (failedPageCount > 0) {
            Text(
                text = stringResource(R.string.ocr_partial_failure_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun OcrPageNavigation(
    selectedPageIndex: Int,
    pageCount: Int,
    onSelectedPageChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.ocr_page_indicator, selectedPageIndex + 1, pageCount),
            style = MaterialTheme.typography.titleMedium,
        )
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = selectedPageIndex > 0,
                onClick = { onSelectedPageChange(selectedPageIndex - 1) },
            ) { Text(stringResource(R.string.ocr_previous_page_action)) }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = selectedPageIndex < pageCount - 1,
                onClick = { onSelectedPageChange(selectedPageIndex + 1) },
            ) { Text(stringResource(R.string.ocr_next_page_action)) }
        }
    }
}

@Composable
private fun OcrPageText(page: OcrPageResult) {
    val displayText = page.displayText()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            modifier = Modifier.semantics { heading() },
            text = stringResource(R.string.ocr_text_section_heading),
            style = MaterialTheme.typography.titleMedium,
        )
        if (displayText.isBlank()) {
            Text(
                text = stringResource(R.string.ocr_preview_empty_page),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            SelectionContainer {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    text = displayText,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
private fun ScannedDocumentPreview(
    pageUri: Uri?,
    pageNumber: Int,
    pageCount: Int,
) {
    val description = stringResource(R.string.ocr_document_preview_description, pageNumber, pageCount)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            modifier = Modifier.semantics { heading() },
            text = stringResource(R.string.ocr_document_section_heading),
            style = MaterialTheme.typography.titleMedium,
        )
        if (pageUri == null) {
            Text(
                text = stringResource(R.string.ocr_preview_unavailable),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            val contentResolver = LocalContext.current.contentResolver
            val previewState by rememberDocumentPreview(pageUri, contentResolver)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                when (val state = previewState) {
                    DocumentPreviewState.Loading -> Text(
                        text = stringResource(R.string.ocr_preview_loading),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    DocumentPreviewState.Unavailable -> Text(
                        text = stringResource(R.string.ocr_preview_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    is DocumentPreviewState.Ready -> Image(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        bitmap = state.bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberDocumentPreview(
    pageUri: Uri,
    contentResolver: android.content.ContentResolver,
) = produceState<DocumentPreviewState>(
    initialValue = DocumentPreviewState.Loading,
    key1 = pageUri,
) {
    value = when (val decoded = withContext(Dispatchers.IO) {
        decodeDocumentPreview(contentResolver, pageUri)
    }) {
        null -> DocumentPreviewState.Unavailable
        else -> DocumentPreviewState.Ready(decoded)
    }
}

private sealed interface DocumentPreviewState {
    data object Loading : DocumentPreviewState
    data object Unavailable : DocumentPreviewState
    data class Ready(val bitmap: Bitmap) : DocumentPreviewState
}

private fun decodeDocumentPreview(
    contentResolver: android.content.ContentResolver,
    pageUri: Uri,
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching {
        contentResolver.openInputStream(pageUri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
    }.getOrNull()
    val sampleSize = DocumentPreviewDecodePolicy.calculateInSampleSize(
        width = bounds.outWidth,
        height = bounds.outHeight,
    ) ?: return null

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return runCatching {
        contentResolver.openInputStream(pageUri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }.getOrNull()
}

@Composable
private fun OcrActions(
    copyPayload: String?,
    onCopyText: (String) -> Unit,
    onRecognizeAgain: () -> Unit,
    onClearRecognizedText: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (copyPayload != null) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onCopyText(copyPayload) },
            ) { Text(stringResource(R.string.ocr_copy_action)) }
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onRecognizeAgain,
        ) { Text(stringResource(R.string.ocr_recognize_again_action)) }
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClearRecognizedText,
        ) { Text(stringResource(R.string.ocr_clear_action)) }
    }
}
