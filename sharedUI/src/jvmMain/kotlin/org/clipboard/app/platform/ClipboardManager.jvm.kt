package org.clipboard.app.platform

import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import org.clipboard.app.models.ClipboardMessage

actual class ClipboardManager {
    
    private val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
    
    // Platform detection
    private val isAppleSilicon = System.getProperty("os.arch") == "aarch64" && 
                                 System.getProperty("os.name").lowercase().contains("mac")
    private val isIntelMac = System.getProperty("os.arch") == "x86_64" && 
                            System.getProperty("os.name").lowercase().contains("mac")
    private val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    private val isLinux = System.getProperty("os.name").lowercase().contains("linux")
    
    actual fun getClipboardText(): String {
        return try {
            println("📋 [ClipboardManager.jvm] Platform: ${getPlatformName()}")
            val contents = clipboard.getContents(null)
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                val text = contents.getTransferData(DataFlavor.stringFlavor) as String
                println("📋 [ClipboardManager.jvm] Retrieved text length: ${text.length}")
                text
            } else {
                println("📋 [ClipboardManager.jvm] No text content available")
                ""
            }
        } catch (e: Exception) {
            println("❌ [ClipboardManager.jvm] Error getting clipboard: ${e.message}")
            ""
        }
    }
    
    actual fun setClipboardText(text: String) {
        try {
            println("📋 [ClipboardManager.jvm] Setting clipboard text length: ${text.length}")
            val selection = StringSelection(text)
            clipboard.setContents(selection, null)
            println("✅ [ClipboardManager.jvm] Clipboard set successfully")
        } catch (e: Exception) {
            println("❌ [ClipboardManager.jvm] Error setting clipboard: ${e.message}")
        }
    }
    
    private fun getPlatformName(): String {
        return when {
            isAppleSilicon -> "Apple Silicon (ARM64)"
            isIntelMac -> "Intel Mac (x86_64)"
            isWindows -> "Windows"
            isLinux -> "Linux"
            else -> "Unknown"
        }
    }
    
    actual fun insertClipboard(message: ClipboardMessage) {
        setClipboardText(message.text)
    }
}
