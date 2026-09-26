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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.core.filesystem.VirtualFileItem
import com.example.ui.components.FileEditorDialog
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

@Composable
fun FileManagerScreen(
    viewModel: RootBoxViewModel,
    modifier: Modifier = Modifier
) {
    val currentPath by viewModel.currentVirtualPath.collectAsState()
    val files by viewModel.currentVirtualFiles.collectAsState()
    val selectedFileForEdit by viewModel.selectedFileForEdit.collectAsState()

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }

    var exportTargetVirtualPath by remember { mutableStateOf<String?>(null) }

    // Import launcher from host storage
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = "imported_${System.currentTimeMillis()}"
            viewModel.importFileToVirtual(uri, fileName)
        }
    }

    // Export launcher to host storage
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { destUri: Uri? ->
        val vPath = exportTargetVirtualPath
        if (destUri != null && vPath != null) {
            viewModel.exportFileFromVirtual(vPath, destUri)
        }
        exportTargetVirtualPath = null
    }

    Column(
        modifier = modifier
            .testTag("file_manager_screen")
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
    ) {
        // Navigation bar with breadcrumbs & actions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateUp() },
                    enabled = currentPath != "/" && currentPath.isNotEmpty(),
                    modifier = Modifier.testTag("file_nav_up_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Up",
                        tint = if (currentPath != "/") CyberCyan else TextMuted
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "VIRTUAL FILESYSTEM",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = currentPath,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Add Folder
                IconButton(onClick = { showNewFolderDialog = true }, modifier = Modifier.testTag("new_folder_button")) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = CyberCyan)
                }

                // Add File
                IconButton(onClick = { showNewFileDialog = true }, modifier = Modifier.testTag("new_file_button")) {
                    Icon(Icons.Default.NoteAdd, contentDescription = "New File", tint = CyberEmerald)
                }

                // Import from Host
                IconButton(onClick = { importLauncher.launch("*/*") }, modifier = Modifier.testTag("import_file_button")) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Import from Host", tint = TextPrimary)
                }

                // Refresh
                IconButton(onClick = { viewModel.refreshVirtualFiles() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextMuted)
                }
            }
        }

        // Files List
        Box(modifier = Modifier.weight(1f)) {
            if (files.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = DarkBorder, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Directory is empty", color = TextMuted, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(files) { item ->
                        FileItemRow(
                            item = item,
                            onClick = {
                                if (item.isDirectory) {
                                    viewModel.navigateVirtualDir(item.virtualPath)
                                } else {
                                    viewModel.openFileForEdit(item.virtualPath)
                                }
                            },
                            onEdit = {
                                viewModel.openFileForEdit(item.virtualPath)
                            },
                            onDelete = {
                                viewModel.deleteVirtualFile(item.virtualPath)
                            },
                            onExport = {
                                exportTargetVirtualPath = item.virtualPath
                                exportLauncher.launch(item.name)
                            }
                        )
                    }
                }
            }
        }
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Create Virtual Directory", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName.trim())
                            newFolderName = ""
                            showNewFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("Create", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // New File Dialog
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Create Virtual File", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("File Name (e.g. script.sh)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            viewModel.createNewFile(newFileName.trim(), "")
                            newFileName = ""
                            showNewFileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald)
                ) {
                    Text("Create", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Text Editor Dialog
    selectedFileForEdit?.let { (path, content) ->
        FileEditorDialog(
            filePath = path,
            initialContent = content,
            onSave = { updatedContent ->
                viewModel.saveFileEdit(path, updatedContent)
            },
            onDismiss = {
                viewModel.closeFileEdit()
            }
        )
    }
}

@Composable
private fun FileItemRow(
    item: VirtualFileItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .border(1.dp, DarkBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            item.isDirectory -> CyberCyan.copy(alpha = 0.15f)
                            item.isExecutable -> CyberEmerald.copy(alpha = 0.15f)
                            else -> DarkBorder
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        item.isDirectory -> Icons.Default.Folder
                        item.isExecutable -> Icons.Default.Code
                        else -> Icons.Default.Description
                    },
                    contentDescription = null,
                    tint = when {
                        item.isDirectory -> CyberCyan
                        item.isExecutable -> CyberEmerald
                        else -> TextSecondary
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Row {
                    Text(
                        text = item.permissions,
                        color = CyberEmerald,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.owner,
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.formattedSize,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextMuted)
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    if (!item.isDirectory) {
                        DropdownMenuItem(
                            text = { Text("Edit in Text Editor", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = CyberCyan) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export to Phone Storage", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = CyberEmerald) },
                            onClick = {
                                menuExpanded = false
                                onExport()
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("Delete", color = CyberRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = CyberRed) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
