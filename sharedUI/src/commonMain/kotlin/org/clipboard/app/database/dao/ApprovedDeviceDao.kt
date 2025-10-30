package org.clipboard.app.database.dao

import kotlinx.coroutines.flow.Flow
import org.clipboard.app.database.entities.ApprovedDeviceEntity

interface ApprovedDeviceDao {
    fun getAllDevices(): Flow<List<ApprovedDeviceEntity>>
    suspend fun getDevice(deviceId: String): ApprovedDeviceEntity?
    suspend fun insertDevice(device: ApprovedDeviceEntity)
    suspend fun deleteDevice(deviceId: String)
    suspend fun updateDevice(device: ApprovedDeviceEntity)
}

