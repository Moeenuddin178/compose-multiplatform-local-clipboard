package org.clipboard.app.viewmodel

import kotlinx.coroutines.flow.*
import org.clipboard.app.database.entities.ClipboardHistoryEntity
import org.clipboard.app.models.Device
import org.clipboard.app.network.ClipboardClient
import org.clipboard.app.network.ClipboardServer
import org.clipboard.app.platform.ClipboardManager
import org.clipboard.app.repository.ClipboardRepository
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ClipboardViewModel(
    private val clipboardManager: ClipboardManager,
    private val repository: ClipboardRepository,
    private val client: ClipboardClient,
    private val server: ClipboardServer,
    private val deviceId: String,
    private val deviceName: String
) {
    
    val currentClipboard = MutableStateFlow("")

    val recentHistory = repository.getRecentHistory(10) // Increased to 10 items

    val isSending = MutableStateFlow(false)

    // Track processed message IDs to prevent duplicate processing
    private val processedMessageIds = mutableSetOf<String>()
    
    // Send to all results
    val sendToAllResults = MutableStateFlow<List<SendResult>>(emptyList())
    
    data class SendResult(
        val deviceName: String,
        val deviceId: String,
        val success: Boolean,
        val error: String? = null
    )
    
    // Observe received messages from server
    val receivedMessages = server.receivedMessages
    
    suspend fun copyFromSystem() {
        val text = clipboardManager.getClipboardText()
        currentClipboard.value = text
        
        // Add to history if text is not empty
        if (text.isNotBlank()) {
            repository.insertClipboard(
                ClipboardHistoryEntity(
                    timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                    text = text,
                    sourceDeviceId = deviceId,
                    sourceDeviceName = deviceName
                )
            )
        }
    }
    
    fun copyToSystem() {
        clipboardManager.setClipboardText(currentClipboard.value)
    }
    
    fun updateClipboardText(text: String) {
        currentClipboard.value = text
    }
    
    suspend fun sendToAllDevices(devices: List<org.clipboard.app.database.entities.ApprovedDeviceEntity>, text: String) {
        isSending.value = true
        sendToAllResults.value = emptyList()
        
        val results = mutableListOf<SendResult>()
        
        try {
            val message = org.clipboard.app.models.ClipboardMessage(
                text = text,
                deviceId = deviceId,
                deviceName = deviceName
            )
            
            // Send to all devices concurrently
            devices.forEach { device ->
                try {
                    val success = client.sendClipboard(device.ipAddress, device.port, message)
                    results.add(
                        SendResult(
                            deviceName = device.name,
                            deviceId = device.deviceId,
                            success = success,
                            error = if (!success) "Failed to send" else null
                        )
                    )
                } catch (e: Exception) {
                    results.add(
                        SendResult(
                            deviceName = device.name,
                            deviceId = device.deviceId,
                            success = false,
                            error = e.message ?: "Unknown error"
                        )
                    )
                }
            }
            
            // Save to history if at least one send was successful
            val hasSuccess = results.any { it.success }
            if (hasSuccess) {
                repository.insertClipboard(
                    ClipboardHistoryEntity(
                        timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                        text = text,
                        sourceDeviceId = deviceId,
                        sourceDeviceName = deviceName
                    )
                )
            }
            
        } finally {
            sendToAllResults.value = results
            isSending.value = false
        }
    }
    
    fun clearSendToAllResults() {
        sendToAllResults.value = emptyList()
    }
    
    suspend fun sendToDevice(device: Device, text: String): Boolean {
        isSending.value = true
        return try {
            val message = org.clipboard.app.models.ClipboardMessage(
                text = text,
                deviceId = deviceId,
                deviceName = deviceName
            )
            val success = client.sendClipboard(device.ipAddress, device.port, message)
            if (success) {
                // Save to history
                repository.insertClipboard(
                    ClipboardHistoryEntity(
                        timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                        text = text,
                        sourceDeviceId = deviceId,
                        sourceDeviceName = deviceName
                    )
                )
            }
            success
        } finally {
            isSending.value = false
        }
    }
    
    fun clearClipboard() {
        currentClipboard.value = ""
    }

    suspend fun deleteHistoryItem(id: Long) {
        repository.deleteClipboard(id)
    }
    
    // Handle received clipboard messages
    suspend fun handleReceivedMessage(message: org.clipboard.app.models.ClipboardMessage) {
        // Generate unique message ID to prevent duplicate processing
        val messageId = "${message.deviceId}_${message.deviceName}_${message.text.hashCode()}_${message.timestamp}"

        // Check if already processed
        if (processedMessageIds.contains(messageId)) {
            println("⏭️ [ClipboardViewModel] Skipping already processed message from ${message.deviceName}")
            return
        }

        println("📨 [ClipboardViewModel] Processing new clipboard message from ${message.deviceName}: ${message.text}")

        // Mark as processed
        processedMessageIds.add(messageId)

        // Update current clipboard with received text
        currentClipboard.value = message.text

        // Copy to system clipboard
        clipboardManager.setClipboardText(message.text)

        // Save to history
        repository.insertClipboard(
            ClipboardHistoryEntity(
                timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                text = message.text,
                sourceDeviceId = message.deviceId,
                sourceDeviceName = message.deviceName
            )
        )

        println("✅ [ClipboardViewModel] Message processed and saved to history")
    }
    
    // Clear received messages from server
    fun clearReceivedMessages() {
        server.clearReceivedMessages()
        // Also clear processed message tracking
        processedMessageIds.clear()
    }
}
