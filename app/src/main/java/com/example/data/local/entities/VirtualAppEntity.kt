package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "virtual_apps")
data class VirtualAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val installDate: Long = System.currentTimeMillis(),
    val apkPath: String = "",
    val sizeBytes: Long = 0L,
    val dataSizeBytes: Long = 1048576L, // 1.0 MB default virtual data
    val cacheSizeBytes: Long = 524288L, // 512 KB default virtual cache
    val permissions: String = "",
    val isSystemApp: Boolean = false,
    val isRunning: Boolean = false,
    val rootAccessGranted: Boolean = false,
    val lastLaunched: Long = 0L,
    val iconBase64: String? = null,
    val targetSdk: Int = 34,
    val minSdk: Int = 24,
    val architecture: String = "arm64-v8a"
) {
    val totalSizeBytes: Long
        get() = sizeBytes + dataSizeBytes + cacheSizeBytes
}
