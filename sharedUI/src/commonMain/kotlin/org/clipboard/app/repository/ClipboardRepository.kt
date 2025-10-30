package org.clipboard.app.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.clipboard.app.getDeviceName
import org.clipboard.app.database.entities.ApprovedDeviceEntity
import org.clipboard.app.database.entities.ClipboardHistoryEntity
import org.clipboard.app.database.entities.SettingsEntity

expect fun loadPersistedSettings(): SettingsEntity?
expect fun savePersistedSettings(settings: SettingsEntity): Boolean

class ClipboardRepository {

    // In-memory storage
    private val _clipboardHistory = MutableStateFlow(emptyList<ClipboardHistoryEntity>())
    private val _approvedDevices = MutableStateFlow(emptyList<ApprovedDeviceEntity>())
    private var _settings = loadPersistedSettings() ?: SettingsEntity(
        deviceName = getDeviceName(),
        serverPort = 8080,
        autoStart = true,
        discoveryMethod = "MDNS",
        keepHistory = true,
        historyRetentionDays = -1,
        maxHistoryItems = 100,
        darkTheme = false
    )

    val clipboardHistory = _clipboardHistory.asStateFlow()
    val approvedDevices = _approvedDevices.asStateFlow()

    // Clipboard History
    fun getAllHistory(): Flow<List<ClipboardHistoryEntity>> = clipboardHistory

    fun searchHistory(query: String): Flow<List<ClipboardHistoryEntity>> =
        MutableStateFlow<List<ClipboardHistoryEntity>>(_clipboardHistory.value.filter {
            it.text.contains(query, ignoreCase = true) ||
            it.sourceDeviceName.contains(query, ignoreCase = true)
        })

    suspend fun insertClipboard(clipboard: ClipboardHistoryEntity) {
        val newItem = clipboard.copy(id = (_clipboardHistory.value.maxOfOrNull { it.id } ?: 0) + 1)
        _clipboardHistory.value = _clipboardHistory.value + newItem
        
        println("📝 [ClipboardRepository] Added to history: ${newItem.text.take(50)}... (Total: ${_clipboardHistory.value.size})")

        // Keep only last 100 items
        if (_clipboardHistory.value.size > 100) {
            _clipboardHistory.value = _clipboardHistory.value.takeLast(100)
        }
    }

    suspend fun deleteClipboard(id: Long) {
        _clipboardHistory.value = _clipboardHistory.value.filter { it.id != id }
    }

    suspend fun deleteAllHistory() {
        _clipboardHistory.value = emptyList()
    }

    fun getRecentHistory(limit: Int): Flow<List<ClipboardHistoryEntity>> =
        clipboardHistory.map { it.takeLast(limit) }

    // Approved Devices
    fun getAllApprovedDevices(): Flow<List<ApprovedDeviceEntity>> = approvedDevices

    suspend fun getApprovedDevice(deviceId: String): ApprovedDeviceEntity? =
        _approvedDevices.value.find { it.deviceId == deviceId }

    suspend fun insertApprovedDevice(device: ApprovedDeviceEntity) {
        // Remove if already exists (replace)
        _approvedDevices.value = _approvedDevices.value.filter { it.deviceId != device.deviceId }
        _approvedDevices.value = _approvedDevices.value + device
        println("✅ [ClipboardRepository] Device added to in-memory store: ${device.name} (${device.deviceId})")
    }

    suspend fun deleteApprovedDevice(deviceId: String) {
        _approvedDevices.value = _approvedDevices.value.filter { it.deviceId != deviceId }
        println("🗑️ [ClipboardRepository] Device removed from in-memory store: $deviceId")
    }

    suspend fun updateApprovedDevice(device: ApprovedDeviceEntity) {
        val index = _approvedDevices.value.indexOfFirst { it.deviceId == device.deviceId }
        if (index >= 0) {
            val updatedList = _approvedDevices.value.toMutableList()
            updatedList[index] = device
            _approvedDevices.value = updatedList
            println("🔄 [ClipboardRepository] Device updated in in-memory store: ${device.name}")
        }
    }

    // Settings
    private val _settingsFlow = MutableStateFlow<SettingsEntity?>(_settings)
    fun getSettings(): Flow<SettingsEntity?> = _settingsFlow.asStateFlow()

    suspend fun saveSettings(settings: SettingsEntity) {
        _settings = settings
        _settingsFlow.value = settings
        savePersistedSettings(settings)
        println("💾 [ClipboardRepository] Settings saved to in-memory store")
    }

    suspend fun updateSettings(settings: SettingsEntity) {
        _settings = settings
        _settingsFlow.value = settings
    }

    suspend fun updateDeviceName(name: String) {
        _settings = _settings.copy(deviceName = name)
        _settingsFlow.value = _settings
    }

    suspend fun updateServerPort(port: Int) {
        _settings = _settings.copy(serverPort = port)
        _settingsFlow.value = _settings
    }

    suspend fun updateDarkTheme(enabled: Boolean) {
        _settings = _settings.copy(darkTheme = enabled)
        _settingsFlow.value = _settings
        savePersistedSettings(_settings)
    }

    suspend fun clearAllData() {
        _clipboardHistory.value = emptyList()
        _approvedDevices.value = emptyList()
        println("🧹 [ClipboardRepository] All data cleared from in-memory store")
    }
}
