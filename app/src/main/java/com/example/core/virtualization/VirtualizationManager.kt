package com.example.core.virtualization

import android.content.Context
import com.example.core.apk.ApkManager
import com.example.core.filesystem.VirtualFilesystem
import com.example.core.network.VirtualNetworkManager
import com.example.core.snapshot.SnapshotManager
import com.example.core.terminal.VirtualTerminalEngine
import com.example.data.local.RootBoxDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VirtualizationManager(
    val context: Context,
    val scope: CoroutineScope
) {
    val database = RootBoxDatabase.getInstance(context)
    val fs = VirtualFilesystem(context)
    val instance = VirtualAndroidInstance(context, fs, scope)
    val terminal = VirtualTerminalEngine(fs, database.virtualAppDao())
    val apkManager = ApkManager(context, fs, database.virtualAppDao())
    val networkManager = VirtualNetworkManager(context, database.networkLogDao(), scope)
    val snapshotManager = SnapshotManager(context, fs, database.snapshotDao(), database.virtualAppDao())

    init {
        scope.launch(Dispatchers.IO) {
            apkManager.initializeDefaultAppsIfNeeded()
            snapshotManager.initializeDefaultSnapshotsIfNeeded()
        }
    }
}
