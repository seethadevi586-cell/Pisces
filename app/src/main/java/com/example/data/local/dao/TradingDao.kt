package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TradingDao {
    // Users
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = :role")
    fun getUsersByRole(role: UserRole): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    // Broker Accounts
    @Query("SELECT * FROM broker_accounts WHERE userId = :userId")
    fun getBrokerAccountsForUser(userId: String): Flow<List<BrokerAccountEntity>>

    @Query("SELECT * FROM broker_accounts WHERE userId = :userId AND brokerCode = :code LIMIT 1")
    suspend fun getBrokerAccount(userId: String, code: BrokerCode): BrokerAccountEntity?

    @Query("SELECT * FROM broker_accounts WHERE id = :id")
    suspend fun getBrokerAccountById(id: String): BrokerAccountEntity?

    @Query("SELECT * FROM broker_accounts")
    fun getAllBrokerAccounts(): Flow<List<BrokerAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrokerAccount(account: BrokerAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrokerAccounts(accounts: List<BrokerAccountEntity>)

    // Strategies
    @Query("SELECT * FROM strategies WHERE status = 'ACTIVE' ORDER BY totalReturnPct DESC")
    fun getActiveStrategies(): Flow<List<StrategyEntity>>

    @Query("SELECT * FROM strategies WHERE id = :id")
    suspend fun getStrategyById(id: String): StrategyEntity?

    @Query("SELECT * FROM strategies WHERE id = :id")
    fun observeStrategyById(id: String): Flow<StrategyEntity?>

    @Query("SELECT * FROM strategies WHERE leaderId = :leaderId")
    fun getStrategiesByLeader(leaderId: String): Flow<List<StrategyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStrategy(strategy: StrategyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStrategies(strategies: List<StrategyEntity>)

    // Strategy Followers
    @Query("SELECT * FROM strategy_followers WHERE strategyId = :strategyId AND isPaused = 0")
    suspend fun getActiveFollowersForStrategy(strategyId: String): List<StrategyFollowerEntity>

    @Query("SELECT * FROM strategy_followers WHERE followerId = :followerId")
    fun getFollowingsForUser(followerId: String): Flow<List<StrategyFollowerEntity>>

    @Query("SELECT * FROM strategy_followers WHERE strategyId = :strategyId AND followerId = :followerId LIMIT 1")
    suspend fun getFollowerRelationship(strategyId: String, followerId: String): StrategyFollowerEntity?

    @Query("SELECT * FROM strategy_followers WHERE strategyId = :strategyId")
    fun observeFollowersForStrategy(strategyId: String): Flow<List<StrategyFollowerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollower(follower: StrategyFollowerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowers(followers: List<StrategyFollowerEntity>)

    @Query("DELETE FROM strategy_followers WHERE strategyId = :strategyId AND followerId = :followerId")
    suspend fun deleteFollower(strategyId: String, followerId: String)

    // Risk Limits
    @Query("SELECT * FROM risk_limits WHERE followerId = :followerId LIMIT 1")
    suspend fun getRiskLimits(followerId: String): RiskLimitEntity?

    @Query("SELECT * FROM risk_limits WHERE followerId = :followerId LIMIT 1")
    fun observeRiskLimits(followerId: String): Flow<RiskLimitEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRiskLimit(riskLimit: RiskLimitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRiskLimits(limits: List<RiskLimitEntity>)

    // Signals
    @Query("SELECT * FROM signals ORDER BY timestamp DESC")
    fun getAllSignals(): Flow<List<SignalEntity>>

    @Query("SELECT * FROM signals WHERE strategyId = :strategyId ORDER BY timestamp DESC")
    fun getSignalsForStrategy(strategyId: String): Flow<List<SignalEntity>>

    @Query("SELECT * FROM signals WHERE id = :signalId")
    suspend fun getSignalById(signalId: String): SignalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: SignalEntity)

    // Copy Orders
    @Query("SELECT * FROM copy_orders WHERE followerId = :followerId ORDER BY createdAt DESC")
    fun getOrdersForFollower(followerId: String): Flow<List<CopyOrderEntity>>

    @Query("SELECT * FROM copy_orders WHERE strategyId = :strategyId ORDER BY createdAt DESC")
    fun getOrdersForStrategy(strategyId: String): Flow<List<CopyOrderEntity>>

    @Query("SELECT * FROM copy_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<CopyOrderEntity>>

    @Query("SELECT * FROM copy_orders WHERE id = :id")
    suspend fun getOrderById(id: String): CopyOrderEntity?

    @Query("SELECT * FROM copy_orders WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getOrderByIdempotencyKey(key: String): CopyOrderEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCopyOrder(order: CopyOrderEntity)

    @Update
    suspend fun updateCopyOrder(order: CopyOrderEntity)

    // Positions
    @Query("SELECT * FROM positions WHERE followerId = :followerId AND status = 'OPEN'")
    fun getOpenPositionsForFollower(followerId: String): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions WHERE followerId = :followerId")
    fun getAllPositionsForFollower(followerId: String): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions WHERE followerId = :followerId AND symbol = :symbol AND status = 'OPEN' LIMIT 1")
    suspend fun getOpenPosition(followerId: String, symbol: String): PositionEntity?

    @Query("SELECT * FROM positions WHERE status = 'OPEN'")
    fun getAllOpenPositions(): Flow<List<PositionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: PositionEntity)

    @Update
    suspend fun updatePosition(position: PositionEntity)

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    // Notifications
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsRead(userId: String)

    // System Config
    @Query("SELECT * FROM system_config WHERE `key` = 'GLOBAL_CONFIG' LIMIT 1")
    fun observeSystemConfig(): Flow<SystemConfigEntity?>

    @Query("SELECT * FROM system_config WHERE `key` = 'GLOBAL_CONFIG' LIMIT 1")
    suspend fun getSystemConfig(): SystemConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSystemConfig(config: SystemConfigEntity)

    // Leader Trades
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderTrade(trade: LeaderTradeEventEntity)

    @Query("SELECT * FROM leader_trades WHERE leaderId = :leaderId ORDER BY tradeTime DESC")
    fun getLeaderTrades(leaderId: String): Flow<List<LeaderTradeEventEntity>>

    @Query("SELECT * FROM leader_trades ORDER BY tradeTime DESC LIMIT 100")
    fun getAllLeaderTrades(): Flow<List<LeaderTradeEventEntity>>

    @Query("SELECT * FROM leader_trades WHERE brokerOrderId = :brokerOrderId LIMIT 1")
    suspend fun getLeaderTradeByOrderId(brokerOrderId: String): LeaderTradeEventEntity?

    @Query("SELECT * FROM leader_trades WHERE idempotencyKey = :idempotencyKey LIMIT 1")
    suspend fun getLeaderTradeByIdempotency(idempotencyKey: String): LeaderTradeEventEntity?

    // Trade Signals
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTradeSignal(signal: TradeSignalEntity)

    @Query("SELECT * FROM trade_signals WHERE strategyId = :strategyId ORDER BY timestamp DESC")
    fun getTradeSignals(strategyId: String): Flow<List<TradeSignalEntity>>

    @Query("SELECT * FROM trade_signals WHERE signalId = :signalId LIMIT 1")
    suspend fun getTradeSignalById(signalId: String): TradeSignalEntity?

    // Position Mappings (Leader Position <-> Follower Copied Positions)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPositionMapping(mapping: PositionMappingEntity)

    @Update
    suspend fun updatePositionMapping(mapping: PositionMappingEntity)

    @Query("SELECT * FROM position_mappings WHERE leaderPositionId = :leaderPositionId AND status = 'OPEN'")
    suspend fun getOpenMappingsForLeaderPosition(leaderPositionId: String): List<PositionMappingEntity>

    @Query("SELECT * FROM position_mappings WHERE symbol = :symbol AND status = 'OPEN'")
    suspend fun getOpenMappingsBySymbol(symbol: String): List<PositionMappingEntity>

    @Query("SELECT * FROM position_mappings WHERE followerId = :followerId AND symbol = :symbol AND status = 'OPEN' LIMIT 1")
    suspend fun getOpenMappingForFollowerSymbol(followerId: String, symbol: String): PositionMappingEntity?

    @Query("SELECT * FROM copy_orders WHERE signalId = :signalId AND followerId = :followerId LIMIT 1")
    suspend fun getCopyOrderBySignalAndFollower(signalId: String, followerId: String): CopyOrderEntity?
}
