package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.entities.NetworkLogEntity
import com.example.data.local.entities.SnapshotEntity
import com.example.data.local.entities.TerminalHistoryEntity
import com.example.data.local.entities.VirtualAppEntity

@Database(
    entities = [
        VirtualAppEntity::class,
        SnapshotEntity::class,
        NetworkLogEntity::class,
        TerminalHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class RootBoxDatabase : RoomDatabase() {
    abstract fun virtualAppDao(): VirtualAppDao
    abstract fun snapshotDao(): SnapshotDao
    abstract fun networkLogDao(): NetworkLogDao
    abstract fun terminalHistoryDao(): TerminalHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: RootBoxDatabase? = null

        fun getInstance(context: Context): RootBoxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RootBoxDatabase::class.java,
                    "rootbox_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
