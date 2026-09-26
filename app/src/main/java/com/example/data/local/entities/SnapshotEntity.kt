package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snapshots")
data class SnapshotEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L,
    val appsCount: Int = 0,
    val systemVersion: String = "Android 14 (vAOSP 64-bit)",
    val backupPath: String = ""
)
