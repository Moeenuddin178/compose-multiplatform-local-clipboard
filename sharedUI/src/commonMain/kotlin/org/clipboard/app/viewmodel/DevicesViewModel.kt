package org.clipboard.app.viewmodel

import kotlinx.coroutines.flow.*
import org.clipboard.app.database.entities.ApprovedDeviceEntity
import org.clipboard.app.models.Device
import org.clipboard.app.network.ClipboardClient
import org.clipboard.app.network.ClipboardServer
import org.clipboard.app.repository.ClipboardRepository
import kotlin.time.ExperimentalTime
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

expect fun getLocalIPAddress(): String

@OptIn(ExperimentalTime::class)
class DevicesViewModel(
    private val repository: ClipboardRepository,
    private val client: ClipboardClient,
    private val server: ClipboardServer,
    private val deviceId: String,
    private val deviceName: String,
    private val deviceDiscovery: org.clipboard.app.network.DeviceDiscovery
) {
    
    val approvedDevices = repository.getAllApprovedDevices()

    // Expose device name for UI
    val currentDeviceName = deviceName

    // Scanning state for UI animation
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()
    
    // Collect discovered devices from DeviceDiscovery
    val discoveredDevices = deviceDiscovery.discoveredDevices
    
    val serverRunning = server.isRunning
    
    // Expose the actual server port (updated on start)
    val serverPort = MutableStateFlow(server.actualPort)

    val localAddress = MutableStateFlow(getLocalIPAddress())
    
    val approvalRequests = server.approvalRequests
    val deviceApprovals = server.deviceApproved
    val deviceUnpairs = server.deviceUnpair

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("❌ [DevicesViewModel] Uncaught exception in coroutine: ${throwable.message}")
        throwable.printStackTrace()
        // Don't rethrow - prevent crash
    }

    private val heartbeatScope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + exceptionHandler
    )
    private var heartbeatJob: Job? = null

    suspend fun startServer(port: Int): Boolean {
        val started = server.start(deviceId, deviceName)
        if (started) {
            serverPort.value = server.actualPort
            startHeartbeat()
        }
        return started
    }
    
    suspend fun stopServer() {
        server.stop()
        heartbeatJob?.cancel()
    }
    
    suspend fun approveDevice(request: ClipboardServer.ApprovalRequest) {
        println("🔄 [DevicesViewModel] Approving device from request: ${request.deviceName} (${request.deviceId}) at ${request.ipAddress}")

        val remotePortForRequester = deviceDiscovery.discoveredDevices.value.firstOrNull { it.deviceId == request.deviceId }?.port ?: 8080

        val deviceEntity = ApprovedDeviceEntity(
            deviceId = request.deviceId,
            name = request.deviceName,
            ipAddress = request.ipAddress,
            port = remotePortForRequester,
            lastSeen = Clock.System.now().toEpochMilliseconds()
        )

        println("💾 [DevicesViewModel] Created device entity: $deviceEntity")

        try {
            repository.insertApprovedDevice(deviceEntity)
            println("✅ [DevicesViewModel] Device inserted into database successfully")
        } catch (e: Exception) {
            println("❌ [DevicesViewModel] Failed to insert device into database: ${e.message}")
            e.printStackTrace()
        }

        // Notify the requester so they add us too (bidirectional)
        println("📤 [DevicesViewModel] Notifying requester about approval...")
        val notified = client.approveDevice(request.ipAddress, remotePortForRequester, this.deviceId, this.deviceName)
        if (notified) {
            println("✅ [DevicesViewModel] Requester notified successfully")
        } else {
            println("⚠️ [DevicesViewModel] Failed to notify requester")
        }

        server.clearApprovalRequest(request)
        println("🧹 [DevicesViewModel] Approval request cleared")
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = heartbeatScope.launch {
            while (true) {
                try {
                    // ping approved devices
                    approvedDevices.collect { devices ->
                        devices.forEach { dev ->
                            val ok = client.ping(dev.ipAddress, dev.port)
                            if (ok) {
            repository.updateApprovedDevice(
                dev.copy(lastSeen = Clock.System.now().toEpochMilliseconds())
            )
                            }
                        }
                        // prune stale (>3 min)
                        val cutoff = kotlin.time.Clock.System.now().toEpochMilliseconds() - 3 * 60 * 1000
                        devices.filter { it.lastSeen < cutoff }.forEach {
                            repository.deleteApprovedDevice(it.deviceId)
                        }
                    }
                } catch (_: Exception) { }
                delay(30_000)
            }
        }
    }
    
    // Request approval from a discovered device (no local auto-approve)
    suspend fun approveDiscoveredDevice(deviceId: String, deviceName: String, ipAddress: String, port: Int) {
        println("🔄 [DevicesViewModel] Requesting approval from: $deviceName ($deviceId) at $ipAddress:$port")

        // If already approved, nothing to do
        val existingDevice = repository.getApprovedDevice(deviceId)
        if (existingDevice != null) {
            println("⚠️ [DevicesViewModel] Device already approved")
            return
        }

        // Send approval request to the remote device; wait for their accept to add locally
        try {
            requestApproval(ipAddress, port)
            println("✅ [DevicesViewModel] Approval request sent")
        } catch (e: Exception) {
            println("❌ [DevicesViewModel] Failed to send approval request: ${e.message}")
        }
    }
    
    suspend fun unpairDevice(deviceId: String) {
        println("🔗❌ [DevicesViewModel] Unpairing device: $deviceId")
        
        // Get the device to find its IP
        val device = repository.getApprovedDevice(deviceId)
        if (device == null) {
            println("⚠️ [DevicesViewModel] Device not found for unpair")
            return
        }
        
        // Delete locally
        repository.deleteApprovedDevice(deviceId)
        println("✅ [DevicesViewModel] Device removed from local list")
        
        // Notify the other device to remove us
        println("📤 [DevicesViewModel] Notifying other device about unpair...")
        val notified = client.unpairDevice(device.ipAddress, device.port, this.deviceId)
        if (notified) {
            println("✅ [DevicesViewModel] Other device notified successfully")
        } else {
            println("⚠️ [DevicesViewModel] Failed to notify other device")
        }
    }
    
    // Handle incoming unpair notifications
    suspend fun handleDeviceUnpair(notification: ClipboardServer.UnpairNotification) {
        println("📨 [DevicesViewModel] Received unpair notification for device: ${notification.deviceId}")
        
        // Remove the device from local list
        val device = repository.getApprovedDevice(notification.deviceId)
        if (device != null) {
            repository.deleteApprovedDevice(notification.deviceId)
            println("✅ [DevicesViewModel] Device removed from list: ${device.name}")
        } else {
            println("⚠️ [DevicesViewModel] Device not found for unpair")
        }
        
        // Clear the notification
        server.clearDeviceUnpair(notification)
    }

    // Handle incoming device approval notifications
    suspend fun handleDeviceApproval(approval: ClipboardServer.DeviceApproval) {
        println("📨 [DevicesViewModel] Received device approval: ${approval.deviceName} (${approval.deviceId}) from ${approval.ipAddress}")

        // Check if already approved
        val existingDevice = repository.getApprovedDevice(approval.deviceId)
        if (existingDevice != null) {
            println("⚠️ [DevicesViewModel] Device already approved, ignoring notification")
            server.clearDeviceApproval(approval)
            return
        }

        // Add the device to approved list
        val remotePortForApproval = deviceDiscovery.discoveredDevices.value.firstOrNull { it.deviceId == approval.deviceId }?.port ?: 8080

        val deviceEntity = ApprovedDeviceEntity(
            deviceId = approval.deviceId,
            name = approval.deviceName,
            ipAddress = approval.ipAddress,
            port = remotePortForApproval,
            lastSeen = kotlin.time.Clock.System.now().toEpochMilliseconds()
        )

        repository.insertApprovedDevice(deviceEntity)
        println("✅ [DevicesViewModel] Device added to approved list via notification: ${approval.deviceName}")

        // Clear the notification
        server.clearDeviceApproval(approval)
    }
    
    suspend fun rejectApprovalRequest(request: ClipboardServer.ApprovalRequest) {
        server.clearApprovalRequest(request)
    }
    
    suspend fun requestApproval(ipAddress: String, port: Int) {
        client.requestApproval(ipAddress, port, deviceId, deviceName)
    }
    
    suspend fun updateLastSeen(deviceId: String) {
        val device = repository.getApprovedDevice(deviceId)
        if (device != null) {
                repository.updateApprovedDevice(
                    device.copy(lastSeen = kotlin.time.Clock.System.now().toEpochMilliseconds())
                )
        }
    }
    
    // Rescan for devices
    suspend fun rescanDevices() {
        println("🔄 [DevicesViewModel] Rescanning for devices...")
        _isScanning.value = true

        try {
            println("🔄 [DevicesViewModel] Current discovered devices count: ${deviceDiscovery.discoveredDevices.value.size}")

            // Stop discovery immediately
            deviceDiscovery.stop()

            // Clear the discovered devices list first
            val currentDevices = deviceDiscovery.discoveredDevices.value
            println("🔄 [DevicesViewModel] Clearing ${currentDevices.size} devices from list")

            // Minimal delay for cleanup - reduced from 500ms to 100ms
            kotlinx.coroutines.delay(100)

            // Restart discovery with actual server port
            val actualPort = server.actualPort
            println("🔄 [DevicesViewModel] Restarting discovery with actual port: $actualPort")
            deviceDiscovery.start(deviceId, deviceName, actualPort)

            // Shorter animation duration - reduced from 2000ms to 800ms for faster feel
            kotlinx.coroutines.delay(800)

        } finally {
            _isScanning.value = false
        }
        
        println("✅ [DevicesViewModel] Discovery restarted")
        println("🔄 [DevicesViewModel] Waiting for devices to be discovered...")
        
        // Give some time for discovery to find devices
        kotlinx.coroutines.delay(2000)
        println("✅ [DevicesViewModel] Rescan complete. Discovered: ${deviceDiscovery.discoveredDevices.value.size} devices")
    }
    
    // Manual add device by IP:Port with reachability check
    suspend fun addManualDevice(ipAddress: String, port: Int, deviceName: String): AddDeviceResult {
        println("🔍 [DevicesViewModel] Adding manual device: $deviceName at $ipAddress:$port")
        
        // Validate IP address format
        if (!isValidIpAddress(ipAddress)) {
            return AddDeviceResult.Error("Invalid IP address format")
        }
        
        // Validate port range
        if (port < 1 || port > 65535) {
            return AddDeviceResult.Error("Port must be between 1 and 65535")
        }
        
        // Check if device already exists
        val existingDevice = repository.getAllApprovedDevices().first().find { 
            it.ipAddress == ipAddress && it.port == port 
        }
        if (existingDevice != null) {
            return AddDeviceResult.Error("Device already exists: ${existingDevice.name}")
        }
        
        // Test reachability via ping
        println("🔍 [DevicesViewModel] Testing reachability to $ipAddress:$port")
        val isReachable = client.ping(ipAddress, port)
        
        if (!isReachable) {
            return AddDeviceResult.Error("Device not reachable at $ipAddress:$port")
        }
        
        // Generate a unique device ID for manual devices
        val manualDeviceId = "manual_${kotlin.time.Clock.System.now().toEpochMilliseconds()}"
        
        // Add device to approved list
        val deviceEntity = ApprovedDeviceEntity(
            deviceId = manualDeviceId,
            name = deviceName,
            ipAddress = ipAddress,
            port = port,
            lastSeen = Clock.System.now().toEpochMilliseconds()
        )
        
        try {
            repository.insertApprovedDevice(deviceEntity)
            println("✅ [DevicesViewModel] Manual device added successfully: $deviceName")
            return AddDeviceResult.Success(deviceEntity)
        } catch (e: Exception) {
            println("❌ [DevicesViewModel] Failed to add manual device: ${e.message}")
            return AddDeviceResult.Error("Failed to add device: ${e.message}")
        }
    }
    
    private fun isValidIpAddress(ip: String): Boolean {
        return try {
            val parts = ip.split(".")
            if (parts.size != 4) return false
            parts.all { part ->
                val num = part.toInt()
                num in 0..255
            }
        } catch (e: Exception) {
            false
        }
    }
    
    sealed class AddDeviceResult {
        data class Success(val device: ApprovedDeviceEntity) : AddDeviceResult()
        data class Error(val message: String) : AddDeviceResult()
    }
}
