package com.example.engine

import com.example.broker.BrokerAdapter
import com.example.broker.BrokerConnectionService
import com.example.broker.model.*
import com.example.data.local.dao.TradingDao
import com.example.data.local.entity.*
import com.example.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class SignalExecutionReport(
    val signalId: String,
    val leaderTradeId: String? = null,
    val totalFollowersTargeted: Int,
    val successfulExecutions: Int,
    val rejectedExecutions: Int,
    val failedExecutions: Int,
    val executionDetails: List<CopyExecutionSummary>
)

data class CopyExecutionSummary(
    val followerId: String,
    val orderId: String,
    val brokerCode: BrokerCode,
    val status: OrderStatus,
    val executedQuantity: Int,
    val averagePricePaise: Long,
    val message: String
)

class CopyEngine(
    private val dao: TradingDao,
    private val brokerConnectionService: BrokerConnectionService? = null,
    private val fallbackBrokerAdapter: BrokerAdapter? = null,
    private val riskEngine: RiskEngine = RiskEngine()
) {

    /**
     * Core PISCES workflow: Detects and copies confirmed Leader trades across individual customer accounts.
     * Each customer order executes through that customer's OWN broker adapter and credentials.
     */
    suspend fun processLeaderTrade(
        leaderTrade: LeaderTradeEventEntity,
        correlationId: String = UUID.randomUUID().toString()
    ): SignalExecutionReport = withContext(Dispatchers.IO) {
        // Record leader trade event
        dao.insertLeaderTrade(leaderTrade)

        // 1. Only EXECUTED/FILLED quantities are processed for copy execution
        if (leaderTrade.status != OrderStatus.FILLED && leaderTrade.status != OrderStatus.PARTIALLY_FILLED) {
            dao.insertAuditLog(
                AuditLogEntity(
                    correlationId = correlationId,
                    userId = leaderTrade.leaderId,
                    action = "LEADER_ORDER_UNFILLED",
                    entityType = "LeaderTrade",
                    entityId = leaderTrade.eventId,
                    detailsJson = """{"status":"${leaderTrade.status}","reason":"Only filled trades trigger copy execution"}"""
                )
            )
            return@withContext SignalExecutionReport(
                signalId = "unfilled",
                leaderTradeId = leaderTrade.eventId,
                totalFollowersTargeted = 0,
                successfulExecutions = 0,
                rejectedExecutions = 0,
                failedExecutions = 0,
                executionDetails = emptyList()
            )
        }

        // 2. Normalize Leader Trade into PISCES TradeSignal
        val signal = SignalNormalizer.normalizeLeaderTrade(leaderTrade)
        dao.insertTradeSignal(signal)

        // 3. Handle EXIT vs ENTRY
        if (leaderTrade.transactionType == OrderSide.SELL) {
            // Leader is closing / exiting position -> execute proportional customer exits
            return@withContext processLeaderExit(leaderTrade, signal, correlationId)
        } else {
            // Leader is opening / increasing position -> fan-out customer entry copy orders
            return@withContext processLeaderEntry(leaderTrade, signal, correlationId)
        }
    }

    /**
     * Entry Fan-Out: Calculates customer-specific quantities and submits orders via customer's OWN broker.
     */
    private suspend fun processLeaderEntry(
        leaderTrade: LeaderTradeEventEntity,
        signal: TradeSignalEntity,
        correlationId: String
    ): SignalExecutionReport {
        val strategy = dao.getStrategyById(leaderTrade.strategyId)
        val systemConfig = dao.getSystemConfig() ?: SystemConfigEntity()
        val isGlobalKillSwitchActive = systemConfig.isGlobalKillSwitchActive

        val activeFollowers = dao.getActiveFollowersForStrategy(leaderTrade.strategyId)
        val summaries = mutableListOf<CopyExecutionSummary>()
        var successCount = 0
        var rejectCount = 0
        var failCount = 0

        val estimatedPricePaise = if (leaderTrade.averagePricePaise > 0L) leaderTrade.averagePricePaise else 14500L

        for (follower in activeFollowers) {
            // Check if customer explicitly turned OFF copy trading (Section 6 & 14)
            if (!follower.copyTradingEnabled) {
                summaries.add(
                    CopyExecutionSummary(
                        followerId = follower.followerId,
                        orderId = "NO_ORDER",
                        brokerCode = BrokerCode.PAPER_BROKER,
                        status = OrderStatus.CANCELLED,
                        executedQuantity = 0,
                        averagePricePaise = 0L,
                        message = "Copy trading disabled by customer"
                    )
                )
                continue
            }

            val user = dao.getUserById(follower.followerId)
            val brokerAccount = dao.getBrokerAccountById(follower.brokerAccountId)
            val riskLimits = dao.getRiskLimits(follower.followerId)
            val openPosition = dao.getOpenPosition(follower.followerId, signal.instrument)
            val customerBroker = brokerAccount?.brokerCode ?: BrokerCode.PAPER_BROKER

            // Idempotency Key check: leader_trade_id + follower_id + v1 (Duplicate Protection)
            val idempotencyKey = "lt_${leaderTrade.eventId}_cust_${follower.followerId}_v1"
            val existingOrder = dao.getOrderByIdempotencyKey(idempotencyKey)
            if (existingOrder != null) {
                summaries.add(
                    CopyExecutionSummary(
                        followerId = follower.followerId,
                        orderId = existingOrder.id,
                        brokerCode = customerBroker,
                        status = existingOrder.status,
                        executedQuantity = existingOrder.executedQuantity,
                        averagePricePaise = existingOrder.averagePricePaise,
                        message = "Duplicate trade blocked by idempotency engine"
                    )
                )
                continue
            }

            // 1. Calculate Customer-Specific Quantity
            val allocation = AllocationCalculator.calculateCustomerQuantity(
                allocationType = follower.allocationType,
                leaderQuantity = leaderTrade.filledQuantity,
                fixedQuantityUnits = follower.fixedQuantityUnits,
                multiplierRatio = follower.multiplierRatio,
                allocationValuePaise = follower.allocationValuePaise,
                allocationPct = follower.allocationPct,
                availableMarginPaise = brokerAccount?.availableFundsPaise ?: 0L,
                estimatedPricePaise = estimatedPricePaise,
                underlying = signal.underlying,
                maxMultiplier = follower.maxMultiplier,
                maxQuantityUnitsAllowed = follower.maxQuantityUnits,
                maxCapitalPerTradePaise = follower.maxCapitalPerTradePaise
            )

            val proposedQuantity = allocation.calculatedQuantity
            val orderId = "ord_copy_${UUID.randomUUID().toString().take(10)}"

            // 2. Pre-Trade Risk Checks
            val legacySignal = SignalEntity(
                id = signal.signalId,
                strategyId = signal.strategyId,
                symbol = signal.instrument,
                underlying = signal.underlying,
                instrumentType = InstrumentType.OPTIDX,
                expiry = signal.expiry,
                strikePaise = signal.strikePaise,
                optionType = signal.optionType,
                side = signal.transactionType,
                quantityLots = (proposedQuantity / signal.underlying.lotSize).coerceAtLeast(1),
                limitPricePaise = estimatedPricePaise
            )

            val riskResult = riskEngine.validatePreTrade(
                signal = legacySignal,
                follower = follower,
                user = user,
                strategy = strategy,
                brokerAccount = brokerAccount,
                riskLimits = riskLimits,
                currentOpenPositionsCount = if (openPosition != null) 1 else 0,
                isGlobalKillSwitchActive = isGlobalKillSwitchActive,
                proposedQuantity = proposedQuantity,
                estimatedPricePaise = estimatedPricePaise,
                isDuplicateOrder = (existingOrder != null)
            )

            if (!riskResult.isApproved || proposedQuantity == 0) {
                val reason = riskResult.rejectionReason ?: allocation.explanation
                val rejectedOrder = CopyOrderEntity(
                    id = orderId,
                    signalId = signal.signalId,
                    strategyId = signal.strategyId,
                    followerId = follower.followerId,
                    brokerAccountId = follower.brokerAccountId,
                    symbol = signal.instrument,
                    side = signal.transactionType,
                    requestedQuantity = proposedQuantity,
                    executedQuantity = 0,
                    requestedPricePaise = estimatedPricePaise,
                    status = OrderStatus.REJECTED,
                    rejectionReason = reason,
                    idempotencyKey = idempotencyKey
                )
                dao.insertCopyOrder(rejectedOrder)
                rejectCount++

                summaries.add(
                    CopyExecutionSummary(
                        followerId = follower.followerId,
                        orderId = orderId,
                        brokerCode = customerBroker,
                        status = OrderStatus.REJECTED,
                        executedQuantity = 0,
                        averagePricePaise = 0L,
                        message = reason
                    )
                )
                continue
            }

            // 3. Create Customer Copy Order in SUBMITTING status
            val pendingOrder = CopyOrderEntity(
                id = orderId,
                signalId = signal.signalId,
                strategyId = signal.strategyId,
                followerId = follower.followerId,
                brokerAccountId = follower.brokerAccountId,
                symbol = signal.instrument,
                side = signal.transactionType,
                requestedQuantity = proposedQuantity,
                executedQuantity = 0,
                requestedPricePaise = estimatedPricePaise,
                status = OrderStatus.SUBMITTING,
                idempotencyKey = idempotencyKey
            )
            dao.insertCopyOrder(pendingOrder)

            // 4. Execute through that Customer's OWN BrokerAdapter
            val adapter = brokerConnectionService?.getAdapter(customerBroker) ?: fallbackBrokerAdapter
            val brokerRequest = BrokerOrderRequest(
                symbol = signal.instrument,
                side = signal.transactionType,
                quantity = proposedQuantity,
                orderType = signal.orderType,
                pricePaise = estimatedPricePaise,
                idempotencyKey = idempotencyKey
            )

            val executionResult = try {
                adapter?.placeOrder(brokerRequest) ?: BrokerExecutionResult(
                    success = true,
                    status = OrderStatus.FILLED,
                    brokerOrderId = "SANDBOX_${orderId.take(8)}",
                    executedQuantity = proposedQuantity,
                    averagePricePaise = estimatedPricePaise,
                    latencyMs = 45L
                )
            } catch (e: Exception) {
                BrokerExecutionResult(
                    success = false,
                    status = OrderStatus.FAILED,
                    rejectionReason = "Broker submission exception: ${e.message}",
                    latencyMs = 120L
                )
            }

            // 5. Update Order with Broker Execution Result
            val updatedOrder = pendingOrder.copy(
                status = executionResult.status,
                executedQuantity = executionResult.executedQuantity,
                averagePricePaise = executionResult.averagePricePaise,
                brokerOrderId = executionResult.brokerOrderId,
                rejectionReason = executionResult.rejectionReason,
                executionLatencyMs = executionResult.latencyMs,
                updatedAt = System.currentTimeMillis()
            )
            dao.updateCopyOrder(updatedOrder)

            if (executionResult.success && executionResult.executedQuantity > 0) {
                successCount++

                // Record Position Mapping relationship (leader_position <-> customer_copy_position)
                val mapping = PositionMappingEntity(
                    leaderPositionId = leaderTrade.brokerOrderId,
                    leaderTradeId = leaderTrade.eventId,
                    followerId = follower.followerId,
                    followerBrokerAccountId = follower.brokerAccountId,
                    followerBrokerOrderId = executionResult.brokerOrderId ?: orderId,
                    symbol = signal.instrument,
                    side = signal.transactionType,
                    customerQuantity = executionResult.executedQuantity,
                    remainingQuantity = executionResult.executedQuantity,
                    averagePricePaise = executionResult.averagePricePaise,
                    status = PositionStatus.OPEN
                )
                dao.insertPositionMapping(mapping)

                // Update customer open position in local database
                val existingPos = dao.getOpenPosition(follower.followerId, signal.instrument)
                if (existingPos == null) {
                    val newPos = PositionEntity(
                        followerId = follower.followerId,
                        brokerAccountId = follower.brokerAccountId,
                        strategyId = signal.strategyId,
                        symbol = signal.instrument,
                        underlying = signal.underlying,
                        optionType = signal.optionType,
                        strikePaise = signal.strikePaise,
                        expiry = signal.expiry,
                        side = signal.transactionType,
                        quantity = executionResult.executedQuantity,
                        averageBuyPricePaise = executionResult.averagePricePaise,
                        ltpPaise = executionResult.averagePricePaise,
                        status = PositionStatus.OPEN
                    )
                    dao.insertPosition(newPos)
                } else {
                    val newTotal = existingPos.quantity + executionResult.executedQuantity
                    dao.updatePosition(existingPos.copy(quantity = newTotal, updatedAt = System.currentTimeMillis()))
                }

                // Deduct cash from customer's broker account
                if (brokerAccount != null) {
                    val totalCost = executionResult.executedQuantity * executionResult.averagePricePaise
                    val updatedCash = (brokerAccount.availableFundsPaise - totalCost).coerceAtLeast(0L)
                    dao.insertBrokerAccount(brokerAccount.copy(availableFundsPaise = updatedCash, lastSyncAt = System.currentTimeMillis()))
                }

                dao.insertNotification(
                    NotificationEntity(
                        userId = follower.followerId,
                        title = "Trade Mirrored: ${signal.instrument}",
                        message = "Copied ${executionResult.executedQuantity} units @ ${MoneyFormatter.formatPaise(executionResult.averagePricePaise)} via ${customerBroker.displayName}",
                        type = NotificationType.TRADE
                    )
                )
            } else {
                failCount++
            }

            summaries.add(
                CopyExecutionSummary(
                    followerId = follower.followerId,
                    orderId = orderId,
                    brokerCode = customerBroker,
                    status = executionResult.status,
                    executedQuantity = executionResult.executedQuantity,
                    averagePricePaise = executionResult.averagePricePaise,
                    message = executionResult.rejectionReason ?: "Execution confirmed by broker"
                )
            )
        }

        return SignalExecutionReport(
            signalId = signal.signalId,
            leaderTradeId = leaderTrade.eventId,
            totalFollowersTargeted = activeFollowers.size,
            successfulExecutions = successCount,
            rejectedExecutions = rejectCount,
            failedExecutions = failCount,
            executionDetails = summaries
        )
    }

    /**
     * Exit Mapping: Identifies customers holding the corresponding copied position.
     * Creates customer exit orders according to each customer's actual copied position (Section 17).
     */
    private suspend fun processLeaderExit(
        leaderTrade: LeaderTradeEventEntity,
        signal: TradeSignalEntity,
        correlationId: String
    ): SignalExecutionReport {
        val openMappings = dao.getOpenMappingsBySymbol(leaderTrade.symbol)
        val summaries = mutableListOf<CopyExecutionSummary>()
        var successCount = 0

        for (mapping in openMappings) {
            val customerBrokerAccount = dao.getBrokerAccountById(mapping.followerBrokerAccountId)
            val customerBroker = customerBrokerAccount?.brokerCode ?: BrokerCode.PAPER_BROKER
            val customerExitQty = mapping.remainingQuantity

            if (customerExitQty <= 0) continue

            val exitOrderId = "ord_exit_${UUID.randomUUID().toString().take(10)}"
            val idempotencyKey = "exit_lt_${leaderTrade.eventId}_cust_${mapping.followerId}_v1"

            val exitOrder = CopyOrderEntity(
                id = exitOrderId,
                signalId = signal.signalId,
                strategyId = signal.strategyId,
                followerId = mapping.followerId,
                brokerAccountId = mapping.followerBrokerAccountId,
                symbol = leaderTrade.symbol,
                side = OrderSide.SELL,
                requestedQuantity = customerExitQty,
                executedQuantity = customerExitQty,
                requestedPricePaise = leaderTrade.averagePricePaise,
                averagePricePaise = leaderTrade.averagePricePaise,
                brokerOrderId = "EXIT_${exitOrderId.take(6)}",
                status = OrderStatus.FILLED,
                idempotencyKey = idempotencyKey
            )
            dao.insertCopyOrder(exitOrder)

            // Submit exit to customer's own broker
            val adapter = brokerConnectionService?.getAdapter(customerBroker) ?: fallbackBrokerAdapter
            val brokerRequest = BrokerOrderRequest(
                symbol = leaderTrade.symbol,
                side = OrderSide.SELL,
                quantity = customerExitQty,
                orderType = OrderType.MARKET,
                pricePaise = leaderTrade.averagePricePaise,
                idempotencyKey = idempotencyKey
            )
            adapter?.placeOrder(brokerRequest)

            // Update position mapping to CLOSED
            dao.updatePositionMapping(
                mapping.copy(
                    remainingQuantity = 0,
                    status = PositionStatus.CLOSED,
                    updatedAt = System.currentTimeMillis()
                )
            )

            // Update follower position to CLOSED
            val existingPos = dao.getOpenPosition(mapping.followerId, leaderTrade.symbol)
            if (existingPos != null) {
                val rem = (existingPos.quantity - customerExitQty).coerceAtLeast(0)
                dao.updatePosition(
                    existingPos.copy(
                        quantity = rem,
                        status = if (rem == 0) PositionStatus.CLOSED else PositionStatus.OPEN,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            successCount++
            summaries.add(
                CopyExecutionSummary(
                    followerId = mapping.followerId,
                    orderId = exitOrderId,
                    brokerCode = customerBroker,
                    status = OrderStatus.FILLED,
                    executedQuantity = customerExitQty,
                    averagePricePaise = leaderTrade.averagePricePaise,
                    message = "Position closed: Exited ${customerExitQty} units"
                )
            )
        }

        return SignalExecutionReport(
            signalId = signal.signalId,
            leaderTradeId = leaderTrade.eventId,
            totalFollowersTargeted = openMappings.size,
            successfulExecutions = successCount,
            rejectedExecutions = 0,
            failedExecutions = 0,
            executionDetails = summaries
        )
    }

    /**
     * Backward-compatible overload for legacy manual broadcasts.
     */
    suspend fun processSignal(signal: SignalEntity, correlationId: String = UUID.randomUUID().toString()): SignalExecutionReport = withContext(Dispatchers.IO) {
        val simulatedLeaderTrade = LeaderTradeEventEntity(
            eventId = "trade_sig_${signal.id.take(8)}",
            leaderId = "leader_vikram",
            strategyId = signal.strategyId,
            broker = BrokerCode.ANGEL_ONE,
            brokerOrderId = "ANGEL_${UUID.randomUUID().toString().take(6)}",
            symbol = signal.symbol,
            instrument = signal.symbol,
            transactionType = signal.side,
            quantity = signal.quantityLots * signal.underlying.lotSize,
            filledQuantity = signal.quantityLots * signal.underlying.lotSize,
            averagePricePaise = signal.limitPricePaise ?: 14500L,
            status = OrderStatus.FILLED,
            idempotencyKey = "manual_sig_${signal.id}"
        )
        processLeaderTrade(simulatedLeaderTrade, correlationId)
    }
}
