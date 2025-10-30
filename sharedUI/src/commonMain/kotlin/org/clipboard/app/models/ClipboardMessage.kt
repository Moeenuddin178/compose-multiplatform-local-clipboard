package org.clipboard.app.models

import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Serializable
data class ClipboardMessage(
    val text: String,
    val deviceId: String,
    val deviceName: String,
    val timestamp: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
)
