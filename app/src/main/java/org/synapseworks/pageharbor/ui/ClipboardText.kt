package org.synapseworks.pageharbor.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/** Copies in-memory text to the system clipboard without retaining it in the app. */
fun copyPlainTextToClipboard(context: Context, label: String, text: String): Boolean {
    val clipboardManager = context.getSystemService(ClipboardManager::class.java) ?: return false
    clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
    return true
}
