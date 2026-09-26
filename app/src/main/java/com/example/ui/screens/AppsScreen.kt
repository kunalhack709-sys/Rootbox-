package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.VirtualAppEntity
import com.example.ui.components.VirtualAppDetailDialog
import com.example.ui.components.VirtualAppRunnerDialog
import com.example.ui.theme.CyberAmber
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
import java.util.Locale

@Composable
fun AppsScreen(
    viewModel: com.example.ui.viewmodel.RootBoxViewModel,
    modifier: Modifier = Modifier
) {
    val apps by viewModel.installedApps.collectAsState()
    val selectedAppDetail by viewModel.selectedAppForDetail.collectAsState()
    val runningAppSim by viewModel.runningAppSim.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var exportTargetPackage by remember { mutableStateOf<String?>(null) }
    var exportMode by remember { mutableStateOf("APK") } // "APK" or "DATA"

    // Single export launcher
    val exportApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri: Uri? ->
        val pkg = exportTargetPackage
        if (uri != null && pkg != null) {
            viewModel.exportApk(pkg, uri)
        }
        exportTargetPackage = null
    }

    val exportDataLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        val pkg = exportTargetPackage
        if (uri != null && pkg != null) {
            viewModel.exportAppData(pkg, uri)
        }
        exportTargetPackage = null
    }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) {
            apps
        } else {
            apps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
            }
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

        // Top Header Bar matching requested: [ RootBox Apps    [+] ]
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RootBox Apps",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${apps.size} virtual applications installed",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = { viewModel.openAddAppSheet() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CyberEmerald)
                        .testTag("apps_add_app_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add App",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search installed packages...", color = TextMuted, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_search_field"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = DarkBorder
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // App List
        if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Android, contentDescription = null, tint = DarkBorder, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No applications found", color = TextMuted, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.openAddAppSheet() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald)
                    ) {
                        Text("+ Add Application", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps) { app ->
                    AppItemCard(
                        app = app,
                        onLaunch = { viewModel.launchVirtualApp(app) },
                        onStop = { viewModel.stopVirtualApp(app) },
                        onForceStop = { viewModel.forceStopApp(app) },
                        onAppInfo = { viewModel.selectAppDetail(app) },
                        onClearData = { viewModel.clearAppData(app.packageName) },
                        onClearCache = { viewModel.clearAppCache(app.packageName) },
                        onExportApk = {
                            exportTargetPackage = app.packageName
                            exportMode = "APK"
                            exportApkLauncher.launch("${app.packageName}_base.apk")
                        },
                        onExportData = {
                            exportTargetPackage = app.packageName
                            exportMode = "DATA"
                            exportDataLauncher.launch("${app.packageName}_data.txt")
                        },
                        onUninstall = { viewModel.uninstallApp(app.packageName) }
                    )
                }
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
            onForceStop = { viewModel.forceStopApp(it) },
            onClearData = { viewModel.clearAppData(it) },
            onClearCache = { viewModel.clearAppCache(it) },
            onExportApk = { pkg, uri -> viewModel.exportApk(pkg, uri) },
            onExportData = { pkg, uri -> viewModel.exportAppData(pkg, uri) },
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
    onLaunch: () -> Unit,
    onStop: () -> Unit,
    onForceStop: () -> Unit,
    onAppInfo: () -> Unit,
    onClearData: () -> Unit,
    onClearCache: () -> Unit,
    onExportApk: () -> Unit,
    onExportData: () -> Unit,
    onUninstall: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onAppInfo)
            .border(
                1.dp,
                if (app.isRunning) CyberEmerald.copy(alpha = 0.6f) else DarkBorder,
                RoundedCornerShape(14.dp)
            )
            .testTag("app_row_${app.packageName}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            val parsedBitmap = remember(app.iconBase64) {
                app.iconBase64?.let { base64 ->
                    try {
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (app.isSystemApp) CyberCyan.copy(alpha = 0.15f) else CyberPurple.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (parsedBitmap != null) {
                    Image(
                        bitmap = parsedBitmap,
                        contentDescription = app.appName,
                        modifier = Modifier.size(30.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = app.appName,
                        tint = if (app.isSystemApp) CyberCyan else CyberPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // App Name & Package
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
                    text = app.packageName,
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(modifier = Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val mb = app.totalSizeBytes / (1024.0 * 1024.0)
                    Text(
                        text = String.format(Locale.US, "%.1f MB", mb),
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    if (app.rootAccessGranted) {
                        Text(
                            text = "ROOT",
                            color = CyberAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick [Launch] button
            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (app.isRunning) CyberAmber else CyberEmerald
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("launch_btn_${app.packageName}")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (app.isRunning) "Running" else "Launch",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Action Menu [⋮]
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("menu_btn_${app.packageName}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Actions",
                        tint = TextMuted
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Launch in RootBox", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyberEmerald) },
                        onClick = {
                            menuExpanded = false
                            onLaunch()
                        }
                    )

                    if (app.isRunning) {
                        DropdownMenuItem(
                            text = { Text("Stop App", color = CyberAmber) },
                            leadingIcon = { Icon(Icons.Default.Stop, contentDescription = null, tint = CyberAmber) },
                            onClick = {
                                menuExpanded = false
                                onStop()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Force Stop (SIGKILL)", color = CyberRed) },
                            leadingIcon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = CyberRed) },
                            onClick = {
                                menuExpanded = false
                                onForceStop()
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("App Information", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = CyberCyan) },
                        onClick = {
                            menuExpanded = false
                            onAppInfo()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Clear App Data", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = CyberAmber) },
                        onClick = {
                            menuExpanded = false
                            onClearData()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Clear Cache", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = CyberCyan) },
                        onClick = {
                            menuExpanded = false
                            onClearCache()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Export APK", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = CyberEmerald) },
                        onClick = {
                            menuExpanded = false
                            onExportApk()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Export App Data", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = CyberCyan) },
                        onClick = {
                            menuExpanded = false
                            onExportData()
                        }
                    )

                    if (!app.isSystemApp) {
                        DropdownMenuItem(
                            text = { Text("Uninstall", color = CyberRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = CyberRed) },
                            onClick = {
                                menuExpanded = false
                                onUninstall()
                            }
                        )
                    }
                }
            }
        }
    }
}
