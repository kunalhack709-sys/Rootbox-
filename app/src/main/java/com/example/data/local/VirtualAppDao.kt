package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.VirtualAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VirtualAppDao {
    @Query("SELECT * FROM virtual_apps ORDER BY isSystemApp ASC, appName ASC")
    fun getAllApps(): Flow<List<VirtualAppEntity>>

    @Query("SELECT * FROM virtual_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): VirtualAppEntity?

    @Query("SELECT COUNT(*) FROM virtual_apps")
    fun getAppsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: VirtualAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<VirtualAppEntity>)

    @Update
    suspend fun updateApp(app: VirtualAppEntity)

    @Query("UPDATE virtual_apps SET isRunning = :isRunning WHERE packageName = :packageName")
    suspend fun setAppRunningState(packageName: String, isRunning: Boolean)

    @Query("UPDATE virtual_apps SET rootAccessGranted = :granted WHERE packageName = :packageName")
    suspend fun setRootAccess(packageName: String, granted: Boolean)

    @Query("UPDATE virtual_apps SET dataSizeBytes = 0, cacheSizeBytes = 0 WHERE packageName = :packageName")
    suspend fun clearAppData(packageName: String)

    @Query("UPDATE virtual_apps SET cacheSizeBytes = 0 WHERE packageName = :packageName")
    suspend fun clearAppCache(packageName: String)

    @Query("UPDATE virtual_apps SET lastLaunched = :timestamp WHERE packageName = :packageName")
    suspend fun updateLastLaunched(packageName: String, timestamp: Long)

    @Query("UPDATE virtual_apps SET isRunning = 0")
    suspend fun stopAllApps()

    @Delete
    suspend fun deleteApp(app: VirtualAppEntity)

    @Query("DELETE FROM virtual_apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)
}
