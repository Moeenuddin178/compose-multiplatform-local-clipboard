package org.clipboard.app.database.entities

// Simple data class for in-memory storage (no Room annotations needed)
data class ClipboardHistoryEntity(
    val id: Long = 0,
    val timestamp: Long,
    val text: String,
    val sourceDeviceId: String,
    val sourceDeviceName: String
)