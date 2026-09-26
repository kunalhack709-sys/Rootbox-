package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.data.local.entities.TerminalHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TerminalHistoryDao {
    @Query("SELECT * FROM terminal_history ORDER BY timestamp DESC LIMIT 100")
    fun getRecentHistory(): Flow<List<TerminalHistoryEntity>>

    @Insert
    suspend fun insertCommand(history: TerminalHistoryEntity)

    @Query("DELETE FROM terminal_history")
    suspend fun clearHistory()
}
