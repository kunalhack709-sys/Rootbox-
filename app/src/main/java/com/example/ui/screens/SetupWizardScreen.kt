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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
fun SetupWizardDialog(
    viewModel: RootBoxViewModel,
    onComplete: () -> Unit
) {
    var isCreated by remember { mutableStateOf(false) }
    var selectedRam by remember { mutableIntStateOf(2048) }
    var selectedStorage by remember { mutableIntStateOf(16) }
    var selectedImage by remember { mutableStateOf("Android 14 (vAOSP 64-bit)") }
    var imageDropdownExpanded by remember { mutableStateOf(false) }

    val images = listOf(
        "Android 14 (vAOSP 64-bit)",
        "Android 13 (LineageOS Virtual Core)",
        "Android 12 (Pure Minimal vAOSP)"
    )

    Dialog(
        onDismissRequest = onComplete,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .testTag("setup_wizard_dialog")
                .fillMaxSize(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, CyberEmerald.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                if (!isCreated) {
                    // Create Environment Screen
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(CyberEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Android, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Welcome to RootBox", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text("Create your virtual Android environment.", color = CyberCyan, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = DarkBorder)
                        Spacer(modifier = Modifier.height(16.dp))

                        // RAM selection
                        Text("RAM", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(2048, 3072, 4096).forEach { ram ->
                                val selected = selectedRam == ram
                                Button(
                                    onClick = { selectedRam = ram },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (selected) CyberEmerald else DarkBackground),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("$ram MB", color = if (selected) Color.Black else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Storage selection
                        Text("Storage", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(16, 32, 64).forEach { size ->
                                val selected = selectedStorage == size
                                Button(
                                    onClick = { selectedStorage = size },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (selected) CyberCyan else DarkBackground),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("$size GB", color = if (selected) Color.Black else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Android Image Selection
                        Text("Android", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        ExposedDropdownMenuBox(
                            expanded = imageDropdownExpanded,
                            onExpandedChange = { imageDropdownExpanded = !imageDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedImage,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = imageDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )

                            ExposedDropdownMenu(
                                expanded = imageDropdownExpanded,
                                onDismissRequest = { imageDropdownExpanded = false },
                                modifier = Modifier.background(DarkSurface)
                            ) {
                                images.forEach { img ->
                                    DropdownMenuItem(
                                        text = { Text(img, color = TextPrimary) },
                                        onClick = {
                                            selectedImage = img
                                            imageDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Security notice inside wizard
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBackground)
                                .border(1.dp, CyberEmerald.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "No bootloader unlocking or physical root required. The virtual environment operates safely within this container.",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // Create Environment Button
                    Button(
                        onClick = {
                            viewModel.createEnvironment(selectedRam, selectedStorage, selectedImage)
                            isCreated = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("create_environment_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Create Environment", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Ready Screen
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CyberEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(40.dp))
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text("RootBox is ready.", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Virtual Android instance initialized with $selectedRam MB RAM and $selectedStorage GB storage partition.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                onComplete()
                                viewModel.openAddAppSheet()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("wizard_add_app_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Add App", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onComplete,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("wizard_open_rootbox_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Open RootBox", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
