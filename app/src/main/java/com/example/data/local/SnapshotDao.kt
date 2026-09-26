package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.SnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM snapshots ORDER BY timestamp DESC")
    fun getAllSnapshots(): Flow<List<SnapshotEntity>>

    @Query("SELECT * FROM snapshots WHERE id = :id LIMIT 1")
    suspend fun getSnapshot(id: String): SnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: SnapshotEntity)

    @Delete
    suspend fun deleteSnapshot(snapshot: SnapshotEntity)

    @Query("DELETE FROM snapshots WHERE id = :id")
    suspend fun deleteById(id: String)
}
