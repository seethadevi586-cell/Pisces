package com.example.data.repository

import com.example.data.local.entity.*
import com.example.domain.model.*

object SeedData {
    val ADMIN_USER = UserEntity(
        id = "user_admin_01",
        name = "Vikram Aditya (Admin)",
        email = "admin@trademirror.internal",
        phone = "+91 98765 00001",
        role = UserRole.ADMIN,
        status = UserStatus.ACTIVE,
        panMasked = "ABCDE1234F",
        onboardingCompleted = true
    )

    val LEADER_USER = UserEntity(
        id = "user_leader_01",
        name = "Rajesh Sharma (Quant Alpha)",
        email = "rajesh.sharma@algotrading.in",
        phone = "+91 98765 00002",
        role = UserRole.LEADER,
        status = UserStatus.ACTIVE,
        panMasked = "BNMPK5678Q",
        onboardingCompleted = true
    )

    val DEMO_FOLLOWER_ME = UserEntity(
        id = "user_follower_me",
        name = "Arjun Mehta (Customer)",
        email = "arjun.mehta@trademirror.com",
        phone = "+91 98765 11000",
        role = UserRole.CUSTOMER,
        status = UserStatus.ACTIVE,
        panMasked = "AAAPM9988K",
        onboardingCompleted = true
    )

    // Dedicated Customers A, B, C, D matching Section 33 CUJ Specification
    val CUSTOMER_A = UserEntity(
        id = "user_customer_a",
        name = "Customer A (Angel One)",
        email = "customer_a@trademirror.com",
        phone = "+91 98765 20001",
        role = UserRole.CUSTOMER,
        status = UserStatus.ACTIVE,
        panMasked = "ABCDE1001A"
    )

    val CUSTOMER_B = UserEntity(
        id = "user_customer_b",
        name = "Customer B (Upstox)",
        email = "customer_b@trademirror.com",
        phone = "+91 98765 20002",
        role = UserRole.CUSTOMER,
        status = UserStatus.ACTIVE,
        panMasked = "ABCDE1002B"
    )

    val CUSTOMER_C = UserEntity(
        id = "user_customer_c",
        name = "Customer C (Angel One)",
        email = "customer_c@trademirror.com",
        phone = "+91 98765 20003",
        role = UserRole.CUSTOMER,
        status = UserStatus.ACTIVE,
        panMasked = "ABCDE1003C"
    )

    val CUSTOMER_D = UserEntity(
        id = "user_customer_d",
        name = "Customer D (Upstox)",
        email = "customer_d@trademirror.com",
        phone = "+91 98765 20004",
        role = UserRole.CUSTOMER,
        status = UserStatus.ACTIVE,
        panMasked = "ABCDE1004D"
    )

    val SECTION_33_CUSTOMERS = listOf(CUSTOMER_A, CUSTOMER_B, CUSTOMER_C, CUSTOMER_D)

    val DEMO_FOLLOWERS = (1..20).map { index ->
        val names = listOf(
            "Rohan Verma", "Sneha Patel", "Ananya Roy", "Karan Singhal", "Pooja Hegde",
            "Manish Tiwari", "Divya Nair", "Suresh Pillai", "Kavita Rao", "Amitabh Sen",
            "Preeti Deshmukh", "Nikhil Chopra", "Siddharth Joshi", "Meera Iyer", "Deepak Gupta",
            "Sunil Bajaj", "Priyanka Ghosh", "Ramesh Yadav", "Alok Kumar", "Bhavna Shah"
        )
        val name = names.getOrElse(index - 1) { "Follower $index" }
        UserEntity(
            id = "user_follower_$index",
            name = name,
            email = "follower$index@trademirror.com",
            phone = "+91 98765 ${10000 + index}",
            role = UserRole.CUSTOMER,
            status = UserStatus.ACTIVE,
            panMasked = "ABCDE${index}000K",
            onboardingCompleted = true
        )
    }

    val STRATEGY_1 = StrategyEntity(
        id = "strat_nifty_momentum",
        leaderId = LEADER_USER.id,
        leaderName = LEADER_USER.name,
        name = "NIFTY Delta Momentum Pro",
        description = "Algorithmic trend-following long-option breakout system optimized for NIFTY weekly index options with tight trailing stops.",
        underlying = UnderlyingIndex.NIFTY,
        minCapitalPaise = 5000000L, // ₹50,000
        winRatePct = 72.4,
        totalReturnPct = 58.2,
        sharpeRatio = 2.45,
        maxDrawdownPct = 6.2,
        monthlySubscriptionFeePaise = 199900L, // ₹1,999 / mo
        activeFollowersCount = 19,
        status = StrategyStatus.ACTIVE
    )

    val STRATEGY_2 = StrategyEntity(
        id = "strat_banknifty_spreads",
        leaderId = LEADER_USER.id,
        leaderName = LEADER_USER.name,
        name = "BankNifty Iron Condor Alpha",
        description = "Delta-neutral theta decay harvesting strategy trading weekly BankNifty options spreads with systematic adjustment rules.",
        underlying = UnderlyingIndex.BANKNIFTY,
        minCapitalPaise = 15000000L, // ₹1,50,000
        winRatePct = 68.8,
        totalReturnPct = 39.5,
        sharpeRatio = 2.10,
        maxDrawdownPct = 4.8,
        monthlySubscriptionFeePaise = 249900L, // ₹2,499 / mo
        activeFollowersCount = 14,
        status = StrategyStatus.ACTIVE
    )

    val STRATEGY_3 = StrategyEntity(
        id = "strat_finnifty_expiry",
        leaderId = LEADER_USER.id,
        leaderName = LEADER_USER.name,
        name = "FinNifty Zero-Hero Scalper",
        description = "High probability expiry-day momentum scalp strategy capturing intraday volatility expansions on Tuesday expiries.",
        underlying = UnderlyingIndex.FINNIFTY,
        minCapitalPaise = 2500000L, // ₹25,000
        winRatePct = 64.0,
        totalReturnPct = 34.6,
        sharpeRatio = 1.85,
        maxDrawdownPct = 9.1,
        monthlySubscriptionFeePaise = 149900L, // ₹1,499 / mo
        activeFollowersCount = 11,
        status = StrategyStatus.ACTIVE
    )

    fun createInitialBrokerAccounts(): List<BrokerAccountEntity> {
        val list = mutableListOf<BrokerAccountEntity>()
        // Leader connected broker account (The SOURCE ACCOUNT)
        list.add(
            BrokerAccountEntity(
                id = "broker_acc_leader",
                userId = LEADER_USER.id,
                brokerCode = BrokerCode.ANGEL_ONE,
                brokerClientId = "ANGEL_LEADER_99",
                status = BrokerAccountStatus.CONNECTED,
                availableFundsPaise = 250000000L, // ₹25,00,000
                usedMarginPaise = 0L,
                isPaperMode = true
            )
        )
        // Section 33 Customers Accounts (Own Separate Broker Connections)
        list.add(
            BrokerAccountEntity(
                id = "broker_acc_a",
                userId = CUSTOMER_A.id,
                brokerCode = BrokerCode.ANGEL_ONE,
                brokerClientId = "ANGEL_CUST_A",
                status = BrokerAccountStatus.CONNECTED,
                availableFundsPaise = 50000000L, // ₹5,00,000
                isPaperMode = true
            )
        )
        list.add(
            BrokerAccountEntity(
                id = "broker_acc_b",
                userId = CUSTOMER_B.id,
                brokerCode = BrokerCode.UPSTOX,
                brokerClientId = "UPSTOX_CUST_B",
                status = BrokerAccountStatus.CONNECTED,
                availableFundsPaise = 50000000L,
                isPaperMode = true
            )
        )
        list.add(
            BrokerAccountEntity(
                id = "broker_acc_c",
                userId = CUSTOMER_C.id,
                brokerCode = BrokerCode.ANGEL_ONE,
                brokerClientId = "ANGEL_CUST_C",
                status = BrokerAccountStatus.CONNECTED,
                availableFundsPaise = 50000000L,
                isPaperMode = true
            )
        )
        list.add(
            BrokerAccountEntity(
                id = "broker_acc_d",
                userId = CUSTOMER_D.id,
                brokerCode = BrokerCode.UPSTOX,
                brokerClientId = "UPSTOX_CUST_D",
                status = BrokerAccountStatus.CONNECTED,
                availableFundsPaise = 50000000L,
                isPaperMode = true
            )
        )
        // Demo follower account
        list.add(
            BrokerAccountEntity(
                id = "broker_acc_me",
                userId = DEMO_FOLLOWER_ME.id,
                brokerCode = BrokerCode.ANGEL_ONE,
                brokerClientId = "ANGEL_A8921",
                status = BrokerAccountStatus.CONNECTED,
                availableFundsPaise = 42500000L, // ₹4,25,000
                usedMarginPaise = 7500000L,
                isPaperMode = true
            )
        )
        // Additional broker accounts for other followers
        DEMO_FOLLOWERS.forEachIndexed { i, f ->
            val broker = when (i % 4) {
                0 -> BrokerCode.ZERODHA
                1 -> BrokerCode.UPSTOX
                2 -> BrokerCode.ANGEL_ONE
                else -> BrokerCode.GROWW
            }
            list.add(
                BrokerAccountEntity(
                    id = "broker_acc_f_$i",
                    userId = f.id,
                    brokerCode = broker,
                    brokerClientId = "CL_${broker.name.take(2)}_${100 + i}",
                    status = BrokerAccountStatus.CONNECTED,
                    availableFundsPaise = 30000000L + (i * 2500000L),
                    usedMarginPaise = 2500000L,
                    isPaperMode = true
                )
            )
        }
        return list
    }

    fun createInitialStrategyFollowers(): List<StrategyFollowerEntity> {
        val list = mutableListOf<StrategyFollowerEntity>()

        // Section 33 Customers Subscriptions
        list.add(
            StrategyFollowerEntity(
                id = "rel_follow_a",
                strategyId = STRATEGY_1.id,
                followerId = CUSTOMER_A.id,
                brokerAccountId = "broker_acc_a",
                copyTradingEnabled = true,
                allocationType = AllocationType.FIXED_QUANTITY,
                fixedQuantityUnits = 25,
                subscriptionActive = true
            )
        )
        list.add(
            StrategyFollowerEntity(
                id = "rel_follow_b",
                strategyId = STRATEGY_1.id,
                followerId = CUSTOMER_B.id,
                brokerAccountId = "broker_acc_b",
                copyTradingEnabled = true,
                allocationType = AllocationType.FIXED_QUANTITY,
                fixedQuantityUnits = 50,
                subscriptionActive = true
            )
        )
        list.add(
            StrategyFollowerEntity(
                id = "rel_follow_c",
                strategyId = STRATEGY_1.id,
                followerId = CUSTOMER_C.id,
                brokerAccountId = "broker_acc_c",
                copyTradingEnabled = true,
                allocationType = AllocationType.RATIO,
                multiplierRatio = 0.25,
                subscriptionActive = true
            )
        )
        list.add(
            StrategyFollowerEntity(
                id = "rel_follow_d",
                strategyId = STRATEGY_1.id,
                followerId = CUSTOMER_D.id,
                brokerAccountId = "broker_acc_d",
                copyTradingEnabled = false, // Copy = OFF
                allocationType = AllocationType.FIXED_QUANTITY,
                fixedQuantityUnits = 25,
                subscriptionActive = true
            )
        )

        // Arjun Mehta follows Strategy 1
        list.add(
            StrategyFollowerEntity(
                id = "rel_follow_me_1",
                strategyId = STRATEGY_1.id,
                followerId = DEMO_FOLLOWER_ME.id,
                brokerAccountId = "broker_acc_me",
                allocationType = AllocationType.FIXED_AMOUNT,
                allocationValuePaise = 5000000L, // ₹50,000 per trade
                allocationPct = 25.0,
                maxMultiplier = 1,
                isPaused = false,
                subscriptionActive = true
            )
        )
        // Other demo followers follow Strategy 1
        DEMO_FOLLOWERS.take(18).forEachIndexed { i, f ->
            list.add(
                StrategyFollowerEntity(
                    id = "rel_follow_$i",
                    strategyId = STRATEGY_1.id,
                    followerId = f.id,
                    brokerAccountId = "broker_acc_f_$i",
                    allocationType = if (i % 2 == 0) AllocationType.FIXED_AMOUNT else AllocationType.PERCENTAGE,
                    allocationValuePaise = 4000000L + (i * 500000L),
                    allocationPct = 20.0,
                    maxMultiplier = 1,
                    isPaused = (i == 17), // 1 paused for demo
                    subscriptionActive = true
                )
            )
        }
        return list
    }

    fun createInitialRiskLimits(): List<RiskLimitEntity> {
        val list = mutableListOf<RiskLimitEntity>()
        SECTION_33_CUSTOMERS.forEach { cust ->
            list.add(
                RiskLimitEntity(
                    id = "risk_${cust.id}",
                    followerId = cust.id,
                    maxDailyLossPaise = 2500000L, // ₹25,000
                    maxOpenPositions = 5,
                    maxTradeAmountPaise = 10000000L, // ₹1,00,000
                    maxQuantityLots = 15,
                    isEmergencyStopped = false,
                    dailyRealizedPnlPaise = 0L
                )
            )
        }
        list.add(
            RiskLimitEntity(
                id = "risk_me",
                followerId = DEMO_FOLLOWER_ME.id,
                maxDailyLossPaise = 1500000L, // ₹15,000
                maxOpenPositions = 4,
                maxTradeAmountPaise = 5000000L, // ₹50,000
                maxQuantityLots = 8,
                isEmergencyStopped = false,
                dailyRealizedPnlPaise = 485000L // +₹4,850 today
            )
        )
        DEMO_FOLLOWERS.forEach { f ->
            list.add(
                RiskLimitEntity(
                    id = "risk_${f.id}",
                    followerId = f.id,
                    maxDailyLossPaise = 2000000L,
                    maxOpenPositions = 5,
                    maxTradeAmountPaise = 6000000L,
                    maxQuantityLots = 10,
                    isEmergencyStopped = false,
                    dailyRealizedPnlPaise = 0L
                )
            )
        }
        return list
    }

    fun createInitialSignals(): List<SignalEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            SignalEntity(
                id = "sig_demo_01",
                strategyId = STRATEGY_1.id,
                symbol = "NIFTY 25000 CE",
                underlying = UnderlyingIndex.NIFTY,
                instrumentType = InstrumentType.OPTIDX,
                expiry = "2026-10-29",
                strikePaise = 2500000L,
                optionType = OptionType.CE,
                side = OrderSide.BUY,
                quantityLots = 2, // 50 units (2 lots)
                orderType = OrderType.MARKET,
                limitPricePaise = 14250L, // ₹142.50
                signalType = SignalType.ENTRY,
                timestamp = now - 3600000L,
                status = SignalStatus.COMPLETED
            ),
            SignalEntity(
                id = "sig_demo_02",
                strategyId = STRATEGY_1.id,
                symbol = "NIFTY 24800 PE",
                underlying = UnderlyingIndex.NIFTY,
                instrumentType = InstrumentType.OPTIDX,
                expiry = "2026-10-29",
                strikePaise = 2480000L,
                optionType = OptionType.PE,
                side = OrderSide.BUY,
                quantityLots = 1, // 25 units
                orderType = OrderType.MARKET,
                limitPricePaise = 9800L, // ₹98.00
                signalType = SignalType.ENTRY,
                timestamp = now - 7200000L,
                status = SignalStatus.COMPLETED
            )
        )
    }

    fun createInitialPositions(): List<PositionEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            PositionEntity(
                id = "pos_demo_01",
                followerId = DEMO_FOLLOWER_ME.id,
                brokerAccountId = "broker_acc_me",
                strategyId = STRATEGY_1.id,
                symbol = "NIFTY 25000 CE",
                underlying = UnderlyingIndex.NIFTY,
                optionType = OptionType.CE,
                strikePaise = 2500000L,
                expiry = "2026-10-29",
                side = OrderSide.BUY,
                quantity = 50, // 2 lots
                averageBuyPricePaise = 14250L, // ₹142.50
                averageSellPricePaise = 0L,
                ltpPaise = 16820L, // ₹168.20 (+₹25.70 per share)
                unrealizedPnlPaise = 128500L, // +₹1,285.00
                realizedPnlPaise = 0L,
                status = PositionStatus.OPEN,
                openedAt = now - 3600000L
            ),
            PositionEntity(
                id = "pos_demo_02",
                followerId = DEMO_FOLLOWER_ME.id,
                brokerAccountId = "broker_acc_me",
                strategyId = STRATEGY_1.id,
                symbol = "NIFTY 24800 PE",
                underlying = UnderlyingIndex.NIFTY,
                optionType = OptionType.PE,
                strikePaise = 2480000L,
                expiry = "2026-10-29",
                side = OrderSide.BUY,
                quantity = 25, // 1 lot
                averageBuyPricePaise = 9800L,
                averageSellPricePaise = 0L,
                ltpPaise = 11240L,
                unrealizedPnlPaise = 36000L, // +₹360.00
                realizedPnlPaise = 0L,
                status = PositionStatus.OPEN,
                openedAt = now - 7200000L
            )
        )
    }

    fun createInitialOrders(): List<CopyOrderEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            CopyOrderEntity(
                id = "ord_demo_01",
                signalId = "sig_demo_01",
                strategyId = STRATEGY_1.id,
                followerId = DEMO_FOLLOWER_ME.id,
                brokerAccountId = "broker_acc_me",
                symbol = "NIFTY 25000 CE",
                side = OrderSide.BUY,
                requestedQuantity = 50,
                executedQuantity = 50,
                requestedPricePaise = 14250L,
                averagePricePaise = 14250L,
                brokerOrderId = "ORD_ZER_9921",
                status = OrderStatus.FILLED,
                idempotencyKey = "${STRATEGY_1.id}_sig_demo_01_${DEMO_FOLLOWER_ME.id}_v1",
                executionLatencyMs = 84L,
                createdAt = now - 3600000L
            ),
            CopyOrderEntity(
                id = "ord_demo_02",
                signalId = "sig_demo_02",
                strategyId = STRATEGY_1.id,
                followerId = DEMO_FOLLOWER_ME.id,
                brokerAccountId = "broker_acc_me",
                symbol = "NIFTY 24800 PE",
                side = OrderSide.BUY,
                requestedQuantity = 25,
                executedQuantity = 25,
                requestedPricePaise = 9800L,
                averagePricePaise = 9800L,
                brokerOrderId = "ORD_ZER_9922",
                status = OrderStatus.FILLED,
                idempotencyKey = "${STRATEGY_1.id}_sig_demo_02_${DEMO_FOLLOWER_ME.id}_v1",
                executionLatencyMs = 92L,
                createdAt = now - 7200000L
            )
        )
    }

    fun createInitialAuditLogs(): List<AuditLogEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            AuditLogEntity(
                correlationId = "corr_init_01",
                userId = DEMO_FOLLOWER_ME.id,
                action = "USER_LOGIN_OTP_VERIFIED",
                entityType = "User",
                entityId = DEMO_FOLLOWER_ME.id,
                detailsJson = """{"authMethod":"OTP_SMS","device":"Android Emulator"}""",
                timestamp = now - 10800000L
            ),
            AuditLogEntity(
                correlationId = "corr_init_02",
                userId = DEMO_FOLLOWER_ME.id,
                action = "BROKER_CONNECTED",
                entityType = "BrokerAccount",
                entityId = "broker_acc_me",
                detailsJson = """{"broker":"ZERODHA","clientId":"ZM9821","mode":"PAPER"}""",
                timestamp = now - 10700000L
            ),
            AuditLogEntity(
                correlationId = "corr_init_03",
                userId = DEMO_FOLLOWER_ME.id,
                action = "STRATEGY_FOLLOW_STARTED",
                entityType = "StrategyFollower",
                entityId = "rel_follow_me_1",
                detailsJson = """{"strategy":"NIFTY Delta Momentum Pro","allocationType":"FIXED_AMOUNT","valuePaise":5000000}""",
                timestamp = now - 10000000L
            )
        )
    }

    fun createInitialNotifications(): List<NotificationEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            NotificationEntity(
                userId = DEMO_FOLLOWER_ME.id,
                title = "Copy Trade Executed",
                message = "Copied BUY 50x NIFTY 25000 CE @ ₹142.50 (Strategy: NIFTY Delta Momentum Pro)",
                type = NotificationType.TRADE,
                isRead = false,
                timestamp = now - 3600000L
            ),
            NotificationEntity(
                userId = DEMO_FOLLOWER_ME.id,
                title = "Daily Risk Limits Active",
                message = "Max Daily Loss set to ₹15,000. Open positions: 2/4.",
                type = NotificationType.RISK,
                isRead = true,
                timestamp = now - 10000000L
            )
        )
    }
}
