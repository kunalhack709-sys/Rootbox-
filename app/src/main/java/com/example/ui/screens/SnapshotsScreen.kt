package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.SnapshotEntity
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.RootBoxViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SnapshotsScreen(
    viewModel: RootBoxViewModel,
    modifier: Modifier = Modifier
) {
    val snapshots by viewModel.snapshots.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var snapshotName by remember { mutableStateOf("") }
    var snapshotDesc by remember { mutableStateOf("") }

    var snapshotToRestore by remember { mutableStateOf<SnapshotEntity?>(null) }
    var autoBackupEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .testTag("snapshots_screen")
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "VIRTUAL SNAPSHOTS",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Freeze & restore entire virtual machine states",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                modifier = Modifier.testTag("create_snapshot_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create Snapshot", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Automatic Backup Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto-Snapshot on APK Install", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Automatically creates restore point before installing packages", color = TextMuted, fontSize = 11.sp)
                }

                Switch(
                    checked = autoBackupEnabled,
                    onCheckedChange = { autoBackupEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = CyberCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Snapshots List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(snapshots) { snap ->
                SnapshotCard(
                    snapshot = snap,
                    onRestore = { snapshotToRestore = snap },
                    onDelete = { viewModel.deleteSnapshot(snap.id) }
                )
            }
        }
    }

    // Create Snapshot Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create System Snapshot", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = snapshotName,
                        onValueChange = { snapshotName = it },
                        label = { Text("Snapshot Name") },
                        placeholder = { Text("e.g. Before Kernel Mod") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = snapshotDesc,
                        onValueChange = { snapshotDesc = it },
                        label = { Text("Description") },
                        placeholder = { Text("Details of virtual environment state") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (snapshotName.isNotBlank()) {
                            viewModel.createSnapshot(snapshotName.trim(), snapshotDesc.trim())
                            snapshotName = ""
                            snapshotDesc = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("Save Snapshot", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Confirm Restore Dialog
    snapshotToRestore?.let { snap ->
        AlertDialog(
            onDismissRequest = { snapshotToRestore = null },
            title = { Text("Restore Virtual Snapshot?", color = TextPrimary) },
            text = {
                Text(
                    text = "Restoring '${snap.name}' will roll back all files in /data, installed packages, and virtual app states to this snapshot timestamp. Physical phone storage is unaffected.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreSnapshot(snap.id)
                        snapshotToRestore = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAmber)
                ) {
                    Text("Confirm Restore", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { snapshotToRestore = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun SnapshotCard(
    snapshot: SnapshotEntity,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
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
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = snapshot.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date(snapshot.timestamp))
                        Text(text = "$dateStr • ${snapshot.systemVersion}", color = TextMuted, fontSize = 11.sp)
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CyberRed, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(text = snapshot.description, color = TextSecondary, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val mb = snapshot.sizeBytes / (1024.0 * 1024.0)
                Text(
                    text = "${snapshot.appsCount} virtual apps • ${String.format(Locale.US, "%.1f MB", mb)}",
                    color = CyberEmerald,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                OutlinedButton(
                    onClick = onRestore,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAmber),
                    modifier = Modifier.testTag("restore_btn_${snapshot.id}")
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
