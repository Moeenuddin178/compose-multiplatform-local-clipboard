package org.clipboard.app

import org.clipboard.app.platform.ClipboardManager
import java.net.InetAddress

actual fun createClipboardManager(): ClipboardManager {
    return ClipboardManager()
}

actual fun getDeviceName(): String {
    return try {
        val hostname = InetAddress.getLocalHost().hostName
        val osName = System.getProperty("os.name")
        val osArch = System.getProperty("os.arch")
        val userName = System.getProperty("user.name")
        
        println("🔍 [AppInitializer.jvm] OS: $osName, Arch: $osArch, User: $userName, Hostname: $hostname")
        
        val deviceName = when {
            osName.lowercase().contains("mac") -> {
                when {
                    osArch.lowercase().contains("aarch64") -> "MacBook M1 $userName"
                    osArch.lowercase().contains("x86_64") -> "MacBook Intel $userName"
                    else -> "MacBook $userName"
                }
            }
            osName.lowercase().contains("windows") -> "Windows PC $userName"
            osName.lowercase().contains("linux") -> "Linux PC $userName"
            else -> "$osName $userName"
        }
        
        println("✅ [AppInitializer.jvm] Generated device name: $deviceName")
        deviceName
    } catch (e: Exception) {
        println("❌ [AppInitializer.jvm] Error getting device name: ${e.message}")
        "Desktop Device"
    }
}

// JVM-specific DeviceDiscovery initialization (no-op for JVM)
actual fun org.clipboard.app.AppInitializer.initializeDeviceDiscovery() {
    // JVM DeviceDiscovery doesn't need context initialization
}
