package org.clipboard.app.platform

import org.clipboard.app.models.ClipboardMessage

actual class ClipboardManager {
	private var currentText: String = ""

	actual fun getClipboardText(): String = currentText

	actual fun setClipboardText(text: String) {
		currentText = text
	}

	actual fun insertClipboard(message: ClipboardMessage) {
		currentText = message.text
	}
}
