package com.example.core.snapshot

import android.content.Context
import com.example.core.filesystem.VirtualFilesystem
import com.example.data.local.SnapshotDao
import com.example.data.local.VirtualAppDao
import com.example.data.local.entities.SnapshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class SnapshotManager(
    private val context: Context,
    private val fs: VirtualFilesystem,
    private val snapshotDao: SnapshotDao,
    private val appDao: VirtualAppDao
) {
    private val snapshotsDir: File = File(context.filesDir, "rootbox_snapshots").apply {
        if (!exists()) mkdirs()
    }

    suspend fun initializeDefaultSnapshotsIfNeeded() = withContext(Dispatchers.IO) {
        val existing = snapshotDao.getAllSnapshots().firstOrNull() ?: emptyList()
        if (existing.isEmpty()) {
            val s1 = SnapshotEntity(
                id = UUID.randomUUID().toString(),
                name = "Clean Installation",
                description = "Base pristine vAOSP image with standard virtual Linux rootfs.",
                timestamp = System.currentTimeMillis() - 86400000L * 2,
                sizeBytes = 240 * 1024 * 1024L,
                appsCount = 3,
                systemVersion = "Android 14 (vAOSP 64-bit)"
            )
            val s2 = SnapshotEntity(
                id = UUID.randomUUID().toString(),
                name = "Testing Environment",
                description = "Configured virtual environment with Termux, BusyBox and SuperSU installed.",
                timestamp = System.currentTimeMillis() - 86400000L,
                sizeBytes = 480 * 1024 * 1024L,
                appsCount = 6,
                systemVersion = "Android 14 (vAOSP 64-bit)"
            )
            snapshotDao.insertSnapshot(s1)
            snapshotDao.insertSnapshot(s2)
        }
    }

    suspend fun createSnapshot(name: String, description: String): Result<SnapshotEntity> = withContext(Dispatchers.IO) {
        try {
            val id = UUID.randomUUID().toString()
            val backupFolder = File(snapshotsDir, id)
            backupFolder.mkdirs()

            // Backup /data directory
            val dataDir = fs.resolveVirtualPath("/data")
            if (dataDir.exists()) {
                val dest = File(backupFolder, "data_backup")
                dataDir.copyRecursively(dest, overwrite = true)
            }

            val apps = appDao.getAllApps().firstOrNull() ?: emptyList()
            val totalSize = fs.calculateTotalStorageUsed()

            val snapshot = SnapshotEntity(
                id = id,
                name = name.ifBlank { "Snapshot ${System.currentTimeMillis()}" },
                description = description.ifBlank { "User captured virtual state" },
                timestamp = System.currentTimeMillis(),
                sizeBytes = totalSize,
                appsCount = apps.size,
                systemVersion = "Android 14 (vAOSP 64-bit)",
                backupPath = backupFolder.absolutePath
            )

            snapshotDao.insertSnapshot(snapshot)
            Result.success(snapshot)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreSnapshot(snapshotId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val snapshot = snapshotDao.getSnapshot(snapshotId)
                ?: return@withContext Result.failure(Exception("Snapshot not found"))

            val backupFolder = File(snapshotsDir, snapshot.id)
            val dataBackup = File(backupFolder, "data_backup")
            if (dataBackup.exists()) {
                val targetData = fs.resolveVirtualPath("/data")
                targetData.deleteRecursively()
                targetData.mkdirs()
                dataBackup.copyRecursively(targetData, overwrite = true)
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSnapshot(snapshotId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val backupFolder = File(snapshotsDir, snapshotId)
            if (backupFolder.exists()) {
                backupFolder.deleteRecursively()
            }
            snapshotDao.deleteById(snapshotId)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
