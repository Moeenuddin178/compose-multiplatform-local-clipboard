package org.clipboard.app.repository

import org.clipboard.app.database.entities.SettingsEntity

actual fun loadPersistedSettings(): SettingsEntity? {
    // For JS platform, use in-memory storage during session
    // LocalStorage can be implemented if needed with JSON serialization
    return null
}

actual fun savePersistedSettings(settings: SettingsEntity): Boolean {
    // For JS platform, use in-memory storage during session
    return true
}

