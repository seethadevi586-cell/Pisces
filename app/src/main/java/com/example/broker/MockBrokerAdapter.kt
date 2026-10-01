package com.example.broker

import com.example.broker.model.*
import com.example.data.local.entity.PositionEntity
import com.example.domain.model.*
import kotlinx.coroutines.delay
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MockBrokerAdapter(
    val brokerCode: BrokerCode = BrokerCode.PAPER_BROKER,
    private var initialCashPaise: Long = 50000000L // ₹5,00,000
) : BrokerAdapter {

    private var isConnected: Boolean = true
    private var availableCashPaise: Long = initialCashPaise
    private var usedMarginPaise: Long = 0L

    // In-memory broker books
    private val brokerPositions = ConcurrentHashMap<String, BrokerPosition>()
    private val brokerOrders = ConcurrentHashMap<String, BrokerOrder>()
    private val brokerTrades = mutableListOf<BrokerTrade>()

    // Developer Test Injection Controls
    var simulatedLatencyMs: Long = 100L
    var failureMode: SimulationFailureMode = SimulationFailureMode.NONE

    override suspend fun authenticate(credentials: BrokerCredentials): BrokerAuthResult {
        delay(simulatedLatencyMs)
        if (failureMode == SimulationFailureMode.BROKER_DISCONNECT) {
            isConnected = false
            return BrokerAuthResult(
                success = false,
                message = "Simulated authentication rejection: Invalid credentials or session expired",
                errorCode = "ERR_AUTH_FAILED"
            )
        }
        isConnected = true
        return BrokerAuthResult(
            success = true,
            accessToken = "mock_jwt_${UUID.randomUUID()}",
            refreshToken = "mock_refresh_${UUID.randomUUID()}",
            feedToken = "mock_feed_token",
            tokenExpiry = System.currentTimeMillis() + 86400000L,
            message = "Connected to ${brokerCode.displayName} Paper Sandbox"
        )
    }

    suspend fun connect(clientId: String, authToken: String): BrokerAuthResult {
        return authenticate(BrokerCredentials(clientCode = clientId, authCode = authToken))
    }

    override suspend fun refreshSession(): BrokerAuthResult {
        delay(50)
        return BrokerAuthResult(
            success = true,
            accessToken = "mock_jwt_refreshed_${System.currentTimeMillis()}",
            tokenExpiry = System.currentTimeMillis() + 86400000L,
            message = "Session refreshed successfully"
        )
    }

    override suspend fun logout(): Boolean {
        delay(50)
        isConnected = false
        return true
    }

    override suspend fun disconnect(): Boolean {
        return logout()
    }

    override suspend fun getProfile(): BrokerProfile {
        delay(simulatedLatencyMs / 2)
        return BrokerProfile(
            clientId = "PISCES_PAPER_01",
            name = "Pisces Paper Sandbox Trader",
            email = "trader@pisces.local",
            phone = "+91 98765 43210",
            broker = brokerCode
        )
    }

    suspend fun getAccountStatus(): BrokerAccountStatus {
        if (!isConnected) return BrokerAccountStatus.DISCONNECTED
        if (failureMode == SimulationFailureMode.BROKER_DISCONNECT) return BrokerAccountStatus.TOKEN_EXPIRED
        return BrokerAccountStatus.CONNECTED
    }

    override suspend fun getFunds(): BrokerFunds {
        delay(simulatedLatencyMs / 2)
        return BrokerFunds(
            availableCashPaise = availableCashPaise,
            usedMarginPaise = usedMarginPaise,
            totalCollateralPaise = availableCashPaise + usedMarginPaise
        )
    }

    override suspend fun getHoldings(): List<BrokerHolding> {
        delay(simulatedLatencyMs / 2)
        return emptyList()
    }

    override suspend fun getPositions(): List<BrokerPosition> {
        delay(simulatedLatencyMs / 2)
        return brokerPositions.values.toList()
    }

    override suspend fun getInstruments(): List<BrokerInstrument> {
        delay(simulatedLatencyMs / 2)
        return listOf(
            BrokerInstrument(
                symbol = "NIFTY24OCT25000CE",
                name = "NIFTY 25000 CE",
                exchange = "NFO",
                token = "54321",
                instrumentType = InstrumentType.OPTIDX,
                lotSize = 25,
                strikePaise = 2500000L
            ),
            BrokerInstrument(
                symbol = "BANKNIFTY24OCT52000PE",
                name = "BANKNIFTY 52000 PE",
                exchange = "NFO",
                token = "65432",
                instrumentType = InstrumentType.OPTIDX,
                lotSize = 15,
                strikePaise = 5200000L
            )
        )
    }

    override suspend fun getLTP(exchange: String, symbol: String, instrumentToken: String): Long {
        delay(simulatedLatencyMs / 4)
        return 12550L // ₹125.50
    }

    override suspend fun getOrderBook(): List<BrokerOrder> {
        delay(simulatedLatencyMs / 2)
        return brokerOrders.values.toList()
    }

    override suspend fun getTradeBook(): List<BrokerTrade> {
        delay(simulatedLatencyMs / 2)
        return brokerTrades.toList()
    }

    override suspend fun getOrderDetails(brokerOrderId: String): BrokerOrder? {
        delay(simulatedLatencyMs / 4)
        return brokerOrders[brokerOrderId]
    }

    suspend fun getOrderStatus(brokerOrderId: String): OrderStatus {
        delay(simulatedLatencyMs / 4)
        return brokerOrders[brokerOrderId]?.status ?: OrderStatus.UNKNOWN
    }

    override suspend fun placeOrder(request: BrokerOrderRequest): BrokerExecutionResult {
        val startTime = System.currentTimeMillis()
        delay(simulatedLatencyMs)

        // Check simulated failure modes
        if (failureMode == SimulationFailureMode.TIMEOUT) {
            delay(1500)
            return BrokerExecutionResult(
                success = false,
                brokerOrderId = "ERR_TIMEOUT",
                status = OrderStatus.FAILED,
                executedQuantity = 0,
                averagePricePaise = 0L,
                rejectionReason = "Broker API socket timeout (GATEWAY_TIMEOUT_504)",
                latencyMs = System.currentTimeMillis() - startTime
            )
        }

        if (failureMode == SimulationFailureMode.BROKER_DISCONNECT) {
            isConnected = false
            return BrokerExecutionResult(
                success = false,
                brokerOrderId = "ERR_DISCONNECT",
                status = OrderStatus.FAILED,
                executedQuantity = 0,
                averagePricePaise = 0L,
                rejectionReason = "Broker session expired. Please re-authenticate.",
                latencyMs = System.currentTimeMillis() - startTime
            )
        }

        if (failureMode == SimulationFailureMode.REJECTION) {
            return BrokerExecutionResult(
                success = false,
                brokerOrderId = "REJ_" + UUID.randomUUID().toString().take(8),
                status = OrderStatus.REJECTED,
                executedQuantity = 0,
                averagePricePaise = 0L,
                rejectionReason = "Broker RMS rejection: Circuit limit reached for options contract",
                latencyMs = System.currentTimeMillis() - startTime
            )
        }

        if (failureMode == SimulationFailureMode.INSUFFICIENT_MARGIN) {
            return BrokerExecutionResult(
                success = false,
                brokerOrderId = "REJ_MARGIN",
                status = OrderStatus.REJECTED,
                executedQuantity = 0,
                averagePricePaise = 0L,
                rejectionReason = "RMS check failed: Insufficient margin available in trading account",
                latencyMs = System.currentTimeMillis() - startTime
            )
        }

        if (failureMode == SimulationFailureMode.MARKET_CLOSED) {
            return BrokerExecutionResult(
                success = false,
                brokerOrderId = "REJ_MARKET_CLOSED",
                status = OrderStatus.REJECTED,
                executedQuantity = 0,
                averagePricePaise = 0L,
                rejectionReason = "Orders can only be placed during NSE market trading hours (09:15 - 15:30 IST)",
                latencyMs = System.currentTimeMillis() - startTime
            )
        }

        // Price determination (Mock LTP with realistic options premium, e.g. ₹125.50)
        val basePricePaise = request.pricePaise ?: 12550L
        val requiredCapitalPaise = (basePricePaise * request.quantity)

        if (request.side == OrderSide.BUY && requiredCapitalPaise > availableCashPaise) {
            return BrokerExecutionResult(
                success = false,
                brokerOrderId = "REJ_FUNDS",
                status = OrderStatus.REJECTED,
                executedQuantity = 0,
                averagePricePaise = 0L,
                rejectionReason = "Insufficient funds: Required ${MoneyFormatter.formatPaise(requiredCapitalPaise)}, Available ${MoneyFormatter.formatPaise(availableCashPaise)}",
                latencyMs = System.currentTimeMillis() - startTime
            )
        }

        val brokerOrderId = "ORD_${UUID.randomUUID().toString().take(8).uppercase()}"

        val (executedQty, status) = if (failureMode == SimulationFailureMode.PARTIAL_FILL) {
            val partial = (request.quantity / 2).coerceAtLeast(1)
            Pair(partial, OrderStatus.PARTIALLY_FILLED)
        } else {
            Pair(request.quantity, OrderStatus.FILLED)
        }

        // Update funds
        if (request.side == OrderSide.BUY) {
            val costPaise = basePricePaise * executedQty
            availableCashPaise = (availableCashPaise - costPaise).coerceAtLeast(0)
            usedMarginPaise += costPaise
        } else {
            val proceedsPaise = basePricePaise * executedQty
            availableCashPaise += proceedsPaise
        }

        // Update position in broker book
        val existingPos = brokerPositions[request.symbol]
        val netQuantity = if (existingPos == null) {
            if (request.side == OrderSide.BUY) executedQty else -executedQty
        } else {
            if (request.side == OrderSide.BUY) existingPos.quantity + executedQty else existingPos.quantity - executedQty
        }

        val updatedBrokerPos = BrokerPosition(
            symbol = request.symbol,
            exchange = request.exchange,
            instrumentToken = request.instrumentToken,
            quantity = netQuantity,
            averagePricePaise = basePricePaise,
            ltpPaise = basePricePaise,
            unrealizedPnlPaise = 0L
        )
        brokerPositions[request.symbol] = updatedBrokerPos

        // Record broker order
        val brokerOrder = BrokerOrder(
            brokerOrderId = brokerOrderId,
            symbol = request.symbol,
            exchange = request.exchange,
            side = request.side,
            quantity = request.quantity,
            executedQuantity = executedQty,
            pricePaise = basePricePaise,
            status = status,
            timestamp = System.currentTimeMillis()
        )
        brokerOrders[brokerOrderId] = brokerOrder

        // Record trade
        if (executedQty > 0) {
            val trade = BrokerTrade(
                tradeId = "TRD_${UUID.randomUUID().toString().take(8).uppercase()}",
                brokerOrderId = brokerOrderId,
                symbol = request.symbol,
                side = request.side,
                quantity = executedQty,
                pricePaise = basePricePaise,
                timestamp = System.currentTimeMillis()
            )
            brokerTrades.add(trade)
        }

        return BrokerExecutionResult(
            success = true,
            brokerOrderId = brokerOrderId,
            status = status,
            executedQuantity = executedQty,
            averagePricePaise = basePricePaise,
            latencyMs = System.currentTimeMillis() - startTime
        )
    }

    override suspend fun modifyOrder(
        brokerOrderId: String,
        newQuantity: Int,
        newPricePaise: Long?
    ): BrokerExecutionResult {
        delay(simulatedLatencyMs)
        val order = brokerOrders[brokerOrderId] ?: return BrokerExecutionResult(
            success = false,
            brokerOrderId = brokerOrderId,
            status = OrderStatus.REJECTED,
            executedQuantity = 0,
            averagePricePaise = 0L,
            rejectionReason = "Order not found in broker book"
        )
        val updated = order.copy(quantity = newQuantity, pricePaise = newPricePaise ?: order.pricePaise)
        brokerOrders[brokerOrderId] = updated
        return BrokerExecutionResult(
            success = true,
            brokerOrderId = brokerOrderId,
            status = OrderStatus.SUBMITTED,
            executedQuantity = order.executedQuantity,
            averagePricePaise = updated.pricePaise
        )
    }

    override suspend fun cancelOrder(brokerOrderId: String): Boolean {
        delay(simulatedLatencyMs / 2)
        val order = brokerOrders[brokerOrderId] ?: return false
        brokerOrders[brokerOrderId] = order.copy(status = OrderStatus.CANCELLED)
        return true
    }

    override suspend fun subscribeMarketData(tokens: List<String>, onData: (NormalizedMarketData) -> Unit) {
        tokens.forEach { token ->
            onData(
                NormalizedMarketData(
                    symbol = token,
                    exchange = "NFO",
                    token = token,
                    ltpPaise = 12550L
                )
            )
        }
    }

    override suspend fun unsubscribeMarketData(tokens: List<String>) {
        // No-op for paper sandbox
    }

    override suspend fun subscribeOrderUpdates(onUpdate: (NormalizedOrderEvent) -> Unit) {
        // No-op for paper sandbox
    }

    override suspend fun reconcilePositions(internalPositions: List<PositionEntity>): ReconciliationResult {
        delay(simulatedLatencyMs)
        val discrepancies = mutableListOf<PositionMismatch>()

        val internalMap = internalPositions.filter { it.status == PositionStatus.OPEN }.associateBy { it.symbol }
        val brokerMap = brokerPositions.toMap()

        // Check internal positions against broker
        internalMap.forEach { (symbol, internalPos) ->
            val brokerPos = brokerMap[symbol]
            if (brokerPos == null) {
                discrepancies.add(
                    PositionMismatch(
                        symbol = symbol,
                        internalQuantity = internalPos.quantity,
                        brokerQuantity = 0,
                        discrepancyType = DiscrepancyType.MISSING_IN_BROKER,
                        details = "Position exists internally with qty ${internalPos.quantity} but is missing in broker book"
                    )
                )
            } else if (brokerPos.quantity != internalPos.quantity) {
                discrepancies.add(
                    PositionMismatch(
                        symbol = symbol,
                        internalQuantity = internalPos.quantity,
                        brokerQuantity = brokerPos.quantity,
                        discrepancyType = DiscrepancyType.QUANTITY_MISMATCH,
                        details = "Internal qty ${internalPos.quantity} does not match broker qty ${brokerPos.quantity}"
                    )
                )
            }
        }

        // Check broker positions against internal
        brokerMap.forEach { (symbol, brokerPos) ->
            if (!internalMap.containsKey(symbol) && brokerPos.quantity != 0) {
                discrepancies.add(
                    PositionMismatch(
                        symbol = symbol,
                        internalQuantity = 0,
                        brokerQuantity = brokerPos.quantity,
                        discrepancyType = DiscrepancyType.UNEXPECTED_IN_BROKER,
                        details = "Broker has unrecorded open position with qty ${brokerPos.quantity}"
                    )
                )
            }
        }

        val totalAudited = (internalMap.keys + brokerMap.keys).distinct().size
        return ReconciliationResult(
            isBalanced = discrepancies.isEmpty(),
            totalPositionsAudited = totalAudited,
            discrepancies = discrepancies
        )
    }

    // Helper for test control to seed or simulate position drift
    fun injectSimulatedDrift(symbol: String, quantityDrift: Int) {
        val current = brokerPositions[symbol]
        if (current != null) {
            brokerPositions[symbol] = current.copy(quantity = current.quantity + quantityDrift)
        } else {
            brokerPositions[symbol] = BrokerPosition(
                symbol = symbol,
                quantity = quantityDrift,
                averagePricePaise = 12000L,
                ltpPaise = 12000L,
                unrealizedPnlPaise = 0L
            )
        }
    }
}
