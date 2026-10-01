package com.example.broker.model

import com.example.domain.model.*

/**
 * Standard Multi-Broker Data Models for PISCES Backend
 */

data class BrokerCredentials(
    val apiKey: String = "",
    val clientCode: String = "",
    val passwordOrPin: String = "",
    val totpKey: String = "",
    val apiSecret: String = "",
    val authCode: String = "",
    val redirectUri: String = "",
    val state: String = ""
)

data class BrokerAuthResult(
    val success: Boolean,
    val accessToken: String = "",
    val refreshToken: String = "",
    val feedToken: String = "",
    val tokenExpiry: Long = 0L,
    val message: String = "",
    val errorCode: String? = null
)

data class BrokerProfile(
    val clientId: String,
    val name: String,
    val email: String,
    val phone: String,
    val broker: BrokerCode,
    val exchanges: List<String> = listOf("NSE", "NFO", "BSE", "BFO"),
    val panMasked: String = "ABCDE****F"
)

data class BrokerFunds(
    val availableCashPaise: Long,
    val usedMarginPaise: Long,
    val totalCollateralPaise: Long,
    val payInPaise: Long = 0L,
    val spanMarginPaise: Long = 0L,
    val exposureMarginPaise: Long = 0L
)

data class BrokerHolding(
    val symbol: String,
    val isin: String,
    val quantity: Int,
    val averagePricePaise: Long,
    val ltpPaise: Long,
    val pnlPaise: Long,
    val exchange: String = "NSE"
)

data class BrokerPosition(
    val symbol: String,
    val exchange: String = "NFO",
    val instrumentToken: String = "",
    val quantity: Int = 0,
    val averagePricePaise: Long = 0L,
    val ltpPaise: Long = 0L,
    val unrealizedPnlPaise: Long = 0L,
    val realizedPnlPaise: Long = 0L,
    val side: OrderSide = OrderSide.BUY,
    val productType: String = "INTRADAY"
)

data class BrokerInstrument(
    val symbol: String,
    val name: String,
    val exchange: String,
    val token: String,
    val instrumentType: InstrumentType = InstrumentType.OPTIDX,
    val lotSize: Int = 25,
    val tickSizePaise: Long = 5L,
    val expiry: String = "",
    val strikePaise: Long = 0L
)

data class BrokerOrderRequest(
    val symbol: String,
    val exchange: String = "NFO",
    val instrumentToken: String = "",
    val side: OrderSide,
    val quantity: Int,
    val orderType: OrderType = OrderType.MARKET,
    val pricePaise: Long? = null,
    val triggerPricePaise: Long? = null,
    val productType: String = "INTRADAY",
    val validity: String = "DAY",
    val idempotencyKey: String,
    val orderTag: String = "PISCES"
)

data class BrokerExecutionResult(
    val success: Boolean,
    val brokerOrderId: String = "",
    val status: OrderStatus,
    val executedQuantity: Int = 0,
    val averagePricePaise: Long = 0L,
    val rejectionReason: String? = null,
    val latencyMs: Long = 0L,
    val rawResponse: String = ""
)

data class BrokerOrder(
    val brokerOrderId: String,
    val internalOrderId: String = "",
    val symbol: String,
    val exchange: String = "NFO",
    val side: OrderSide,
    val quantity: Int,
    val executedQuantity: Int = 0,
    val pricePaise: Long = 0L,
    val status: OrderStatus = OrderStatus.SUBMITTED,
    val rejectionReason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class BrokerTrade(
    val tradeId: String,
    val brokerOrderId: String,
    val symbol: String,
    val side: OrderSide,
    val quantity: Int,
    val pricePaise: Long,
    val timestamp: Long = System.currentTimeMillis()
)

data class NormalizedMarketData(
    val symbol: String,
    val exchange: String,
    val token: String,
    val ltpPaise: Long,
    val changePaise: Long = 0L,
    val highPaise: Long = 0L,
    val lowPaise: Long = 0L,
    val openPaise: Long = 0L,
    val volume: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

data class NormalizedOrderEvent(
    val type: NormalizedEventType,
    val brokerOrderId: String,
    val symbol: String,
    val side: OrderSide,
    val quantity: Int,
    val filledQuantity: Int,
    val pricePaise: Long,
    val status: OrderStatus,
    val rejectionReason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class PositionMismatch(
    val symbol: String,
    val internalQuantity: Int,
    val brokerQuantity: Int,
    val discrepancyType: DiscrepancyType,
    val details: String
)

enum class DiscrepancyType {
    MISSING_IN_BROKER,
    UNEXPECTED_IN_BROKER,
    QUANTITY_MISMATCH,
    PRICE_MISMATCH
}

data class ReconciliationResult(
    val timestamp: Long = System.currentTimeMillis(),
    val isBalanced: Boolean,
    val totalPositionsAudited: Int,
    val discrepancies: List<PositionMismatch>
)

data class SafeBrokerStatus(
    val brokerCode: BrokerCode,
    val status: BrokerAccountStatus,
    val clientId: String,
    val lastSyncAt: Long = 0L,
    val availableFundsPaise: Long = 0L
)

