package com.example.core.network

data class NetworkConfig(
    val interfaceName: String = "vnet0",
    val virtualIp: String = "192.168.100.2",
    val subnetMask: String = "255.255.255.0",
    val gatewayIp: String = "192.168.100.1",
    val primaryDns: String = "8.8.8.8",
    val secondaryDns: String = "1.1.1.1",
    val isInternetEnabled: Boolean = true,
    val networkMode: String = "NAT (Host Bridged)", // "NAT (Host Bridged)", "Isolated (Localhost Only)", "Host-Only Network"
    val isPacketLoggingEnabled: Boolean = true
)
