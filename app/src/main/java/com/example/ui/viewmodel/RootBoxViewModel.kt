package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.filesystem.VirtualFileItem
import com.example.core.network.NetworkConfig
import com.example.core.network.NetworkStats
import com.example.core.terminal.LineType
import com.example.core.terminal.TerminalLine
import com.example.core.virtualization.HardwareConfig
import com.example.core.virtualization.InstanceStatus
import com.example.core.virtualization.VirtualizationManager
import com.example.data.local.entities.NetworkLogEntity
import com.example.data.local.entities.SnapshotEntity
import com.example.data.local.entities.VirtualAppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class NavigationTab(val label: String) {
    DASHBOARD("Dashboard"),
    TERMINAL("Terminal"),
    FILES("Files"),
    APPS("Apps"),
    NETWORK("Network"),
    SNAPSHOTS("Snapshots"),
    SETTINGS("Settings")
}

class RootBoxViewModel(application: Application) : AndroidViewModel(application) {

    val vManager = VirtualizationManager(application, viewModelScope)

    private val _currentTab = MutableStateFlow(NavigationTab.DASHBOARD)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    val instanceStatus: StateFlow<InstanceStatus> = vManager.instance.status
    val hardwareConfig: StateFlow<HardwareConfig> = vManager.instance.hardwareConfig
    val cpuUsage: StateFlow<Float> = vManager.instance.cpuUsage
    val ramUsageMb: StateFlow<Float> = vManager.instance.ramUsageMb
    val storageUsedBytes: StateFlow<Long> = vManager.instance.storageUsedBytes

    private val _uptimeStr = MutableStateFlow("00:00:00")
    val uptimeStr: StateFlow<String> = _uptimeStr.asStateFlow()

    val installedApps: StateFlow<List<VirtualAppEntity>> =
        vManager.database.virtualAppDao().getAllApps()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val snapshots: StateFlow<List<SnapshotEntity>> =
        vManager.database.snapshotDao().getAllSnapshots()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val networkConfig: StateFlow<NetworkConfig> = vManager.networkManager.config
    val networkStats: StateFlow<NetworkStats> = vManager.networkManager.stats
    val networkLogs: StateFlow<List<NetworkLogEntity>> =
        vManager.database.networkLogDao().getRecentLogs()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Terminal state
    private val _terminalLines = MutableStateFlow<List<TerminalLine>>(
        listOf(
            TerminalLine("=== RootBox Virtual Android Environment (vAOSP 14) ===", LineType.SYSTEM),
            TerminalLine("Kernel: 6.1.0-rootbox-vAOSP aarch64", LineType.SYSTEM),
            TerminalLine("Virtual root access is enabled (UID 0). Physical device remains 100% unrooted.", LineType.WARNING),
            TerminalLine("Type 'help' to view available commands.", LineType.SYSTEM),
            TerminalLine("", LineType.OUTPUT)
        )
    )
    val terminalLines: StateFlow<List<TerminalLine>> = _terminalLines.asStateFlow()

    private val _terminalPrompt = MutableStateFlow(vManager.terminal.getPrompt())
    val terminalPrompt: StateFlow<String> = _terminalPrompt.asStateFlow()

    // File Manager state
    private val _currentVirtualPath = MutableStateFlow("/")
    val currentVirtualPath: StateFlow<String> = _currentVirtualPath.asStateFlow()

    private val _currentVirtualFiles = MutableStateFlow<List<VirtualFileItem>>(emptyList())
    val currentVirtualFiles: StateFlow<List<VirtualFileItem>> = _currentVirtualFiles.asStateFlow()

    private val _selectedFileForEdit = MutableStateFlow<Pair<String, String>?>(null)
    val selectedFileForEdit: StateFlow<Pair<String, String>?> = _selectedFileForEdit.asStateFlow()

    private val _selectedAppForDetail = MutableStateFlow<VirtualAppEntity?>(null)
    val selectedAppForDetail: StateFlow<VirtualAppEntity?> = _selectedAppForDetail.asStateFlow()

    private val _runningAppSim = MutableStateFlow<VirtualAppEntity?>(null)
    val runningAppSim: StateFlow<VirtualAppEntity?> = _runningAppSim.asStateFlow()

    private val _showSetupWizard = MutableStateFlow(false)
    val showSetupWizard: StateFlow<Boolean> = _showSetupWizard.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        refreshVirtualFiles("/")
        startUptimeTicker()
    }

    private fun startUptimeTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                _uptimeStr.value = vManager.instance.formatUptime()
            }
        }
    }

    fun setTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun startInstance() {
        vManager.instance.start()
        showMessage("Starting RootBox virtual instance...")
    }

    fun stopInstance() {
        vManager.instance.stop()
        showMessage("RootBox virtual instance stopped.")
    }

    fun pauseInstance() {
        vManager.instance.pause()
        showMessage("RootBox virtual instance paused.")
    }

    fun resumeInstance() {
        vManager.instance.resume()
        showMessage("RootBox virtual instance resumed.")
    }

    fun restartInstance() {
        vManager.instance.restart()
        showMessage("Restarting RootBox virtual instance...")
    }

    // Terminal
    fun sendTerminalCommand(input: String) {
        if (input.isBlank()) return
        viewModelScope.launch {
            val lines = vManager.terminal.executeCommand(input)
            if (lines.any { it.text == "__CLEAR_SCREEN__" }) {
                _terminalLines.value = emptyList()
            } else {
                _terminalLines.value = _terminalLines.value + lines
            }
            _terminalPrompt.value = vManager.terminal.getPrompt()
        }
    }

    fun clearTerminal() {
        _terminalLines.value = emptyList()
    }

    // File Manager
    fun navigateVirtualDir(virtualPath: String) {
        _currentVirtualPath.value = virtualPath
        refreshVirtualFiles(virtualPath)
    }

    fun navigateUp() {
        val current = _currentVirtualPath.value
        if (current == "/" || current.isEmpty()) return
        val parent = java.io.File(current).parent ?: "/"
        navigateVirtualDir(parent)
    }

    fun refreshVirtualFiles(path: String = _currentVirtualPath.value) {
        viewModelScope.launch(Dispatchers.IO) {
            val files = vManager.fs.listFiles(path)
            _currentVirtualFiles.value = files
        }
    }

    fun openFileForEdit(virtualPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val content = vManager.fs.readFile(virtualPath)
            _selectedFileForEdit.value = Pair(virtualPath, content)
        }
    }

    fun closeFileEdit() {
        _selectedFileForEdit.value = null
    }

    fun saveFileEdit(virtualPath: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = vManager.fs.writeFile(virtualPath, content)
            if (ok) {
                showMessage("Saved $virtualPath successfully.")
                refreshVirtualFiles()
                _selectedFileForEdit.value = null
            } else {
                showMessage("Failed to save $virtualPath.")
            }
        }
    }

    fun deleteVirtualFile(virtualPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = vManager.fs.delete(virtualPath)
            if (ok) {
                showMessage("Deleted $virtualPath")
                refreshVirtualFiles()
            } else {
                showMessage("Could not delete $virtualPath")
            }
        }
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val current = _currentVirtualPath.value
            val target = if (current == "/") "/$name" else "$current/$name"
            val ok = vManager.fs.createDirectory(target)
            if (ok) {
                showMessage("Folder created: $name")
                refreshVirtualFiles()
            } else {
                showMessage("Failed to create folder.")
            }
        }
    }

    fun createNewFile(name: String, content: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val current = _currentVirtualPath.value
            val target = if (current == "/") "/$name" else "$current/$name"
            val ok = vManager.fs.writeFile(target, content)
            if (ok) {
                showMessage("File created: $name")
                refreshVirtualFiles()
            } else {
                showMessage("Failed to create file.")
            }
        }
    }

    fun importFileToVirtual(uri: Uri, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                    val ok = vManager.fs.importFromInputStream(_currentVirtualPath.value, fileName, stream)
                    if (ok) {
                        showMessage("Imported $fileName into virtual storage.")
                        refreshVirtualFiles()
                    } else {
                        showMessage("Import failed.")
                    }
                }
            } catch (e: Exception) {
                showMessage("Error importing: ${e.localizedMessage}")
            }
        }
    }

    fun exportFileFromVirtual(virtualPath: String, destUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.openOutputStream(destUri)?.use { stream ->
                    val ok = vManager.fs.exportToOutputStream(virtualPath, stream)
                    if (ok) {
                        showMessage("Exported $virtualPath successfully.")
                    } else {
                        showMessage("Export failed.")
                    }
                }
            } catch (e: Exception) {
                showMessage("Error exporting: ${e.localizedMessage}")
            }
        }
    }

    // Apps & APK
    fun installApk(uri: Uri) {
        viewModelScope.launch {
            showMessage("Parsing and installing APK into virtual container...")
            val result = vManager.apkManager.installApkFromUri(uri)
            result.onSuccess { app ->
                showMessage("Installed ${app.appName} into RootBox.")
            }.onFailure { err ->
                showMessage("Installation failed: ${err.message}")
            }
        }
    }

    fun uninstallApp(packageName: String) {
        viewModelScope.launch {
            val ok = vManager.apkManager.uninstallApp(packageName)
            if (ok) {
                showMessage("Uninstalled $packageName.")
                if (_selectedAppForDetail.value?.packageName == packageName) {
                    _selectedAppForDetail.value = null
                }
            } else {
                showMessage("Failed to uninstall $packageName.")
            }
        }
    }

    fun selectAppDetail(app: VirtualAppEntity?) {
        _selectedAppForDetail.value = app
    }

    fun launchVirtualApp(app: VirtualAppEntity) {
        viewModelScope.launch {
            vManager.apkManager.launchApp(app.packageName)
            _runningAppSim.value = app
            showMessage("Launched ${app.appName} inside RootBox container.")
        }
    }

    fun closeRunningAppSim() {
        val app = _runningAppSim.value
        if (app != null) {
            viewModelScope.launch {
                vManager.apkManager.stopApp(app.packageName)
            }
        }
        _runningAppSim.value = null
    }

    fun stopVirtualApp(app: VirtualAppEntity) {
        viewModelScope.launch {
            vManager.apkManager.stopApp(app.packageName)
            showMessage("Stopped ${app.appName}.")
        }
    }

    // Snapshots
    fun createSnapshot(name: String, desc: String) {
        viewModelScope.launch {
            showMessage("Capturing RootBox virtual snapshot...")
            val result = vManager.snapshotManager.createSnapshot(name, desc)
            result.onSuccess {
                showMessage("Snapshot '${it.name}' created.")
            }.onFailure {
                showMessage("Snapshot creation failed: ${it.message}")
            }
        }
    }

    fun restoreSnapshot(id: String) {
        viewModelScope.launch {
            showMessage("Restoring virtual snapshot...")
            val result = vManager.snapshotManager.restoreSnapshot(id)
            result.onSuccess {
                showMessage("Virtual environment state restored successfully.")
                refreshVirtualFiles()
            }.onFailure {
                showMessage("Restore failed: ${it.message}")
            }
        }
    }

    fun deleteSnapshot(id: String) {
        viewModelScope.launch {
            val result = vManager.snapshotManager.deleteSnapshot(id)
            result.onSuccess {
                showMessage("Snapshot deleted.")
            }.onFailure {
                showMessage("Failed to delete snapshot.")
            }
        }
    }

    // Network
    fun toggleNetworkInternet(enabled: Boolean) {
        vManager.networkManager.toggleInternetAccess(enabled)
        showMessage(if (enabled) "Virtual internet access enabled" else "Virtual internet access disabled")
    }

    fun updateDns(pri: String, sec: String) {
        vManager.networkManager.updateDns(pri, sec)
        showMessage("DNS servers updated: $pri, $sec")
    }

    fun updateNetworkMode(mode: String) {
        vManager.networkManager.updateNetworkMode(mode)
        showMessage("Virtual network mode set to $mode")
    }

    fun clearNetworkLogs() {
        vManager.networkManager.clearLogs()
        showMessage("Virtual network logs cleared.")
    }

    // Hardware Settings
    fun updateHardwareConfig(config: HardwareConfig) {
        vManager.instance.updateHardwareConfig(config)
        showMessage("Hardware configuration updated.")
    }

    fun dismissSetupWizard() {
        _showSetupWizard.value = false
    }

    fun openSetupWizard() {
        _showSetupWizard.value = true
    }
}
