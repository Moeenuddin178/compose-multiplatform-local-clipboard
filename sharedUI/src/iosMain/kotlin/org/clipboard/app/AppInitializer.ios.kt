package org.clipboard.app

import org.clipboard.app.platform.ClipboardManager
import platform.UIKit.UIDevice
import platform.Foundation.NSProcessInfo

actual fun createClipboardManager(): ClipboardManager {
    return ClipboardManager()
}

actual fun getDeviceName(): String {
    return try {
        val device = UIDevice.currentDevice
        val deviceName = device.name
        val systemName = device.systemName
        val systemVersion = device.systemVersion
        
        when {
            systemName.lowercase().contains("ios") -> "iPhone $deviceName"
            systemName.lowercase().contains("ipados") -> "iPad $deviceName"
            systemName.lowercase().contains("tvos") -> "Apple TV $deviceName"
            systemName.lowercase().contains("watchos") -> "Apple Watch $deviceName"
            else -> "$systemName $deviceName"
        }
    } catch (e: Exception) {
        println("❌ [AppInitializer.ios] Error getting device name: ${e.message}")
        "iOS Device"
    }
}

// iOS-specific DeviceDiscovery initialization (no-op for iOS)
actual fun org.clipboard.app.AppInitializer.initializeDeviceDiscovery() {
    // iOS DeviceDiscovery doesn't need context initialization
}
