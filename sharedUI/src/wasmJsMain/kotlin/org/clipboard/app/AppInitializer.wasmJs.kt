package org.clipboard.app

import org.clipboard.app.network.DeviceDiscovery
import org.clipboard.app.network.MulticastDiscovery
import org.clipboard.app.platform.ClipboardManager

actual fun createClipboardManager(): ClipboardManager = ClipboardManager()

actual fun getDeviceName(): String = "Web"

actual fun AppInitializer.initializeDeviceDiscovery() {
	// Web has no special initialization required
}
