package org.clipboard.app.platform

import org.clipboard.app.models.ClipboardMessage

expect class ClipboardManager {
    fun getClipboardText(): String
    fun setClipboardText(text: String)
    fun insertClipboard(message: ClipboardMessage)
}
