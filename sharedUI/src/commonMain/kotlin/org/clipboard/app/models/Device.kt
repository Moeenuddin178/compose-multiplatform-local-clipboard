package org.clipboard.app.models

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.Instant

@OptIn(ExperimentalTime::class)
@Serializable
data class Device(
    val deviceId: String,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val isApproved: Boolean = false,
    val lastSeen: Long = Clock.System.now().toEpochMilliseconds()
)
