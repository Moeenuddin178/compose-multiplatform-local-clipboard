package org.clipboard.app.database

import org.clipboard.app.database.dao.ApprovedDeviceDao
import org.clipboard.app.database.dao.ClipboardHistoryDao
import org.clipboard.app.database.dao.SettingsDao

interface AppDatabase {
    fun clipboardHistoryDao(): ClipboardHistoryDao
    fun approvedDeviceDao(): ApprovedDeviceDao
    fun settingsDao(): SettingsDao
}

expect fun createAppDatabase(): AppDatabase

