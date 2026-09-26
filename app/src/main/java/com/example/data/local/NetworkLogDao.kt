package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.data.local.entities.NetworkLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkLogDao {
    @Query("SELECT * FROM network_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<NetworkLogEntity>>

    @Insert
    suspend fun insertLog(log: NetworkLogEntity)

    @Query("DELETE FROM network_logs")
    suspend fun clearLogs()
}
