package org.clipboard.app.repository

import org.clipboard.app.database.entities.SettingsEntity

actual fun loadPersistedSettings(): SettingsEntity? {
    // iOS uses in-memory storage during session
    return null
}

actual fun savePersistedSettings(settings: SettingsEntity): Boolean {
    // iOS uses in-memory storage during session
    return true
}
