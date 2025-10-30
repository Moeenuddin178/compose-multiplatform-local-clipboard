package org.clipboard.app

import org.clipboard.app.network.ClipboardClient
import org.clipboard.app.network.ClipboardServer
import org.clipboard.app.network.DeviceDiscovery
import org.clipboard.app.network.MulticastDiscovery
import org.clipboard.app.platform.ClipboardManager
import org.clipboard.app.repository.ClipboardRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object AppContainer {

	data class Config(
		val defaultServerPort: Int = 8080
	)

	private var isInitialized: Boolean = false
	private val initMutex = Mutex()

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

	suspend fun init(config: Config = Config()) {
		initMutex.withLock {
			if (isInitialized) return

			repository = ClipboardRepository()
			clipboardManager = createClipboardManager()
			server = ClipboardServer(port = config.defaultServerPort)
			client = ClipboardClient()
			deviceDiscovery = DeviceDiscovery()
			multicastDiscovery = MulticastDiscovery()

			isInitialized = true
		}
	}

	suspend fun reset() {
		initMutex.withLock {
			isInitialized = false
		}
	}
}



