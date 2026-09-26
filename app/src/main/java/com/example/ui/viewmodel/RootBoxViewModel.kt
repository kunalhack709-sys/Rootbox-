package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.apk.ApkValidationResult
import com.example.core.apk.InstallStepState
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

    // App Management State
    private val _selectedAppForDetail = MutableStateFlow<VirtualAppEntity?>(null)
    val selectedAppForDetail: StateFlow<VirtualAppEntity?> = _selectedAppForDetail.asStateFlow()

    private val _runningAppSim = MutableStateFlow<VirtualAppEntity?>(null)
    val runningAppSim: StateFlow<VirtualAppEntity?> = _runningAppSim.asStateFlow()

    // New Add App & Install Dialog State
    private val _showAddAppSheet = MutableStateFlow(false)
    val showAddAppSheet: StateFlow<Boolean> = _showAddAppSheet.asStateFlow()

    private val _installProgressState = MutableStateFlow<InstallStepState?>(null)
    val installProgressState: StateFlow<InstallStepState?> = _installProgressState.asStateFlow()

    private val _validationFailure = MutableStateFlow<ApkValidationResult?>(null)
    val validationFailure: StateFlow<ApkValidationResult?> = _validationFailure.asStateFlow()

    private val _rootRequestPrompt = MutableStateFlow<VirtualAppEntity?>(null)
    val rootRequestPrompt: StateFlow<VirtualAppEntity?> = _rootRequestPrompt.asStateFlow()

    // First Run State
    private val _isEnvironmentInitialized = MutableStateFlow(true)
    val isEnvironmentInitialized: StateFlow<Boolean> = _isEnvironmentInitialized.asStateFlow()

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

    fun openAddAppSheet() {
        _showAddAppSheet.value = true
    }

    fun closeAddAppSheet() {
        _showAddAppSheet.value = false
    }

    fun dismissInstallProgress() {
        _installProgressState.value = null
    }

    fun dismissValidationFailure() {
        _validationFailure.value = null
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

    // Apps & APK Installation Flow
    fun installApkWithValidation(uri: Uri) {
        closeAddAppSheet()
        viewModelScope.launch {
            _installProgressState.value = InstallStepState(
                progressPercent = 5,
                currentStatus = "Validating APK package..."
            )

            // Step 1: Validate
            val validation = vManager.apkManager.validateApk(uri)
            if (!validation.isValid) {
                _installProgressState.value = null
                _validationFailure.value = validation
                return@launch
            }

            // Step 2: Install with step progress
            vManager.apkManager.installApkFromUriWithProgress(uri) { state ->
                _installProgressState.value = state
            }
        }
    }

    fun installMultipleApks(uris: List<Uri>) {
        closeAddAppSheet()
        viewModelScope.launch {
            if (uris.isEmpty()) return@launch
            _installProgressState.value = InstallStepState(
                totalApks = uris.size,
                currentStatus = "Starting batch installation of ${uris.size} APKs..."
            )
            vManager.apkManager.installMultipleApks(uris) { state ->
                _installProgressState.value = state
            }
        }
    }

    fun installSampleApp(sampleKey: String) {
        closeAddAppSheet()
        viewModelScope.launch {
            _installProgressState.value = InstallStepState(
                progressPercent = 10,
                currentStatus = "Preparing sample package..."
            )
            vManager.apkManager.installSampleApp(sampleKey) { state ->
                _installProgressState.value = state
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
        // If app requests superuser and has not yet been granted/denied, prompt user
        if (app.permissions.contains("SUPERUSER") && !app.rootAccessGranted) {
            _rootRequestPrompt.value = app
        } else {
            proceedLaunchApp(app)
        }
    }

    fun allowRootAccess(packageName: String) {
        viewModelScope.launch {
            vManager.apkManager.setRootAccess(packageName, true)
            val app = _rootRequestPrompt.value
            _rootRequestPrompt.value = null
            if (app != null && app.packageName == packageName) {
                proceedLaunchApp(app.copy(rootAccessGranted = true))
            }
            showMessage("Virtual root granted to $packageName inside RootBox container.")
        }
    }

    fun denyRootAccess(packageName: String) {
        viewModelScope.launch {
            vManager.apkManager.setRootAccess(packageName, false)
            val app = _rootRequestPrompt.value
            _rootRequestPrompt.value = null
            if (app != null && app.packageName == packageName) {
                proceedLaunchApp(app.copy(rootAccessGranted = false))
            }
            showMessage("Virtual root denied for $packageName.")
        }
    }

    private fun proceedLaunchApp(app: VirtualAppEntity) {
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
            if (_selectedAppForDetail.value?.packageName == app.packageName) {
                _selectedAppForDetail.value = _selectedAppForDetail.value?.copy(isRunning = false)
            }
        }
    }

    fun forceStopApp(app: VirtualAppEntity) {
        viewModelScope.launch {
            vManager.apkManager.stopApp(app.packageName)
            if (_runningAppSim.value?.packageName == app.packageName) {
                _runningAppSim.value = null
            }
            showMessage("Force stopped ${app.appName} (SIGKILL).")
            if (_selectedAppForDetail.value?.packageName == app.packageName) {
                _selectedAppForDetail.value = _selectedAppForDetail.value?.copy(isRunning = false)
            }
        }
    }

    fun clearAppData(packageName: String) {
        viewModelScope.launch {
            val ok = vManager.apkManager.clearAppData(packageName)
            if (ok) {
                showMessage("Cleared app data for $packageName.")
                if (_selectedAppForDetail.value?.packageName == packageName) {
                    _selectedAppForDetail.value = _selectedAppForDetail.value?.copy(dataSizeBytes = 0, cacheSizeBytes = 0)
                }
            } else {
                showMessage("Failed to clear app data.")
            }
        }
    }

    fun clearAppCache(packageName: String) {
        viewModelScope.launch {
            val ok = vManager.apkManager.clearAppCache(packageName)
            if (ok) {
                showMessage("Cleared cache for $packageName.")
                if (_selectedAppForDetail.value?.packageName == packageName) {
                    _selectedAppForDetail.value = _selectedAppForDetail.value?.copy(cacheSizeBytes = 0)
                }
            } else {
                showMessage("Failed to clear cache.")
            }
        }
    }

    fun exportApk(packageName: String, destUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.openOutputStream(destUri)?.use { stream ->
                    val ok = vManager.apkManager.exportApk(packageName, stream)
                    if (ok) {
                        showMessage("Exported $packageName APK successfully.")
                    } else {
                        showMessage("Export failed: APK not found in virtual storage.")
                    }
                }
            } catch (e: Exception) {
                showMessage("Error exporting APK: ${e.localizedMessage}")
            }
        }
    }

    fun exportAppData(packageName: String, destUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.openOutputStream(destUri)?.use { stream ->
                    val ok = vManager.apkManager.exportAppData(packageName, stream)
                    if (ok) {
                        showMessage("Exported app data for $packageName.")
                    } else {
                        showMessage("Export failed.")
                    }
                }
            } catch (e: Exception) {
                showMessage("Error exporting app data: ${e.localizedMessage}")
            }
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

    fun createEnvironment(ramMb: Int, storageGb: Int, systemImage: String) {
        val current = hardwareConfig.value
        updateHardwareConfig(
            current.copy(
                allocatedRamMb = ramMb,
                virtualStorageGb = storageGb,
                systemImage = systemImage
            )
        )
        _isEnvironmentInitialized.value = true
        _showSetupWizard.value = false
        startInstance()
        showMessage("Virtual Android environment created and ready!")
    }

    fun dismissSetupWizard() {
        _showSetupWizard.value = false
    }

    fun openSetupWizard() {
        _showSetupWizard.value = true
    }
}
