package org.clipboard.app.network

import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.clipboard.app.models.ClipboardMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

class ClipboardServer(private val port: Int = 8080, private val maxPortAttempts: Int = 5) {

    private var server: EmbeddedServer<*, *>? = null
    private var _actualPort: Int = port
    val actualPort: Int get() = _actualPort

    // Local device identity (set on start)
    private var localDeviceId: String = ""
    private var localDeviceName: String = ""

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    private val _receivedMessages = MutableStateFlow<List<ClipboardMessage>>(emptyList())
    val receivedMessages: StateFlow<List<ClipboardMessage>> = _receivedMessages.asStateFlow()
    
    private val _approvalRequests = MutableStateFlow<List<ApprovalRequest>>(emptyList())
    val approvalRequests: StateFlow<List<ApprovalRequest>> = _approvalRequests.asStateFlow()

    private val _deviceApproved = MutableStateFlow<List<DeviceApproval>>(emptyList())
    val deviceApproved: StateFlow<List<DeviceApproval>> = _deviceApproved.asStateFlow()

    private val _deviceUnpair = MutableStateFlow<List<UnpairNotification>>(emptyList())
    val deviceUnpair: StateFlow<List<UnpairNotification>> = _deviceUnpair.asStateFlow()

    @Serializable
    data class ApprovalRequest(val deviceId: String, val deviceName: String, val ipAddress: String)

    @Serializable
    data class DeviceApproval(val deviceId: String, val deviceName: String, val ipAddress: String)
    
    @Serializable
    data class UnpairNotification(val deviceId: String)
    
    suspend fun start(deviceId: String, deviceName: String): Boolean {
        if (_isRunning.value) {
            println("⚠️ [ClipboardServer] Server already running")
            return true
        }

        // Store local device identity for self-filtering
        localDeviceId = deviceId
        localDeviceName = deviceName

        // Try multiple ports if the initial port is busy
        for (attempt in 0 until maxPortAttempts) {
            val currentPort = port + attempt
            println("🚀 [ClipboardServer] Attempting to start server on port $currentPort (attempt ${attempt + 1}/$maxPortAttempts)...")

            try {
                server = embeddedServer(CIO, port = currentPort) {
                    install(ContentNegotiation) {
                        json(Json {
                            prettyPrint = true
                            isLenient = true
                        })
                    }

                    routing {
                        get("/discover") {
                            println("📨 [ClipboardServer] Discover request received")
                            try {
                                val deviceInfo = DeviceInfo(deviceId, deviceName)
                                println("📨 [ClipboardServer] Responding with device info: $deviceInfo")
                                call.respond(deviceInfo)
                                println("📨 [ClipboardServer] Discover response sent successfully")
                            } catch (e: Exception) {
                                println("❌ [ClipboardServer] Error in discover endpoint: ${e.message}")
                                e.printStackTrace()
                                call.respondText("Internal server error", status = io.ktor.http.HttpStatusCode.InternalServerError)
                            }
                        }

                        get("/ping") {
                            call.respond(mapOf(
                                "status" to "ok",
                                "deviceId" to localDeviceId,
                                "deviceName" to localDeviceName,
                                "port" to _actualPort
                            ))
                        }

                        post("/clipboard") {
                            println("📨 [ClipboardServer] Clipboard message received")
                            val message = call.receive<ClipboardMessage>()
                            _receivedMessages.value = _receivedMessages.value + message
                            call.respond(mapOf("status" to "received"))
                        }

                        post("/approve-device") {
                            println("📨 [ClipboardServer] Device approval notification received!")
                            val request = call.receive<Map<String, String>>()
                            val deviceId = request["deviceId"] ?: return@post
                            val deviceName = request["deviceName"] ?: return@post

                            // Get actual client IP address from headers
                            val ipAddress = call.request.header("X-Forwarded-For")?.split(",")?.first()?.trim()
                                ?: call.request.header("X-Real-IP")
                                ?: call.request.local.remoteHost // Use actual remote host IP

                            // Ignore self notifications
                            if (deviceId == localDeviceId) {
                                println("⏭️ [ClipboardServer] Ignoring self approval notification: $deviceName ($deviceId)")
                                call.respond(mapOf("status" to "ignored"))
                                return@post
                            }

                            println("📨 [ClipboardServer] Adding approved device: $deviceName ($deviceId) at $ipAddress")

                            // This is a direct approval - add the device to approved list
                            _deviceApproved.value = _deviceApproved.value + DeviceApproval(deviceId, deviceName, ipAddress)

                            call.respond(mapOf("status" to "approved"))
                        }

                        post("/unpair-device") {
                            println("📨 [ClipboardServer] Device unpair notification received!")
                            val request = call.receive<Map<String, String>>()
                            val deviceId = request["deviceId"] ?: return@post

                            println("📨 [ClipboardServer] Removing device: $deviceId")
                            
                            // Notify the ViewModel to handle unpair
                            _deviceUnpair.value = _deviceUnpair.value + UnpairNotification(deviceId)

                            call.respond(mapOf("status" to "unpaired"))
                        }

                        post("/approve") {
                            println("📨 [ClipboardServer] Approval request received!")
                            val request = call.receive<Map<String, String>>()
                            val deviceId = request["deviceId"] ?: return@post
                            val deviceName = request["deviceName"] ?: return@post

                            // Get actual client IP address from headers or connection
                            val ipAddress = call.request.header("X-Forwarded-For")?.split(",")?.first()?.trim()
                                ?: call.request.header("X-Real-IP")
                                ?: try {
                                    // Try multiple methods to get remote IP
                                    call.request.local.remoteAddress?.toString()
                                        ?: call.request.local.remoteHost
                                } catch (e: Exception) {
                                    println("⚠️ [ClipboardServer] Error getting remote IP: ${e.message}")
                                    call.request.local.remoteHost
                                }

                            println("📨 [ClipboardServer] Device ID: $deviceId")
                            println("📨 [ClipboardServer] Device Name: $deviceName")
                            println("📨 [ClipboardServer] Client IP: $ipAddress")
                            println("📨 [ClipboardServer] Remote address: ${call.request.local.remoteAddress}")
                            println("📨 [ClipboardServer] Remote host: ${call.request.local.remoteHost}")

                            // Ignore self requests
                            if (deviceId == localDeviceId) {
                                println("⏭️ [ClipboardServer] Ignoring self approval request: $deviceName ($deviceId)")
                                call.respond(mapOf("status" to "ignored"))
                                return@post
                            }

                            // De-duplicate pending requests from same device
                            val approvalRequest = ApprovalRequest(deviceId, deviceName, ipAddress ?: "unknown")
                            val exists = _approvalRequests.value.any { it.deviceId == approvalRequest.deviceId }
                            if (exists) {
                                println("⚠️ [ClipboardServer] Approval request already pending for $deviceName, skipping enqueue")
                                call.respond(mapOf("status" to "duplicate"))
                                return@post
                            }
                            _approvalRequests.value = _approvalRequests.value + approvalRequest

                            println("📨 [ClipboardServer] Approval request added. Total requests: ${_approvalRequests.value.size}")

                            call.respond(mapOf("status" to "requested"))
                        }
                    }
                }

                // CRITICAL: Start server and catch ALL exceptions
                // On Kotlin Native, exceptions from Ktor's internal coroutines can crash the app
                // We MUST catch everything and never let exceptions propagate
                try {
                    server?.start(wait = false)
                    
                    // Give server a moment to bind and throw exception if port is busy
                    // Longer delay to catch CIO's internal coroutine exceptions before they crash
                    delay(1500)
                    
                    _actualPort = currentPort
                    _isRunning.value = true
                    println("✅ [ClipboardServer] Server started successfully on port $currentPort")
                    return true
                } catch (startException: Exception) {
                    // Exception thrown during start() - this happens in Ktor's internal coroutine
                    println("❌ [ClipboardServer] Server start() threw exception on port $currentPort: ${startException.message}")
                    println("❌ [ClipboardServer] Exception type: ${startException::class.simpleName}")
                    startException.printStackTrace()
                    
                    // CRITICAL: Don't try to stop() a server that failed to start
                    // This can cause additional exceptions that crash the app
                    server = null
                    _isRunning.value = false
                    
                        // Check if this is an address in use error for retry logic
                        val exceptionName = startException::class.simpleName ?: ""
                        val isAddressInUse = startException.message?.contains("Address already in use", ignoreCase = true) == true ||
                                startException.message?.contains("EADDRINUSE", ignoreCase = true) == true ||
                                startException.cause?.message?.contains("Address already in use", ignoreCase = true) == true ||
                                startException.cause?.message?.contains("EADDRINUSE", ignoreCase = true) == true ||
                                exceptionName.contains("AddressAlreadyInUseException", ignoreCase = true)
                    
                    if (isAddressInUse && attempt < maxPortAttempts - 1) {
                        // Continue to next port attempt - don't rethrow to prevent crash
                        println("🔄 [ClipboardServer] Port $currentPort busy, trying next port...")
                        continue
                    } else {
                        // Final failure - return false instead of throwing to prevent crash
                        println("❌ [ClipboardServer] Final failure on port $currentPort, not retrying")
                        return false
                    }
                }

            } catch (e: Exception) {
                // Outer catch - handle any unexpected exceptions from server creation
                println("❌ [ClipboardServer] Unexpected exception during server setup on port $currentPort: ${e.message}")
                e.printStackTrace()
                
                server = null
                _isRunning.value = false
                
                // Check for address in use for retry
                val isAddressInUse = e.message?.contains("Address already in use", ignoreCase = true) == true ||
                        e.message?.contains("EADDRINUSE", ignoreCase = true) == true ||
                        e.cause?.message?.contains("Address already in use", ignoreCase = true) == true ||
                        e.cause?.message?.contains("EADDRINUSE", ignoreCase = true) == true
                
                if (isAddressInUse && attempt < maxPortAttempts - 1) {
                    println("🔄 [ClipboardServer] Port $currentPort busy, trying next port...")
                    continue
                } else {
                    // Don't throw - return false to prevent crash
                    return false
                }
            }
        }

        println("❌ [ClipboardServer] Failed to start server on any port after $maxPortAttempts attempts")
        return false
    }

    
    suspend fun stop() {
        println("🛑 [ClipboardServer] Stopping server...")
        try {
            _isRunning.value = false // Set this first
            
            // Stop server with timeout
            server?.stop(gracePeriodMillis = 1000, timeoutMillis = 3000)
            server = null
            
            // Wait for port to be fully released before allowing restart
            delay(1000)
            
            // Clear all state
            _receivedMessages.value = emptyList()
            _approvalRequests.value = emptyList()
            _deviceApproved.value = emptyList()
            _deviceUnpair.value = emptyList()
            
            println("✅ [ClipboardServer] Server stopped and cleaned up")
        } catch (e: Exception) {
            println("❌ [ClipboardServer] Error stopping server: ${e.message}")
            // Force cleanup even on error
            server = null
            _isRunning.value = false
        }
    }
    
    fun clearReceivedMessages() {
        _receivedMessages.value = emptyList()
    }
    
    fun clearApprovalRequest(request: ApprovalRequest) {
        _approvalRequests.value = _approvalRequests.value.filter { it != request }
    }

    fun clearDeviceApproval(approval: DeviceApproval) {
        _deviceApproved.value = _deviceApproved.value.filter { it != approval }
    }
    
    fun clearDeviceUnpair(notification: UnpairNotification) {
        _deviceUnpair.value = _deviceUnpair.value.filter { it != notification }
    }
    
    @Serializable
    data class DeviceInfo(val deviceId: String, val name: String)
}
