package com.example.domain.model

import java.text.NumberFormat
import java.util.Locale

/**
 * Standard enums for Indian Options Copy-Trading Platform (PISCES)
 */
enum class UserRole {
    CUSTOMER,
    LEADER,
    ADMIN
}

enum class UserStatus {
    ACTIVE,
    SUSPENDED,
    PENDING_KYC
}

enum class OrderSide {
    BUY,
    SELL
}

enum class OptionType {
    CE, // Call Option
    PE  // Put Option
}

enum class UnderlyingIndex(val displayName: String, val lotSize: Int, val tickSizePaise: Long, val exchange: String) {
    NIFTY("NIFTY 50", 25, 5, "NFO"),          // 25 per lot
    BANKNIFTY("BANK NIFTY", 15, 5, "NFO"),    // 15 per lot
    FINNIFTY("NIFTY FINANCIAL", 25, 5, "NFO"),// 25 per lot
    SENSEX("BSE SENSEX", 10, 5, "BFO")        // 10 per lot
}

enum class InstrumentType {
    OPTIDX, // Index Option
    FUTIDX, // Index Future
    OPTSTK  // Stock Option
}

enum class OrderType {
    MARKET,
    LIMIT,
    STOP_LOSS,
    STOP_LOSS_MARKET
}

enum class SignalType {
    ENTRY,
    EXIT,
    ADJUST
}

enum class SignalStatus {
    DRAFT,
    SUBMITTED,
    EXECUTING,
    COMPLETED,
    CANCELLED
}

/**
 * Normalized PISCES Order Statuses
 */
enum class OrderStatus {
    CREATED,
    VALIDATING,
    RISK_REJECTED,
    SUBMITTING,
    SUBMITTED,
    OPEN,
    PARTIALLY_FILLED,
    FILLED,
    CANCEL_PENDING,
    CANCELLED,
    REJECTED,
    FAILED,
    UNKNOWN;

    companion object {
        // Backwards compatibility aliases
        val QUEUED = VALIDATING
        val ACCEPTED = SUBMITTED
        val CANCEL_REQUESTED = CANCEL_PENDING
    }
}

enum class PositionStatus {
    OPEN,
    CLOSED
}

enum class AllocationType(val displayName: String) {
    FIXED_QUANTITY("Fixed Quantity"),           // e.g. exactly 25 units
    RATIO("Multiplier Ratio"),                   // e.g. 0.50x of leader quantity
    CAPITAL_PERCENTAGE("Capital Percentage"),    // e.g. 20% of available margin
    FIXED_CAPITAL_AMOUNT("Fixed Capital Amount"),// e.g. Max ₹50,000 per trade
    FIXED_AMOUNT("Fixed Capital Amount"),        // Backwards compatibility
    PERCENTAGE("Capital Percentage")             // Backwards compatibility
}

enum class BrokerCode(val displayName: String, val isOfficialSupported: Boolean) {
    ANGEL_ONE("Angel One SmartAPI", true),
    UPSTOX("Upstox Developer API", true),
    PAPER_BROKER("PISCES Paper Sandbox", true),
    ZERODHA("Zerodha Kite Connect", false),
    GROWW("Groww API", false)
}

enum class BrokerAccountStatus {
    CONNECTED,
    DISCONNECTED,
    AUTHENTICATION_REQUIRED,
    TOKEN_EXPIRED,
    ERROR,
    SYNCING
}

enum class StrategyStatus {
    ACTIVE,
    PAUSED,
    ARCHIVED
}

enum class NotificationType {
    TRADE,
    RISK,
    SYSTEM,
    SUBSCRIPTION
}

enum class SimulationFailureMode {
    NONE,
    TIMEOUT,
    REJECTION,
    PARTIAL_FILL,
    BROKER_DISCONNECT,
    INSUFFICIENT_MARGIN,
    MARKET_CLOSED
}

enum class NormalizedEventType {
    MARKET_DATA,
    ORDER_UPDATE,
    TRADE_UPDATE,
    POSITION_UPDATE,
    CONNECTION_HEALTH
}

/**
 * Money representation utilities (Amounts are strictly in Indian Paise to avoid floating-point errors)
 * 1 INR = 100 Paise
 */
object MoneyFormatter {
    private val indianLocale = Locale("en", "IN")
    private val formatter = NumberFormat.getCurrencyInstance(indianLocale).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }

    fun formatPaise(paise: Long): String {
        val rupees = paise / 100.0
        return try {
            formatter.format(rupees)
        } catch (_: Exception) {
            val prefix = if (rupees < 0) "-₹" else "₹"
            val absVal = Math.abs(rupees)
            String.format(Locale.US, "%s%.2f", prefix, absVal)
        }
    }

    fun formatPaiseSigned(paise: Long): String {
        val formatted = formatPaise(paise)
        return if (paise > 0) "+$formatted" else formatted
    }

    fun rupeesToPaise(rupees: Double): Long {
        return (rupees * 100).toLong()
    }
}
