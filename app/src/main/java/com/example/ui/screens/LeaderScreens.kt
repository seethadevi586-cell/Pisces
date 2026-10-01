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
import com.example.data.local.entity.LeaderTradeEventEntity
import com.example.data.local.entity.PositionEntity
import com.example.domain.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppRoleView
import com.example.ui.viewmodel.TradingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LeaderScreen(viewModel: TradingViewModel) {
    val selectedTab by viewModel.leaderTab.collectAsState()
    val tabTitles = listOf("Overview", "Signal Studio", "Copiers", "Strategies")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
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
                    onClick = { viewModel.setLeaderTab(index) },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) PureWhite else TextSecondary
                        )
                    },
                    modifier = Modifier.testTag("leader_tab_$index")
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> LeaderOverviewTab(viewModel)
                1 -> GenerateSignalStudioTab(viewModel)
                2 -> LeaderFollowersTab(viewModel)
                3 -> LeaderStrategiesTab(viewModel)
            }
        }
    }
}

@Composable
fun LeaderOverviewTab(viewModel: TradingViewModel) {
    val strategies by viewModel.leaderStrategies.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()
    val leaderTrades by viewModel.leaderTrades.collectAsState()
    val leaderBrokerAccounts by viewModel.leaderBrokerAccounts.collectAsState()
    val allPositions by viewModel.allPositions.collectAsState()
    val lastReport by viewModel.lastExecutionReport.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showArchitectureGuide by remember { mutableStateOf(false) }

    // Source broker trade execution form state
    var selectedBrokerCode by remember { mutableStateOf(BrokerCode.ANGEL_ONE) }
    var selectedStrategyId by remember(strategies) { mutableStateOf(strategies.firstOrNull()?.id ?: "strat_nifty_momentum") }
    var tradeSide by remember { mutableStateOf(OrderSide.BUY) }
    var tradeSymbol by remember { mutableStateOf("NIFTY 25000 CE") }
    var tradeQuantityText by remember { mutableStateOf("100") }
    var tradePriceText by remember { mutableStateOf("145.00") }

    val totalFollowers = strategies.sumOf { it.activeFollowersCount }
    val executedOrders = allOrders.filter { it.status == OrderStatus.FILLED }
    val leaderAccount = leaderBrokerAccounts.firstOrNull()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Leader Account & Connected Broker Card (Section 1)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                modifier = Modifier.fillMaxWidth().testTag("leader_portal_header_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SOURCE ACCOUNT • STRATEGY PROVIDER",
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
                                text = "LEADER ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = PureWhite
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vikram Singhania (Quant Alpha)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Connected Leader Broker Gateway details
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceSecondary)
                            .border(1.dp, SurfaceBorderSubtle, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.AccountBalance, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = leaderAccount?.brokerCode?.displayName ?: "Angel One SmartAPI",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = PureWhite
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(PureWhite)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LIVE CONNECTED",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                        color = PureBlack
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Client Code: ${leaderAccount?.brokerClientId ?: "ANGEL_LEADER_99"}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("Available Margin: ${MoneyFormatter.formatPaise(leaderAccount?.availableFundsPaise ?: 124500000L)}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Registered Static Egress IP: 13.235.12.88", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp), color = TextSecondary)
                                Text("WebSocket Stream: OK", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = PureWhite)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricStatCard(
                            title = "Active Copiers",
                            value = "$totalFollowers",
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "Filled Orders",
                            value = "${executedOrders.size}",
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "Strategies",
                            value = "${strategies.size}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Action Guides: "How to Add My Strategy" & "How My Trades Get Copied"
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                modifier = Modifier.fillMaxWidth().testTag("strategy_and_copy_guide_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LEADER WORKFLOW & STRATEGY LAUNCH",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = TextSecondary
                            )
                            Text(
                                text = "How to add strategies & mirror trades to customers",
                                style = MaterialTheme.typography.bodySmall,
                                color = PureWhite
                            )
                        }
                    }

                    // How I Can Add My Strategy summary
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceSecondary)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.AddCircleOutline, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("HOW I CAN ADD MY STRATEGY?", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                            }
                            Text(
                                text = "1. Click '+ Launch Strategy' below\n2. Configure strategy name, underlying index (NIFTY/BANKNIFTY), target win rate, minimum capital, and monthly fee\n3. Deploy: Your strategy is immediately listed in the Marketplace where followers can subscribe and mirror your trades.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // How My Trade Will Get Copied by Customer summary
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceSecondary)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Hub, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("HOW MY TRADE GETS COPIED BY CUSTOMERS?", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                                }
                                TextButton(
                                    onClick = { showArchitectureGuide = !showArchitectureGuide },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(if (showArchitectureGuide) "Hide Architecture" else "View Pipeline", fontSize = 11.sp, color = PureWhite)
                                }
                            }
                            Text(
                                text = "• You trade in YOUR own connected broker account (Angel One / Upstox).\n• PISCES Trade Detector captures the executed fill (never unfilled orders).\n• Normalizes event -> Finds subscribers -> Runs customer risk checks.\n• Calculates customer quantity (Fixed Qty, Ratio, % Capital).\n• Submits order using that CUSTOMER'S OWN connected broker credentials.\n• Customer money remains completely separate in their own broker account.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            if (showArchitectureGuide) {
                                HorizontalDivider(color = SurfaceBorderSubtle, modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = "CORE PIPELINE ARCHITECTURE:\n" +
                                           "LEADER BROKER (Angel One/Upstox)\n" +
                                           "   ↓ (Leader places trade)\n" +
                                           "PISCES TRADE DETECTOR\n" +
                                           "   ↓ (Normalized LeaderTradeEvent)\n" +
                                           "SIGNAL NORMALIZER & RISK ENGINE\n" +
                                           "   ↓ (Customer settings: Fixed Qty / Ratio / Capital %)\n" +
                                           "COPY-TRADING ENGINE\n" +
                                           "   ├── Customer A Broker (Angel One Adapter)\n" +
                                           "   └── Customer B Broker (Upstox Adapter)\n" +
                                           "• Duplicate Protection: unique (leader_trade_id + customer_id)\n" +
                                           "• Exit Mirroring: Exits match customer's actual copied position.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                    color = PureWhite
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PiscesPrimaryButton(
                            text = "+ Launch New Strategy",
                            onClick = { showCreateDialog = true },
                            icon = Icons.Outlined.Add,
                            modifier = Modifier.weight(1f).testTag("btn_leader_launch_strategy_overview")
                        )
                        PiscesSecondaryButton(
                            text = "Signal Studio",
                            onClick = { viewModel.setLeaderTab(1) },
                            icon = Icons.Outlined.Bolt,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Interactive Source Account Trade Tester (Sections 2, 4, 10, 14)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                modifier = Modifier.fillMaxWidth().testTag("source_account_trade_tester_card")
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRADE IN LEADER BROKER (SOURCE ACCOUNT)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
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
                                text = "TRADE DETECTOR ON",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = PureWhite
                            )
                        }
                    }

                    Text(
                        text = "Place a trade directly into your leader broker account. PISCES Trade Detector will detect the execution and automatically fan out customer copy orders through each follower's own broker connection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // Broker Selector
                    Text("1. Leader Connected Broker", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(BrokerCode.ANGEL_ONE, BrokerCode.UPSTOX).forEach { broker ->
                            FilterChip(
                                selected = selectedBrokerCode == broker,
                                onClick = { selectedBrokerCode = broker },
                                label = { Text(broker.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PureWhite,
                                    selectedLabelColor = PureBlack,
                                    containerColor = SurfaceSecondary,
                                    labelColor = PureWhite
                                )
                            )
                        }
                    }

                    // Side & Contract
                    Text("2. Action & Order Parameters", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = tradeSide == OrderSide.BUY,
                            onClick = { tradeSide = OrderSide.BUY },
                            label = { Text("BUY (ENTRY)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PureWhite,
                                selectedLabelColor = PureBlack,
                                containerColor = SurfaceSecondary,
                                labelColor = PureWhite
                            )
                        )
                        FilterChip(
                            selected = tradeSide == OrderSide.SELL,
                            onClick = { tradeSide = OrderSide.SELL },
                            label = { Text("SELL (EXIT)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PureWhite,
                                selectedLabelColor = PureBlack,
                                containerColor = SurfaceSecondary,
                                labelColor = PureWhite
                            )
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = tradeSymbol,
                            onValueChange = { tradeSymbol = it },
                            label = { Text("Instrument / Contract") },
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
                            modifier = Modifier.weight(1.5f).testTag("input_leader_trade_symbol")
                        )

                        OutlinedTextField(
                            value = tradeQuantityText,
                            onValueChange = { tradeQuantityText = it },
                            label = { Text("Quantity (Units)") },
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
                            modifier = Modifier.weight(1f).testTag("input_leader_trade_qty")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = tradePriceText,
                            onValueChange = { tradePriceText = it },
                            label = { Text("Execution Price (₹)") },
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
                            modifier = Modifier.weight(1f)
                        )

                        Box(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                            Text(
                                text = "Contract lot size: 25\nOrder Type: MARKET\nIdempotency: Active",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = TextSecondary
                            )
                        }
                    }

                    PiscesPrimaryButton(
                        text = if (isBusy) "Executing in Broker & Mirroring..." else "Execute in My Broker & Mirror to Followers",
                        onClick = {
                            val qty = tradeQuantityText.toIntOrNull() ?: 100
                            val pricePaise = (tradePriceText.toDoubleOrNull() ?: 145.0 * 100).toLong()

                            viewModel.executeLeaderTrade(
                                strategyId = selectedStrategyId,
                                brokerCode = selectedBrokerCode,
                                symbol = tradeSymbol.trim(),
                                side = tradeSide,
                                quantity = qty,
                                orderType = OrderType.MARKET,
                                pricePaise = pricePaise
                            )
                        },
                        enabled = !isBusy,
                        icon = Icons.Outlined.PlayArrow,
                        modifier = Modifier.testTag("btn_execute_leader_direct_trade")
                    )

                    // Live Fan-Out Execution Report Display
                    if (lastReport != null) {
                        val report = lastReport!!
                        HorizontalDivider(color = SurfaceBorderSubtle)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LIVE FAN-OUT EXECUTION REPORT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PureWhite
                                )
                                Text(
                                    text = "${report.successfulExecutions} filled / ${report.totalFollowersTargeted} copiers",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PureWhite
                                )
                            }

                            report.executionDetails.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceSecondary)
                                        .border(1.dp, SurfaceBorderSubtle, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${item.followerId.replace("cust_", "Customer ").replace("user_", "User ")} (${item.brokerCode.displayName})",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = PureWhite
                                            )
                                            Text(
                                                text = item.message,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = if (item.executedQuantity > 0) "${item.executedQuantity} units" else "NO ORDER",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = PureWhite
                                            )
                                            Text(
                                                text = item.status.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (item.status == OrderStatus.FILLED) PureWhite else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Leader Open Positions Blotter (Section 1)
        val openPositions = allPositions.filter { it.status == PositionStatus.OPEN }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MY BROKER OPEN POSITIONS (${openPositions.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = PureWhite
                )
            }
        }

        if (openPositions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No active open positions in leader broker account.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        } else {
            items(items = openPositions, key = { "leader_pos_${it.id}" }) { pos ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(pos.symbol, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                            Text("Qty: ${pos.quantity} units • Buy: ${MoneyFormatter.formatPaise(pos.averageBuyPricePaise)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.executeLeaderTrade(
                                    strategyId = pos.strategyId,
                                    brokerCode = leaderAccount?.brokerCode ?: BrokerCode.ANGEL_ONE,
                                    symbol = pos.symbol,
                                    side = OrderSide.SELL,
                                    quantity = pos.quantity,
                                    orderType = OrderType.MARKET,
                                    pricePaise = pos.ltpPaise
                                )
                            },
                            enabled = !isBusy,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Exit Position", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 5. Leader Executed Trades Blotter (Section 1 & 2)
        item {
            Text(
                text = "RECENT LEADER TRADES DETECTED (${leaderTrades.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = PureWhite
            )
        }

        if (leaderTrades.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Trades placed in your broker will appear here as Trade Detector captures fills.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        } else {
            items(items = leaderTrades.take(5), key = { "leader_trade_${it.eventId}" }) { trade ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OptionTypeBadge(OptionType.CE, trade.transactionType)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(trade.symbol, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PureWhite)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(trade.status.name, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = PureBlack)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Filled: ${trade.filledQuantity} units @ ${MoneyFormatter.formatPaise(trade.averagePricePaise)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("Broker: ${trade.broker.displayName}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Order ID: ${trade.brokerOrderId}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp), color = TextSecondary)
                            Text(timeFormat.format(Date(trade.tradeTime)), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // 6. Managed Strategies
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MANAGED STRATEGIES (${strategies.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = PureWhite
                )
                TextButton(onClick = { viewModel.setLeaderTab(3) }) {
                    Text("Manage All", color = PureWhite, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        items(items = strategies, key = { "managed_strat_${it.id}" }) { strat ->
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
                            text = strat.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = PureWhite
                        )
                        Text(
                            text = "+${strat.totalReturnPct}%",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = PureWhite
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Underlying: ${strat.underlying.displayName} • ${strat.activeFollowersCount} Followers • Fee: ${MoneyFormatter.formatPaise(strat.monthlySubscriptionFeePaise)}/mo",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
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

    if (showCreateDialog) {
        CreateStrategyDialog(
            isBusy = isBusy,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, underlying, minCapital, fee, winRate, returnPct ->
                viewModel.createStrategy(
                    name = name,
                    description = desc,
                    underlying = underlying,
                    minCapitalRupees = minCapital,
                    monthlyFeeRupees = fee,
                    targetWinRatePct = winRate,
                    expectedReturnPct = returnPct
                )
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun GenerateSignalStudioTab(viewModel: TradingViewModel) {
    val strategies by viewModel.leaderStrategies.collectAsState()
    val lastReport by viewModel.lastExecutionReport.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()

    var selectedStrategyId by remember(strategies) { mutableStateOf(strategies.firstOrNull()?.id ?: "strat_nifty_momentum") }
    var underlying by remember { mutableStateOf(UnderlyingIndex.NIFTY) }
    var optionType by remember { mutableStateOf(OptionType.CE) }
    var side by remember { mutableStateOf(OrderSide.BUY) }
    var strikeRupees by remember { mutableStateOf("25000") }
    var expiryDate by remember { mutableStateOf("2026-10-29") }
    var quantityLotsText by remember { mutableStateOf("2") }
    var orderType by remember { mutableStateOf(OrderType.MARKET) }
    var limitPriceRupees by remember { mutableStateOf("145.00") }

    val lots = quantityLotsText.toIntOrNull() ?: 1
    val totalUnits = lots * underlying.lotSize
    val symbol = "${underlying.name} $strikeRupees ${optionType.name}"

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
                    text = "OPTIONS SIGNAL STUDIO",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Broadcast signals to all active followers. Every trade passes pre-trade risk validation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("1. Underlying Index", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UnderlyingIndex.values().forEach { idx ->
                            FilterChip(
                                selected = underlying == idx,
                                onClick = { underlying = idx },
                                label = { Text(idx.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PureWhite,
                                    selectedLabelColor = PureBlack,
                                    containerColor = SurfaceSecondary,
                                    labelColor = PureWhite
                                )
                            )
                        }
                    }

                    Text("2. Action & Option Contract", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = side == OrderSide.BUY,
                            onClick = { side = OrderSide.BUY },
                            label = { Text("BUY") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PureWhite,
                                selectedLabelColor = PureBlack,
                                containerColor = SurfaceSecondary,
                                labelColor = PureWhite
                            )
                        )
                        FilterChip(
                            selected = side == OrderSide.SELL,
                            onClick = { side = OrderSide.SELL },
                            label = { Text("SELL") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PureWhite,
                                selectedLabelColor = PureBlack,
                                containerColor = SurfaceSecondary,
                                labelColor = PureWhite
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        FilterChip(
                            selected = optionType == OptionType.CE,
                            onClick = { optionType = OptionType.CE },
                            label = { Text("CALL (CE)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PureWhite,
                                selectedLabelColor = PureBlack,
                                containerColor = SurfaceSecondary,
                                labelColor = PureWhite
                            )
                        )
                        FilterChip(
                            selected = optionType == OptionType.PE,
                            onClick = { optionType = OptionType.PE },
                            label = { Text("PUT (PE)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PureWhite,
                                selectedLabelColor = PureBlack,
                                containerColor = SurfaceSecondary,
                                labelColor = PureWhite
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = strikeRupees,
                            onValueChange = { strikeRupees = it },
                            label = { Text("Strike Price") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PureWhite,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                focusedContainerColor = SurfaceSecondary,
                                unfocusedContainerColor = SurfaceSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = expiryDate,
                            onValueChange = { expiryDate = it },
                            label = { Text("Expiry Date") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PureWhite,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                focusedContainerColor = SurfaceSecondary,
                                unfocusedContainerColor = SurfaceSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = quantityLotsText,
                            onValueChange = { quantityLotsText = it },
                            label = { Text("Quantity (Lots)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PureWhite,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                focusedContainerColor = SurfaceSecondary,
                                unfocusedContainerColor = SurfaceSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = limitPriceRupees,
                            onValueChange = { limitPriceRupees = it },
                            label = { Text("Est. Price (₹)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PureWhite,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite,
                                focusedContainerColor = SurfaceSecondary,
                                unfocusedContainerColor = SurfaceSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceSecondary)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Contract: $symbol | Total Units: $totalUnits ($lots lots x ${underlying.lotSize})",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = PureWhite
                        )
                    }

                    PiscesPrimaryButton(
                        text = if (isBusy) "Broadcasting Signal..." else "Broadcast Signal to Followers",
                        onClick = {
                            val strikePaise = (strikeRupees.toDoubleOrNull() ?: 25000.0 * 100).toLong()
                            val limitPaise = (limitPriceRupees.toDoubleOrNull() ?: 145.0 * 100).toLong()

                            viewModel.broadcastSignal(
                                strategyId = selectedStrategyId,
                                symbol = symbol,
                                underlying = underlying,
                                optionType = optionType,
                                strikePaise = strikePaise,
                                expiry = expiryDate,
                                side = side,
                                quantityLots = lots,
                                orderType = orderType,
                                limitPricePaise = limitPaise
                            )
                        },
                        enabled = !isBusy,
                        icon = Icons.Outlined.Send,
                        modifier = Modifier.testTag("broadcast_signal_button")
                    )
                }
            }
        }

        // Fan-out Execution Report
        if (lastReport != null) {
            val report = lastReport!!
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "FAN-OUT EXECUTION REPORT",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Targeted: ${report.totalFollowersTargeted} Copiers",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PureWhite
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricStatCard(
                                title = "Filled",
                                value = "${report.successfulExecutions}",
                                isPositive = true,
                                modifier = Modifier.weight(1f)
                            )
                            MetricStatCard(
                                title = "Rejected",
                                value = "${report.rejectedExecutions}",
                                isPositive = false,
                                modifier = Modifier.weight(1f)
                            )
                            MetricStatCard(
                                title = "Failed",
                                value = "${report.failedExecutions}",
                                isPositive = false,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Follower Execution Details:", style = MaterialTheme.typography.labelMedium, color = PureWhite)
                        Spacer(modifier = Modifier.height(6.dp))

                        report.executionDetails.forEach { detail ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = detail.followerId.replace("user_follower_", "Follower #"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                OrderStatusBadge(detail.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderFollowersTab(viewModel: TradingViewModel) {
    val allUsers by viewModel.allUsers.collectAsState()
    val followers = allUsers.filter { it.role == UserRole.CUSTOMER }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "ACTIVE COPIERS (${followers.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = PureWhite
            )
        }

        items(items = followers, key = { it.id }) { f ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(f.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                        Text("KYC PAN: ${f.panMasked} • Status: ${f.status.name}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceSecondary)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text("CONNECTED", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderStrategiesTab(viewModel: TradingViewModel) {
    val strategies by viewModel.leaderStrategies.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

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
                Column {
                    Text(
                        text = "STRATEGY MANAGEMENT",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = PureWhite
                    )
                    Text(
                        text = "Create & deploy quantitative strategies for trade mirroring",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                PiscesPrimaryButton(
                    text = "+ New Strategy",
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.widthIn(max = 150.dp).testTag("btn_open_create_strategy")
                )
            }
        }

        if (strategies.isEmpty()) {
            item {
                EmptyStateView(
                    title = "No Strategies Created",
                    message = "Build your first systematic option trading strategy to start broadcasting signals to copiers.",
                    actionText = "+ Add Strategy",
                    onAction = { showCreateDialog = true }
                )
            }
        } else {
            items(items = strategies, key = { it.id }) { strat ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                    modifier = Modifier.fillMaxWidth().testTag("strategy_item_${strat.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(strat.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                                Text("Index: ${strat.underlying.displayName}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceSecondary)
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = strat.status.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = PureWhite
                                )
                            }
                        }

                        Text(strat.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                        HorizontalDivider(color = SurfaceBorderSubtle)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Min Capital", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(MoneyFormatter.formatPaise(strat.minCapitalPaise), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                            }
                            Column {
                                Text("Win Rate", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("${strat.winRatePct}%", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                            }
                            Column {
                                Text("Monthly Fee", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(MoneyFormatter.formatPaise(strat.monthlySubscriptionFeePaise), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Copiers", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("${strat.activeFollowersCount}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = PureWhite)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateStrategyDialog(
            isBusy = isBusy,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, underlying, minCapital, fee, winRate, returnPct ->
                viewModel.createStrategy(
                    name = name,
                    description = desc,
                    underlying = underlying,
                    minCapitalRupees = minCapital,
                    monthlyFeeRupees = fee,
                    targetWinRatePct = winRate,
                    expectedReturnPct = returnPct
                )
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun CreateStrategyDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String, underlying: UnderlyingIndex, minCapital: Double, fee: Double, winRate: Double, returnPct: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var underlying by remember { mutableStateOf(UnderlyingIndex.NIFTY) }
    var minCapitalRupees by remember { mutableStateOf("100000") }
    var monthlyFeeRupees by remember { mutableStateOf("2499") }
    var targetWinRate by remember { mutableStateOf("70.0") }
    var expectedReturn by remember { mutableStateOf("45.0") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        containerColor = SurfacePrimary,
        titleContentColor = PureWhite,
        textContentColor = PureWhite,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AddChart, contentDescription = null, tint = PureWhite, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Launch New Strategy", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Configure your trading model. Once launched, followers can subscribe and mirror signals via their connected brokers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Strategy Name") },
                    placeholder = { Text("e.g. Nifty Gamma Scalper") },
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
                    modifier = Modifier.fillMaxWidth().testTag("input_new_strategy_name")
                )

                Text("Underlying Market", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    UnderlyingIndex.values().forEach { idx ->
                        FilterChip(
                            selected = underlying == idx,
                            onClick = { underlying = idx },
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

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Strategy Description & Logic") },
                    placeholder = { Text("e.g. Rule-based momentum scalping on weekly options with 1:2 risk-reward") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedContainerColor = SurfaceSecondary,
                        unfocusedContainerColor = SurfaceSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_new_strategy_desc")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minCapitalRupees,
                        onValueChange = { minCapitalRupees = it },
                        label = { Text("Min Capital (₹)") },
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
                        modifier = Modifier.weight(1f).testTag("input_new_strategy_capital")
                    )

                    OutlinedTextField(
                        value = monthlyFeeRupees,
                        onValueChange = { monthlyFeeRupees = it },
                        label = { Text("Monthly Fee (₹)") },
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
                        modifier = Modifier.weight(1f).testTag("input_new_strategy_fee")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetWinRate,
                        onValueChange = { targetWinRate = it },
                        label = { Text("Target Win Rate %") },
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
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = expectedReturn,
                        onValueChange = { expectedReturn = it },
                        label = { Text("Expected Return %") },
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
                        modifier = Modifier.weight(1f)
                    )
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, color = PureWhite, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            PiscesPrimaryButton(
                text = if (isBusy) "Deploying..." else "Launch Strategy",
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Strategy name cannot be empty"
                        return@PiscesPrimaryButton
                    }
                    val cap = minCapitalRupees.toDoubleOrNull() ?: 100000.0
                    val fee = monthlyFeeRupees.toDoubleOrNull() ?: 0.0
                    val win = targetWinRate.toDoubleOrNull() ?: 68.0
                    val ret = expectedReturn.toDoubleOrNull() ?: 42.0

                    onCreate(
                        name,
                        description.ifBlank { "Systematic Indian index options strategy with pre-trade risk controls." },
                        underlying,
                        cap,
                        fee,
                        win,
                        ret
                    )
                },
                loading = isBusy,
                modifier = Modifier.testTag("btn_submit_create_strategy")
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

