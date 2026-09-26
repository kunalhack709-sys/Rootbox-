package com.example.core.virtualization

import android.content.Context
import android.content.Intent
import com.example.core.filesystem.VirtualFilesystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class VirtualAndroidInstance(
    private val context: Context,
    private val fs: VirtualFilesystem,
    private val scope: CoroutineScope
) {
    val instanceId: String = "rootbox-vaosp-core"

    private val _status = MutableStateFlow(InstanceStatus.RUNNING)
    val status: StateFlow<InstanceStatus> = _status.asStateFlow()

    private val _hardwareConfig = MutableStateFlow(HardwareConfig())
    val hardwareConfig: StateFlow<HardwareConfig> = _hardwareConfig.asStateFlow()

    private val _cpuUsage = MutableStateFlow(4.2f)
    val cpuUsage: StateFlow<Float> = _cpuUsage.asStateFlow()

    private val _ramUsageMb = MutableStateFlow(1240f)
    val ramUsageMb: StateFlow<Float> = _ramUsageMb.asStateFlow()

    private val _storageUsedBytes = MutableStateFlow(8420000000L) // 8.42 GB virtual storage
    val storageUsedBytes: StateFlow<Long> = _storageUsedBytes.asStateFlow()

    private val _uptimeSeconds = MutableStateFlow(18420L)
    val uptimeSeconds: StateFlow<Long> = _uptimeSeconds.asStateFlow()

    private var monitorJob: Job? = null

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(2000L)
                if (_status.value == InstanceStatus.RUNNING) {
                    _uptimeSeconds.value += 2
                    // Fluctuate CPU naturally
                    val baseCpu = 3.5f
                    val delta = (Random.nextFloat() * 4.0f) - 1.5f
                    _cpuUsage.value = (baseCpu + delta).coerceIn(1.2f, 28.5f)

                    // RAM usage
                    val baseRam = 1200f
                    val ramDelta = Random.nextFloat() * 60f - 20f
                    _ramUsageMb.value = (baseRam + ramDelta).coerceIn(1024f, _hardwareConfig.value.allocatedRamMb.toFloat() - 100f)

                    // Virtual storage calculation (actual fs size + base system image allocation)
                    val realSize = fs.calculateTotalStorageUsed()
                    val baseImageSize = 8200000000L // 8.2 GB virtual AOSP image size
                    _storageUsedBytes.value = baseImageSize + realSize
                } else if (_status.value == InstanceStatus.PAUSED) {
                    _cpuUsage.value = 0.4f
                } else if (_status.value == InstanceStatus.STOPPED) {
                    _cpuUsage.value = 0.0f
                    _ramUsageMb.value = 0.0f
                }
            }
        }
    }

    fun start() {
        scope.launch(Dispatchers.Default) {
            _status.value = InstanceStatus.STARTING
            delay(1200L)
            _status.value = InstanceStatus.RUNNING
            try {
                val serviceIntent = Intent(context, VirtualInstanceService::class.java).apply {
                    action = VirtualInstanceService.ACTION_START
                }
                context.startService(serviceIntent)
            } catch (e: Exception) {
                // Ignore service start exception
            }
        }
    }

    fun stop() {
        _status.value = InstanceStatus.STOPPED
        try {
            val serviceIntent = Intent(context, VirtualInstanceService::class.java).apply {
                action = VirtualInstanceService.ACTION_STOP
            }
            context.startService(serviceIntent)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun pause() {
        if (_status.value == InstanceStatus.RUNNING) {
            _status.value = InstanceStatus.PAUSED
        }
    }

    fun resume() {
        if (_status.value == InstanceStatus.PAUSED) {
            _status.value = InstanceStatus.RUNNING
        }
    }

    fun restart() {
        scope.launch(Dispatchers.Default) {
            _status.value = InstanceStatus.STOPPED
            delay(800L)
            start()
        }
    }

    fun updateHardwareConfig(config: HardwareConfig) {
        _hardwareConfig.value = config
    }

    fun formatUptime(): String {
        val totalSec = _uptimeSeconds.value
        val hours = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        val secs = totalSec % 60
        return String.format("%02d:%02d:%02d", hours, mins, secs)
    }
}
