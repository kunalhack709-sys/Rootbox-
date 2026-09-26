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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.virtualization.HardwareConfig
import com.example.ui.components.SecurityNoticeBanner
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.RootBoxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: RootBoxViewModel,
    modifier: Modifier = Modifier
) {
    val currentConfig by viewModel.hardwareConfig.collectAsState()

    var ramMb by remember(currentConfig.allocatedRamMb) { mutableIntStateOf(currentConfig.allocatedRamMb) }
    var cpuCores by remember(currentConfig.cpuCores) { mutableIntStateOf(currentConfig.cpuCores) }
    var storageGb by remember(currentConfig.virtualStorageGb) { mutableIntStateOf(currentConfig.virtualStorageGb) }
    var systemImage by remember(currentConfig.systemImage) { mutableStateOf(currentConfig.systemImage) }
    var allowSharedStorage by remember(currentConfig.allowSharedStorage) { mutableStateOf(currentConfig.allowSharedStorage) }
    var rootMode by remember(currentConfig.virtualRootMode) { mutableStateOf(currentConfig.virtualRootMode) }

    var sysDropdownOpen by remember { mutableStateOf(false) }
    var rootModeDropdownOpen by remember { mutableStateOf(false) }

    val systemImages = listOf(
        "Android 14 (vAOSP 64-bit Minimal)",
        "Android 13 (LineageOS Virtual Core)",
        "Android 12 (Minimal Pure AOSP)"
    )

    val rootModes = listOf(
        "Root Always (UID 0)",
        "Interactive SU Prompt",
        "Restricted (UID 2000)"
    )

    LazyColumn(
        modifier = modifier
            .testTag("settings_screen")
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            SecurityNoticeBanner()
        }

        // Hardware Allocation Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyberEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("VIRTUAL HARDWARE ALLOCATION", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // RAM Selection
                    Text("Virtual RAM Allocation: ${ramMb / 1024} GB (${ramMb} MB)", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2048, 3072, 4096, 6144).forEach { size ->
                            val isSelected = ramMb == size
                            Button(
                                onClick = { ramMb = size },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) CyberEmerald else DarkBorder
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${size / 1024}GB",
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // CPU Cores Selection
                    Text("Virtual CPU Cores: $cpuCores Cores", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2, 4, 6, 8).forEach { cores ->
                            val isSelected = cpuCores == cores
                            Button(
                                onClick = { cpuCores = cores },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) CyberCyan else DarkBorder
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${cores}C",
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Virtual Storage Partition
                    Text("Virtual Storage Size: $storageGb GB", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(16, 32, 64, 128).forEach { size ->
                            val isSelected = storageGb == size
                            Button(
                                onClick = { storageGb = size },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) CyberAmber else DarkBorder
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${size}GB",
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Virtual System Image & Root Mode
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SYSTEM IMAGE & ROOT POLICY", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Virtual Android Image", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = sysDropdownOpen,
                        onExpandedChange = { sysDropdownOpen = !sysDropdownOpen }
                    ) {
                        OutlinedTextField(
                            value = systemImage,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sysDropdownOpen) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )

                        ExposedDropdownMenu(
                            expanded = sysDropdownOpen,
                            onDismissRequest = { sysDropdownOpen = false },
                            modifier = Modifier.background(DarkSurface)
                        ) {
                            systemImages.forEach { img ->
                                DropdownMenuItem(
                                    text = { Text(img, color = TextPrimary) },
                                    onClick = {
                                        systemImage = img
                                        sysDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Virtual Superuser Mode", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = rootModeDropdownOpen,
                        onExpandedChange = { rootModeDropdownOpen = !rootModeDropdownOpen }
                    ) {
                        OutlinedTextField(
                            value = rootMode,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rootModeDropdownOpen) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )

                        ExposedDropdownMenu(
                            expanded = rootModeDropdownOpen,
                            onDismissRequest = { rootModeDropdownOpen = false },
                            modifier = Modifier.background(DarkSurface)
                        ) {
                            rootModes.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode, color = TextPrimary) },
                                    onClick = {
                                        rootMode = mode
                                        rootModeDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Shared storage switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Shared Storage Access", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Bridges /sdcard/Download with phone public shared media", color = TextMuted, fontSize = 11.sp)
                        }

                        Switch(
                            checked = allowSharedStorage,
                            onCheckedChange = { allowSharedStorage = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = CyberEmerald)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.updateHardwareConfig(
                                currentConfig.copy(
                                    allocatedRamMb = ramMb,
                                    cpuCores = cpuCores,
                                    virtualStorageGb = storageGb,
                                    systemImage = systemImage,
                                    virtualRootMode = rootMode,
                                    allowSharedStorage = allowSharedStorage
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                        modifier = Modifier.fillMaxWidth().testTag("save_settings_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Configuration", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
