package org.clipboard.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.clipboard.app.database.dao.ApprovedDeviceDao
import org.clipboard.app.database.dao.ClipboardHistoryDao
import org.clipboard.app.database.dao.SettingsDao
import org.clipboard.app.database.entities.ApprovedDeviceEntity
import org.clipboard.app.database.entities.ClipboardHistoryEntity
import org.clipboard.app.database.entities.SettingsEntity

private class InMemoryApprovedDeviceDao : ApprovedDeviceDao {
	private val state = MutableStateFlow<List<ApprovedDeviceEntity>>(emptyList())
	override fun getAllDevices(): Flow<List<ApprovedDeviceEntity>> = state
	override suspend fun getDevice(deviceId: String): ApprovedDeviceEntity? = state.value.firstOrNull { it.deviceId == deviceId }
	override suspend fun insertDevice(device: ApprovedDeviceEntity) {
		state.value = state.value.filterNot { it.deviceId == device.deviceId } + device
	}
	override suspend fun deleteDevice(deviceId: String) {
		state.value = state.value.filterNot { it.deviceId == deviceId }
	}
	override suspend fun updateDevice(device: ApprovedDeviceEntity) {
		insertDevice(device)
	}
}

private class InMemoryClipboardHistoryDao : ClipboardHistoryDao {
	private val state = MutableStateFlow<List<ClipboardHistoryEntity>>(emptyList())
	override fun getAllHistory(): Flow<List<ClipboardHistoryEntity>> = state
	override fun searchHistory(query: String): Flow<List<ClipboardHistoryEntity>> = state.map { list ->
		list.filter { it.text.contains(query, ignoreCase = true) }
	}
	override suspend fun insertClipboard(clipboard: ClipboardHistoryEntity) {
		state.value = (state.value + clipboard).sortedByDescending { it.timestamp }
	}
	override suspend fun deleteClipboard(id: Long) {
		state.value = state.value.filterNot { it.id == id }
	}
	override suspend fun deleteAll() {
		state.value = emptyList()
	}
	override suspend fun getCount(): Int = state.value.size
	override fun getRecent(limit: Int): Flow<List<ClipboardHistoryEntity>> = state.map { it.take(limit) }
}

private class InMemorySettingsDao : SettingsDao {
	private val state = MutableStateFlow<SettingsEntity?>(null)
	override fun getSettings(): Flow<SettingsEntity?> = state
	override suspend fun insertSettings(settingsEntity: SettingsEntity) { state.value = settingsEntity }
	override suspend fun updateSettings(settingsEntity: SettingsEntity) { state.value = settingsEntity }
	override suspend fun updateDeviceName(deviceName: String) {
		state.value = (state.value ?: SettingsEntity(
			deviceName = deviceName,
			serverPort = 8080,
			autoStart = false,
			discoveryMethod = "none",
			keepHistory = true,
			historyRetentionDays = 30,
			maxHistoryItems = 200
		)).copy(deviceName = deviceName)
	}
	override suspend fun updateServerPort(port: Int) {
		state.value = (state.value ?: SettingsEntity(
			deviceName = "Web",
			serverPort = port,
			autoStart = false,
			discoveryMethod = "none",
			keepHistory = true,
			historyRetentionDays = 30,
			maxHistoryItems = 200
		)).copy(serverPort = port)
	}
}

private class InMemoryAppDatabase : AppDatabase {
	private val approved = InMemoryApprovedDeviceDao()
	private val history = InMemoryClipboardHistoryDao()
	private val settings = InMemorySettingsDao()
	override fun clipboardHistoryDao(): ClipboardHistoryDao = history
	override fun approvedDeviceDao(): ApprovedDeviceDao = approved
	override fun settingsDao(): SettingsDao = settings
}

actual fun createAppDatabase(): AppDatabase = InMemoryAppDatabase()
