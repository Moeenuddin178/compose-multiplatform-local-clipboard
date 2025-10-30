package org.clipboard.app

import android.content.Context
import android.os.Build
import org.clipboard.app.platform.ClipboardManager

// This will be provided via dependency injection or context
var appContext: Context? = null

actual fun createClipboardManager(): ClipboardManager {
    println("📋 [AppInitializer.android] createClipboardManager called")
    println("📋 [AppInitializer.android] appContext is null: ${appContext == null}")
    
    val ctx = appContext ?: run {
        println("❌ [AppInitializer.android] Context is null in createClipboardManager!")
        throw IllegalStateException("Context not set")
    }
    
    println("✅ [AppInitializer.android] Creating ClipboardManager with context")
    return ClipboardManager(ctx)
}

actual fun getDeviceName(): String {
    return try {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val userName = System.getProperty("user.name") ?: "User"
        
        when {
            manufacturer.lowercase().contains("samsung") -> "Samsung $model $userName"
            manufacturer.lowercase().contains("google") -> "Google $model $userName"
            manufacturer.lowercase().contains("xiaomi") -> "Xiaomi $model $userName"
            manufacturer.lowercase().contains("huawei") -> "Huawei $model $userName"
            manufacturer.lowercase().contains("oneplus") -> "OnePlus $model $userName"
            manufacturer.lowercase().contains("oppo") -> "OPPO $model $userName"
            manufacturer.lowercase().contains("vivo") -> "Vivo $model $userName"
            manufacturer.lowercase().contains("realme") -> "Realme $model $userName"
            manufacturer.lowercase().contains("motorola") -> "Motorola $model $userName"
            manufacturer.lowercase().contains("lg") -> "LG $model $userName"
            manufacturer.lowercase().contains("sony") -> "Sony $model $userName"
            else -> "$manufacturer $model $userName"
        }
    } catch (e: Exception) {
        println("❌ [AppInitializer.android] Error getting device name: ${e.message}")
        "Android Device"
    }
}

// Android-specific DeviceDiscovery initialization
actual fun org.clipboard.app.AppInitializer.initializeDeviceDiscovery() {
    println("🔧 [AppInitializer.android] initializeDeviceDiscovery called")
    println("🔧 [AppInitializer.android] appContext is null: ${appContext == null}")
    
    val context = appContext ?: run {
        println("❌ [AppInitializer.android] Context is null!")
        throw IllegalStateException("Context not set")
    }
    
    println("✅ [AppInitializer.android] Context found: ${context.javaClass.simpleName}")
    println("🔧 [AppInitializer.android] Calling deviceDiscovery.initialize()...")
    deviceDiscovery.initialize(context)
    println("✅ [AppInitializer.android] DeviceDiscovery initialized")
}

// Note: Android database creation is handled in the database package
// This function is not used - database is created via createAppDatabase() with context
