package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.VirtualAppEntity
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VirtualAppDetailDialog(
    app: VirtualAppEntity,
    onDismiss: () -> Unit,
    onLaunch: (VirtualAppEntity) -> Unit,
    onStop: (VirtualAppEntity) -> Unit,
    onForceStop: (VirtualAppEntity) -> Unit,
    onClearData: (String) -> Unit,
    onClearCache: (String) -> Unit,
    onExportApk: (String, Uri) -> Unit,
    onExportData: (String, Uri) -> Unit,
    onUninstall: (String) -> Unit
) {
    // Export APK file picker
    val exportApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri: Uri? ->
        if (uri != null) {
            onExportApk(app.packageName, uri)
        }
    }

    // Export App Data file picker
    val exportDataLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            onExportData(app.packageName, uri)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .testTag("virtual_app_detail_dialog")
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(CyberEmerald.copy(alpha = 0.15f))
                            .border(1.dp, CyberEmerald.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = app.appName,
                            tint = CyberEmerald,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.appName,
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = app.packageName,
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (app.isRunning) CyberEmerald.copy(alpha = 0.2f) else DarkBorder)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (app.isRunning) "RUNNING" else "STOPPED",
                                    color = if (app.isRunning) CyberEmerald else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (app.rootAccessGranted) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CyberAmber.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "VIRTUAL ROOT",
                                        color = CyberAmber,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkBorder)
                Spacer(modifier = Modifier.height(16.dp))

                // Detail Specs
                DetailRow("Version", "${app.versionName} (Build ${app.versionCode})")
                DetailRow("Target SDK", "Android ${app.targetSdk} (API ${app.targetSdk})")
                DetailRow("Minimum SDK", "Android ${app.minSdk} (API ${app.minSdk})")
                DetailRow("Architecture", app.architecture)
                DetailRow("Sandbox UID", "u0_a${kotlin.math.abs(app.packageName.hashCode() % 500) + 10}")
                val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date(app.installDate))
                DetailRow("Installed Date", dateStr)

                Spacer(modifier = Modifier.height(14.dp))

                // Storage Breakdown Card
                Text("STORAGE BREAKDOWN", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkBackground)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        StorageRow("APK Package Size", app.sizeBytes)
                        StorageRow("Virtual App Data (/data/data)", app.dataSizeBytes)
                        StorageRow("Virtual Cache", app.cacheSizeBytes)
                        HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))
                        StorageRow("Total Storage Used", app.totalSizeBytes, isBold = true)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Declared Permissions
                Text("VIRTUAL PERMISSIONS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBackground)
                        .padding(10.dp)
                ) {
                    val permsList = app.permissions.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (permsList.isEmpty()) {
                        Text("No dangerous permissions requested", color = TextMuted, fontSize = 11.sp)
                    } else {
                        Column {
                            permsList.forEach { p ->
                                Text(
                                    text = "• $p",
                                    color = if (p.contains("SUPERUSER")) CyberAmber else TextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions Grid
                Text("MANAGEMENT ACTIONS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onClearCache(app.packageName) },
                        modifier = Modifier.weight(1f).testTag("clear_cache_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                    ) {
                        Text("Clear Cache", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { onClearData(app.packageName) },
                        modifier = Modifier.weight(1f).testTag("clear_data_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAmber)
                    ) {
                        Text("Clear Data", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportApkLauncher.launch("${app.packageName}_base.apk") },
                        modifier = Modifier.weight(1f).testTag("export_apk_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export APK", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { exportDataLauncher.launch("${app.packageName}_data_backup.txt") },
                        modifier = Modifier.weight(1f).testTag("export_data_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export Data", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Launch / Stop / Force Stop / Uninstall buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!app.isRunning) {
                        Button(
                            onClick = {
                                onLaunch(app)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                            modifier = Modifier.weight(1f).testTag("detail_launch_app_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Launch", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = { onStop(app) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberAmber),
                            modifier = Modifier.weight(1f).testTag("detail_stop_app_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { onForceStop(app) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
                            modifier = Modifier.weight(1f).testTag("detail_force_stop_btn")
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Force Stop", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    if (!app.isSystemApp) {
                        OutlinedButton(
                            onClick = {
                                onUninstall(app.packageName)
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRed),
                            modifier = Modifier.testTag("detail_uninstall_app_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Uninstall", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Close", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StorageRow(label: String, bytes: Long, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = if (isBold) TextPrimary else TextSecondary, fontSize = 11.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        val mb = bytes / (1024.0 * 1024.0)
        val formatted = if (bytes < 1024 * 1024) String.format(Locale.US, "%.1f KB", bytes / 1024.0) else String.format(Locale.US, "%.2f MB", mb)
        Text(text = formatted, color = if (isBold) CyberEmerald else TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
    }
}
