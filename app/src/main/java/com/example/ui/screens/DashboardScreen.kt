package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.virtualization.InstanceStatus
import com.example.ui.components.HardwareMetricCard
import com.example.ui.components.SecurityNoticeBanner
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.CyberRed
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.RootBoxViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: RootBoxViewModel,
    modifier: Modifier = Modifier
) {
    val status by viewModel.instanceStatus.collectAsState()
    val hardwareConfig by viewModel.hardwareConfig.collectAsState()
    val cpuUsage by viewModel.cpuUsage.collectAsState()
    val ramUsageMb by viewModel.ramUsageMb.collectAsState()
    val storageBytes by viewModel.storageUsedBytes.collectAsState()
    val uptime by viewModel.uptimeStr.collectAsState()
    val networkStats by viewModel.networkStats.collectAsState()
    val snapshots by viewModel.snapshots.collectAsState()
    val apps by viewModel.installedApps.collectAsState()

    val ramMax = hardwareConfig.allocatedRamMb
    val ramProgress = (ramUsageMb / ramMax.toFloat()).coerceIn(0f, 1f)
    val storageGb = storageBytes / (1024.0 * 1024.0 * 1024.0)
    val storageProgress = (storageGb / hardwareConfig.virtualStorageGb.toDouble()).toFloat().coerceIn(0f, 1f)

    LazyColumn(
        modifier = modifier
            .testTag("dashboard_screen")
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Title Banner
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(CyberEmerald)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ROOTBOX",
                                    color = TextPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                            }
                            Text(
                                text = "Virtual Android Environment (vAOSP 14)",
                                color = CyberCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        StatusBadge(status = status)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Uptime: $uptime",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Arch: ARM64-v8a",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Root: UID 0",
                            color = CyberEmerald,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Security Notice Banner
        item {
            SecurityNoticeBanner()
        }

        // Metric Gauges (2x2 Grid)
        item {
            Text(
                text = "RESOURCE MONITOR",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HardwareMetricCard(
                    title = "RAM",
                    value = String.format(Locale.US, "%.1f GB / %d GB", ramUsageMb / 1024.0, ramMax / 1024),
                    subValue = "${(ramProgress * 100).toInt()}% Allocated",
                    icon = Icons.Default.Memory,
                    accentColor = CyberEmerald,
                    progress = ramProgress,
                    modifier = Modifier.weight(1f)
                )

                HardwareMetricCard(
                    title = "Storage",
                    value = String.format(Locale.US, "%.1f GB / %d GB", storageGb, hardwareConfig.virtualStorageGb),
                    subValue = "${(storageProgress * 100).toInt()}% Used",
                    icon = Icons.Default.Storage,
                    accentColor = CyberBlue,
                    progress = storageProgress,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HardwareMetricCard(
                    title = "CPU",
                    value = String.format(Locale.US, "%.1f%%", cpuUsage),
                    subValue = "${hardwareConfig.cpuCores} Virtual Cores",
                    icon = Icons.Default.Speed,
                    accentColor = CyberPurple,
                    progress = (cpuUsage / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.weight(1f)
                )

                HardwareMetricCard(
                    title = "Network",
                    value = if (networkStats.isConnected) "Connected" else "Isolated",
                    subValue = "vnet0 (${hardwareConfig.networkMode.take(8)})",
                    icon = Icons.Default.Wifi,
                    accentColor = if (networkStats.isConnected) CyberCyan else CyberAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Instance Controls
        item {
            Text(
                text = "INSTANCE CONTROLS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (status == InstanceStatus.STOPPED) {
                    Button(
                        onClick = { viewModel.startInstance() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("launch_instance_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Instance", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            if (status == InstanceStatus.RUNNING) viewModel.pauseInstance() else viewModel.resumeInstance()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (status == InstanceStatus.RUNNING) CyberAmber else CyberEmerald
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pause_resume_button")
                    ) {
                        Icon(
                            if (status == InstanceStatus.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (status == InstanceStatus.RUNNING) "Pause" else "Resume", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.restartInstance() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("restart_instance_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restart")
                    }

                    OutlinedButton(
                        onClick = { viewModel.stopInstance() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRed),
                        modifier = Modifier.testTag("stop_instance_button")
                    ) {
                        Text("Stop", color = CyberRed)
                    }
                }
            }
        }

        // Quick Navigation Grid
        item {
            Text(
                text = "VIRTUAL SUBSYSTEMS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickNavButton(
                        title = "Terminal",
                        subtitle = "Root Shell (UID 0)",
                        icon = Icons.Default.Terminal,
                        color = CyberEmerald,
                        onClick = { viewModel.setTab(NavigationTab.TERMINAL) },
                        modifier = Modifier.weight(1f)
                    )

                    QuickNavButton(
                        title = "Files",
                        subtitle = "/system, /data, /sdcard",
                        icon = Icons.Default.Folder,
                        color = CyberBlue,
                        onClick = { viewModel.setTab(NavigationTab.FILES) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickNavButton(
                        title = "Apps (${apps.size})",
                        subtitle = "Install APKs & Tools",
                        icon = Icons.Default.Android,
                        color = CyberPurple,
                        onClick = { viewModel.setTab(NavigationTab.APPS) },
                        modifier = Modifier.weight(1f)
                    )

                    QuickNavButton(
                        title = "Settings",
                        subtitle = "RAM, CPU & Display",
                        icon = Icons.Default.Settings,
                        color = CyberCyan,
                        onClick = { viewModel.setTab(NavigationTab.SETTINGS) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Snapshots Preview Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Snapshots (${snapshots.size})",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Manage All",
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { viewModel.setTab(NavigationTab.SNAPSHOTS) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    snapshots.take(2).forEach { snap ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(CyberEmerald)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = snap.name,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            val dateStr = SimpleDateFormat("MMM dd", Locale.US).format(Date(snap.timestamp))
                            Text(
                                text = "$dateStr • ${snap.appsCount} apps",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickNavButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag("nav_btn_${title.lowercase()}")
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(text = subtitle, color = TextMuted, fontSize = 10.sp)
            }
        }
    }
}
