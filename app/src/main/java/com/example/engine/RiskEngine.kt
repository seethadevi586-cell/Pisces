package com.example.engine

import com.example.data.local.entity.*
import com.example.domain.model.*

data class RiskCheckResult(
    val isApproved: Boolean,
    val rejectionReason: String? = null,
    val failureCode: String? = null
)

class RiskEngine {

    /**
     * Executes pre-trade risk validations for an incoming copy trade.
     */
    fun validatePreTrade(
        signal: SignalEntity,
        follower: StrategyFollowerEntity,
        user: UserEntity?,
        strategy: StrategyEntity?,
        brokerAccount: BrokerAccountEntity?,
        riskLimits: RiskLimitEntity?,
        currentOpenPositionsCount: Int,
        isGlobalKillSwitchActive: Boolean,
        proposedQuantity: Int,
        estimatedPricePaise: Long,
        isDuplicateOrder: Boolean
    ): RiskCheckResult {
        // 1. Global Kill Switch Check
        if (isGlobalKillSwitchActive) {
            return RiskCheckResult(
                isApproved = false,
                rejectionReason = "Global execution kill switch is active across the platform",
                failureCode = "GLOBAL_KILL_SWITCH_ACTIVE"
            )
        }

        // 2. User Status Check
        if (user == null || user.status != UserStatus.ACTIVE) {
            return RiskCheckResult(
                isApproved = false,
                rejectionReason = "User account is suspended, unverified, or inactive",
                failureCode = "USER_INACTIVE"
            )
        }

        // 3. Strategy Active Check
        if (strategy == null || strategy.status != StrategyStatus.ACTIVE) {
            return RiskCheckResult(
                isApproved = false,
                rejectionReason = "Strategy is not currently active for copying",
                failureCode = "STRATEGY_INACTIVE"
            )
        }

        // 4. Follower Copy Status & Pause Check
        if (!follower.copyTradingEnabled) {
            return RiskCheckResult(
                isApproved = false,
                rejectionReason = "Customer copy trading is disabled",
                failureCode = "COPY_TRADING_DISABLED"
            )
        }

        if (follower.isPaused) {
            return RiskCheckResult(
                isApproved = false,
                rejectionReason = "Follower has paused copying for this strategy",
                failureCode = "FOLLOWER_PAUSED"
            )
        }

        // 5. Subscription Check
        if (!follower.subscriptionActive) {
            return RiskCheckResult(
                isApproved = false,
                rejectionReason = "Active subscription required for copying this strategy",
                failureCode = "SUBSCRIPTION_EXPIRED"
            )
        }

        // 6. User Emergency Stop Check
        if (follower.isEmergencyStopped || (riskLimits != null && riskLimits.isEmergencyStopped)) {
            return RiskCheckResult(
                isApproved = false,
                rejectionReason = "Emergency copy freeze is engaged. All copy executions blocked.",
                failureCode = "USER_EMERGENCY_STOP_ENGAGED"
            )
        }

        // 6b. Allowed Instruments Check
        if (!follower.allowedInstruments.contains(signal.underlying.name)) {
            return RiskCheckResult(
                isApproved = false,
                rejectionReason = "Instrument ${signal.underlying.name} is not in customer's allowed list (${follower.allowedInstruments})",
                failureCode = "INSTRUMENT_NOT_ALLOWED"
            )
        }

        // 7. Broker Connection Check
        if (brokerAccount == null || brokerAccount.status != BrokerAccountStatus.CONNECTED) {
            return RiskCheckResult(
                isApproved = false,
                rejectionReason = "Broker account is disconnected or authentication required",
                failureCode = "BROKER_DISCONNECTED"
            )
        }

        if (brokerAccount.tokenExpiry > 0L && brokerAccount.tokenExpiry < System.currentTimeMillis()) {
            return RiskCheckResult(
                isApproved = false,
                rejectionReason = "Broker access token has expired. Please re-authenticate.",
                failureCode = "BROKER_TOKEN_EXPIRED"
            )
        }

        // 8. Duplicate Order / Idempotency Check
        if (isDuplicateOrder) {
            return RiskCheckResult(
                isApproved = false,
                rejectionReason = "Duplicate copy order detected for this signal and follower",
                failureCode = "DUPLICATE_ORDER_REJECTED"
            )
        }

        // 9. Lot Size & Quantity Check
        if (!AllocationCalculator.isValidLotQuantity(proposedQuantity, signal.underlying)) {
            return RiskCheckResult(
                isApproved = false,
                rejectionReason = "Proposed quantity $proposedQuantity is not a valid lot multiple of ${signal.underlying.name} (${signal.underlying.lotSize})",
                failureCode = "INVALID_LOT_SIZE"
            )
        }

        // 10. Maximum Quantity Allowed per trade
        if (riskLimits != null) {
            val maxAllowedQuantity = riskLimits.maxQuantityLots * signal.underlying.lotSize
            if (proposedQuantity > maxAllowedQuantity) {
                return RiskCheckResult(
                    isApproved = false,
                    rejectionReason = "Order quantity ($proposedQuantity) exceeds maximum allowed risk limit ($maxAllowedQuantity units)",
                    failureCode = "EXCEEDS_MAX_QUANTITY_LIMIT"
                )
            }
        }

        // 11. Maximum Open Positions Check
        if (riskLimits != null && currentOpenPositionsCount >= riskLimits.maxOpenPositions && signal.side == OrderSide.BUY) {
            return RiskCheckResult(
                isApproved = false,
                rejectionReason = "Maximum open positions limit reached (${riskLimits.maxOpenPositions})",
                failureCode = "MAX_OPEN_POSITIONS_REACHED"
            )
        }

        // 12. Trade Capital / Margin Available Check
        val totalCapitalRequiredPaise = proposedQuantity * estimatedPricePaise
        if (signal.side == OrderSide.BUY && totalCapitalRequiredPaise > brokerAccount.availableFundsPaise) {
            return RiskCheckResult(
                isApproved = false,
                rejectionReason = "Insufficient funds: Trade requires ${MoneyFormatter.formatPaise(totalCapitalRequiredPaise)} but available is ${MoneyFormatter.formatPaise(brokerAccount.availableFundsPaise)}",
                failureCode = "INSUFFICIENT_FUNDS"
            )
        }

        // 13. Maximum Trade Amount Limit
        if (riskLimits != null && totalCapitalRequiredPaise > riskLimits.maxTradeAmountPaise) {
            return RiskCheckResult(
                isApproved = false,
                rejectionReason = "Trade value (${MoneyFormatter.formatPaise(totalCapitalRequiredPaise)}) exceeds user max trade limit (${MoneyFormatter.formatPaise(riskLimits.maxTradeAmountPaise)})",
                failureCode = "EXCEEDS_MAX_TRADE_AMOUNT"
            )
        }

        // 14. Maximum Daily Loss Limit
        if (riskLimits != null && riskLimits.dailyRealizedPnlPaise < -riskLimits.maxDailyLossPaise) {
            return RiskCheckResult(
                isApproved = false,
                rejectionReason = "Daily loss limit breached: Current loss ${MoneyFormatter.formatPaise(riskLimits.dailyRealizedPnlPaise)} exceeds threshold ${MoneyFormatter.formatPaise(riskLimits.maxDailyLossPaise)}",
                failureCode = "DAILY_LOSS_LIMIT_BREACHED"
            )
        }

        // 15. Valid Option Strike Check
        if (signal.strikePaise <= 0) {
            return RiskCheckResult(
                isApproved = false,
                rejectionReason = "Invalid strike price: ${signal.strikePaise}",
                failureCode = "INVALID_STRIKE_PRICE"
            )
        }

        return RiskCheckResult(isApproved = true)
    }
}
