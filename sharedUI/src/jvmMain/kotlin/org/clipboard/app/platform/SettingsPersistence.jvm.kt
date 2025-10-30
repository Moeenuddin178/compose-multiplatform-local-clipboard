package org.clipboard.app.platform

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Paths
import org.clipboard.app.getDeviceName

@Serializable
data class SettingsData(
    val deviceName: String,
    val serverPort: Int,
    val autoStart: Boolean,
    val discoveryMethod: String,
    val keepHistory: Boolean,
    val historyRetentionDays: Int,
    val maxHistoryItems: Int,
    val darkTheme: Boolean = false
)

object SettingsPersistence {
    private val settingsFile = File(System.getProperty("user.home") + File.separator + ".local-clipboard" + File.separator + "settings.json")
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    init {
        // Create directory if it doesn't exist
        settingsFile.parentFile?.mkdirs()
    }
    
    fun saveSettings(settings: SettingsData): Boolean {
        return try {
            val jsonString = json.encodeToString(SettingsData.serializer(), settings)
            settingsFile.writeText(jsonString)
            println("💾 [SettingsPersistence] Settings saved to: ${settingsFile.absolutePath}")
            true
        } catch (e: Exception) {
            println("❌ [SettingsPersistence] Failed to save settings: ${e.message}")
            false
        }
    }
    
    fun loadSettings(): SettingsData? {
        return try {
            if (!settingsFile.exists()) {
                println("📁 [SettingsPersistence] Settings file doesn't exist, using defaults")
                return null
            }
            
            val jsonString = settingsFile.readText()
            val settings = json.decodeFromString(SettingsData.serializer(), jsonString)
            println("📂 [SettingsPersistence] Settings loaded from: ${settingsFile.absolutePath}")
            settings
        } catch (e: Exception) {
            println("❌ [SettingsPersistence] Failed to load settings: ${e.message}")
            null
        }
    }
    
    fun getDefaultSettings(): SettingsData {
        return SettingsData(
            deviceName = try { getDeviceName() } catch (_: Throwable) { "Desktop Device" },
            serverPort = 8080,
            autoStart = true,
            discoveryMethod = "MDNS",
            keepHistory = true,
            historyRetentionDays = -1,
            maxHistoryItems = 100,
            darkTheme = false
        )
    }
}
