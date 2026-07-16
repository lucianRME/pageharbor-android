package org.synapseworks.pageharbor

import android.content.ClipDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.synapseworks.pageharbor.ui.copyPlainTextToClipboard

class ClipboardTextTest {
    @Test
    fun productionClipboardPathWritesExactPlainTextAndCleansUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        val expectedText = "PageHarbor clipboard test"
        var clip: ClipData? = null
        var description: ClipDescription? = null
        var copied = false

        try {
            instrumentation.runOnMainSync {
                copied = copyPlainTextToClipboard(activity, "Recognized text", expectedText)
                val clipboardManager = activity.getSystemService(ClipboardManager::class.java)
                clip = clipboardManager.primaryClip
                description = clipboardManager.primaryClipDescription
            }

            assertTrue(copied)
            assertNotNull(clip)
            assertNotNull(description)
            assertTrue(description!!.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
            assertEquals(expectedText, clip!!.getItemAt(0).coerceToText(activity).toString())
        } finally {
            instrumentation.runOnMainSync {
                val clipboardManager = activity.getSystemService(ClipboardManager::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    clipboardManager.clearPrimaryClip()
                } else {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
                }
                activity.finish()
            }
        }
    }
}
