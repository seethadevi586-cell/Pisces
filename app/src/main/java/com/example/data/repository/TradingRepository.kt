package com.example.data.repository

import android.content.Context
import com.example.broker.BrokerConnectionService
import com.example.broker.MockBrokerAdapter
import com.example.broker.model.*
import com.example.data.local.AppDatabase
import com.example.data.local.dao.TradingDao
import com.example.data.local.entity.*
import com.example.domain.model.*
import com.example.engine.CopyEngine
import com.example.engine.ReconciliationEngine
import com.example.engine.SignalExecutionReport
import com.example.engine.TradeDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

data class Section33TestResult(
    val timestamp: Long = System.currentTimeMillis(),
    val step1LeaderTradeSuccess: Boolean,
    val customerAQty: Int,      // Expected: 25 (Angel One, Fixed)
    val customerBQty: Int,      // Expected: 50 (Upstox, Fixed)
    val customerCQty: Int,      // Expected: 25 (Angel One, Ratio 0.25)
    val customerDOrdersCount: Int, // Expected: 0 (Upstox, Copy OFF)
    val duplicatePrevented: Boolean, // Expected: true
    val exitCustomerAQty: Int,  // Expected: 25
    val exitCustomerBQty: Int,  // Expected: 50
    val exitCustomerCQty: Int,  // Expected: 25
    val isAllPassed: Boolean,
    val summaryLog: String
)

class TradingRepository(
    private val dao: TradingDao,
    val mockBrokerAdapter: MockBrokerAdapter
) {
    val brokerConnectionService = BrokerConnectionService(dao, mockBrokerAdapter)
    val copyEngine = CopyEngine(dao, brokerConnectionService, mockBrokerAdapter)
    val tradeDetector = TradeDetector(dao, copyEngine, brokerConnectionService)
    private val reconciliationEngine = ReconciliationEngine(dao, mockBrokerAdapter)

    companion object {
        @Volatile
        private var INSTANCE: TradingRepository? = null

        fun getInstance(context: Context): TradingRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val broker = MockBrokerAdapter()
                val instance = TradingRepository(db.tradingDao(), broker)
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun initializeSeedDataIfNeeded() = withContext(Dispatchers.IO) {
        val admin = dao.getUserById(SeedData.ADMIN_USER.id)
        if (admin == null) {
            // Seed all initial entities
            dao.insertUsers(listOf(SeedData.ADMIN_USER, SeedData.LEADER_USER, SeedData.DEMO_FOLLOWER_ME) + SeedData.SECTION_33_CUSTOMERS + SeedData.DEMO_FOLLOWERS)
            dao.insertStrategies(listOf(SeedData.STRATEGY_1, SeedData.STRATEGY_2, SeedData.STRATEGY_3))
            dao.insertBrokerAccounts(SeedData.createInitialBrokerAccounts())
            dao.insertFollowers(SeedData.createInitialStrategyFollowers())
            dao.insertRiskLimits(SeedData.createInitialRiskLimits())
            SeedData.createInitialSignals().forEach { dao.insertSignal(it) }
            SeedData.createInitialOrders().forEach { dao.insertCopyOrder(it) }
            SeedData.createInitialPositions().forEach { dao.insertPosition(it) }
            SeedData.createInitialAuditLogs().forEach { dao.insertAuditLog(it) }
            SeedData.createInitialNotifications().forEach { dao.insertNotification(it) }
            dao.saveSystemConfig(SystemConfigEntity())
        }
    }

    // Flows
    fun observeActiveStrategies(): Flow<List<StrategyEntity>> = dao.getActiveStrategies()
    fun observeStrategiesByLeader(leaderId: String): Flow<List<StrategyEntity>> = dao.getStrategiesByLeader(leaderId)
    fun observeOpenPositions(followerId: String): Flow<List<PositionEntity>> = dao.getOpenPositionsForFollower(followerId)
    fun observeAllOpenPositions(): Flow<List<PositionEntity>> = dao.getAllOpenPositions()
    fun observeOrdersForFollower(followerId: String): Flow<List<CopyOrderEntity>> = dao.getOrdersForFollower(followerId)
    fun observeAllOrders(): Flow<List<CopyOrderEntity>> = dao.getAllOrders()
    fun observeLeaderTrades(leaderId: String): Flow<List<LeaderTradeEventEntity>> = dao.getLeaderTrades(leaderId)
    fun observeAllLeaderTrades(): Flow<List<LeaderTradeEventEntity>> = dao.getAllLeaderTrades()
    fun observeSignals(): Flow<List<SignalEntity>> = dao.getAllSignals()
    fun observeFollowings(followerId: String): Flow<List<StrategyFollowerEntity>> = dao.getFollowingsForUser(followerId)
    fun observeBrokerAccounts(userId: String): Flow<List<BrokerAccountEntity>> = dao.getBrokerAccountsForUser(userId)
    fun observeAllBrokerAccounts(): Flow<List<BrokerAccountEntity>> = dao.getAllBrokerAccounts()
    fun observeRiskLimits(followerId: String): Flow<RiskLimitEntity?> = dao.observeRiskLimits(followerId)
    fun observeAuditLogs(): Flow<List<AuditLogEntity>> = dao.getAuditLogs()
    fun observeNotifications(userId: String): Flow<List<NotificationEntity>> = dao.getNotificationsForUser(userId)
    fun observeSystemConfig(): Flow<SystemConfigEntity?> = dao.observeSystemConfig()
    fun observeAllUsers(): Flow<List<UserEntity>> = dao.getAllUsers()

    // Actions
    suspend fun followStrategy(
        followerId: String,
        strategyId: String,
        brokerAccountId: String,
        allocationType: AllocationType,
        allocationValuePaise: Long,
        allocationPct: Double,
        maxMultiplier: Int
    ) = withContext(Dispatchers.IO) {
        val follower = StrategyFollowerEntity(
            id = "rel_${UUID.randomUUID().toString().take(8)}",
            strategyId = strategyId,
            followerId = followerId,
            brokerAccountId = brokerAccountId,
            allocationType = allocationType,
            allocationValuePaise = allocationValuePaise,
            allocationPct = allocationPct,
            maxMultiplier = maxMultiplier,
            isPaused = false,
            subscriptionActive = true
        )
        dao.insertFollower(follower)

        val strategy = dao.getStrategyById(strategyId)
        if (strategy != null) {
            dao.insertStrategy(strategy.copy(activeFollowersCount = strategy.activeFollowersCount + 1))
        }

        dao.insertAuditLog(
            AuditLogEntity(
                userId = followerId,
                action = "START_COPYING",
                entityType = "Strategy",
                entityId = strategyId,
                detailsJson = """{"allocationType":"$allocationType","valuePaise":$allocationValuePaise,"pct":$allocationPct}"""
            )
        )
    }

    suspend fun stopFollowing(followerId: String, strategyId: String) = withContext(Dispatchers.IO) {
        dao.deleteFollower(strategyId, followerId)
        val strategy = dao.getStrategyById(strategyId)
        if (strategy != null) {
            dao.insertStrategy(strategy.copy(activeFollowersCount = (strategy.activeFollowersCount - 1).coerceAtLeast(0)))
        }
        dao.insertAuditLog(
            AuditLogEntity(
                userId = followerId,
                action = "STOP_COPYING",
                entityType = "Strategy",
                entityId = strategyId,
                detailsJson = """{"reason":"User manual stop"}"""
            )
        )
    }

    suspend fun createStrategy(strategy: StrategyEntity) = withContext(Dispatchers.IO) {
        dao.insertStrategy(strategy)
        dao.insertAuditLog(
            AuditLogEntity(
                userId = strategy.leaderId,
                action = "CREATE_STRATEGY",
                entityType = "Strategy",
                entityId = strategy.id,
                detailsJson = """{"name":"${strategy.name}","underlying":"${strategy.underlying.name}","minCapital":${strategy.minCapitalPaise}}"""
            )
        )
    }

    suspend fun toggleFollowerPause(followerId: String, strategyId: String, isPaused: Boolean) = withContext(Dispatchers.IO) {
        val current = dao.getFollowerRelationship(strategyId, followerId)
        if (current != null) {
            dao.insertFollower(current.copy(isPaused = isPaused))
            dao.insertAuditLog(
                AuditLogEntity(
                    userId = followerId,
                    action = if (isPaused) "PAUSE_COPYING" else "RESUME_COPYING",
                    entityType = "StrategyFollower",
                    entityId = current.id,
                    detailsJson = """{"isPaused":$isPaused}"""
                )
            )
        }
    }

    suspend fun toggleEmergencyStop(followerId: String, stopEngaged: Boolean) = withContext(Dispatchers.IO) {
        val limits = dao.getRiskLimits(followerId) ?: RiskLimitEntity(followerId = followerId)
        dao.insertRiskLimit(limits.copy(isEmergencyStopped = stopEngaged, updatedAt = System.currentTimeMillis()))
        dao.insertAuditLog(
            AuditLogEntity(
                userId = followerId,
                action = if (stopEngaged) "USER_EMERGENCY_STOP_TRIGGERED" else "USER_EMERGENCY_STOP_RELEASED",
                entityType = "RiskLimits",
                entityId = limits.id,
                detailsJson = """{"isEmergencyStopped":$stopEngaged}"""
            )
        )
    }

    suspend fun updateRiskLimits(limits: RiskLimitEntity) = withContext(Dispatchers.IO) {
        dao.insertRiskLimit(limits.copy(updatedAt = System.currentTimeMillis()))
        dao.insertAuditLog(
            AuditLogEntity(
                userId = limits.followerId,
                action = "UPDATE_RISK_LIMITS",
                entityType = "RiskLimits",
                entityId = limits.id,
                detailsJson = """{"maxDailyLossPaise":${limits.maxDailyLossPaise},"maxPositions":${limits.maxOpenPositions}}"""
            )
        )
    }

    suspend fun broadcastSignal(signal: SignalEntity): SignalExecutionReport = withContext(Dispatchers.IO) {
        dao.insertSignal(signal)
        val correlationId = "corr_sig_${UUID.randomUUID().toString().take(8)}"
        dao.insertAuditLog(
            AuditLogEntity(
                correlationId = correlationId,
                userId = signal.strategyId,
                action = "LEADER_SIGNAL_BROADCAST",
                entityType = "Signal",
                entityId = signal.id,
                detailsJson = """{"symbol":"${signal.symbol}","side":"${signal.side}","lots":${signal.quantityLots}}"""
            )
        )
        copyEngine.processSignal(signal, correlationId)
    }

    suspend fun toggleGlobalKillSwitch(isActive: Boolean) = withContext(Dispatchers.IO) {
        val current = dao.getSystemConfig() ?: SystemConfigEntity()
        dao.saveSystemConfig(current.copy(isGlobalKillSwitchActive = isActive, updatedAt = System.currentTimeMillis()))
        dao.insertAuditLog(
            AuditLogEntity(
                userId = "ADMIN_OPERATOR",
                action = if (isActive) "GLOBAL_KILL_SWITCH_ENGAGED" else "GLOBAL_KILL_SWITCH_DISENGAGED",
                entityType = "SystemConfig",
                entityId = "GLOBAL_CONFIG",
                detailsJson = """{"active":$isActive}"""
            )
        )
    }

    suspend fun setDeveloperSimulationControls(mode: SimulationFailureMode, latencyMs: Long) = withContext(Dispatchers.IO) {
        mockBrokerAdapter.failureMode = mode
        mockBrokerAdapter.simulatedLatencyMs = latencyMs
        val current = dao.getSystemConfig() ?: SystemConfigEntity()
        dao.saveSystemConfig(current.copy(simulatedFailureMode = mode, simulatedLatencyMs = latencyMs))
    }

    suspend fun triggerReconciliationAudit(): ReconciliationResult = withContext(Dispatchers.IO) {
        reconciliationEngine.runReconciliationAudit()
    }

    suspend fun connectBrokerAccount(userId: String, brokerCode: BrokerCode, clientId: String, token: String): BrokerAuthResult = withContext(Dispatchers.IO) {
        val credentials = BrokerCredentials(
            apiKey = token,
            apiSecret = token,
            clientCode = clientId,
            passwordOrPin = "1234",
            totpKey = "123456",
            authCode = token
        )
        brokerConnectionService.connectAccount(userId, brokerCode, clientId, credentials)
    }

    suspend fun disconnectBrokerAccount(userId: String, brokerCode: BrokerCode): Boolean = withContext(Dispatchers.IO) {
        brokerConnectionService.disconnectAccount(userId, brokerCode)
    }

    suspend fun markNotificationsRead(userId: String) = withContext(Dispatchers.IO) {
        dao.markAllNotificationsRead(userId)
    }

    suspend fun executeLeaderBrokerTrade(
        leaderId: String,
        strategyId: String,
        brokerCode: BrokerCode,
        symbol: String,
        side: OrderSide,
        quantity: Int,
        orderType: OrderType = OrderType.MARKET,
        pricePaise: Long = 14500L
    ): SignalExecutionReport = withContext(Dispatchers.IO) {
        tradeDetector.executeAndDetectLeaderTrade(
            leaderId = leaderId,
            strategyId = strategyId,
            brokerCode = brokerCode,
            symbol = symbol,
            side = side,
            quantity = quantity,
            orderType = orderType,
            pricePaise = pricePaise
        )
    }

    suspend fun updateCustomerCopySettings(
        followerId: String,
        strategyId: String,
        copyTradingEnabled: Boolean,
        allocationType: AllocationType,
        fixedQuantityUnits: Int,
        multiplierRatio: Double,
        allocationPct: Double,
        allocationValuePaise: Long,
        maxQuantityUnits: Int,
        maxCapitalPerTradePaise: Long,
        maxDailyLossPaise: Long,
        maxOpenPositions: Int,
        isEmergencyStopped: Boolean
    ) = withContext(Dispatchers.IO) {
        val current = dao.getFollowerRelationship(strategyId, followerId)
        if (current != null) {
            val updated = current.copy(
                copyTradingEnabled = copyTradingEnabled,
                allocationType = allocationType,
                fixedQuantityUnits = fixedQuantityUnits,
                multiplierRatio = multiplierRatio,
                allocationPct = allocationPct,
                allocationValuePaise = allocationValuePaise,
                maxQuantityUnits = maxQuantityUnits,
                maxCapitalPerTradePaise = maxCapitalPerTradePaise,
                maxDailyLossPaise = maxDailyLossPaise,
                maxOpenPositions = maxOpenPositions,
                isEmergencyStopped = isEmergencyStopped
            )
            dao.insertFollower(updated)
            dao.insertAuditLog(
                AuditLogEntity(
                    userId = followerId,
                    action = "UPDATE_COPY_SETTINGS",
                    entityType = "CopySettings",
                    entityId = strategyId,
                    detailsJson = """{"copyEnabled":$copyTradingEnabled,"mode":"${allocationType.name}","fixedQty":$fixedQuantityUnits,"ratio":$multiplierRatio}"""
                )
            )
        }
    }

    suspend fun runSection33CUJTest(): Section33TestResult = withContext(Dispatchers.IO) {
        // 1. Ensure test users, broker accounts and followers exist in DB
        dao.insertUsers(SeedData.SECTION_33_CUSTOMERS)
        dao.insertBrokerAccounts(SeedData.createInitialBrokerAccounts().filter {
            it.id in listOf("broker_acc_leader", "broker_acc_a", "broker_acc_b", "broker_acc_c", "broker_acc_d")
        })
        dao.insertFollowers(SeedData.createInitialStrategyFollowers().filter {
            it.id in listOf("rel_follow_a", "rel_follow_b", "rel_follow_c", "rel_follow_d")
        })

        val log = StringBuilder()
        val symbol = "NIFTY 25000 CE"
        val testTradeId = "cuj_lt_${UUID.randomUUID().toString().take(6)}"

        log.appendLine("▶ STEP 1: Leader executes BUY $symbol (100 units) via Angel One...")
        val leaderEntry = LeaderTradeEventEntity(
            eventId = testTradeId,
            leaderId = SeedData.LEADER_USER.id,
            strategyId = SeedData.STRATEGY_1.id,
            broker = BrokerCode.ANGEL_ONE,
            brokerOrderId = "ANGEL_LEADER_ORD_100",
            symbol = symbol,
            instrument = symbol,
            transactionType = OrderSide.BUY,
            quantity = 100,
            filledQuantity = 100,
            averagePricePaise = 14500L,
            status = OrderStatus.FILLED,
            idempotencyKey = "cuj_entry_$testTradeId"
        )

        val report1 = copyEngine.processLeaderTrade(leaderEntry)
        log.appendLine("Signal fan-out targeted: ${report1.totalFollowersTargeted} followers")

        val orderA = report1.executionDetails.firstOrNull { it.followerId == SeedData.CUSTOMER_A.id }
        val orderB = report1.executionDetails.firstOrNull { it.followerId == SeedData.CUSTOMER_B.id }
        val orderC = report1.executionDetails.firstOrNull { it.followerId == SeedData.CUSTOMER_C.id }
        val orderD = report1.executionDetails.firstOrNull { it.followerId == SeedData.CUSTOMER_D.id }

        val qtyA = orderA?.executedQuantity ?: 0
        val qtyB = orderB?.executedQuantity ?: 0
        val qtyC = orderC?.executedQuantity ?: 0
        val countD = if (orderD != null && orderD.executedQuantity > 0) 1 else 0

        log.appendLine("• Customer A (Fixed=25, Angel One): $qtyA units [Expected: 25]")
        log.appendLine("• Customer B (Fixed=50, Upstox): $qtyB units [Expected: 50]")
        log.appendLine("• Customer C (Ratio=0.25, Angel One): $qtyC units [Expected: 25]")
        log.appendLine("• Customer D (Copy=OFF, Upstox): ${if (countD == 0) "NO ORDER CREATED" else "$countD orders"} [Expected: NO ORDER]")

        val step1Passed = (qtyA == 25 && qtyB == 50 && qtyC == 25 && countD == 0)

        // 2. Duplicate Protection Test
        log.appendLine("▶ STEP 2: Re-sending identical Leader trade event ($testTradeId)...")
        val report2 = copyEngine.processLeaderTrade(leaderEntry)
        val duplicatePrevented = (report2.successfulExecutions == 0)
        log.appendLine("• Duplicate Prevention Engine: ${if (duplicatePrevented) "PASSED (0 duplicate orders)" else "FAILED"}")

        // 3. Leader Exit Test
        log.appendLine("▶ STEP 3: Leader exits position (SELL $symbol 100 units)...")
        val leaderExit = LeaderTradeEventEntity(
            eventId = "exit_$testTradeId",
            leaderId = SeedData.LEADER_USER.id,
            strategyId = SeedData.STRATEGY_1.id,
            broker = BrokerCode.ANGEL_ONE,
            brokerOrderId = "ANGEL_LEADER_EXIT_100",
            symbol = symbol,
            instrument = symbol,
            transactionType = OrderSide.SELL,
            quantity = 100,
            filledQuantity = 100,
            averagePricePaise = 17200L,
            status = OrderStatus.FILLED,
            idempotencyKey = "cuj_exit_$testTradeId"
        )

        val report3 = copyEngine.processLeaderTrade(leaderExit)
        val exitA = report3.executionDetails.firstOrNull { it.followerId == SeedData.CUSTOMER_A.id }
        val exitB = report3.executionDetails.firstOrNull { it.followerId == SeedData.CUSTOMER_B.id }
        val exitC = report3.executionDetails.firstOrNull { it.followerId == SeedData.CUSTOMER_C.id }

        val exitQtyA = exitA?.executedQuantity ?: 0
        val exitQtyB = exitB?.executedQuantity ?: 0
        val exitQtyC = exitC?.executedQuantity ?: 0

        log.appendLine("• Exit Customer A: $exitQtyA units [Expected: 25]")
        log.appendLine("• Exit Customer B: $exitQtyB units [Expected: 50]")
        log.appendLine("• Exit Customer C: $exitQtyC units [Expected: 25]")

        val step3Passed = (exitQtyA == 25 && exitQtyB == 50 && exitQtyC == 25)
        val allPassed = step1Passed && duplicatePrevented && step3Passed

        Section33TestResult(
            step1LeaderTradeSuccess = (report1.successfulExecutions >= 3),
            customerAQty = qtyA,
            customerBQty = qtyB,
            customerCQty = qtyC,
            customerDOrdersCount = countD,
            duplicatePrevented = duplicatePrevented,
            exitCustomerAQty = exitQtyA,
            exitCustomerBQty = exitQtyB,
            exitCustomerCQty = exitQtyC,
            isAllPassed = allPassed,
            summaryLog = log.toString()
        )
    }
}
