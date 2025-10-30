package org.clipboard.app.database.dao

import kotlinx.coroutines.flow.Flow
import org.clipboard.app.database.entities.ClipboardHistoryEntity

interface ClipboardHistoryDao {
    fun getAllHistory(): Flow<List<ClipboardHistoryEntity>>
    fun searchHistory(query: String): Flow<List<ClipboardHistoryEntity>>
    suspend fun insertClipboard(clipboard: ClipboardHistoryEntity)
    suspend fun deleteClipboard(id: Long)
    suspend fun deleteAll()
    suspend fun getCount(): Int
    fun getRecent(limit: Int): Flow<List<ClipboardHistoryEntity>>
}

