package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.*
import java.util.UUID

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole = UserRole.CUSTOMER,
    val status: UserStatus = UserStatus.ACTIVE,
    val panMasked: String = "ABCDE****F",
    val onboardingCompleted: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "broker_accounts",
    indices = [
        Index(value = ["userId", "brokerCode"], unique = true),
        Index(value = ["status"])
    ]
)
data class BrokerAccountEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val brokerCode: BrokerCode,
    val brokerClientId: String,
    val status: BrokerAccountStatus = BrokerAccountStatus.CONNECTED,
    val encryptedCredentials: String = "",
    val accessTokenReference: String = "",
    val refreshTokenReference: String = "",
    val tokenExpiry: Long = 0L,
    val availableFundsPaise: Long = 50000000L, // Default ₹5,00,000 in paper trading
    val usedMarginPaise: Long = 0L,
    val lastConnectedAt: Long = System.currentTimeMillis(),
    val lastSyncAt: Long = System.currentTimeMillis(),
    val isPaperMode: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "strategies",
    indices = [Index(value = ["leaderId"])]
)
data class StrategyEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val leaderId: String,
    val leaderName: String,
    val name: String,
    val description: String,
    val underlying: UnderlyingIndex = UnderlyingIndex.NIFTY,
    val minCapitalPaise: Long = 10000000L, // ₹1,00,000
    val winRatePct: Double = 68.5,
    val totalReturnPct: Double = 42.8,
    val sharpeRatio: Double = 2.15,
    val maxDrawdownPct: Double = 8.4,
    val monthlySubscriptionFeePaise: Long = 299900L, // ₹2,999
    val activeFollowersCount: Int = 18,
    val status: StrategyStatus = StrategyStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "strategy_followers",
    indices = [Index(value = ["strategyId", "followerId"], unique = true)]
)
data class StrategyFollowerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val strategyId: String,
    val followerId: String,
    val brokerAccountId: String,
    val copyTradingEnabled: Boolean = true,
    val allocationType: AllocationType = AllocationType.FIXED_QUANTITY,
    val fixedQuantityUnits: Int = 25,
    val multiplierRatio: Double = 0.50,
    val allocationValuePaise: Long = 5000000L, // Max ₹50,000 per trade for FIXED_CAPITAL_AMOUNT
    val allocationPct: Double = 20.0, // 20% for CAPITAL_PERCENTAGE
    val maxMultiplier: Int = 1,
    val maxQuantityUnits: Int = 250,
    val maxCapitalPerTradePaise: Long = 5000000L, // ₹50,000
    val maxDailyLossPaise: Long = 1500000L, // ₹15,000
    val maxOpenPositions: Int = 4,
    val isEmergencyStopped: Boolean = false,
    val allowedInstruments: String = "NIFTY,BANKNIFTY,FINNIFTY,SENSEX",
    val isPaused: Boolean = false,
    val subscriptionActive: Boolean = true,
    val startedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "risk_limits",
    indices = [Index(value = ["followerId"], unique = true)]
)
data class RiskLimitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val followerId: String,
    val maxDailyLossPaise: Long = 1500000L, // ₹15,000 max daily loss
    val maxOpenPositions: Int = 4,
    val maxTradeAmountPaise: Long = 5000000L, // ₹50,000
    val maxQuantityLots: Int = 10,
    val isEmergencyStopped: Boolean = false,
    val dailyRealizedPnlPaise: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "signals",
    indices = [Index(value = ["strategyId"]), Index(value = ["timestamp"])]
)
data class SignalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val strategyId: String,
    val symbol: String, // e.g. "NIFTY26OCT25000CE"
    val underlying: UnderlyingIndex,
    val instrumentType: InstrumentType = InstrumentType.OPTIDX,
    val expiry: String, // e.g. "2026-10-29"
    val strikePaise: Long, // e.g. 25000 * 100
    val optionType: OptionType,
    val side: OrderSide,
    val quantityLots: Int,
    val orderType: OrderType = OrderType.MARKET,
    val limitPricePaise: Long? = null,
    val signalType: SignalType = SignalType.ENTRY,
    val timestamp: Long = System.currentTimeMillis(),
    val status: SignalStatus = SignalStatus.COMPLETED
)

@Entity(
    tableName = "copy_orders",
    indices = [
        Index(value = ["signalId"]),
        Index(value = ["followerId"]),
        Index(value = ["strategyId"]),
        Index(value = ["status"]),
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["createdAt"])
    ]
)
data class CopyOrderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val signalId: String,
    val strategyId: String,
    val followerId: String,
    val brokerAccountId: String,
    val symbol: String,
    val side: OrderSide,
    val requestedQuantity: Int,
    val executedQuantity: Int = 0,
    val requestedPricePaise: Long,
    val averagePricePaise: Long = 0L,
    val brokerOrderId: String? = null,
    val status: OrderStatus = OrderStatus.CREATED,
    val rejectionReason: String? = null,
    val idempotencyKey: String,
    val executionLatencyMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "positions",
    indices = [
        Index(value = ["followerId", "symbol"]),
        Index(value = ["status"]),
        Index(value = ["strategyId"])
    ]
)
data class PositionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val followerId: String,
    val brokerAccountId: String,
    val strategyId: String,
    val symbol: String,
    val underlying: UnderlyingIndex,
    val optionType: OptionType,
    val strikePaise: Long,
    val expiry: String,
    val side: OrderSide,
    val quantity: Int, // Net open quantity
    val averageBuyPricePaise: Long = 0L,
    val averageSellPricePaise: Long = 0L,
    val ltpPaise: Long, // Last Traded Price in paise
    val unrealizedPnlPaise: Long = 0L,
    val realizedPnlPaise: Long = 0L,
    val status: PositionStatus = PositionStatus.OPEN,
    val openedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "audit_logs",
    indices = [Index(value = ["correlationId"]), Index(value = ["timestamp"])]
)
data class AuditLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val correlationId: String = UUID.randomUUID().toString(),
    val userId: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val detailsJson: String,
    val ipAddress: String = "127.0.0.1",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val message: String,
    val type: NotificationType = NotificationType.TRADE,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "system_config")
data class SystemConfigEntity(
    @PrimaryKey val key: String = "GLOBAL_CONFIG",
    val isGlobalKillSwitchActive: Boolean = false,
    val simulatedLatencyMs: Long = 150L,
    val simulatedFailureMode: SimulationFailureMode = SimulationFailureMode.NONE,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "leader_trades",
    indices = [
        Index(value = ["leaderId"]),
        Index(value = ["strategyId"]),
        Index(value = ["brokerOrderId"]),
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["createdAt"])
    ]
)
data class LeaderTradeEventEntity(
    @PrimaryKey val eventId: String = UUID.randomUUID().toString(),
    val leaderId: String,
    val strategyId: String,
    val broker: BrokerCode,
    val brokerOrderId: String,
    val brokerTradeId: String = "",
    val exchange: String = "NFO",
    val segment: String = "OPTIDX",
    val instrument: String,
    val instrumentToken: String = "",
    val symbol: String,
    val transactionType: OrderSide,
    val orderType: OrderType = OrderType.MARKET,
    val productType: String = "NRML",
    val quantity: Int,
    val filledQuantity: Int,
    val averagePricePaise: Long,
    val tradeTime: Long = System.currentTimeMillis(),
    val status: OrderStatus = OrderStatus.FILLED,
    val source: String = "BROKER_STREAM",
    val idempotencyKey: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "trade_signals",
    indices = [
        Index(value = ["strategyId"]),
        Index(value = ["leaderTradeId"]),
        Index(value = ["timestamp"])
    ]
)
data class TradeSignalEntity(
    @PrimaryKey val signalId: String = UUID.randomUUID().toString(),
    val strategyId: String,
    val leaderTradeId: String,
    val instrument: String,
    val exchange: String = "NFO",
    val transactionType: OrderSide,
    val orderType: OrderType = OrderType.MARKET,
    val quantity: Int,
    val filledQuantity: Int,
    val pricePaise: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val signalVersion: Int = 1,
    val status: SignalStatus = SignalStatus.COMPLETED,
    val underlying: UnderlyingIndex = UnderlyingIndex.NIFTY,
    val optionType: OptionType = OptionType.CE,
    val strikePaise: Long = 0L,
    val expiry: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "position_mappings",
    indices = [
        Index(value = ["leaderPositionId"]),
        Index(value = ["followerId"]),
        Index(value = ["status"])
    ]
)
data class PositionMappingEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val leaderPositionId: String,
    val leaderTradeId: String,
    val followerId: String,
    val followerBrokerAccountId: String,
    val followerBrokerOrderId: String,
    val symbol: String,
    val side: OrderSide,
    val customerQuantity: Int,
    val remainingQuantity: Int,
    val averagePricePaise: Long,
    val status: PositionStatus = PositionStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

