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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

@Composable
fun SetupWizardDialog(
    viewModel: RootBoxViewModel,
    onComplete: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }

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
                // Header Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROOTBOX SETUP WIZARD",
                        color = CyberEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Step $step of 3",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Step Content
                Box(modifier = Modifier.weight(1f)) {
                    when (step) {
                        1 -> StepOneSecurity()
                        2 -> StepTwoHardware()
                        3 -> StepThreeFinish()
                    }
                }

                // Bottom Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (step > 1) {
                        OutlinedButton(
                            onClick = { step-- },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Button(
                        onClick = {
                            if (step < 3) {
                                step++
                            } else {
                                onComplete()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                        modifier = Modifier.testTag("wizard_next_button")
                    ) {
                        Text(
                            text = if (step == 3) "Launch RootBox" else "Continue",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepOneSecurity() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(CyberCyan.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(30.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome to RootBox",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "RootBox provides a self-contained, no-root virtual Android container inside your phone. Compatible apps and tools can run with elevated privileges inside this virtual sandbox.",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkBackground)
                .border(1.dp, CyberEmerald.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("No Bootloader Unlock Needed", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Your physical Android system, kernel and private files are never modified.", color = TextMuted, fontSize = 11.sp)

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Virtual Root (UID 0) Confined to Sandbox", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Root privileges apply exclusively within the RootBox virtual filesystem.", color = TextMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun StepTwoHardware() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(CyberEmerald.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Memory, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(30.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Virtual Machine Profile",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "RootBox allocates a slice of your device's memory and storage for the virtual AOSP instance.",
            color = TextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkBackground)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Recommended Baseline Settings:", color = CyberEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• RAM Allocation: 3.0 GB Virtual RAM", color = TextPrimary, fontSize = 12.sp)
                Text("• CPU Virtual Cores: 4 Cores", color = TextPrimary, fontSize = 12.sp)
                Text("• Storage Partition: 32 GB Virtual Disk", color = TextPrimary, fontSize = 12.sp)
                Text("• Virtual Display: 1080x2400 (420 DPI)", color = TextPrimary, fontSize = 12.sp)
                Text("• Virtual Interface: vnet0 (192.168.100.2)", color = TextPrimary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StepThreeFinish() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(CyberEmerald.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(30.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ready to Launch RootBox",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your virtual rootfs tree has been initialized with /system, /data, /vendor, /tmp and /sdcard. The virtual su binary and package manager are ready.",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkBackground)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Pre-installed Virtual Suite:", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("✓ Termux Virtual CLI", color = TextPrimary, fontSize = 12.sp)
                Text("✓ BusyBox Utilities (1.36.1)", color = TextPrimary, fontSize = 12.sp)
                Text("✓ Root Explorer Virtual Shell", color = TextPrimary, fontSize = 12.sp)
                Text("✓ SuperSU Virtual Manager", color = TextPrimary, fontSize = 12.sp)
                Text("✓ MicroWebServer HTTPD", color = TextPrimary, fontSize = 12.sp)
            }
        }
    }
}
