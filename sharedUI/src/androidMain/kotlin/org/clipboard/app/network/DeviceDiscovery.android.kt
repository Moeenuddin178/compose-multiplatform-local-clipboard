package org.clipboard.app.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.clipboard.app.models.Device

actual class DeviceDiscovery {
    // Note: Context will need to be provided through a factory function
    // For now, this is a simplified implementation
    private var nsdManager: NsdManager? = null
    
    init {
        // Context initialization will be handled through dependency injection
    }
    
    fun initialize(context: Context) {
        this.nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }
    
    private val _discoveredDevices = MutableStateFlow<List<Device>>(emptyList())
    actual val discoveredDevices: StateFlow<List<Device>> = _discoveredDevices
    
    // Track devices by (deviceId, ip, port) to prevent duplicates across IPv4/IPv6
    private val deviceKeys = mutableSetOf<String>()
    
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    
    private var currentDeviceId: String = ""
    
    actual suspend fun start(deviceId: String, deviceName: String, port: Int) {
        println("🚀 [DeviceDiscovery.android] STARTING DISCOVERY (Call from: ${Thread.currentThread().stackTrace[2].methodName})")
        if (nsdManager == null) {
            // Not initialized yet
            return
        }
        currentDeviceId = deviceId
        registerService(deviceId, deviceName, port)
        discoverServices()
    }
    
    actual fun stop() {
        try {
            println("🛑 [DeviceDiscovery.android] Stopping discovery...")
            println("   Current devices: ${_discoveredDevices.value.size}")
            
            nsdManager?.let { manager ->
                discoveryListener?.let { manager.stopServiceDiscovery(it) }
                registrationListener?.let { manager.unregisterService(it) }
            }
            
            // Clear discovered devices list and tracking
            _discoveredDevices.value = emptyList()
            deviceKeys.clear()
            println("   Cleared all discovered devices")
        } catch (e: Exception) {
            println("   ⚠️ Error during stop: ${e.message}")
        }
    }
    
    private fun discoverServices() {
        val manager = nsdManager ?: return
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String?) {}
            override fun onDiscoveryStopped(serviceType: String?) {}
            
            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}
                    override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
                        val serviceName = serviceInfo?.serviceName ?: ""
                        
                        // Parse device name from format "Device Name (device_id)"
                        val deviceName = try {
                            val match = Regex("^(.+?)\\s+\\(device_.+\\)$").find(serviceName)
                            match?.groupValues?.get(1) ?: serviceName.takeIf { !it.startsWith("device_") } ?: "Unknown Device"
                        } catch (e: Exception) {
                            serviceName.takeIf { !it.startsWith("device_") } ?: "Unknown Device"
                        }
                        
                        // Extract device ID from service name
                        val deviceId = try {
                            val match = Regex("\\(device_.+\\)").find(serviceName)
                            match?.value?.removeSurrounding("(", ")") ?: serviceName
                        } catch (e: Exception) {
                            serviceName
                        }
                        
                        // Get port from TXT records if available, otherwise use service port
                        val txtRecords = serviceInfo?.attributes
                        val portFromTxt = txtRecords?.get("port")?.let { 
                            try { String(it).toInt() } catch (e: Exception) { null }
                        }
                        val finalPort = portFromTxt ?: (serviceInfo?.port ?: 8080)
                        
                        val device = Device(
                            deviceId = deviceId,
                            name = deviceName,
                            ipAddress = serviceInfo?.host?.hostAddress ?: "",
                            port = finalPort,
                            isApproved = false
                        )
                        
                        // Skip own device by matching currentDeviceId or service name
                        if (deviceId == currentDeviceId || serviceName.contains(currentDeviceId)) {
                            println("⏭️ [DeviceDiscovery.android] Skipping own device: $serviceName (ID: $deviceId)")
                            return
                        }
                        
                        // Create unique key for de-duplication across IPv4/IPv6
                        val deviceKey = "${device.deviceId}:${device.ipAddress}:${device.port}"
                        
                        // Additional check: also check if device with same ID already exists (different IP/port)
                        val existingDeviceWithSameId = _discoveredDevices.value.find { it.deviceId == device.deviceId }
                        
                        // Check if device already exists using the key
                        if (!deviceKeys.contains(deviceKey) && existingDeviceWithSameId == null) {
                            deviceKeys.add(deviceKey)
                            println("📱 [DeviceDiscovery.android] Adding NEW device: ${device.name} at ${device.ipAddress}:${device.port}")
                            _discoveredDevices.value = _discoveredDevices.value + device
                            println("📊 [DeviceDiscovery.android] Total devices: ${_discoveredDevices.value.size}")
                        } else {
                            if (deviceKeys.contains(deviceKey)) {
                                println("⚠️ [DeviceDiscovery.android] Device already exists (same key): ${device.name} (${device.ipAddress}:${device.port})")
                            }
                            if (existingDeviceWithSameId != null) {
                                println("⚠️ [DeviceDiscovery.android] Device with same ID already exists: ${device.name} (ID: ${device.deviceId})")
                                println("   Existing: ${existingDeviceWithSameId.name} at ${existingDeviceWithSameId.ipAddress}:${existingDeviceWithSameId.port}")
                            }
                        }
                    }
                })
            }
            
            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                serviceInfo?.let { service ->
                    println("🔍 [DeviceDiscovery.android] SERVICE LOST: ${service.serviceName}")
                    
                    // Extract device ID from lost service
                    val lostDeviceId = try {
                        val match = Regex("\\(device_.+\\)").find(service.serviceName)
                        match?.value?.removeSurrounding("(", ")") ?: service.serviceName
                    } catch (e: Exception) {
                        service.serviceName
                    }
                    
                    // Remove device from discovered list
                    val beforeCount = _discoveredDevices.value.size
                    _discoveredDevices.value = _discoveredDevices.value.filter { it.deviceId != lostDeviceId }
                    val afterCount = _discoveredDevices.value.size
                    
                    if (beforeCount != afterCount) {
                        println("🗑️ [DeviceDiscovery.android] Removed lost device: $lostDeviceId")
                        println("📊 [DeviceDiscovery.android] Devices remaining: $afterCount")
                        
                        // Clean up device keys
                        deviceKeys.removeAll { key -> key.startsWith("$lostDeviceId:") }
                    }
                }
            }
        }
        
        try {
            manager.discoverServices("_clipboard._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            println("✅ [DeviceDiscovery.android] Service discovery started")
        } catch (e: Exception) {
            println("❌ [DeviceDiscovery.android] Failed to start service discovery: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun registerService(deviceId: String, deviceName: String, port: Int) {
        val manager = nsdManager ?: return
        
        // Use readable device name as service name and add TXT records
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "$deviceName ($deviceId)"
            serviceType = "_clipboard._tcp."
            setPort(port)
            // Add TXT records with metadata
            setAttribute("deviceId", deviceId)
            setAttribute("deviceName", deviceName)
            setAttribute("port", port.toString())
        }
        
        println("📱 [DeviceDiscovery.android] Registering service...")
        println("   Device Name: $deviceName")
        println("   Device ID: $deviceId")
        println("   Port: $port")
        println("   Service Name: ${serviceInfo.serviceName}")
        println("   Service Type: ${serviceInfo.serviceType}")
        
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
                println("✅ [DeviceDiscovery.android] Service REGISTERED successfully!")
                println("   Registered service: ${serviceInfo?.serviceName}")
                println("   Type: ${serviceInfo?.serviceType}")
                println("   Port: ${serviceInfo?.port}")
            }
            
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                println("❌ [DeviceDiscovery.android] Service REGISTRATION FAILED!")
                println("   Error code: $errorCode")
                println("   Service: ${serviceInfo?.serviceName}")
                when (errorCode) {
                    NsdManager.FAILURE_ALREADY_ACTIVE -> println("   Reason: FAILURE_ALREADY_ACTIVE")
                    NsdManager.FAILURE_INTERNAL_ERROR -> println("   Reason: FAILURE_INTERNAL_ERROR")
                    NsdManager.FAILURE_MAX_LIMIT -> println("   Reason: FAILURE_MAX_LIMIT")
                    else -> println("   Reason: Unknown error")
                }
            }
            
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                println("🔄 [DeviceDiscovery.android] Service UNREGISTERED: ${serviceInfo?.serviceName}")
            }
            
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                println("⚠️ [DeviceDiscovery.android] Unregistration failed: error code $errorCode")
            }
        }
        
        try {
            manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
            println("✅ [DeviceDiscovery.android] Service registration started")
        } catch (e: Exception) {
            println("❌ [DeviceDiscovery.android] Failed to register service: ${e.message}")
            e.printStackTrace()
        }
    }
}
