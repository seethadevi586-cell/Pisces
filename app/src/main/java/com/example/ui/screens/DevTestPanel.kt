package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.ui.components.PiscesPrimaryButton
import com.example.ui.components.PiscesSecondaryButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppRoleView
import com.example.ui.viewmodel.TradingViewModel

@Composable
fun DevTestPanelScreen(viewModel: TradingViewModel) {
    val systemConfig by viewModel.systemConfig.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()

    var selectedMode by remember(systemConfig) {
        mutableStateOf(systemConfig?.simulatedFailureMode ?: SimulationFailureMode.NONE)
    }
    var selectedLatency by remember(systemConfig) {
        mutableStateOf(systemConfig?.simulatedLatencyMs ?: 150L)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Persistent Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfacePrimary)
                    .border(1.dp, PureWhite, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PAPER TRADING • NO REAL MONEY",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = PureWhite
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.BugReport,
                            contentDescription = null,
                            tint = PureWhite
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DEVELOPER CHAOS & FAULT INJECTION",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = PureWhite
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Paper Broker Fault Simulation",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Simulate broker API timeouts, exchange circuit rejections, partial fills, and session drops to verify copy engine resilience without risking capital.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "1. Simulated Broker Failure Mode",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PureWhite
                    )

                    SimulationFailureMode.values().forEach { mode ->
                        val isSelected = selectedMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SurfaceSecondary else Color.Transparent)
                                .border(if (isSelected) 1.dp else 0.dp, if (isSelected) SurfaceBorder else Color.Transparent, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedMode = mode
                                    viewModel.setSimulationControls(mode, selectedLatency)
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PureWhite,
                                    unselectedColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("radio_mode_${mode.name}")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = mode.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PureWhite
                                )
                                Text(
                                    text = when (mode) {
                                        SimulationFailureMode.NONE -> "Normal execution: orders match and fill instantly"
                                        SimulationFailureMode.TIMEOUT -> "Simulates 504 socket timeout at broker"
                                        SimulationFailureMode.REJECTION -> "Simulates exchange circuit limit RMS rejection"
                                        SimulationFailureMode.PARTIAL_FILL -> "Simulates 50% partial liquidity match"
                                        SimulationFailureMode.BROKER_DISCONNECT -> "Simulates session token invalidation"
                                        SimulationFailureMode.INSUFFICIENT_MARGIN -> "Simulates margin check failure at broker"
                                        SimulationFailureMode.MARKET_CLOSED -> "Simulates order rejected outside market trading hours"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "2. Simulated Latency: ${selectedLatency}ms",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PureWhite
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(50L, 150L, 500L, 1500L).forEach { lat ->
                            FilterChip(
                                selected = selectedLatency == lat,
                                onClick = {
                                    selectedLatency = lat
                                    viewModel.setSimulationControls(selectedMode, lat)
                                },
                                label = { Text("${lat}ms") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PureWhite,
                                    selectedLabelColor = PureBlack,
                                    containerColor = SurfaceSecondary,
                                    labelColor = PureWhite
                                )
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "3. Quick Test Signal Injections",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PureWhite
                    )
                    Text(
                        text = "Fire immediate test options signals across all copiers under current failure settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    PiscesPrimaryButton(
                        text = if (isBusy) "Executing Fan-Out..." else "Fire BUY Signal (NIFTY 25000 CE)",
                        onClick = {
                            viewModel.broadcastSignal(
                                strategyId = "strat_nifty_momentum",
                                symbol = "NIFTY 25000 CE",
                                underlying = UnderlyingIndex.NIFTY,
                                optionType = OptionType.CE,
                                strikePaise = 2500000L,
                                expiry = "2026-10-29",
                                side = OrderSide.BUY,
                                quantityLots = 2,
                                orderType = OrderType.MARKET,
                                limitPricePaise = 14500L
                            )
                        },
                        enabled = !isBusy,
                        icon = Icons.Outlined.PlayCircle,
                        modifier = Modifier.testTag("fire_quick_test_signal_button")
                    )

                    PiscesSecondaryButton(
                        text = "Fire SELL Signal (NIFTY 24800 PE)",
                        onClick = {
                            viewModel.broadcastSignal(
                                strategyId = "strat_nifty_momentum",
                                symbol = "NIFTY 24800 PE",
                                underlying = UnderlyingIndex.NIFTY,
                                optionType = OptionType.PE,
                                strikePaise = 2480000L,
                                expiry = "2026-10-29",
                                side = OrderSide.SELL,
                                quantityLots = 1,
                                orderType = OrderType.MARKET,
                                limitPricePaise = 9800L
                            )
                        },
                        enabled = !isBusy,
                        icon = Icons.Outlined.PlayCircle
                    )
                }
            }
        }

        // Section 33 Automated CUJ Verification Card
        item {
            val cujResult by viewModel.cujTestResult.collectAsState()

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfacePrimary),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SurfaceBorder)),
                modifier = Modifier.fillMaxWidth().testTag("cuj_test_section_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "4. SECTION 33 CUJ TEST SUITE",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = PureWhite
                            )
                            Text(
                                text = "Automated test: Leader trade -> Customers A, B, C, D -> Duplicate protection -> Leader exit",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    PiscesPrimaryButton(
                        text = if (isBusy) "Running Automated Test Suite..." else "Run Section 33 Automated CUJ Test",
                        onClick = { viewModel.runSection33Test() },
                        enabled = !isBusy,
                        icon = Icons.Outlined.FactCheck,
                        modifier = Modifier.testTag("run_section_33_test_button")
                    )

                    if (cujResult != null) {
                        val res = cujResult!!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceSecondary)
                                .border(1.dp, if (res.isAllPassed) PureWhite else Color.Red, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (res.isAllPassed) "TEST STATUS: 100% PASSED" else "TEST STATUS: FAILED",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (res.isAllPassed) PureWhite else Color.Red
                                    )
                                    Text(
                                        text = "Section 33 Spec",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }

                                HorizontalDivider(color = SurfaceBorderSubtle)

                                Text(
                                    text = res.summaryLog,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                    color = PureWhite
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            PiscesSecondaryButton(
                text = "Back to Customer Portfolio",
                onClick = { viewModel.selectRole(AppRoleView.CUSTOMER) },
                icon = Icons.Outlined.Person
            )
        }
    }
}
