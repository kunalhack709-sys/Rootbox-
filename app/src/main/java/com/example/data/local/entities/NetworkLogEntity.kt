package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_logs")
data class NetworkLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val protocol: String = "TCP",
    val source: String = "192.168.100.2:48102",
    val destination: String = "1.1.1.1:443",
    val bytesTransferred: Long = 1024L,
    val status: String = "ALLOWED" // ALLOWED or BLOCKED
)
