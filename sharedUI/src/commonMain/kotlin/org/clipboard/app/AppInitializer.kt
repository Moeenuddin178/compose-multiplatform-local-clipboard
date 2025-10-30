package org.clipboard.app

import org.clipboard.app.network.ClipboardClient
import kotlin.time.ExperimentalTime
import kotlin.time.Clock
import org.clipboard.app.network.ClipboardServer
import org.clipboard.app.network.DeviceDiscovery
import org.clipboard.app.network.MulticastDiscovery
import org.clipboard.app.platform.ClipboardManager
import org.clipboard.app.repository.ClipboardRepository
import org.clipboard.app.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineExceptionHandler
import org.clipboard.app.AppContainer

expect fun createClipboardManager(): ClipboardManager
expect fun getDeviceName(): String

@OptIn(ExperimentalTime::class)
class AppInitializer {

    lateinit var repository: ClipboardRepository
        private set
    
    lateinit var clipboardManager: ClipboardManager
        private set
    
    lateinit var server: ClipboardServer
        private set
    
    lateinit var client: ClipboardClient
        private set
    
    lateinit var deviceDiscovery: DeviceDiscovery
        private set
    
    lateinit var multicastDiscovery: MulticastDiscovery
        private set
    
    lateinit var clipboardViewModel: ClipboardViewModel
        private set
    
    lateinit var historyViewModel: HistoryViewModel
        private set
    
    lateinit var devicesViewModel: DevicesViewModel
        private set
    
    lateinit var settingsViewModel: SettingsViewModel
        private set
    
    private var deviceId: String = ""
    private var deviceName: String = "My Device"
    private var viewModelsInitialized: Boolean = false
    private var serverStartJob: kotlinx.coroutines.Job? = null
    
    fun areViewModelsReady(): Boolean = viewModelsInitialized
    
    suspend fun initialize() {
        println("🚀 [AppInitializer] Starting initialization...")
        
        try {
            // Initialize shared container (idempotent)
            AppContainer.init(AppContainer.Config(defaultServerPort = 8080))

            // Pull singletons from container
            println("📦 [AppInitializer] Resolving dependencies from AppContainer...")
            repository = AppContainer.repository
            clipboardManager = AppContainer.clipboardManager
            server = AppContainer.server
            client = AppContainer.client
            deviceDiscovery = AppContainer.deviceDiscovery
            multicastDiscovery = AppContainer.multicastDiscovery
            println("✅ [AppInitializer] Dependencies resolved")
            
            // Initialize DeviceDiscovery with context (platform-specific)
            println("🔧 [AppInitializer] Initializing DeviceDiscovery...")
            initializeDeviceDiscovery()
            println("✅ [AppInitializer] DeviceDiscovery initialized")
            
            // Initialize with platform-specific device name
            deviceId = "device_${Clock.System.now().toEpochMilliseconds()}"
            deviceName = getDeviceName()
            println("📱 [AppInitializer] Device ID: $deviceId, Name: $deviceName")
            
            // Initialize ViewModels
            println("🎨 [AppInitializer] Creating ViewModels...")
            clipboardViewModel = ClipboardViewModel(
                clipboardManager = clipboardManager,
                repository = repository,
                client = client,
                server = server,
                deviceId = deviceId,
                deviceName = deviceName
            )
            println("✅ [AppInitializer] ClipboardViewModel created")
            
            historyViewModel = HistoryViewModel(
                repository = repository,
                client = client,
                deviceId = deviceId,
                deviceName = deviceName
            )
            println("✅ [AppInitializer] HistoryViewModel created")
            
            devicesViewModel = DevicesViewModel(
                repository = repository,
                client = client,
                server = server,
                deviceId = deviceId,
                deviceName = deviceName,
                deviceDiscovery = deviceDiscovery
            )
            println("✅ [AppInitializer] DevicesViewModel created")
            
            settingsViewModel = SettingsViewModel(
                repository = repository,
                server = server,
                deviceDiscovery = deviceDiscovery,
                deviceId = deviceId,
                deviceName = deviceName
            )
            println("✅ [AppInitializer] SettingsViewModel created")
            
            // Mark ViewModels as initialized
            viewModelsInitialized = true
            println("✅ [AppInitializer] All ViewModels initialized")
            
            // Start server (auto-start enabled by default)
            // Don't wait for server/discovery to complete - start them asynchronously
            println("🚀 [AppInitializer] Starting server and discovery (async)...")
            if (true) { // TODO: Load auto-start setting from database
                // Wrap server start in supervisorScope to catch all exceptions
                // CRITICAL: On Kotlin Native, uncaught exceptions terminate the app
                val serverExceptionHandler = CoroutineExceptionHandler { _, throwable ->
                    // Don't log CancellationException as errors
                    if (throwable is kotlinx.coroutines.CancellationException) {
                        println("⚠️ [AppInitializer] Server start job cancelled (normal)")
                        return@CoroutineExceptionHandler
                    }
                    // CRITICAL: Log but NEVER rethrow - this prevents app termination
                    println("❌ [AppInitializer] Uncaught exception in server start coroutine: ${throwable.message}")
                    throwable.printStackTrace()
                    // Mark server as not running
                    // Launch stop in background coroutine to handle suspend
                    CoroutineScope(Dispatchers.Default).launch {
                        try {
                            server.stop()
                        } catch (_: Exception) {
                            // Ignore stop errors
                        }
                    }
                    // DO NOT rethrow - this would crash the app on Kotlin Native
                }
                
                val serverStartScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + serverExceptionHandler)
                
                // Launch server start in background with exception handler
                // CRITICAL: Use nonCancellable + supervisorScope to catch ALL exceptions
                // This prevents exceptions from Ktor's internal coroutines from crashing the app
                serverStartJob = serverStartScope.launch {
                    kotlinx.coroutines.supervisorScope {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                            try {
                                val started = server.start(deviceId, deviceName)
                                if (!started) {
                                    println("⚠️ [AppInitializer] Server start() returned false")
                                }
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                // Cancellation is normal - don't log as error
                                println("⚠️ [AppInitializer] Server start cancelled")
                            } catch (e: Exception) {
                                // Catch ANY exception - don't rethrow to prevent crash
                                println("❌ [AppInitializer] Server start() threw exception: ${e.message}")
                                e.printStackTrace()
                                // CRITICAL: DO NOT rethrow - this would crash the app on Kotlin Native
                                // The exception handler will also catch this, but we catch it here too
                                // to prevent it from propagating to Ktor's internal coroutines
                            }
                        }
                    }
                }
                
                // Wait a bit for server to start or fail
                kotlinx.coroutines.delay(1500)
                
                // Check if server is running (check multiple times with delays)
                var serverStartedSuccessfully = false
                repeat(5) {
                    if (server.isRunning.value) {
                        serverStartedSuccessfully = true
                        return@repeat
                    }
                    kotlinx.coroutines.delay(200)
                }
                
                if (!serverStartedSuccessfully) {
                    println("⚠️ [AppInitializer] Server failed to start after checks, but continuing without crash")
                    // Cancel the job if still running
                    try {
                        serverStartJob?.cancel()
                    } catch (_: Exception) {
                        // Ignore cancellation errors
                    }
                    // Don't crash - app can still function without server
                    return
                }
                
                val actualPort = server.actualPort
                println("🔌 [AppInitializer] Server running on port: $actualPort")

                // Start discovery asynchronously without blocking
                // Ensure clean state before starting discovery (like scan button does)
                println("🔄 [AppInitializer] Preparing clean discovery state...")
                deviceDiscovery.stop() // Clean slate like scan button
                multicastDiscovery.stop()

                // Start discovery with minimal delay to avoid blocking UI
                // Use exception handler to prevent crashes from unhandled exceptions
                val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
                    // Don't log cancellation exceptions as errors (they're normal)
                    if (throwable !is kotlinx.coroutines.CancellationException) {
                        println("❌ [AppInitializer] Uncaught exception in discovery coroutine: ${throwable.message}")
                        throwable.printStackTrace()
                    }
                    // Don't rethrow - prevent crash
                }
                CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler).launch {
                    delay(200) // Small delay to ensure server is ready
                    println("🚀 [AppInitializer] Starting initial device discovery...")
                    try {
                        deviceDiscovery.start(deviceId, deviceName, actualPort)
                        multicastDiscovery.start(deviceId, deviceName, actualPort)
                        println("✅ [AppInitializer] Server and discovery started successfully")
                    } catch (e: Exception) {
                        println("⚠️ [AppInitializer] Discovery start error: ${e.message}")
                        e.printStackTrace()
                        // Exception is caught - won't propagate
                    }
                }
            }
            
            println("🎉 [AppInitializer] Initialization complete!")
        } catch (e: Exception) {
            println("❌ [AppInitializer] Initialization failed: ${e.message}")
            println("❌ [AppInitializer] Stack trace: ${e.stackTraceToString()}")
            throw e
        }
    }
    
    fun generateDeviceId(): String {
        return "device_${Clock.System.now().toEpochMilliseconds()}"
    }
    
    fun cleanup() {
        println("🧹 [AppInitializer] Cleaning up all services...")
        try {
            // Stop server in blocking coroutine
            kotlinx.coroutines.runBlocking {
                try {
                    server.stop()
                } catch (e: Exception) {
                    println("⚠️ [AppInitializer] Error stopping server: ${e.message}")
                }
            }
            
            // Stop discovery services
            deviceDiscovery.stop()
            multicastDiscovery.stop()
            
            // Cancel any running coroutines
            serverStartJob?.cancel()
            
            println("✅ [AppInitializer] Cleanup complete")
        } catch (e: Exception) {
            println("⚠️ [AppInitializer] Error during cleanup: ${e.message}")
            e.printStackTrace()
        }
    }
}

// Platform-specific initialization
expect fun org.clipboard.app.AppInitializer.initializeDeviceDiscovery()
