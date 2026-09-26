package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.VirtualAppEntity
import com.example.ui.components.VirtualAppDetailDialog
import com.example.ui.components.VirtualAppRunnerDialog
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.CyberRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.RootBoxViewModel
import java.util.Locale

@Composable
fun AppsScreen(
    viewModel: RootBoxViewModel,
    modifier: Modifier = Modifier
) {
    val apps by viewModel.installedApps.collectAsState()
    val selectedAppDetail by viewModel.selectedAppForDetail.collectAsState()
    val runningAppSim by viewModel.runningAppSim.collectAsState()

    // File picker for APK installation
    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.installApk(uri)
        }
    }

    Column(
        modifier = modifier
            .testTag("apps_screen")
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Header with Install APK action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "VIRTUAL APPLICATIONS",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${apps.size} packages installed in /data/app",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = { apkPickerLauncher.launch("application/vnd.android.package-archive") },
                colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                modifier = Modifier.testTag("install_apk_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Install APK", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // App List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(apps) { app ->
                AppItemCard(
                    app = app,
                    onClick = { viewModel.selectAppDetail(app) },
                    onLaunch = { viewModel.launchVirtualApp(app) },
                    onStop = { viewModel.stopVirtualApp(app) }
                )
            }
        }
    }

    // App Detail Dialog
    selectedAppDetail?.let { app ->
        VirtualAppDetailDialog(
            app = app,
            onDismiss = { viewModel.selectAppDetail(null) },
            onLaunch = { viewModel.launchVirtualApp(it) },
            onStop = { viewModel.stopVirtualApp(it) },
            onUninstall = { viewModel.uninstallApp(it) }
        )
    }

    // Live App Simulator Window
    runningAppSim?.let { app ->
        VirtualAppRunnerDialog(
            app = app,
            onClose = { viewModel.closeRunningAppSim() },
            onStop = { viewModel.stopVirtualApp(app) }
        )
    }
}

@Composable
private fun AppItemCard(
    app: VirtualAppEntity,
    onClick: () -> Unit,
    onLaunch: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (app.isRunning) CyberEmerald.copy(alpha = 0.6f) else DarkBorder,
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (app.isSystemApp) CyberCyan.copy(alpha = 0.15f) else CyberPurple.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = app.appName,
                    tint = if (app.isSystemApp) CyberCyan else CyberPurple,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (app.isRunning) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CyberEmerald)
                        )
                    }
                }

                Text(
                    text = "${app.packageName} • v${app.versionName}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(modifier = Modifier.padding(top = 2.dp)) {
                    val mb = app.sizeBytes / (1024.0 * 1024.0)
                    Text(
                        text = String.format(Locale.US, "%.1f MB", mb),
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (app.permissions.contains("SUPERUSER")) {
                        Text(
                            text = "ROOT ACCESS",
                            color = CyberEmerald,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!app.isRunning) {
                    IconButton(
                        onClick = onLaunch,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberEmerald.copy(alpha = 0.15f))
                            .testTag("launch_item_${app.packageName}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Launch", tint = CyberEmerald, modifier = Modifier.size(20.dp))
                    }
                } else {
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberRed.copy(alpha = 0.15f))
                            .testTag("stop_item_${app.packageName}")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = CyberRed, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(onClick = onClick) {
                    Icon(Icons.Default.Info, contentDescription = "Details", tint = TextMuted)
                }
            }
        }
    }
}
