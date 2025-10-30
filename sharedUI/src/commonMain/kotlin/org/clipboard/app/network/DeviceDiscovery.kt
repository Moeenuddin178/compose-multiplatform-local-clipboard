package org.clipboard.app.network

import org.clipboard.app.models.Device
import kotlinx.coroutines.flow.StateFlow

expect class DeviceDiscovery() {
    val discoveredDevices: StateFlow<List<Device>>
    
    suspend fun start(deviceId: String, deviceName: String, port: Int)
    fun stop()
}
