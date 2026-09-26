package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.terminal.LineType
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.CyberRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalPrompt
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TerminalText
import com.example.ui.theme.TerminalYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.RootBoxViewModel

@Composable
fun TerminalScreen(
    viewModel: RootBoxViewModel,
    modifier: Modifier = Modifier
) {
    val lines by viewModel.terminalLines.collectAsState()
    val prompt by viewModel.terminalPrompt.collectAsState()
    var inputCommand by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new output
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    val quickCommands = listOf(
        "apt update",
        "apt install nmap",
        "nmap 192.168.100.1",
        "ifconfig",
        "neofetch",
        "kali-banner",
        "curl -I https://google.com",
        "whois google.com",
        "dig example.com",
        "traceroute 1.1.1.1",
        "netstat -tuln",
        "python3 -c \"print('RootBox')\"",
        "msfconsole",
        "apt list --installed",
        "id",
        "uname -a",
        "ls /",
        "df -h",
        "clear"
    )

    Column(
        modifier = modifier
            .testTag("terminal_screen")
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Terminal Window Header with Kali branding
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Cyber Window Controls
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(CyberRed))
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(TerminalYellow))
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(CyberEmerald))

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Kali Terminal",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "[root@kali]",
                    color = CyberEmerald,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { viewModel.sendTerminalCommand("kali-mode") },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.2f)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("Kali Mode", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { viewModel.clearTerminal() },
                    modifier = Modifier.size(28.dp).testTag("clear_terminal_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Terminal",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Terminal Console Output Window
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(TerminalBg)
                .border(1.dp, DarkBorder)
                .padding(10.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(lines) { line ->
                    val color = when (line.type) {
                        LineType.COMMAND -> TerminalCyan
                        LineType.OUTPUT -> TerminalText
                        LineType.ERROR -> TerminalRed
                        LineType.SUCCESS -> CyberEmerald
                        LineType.SYSTEM -> TerminalCyan
                        LineType.WARNING -> TerminalYellow
                    }

                    Text(
                        text = line.text,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }

        // Quick Command Shortcuts Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickCommands.forEach { cmd ->
                FilterChip(
                    selected = false,
                    onClick = {
                        viewModel.sendTerminalCommand(cmd)
                    },
                    label = {
                        Text(
                            text = cmd,
                            color = when {
                                cmd.startsWith("apt") -> CyberEmerald
                                cmd.startsWith("nmap") || cmd.startsWith("msf") -> CyberAmber
                                else -> CyberCyan
                            },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = DarkBackground,
                        labelColor = CyberCyan
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = DarkBorder,
                        enabled = true,
                        selected = false
                    ),
                    modifier = Modifier.testTag("quick_cmd_${cmd.replace(" ", "_")}")
                )
            }
        }

        // Command Prompt & Input Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                if (prompt.contains("\n")) {
                    // Multi-line Kali prompt: ┌──(root㉿kali)-[/]
                    val parts = prompt.split("\n")
                    Text(
                        text = parts[0],
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeSymbol = if (prompt.contains("\n")) prompt.substringAfterLast("\n") else prompt
                    Text(
                        text = activeSymbol,
                        color = CyberEmerald,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                    )

                    OutlinedTextField(
                        value = inputCommand,
                        onValueChange = { inputCommand = it },
                        placeholder = {
                            Text("e.g. apt install nmap, ifconfig...", color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("terminal_input_field"),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TextPrimary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputCommand.isNotBlank()) {
                                    viewModel.sendTerminalCommand(inputCommand)
                                    inputCommand = ""
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkBorder
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (inputCommand.isNotBlank()) {
                                viewModel.sendTerminalCommand(inputCommand)
                                inputCommand = ""
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CyberEmerald)
                            .testTag("send_command_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
