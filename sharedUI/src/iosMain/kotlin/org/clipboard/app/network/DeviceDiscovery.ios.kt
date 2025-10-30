package org.clipboard.app.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.clipboard.app.models.Device
import platform.Foundation.NSNetService
import platform.Foundation.NSNetServiceBrowser
import platform.Foundation.NSNetServiceBrowserDelegateProtocol
import platform.Foundation.NSNetServiceDelegateProtocol
import platform.darwin.NSObject
import platform.Foundation.NSData
import platform.posix.AF_INET
import platform.posix.sockaddr
import platform.posix.sockaddr_in
import platform.posix.memcpy
import kotlinx.cinterop.*

@OptIn(ExperimentalTime::class, kotlinx.cinterop.ExperimentalForeignApi::class)
actual class DeviceDiscovery {
    private val _discoveredDevices = MutableStateFlow<List<Device>>(emptyList())
    actual val discoveredDevices: StateFlow<List<Device>> = _discoveredDevices
    
    // Track devices by (deviceId, ip, port) to prevent duplicates across IPv4/IPv6
    private val deviceKeys = mutableSetOf<String>()
    
    private var serviceBrowser: NSNetServiceBrowser? = null
    private var localService: NSNetService? = null
    private var browserDelegate: BrowserDelegate? = null
    
    private var currentDeviceId: String = ""
    
        actual suspend fun start(deviceId: String, deviceName: String, port: Int) {
        println("🚀 [DeviceDiscovery.ios] Starting device discovery for device: $deviceName (ID: $deviceId) on port: $port")
        currentDeviceId = deviceId
        browserDelegate = BrowserDelegate(_discoveredDevices, deviceKeys, currentDeviceId)
        // Start browsing for services
        serviceBrowser = NSNetServiceBrowser().apply {
            delegate = browserDelegate
            println("🔍 [DeviceDiscovery.ios] Starting mDNS browsing for _clipboard._tcp services")
            searchForServicesOfType("_clipboard._tcp", inDomain = "")
        }
        
        // Publish our own service with readable name and TXT records
        localService = NSNetService(
            domain = "",
            type = "_clipboard._tcp",
            name = "$deviceName ($deviceId)",
            port = port
        ).apply {
            // Set delegate to monitor publishing status
            delegate = object : NSObject(), NSNetServiceDelegateProtocol {
                override fun netServiceDidPublish(sender: NSNetService) {
                    println("✅ [DeviceDiscovery.ios] Service successfully published: ${sender.name} on port ${sender.port}")
                }
                
                override fun netService(sender: NSNetService, didNotPublish: kotlin.collections.Map<Any?, *>) {
                    println("❌ [DeviceDiscovery.ios] Service failed to publish: ${didNotPublish}")
                }
                
                override fun netServiceWillPublish(sender: NSNetService) {
                    println("📤 [DeviceDiscovery.ios] Service will publish: ${sender.name}")
                }
                
                override fun netServiceDidStop(sender: NSNetService) {
                    println("🛑 [DeviceDiscovery.ios] Service stopped: ${sender.name}")
                }
                
                override fun netServiceDidResolveAddress(sender: NSNetService) {
                    println("✅ [DeviceDiscovery.ios] Local service resolved: ${sender.name}")
                }
            }
            
            println("📡 [DeviceDiscovery.ios] Publishing service: $deviceName ($deviceId) on port $port")
            // Note: iOS TXT record setting would require additional platform-specific code
            publishWithOptions(0u)
        }
    }
    
    actual fun stop() {
        serviceBrowser?.stop()
        serviceBrowser = null
        localService?.stop()
        localService = null
        // Clear discovered devices list and tracking
        _discoveredDevices.value = emptyList()
        deviceKeys.clear()
    }
    
    private class BrowserDelegate(
        private val devices: MutableStateFlow<List<Device>>,
        private val deviceKeys: MutableSet<String>,
        private val currentDeviceId: String
    ) : NSObject(), NSNetServiceBrowserDelegateProtocol {
        
        // Create coroutine scope with exception handler to prevent crashes
        // On iOS/Native, unhandled exceptions will crash the app, so we MUST catch everything
        private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            println("❌ [DeviceDiscovery.ios] Uncaught exception in coroutine: ${throwable.message}")
            println("❌ [DeviceDiscovery.ios] Exception type: ${throwable::class.simpleName}")
            throwable.printStackTrace()
            // Do NOT rethrow - this would crash the app on iOS/Native
            // The exception is logged but won't propagate
        }
        
        private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob() + exceptionHandler)
        
        override fun netServiceBrowser(
            browser: platform.Foundation.NSNetServiceBrowser,
            didFindService: platform.Foundation.NSNetService,
            moreComing: Boolean
        ) {
            println("📱 [DeviceDiscovery.ios] BrowserDelegate.netServiceBrowser CALLED")
            println("📱 [DeviceDiscovery.ios] More coming: $moreComing")
            // NSNetServiceBrowser callbacks are typically on main thread, but use coroutine for safety
            scope.launch {
                try {
                    println("📱 [DeviceDiscovery.ios] BrowserDelegate: Service discovered!")
                    // Start resolution asynchronously - resolveWithTimeout doesn't block, it just starts the process
                    val serviceName = didFindService.name ?: ""
                    println("📱 [DeviceDiscovery.ios] Service name: $serviceName")
                    
                    // Parse device name from format "Device Name (device_id)" before resolution
                    val deviceName = try {
                        // Ensure serviceName is not null and convert to String safely
                        val safeServiceName = serviceName?.toString() ?: ""
                        println("🔍 [DeviceDiscovery.ios] Raw service name: '$safeServiceName'")

                        val match = Regex("^(.+?)\\s+\\(device_.+\\)$").find(safeServiceName)
                        val parsedName = match?.groupValues?.get(1) ?: safeServiceName

                        // Ensure result is a valid Kotlin String
                        parsedName.toString()
                    } catch (e: Exception) {
                        println("⚠️ [DeviceDiscovery.ios] Error parsing device name: ${e.message}")
                        try {
                            serviceName?.toString() ?: "Unknown Device"
                        } catch (e2: Exception) {
                            "Unknown Device"
                        }
                    }
                    
                    // Extract device ID from service name
                    val deviceId = try {
                        val safeServiceName = serviceName?.toString() ?: ""
                        val match = Regex("\\(device_.+\\)").find(safeServiceName)
                        match?.value?.removeSurrounding("(", ")") ?: safeServiceName
                    } catch (e: Exception) {
                        println("⚠️ [DeviceDiscovery.ios] Error parsing device ID: ${e.message}")
                        try {
                            serviceName?.toString() ?: "unknown_device"
                        } catch (e2: Exception) {
                            "unknown_device"
                        }
                    }
                    
                    // Get port from service (before resolution completes)
                    val servicePort = try {
                        // On iOS, port might be NSNumber, convert safely
                        val nsPort = didFindService.port
                        println("🔍 [DeviceDiscovery.ios] Raw port value: $nsPort (type: ${nsPort::class.simpleName})")

                        // Handle NSNumber conversion safely
                        when (nsPort) {
                            is Number -> nsPort.toInt()
                            is String -> nsPort.toIntOrNull() ?: 8080
                            else -> {
                                // Try toString() for other NSObject types
                                try {
                                    nsPort.toString().toIntOrNull() ?: 8080
                                } catch (e: Exception) {
                                    println("⚠️ [DeviceDiscovery.ios] Failed to convert port object to string")
                                    8080
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("⚠️ [DeviceDiscovery.ios] Error getting port: ${e.message}")
                        8080 // Default port
                    }
                    
                    // CRITICAL: Skip own device to prevent self-discovery conflicts
                    // This prevents desktop app and iOS emulator on same Mac from discovering each other as separate devices
                    if (deviceId == currentDeviceId) {
                        println("⏭️ [DeviceDiscovery.ios] Skipping own device: $deviceName (ID: $deviceId)")
                        return@launch
                    }
                    
                    // Create unique key for de-duplication
                    val deviceKey = "${deviceId}:${deviceName}:${servicePort}"
                    println("🔑 [DeviceDiscovery.ios] Device key: $deviceKey")
                    
                    // Check if device already exists using the key
                    // Note: Since we're on main thread via coroutine, no need for explicit synchronization
                    if (!deviceKeys.contains(deviceKey)) {
                        deviceKeys.add(deviceKey)
                        
                        // Add device immediately with available info (resolution happens in background)
                        val safeDeviceId = try {
                            deviceId.toString()
                        } catch (e: Exception) {
                            "unknown_device_${Clock.System.now().toEpochMilliseconds()}"
                        }

                        val safeDeviceName = try {
                            deviceName.toString()
                        } catch (e: Exception) {
                            "Unknown Device"
                        }

                        val device = Device(
                            deviceId = safeDeviceId,
                            name = safeDeviceName,
                            ipAddress = "0.0.0.0", // Will be set when resolution completes
                            port = servicePort, // Already converted to Int above
                            isApproved = false
                        )

                        println("📱 [DeviceDiscovery.ios] Adding device: ${device.name} at port ${device.port}")
                        devices.value = devices.value + device
                        
                        // Create a delegate to handle resolution and extract IP address
                        val serviceDelegate = ServiceDelegate(safeDeviceId, devices, deviceKeys)
                        didFindService.delegate = serviceDelegate
                        
                        // Start async resolution - this is non-blocking, it just initiates the resolution
                        // Resolution will happen in background and delegate will update IP address
                        println("🔍 [DeviceDiscovery.ios] Starting resolution for device: $deviceId (${device.name})")
                        try {
                            didFindService.resolveWithTimeout(10.0)
                            println("✅ [DeviceDiscovery.ios] Resolution initiated successfully for device: $deviceId")
                        } catch (e: Exception) {
                            println("⚠️ [DeviceDiscovery.ios] Error starting resolution: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    println("❌ [DeviceDiscovery.ios] Error handling discovered service: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
        
        override fun netServiceBrowser(
            browser: platform.Foundation.NSNetServiceBrowser,
            didNotSearch: kotlin.collections.Map<Any?, *>
        ) {
            println("❌ [DeviceDiscovery.ios] BrowserDelegate.netServiceBrowser.didNotSearch CALLED")
            println("❌ [DeviceDiscovery.ios] Error details: $didNotSearch")
        }
        
        override fun netServiceBrowserDidStopSearch(browser: platform.Foundation.NSNetServiceBrowser) {
            println("🛑 [DeviceDiscovery.ios] BrowserDelegate.netServiceBrowserDidStopSearch CALLED")
        }
        
        override fun netServiceBrowserWillSearch(browser: platform.Foundation.NSNetServiceBrowser) {
            println("🚀 [DeviceDiscovery.ios] BrowserDelegate.netServiceBrowserWillSearch CALLED")
        }
    }
    
    // Delegate to handle service resolution and extract IP address
    private class ServiceDelegate(
        private val deviceId: String,
        private val devices: MutableStateFlow<List<Device>>,
        private val deviceKeys: MutableSet<String>
    ) : NSObject(), NSNetServiceDelegateProtocol {
        
        private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            println("❌ [DeviceDiscovery.ios.ServiceDelegate] Uncaught exception: ${throwable.message}")
            throwable.printStackTrace()
        }
        
        private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob() + exceptionHandler)
        
        override fun netServiceDidResolveAddress(sender: NSNetService) {
            println("✅ [DeviceDiscovery.ios] netServiceDidResolveAddress CALLED for device $deviceId")
            scope.launch {
                try {
                    println("🔍 [DeviceDiscovery.ios] Getting addresses for device $deviceId...")
                    val addresses = sender.addresses
                    if (addresses == null) {
                        println("⚠️ [DeviceDiscovery.ios] Service resolved but addresses is null")
                        return@launch
                    }
                    
                    // NSArray access in Kotlin Native
                    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                    val addressCount = addresses.count().toInt()
                    println("🔍 [DeviceDiscovery.ios] Address count for device $deviceId: $addressCount")
                    if (addressCount == 0) {
                        println("⚠️ [DeviceDiscovery.ios] Service resolved but no addresses found")
                        return@launch
                    }
                    
                    // Extract IPv4 address from socket address
                    var ipAddress: String? = null
                    var resolvedPort: Int = -1
                    
                    println("🔍 [DeviceDiscovery.ios] Resolving addresses for device $deviceId, count: $addressCount")
                    
                    for (i in 0 until addressCount) {
                        @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                        val addressData = try {
                            addresses[i] as? NSData
                        } catch (e: Exception) {
                            println("⚠️ [DeviceDiscovery.ios] Error accessing address at index $i: ${e.message}")
                            null
                        }
                        if (addressData == null) {
                            println("⚠️ [DeviceDiscovery.ios] Address data is null at index $i")
                            continue
                        }
                        
                        @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                        val addressLength = addressData.length.toInt()
                        println("🔍 [DeviceDiscovery.ios] Address $i length: $addressLength")
                        
                        if (addressLength < 8) {
                            println("⚠️ [DeviceDiscovery.ios] Address too short: $addressLength")
                            continue
                        }
                        
                        // Skip IPv6 addresses (length 28 = AF_INET6)
                        if (addressLength == 28) {
                            println("⚠️ [DeviceDiscovery.ios] Skipping IPv6 address (length=28)")
                            continue
                        }
                        
                        // Read socket address structure
                        @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                        val bytes = addressData.bytes
                        if (bytes == null) {
                            println("⚠️ [DeviceDiscovery.ios] Address bytes are null")
                            continue
                        }
                        
                        // Try to parse as IPv4 sockaddr_in
                        try {
                            // Read bytes into ByteArray using memcpy
                            val bytesArray = ByteArray(minOf(addressLength, 28))
                            
                            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                            bytesArray.usePinned { pinned ->
                                bytes?.let { blob ->
                                    memcpy(pinned.addressOf(0), blob, minOf(addressLength, 28).toULong())
                                }
                            }
                            
                            // Print raw bytes for debugging
                            val hexString = bytesArray.take(8).joinToString(" ") { it.toString(16).padStart(2, '0') }
                            println("🔍 [DeviceDiscovery.ios] Raw bytes (first 8): $hexString")
                            
                            // Extract family from byte 1 (BSD sockaddr layout)
                            // BSD/Darwin sockaddr: byte 0 = sa_len, byte 1 = sa_family
                            val family = bytesArray[1].toUByte().toInt() and 0xFF
                            
                            println("🔍 [DeviceDiscovery.ios] Address family: $family (AF_INET=$AF_INET)")
                            
                            if (family == AF_INET.toInt() && addressLength >= 16) {
                                // sockaddr_in structure layout:
                                // sa_family: 1 byte at offset 0
                                // sin_port: 2 bytes (offset 2-3)
                                // sin_addr: 4 bytes (offset 4-7) - this is what we need
                                
                                // Read IP address bytes from offset 4
                                val b1 = (bytesArray[4].toInt() and 0xFF)
                                val b2 = (bytesArray[5].toInt() and 0xFF)
                                val b3 = (bytesArray[6].toInt() and 0xFF)
                                val b4 = (bytesArray[7].toInt() and 0xFF)
                                
                                ipAddress = "$b1.$b2.$b3.$b4"
                                
                                // Extract port from sockaddr_in (bytes 2-3, network byte order/big-endian)
                                val portByte1 = (bytesArray[2].toInt() and 0xFF)
                                val portByte2 = (bytesArray[3].toInt() and 0xFF)
                                resolvedPort = (portByte1 shl 8) or portByte2
                                
                                println("✅ [DeviceDiscovery.ios] Extracted IP address: $ipAddress for device $deviceId")
                                println("✅ [DeviceDiscovery.ios] Extracted port: $resolvedPort")
                                break
                            } else {
                                println("⚠️ [DeviceDiscovery.ios] Not IPv4 (family=$family, length=$addressLength)")
                            }
                        } catch (e: Exception) {
                            println("❌ [DeviceDiscovery.ios] Error parsing address bytes: ${e.message}")
                            e.printStackTrace()
                            continue
                        }
                    }
                    
                    val finalIpAddress = ipAddress
                    if (finalIpAddress != null && finalIpAddress.isNotEmpty()) {
                        // Update device with IP address
                        // Since we're in a coroutine scope, no need for synchronized
                        val currentDevices = devices.value.toMutableList()
                        val deviceIndex = currentDevices.indexOfFirst { it.deviceId == deviceId }
                        if (deviceIndex >= 0) {
                            val existingDevice = currentDevices[deviceIndex]
                            // Always update IP address and port, even if one already exists (in case it changed)
                            val updatedDevice = existingDevice.copy(
                                ipAddress = finalIpAddress,
                                port = resolvedPort
                            )
                            currentDevices[deviceIndex] = updatedDevice
                            devices.value = currentDevices.toList()
                            println("✅ [DeviceDiscovery.ios] Updated device ${updatedDevice.name} with IP: $finalIpAddress")
                            println("✅ [DeviceDiscovery.ios] Total devices in list: ${currentDevices.size}")
                        } else {
                            println("⚠️ [DeviceDiscovery.ios] Device $deviceId not found in current devices list")
                            println("⚠️ [DeviceDiscovery.ios] Current devices: ${currentDevices.map { it.deviceId }}")
                        }
                    } else {
                        println("⚠️ [DeviceDiscovery.ios] Could not extract IP address for device $deviceId")
                    }
                } catch (e: Exception) {
                    println("❌ [DeviceDiscovery.ios] Error in netServiceDidResolveAddress: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
        
        override fun netService(sender: NSNetService, didNotResolve: kotlin.collections.Map<Any?, *>) {
            println("⚠️ [DeviceDiscovery.ios] Service resolution failed for device $deviceId")
            println("⚠️ [DeviceDiscovery.ios] Did not resolve error: $didNotResolve")
            // Retry resolution after a delay
            scope.launch {
                try {
                    kotlinx.coroutines.delay(3000)
                    println("🔄 [DeviceDiscovery.ios] Retrying resolution for device $deviceId")
                    sender.resolveWithTimeout(5.0)
                } catch (e: Exception) {
                    println("❌ [DeviceDiscovery.ios] Error retrying resolution: ${e.message}")
                }
            }
        }
    }
}

