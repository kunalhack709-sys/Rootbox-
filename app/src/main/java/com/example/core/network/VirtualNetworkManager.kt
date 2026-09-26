package com.example.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.NetworkLogDao
import com.example.data.local.entities.NetworkLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class NetworkStats(
    val isConnected: Boolean = true,
    val rxBytes: Long = 1420580L,
    val txBytes: Long = 854120L,
    val rxPackets: Long = 1840L,
    val txPackets: Long = 1120L,
    val linkSpeedMbps: Int = 1000,
    val pingMs: Int = 14,
    val lastSyncTime: String = "Just now"
) {
    val formattedRx: String
        get() = formatBytes(rxBytes)

    val formattedTx: String
        get() = formatBytes(txBytes)

    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.2f MB", mb)
    }
}

class VirtualNetworkManager(
    private val context: Context,
    private val logDao: NetworkLogDao,
    private val scope: CoroutineScope
) {
    private val _config = MutableStateFlow(NetworkConfig())
    val config: StateFlow<NetworkConfig> = _config.asStateFlow()

    private val _stats = MutableStateFlow(NetworkStats())
    val stats: StateFlow<NetworkStats> = _stats.asStateFlow()

    init {
        checkHostConnectivity()
    }

    fun checkHostConnectivity(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val currentConfig = _config.value
        val isVirtuallyConnected = currentConfig.isInternetEnabled && (currentConfig.networkMode != "Isolated (Localhost Only)") && hasInternet

        _stats.value = _stats.value.copy(
            isConnected = isVirtuallyConnected,
            lastSyncTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        )
        return isVirtuallyConnected
    }

    fun toggleInternetAccess(enabled: Boolean) {
        _config.value = _config.value.copy(isInternetEnabled = enabled)
        checkHostConnectivity()
        logPacket(
            protocol = "CONTROL",
            dest = "vnet0",
            status = if (enabled) "ENABLED" else "BLOCKED",
            bytes = 0L
        )
    }

    fun updateDns(primary: String, secondary: String) {
        _config.value = _config.value.copy(primaryDns = primary, secondaryDns = secondary)
        logPacket(
            protocol = "DNS",
            dest = primary,
            status = "CONFIGURED",
            bytes = 64L
        )
    }

    fun updateNetworkMode(mode: String) {
        _config.value = _config.value.copy(networkMode = mode)
        checkHostConnectivity()
    }

    fun togglePacketLogging(enabled: Boolean) {
        _config.value = _config.value.copy(isPacketLoggingEnabled = enabled)
    }

    fun logPacket(protocol: String, dest: String, status: String, bytes: Long) {
        if (!_config.value.isPacketLoggingEnabled) return
        scope.launch(Dispatchers.IO) {
            val log = NetworkLogEntity(
                protocol = protocol,
                source = "${_config.value.virtualIp}:${Random.nextInt(40000, 65000)}",
                destination = dest,
                bytesTransferred = bytes,
                status = status
            )
            logDao.insertLog(log)

            // Update stats
            val current = _stats.value
            _stats.value = current.copy(
                rxBytes = current.rxBytes + (bytes * 0.7).toLong(),
                txBytes = current.txBytes + (bytes * 0.3).toLong(),
                rxPackets = current.rxPackets + 1,
                txPackets = current.txPackets + 1
            )
        }
    }

    fun clearLogs() {
        scope.launch(Dispatchers.IO) {
            logDao.clearLogs()
        }
    }
}
