package org.clipboard.app.platform

import android.content.ClipData
import android.content.ClipboardManager as AndroidClipboardManager
import android.content.Context
import org.clipboard.app.models.ClipboardMessage

actual class ClipboardManager(private val context: Context) {
    
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as AndroidClipboardManager
    
    actual fun getClipboardText(): String {
        val clip = clipboard.primaryClip ?: return ""
        if (clip.itemCount > 0) {
            return clip.getItemAt(0)?.coerceToText(context)?.toString() ?: ""
        }
        return ""
    }
    
    actual fun setClipboardText(text: String) {
        val clip = ClipData.newPlainText("Clipboard", text)
        clipboard.setPrimaryClip(clip)
    }
    
    actual fun insertClipboard(message: ClipboardMessage) {
        setClipboardText(message.text)
    }
}
