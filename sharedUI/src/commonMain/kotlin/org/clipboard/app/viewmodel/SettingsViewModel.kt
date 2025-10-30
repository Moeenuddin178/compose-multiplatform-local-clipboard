package org.clipboard.app.viewmodel

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.clipboard.app.database.entities.SettingsEntity
import org.clipboard.app.models.DiscoveryMethod
import org.clipboard.app.network.ClipboardServer
import org.clipboard.app.network.DeviceDiscovery
import org.clipboard.app.repository.ClipboardRepository

class SettingsViewModel(
    private val repository: ClipboardRepository,
    private val server: ClipboardServer,
    private val deviceDiscovery: DeviceDiscovery,
    private val deviceId: String,
    private val deviceName: String
) {
    
    val settings = repository.getSettings()
    
    // Expose device name for UI
    val currentDeviceName = deviceName
    
    // Expose dark theme state
    val darkTheme: StateFlow<Boolean> = settings.map { it?.darkTheme ?: false }.stateIn(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    
    suspend fun updateDeviceName(name: String) {
        repository.updateDeviceName(name)
        // Restart server and discovery with new device name
        restartServerAndDiscovery()
    }
    
    suspend fun updateServerPort(port: Int) {
        repository.updateServerPort(port)
        // Restart server and discovery with new port
        restartServerAndDiscovery()
    }
    
    suspend fun updateDiscoveryMethod(method: DiscoveryMethod) {
        val currentSettings = settings.firstOrNull() ?: return
        repository.updateSettings(
            currentSettings.copy(discoveryMethod = method.name)
        )
    }
    
    suspend fun updateAutoStart(autoStart: Boolean) {
        val currentSettings = settings.firstOrNull() ?: return
        repository.updateSettings(
            currentSettings.copy(autoStart = autoStart)
        )
    }
    
    suspend fun updateKeepHistory(keepHistory: Boolean) {
        val currentSettings = settings.firstOrNull() ?: return
        repository.updateSettings(
            currentSettings.copy(keepHistory = keepHistory)
        )
    }
    
    suspend fun updateHistoryRetention(days: Int) {
        val currentSettings = settings.firstOrNull() ?: return
        repository.updateSettings(
            currentSettings.copy(historyRetentionDays = days)
        )
    }
    
    suspend fun updateMaxHistoryItems(maxItems: Int) {
        val currentSettings = settings.firstOrNull() ?: return
        repository.updateSettings(
            currentSettings.copy(maxHistoryItems = maxItems)
        )
    }
    
    suspend fun updateDarkTheme(enabled: Boolean) {
        repository.updateDarkTheme(enabled)
    }
    
    suspend fun clearAllData() {
        repository.clearAllData()
    }
    
    private suspend fun restartServerAndDiscovery() {
        println("🔄 [SettingsViewModel] Restarting server and discovery...")
        
        // Stop current server and discovery
        server.stop()
        deviceDiscovery.stop()
        
        // Get updated settings
        val currentSettings = settings.firstOrNull() ?: return
        val newDeviceName = currentSettings.deviceName
        val newPort = currentSettings.serverPort
        
        println("🔄 [SettingsViewModel] New settings - Name: $newDeviceName, Port: $newPort")
        
        // Restart server with new settings
        val serverStarted = server.start(deviceId, newDeviceName)
        if (serverStarted) {
            val actualPort = server.actualPort
            println("✅ [SettingsViewModel] Server restarted on port: $actualPort")
            
            // Restart discovery with actual port
            kotlinx.coroutines.delay(500)
            deviceDiscovery.start(deviceId, newDeviceName, actualPort)
            println("✅ [SettingsViewModel] Discovery restarted")
        } else {
            println("❌ [SettingsViewModel] Failed to restart server")
        }
    }
}
