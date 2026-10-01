package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.data.local.entity.*
import com.example.domain.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppRoleView
import com.example.ui.viewmodel.TradingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerScreen(viewModel: TradingViewModel) {
    val selectedTab by viewModel.customerTab.collectAsState()
    val riskLimits by viewModel.customerRiskLimits.collectAsState()
    val isEmergencyStopped = riskLimits?.isEmergencyStopped ?: false
    val showFreezeDialog by viewModel.showFreezeDialog.collectAsState()
    val selectedStrategyDetails by viewModel.selectedStrategyDetails.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        EmergencyStopBanner(isStopped = isEmergencyStopped)

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> CustomerDashboardTab(viewModel)
                1 -> StrategyMarketplaceTab(viewModel)
                2 -> PositionsTab(viewModel)
                3 -> OrdersTab(viewModel)
                4 -> CustomerProfileTab(viewModel)
                else -> CustomerDashboardTab(viewModel)
            }
        }
    }

    // Modal Confirmation Dialog for Emergency Copy Freeze
    if (showFreezeDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissFreezeDialog() },
            containerColor = SurfacePrimary,
            titleContentColor = PureWhite,
            textContentColor = TextSecondary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isEmergencyStopped) Icons.Outlined.PlayCircle else Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEmergencyStopped) "Resume Copy Trading?" else "Confirm Emergency Freeze",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = if (isEmergencyStopped) {
                        "Resuming copy trading will allow new algorithmic signals from your followed strategies to execute automatically in your paper broker account."
                    } else {
                        "Are you sure you want to FREEZE automated copy trading? All incoming signals will be blocked. Existing open positions will be preserved."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmEmergencyFreeze(!isEmergencyStopped) },
                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_freeze_action_button")
                ) {
                    Text(if (isEmergencyStopped) "Confirm Resume" else "CONFIRM FREEZE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissFreezeDialog() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Strategy Details Modal Sheet
    if (selectedStrategyDetails != null) {
        val strat = selectedStrategyDetails!!
        var allocationType by remember { mutableStateOf(AllocationType.FIXED_AMOUNT) }
        var allocationAmountRupees by remember { mutableStateOf("50000") }
        var allocationPercentage by remember { mutableStateOf("20") }
        var multiplier by remember { mutableStateOf("1") }

        AlertDialog(
            onDismissRequest = { viewModel.openStrategyDetails(null) },
            containerColor = SurfacePrimary,
            titleContentColor = PureWhite,
            textContentColor = PureWhite,
            title = {
                Column {
                    Text(strat.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("By ${strat.leaderName} • ${strat.underlying.displayName}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(strat.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                    // Performance grid (strictly monochrome)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceSecondary, RoundedCornerShape(8.dp))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Win Rate", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("${strat.winRatePct}%", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                        }
                        Column {
                            Text("Sharpe", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("${strat.sharpeRatio}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                        }
                        Column {
                            Text("Max DD", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("-${strat.maxDrawdownPct}%", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                        }
                        Column {
                            Text("Min Cap", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(MoneyFormatter.formatPaise(strat.minCapitalPaise), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                        }
                    }

                    // Disclaimer
                    Text(
                        text = "DISCLAIMER: Historical performance is not a guarantee of future returns. Options trading involves risk.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextSecondary
                    )

                    HorizontalDivider(color = SurfaceBorderSubtle)

                    Text("Copy Allocation Settings", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = PureWhite)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = allocationType == AllocationType.FIXED_AMOUNT,
                            onClick = { allocationType = AllocationType.FIXED_AMOUNT },
                            label = { Text("Fixed INR") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PureWhite,
                                selectedLabelColor = PureBlack,
                                containerColor = SurfaceSecondary,
                                labelColor = PureWhite
                            )
                        )
                        FilterChip(
                            selected = allocationType == AllocationType.PERCENTAGE,
                            onClick = { allocationType = AllocationType.PERCENTAGE },
                            label = { Text("Percentage") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PureWhite,
                                selectedLabelColor = PureBlack,
                                containerColor = SurfaceSecondary,
                                labelColor = PureWhite
                            )
                        )
                    }

                    if (allocationType == AllocationType.FIXED_AMOUNT) {
                        OutlinedTextField(
                            value = allocationAmountRupees,
                            onValueChange = { allocationAmountRupees = it },
                            label = { Text("Max Capital Per Trade (₹)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PureWhite,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                focusedContainerColor = SurfaceSecondary,
                                unfocusedContainerColor = SurfaceSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = allocationPercentage,
                            onValueChange = { allocationPercentage = it },
                            label = { Text("Margin Allocation (%)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PureWhite,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                focusedContainerColor = SurfaceSecondary,
                                unfocusedContainerColor = SurfaceSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = multiplier,
                        onValueChange = { multiplier = it },
                        label = { Text("Lot Multiplier (1 = 1x)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PureWhite,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedContainerColor = SurfaceSecondary,
                            unfocusedContainerColor = SurfaceSecondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paise = (allocationAmountRupees.toDoubleOrNull() ?: 50000.0 * 100).toLong()
                        val pct = allocationPercentage.toDoubleOrNull() ?: 20.0
                        val mult = multiplier.toIntOrNull() ?: 1

                        viewModel.followStrategy(
                            strategyId = strat.id,
                            brokerAccountId = "broker_acc_me",
                            allocationType = allocationType,
                            allocationValuePaise = paise,
                            allocationPct = pct,
                            maxMultiplier = mult
                        )
                        viewModel.openStrategyDetails(null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("subscribe_strategy_button")
                ) {
                    Text("Subscribe & Mirror", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.openStrategyDetails(null) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun CustomerDashboardTab(viewModel: TradingViewModel) {
    val positions by viewModel.customerPositions.collectAsState()
    val orders by viewModel.customerOrders.collectAsState()
    val followings by viewModel.customerFollowings.collectAsState()
    val brokerAccounts by viewModel.customerBrokerAccounts.collectAsState()
    val riskLimits by viewModel.customerRiskLimits.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()

    val totalUnrealizedPaise = remember(positions) { positions.sumOf { it.unrealizedPnlPaise } }
    val totalRealizedTodayPaise = remember(riskLimits) { riskLimits?.dailyRealizedPnlPaise ?: 0L }
    val primaryAccount = remember(brokerAccounts) { brokerAccounts.firstOrNull() }
    val isEmergencyStopped = riskLimits?.isEmergencyStopped ?: false
    var editingFollowing by remember { mutableStateOf<StrategyFollowerEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Portfolio Card
        item(key = "dashboard_portfolio_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PORTFOLIO VALUE",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = TextSecondary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceSecondary)
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PAPER TRADING",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = PureWhite
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = MoneyFormatter.formatPaise(primaryAccount?.availableFundsPaise ?: 50000000L),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricStatCard(
                            title = "Today's P&L",
                            value = (if (totalRealizedTodayPaise >= 0) "+ " else "") + MoneyFormatter.formatPaise(totalRealizedTodayPaise),
                            isPositive = totalRealizedTodayPaise >= 0,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "Unrealized P&L",
                            value = (if (totalUnrealizedPaise >= 0) "+ " else "") + MoneyFormatter.formatPaise(totalUnrealizedPaise),
                            isPositive = totalUnrealizedPaise >= 0,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Broker: ${primaryAccount?.brokerCode?.name ?: "SANDBOX"} (${primaryAccount?.status?.name ?: "CONNECTED"})",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "Active Copiers: ${followings.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Emergency Copy Freeze Section
        item(key = "dashboard_emergency_freeze_card") {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isEmergencyStopped) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Emergency Copy Freeze",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = PureWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isEmergencyStopped) {
                            "Automated copy trade execution is currently FROZEN. Existing positions remain intact. Tap below to resume."
                        } else {
                            "Immediately stop new automated copy-trade execution across all mirrored strategies."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isEmergencyStopped) {
                        PiscesPrimaryButton(
                            text = "FREEZE COPY TRADING",
                            onClick = { viewModel.promptEmergencyFreezeConfirmation() },
                            loading = isBusy,
                            icon = Icons.Outlined.PauseCircle,
                            modifier = Modifier.testTag("freeze_copy_trading_button")
                        )
                    } else {
                        PiscesSecondaryButton(
                            text = "RESUME COPY TRADING",
                            onClick = { viewModel.promptEmergencyFreezeConfirmation() },
                            loading = isBusy,
                            icon = Icons.Outlined.PlayCircle,
                            modifier = Modifier.testTag("resume_copy_trading_button")
                        )
                    }
                }
            }
        }

        // Open Positions Preview
        if (positions.isNotEmpty()) {
            item(key = "dashboard_positions_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Open Positions (${positions.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PureWhite
                    )
                    TextButton(onClick = { viewModel.setCustomerTab(2) }) {
                        Text("View All", color = PureWhite, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            items(items = positions.take(2), key = { "dash_pos_${it.id}" }) { pos ->
                PositionItemCard(pos)
            }
        }

        // Active Subscriptions
        item(key = "dashboard_subscriptions_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Subscriptions (${followings.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PureWhite
                )
                TextButton(onClick = { viewModel.setCustomerTab(1) }) {
                    Text("Explore Marketplace", color = PureWhite, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (followings.isEmpty()) {
            item(key = "dashboard_empty_subscriptions") {
                EmptyStateView(
                    title = "No Active Subscriptions",
                    message = "Browse the Strategy Marketplace to mirror trades into your paper trading account.",
                    actionText = "Explore Strategies",
                    onAction = { viewModel.setCustomerTab(1) }
                )
            }
        } else {
            items(items = followings, key = { it.id }) { following ->
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
                                text = following.strategyId.replace("strat_", "").replace("_", " ").uppercase(),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PureWhite
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SurfaceSecondary)
                                        .border(1.dp, SurfaceBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (following.isPaused) "PAUSED" else "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = PureWhite
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Allocation: ${following.allocationType} (Cap: ${MoneyFormatter.formatPaise(following.allocationValuePaise)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { editingFollowing = following },
                                enabled = !isBusy,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1.3f).testTag("btn_configure_copy_${following.id}")
                            ) {
                                Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Settings", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.toggleFollowerPause(following.strategyId, !following.isPaused) },
                                enabled = !isBusy,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (following.isPaused) "Resume" else "Pause", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.stopFollowing(following.strategyId) },
                                enabled = !isBusy,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Text("Cancel", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Recent Orders Header
        item(key = "dashboard_orders_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Copy Orders",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PureWhite
                )
                TextButton(onClick = { viewModel.setCustomerTab(3) }) {
                    Text("View Blotter", color = PureWhite, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (orders.isEmpty()) {
            item(key = "dashboard_empty_orders") {
                EmptyStateView(
                    title = "No Recent Orders",
                    message = "Simulated copy executions will appear here automatically."
                )
            }
        } else {
            items(items = orders.take(3), key = { it.id }) { order ->
                OrderItemCard(order)
            }
        }
    }

    if (editingFollowing != null) {
        CustomerCopySettingsDialog(
            following = editingFollowing!!,
            isBusy = isBusy,
            onDismiss = { editingFollowing = null },
            onSave = { copyEnabled, mode, fixedQty, ratio, pct, capAmountPaise, maxQty, maxCapPaise, maxDailyLossPaise, maxOpenPos, emergencyFreeze ->
                viewModel.updateCopySettings(
                    strategyId = editingFollowing!!.strategyId,
                    copyEnabled = copyEnabled,
                    mode = mode,
                    fixedQty = fixedQty,
                    ratio = ratio,
                    pct = pct,
                    capAmountPaise = capAmountPaise,
                    maxQty = maxQty,
                    maxCapPaise = maxCapPaise,
                    maxDailyLossPaise = maxDailyLossPaise,
                    maxPositions = maxOpenPos,
                    emergencyFreeze = emergencyFreeze
                )
                editingFollowing = null
            }
        )
    }
}

@Composable
fun StrategyMarketplaceTab(viewModel: TradingViewModel) {
    val strategies by viewModel.activeStrategies.collectAsState()
    val followings by viewModel.customerFollowings.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedIndexFilter by remember { mutableStateOf<UnderlyingIndex?>(null) }

    val filteredStrategies = remember(strategies, searchQuery, selectedIndexFilter) {
        strategies.filter { strat ->
            val matchesQuery = searchQuery.isBlank() ||
                strat.name.contains(searchQuery, ignoreCase = true) ||
                strat.leaderName.contains(searchQuery, ignoreCase = true) ||
                strat.description.contains(searchQuery, ignoreCase = true)
            val matchesFilter = selectedIndexFilter == null || strat.underlying == selectedIndexFilter
            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "marketplace_header") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STRATEGY MARKETPLACE",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = PureWhite
                    )
                    OutlinedButton(
                        onClick = {
                            viewModel.selectRole(AppRoleView.LEADER)
                            viewModel.setLeaderTab(3)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_launch_strategy_shortcut")
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Strategy", fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Systematic Indian index options strategies. Review risk profiles before subscribing or launch your own strategy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Search Box with Immediate Local Filtering (Zero Latency)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search strategies or leaders...", color = TextSecondary) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = "Search", tint = PureWhite, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear", tint = PureWhite, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedContainerColor = SurfacePrimary,
                        unfocusedContainerColor = SurfacePrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("marketplace_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedIndexFilter == null,
                        onClick = { selectedIndexFilter = null },
                        label = { Text("All Indices", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PureWhite,
                            selectedLabelColor = PureBlack,
                            containerColor = SurfaceSecondary,
                            labelColor = PureWhite
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    listOf(UnderlyingIndex.NIFTY, UnderlyingIndex.BANKNIFTY, UnderlyingIndex.FINNIFTY).forEach { idx ->
                        FilterChip(
                            selected = selectedIndexFilter == idx,
                            onClick = { selectedIndexFilter = if (selectedIndexFilter == idx) null else idx },
                            label = { Text(idx.name, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PureWhite,
                                selectedLabelColor = PureBlack,
                                containerColor = SurfaceSecondary,
                                labelColor = PureWhite
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        if (filteredStrategies.isEmpty()) {
            item(key = "marketplace_empty_filter") {
                EmptyStateView(
                    title = "No Matching Strategies",
                    message = "Try clearing your search terms or selecting a different underlying index.",
                    actionText = "Reset Filters",
                    onAction = {
                        searchQuery = ""
                        selectedIndexFilter = null
                    }
                )
            }
        } else {
            items(items = filteredStrategies, key = { it.id }) { strategy ->
                val isFollowing = followings.any { it.strategyId == strategy.id }

                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                    modifier = Modifier.fillMaxWidth().testTag("strategy_card_${strategy.id}")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strategy.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PureWhite
                                )
                                Text(
                                    text = "Leader: ${strategy.leaderName} • ${strategy.underlying.displayName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceSecondary)
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "+${strategy.totalReturnPct}% HISTORICAL",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PureWhite
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = strategy.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceSecondary, RoundedCornerShape(8.dp))
                                .border(1.dp, SurfaceBorderSubtle, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Win Rate", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("${strategy.winRatePct}%", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                            }
                            Column {
                                Text("Sharpe", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("${strategy.sharpeRatio}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                            }
                            Column {
                                Text("Max DD", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("-${strategy.maxDrawdownPct}%", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                            }
                            Column {
                                Text("Min Capital", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(MoneyFormatter.formatPaise(strategy.minCapitalPaise), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PiscesSecondaryButton(
                                text = "Details",
                                onClick = { viewModel.openStrategyDetails(strategy) },
                                modifier = Modifier.weight(1f)
                            )

                            if (isFollowing) {
                                PiscesSecondaryButton(
                                    text = "Subscribed",
                                    onClick = { viewModel.stopFollowing(strategy.id) },
                                    enabled = !isBusy,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                PiscesPrimaryButton(
                                    text = "Subscribe",
                                    onClick = { viewModel.openStrategyDetails(strategy) },
                                    enabled = !isBusy,
                                    modifier = Modifier.weight(1f).testTag("subscribe_button_${strategy.id}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PositionsTab(viewModel: TradingViewModel) {
    val positions by viewModel.customerPositions.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OPEN POSITIONS (${positions.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = PureWhite
                )
            }
        }

        if (positions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
                ) {
                    Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No open positions. Active mirrored signals will populate here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(items = positions, key = { it.id }) { pos ->
                PositionItemCard(pos)
            }
        }
    }
}

@Composable
fun PositionItemCard(pos: PositionEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OptionTypeBadge(pos.optionType, pos.side)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = pos.symbol,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PureWhite
                    )
                }
                Text(
                    text = (if (pos.unrealizedPnlPaise >= 0) "+ " else "") + MoneyFormatter.formatPaise(pos.unrealizedPnlPaise),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PureWhite
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Quantity: ${pos.quantity} units (${pos.quantity / pos.underlying.lotSize} lots)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "Avg: ${MoneyFormatter.formatPaise(pos.averageBuyPricePaise)} | LTP: ${MoneyFormatter.formatPaise(pos.ltpPaise)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun OrdersTab(viewModel: TradingViewModel) {
    val orders by viewModel.customerOrders.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "COPY ORDER BLOTTER (${orders.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = PureWhite
            )
        }

        if (orders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder))
                ) {
                    Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No orders recorded.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(items = orders, key = { it.id }) { order ->
                OrderItemCard(order)
            }
        }
    }
}

@Composable
fun OrderItemCard(order: CopyOrderEntity) {
    val sdf = remember { SimpleDateFormat("HH:mm:ss • dd MMM", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Filled: ${order.executedQuantity} / ${order.requestedQuantity} • Exec: ${MoneyFormatter.formatPaise(order.averagePricePaise)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "${order.executionLatencyMs}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = PureWhite
                )
            }
            if (order.rejectionReason != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Cancel, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reason: ${order.rejectionReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PureWhite
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ID: ${order.id.take(12)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextSecondary
                )
                Text(
                    text = sdf.format(Date(order.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun CustomerProfileTab(viewModel: TradingViewModel) {
    val brokerAccounts by viewModel.customerBrokerAccounts.collectAsState()
    val riskLimits by viewModel.customerRiskLimits.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    var profileSubTab by remember { mutableStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User identity banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceSecondary)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = PureWhite)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Arjun Mehta", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                        Text("KYC PAN: AAA***98K • +91 98765 11000", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("Account Tier: Active Retail Paper Investor", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }

        // Subtabs for Profile Sections: 0: Broker Sandbox, 1: Risk Limits, 2: Notifications
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfacePrimary)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Broker Account", "Risk Controls", "Audit Alerts").forEachIndexed { index, title ->
                    val isSelected = profileSubTab == index
                    Surface(
                        selected = isSelected,
                        onClick = { profileSubTab = index },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) PureWhite else Color.Transparent,
                        contentColor = if (isSelected) PureBlack else TextSecondary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        when (profileSubTab) {
            0 -> {
                // Broker Connection Screen
                item {
                    BrokerConnectionSection(viewModel, brokerAccounts)
                }
            }
            1 -> {
                // Risk Limits Screen
                item {
                    RiskLimitsSection(viewModel, riskLimits)
                }
            }
            2 -> {
                // Notifications / Audit alerts
                item {
                    NotificationsSection(viewModel, notifications)
                }
            }
        }

        item {
            HorizontalDivider(color = SurfaceBorderSubtle)
        }

        // Switch to Leader or Admin panels
        item {
            Text("Stakeholder Role Switching", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PiscesSecondaryButton(
                    text = "Leader Portal",
                    onClick = { viewModel.selectRole(AppRoleView.LEADER) },
                    icon = Icons.Outlined.ShowChart,
                    modifier = Modifier.weight(1f)
                )
                PiscesSecondaryButton(
                    text = "Admin Blotter",
                    onClick = { viewModel.selectRole(AppRoleView.ADMIN) },
                    icon = Icons.Outlined.Security,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            PiscesSecondaryButton(
                text = "Developer Chaos Simulator",
                onClick = { viewModel.selectRole(AppRoleView.DEV_TEST_PANEL) },
                icon = Icons.Outlined.BugReport
            )
        }

        item {
            PiscesSecondaryButton(
                text = "Sign Out Session",
                onClick = { viewModel.logout() },
                icon = Icons.Outlined.ExitToApp
            )
        }
    }
}

@Composable
fun BrokerConnectionSection(viewModel: TradingViewModel, brokerAccounts: List<BrokerAccountEntity>) {
    var selectedBroker by remember { mutableStateOf(BrokerCode.ANGEL_ONE) }
    var clientId by remember { mutableStateOf("ANGEL_A8921") }
    var tokenInput by remember { mutableStateOf("jwt_secure_session_token") }
    var apiKeyInput by remember { mutableStateOf("smartapi_key_pisces") }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.US) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Connection Form Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
            shape = RoundedCornerShape(14.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
            modifier = Modifier.fillMaxWidth().testTag("broker_connection_card")
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SECURE BROKER GATEWAY",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = PureWhite
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceSecondary)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AES-256-GCM",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PureWhite
                        )
                    }
                }

                Text(
                    text = "Configure official API endpoints for Indian options execution. All credentials, session JWTs, and refresh tokens are encrypted server-side and never displayed in plain text.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // Broker Selector Chips
                Text(
                    text = "Select Broker Architecture",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = PureWhite
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(BrokerCode.ANGEL_ONE, BrokerCode.UPSTOX, BrokerCode.PAPER_BROKER).forEach { code ->
                        FilterChip(
                            selected = selectedBroker == code,
                            onClick = {
                                selectedBroker = code
                                clientId = when (code) {
                                    BrokerCode.ANGEL_ONE -> "ANGEL_A8921"
                                    BrokerCode.UPSTOX -> "UPSTOX_28109"
                                    else -> "PAPER_PB100"
                                }
                            },
                            label = { Text(code.displayName, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PureWhite,
                                selectedLabelColor = PureBlack,
                                containerColor = SurfaceSecondary,
                                labelColor = PureWhite
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("filter_broker_${code.name}")
                        )
                    }
                }

                // Client ID input
                OutlinedTextField(
                    value = clientId,
                    onValueChange = { clientId = it },
                    label = { Text("Client Code / Account ID") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedContainerColor = SurfaceSecondary,
                        unfocusedContainerColor = SurfaceSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_broker_client_id")
                )

                // API Key / Auth token (Masked Visual Transformation)
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("API Key / Developer App Key") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedContainerColor = SurfaceSecondary,
                        unfocusedContainerColor = SurfaceSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_broker_api_key")
                )

                // Session Token / TOTP
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("Session JWT / TOTP Token") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedContainerColor = SurfaceSecondary,
                        unfocusedContainerColor = SurfaceSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_broker_session_token")
                )

                PiscesPrimaryButton(
                    text = "Connect / Synchronize Broker",
                    onClick = { viewModel.connectBroker(selectedBroker, clientId, tokenInput) },
                    icon = Icons.Outlined.Sync,
                    modifier = Modifier.testTag("btn_connect_broker")
                )
            }
        }

        // Active Broker Connections Header
        Text(
            text = "CONNECTED BROKERS & HEALTH",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = PureWhite
        )

        if (brokerAccounts.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No broker accounts connected yet.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
        }

        brokerAccounts.forEach { acc ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                modifier = Modifier.fillMaxWidth().testTag("broker_account_card_${acc.brokerCode.name}")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = acc.brokerCode.displayName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = PureWhite
                            )
                            Text(
                                text = "Client: ${acc.brokerClientId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        // Connection Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (acc.status == BrokerAccountStatus.CONNECTED) PureWhite else SurfaceSecondary)
                                .border(1.dp, SurfaceBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = acc.status.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (acc.status == BrokerAccountStatus.CONNECTED) PureBlack else PureWhite
                            )
                        }
                    }

                    HorizontalDivider(color = SurfaceBorderSubtle)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Available Margin", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = MoneyFormatter.formatPaise(acc.availableFundsPaise),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = PureWhite
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Last Synchronization", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = dateFormat.format(Date(acc.lastSyncAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = PureWhite
                            )
                        }
                    }

                    // Security & Token Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceSecondary)
                            .border(1.dp, SurfaceBorderSubtle, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("API / Token Status", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Text(
                            text = if (acc.status == BrokerAccountStatus.CONNECTED) "Encrypted & Active" else "Disconnected",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PureWhite
                        )
                    }

                    // Action buttons: Reconnect & Disconnect
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.connectBroker(acc.brokerCode, acc.brokerClientId, "jwt_refreshed_session") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).testTag("btn_reconnect_${acc.brokerCode.name}")
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reconnect", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.disconnectBroker(acc.brokerCode) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).testTag("btn_disconnect_${acc.brokerCode.name}")
                        ) {
                            Icon(Icons.Outlined.LinkOff, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Disconnect", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RiskLimitsSection(viewModel: TradingViewModel, riskLimits: RiskLimitEntity?) {
    var maxLossRupees by remember(riskLimits) { mutableStateOf((riskLimits?.maxDailyLossPaise?.div(100) ?: 15000L).toString()) }
    var maxPositions by remember(riskLimits) { mutableStateOf((riskLimits?.maxOpenPositions ?: 4).toString()) }
    var maxTradeRupees by remember(riskLimits) { mutableStateOf((riskLimits?.maxTradeAmountPaise?.div(100) ?: 50000L).toString()) }
    var maxLots by remember(riskLimits) { mutableStateOf((riskLimits?.maxQuantityLots ?: 10).toString()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Portfolio Risk Guardrails", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
            Text(
                text = "Pre-trade risk criteria enforced by the copy engine before any order hits the broker adapter.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            OutlinedTextField(
                value = maxLossRupees,
                onValueChange = { maxLossRupees = it },
                label = { Text("Max Daily Loss Limit (₹)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PureWhite,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite,
                    focusedContainerColor = SurfaceSecondary,
                    unfocusedContainerColor = SurfaceSecondary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = maxPositions,
                onValueChange = { maxPositions = it },
                label = { Text("Max Concurrent Open Positions") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PureWhite,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite,
                    focusedContainerColor = SurfaceSecondary,
                    unfocusedContainerColor = SurfaceSecondary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = maxTradeRupees,
                onValueChange = { maxTradeRupees = it },
                label = { Text("Max Trade Capital Limit (₹)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PureWhite,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite,
                    focusedContainerColor = SurfaceSecondary,
                    unfocusedContainerColor = SurfaceSecondary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = maxLots,
                onValueChange = { maxLots = it },
                label = { Text("Max Quantity in Lots") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PureWhite,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite,
                    focusedContainerColor = SurfaceSecondary,
                    unfocusedContainerColor = SurfaceSecondary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            PiscesPrimaryButton(
                text = "Save Risk Limits",
                onClick = {
                    val lossPaise = (maxLossRupees.toLongOrNull() ?: 15000L) * 100
                    val pos = maxPositions.toIntOrNull() ?: 4
                    val tradePaise = (maxTradeRupees.toLongOrNull() ?: 50000L) * 100
                    val lots = maxLots.toIntOrNull() ?: 10
                    viewModel.updateRiskLimits(lossPaise, pos, tradePaise, lots)
                }
            )
        }
    }
}

@Composable
fun NotificationsSection(viewModel: TradingViewModel, notifications: List<NotificationEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Audit Notifications", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
            TextButton(onClick = { viewModel.markNotificationsRead() }) {
                Text("Mark Read", color = PureWhite)
            }
        }

        if (notifications.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No alerts recorded.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        } else {
            notifications.forEach { notif ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(notif.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                            Text(notif.type.name, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(notif.message, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerCopySettingsDialog(
    following: StrategyFollowerEntity,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        copyTradingEnabled: Boolean,
        mode: AllocationType,
        fixedQuantityUnits: Int,
        multiplierRatio: Double,
        allocationPct: Double,
        allocationValuePaise: Long,
        maxQuantityUnits: Int,
        maxCapitalPerTradePaise: Long,
        maxDailyLossPaise: Long,
        maxOpenPositions: Int,
        emergencyFreeze: Boolean
    ) -> Unit
) {
    var copyEnabled by remember(following) { mutableStateOf(following.copyTradingEnabled) }
    var selectedMode by remember(following) { mutableStateOf(following.allocationType) }
    var fixedQuantityText by remember(following) { mutableStateOf(following.fixedQuantityUnits.toString()) }
    var ratioText by remember(following) { mutableStateOf(following.multiplierRatio.toString()) }
    var pctText by remember(following) { mutableStateOf(following.allocationPct.toString()) }
    var capitalRupeesText by remember(following) { mutableStateOf((following.allocationValuePaise / 100).toString()) }

    var maxQuantityText by remember(following) { mutableStateOf(following.maxQuantityUnits.toString()) }
    var maxCapitalRupeesText by remember(following) { mutableStateOf((following.maxCapitalPerTradePaise / 100).toString()) }
    var maxDailyLossRupeesText by remember(following) { mutableStateOf((following.maxDailyLossPaise / 100).toString()) }
    var maxPositionsText by remember(following) { mutableStateOf(following.maxOpenPositions.toString()) }
    var emergencyFreeze by remember(following) { mutableStateOf(following.isEmergencyStopped) }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        containerColor = SurfacePrimary,
        titleContentColor = PureWhite,
        textContentColor = PureWhite,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Tune, contentDescription = null, tint = PureWhite, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Customer Copy Settings", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Strategy: ${following.strategyId.replace("strat_", "").replace("_", " ").uppercase()}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = PureWhite
                    )
                    Text(
                        text = "Orders will execute through your connected broker account using your separate credentials and margin.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                // 1. Copy Trading Master Switch (Section 6)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceSecondary)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Copy Trading", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                            Text(if (copyEnabled) "Active: Mirrored orders execute automatically" else "Disabled: No orders will be copied", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Switch(
                            checked = copyEnabled,
                            onCheckedChange = { copyEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureBlack,
                                checkedTrackColor = PureWhite,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SurfacePrimary
                            ),
                            modifier = Modifier.testTag("switch_copy_trading_enabled")
                        )
                    }
                }

                // 2. Quantity Mode (Section 6, 7, 8, 9)
                item {
                    Text("Quantity Mode", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedMode == AllocationType.FIXED_QUANTITY,
                                onClick = { selectedMode = AllocationType.FIXED_QUANTITY },
                                label = { Text("1. Fixed Quantity", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PureWhite, selectedLabelColor = PureBlack, containerColor = SurfaceSecondary, labelColor = PureWhite),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedMode == AllocationType.RATIO,
                                onClick = { selectedMode = AllocationType.RATIO },
                                label = { Text("2. Ratio", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PureWhite, selectedLabelColor = PureBlack, containerColor = SurfaceSecondary, labelColor = PureWhite),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedMode == AllocationType.CAPITAL_PERCENTAGE || selectedMode == AllocationType.PERCENTAGE,
                                onClick = { selectedMode = AllocationType.CAPITAL_PERCENTAGE },
                                label = { Text("3. Capital %", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PureWhite, selectedLabelColor = PureBlack, containerColor = SurfaceSecondary, labelColor = PureWhite),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedMode == AllocationType.FIXED_CAPITAL_AMOUNT || selectedMode == AllocationType.FIXED_AMOUNT,
                                onClick = { selectedMode = AllocationType.FIXED_CAPITAL_AMOUNT },
                                label = { Text("4. Fixed Amount", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PureWhite, selectedLabelColor = PureBlack, containerColor = SurfaceSecondary, labelColor = PureWhite),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Mode-specific configuration field
                item {
                    when (selectedMode) {
                        AllocationType.FIXED_QUANTITY -> {
                            OutlinedTextField(
                                value = fixedQuantityText,
                                onValueChange = { fixedQuantityText = it },
                                label = { Text("Fixed Copy Quantity (e.g. 25 units)") },
                                placeholder = { Text("25") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PureWhite, unfocusedBorderColor = SurfaceBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite, focusedContainerColor = SurfaceSecondary, unfocusedContainerColor = SurfaceSecondary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("input_fixed_quantity")
                            )
                        }
                        AllocationType.RATIO -> {
                            OutlinedTextField(
                                value = ratioText,
                                onValueChange = { ratioText = it },
                                label = { Text("Multiplier Ratio (e.g. 0.50x of leader)") },
                                placeholder = { Text("0.50") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PureWhite, unfocusedBorderColor = SurfaceBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite, focusedContainerColor = SurfaceSecondary, unfocusedContainerColor = SurfaceSecondary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("input_multiplier_ratio")
                            )
                        }
                        AllocationType.CAPITAL_PERCENTAGE, AllocationType.PERCENTAGE -> {
                            OutlinedTextField(
                                value = pctText,
                                onValueChange = { pctText = it },
                                label = { Text("Capital Allocation Percentage (e.g. 20%)") },
                                placeholder = { Text("20") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PureWhite, unfocusedBorderColor = SurfaceBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite, focusedContainerColor = SurfaceSecondary, unfocusedContainerColor = SurfaceSecondary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("input_capital_pct")
                            )
                        }
                        AllocationType.FIXED_CAPITAL_AMOUNT, AllocationType.FIXED_AMOUNT -> {
                            OutlinedTextField(
                                value = capitalRupeesText,
                                onValueChange = { capitalRupeesText = it },
                                label = { Text("Fixed Trade Capital (e.g. ₹50,000)") },
                                placeholder = { Text("50000") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PureWhite, unfocusedBorderColor = SurfaceBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite, focusedContainerColor = SurfaceSecondary, unfocusedContainerColor = SurfaceSecondary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("input_fixed_capital_rupees")
                            )
                        }
                    }
                }

                // 3. Risk Guardrails (Section 6)
                item {
                    Text("Risk Guardrails & Position Limits", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = maxQuantityText,
                            onValueChange = { maxQuantityText = it },
                            label = { Text("Max Qty Units") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PureWhite, unfocusedBorderColor = SurfaceBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite, focusedContainerColor = SurfaceSecondary, unfocusedContainerColor = SurfaceSecondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxCapitalRupeesText,
                            onValueChange = { maxCapitalRupeesText = it },
                            label = { Text("Max Capital (₹)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PureWhite, unfocusedBorderColor = SurfaceBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite, focusedContainerColor = SurfaceSecondary, unfocusedContainerColor = SurfaceSecondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = maxDailyLossRupeesText,
                            onValueChange = { maxDailyLossRupeesText = it },
                            label = { Text("Daily Loss Limit (₹)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PureWhite, unfocusedBorderColor = SurfaceBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite, focusedContainerColor = SurfaceSecondary, unfocusedContainerColor = SurfaceSecondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxPositionsText,
                            onValueChange = { maxPositionsText = it },
                            label = { Text("Max Open Pos") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PureWhite, unfocusedBorderColor = SurfaceBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite, focusedContainerColor = SurfaceSecondary, unfocusedContainerColor = SurfaceSecondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Emergency Copy Freeze (Section 6)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceSecondary)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Emergency Copy Freeze", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                            Text("Halt new mirrored entries immediately", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Switch(
                            checked = emergencyFreeze,
                            onCheckedChange = { emergencyFreeze = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureBlack,
                                checkedTrackColor = PureWhite,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SurfacePrimary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            PiscesPrimaryButton(
                text = "Save Settings",
                onClick = {
                    val fixedQty = fixedQuantityText.toIntOrNull() ?: 25
                    val ratio = ratioText.toDoubleOrNull() ?: 0.50
                    val pct = pctText.toDoubleOrNull() ?: 20.0
                    val capPaise = (capitalRupeesText.toDoubleOrNull() ?: 50000.0 * 100).toLong()

                    val maxQty = maxQuantityText.toIntOrNull() ?: 250
                    val maxCap = (maxCapitalRupeesText.toDoubleOrNull() ?: 50000.0 * 100).toLong()
                    val maxLoss = (maxDailyLossRupeesText.toDoubleOrNull() ?: 15000.0 * 100).toLong()
                    val maxPos = maxPositionsText.toIntOrNull() ?: 4

                    onSave(
                        copyEnabled,
                        selectedMode,
                        fixedQty,
                        ratio,
                        pct,
                        capPaise,
                        maxQty,
                        maxCap,
                        maxLoss,
                        maxPos,
                        emergencyFreeze
                    )
                },
                modifier = Modifier.testTag("btn_save_copy_settings")
            )
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isBusy,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
