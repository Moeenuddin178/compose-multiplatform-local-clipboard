package org.clipboard.app.repository

import org.clipboard.app.database.entities.SettingsEntity

actual fun loadPersistedSettings(): SettingsEntity? {
    // Android uses in-memory storage during session
    return null
}

actual fun savePersistedSettings(settings: SettingsEntity): Boolean {
    // Android uses in-memory storage during session
    return true
}
