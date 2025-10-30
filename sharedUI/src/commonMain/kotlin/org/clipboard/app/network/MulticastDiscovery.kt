package org.clipboard.app.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.clipboard.app.models.Device

class MulticastDiscovery(
    private val port: Int = 4321,
    private val multicastAddress: String = "239.255.43.21"
) {
    private val _discoveredDevices = MutableStateFlow<List<Device>>(emptyList())
    val discoveredDevices: StateFlow<List<Device>> = _discoveredDevices

    fun start(deviceId: String, deviceName: String, devicePort: Int) {
        // TODO: Implement UDP multicast discovery
        // For now, this is a stub implementation that does nothing
    }

    fun stop() {
        _discoveredDevices.value = emptyList()
    }
}
