package org.clipboard.app.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.clipboard.app.models.Device

actual class DeviceDiscovery actual constructor() {
	private val _discoveredDevices = MutableStateFlow<List<Device>>(emptyList())
	actual val discoveredDevices: StateFlow<List<Device>> = _discoveredDevices

	actual suspend fun start(deviceId: String, deviceName: String, port: Int) {
		// No-op for web
	}

	actual fun stop() {
		_discoveredDevices.value = emptyList()
	}
}
