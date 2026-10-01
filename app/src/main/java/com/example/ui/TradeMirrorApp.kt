package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppRoleView
import com.example.ui.viewmodel.TradingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeMirrorApp(viewModel: TradingViewModel) {
    val isSplashVisible by viewModel.isSplashVisible.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val customerTab by viewModel.customerTab.collectAsState()
    val leaderTab by viewModel.leaderTab.collectAsState()
    val adminTab by viewModel.adminTab.collectAsState()
    val riskLimits by viewModel.customerRiskLimits.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val systemConfig by viewModel.systemConfig.collectAsState()

    val isCustomerStopped = riskLimits?.isEmergencyStopped ?: false
    val isGlobalKillSwitchActive = systemConfig?.isGlobalKillSwitchActive ?: false
    val isOffline by viewModel.isOffline.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    var showRoleSheet by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    PiscesTheme {
        if (isSplashVisible) {
            SplashScreen(viewModel)
        } else if (!isLoggedIn) {
            LoginScreen(viewModel)
        } else {
            Scaffold(
                topBar = {
                    Column(
                        modifier = Modifier
                            .background(SurfacePrimary)
                            .statusBarsPadding()
                    ) {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SurfaceSecondary)
                                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.TrendingUp,
                                            contentDescription = "PISCES",
                                            tint = PureWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "PISCES",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 2.sp
                                            ),
                                            color = PureWhite
                                        )
                                        Text(
                                            text = "PAPER TRADING • NO REAL MONEY",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = TextSecondary
                                        )
                                    }
                                }
                            },
                            actions = {
                                // Emergency Freeze Button in TopBar
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.promptEmergencyFreezeConfirmation()
                                    },
                                    modifier = Modifier.testTag("topbar_emergency_freeze_button")
                                ) {
                                    Icon(
                                        imageVector = if (isCustomerStopped) Icons.Outlined.PlayCircle else Icons.Outlined.PauseCircle,
                                        contentDescription = "Emergency Freeze",
                                        tint = PureWhite
                                    )
                                }

                                // Notifications Icon with unread dot
                                BadgedBox(
                                    badge = {
                                        if (notifications.any { !it.isRead }) {
                                            Badge(
                                                containerColor = PureWhite,
                                                contentColor = PureBlack
                                            ) {
                                                Text("${notifications.count { !it.isRead }}")
                                            }
                                        }
                                    }
                                ) {
                                    IconButton(
                                        onClick = { showNotificationsDialog = true },
                                        modifier = Modifier.testTag("notifications_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Notifications,
                                            contentDescription = "Notifications",
                                            tint = PureWhite
                                        )
                                    }
                                }

                                // Role selector pill
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showRoleSheet = true
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = SurfaceSecondary,
                                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                                    modifier = Modifier
                                        .padding(start = 4.dp, end = 12.dp)
                                        .testTag("open_role_selector_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when (currentRole) {
                                                AppRoleView.CUSTOMER -> "Customer"
                                                AppRoleView.LEADER -> "Leader"
                                                AppRoleView.ADMIN -> "Admin"
                                                AppRoleView.DEV_TEST_PANEL -> "Dev Chaos"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = PureWhite
                                        )
                                        Icon(
                                            imageVector = Icons.Outlined.ArrowDropDown,
                                            contentDescription = null,
                                            tint = PureWhite,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfacePrimary)
                        )
                        OfflineNoticeBanner(
                            isOffline = isOffline,
                            onRetry = { viewModel.toggleOffline(false) }
                        )
                        HorizontalDivider(color = SurfaceBorderSubtle)
                    }
                },
                bottomBar = {
                    // Monochrome Bottom Navigation Bar
                    NavigationBar(
                        containerColor = SurfacePrimary,
                        contentColor = PureWhite,
                        tonalElevation = 0.dp,
                        windowInsets = NavigationBarDefaults.windowInsets
                    ) {
                        when (currentRole) {
                            AppRoleView.CUSTOMER -> {
                                val items = listOf(
                                    Triple(0, "Dashboard", Icons.Outlined.Dashboard),
                                    Triple(1, "Marketplace", Icons.Outlined.Storefront),
                                    Triple(2, "Positions", Icons.Outlined.Layers),
                                    Triple(3, "Orders", Icons.Outlined.ReceiptLong),
                                    Triple(4, "Profile", Icons.Outlined.Person)
                                )
                                items.forEach { (index, label, icon) ->
                                    val isSelected = customerTab == index
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.setCustomerTab(index)
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = if (isSelected) PureBlack else TextSecondary
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) PureWhite else TextSecondary
                                                )
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = PureWhite,
                                            selectedIconColor = PureBlack,
                                            unselectedIconColor = TextSecondary,
                                            selectedTextColor = PureWhite,
                                            unselectedTextColor = TextSecondary
                                        ),
                                        modifier = Modifier.testTag("nav_customer_$index")
                                    )
                                }
                            }
                            AppRoleView.LEADER -> {
                                val items = listOf(
                                    Triple(0, "Overview", Icons.Outlined.Dashboard),
                                    Triple(1, "Signal Studio", Icons.Outlined.Bolt),
                                    Triple(2, "Copiers", Icons.Outlined.Groups),
                                    Triple(3, "Strategies", Icons.Outlined.AutoGraph)
                                )
                                items.forEach { (index, label, icon) ->
                                    val isSelected = leaderTab == index
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.setLeaderTab(index)
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = if (isSelected) PureBlack else TextSecondary
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) PureWhite else TextSecondary
                                                )
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = PureWhite,
                                            selectedIconColor = PureBlack,
                                            unselectedIconColor = TextSecondary,
                                            selectedTextColor = PureWhite,
                                            unselectedTextColor = TextSecondary
                                        ),
                                        modifier = Modifier.testTag("nav_leader_$index")
                                    )
                                }
                            }
                            AppRoleView.ADMIN -> {
                                val items = listOf(
                                    Triple(0, "Overview", Icons.Outlined.Speed),
                                    Triple(1, "Blotter", Icons.Outlined.FormatListBulleted),
                                    Triple(2, "Reconcile", Icons.Outlined.Rule),
                                    Triple(3, "Logs", Icons.Outlined.History),
                                    Triple(4, "Users", Icons.Outlined.People)
                                )
                                items.forEach { (index, label, icon) ->
                                    val isSelected = adminTab == index
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.setAdminTab(index)
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = if (isSelected) PureBlack else TextSecondary
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) PureWhite else TextSecondary
                                                )
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = PureWhite,
                                            selectedIconColor = PureBlack,
                                            unselectedIconColor = TextSecondary,
                                            selectedTextColor = PureWhite,
                                            unselectedTextColor = TextSecondary
                                        ),
                                        modifier = Modifier.testTag("nav_admin_$index")
                                    )
                                }
                            }
                            AppRoleView.DEV_TEST_PANEL -> {
                                NavigationBarItem(
                                    selected = true,
                                    onClick = {},
                                    icon = { Icon(Icons.Outlined.BugReport, contentDescription = "Chaos", tint = PureBlack) },
                                    label = { Text("Chaos Simulator", color = PureWhite) },
                                    colors = NavigationBarItemDefaults.colors(indicatorColor = PureWhite)
                                )
                            }
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(PureBlack)
                ) {
                    when (currentRole) {
                        AppRoleView.CUSTOMER -> CustomerScreen(viewModel)
                        AppRoleView.LEADER -> LeaderScreen(viewModel)
                        AppRoleView.ADMIN -> AdminScreen(viewModel)
                        AppRoleView.DEV_TEST_PANEL -> DevTestPanelScreen(viewModel)
                    }
                }
            }

            // Role Switcher Modal Bottom Sheet
            if (showRoleSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showRoleSheet = false },
                    containerColor = SurfacePrimary,
                    sheetState = rememberModalBottomSheetState()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .navigationBarsPadding()
                    ) {
                        Text(
                            text = "SWITCH PISCES ROLE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Explore the trade mirroring engine from different stakeholder perspectives.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val roles = listOf(
                            RoleOption(
                                AppRoleView.CUSTOMER,
                                "Customer / Follower",
                                "Arjun Mehta • Browse strategies, auto-copy signals, risk limits & blotter",
                                Icons.Outlined.Person
                            ),
                            RoleOption(
                                AppRoleView.LEADER,
                                "Strategy Leader",
                                "Rajesh Sharma (Quant Alpha) • Broadcast signals & monitor copier executions",
                                Icons.Outlined.ShowChart
                            ),
                            RoleOption(
                                AppRoleView.ADMIN,
                                "Admin & Compliance",
                                "Vikram Aditya • Master blotter, automated reconciliation & global kill switch",
                                Icons.Outlined.Security
                            ),
                            RoleOption(
                                AppRoleView.DEV_TEST_PANEL,
                                "Developer Chaos Simulator",
                                "Inject paper broker timeouts, circuit limit rejections & partial fills",
                                Icons.Outlined.BugReport
                            )
                        )

                        roles.forEach { item ->
                            val isSelected = currentRole == item.role
                            Card(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.selectRole(item.role)
                                    showRoleSheet = false
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) SurfaceSecondary else Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PureWhite)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("sheet_select_role_${item.role.name}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) PureWhite else SurfaceSecondary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) PureBlack else PureWhite,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = PureWhite
                                        )
                                        Text(
                                            text = item.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = PureWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Notifications Dialog
            if (showNotificationsDialog) {
                AlertDialog(
                    onDismissRequest = { showNotificationsDialog = false },
                    containerColor = SurfacePrimary,
                    titleContentColor = PureWhite,
                    textContentColor = TextSecondary,
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("PISCES Notifications", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            TextButton(onClick = { viewModel.markNotificationsRead() }) {
                                Text("Mark Read", color = PureWhite)
                            }
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (notifications.isEmpty()) {
                                Text("No alerts recorded.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            } else {
                                notifications.forEach { notif ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
                                        shape = RoundedCornerShape(8.dp),
                                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(notif.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(notif.message, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showNotificationsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Close", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }
}

data class RoleOption(
    val role: AppRoleView,
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

