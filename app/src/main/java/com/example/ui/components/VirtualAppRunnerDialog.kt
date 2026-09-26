package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.data.local.entities.VirtualAppEntity
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun VirtualAppRunnerDialog(
    app: VirtualAppEntity,
    onClose: () -> Unit,
    onStop: () -> Unit
) {
    val pid = remember(app.packageName) {
        kotlin.math.abs(app.packageName.hashCode() % 800) + 120
    }

    val simulatedLogs = remember(app.packageName) {
        listOf(
            "[RootBox::Runtime] Spawning zygote fork for ${app.packageName}...",
            "[RootBox::Sandbox] Assigned virtual UID u0_a${pid % 100} (Elevated Virtual Root context: ALLOWED)",
            "[am] Starting: Intent { act=android.intent.action.MAIN cmp=${app.packageName}/.MainActivity }",
            "[SystemServer] WindowManager: Created surface layer 1080x2400 (DPI 420)",
            "[ActivityThread] Loaded package: ${app.packageName} (version ${app.versionName})",
            "[vAOSP::su] App requested superuser binary check -> /system/xbin/su (OK: GRANTED)",
            "[OpenGLRenderer] Initializing Virtual GLES 3.2 Context via Vulkan host bridge",
            "[RootBox::vnet0] Virtual socket bound: 192.168.100.2:${40000 + pid}",
            "[ProcessMonitor] PID $pid state: TOP_ACTIVITY (Active in virtual display)"
        )
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .testTag("virtual_app_runner_dialog")
                .fillMaxSize(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, CyberEmerald.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Window Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberEmerald.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = null,
                            tint = CyberEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.appName,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "PID: $pid • Virtual Sandbox • vAOSP 14",
                            color = CyberEmerald,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_runner_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Status Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberEmerald.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Virtual Root Context: Active", color = CyberEmerald, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("48.2 MB RAM", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                // Simulated App Runtime Terminal & Execution Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(TerminalBg)
                        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = "=== [RootBox Virtual Application Container] ===",
                                color = CyberCyan,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Target: ${app.packageName} (isolated environment)",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(simulatedLogs) { log ->
                            Text(
                                text = log,
                                color = when {
                                    log.contains("GRANTED") || log.contains("ALLOWED") -> CyberEmerald
                                    log.contains("Starting") || log.contains("Loaded") -> TextPrimary
                                    else -> TextSecondary
                                },
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(CyberEmerald)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Application process alive in background container",
                                    color = CyberEmerald,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // Footer Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onStop()
                            onClose()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kill_process_button")
                    ) {
                        Text("Terminate (SIGKILL)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBorder),
                        modifier = Modifier.testTag("minimize_runner_button")
                    ) {
                        Text("Run in Background", color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
