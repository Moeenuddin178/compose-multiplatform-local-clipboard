package org.clipboard.app.database.dao

import kotlinx.coroutines.flow.Flow
import org.clipboard.app.database.entities.SettingsEntity

interface SettingsDao {
    fun getSettings(): Flow<SettingsEntity?>
    suspend fun insertSettings(settingsEntity: SettingsEntity)
    suspend fun updateSettings(settingsEntity: SettingsEntity)
    suspend fun updateDeviceName(deviceName: String)
    suspend fun updateServerPort(port: Int)
}

