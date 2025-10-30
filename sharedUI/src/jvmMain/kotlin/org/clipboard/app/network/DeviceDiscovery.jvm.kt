package org.clipboard.app.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.clipboard.app.models.Device
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener
import javax.jmdns.ServiceInfo
import java.net.InetAddress
import java.net.NetworkInterface

actual class DeviceDiscovery {
    private val _discoveredDevices = MutableStateFlow<List<Device>>(emptyList())
    actual val discoveredDevices: StateFlow<List<Device>> = _discoveredDevices
    
    // Track devices by (deviceId, ip, port) to prevent duplicates across IPv4/IPv6
    private val deviceKeys = mutableSetOf<String>()
    
    private var jmdns: JmDNS? = null
    private var scheduledSearch: kotlinx.coroutines.Job? = null
    
    // Platform detection
    private val isAppleSilicon = System.getProperty("os.arch") == "aarch64" && 
                                 System.getProperty("os.name").lowercase().contains("mac")
    private val isIntelMac = System.getProperty("os.arch") == "x86_64" && 
                            System.getProperty("os.name").lowercase().contains("mac")
    private val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    private val isLinux = System.getProperty("os.name").lowercase().contains("linux")
    
    private val serviceListener = object : ServiceListener {
        override fun serviceAdded(event: ServiceEvent?) {
            println("🔍 [DeviceDiscovery.jvm] SERVICE ADDED")
            println("   Name: ${event?.name}")
            println("   Type: ${event?.type}")
            
            // Request service info
            event?.let { 
                println("   Requesting service info...")
                try {
                    jmdns?.requestServiceInfo(it.type, it.name, 5000)
                } catch (e: Exception) {
                    println("   ❌ Failed to request service info: ${e.message}")
                }
            }
        }
        
        override fun serviceRemoved(event: ServiceEvent?) {
            println("🔍 [DeviceDiscovery.jvm] SERVICE REMOVED: ${event?.name}")
            event?.let { service ->
                _discoveredDevices.value = _discoveredDevices.value.filter { 
                    it.deviceId != service.name 
                }
                println("   Removed device: ${service.name}")
                println("   Remaining devices: ${_discoveredDevices.value.size}")
            }
        }
        
        override fun serviceResolved(event: ServiceEvent?) {
            println("========================================")
            println("✅ [DeviceDiscovery.jvm] SERVICE RESOLVED")
            event?.let { service ->
                println("   Name: ${service.name}")
                println("   Type: ${service.type}")
                
                val info = service.info
                if (info != null) {
                    println("   IP Addresses: ${info.inetAddresses?.map { it.hostAddress }}")
                    println("   Port: ${info.port}")
                    
                    // Extract device name from service name or properties
                    // Format: "Device Name (device_id)"
                    val serviceName = service.name
                    val deviceName = try {
                        val match = Regex("^(.+?)\\s+\\(device_.+\\)$").find(serviceName)
                        match?.groupValues?.get(1) ?: serviceName
                    } catch (e: Exception) {
                        println("   ⚠️ Error parsing device name: ${e.message}")
                        serviceName
                    }
                    println("   Parsed Device Name: $deviceName")
                    
                    // Extract device ID from service name
                    val deviceId = try {
                        val match = Regex("\\(device_.+\\)").find(serviceName)
                        match?.value?.removeSurrounding("(", ")") ?: serviceName
                    } catch (e: Exception) {
                        serviceName
                    }
                    
                    // Use service port directly
                    val finalPort = info.port
                    
                    val device = Device(
                        deviceId = deviceId,
                        name = deviceName,
                        ipAddress = info.inetAddresses?.firstOrNull()?.hostAddress ?: "",
                        port = finalPort,
                        isApproved = false
                    )
                    println("   📱 Device: ${device.name}")
                    println("   🌐 Address: ${device.ipAddress}:${device.port}")

                    // Skip own device by matching currentDeviceId
                    if (deviceId == currentDeviceId) {
                        println("   ⏭️ Skipping own device: ${service.name}")
                        return@let
                    }
                    
                    // Create unique key for de-duplication across IPv4/IPv6
                    val deviceKey = "${device.deviceId}:${device.ipAddress}:${device.port}"
                    
                    // Check if device already exists using the key
                    if (!deviceKeys.contains(deviceKey)) {
                        deviceKeys.add(deviceKey)
                        println("   ✅ Adding NEW device")
                        _discoveredDevices.value = _discoveredDevices.value + device
                    } else {
                        println("   ⚠️ Device already exists, skipping")
                        println("   ⚠️ Existing device: ${device.name} (${device.deviceId})")
                    }
                    
                    println("   📊 Total discovered devices: ${_discoveredDevices.value.size}")
                } else {
                    println("   ❌ Service info is null!")
                }
            }
            println("========================================")
        }
    }
    
    private var currentDeviceId: String = ""
    
    actual suspend fun start(deviceId: String, deviceName: String, port: Int) {
        try {
            println("========================================")
            println("🚀 [DeviceDiscovery.jvm] STARTING DISCOVERY (Call from: ${Thread.currentThread().stackTrace[2].methodName})")
            println("📱 Device ID: $deviceId")
            println("📱 Device Name: $deviceName")
            println("🔌 Port: $port")
            println("========================================")
            currentDeviceId = deviceId
            
            // Platform-specific network interface detection
            val localAddress = getPlatformSpecificNetworkAddress()
            
            println("🌐 [DeviceDiscovery.jvm] Creating JmDNS with address: $localAddress")
            jmdns = JmDNS.create(localAddress)
            println("✅ [DeviceDiscovery.jvm] JmDNS created successfully")
            
            // Register our service with readable name and TXT records
            println("📝 [DeviceDiscovery.jvm] Creating service info...")
            val serviceInfo = ServiceInfo.create(
                "_clipboard._tcp.local.",
                "$deviceName ($deviceId)",
                port,
                0,
                0,
                mapOf(
                    "deviceId" to deviceId, 
                    "deviceName" to deviceName,
                    "port" to port.toString()
                )
            )
            println("📝 [DeviceDiscovery.jvm] Service info created: ${serviceInfo.name}")
            
            println("📤 [DeviceDiscovery.jvm] Registering service...")
            jmdns?.registerService(serviceInfo)
            println("✅ [DeviceDiscovery.jvm] Service registered: $deviceName ($deviceId) on port $port")
            
            // Listen for other services
            println("👂 [DeviceDiscovery.jvm] Adding service listener...")
            jmdns?.addServiceListener("_clipboard._tcp.local.", serviceListener)
            println("✅ [DeviceDiscovery.jvm] Service listener added for _clipboard._tcp.local.")
            
            println("✅ [DeviceDiscovery.jvm] Discovery initialization complete!")
            println("🔄 [DeviceDiscovery.jvm] Listening for services...")
            
            // Start periodic service discovery
            scheduledSearch = CoroutineScope(Dispatchers.Default).launch {
                while (jmdns != null) {
                    delay(3000) // Check every 3 seconds
                    try {
                        println("🔍 [DeviceDiscovery.jvm] Periodic search for services...")
                        val serviceInfos = jmdns?.list("_clipboard._tcp.local.")
                        println("📊 [DeviceDiscovery.jvm] Found ${serviceInfos?.size ?: 0} services")
                        serviceInfos?.forEach { serviceInfo ->
                            println("🔍 [DeviceDiscovery.jvm] Service: ${serviceInfo.name}")
                            if (!serviceInfo.name.contains(deviceId)) { // Don't add our own service
                                println("🔍 [DeviceDiscovery.jvm] Found service: ${serviceInfo.name}")
                                // Manually resolve and add
                                CoroutineScope(Dispatchers.Default).launch {
                                    try {
                                        jmdns?.requestServiceInfo("_clipboard._tcp.local.", serviceInfo.qualifiedName, 5000)
                                    } catch (e: Exception) {
                                        println("⚠️ [DeviceDiscovery.jvm] Failed to request service info: ${e.message}")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("⚠️ [DeviceDiscovery.jvm] Error in periodic search: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ [DeviceDiscovery.jvm] CRITICAL ERROR during start: ${e.message}")
            e.printStackTrace()
        }
    }
    
    actual fun stop() {
        try {
            println("🛑 [DeviceDiscovery.jvm] Stopping discovery...")
            println("   Current devices: ${_discoveredDevices.value.size}")
            
            // Stop periodic search
            scheduledSearch?.cancel()
            scheduledSearch = null
            
            jmdns?.unregisterAllServices()
            jmdns?.close()
            jmdns = null
            
            // Clear discovered devices list and tracking
            _discoveredDevices.value = emptyList()
            deviceKeys.clear()
            println("   Cleared all discovered devices")
        } catch (e: Exception) {
            println("   ⚠️ Error during stop: ${e.message}")
        }
    }
    
    private fun getPlatformSpecificNetworkAddress(): InetAddress {
        return try {
            println("🔍 [DeviceDiscovery.jvm] Platform detection:")
            println("   Apple Silicon: $isAppleSilicon")
            println("   Intel Mac: $isIntelMac")
            println("   Windows: $isWindows")
            println("   Linux: $isLinux")
            
            val interfaces = NetworkInterface.getNetworkInterfaces()
            var candidate: InetAddress? = null
            
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val ifaceName = iface.displayName.lowercase()
                
                // Platform-specific interface preferences
                val isPreferredInterface = when {
                    isAppleSilicon -> ifaceName.contains("en0") || ifaceName.contains("en1") // WiFi/Ethernet on Apple Silicon
                    isIntelMac -> ifaceName.contains("en0") || ifaceName.contains("en1") // WiFi/Ethernet on Intel Mac
                    isWindows -> ifaceName.contains("ethernet") || ifaceName.contains("wi-fi") || ifaceName.contains("wlan")
                    isLinux -> ifaceName.contains("eth0") || ifaceName.contains("wlan0") || ifaceName.contains("enp")
                    else -> true // Default behavior
                }
                
                if (!iface.isLoopback && iface.isUp && isPreferredInterface) {
                    val addresses = iface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                            candidate = addr
                            println("✅ [DeviceDiscovery.jvm] Found preferred interface: ${iface.displayName} with address: $candidate")
                            break
                        }
                    }
                    if (candidate != null) break
                }
            }
            
            candidate ?: InetAddress.getLocalHost().also { 
                println("⚠️ [DeviceDiscovery.jvm] Using fallback localhost: $it")
            }
        } catch (e: Exception) {
            println("❌ [DeviceDiscovery.jvm] Exception getting network interface: ${e.message}")
            e.printStackTrace()
            InetAddress.getLocalHost().also { 
                println("⚠️ [DeviceDiscovery.jvm] Using localhost fallback: $it")
            }
        }
    }
}

