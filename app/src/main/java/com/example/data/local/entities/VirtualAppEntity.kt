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
    val permissions: String = "",
    val isSystemApp: Boolean = false,
    val isRunning: Boolean = false,
    val iconBase64: String? = null,
    val targetSdk: Int = 34,
    val minSdk: Int = 24
)
