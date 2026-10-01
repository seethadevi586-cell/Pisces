package com.example.engine

import com.example.data.local.dao.TradingDao
import com.example.data.local.entity.LeaderTradeEventEntity
import com.example.data.local.entity.SignalEntity
import com.example.data.local.entity.TradeSignalEntity
import com.example.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class SignalNormalizer(
    private val dao: TradingDao
) {

    /**
     * Normalizes a validated leader trade event into a standardized PISCES TradeSignal.
     * Only executed/filled trades generate copy signals.
     */
    suspend fun normalize(event: LeaderTradeEventEntity): TradeSignalEntity = withContext(Dispatchers.IO) {
        val signal = normalizeLeaderTrade(event)
        dao.insertTradeSignal(signal)

        // Also save backwards-compatible SignalEntity for existing screens
        val legacySignal = SignalEntity(
            id = signal.signalId,
            strategyId = event.strategyId,
            symbol = event.symbol,
            underlying = signal.underlying,
            instrumentType = InstrumentType.OPTIDX,
            expiry = "2026-10-29",
            strikePaise = 2500000L,
            optionType = signal.optionType,
            side = event.transactionType,
            quantityLots = (event.filledQuantity / signal.underlying.lotSize.coerceAtLeast(1)).coerceAtLeast(1),
            orderType = event.orderType,
            limitPricePaise = event.averagePricePaise,
            signalType = if (event.transactionType == OrderSide.BUY) SignalType.ENTRY else SignalType.EXIT,
            timestamp = event.tradeTime,
            status = SignalStatus.COMPLETED
        )
        dao.insertSignal(legacySignal)

        signal
    }

    companion object {
        fun normalizeLeaderTrade(event: LeaderTradeEventEntity): TradeSignalEntity {
            val signalId = "sig_${UUID.randomUUID().toString().take(10)}"

            val underlying = when {
                event.symbol.contains("BANKNIFTY") -> UnderlyingIndex.BANKNIFTY
                event.symbol.contains("FINNIFTY") -> UnderlyingIndex.FINNIFTY
                event.symbol.contains("SENSEX") -> UnderlyingIndex.SENSEX
                else -> UnderlyingIndex.NIFTY
            }

            val optionType = when {
                event.symbol.endsWith("PE") -> OptionType.PE
                else -> OptionType.CE
            }

            return TradeSignalEntity(
                signalId = signalId,
                strategyId = event.strategyId,
                leaderTradeId = event.eventId,
                instrument = event.instrument,
                exchange = event.exchange,
                transactionType = event.transactionType,
                orderType = event.orderType,
                quantity = event.quantity,
                filledQuantity = event.filledQuantity,
                pricePaise = event.averagePricePaise,
                timestamp = event.tradeTime,
                signalVersion = 1,
                status = SignalStatus.COMPLETED,
                underlying = underlying,
                optionType = optionType,
                createdAt = System.currentTimeMillis()
            )
        }
    }
}
