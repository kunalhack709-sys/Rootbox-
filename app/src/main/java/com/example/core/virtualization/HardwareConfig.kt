package com.example.core.virtualization

data class HardwareConfig(
    val allocatedRamMb: Int = 3072, // 3 GB
    val cpuCores: Int = 4,
    val virtualStorageGb: Int = 32,
    val resolutionWidth: Int = 1080,
    val resolutionHeight: Int = 2400,
    val dpi: Int = 420,
    val networkMode: String = "NAT (Bridged)", // NAT, Isolated, Host-Only
    val systemImage: String = "Android 14 (vAOSP 64-bit Minimal)",
    val allowSharedStorage: Boolean = true,
    val virtualRootMode: String = "Root Always (UID 0)" // Root Always, Interactive, Restricted
)
