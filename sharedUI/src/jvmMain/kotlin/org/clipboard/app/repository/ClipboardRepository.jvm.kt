package org.clipboard.app.repository

import org.clipboard.app.database.entities.SettingsEntity
import org.clipboard.app.platform.SettingsPersistence
import org.clipboard.app.platform.SettingsData

actual fun loadPersistedSettings(): SettingsEntity? {
    val settingsData = SettingsPersistence.loadSettings()
    return settingsData?.let { data ->
        SettingsEntity(
            deviceName = data.deviceName,
            serverPort = data.serverPort,
            autoStart = data.autoStart,
            discoveryMethod = data.discoveryMethod,
            keepHistory = data.keepHistory,
            historyRetentionDays = data.historyRetentionDays,
            maxHistoryItems = data.maxHistoryItems
        )
    }
}

actual fun savePersistedSettings(settings: SettingsEntity): Boolean {
    val settingsData = SettingsData(
        deviceName = settings.deviceName,
        serverPort = settings.serverPort,
        autoStart = settings.autoStart,
        discoveryMethod = settings.discoveryMethod,
        keepHistory = settings.keepHistory,
        historyRetentionDays = settings.historyRetentionDays,
        maxHistoryItems = settings.maxHistoryItems
    )
    return SettingsPersistence.saveSettings(settingsData)
}
