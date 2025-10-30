package org.clipboard.app.database.entities

// Simple data class for in-memory storage (no Room annotations needed)
data class SettingsEntity(
    val id: Int = 0,
    val deviceName: String,
    val serverPort: Int,
    val autoStart: Boolean,
    val discoveryMethod: String,
    val keepHistory: Boolean,
    val historyRetentionDays: Int,
    val maxHistoryItems: Int,
    val darkTheme: Boolean = false
)