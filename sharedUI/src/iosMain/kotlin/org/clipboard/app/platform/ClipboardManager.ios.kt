package org.clipboard.app.platform

import platform.UIKit.UIPasteboard
import org.clipboard.app.models.ClipboardMessage

actual class ClipboardManager {
    
    private val pasteboard = UIPasteboard.generalPasteboard
    
    actual fun getClipboardText(): String {
        return pasteboard.string ?: ""
    }
    
    actual fun setClipboardText(text: String) {
        pasteboard.setString(text)
    }
    
    actual fun insertClipboard(message: ClipboardMessage) {
        setClipboardText(message.text)
    }
}
