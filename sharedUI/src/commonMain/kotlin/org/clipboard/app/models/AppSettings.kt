package org.clipboard.app.models

import kotlinx.serialization.Serializable

enum class DiscoveryMethod {
    MDNS,
    MULTICAST
}

@Serializable
data class AppSettings(
    val deviceName: String,
    val serverPort: Int = 8080,
    val autoStart: Boolean = true,
    val discoveryMethod: DiscoveryMethod = DiscoveryMethod.MDNS,
    val keepHistory: Boolean = true,
    val historyRetentionDays: Int = -1, // -1 means forever
    val maxHistoryItems: Int = 100
)
