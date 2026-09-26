package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StatusBadge
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.NetworkScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SetupWizardDialog
import com.example.ui.screens.SnapshotsScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.NavigationTab
import com.example.ui.viewmodel.RootBoxViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: RootBoxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                RootBoxApp(viewModel)
            }
        }
    }
}

@Composable
fun RootBoxApp(viewModel: RootBoxViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val status by viewModel.instanceStatus.collectAsState()
    val showWizard by viewModel.showSetupWizard.collectAsState()
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Back handler to navigate back to Dashboard
    BackHandler(enabled = currentTab != NavigationTab.DASHBOARD) {
        viewModel.setTab(NavigationTab.DASHBOARD)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBarRow(
                currentTab = currentTab,
                status = status,
                onOpenWizard = { viewModel.openSetupWizard() }
            )
        },
        bottomBar = {
            RootBoxBottomNav(
                currentTab = currentTab,
                onTabSelected = { viewModel.setTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { targetTab ->
                when (targetTab) {
                    NavigationTab.DASHBOARD -> DashboardScreen(viewModel)
                    NavigationTab.TERMINAL -> TerminalScreen(viewModel)
                    NavigationTab.FILES -> FileManagerScreen(viewModel)
                    NavigationTab.APPS -> AppsScreen(viewModel)
                    NavigationTab.NETWORK -> NetworkScreen(viewModel)
                    NavigationTab.SNAPSHOTS -> SnapshotsScreen(viewModel)
                    NavigationTab.SETTINGS -> SettingsScreen(viewModel)
                }
            }
        }
    }

    if (showWizard) {
        SetupWizardDialog(
            viewModel = viewModel,
            onComplete = { viewModel.dismissSetupWizard() }
        )
    }
}

@Composable
private fun TopAppBarRow(
    currentTab: NavigationTab,
    status: com.example.core.virtualization.InstanceStatus,
    onOpenWizard: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(CyberEmerald.copy(alpha = 0.2f))
                    .border(1.dp, CyberEmerald.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#",
                    color = CyberEmerald,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = if (currentTab == NavigationTab.DASHBOARD) "ROOTBOX" else "ROOTBOX • ${currentTab.label.uppercase()}",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "No-Root Virtual Sandbox",
                    color = CyberCyan,
                    fontSize = 10.sp
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(status = status)

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onOpenWizard,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("wizard_help_button")
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "Setup Guide",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RootBoxBottomNav(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(DarkSurface)
            .border(1.dp, DarkBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf(
                Triple(NavigationTab.DASHBOARD, "Dashboard", Icons.Default.Dashboard),
                Triple(NavigationTab.TERMINAL, "Terminal", Icons.Default.Terminal),
                Triple(NavigationTab.FILES, "Files", Icons.Default.Folder),
                Triple(NavigationTab.APPS, "Apps", Icons.Default.Android),
                Triple(NavigationTab.NETWORK, "Network", Icons.Default.Wifi),
                Triple(NavigationTab.SNAPSHOTS, "Snapshots", Icons.Default.CameraAlt),
                Triple(NavigationTab.SETTINGS, "Settings", Icons.Default.Settings)
            )

            tabs.forEach { (tab, label, icon) ->
                val selected = currentTab == tab
                Column(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("tab_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (selected) CyberEmerald.copy(alpha = 0.2f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (selected) CyberEmerald else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = label,
                        color = if (selected) CyberEmerald else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
