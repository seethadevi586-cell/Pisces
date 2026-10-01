package com.example.broker

import com.example.broker.model.*
import com.example.data.local.entity.PositionEntity
import com.example.domain.model.OrderStatus

/**
 * Common Broker Adapter Interface for PISCES.
 * Isolates copy-trading and risk engines from broker-specific mechanics.
 */
interface BrokerAdapter {

    suspend fun authenticate(credentials: BrokerCredentials): BrokerAuthResult
    suspend fun refreshSession(): BrokerAuthResult
    suspend fun logout(): Boolean

    suspend fun getProfile(): BrokerProfile
    suspend fun getFunds(): BrokerFunds
    suspend fun getHoldings(): List<BrokerHolding>
    suspend fun getPositions(): List<BrokerPosition>

    suspend fun getInstruments(): List<BrokerInstrument>
    suspend fun getLTP(exchange: String, symbol: String, instrumentToken: String): Long

    suspend fun placeOrder(request: BrokerOrderRequest): BrokerExecutionResult
    suspend fun modifyOrder(brokerOrderId: String, newQuantity: Int, newPricePaise: Long?): BrokerExecutionResult
    suspend fun cancelOrder(brokerOrderId: String): Boolean

    suspend fun getOrderBook(): List<BrokerOrder>
    suspend fun getTradeBook(): List<BrokerTrade>
    suspend fun getOrderDetails(brokerOrderId: String): BrokerOrder?

    suspend fun subscribeMarketData(tokens: List<String>, onData: (NormalizedMarketData) -> Unit)
    suspend fun unsubscribeMarketData(tokens: List<String>)

    suspend fun subscribeOrderUpdates(onUpdate: (NormalizedOrderEvent) -> Unit)

    suspend fun reconcilePositions(internalPositions: List<PositionEntity>): ReconciliationResult
    suspend fun disconnect(): Boolean
}
