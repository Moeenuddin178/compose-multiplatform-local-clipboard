package org.clipboard.app.database.entities

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
// Simple data class for in-memory storage (no Room annotations needed)
data class ApprovedDeviceEntity(
    val deviceId: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val lastSeen: Long = Clock.System.now().toEpochMilliseconds()
)