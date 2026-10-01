package com.example

import com.example.data.local.entity.*
import com.example.domain.model.*
import com.example.engine.AllocationCalculator
import com.example.engine.RiskEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AllocationAndRiskUnitTest {

    private lateinit var riskEngine: RiskEngine

    @Before
    fun setup() {
        riskEngine = RiskEngine()
    }

    @Test
    fun testAllocation_FixedAmount_CalculatesCorrectLots() {
        // NIFTY lot size = 25
        // Option premium = ₹100.00 (10,000 paise)
        // 1 lot cost = 25 * 10,000 = 2,50,000 paise = ₹2,500
        // Budget = ₹10,000 (1,000,000 paise)
        // Max lots = 1,000,000 / 250,000 = 4 lots = 100 units
        val result = AllocationCalculator.calculateQuantity(
            allocationType = AllocationType.FIXED_AMOUNT,
            allocationValuePaise = 1000000L,
            allocationPct = 0.0,
            availableMarginPaise = 5000000L,
            estimatedPricePaise = 10000L,
            underlying = UnderlyingIndex.NIFTY,
            maxMultiplier = 1
        )

        assertEquals(4, result.calculatedLots)
        assertEquals(100, result.calculatedQuantity)
        assertEquals(1000000L, result.estimatedCapitalRequiredPaise)
    }

    @Test
    fun testAllocation_PercentageMargin_CalculatesCorrectLots() {
        // Available margin = ₹1,00,000 (10,000,000 paise)
        // Percentage = 20% -> Budget = ₹20,000 (2,000,000 paise)
        // BankNifty lot size = 15
        // Premium = ₹200.00 (20,000 paise)
        // 1 lot cost = 15 * 20,000 = 300,000 paise = ₹3,000
        // Expected lots = 2,000,000 / 300,000 = 6 lots = 90 units
        val result = AllocationCalculator.calculateQuantity(
            allocationType = AllocationType.PERCENTAGE,
            allocationValuePaise = 0L,
            allocationPct = 20.0,
            availableMarginPaise = 10000000L,
            estimatedPricePaise = 20000L,
            underlying = UnderlyingIndex.BANKNIFTY,
            maxMultiplier = 1
        )

        assertEquals(6, result.calculatedLots)
        assertEquals(90, result.calculatedQuantity)
        assertEquals(1800000L, result.estimatedCapitalRequiredPaise)
    }

    @Test
    fun testAllocation_InsufficientBudget_ReturnsZeroQuantity() {
        // Budget ₹1,000 (100,000 paise)
        // 1 lot of Nifty at ₹100 premium costs ₹2,500 (250,000 paise)
        val result = AllocationCalculator.calculateQuantity(
            allocationType = AllocationType.FIXED_AMOUNT,
            allocationValuePaise = 100000L,
            allocationPct = 0.0,
            availableMarginPaise = 100000L,
            estimatedPricePaise = 10000L,
            underlying = UnderlyingIndex.NIFTY
        )

        assertEquals(0, result.calculatedLots)
        assertEquals(0, result.calculatedQuantity)
    }

    @Test
    fun testLotSizeValidation() {
        assertTrue(AllocationCalculator.isValidLotQuantity(25, UnderlyingIndex.NIFTY))
        assertTrue(AllocationCalculator.isValidLotQuantity(50, UnderlyingIndex.NIFTY))
        assertFalse(AllocationCalculator.isValidLotQuantity(30, UnderlyingIndex.NIFTY)) // Invalid for Nifty

        assertTrue(AllocationCalculator.isValidLotQuantity(15, UnderlyingIndex.BANKNIFTY))
        assertTrue(AllocationCalculator.isValidLotQuantity(30, UnderlyingIndex.BANKNIFTY))
        assertFalse(AllocationCalculator.isValidLotQuantity(25, UnderlyingIndex.BANKNIFTY)) // Invalid for BankNifty
    }

    @Test
    fun testRiskEngine_GlobalKillSwitch_BlocksOrder() {
        val signal = createTestSignal()
        val follower = createTestFollower()

        val check = riskEngine.validatePreTrade(
            signal = signal,
            follower = follower,
            user = UserEntity(name = "Test", email = "test@example.com", phone = "123"),
            strategy = StrategyEntity(leaderId = "1", leaderName = "L", name = "S", description = "D"),
            brokerAccount = BrokerAccountEntity(userId = "1", brokerCode = BrokerCode.PAPER_BROKER, brokerClientId = "C1"),
            riskLimits = RiskLimitEntity(followerId = follower.followerId),
            currentOpenPositionsCount = 0,
            isGlobalKillSwitchActive = true, // KILL SWITCH ENGAGED
            proposedQuantity = 50,
            estimatedPricePaise = 10000L,
            isDuplicateOrder = false
        )

        assertFalse(check.isApproved)
        assertEquals("GLOBAL_KILL_SWITCH_ACTIVE", check.failureCode)
    }

    @Test
    fun testRiskEngine_UserEmergencyStop_BlocksOrder() {
        val signal = createTestSignal()
        val follower = createTestFollower()
        val riskLimits = RiskLimitEntity(followerId = follower.followerId, isEmergencyStopped = true)

        val check = riskEngine.validatePreTrade(
            signal = signal,
            follower = follower,
            user = UserEntity(name = "Test", email = "test@example.com", phone = "123"),
            strategy = StrategyEntity(leaderId = "1", leaderName = "L", name = "S", description = "D"),
            brokerAccount = BrokerAccountEntity(userId = "1", brokerCode = BrokerCode.PAPER_BROKER, brokerClientId = "C1"),
            riskLimits = riskLimits,
            currentOpenPositionsCount = 0,
            isGlobalKillSwitchActive = false,
            proposedQuantity = 50,
            estimatedPricePaise = 10000L,
            isDuplicateOrder = false
        )

        assertFalse(check.isApproved)
        assertEquals("USER_EMERGENCY_STOP_ENGAGED", check.failureCode)
    }

    @Test
    fun testRiskEngine_BrokerDisconnected_BlocksOrder() {
        val signal = createTestSignal()
        val follower = createTestFollower()
        val brokerAccount = BrokerAccountEntity(
            userId = "1",
            brokerCode = BrokerCode.PAPER_BROKER,
            brokerClientId = "C1",
            status = BrokerAccountStatus.DISCONNECTED
        )

        val check = riskEngine.validatePreTrade(
            signal = signal,
            follower = follower,
            user = UserEntity(name = "Test", email = "test@example.com", phone = "123"),
            strategy = StrategyEntity(leaderId = "1", leaderName = "L", name = "S", description = "D"),
            brokerAccount = brokerAccount,
            riskLimits = RiskLimitEntity(followerId = follower.followerId),
            currentOpenPositionsCount = 0,
            isGlobalKillSwitchActive = false,
            proposedQuantity = 50,
            estimatedPricePaise = 10000L,
            isDuplicateOrder = false
        )

        assertFalse(check.isApproved)
        assertEquals("BROKER_DISCONNECTED", check.failureCode)
    }

    @Test
    fun testRiskEngine_DuplicateOrderIdempotency_BlocksOrder() {
        val signal = createTestSignal()
        val follower = createTestFollower()

        val check = riskEngine.validatePreTrade(
            signal = signal,
            follower = follower,
            user = UserEntity(name = "Test", email = "test@example.com", phone = "123"),
            strategy = StrategyEntity(leaderId = "1", leaderName = "L", name = "S", description = "D"),
            brokerAccount = BrokerAccountEntity(userId = "1", brokerCode = BrokerCode.PAPER_BROKER, brokerClientId = "C1"),
            riskLimits = RiskLimitEntity(followerId = follower.followerId),
            currentOpenPositionsCount = 0,
            isGlobalKillSwitchActive = false,
            proposedQuantity = 50,
            estimatedPricePaise = 10000L,
            isDuplicateOrder = true // DUPLICATE DETECTED
        )

        assertFalse(check.isApproved)
        assertEquals("DUPLICATE_ORDER_REJECTED", check.failureCode)
    }

    @Test
    fun testRiskEngine_MaxDailyLossBreach_BlocksOrder() {
        val signal = createTestSignal()
        val follower = createTestFollower()
        // Max loss = ₹15,000 (1,500,000 paise). Realized daily P&L = -₹16,000 (-1,600,000 paise)
        val riskLimits = RiskLimitEntity(
            followerId = follower.followerId,
            maxDailyLossPaise = 1500000L,
            dailyRealizedPnlPaise = -1600000L
        )

        val check = riskEngine.validatePreTrade(
            signal = signal,
            follower = follower,
            user = UserEntity(name = "Test", email = "test@example.com", phone = "123"),
            strategy = StrategyEntity(leaderId = "1", leaderName = "L", name = "S", description = "D"),
            brokerAccount = BrokerAccountEntity(userId = "1", brokerCode = BrokerCode.PAPER_BROKER, brokerClientId = "C1"),
            riskLimits = riskLimits,
            currentOpenPositionsCount = 0,
            isGlobalKillSwitchActive = false,
            proposedQuantity = 50,
            estimatedPricePaise = 10000L,
            isDuplicateOrder = false
        )

        assertFalse(check.isApproved)
        assertEquals("DAILY_LOSS_LIMIT_BREACHED", check.failureCode)
    }

    private fun createTestSignal() = SignalEntity(
        id = "sig_1",
        strategyId = "strat_1",
        symbol = "NIFTY 25000 CE",
        underlying = UnderlyingIndex.NIFTY,
        expiry = "2026-10-29",
        strikePaise = 2500000L,
        optionType = OptionType.CE,
        side = OrderSide.BUY,
        quantityLots = 2
    )

    private fun createTestFollower() = StrategyFollowerEntity(
        strategyId = "strat_1",
        followerId = "user_f1",
        brokerAccountId = "acc_1"
    )
}
