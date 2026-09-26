package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.window.DialogProperties
import com.example.core.apk.InstallStepState
import com.example.data.local.entities.VirtualAppEntity
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ApkInstallProgressDialog(
    state: InstallStepState,
    onLaunch: (VirtualAppEntity) -> Unit,
    onAppInfo: (VirtualAppEntity) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (state.isComplete || state.errorMessage != null) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = state.isComplete || state.errorMessage != null,
            dismissOnClickOutside = state.isComplete || state.errorMessage != null
        )
    ) {
        Card(
            modifier = Modifier
                .testTag("apk_install_progress_dialog")
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(
                    1.dp,
                    if (state.isComplete) CyberEmerald.copy(alpha = 0.5f) else DarkBorder,
                    RoundedCornerShape(20.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth()
            ) {
                if (state.isComplete && state.completedApp != null) {
                    // Success View
                    val app = state.completedApp
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CyberEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("✓ Installation complete", color = CyberEmerald, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Installed safely inside RootBox virtual environment", color = TextMuted, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBackground)
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(app.appName, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(app.packageName, color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Version: ${app.versionName} • Location: /data/app/${app.packageName}", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onDismiss()
                                onLaunch(app)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                            modifier = Modifier.weight(1f).testTag("install_done_launch_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onAppInfo(app)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                            modifier = Modifier.weight(1f).testTag("install_done_info_btn")
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("App Info")
                        }
                    }
                } else if (state.errorMessage != null) {
                    // Error View
                    Text("Installation failed", color = CyberRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.errorMessage, color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("OK", color = Color.White)
                    }
                } else {
                    // Installing Progress View
                    val isBatch = state.totalApks > 1
                    val title = if (isBatch) "Installing ${state.totalApks} applications" else "Installing Application..."

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            if (state.currentAppName.isNotBlank()) {
                                Text("App: ${state.currentAppName}", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Package: ${state.currentPackage}", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = CyberEmerald,
                            strokeWidth = 2.5.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(state.currentStatus, color = TextSecondary, fontSize = 12.sp)
                            Text("${state.progressPercent}%", color = CyberEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { (state.progressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = CyberEmerald,
                            trackColor = DarkBorder
                        )
                    }

                    // If multiple APKs, list them with icons
                    if (isBatch && state.batchList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkBackground)
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                state.batchList.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item.name, color = TextPrimary, fontSize = 12.sp)
                                        when (item.status) {
                                            "SUCCESS" -> Text("✓ Installed", color = CyberEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            "INSTALLING" -> Text("⟳ Installing...", color = CyberCyan, fontSize = 11.sp)
                                            "FAILED" -> Text("✗ Failed", color = CyberRed, fontSize = 11.sp)
                                            else -> Text("Pending", color = TextMuted, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "The APK is being installed strictly inside the virtual Android environment (/data/app), not directly into the host Android system.",
                        color = TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
