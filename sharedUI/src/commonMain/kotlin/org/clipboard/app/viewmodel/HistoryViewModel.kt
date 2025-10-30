package org.clipboard.app.viewmodel

import kotlinx.coroutines.flow.*
import org.clipboard.app.models.Device
import org.clipboard.app.network.ClipboardClient
import org.clipboard.app.repository.ClipboardRepository

class HistoryViewModel(
    private val repository: ClipboardRepository,
    private val client: ClipboardClient,
    private val deviceId: String,
    private val deviceName: String
) {
    
    val searchQuery = MutableStateFlow("")
    
    val historyItems = repository.clipboardHistory
    
    val filteredHistory = combine(searchQuery, historyItems) { query, items ->
        if (query.isBlank()) {
            items
        } else {
            items.filter { it.text.contains(query, ignoreCase = true) }
        }
    }.stateIn(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    suspend fun deleteItem(id: Long) {
        repository.deleteClipboard(id)
    }
    
    suspend fun clearAll() {
        repository.deleteAllHistory()
    }
    
    suspend fun sendToDevice(device: Device, text: String): Boolean {
        val message = org.clipboard.app.models.ClipboardMessage(
            text = text,
            deviceId = deviceId,
            deviceName = deviceName
        )
        return client.sendClipboard(device.ipAddress, device.port, message)
    }
}
