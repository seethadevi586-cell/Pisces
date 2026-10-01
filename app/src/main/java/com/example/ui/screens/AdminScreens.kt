package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.broker.model.*
import com.example.domain.model.MoneyFormatter
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppRoleView
import com.example.ui.viewmodel.TradingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminScreen(viewModel: TradingViewModel) {
    val selectedTab by viewModel.adminTab.collectAsState()
    val systemConfig by viewModel.systemConfig.collectAsState()
    val isKillSwitchActive = systemConfig?.isGlobalKillSwitchActive ?: false

    val tabTitles = listOf("Overview", "Master Blotter", "Reconciliation", "Audit Logs", "Users")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        KillSwitchBanner(isActive = isKillSwitchActive)

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfacePrimary,
            contentColor = PureWhite,
            edgePadding = 12.dp,
            divider = { HorizontalDivider(color = SurfaceBorder) }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { viewModel.setAdminTab(index) },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) PureWhite else TextSecondary
                        )
                    },
                    modifier = Modifier.testTag("admin_tab_$index")
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> AdminOverviewTab(viewModel)
                1 -> MasterBlotterTab(viewModel)
                2 -> ReconciliationTab(viewModel)
                3 -> AuditLogsTab(viewModel)
                4 -> AdminUsersTab(viewModel)
            }
        }
    }
}

@Composable
fun AdminOverviewTab(viewModel: TradingViewModel) {
    val systemConfig by viewModel.systemConfig.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()
    val allPositions by viewModel.allPositions.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val isKillSwitchActive = systemConfig?.isGlobalKillSwitchActive ?: false

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ADMIN & COMPLIANCE BLOTTER",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "System Health & Execution Control",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricStatCard(
                            title = "Registered Users",
                            value = "${allUsers.size}",
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "Total Orders",
                            value = "${allOrders.size}",
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "Open Positions",
                            value = "${allPositions.size}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Global Execution Kill Switch Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PureWhite)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GLOBAL EXECUTION KILL SWITCH",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Immediate freeze on copy engine routing. Blocks all orders across every follower and strategy.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isKillSwitchActive) {
                        PiscesPrimaryButton(
                            text = "ENGAGE GLOBAL KILL SWITCH (FREEZE ALL)",
                            onClick = { viewModel.toggleGlobalKillSwitch(true) },
                            icon = Icons.Outlined.Block,
                            modifier = Modifier.testTag("admin_global_kill_switch_button")
                        )
                    } else {
                        PiscesSecondaryButton(
                            text = "RELEASE GLOBAL KILL SWITCH (RESUME)",
                            onClick = { viewModel.toggleGlobalKillSwitch(false) },
                            icon = Icons.Outlined.CheckCircle,
                            modifier = Modifier.testTag("admin_global_kill_switch_button")
                        )
                    }
                }
            }
        }

        item {
            PiscesSecondaryButton(
                text = "Switch to Customer Portfolio",
                onClick = { viewModel.selectRole(AppRoleView.CUSTOMER) },
                icon = Icons.Outlined.Person
            )
        }
    }
}

@Composable
fun MasterBlotterTab(viewModel: TradingViewModel) {
    val allOrders by viewModel.allOrders.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "MASTER ORDER BLOTTER (${allOrders.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = PureWhite
            )
        }

        items(items = allOrders, key = { it.id }) { order ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${order.side.name} ${order.symbol}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = PureWhite
                        )
                        OrderStatusBadge(order.status)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Follower: ${order.followerId} • Qty: ${order.executedQuantity}/${order.requestedQuantity} • Avg: ${MoneyFormatter.formatPaise(order.averagePricePaise)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    if (order.rejectionReason != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reject Reason: ${order.rejectionReason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PureWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReconciliationTab(viewModel: TradingViewModel) {
    val reconResult by viewModel.reconciliationResult.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "POSITION RECONCILIATION AUDITOR",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Continuous automated audit comparing internal DB positions with broker settlement books.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        item {
            PiscesPrimaryButton(
                text = if (isBusy) "Auditing Position Records..." else "Run Position Reconciliation Audit",
                onClick = { viewModel.triggerReconciliation() },
                enabled = !isBusy,
                icon = Icons.Outlined.Refresh,
                modifier = Modifier.testTag("run_reconciliation_button")
            )
        }

        if (reconResult != null) {
            val res = reconResult!!
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PureWhite)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (res.isBalanced) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (res.isBalanced) "AUDIT STATUS: BALANCED" else "AUDIT STATUS: DISCREPANCIES DETECTED",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = PureWhite
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Total Positions Audited: ${res.totalPositionsAudited} | Discrepancies Found: ${res.discrepancies.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (res.discrepancies.isNotEmpty()) {
                item {
                    Text(
                        text = "Discrepancy Breakdown:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PureWhite
                    )
                }

                items(res.discrepancies) { disc ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "${disc.discrepancyType.name}: ${disc.symbol}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PureWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = disc.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogsTab(viewModel: TradingViewModel) {
    val logs by viewModel.auditLogs.collectAsState()
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "STRUCTURED AUDIT TRAIL (${logs.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = PureWhite
            )
        }

        items(items = logs, key = { it.id }) { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = log.action,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = PureWhite
                        )
                        Text(
                            text = sdf.format(Date(log.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Entity: ${log.entityType} (${log.entityId}) • Corr: ${log.correlationId.take(12)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = log.detailsJson,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = PureWhite
                    )
                }
            }
        }
    }
}

@Composable
fun AdminUsersTab(viewModel: TradingViewModel) {
    val allUsers by viewModel.allUsers.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "USER DIRECTORY (${allUsers.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = PureWhite
            )
        }

        items(items = allUsers, key = { it.id }) { user ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(user.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                        Text("${user.email} • PAN: ${user.panMasked}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceSecondary)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(user.role.name, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                    }
                }
            }
        }
    }
}
