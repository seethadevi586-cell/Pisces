package com.example.engine

import com.example.broker.BrokerAdapter
import com.example.broker.BrokerConnectionService
import com.example.broker.model.BrokerOrderRequest
import com.example.data.local.dao.TradingDao
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.LeaderTradeEventEntity
import com.example.domain.model.BrokerCode
import com.example.domain.model.OrderSide
import com.example.domain.model.OrderStatus
import com.example.domain.model.OrderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * PISCES Trade Detector.
 * Listens to and ingests actual trades executed in the Leader's connected broker account
 * (Angel One SmartAPI, Upstox Developer API, or Paper Leader Sandbox).
 * Normalizes all broker-specific events (New, Accepted, Rejected, Partial Fill, Full Fill, Exit)
 * and dispatches them to the Signal Normalizer and Copy-Trading Engine.
 */
class TradeDetector(
    private val dao: TradingDao,
    private val copyEngine: CopyEngine,
    private val brokerConnectionService: BrokerConnectionService
) {

    /**
     * Executes a trade in the Leader's connected broker account, confirms broker execution,
     * creates a normalized LeaderTradeEvent, and triggers automated follower trade mirroring.
     */
    suspend fun executeAndDetectLeaderTrade(
        leaderId: String,
        strategyId: String,
        brokerCode: BrokerCode,
        symbol: String,
        side: OrderSide,
        quantity: Int,
        orderType: OrderType = OrderType.MARKET,
        pricePaise: Long = 14500L
    ): SignalExecutionReport = withContext(Dispatchers.IO) {
        val adapter = brokerConnectionService.getAdapter(brokerCode)
        val correlationId = UUID.randomUUID().toString()
        val idempotencyKey = "leader_trade_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

        // 1. Submit trade to Leader's OWN broker account
        val brokerRequest = BrokerOrderRequest(
            symbol = symbol,
            side = side,
            quantity = quantity,
            orderType = orderType,
            pricePaise = pricePaise,
            idempotencyKey = idempotencyKey
        )

        val brokerResult = try {
            adapter.placeOrder(brokerRequest)
        } catch (e: Exception) {
            com.example.broker.model.BrokerExecutionResult(
                success = false,
                status = OrderStatus.FAILED,
                rejectionReason = "Broker connection failure: ${e.message}"
            )
        }

        // 2. Audit log the leader's direct broker order
        dao.insertAuditLog(
            AuditLogEntity(
                correlationId = correlationId,
                userId = leaderId,
                action = "LEADER_BROKER_TRADE_DETECTED",
                entityType = "LeaderTrade",
                entityId = brokerResult.brokerOrderId ?: "N/A",
                detailsJson = """{"broker":"${brokerCode.name}","symbol":"$symbol","side":"$side","qty":$quantity,"status":"${brokerResult.status}"}"""
            )
        )

        // 3. Normalize into LeaderTradeEventEntity
        val leaderTradeEvent = LeaderTradeEventEntity(
            eventId = "lte_${UUID.randomUUID().toString().take(12)}",
            leaderId = leaderId,
            strategyId = strategyId,
            broker = brokerCode,
            brokerOrderId = brokerResult.brokerOrderId ?: "ORD_${UUID.randomUUID().toString().take(8)}",
            brokerTradeId = "TRD_${UUID.randomUUID().toString().take(8)}",
            exchange = "NFO",
            segment = "OPTIDX",
            instrument = symbol,
            symbol = symbol,
            transactionType = side,
            orderType = orderType,
            quantity = quantity,
            filledQuantity = brokerResult.executedQuantity,
            averagePricePaise = if (brokerResult.averagePricePaise > 0L) brokerResult.averagePricePaise else pricePaise,
            tradeTime = System.currentTimeMillis(),
            status = brokerResult.status,
            source = "LEADER_BROKER_EXECUTION",
            idempotencyKey = idempotencyKey
        )

        // 4. Pass to Copy Engine for customer copy fan-out
        copyEngine.processLeaderTrade(leaderTradeEvent, correlationId)
    }

    /**
     * Ingests an incoming trade event detected via broker WebSocket or tradebook poll.
     */
    suspend fun ingestExternalLeaderTrade(event: LeaderTradeEventEntity): SignalExecutionReport = withContext(Dispatchers.IO) {
        val existing = dao.getLeaderTradeByIdempotency(event.idempotencyKey)
        if (existing != null) {
            return@withContext SignalExecutionReport(
                signalId = "duplicate_suppressed",
                leaderTradeId = event.eventId,
                totalFollowersTargeted = 0,
                successfulExecutions = 0,
                rejectedExecutions = 0,
                failedExecutions = 0,
                executionDetails = emptyList()
            )
        }
        copyEngine.processLeaderTrade(event)
    }

    /**
     * Polls the Leader's connected broker to reconcile recent executed trades.
     */
    suspend fun syncLeaderTradesFromBroker(leaderId: String, brokerCode: BrokerCode): Int = withContext(Dispatchers.IO) {
        val adapter = brokerConnectionService.getAdapter(brokerCode)
        val trades = try {
            adapter.getTradeBook()
        } catch (_: Exception) {
            emptyList()
        }

        var newTradesDetected = 0
        for (trade in trades) {
            val existing = dao.getLeaderTradeByOrderId(trade.brokerOrderId)
            if (existing == null && trade.quantity > 0) {
                val event = LeaderTradeEventEntity(
                    leaderId = leaderId,
                    strategyId = "strat_nifty_momentum",
                    broker = brokerCode,
                    brokerOrderId = trade.brokerOrderId,
                    brokerTradeId = trade.tradeId,
                    symbol = trade.symbol,
                    instrument = trade.symbol,
                    transactionType = trade.side,
                    quantity = trade.quantity,
                    filledQuantity = trade.quantity,
                    averagePricePaise = trade.pricePaise,
                    status = OrderStatus.FILLED,
                    source = "BROKER_TRADE_BOOK_SYNC",
                    idempotencyKey = "sync_${trade.brokerOrderId}_${trade.tradeId}"
                )
                copyEngine.processLeaderTrade(event)
                newTradesDetected++
            }
        }
        newTradesDetected
    }
}
