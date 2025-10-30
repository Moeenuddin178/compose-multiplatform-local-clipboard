package org.clipboard.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.clipboard.app.database.dao.ApprovedDeviceDao
import org.clipboard.app.database.dao.ClipboardHistoryDao
import org.clipboard.app.database.dao.SettingsDao
import org.clipboard.app.database.entities.ApprovedDeviceEntity
import org.clipboard.app.database.entities.ClipboardHistoryEntity
import org.clipboard.app.database.entities.SettingsEntity

// Android in-memory implementation (can be replaced with Room later)
class AppDatabaseImpl : AppDatabase {

    // In-memory storage
    private val historyItems = mutableListOf<ClipboardHistoryEntity>()
    private val approvedDevices = mutableListOf<ApprovedDeviceEntity>()
    private var settings = SettingsEntity(
        deviceName = "My Device",
        serverPort = 8080,
        autoStart = true,
        discoveryMethod = "MDNS",
        keepHistory = true,
        historyRetentionDays = -1,
        maxHistoryItems = 100
    )

    override fun clipboardHistoryDao(): ClipboardHistoryDao = object : ClipboardHistoryDao {
        override fun getAllHistory(): Flow<List<ClipboardHistoryEntity>> = flowOf(historyItems.toList())
        override fun searchHistory(query: String): Flow<List<ClipboardHistoryEntity>> =
            flowOf(historyItems.filter { it.text.contains(query, ignoreCase = true) })
        override suspend fun insertClipboard(clipboard: ClipboardHistoryEntity) {
            historyItems.add(clipboard.copy(id = (historyItems.maxOfOrNull { it.id } ?: 0) + 1))
        }
        override suspend fun deleteClipboard(id: Long) {
            historyItems.removeAll { it.id == id }
        }
        override suspend fun deleteAll() = historyItems.clear()
        override suspend fun getCount(): Int = historyItems.size
        override fun getRecent(limit: Int): Flow<List<ClipboardHistoryEntity>> =
            flowOf(historyItems.takeLast(limit))
    }

    override fun approvedDeviceDao(): ApprovedDeviceDao = object : ApprovedDeviceDao {
        override fun getAllDevices(): Flow<List<ApprovedDeviceEntity>> = flowOf(approvedDevices.toList())
        override suspend fun getDevice(deviceId: String): ApprovedDeviceEntity? =
            approvedDevices.find { it.deviceId == deviceId }
        override suspend fun insertDevice(device: ApprovedDeviceEntity) {
            approvedDevices.add(device)
        }
        override suspend fun deleteDevice(deviceId: String) {
            approvedDevices.removeAll { it.deviceId == deviceId }
        }
        override suspend fun updateDevice(device: ApprovedDeviceEntity) {
            val index = approvedDevices.indexOfFirst { it.deviceId == device.deviceId }
            if (index >= 0) {
                approvedDevices[index] = device
            }
        }
    }

    override fun settingsDao(): SettingsDao = object : SettingsDao {
        override fun getSettings(): Flow<SettingsEntity?> = flowOf(settings)
        override suspend fun insertSettings(settingsEntity: SettingsEntity) {
            settings = settingsEntity
        }
        override suspend fun updateSettings(settingsEntity: SettingsEntity) {
            settings = settingsEntity
        }
        override suspend fun updateDeviceName(deviceName: String) {
            settings = settings.copy(deviceName = deviceName)
        }
        override suspend fun updateServerPort(port: Int) {
            settings = settings.copy(serverPort = port)
        }
    }
}

// Factory function implementation
actual fun createAppDatabase(): AppDatabase = AppDatabaseImpl()







