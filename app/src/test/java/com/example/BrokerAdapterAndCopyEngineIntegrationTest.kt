package com.example

import com.example.broker.MockBrokerAdapter
import com.example.broker.model.*
import com.example.data.local.entity.PositionEntity
import com.example.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BrokerAdapterAndCopyEngineIntegrationTest {

    private lateinit var broker: MockBrokerAdapter

    @Before
    fun setup() {
        broker = MockBrokerAdapter(BrokerCode.PAPER_BROKER, initialCashPaise = 10000000L) // ₹1,00,000
        broker.simulatedLatencyMs = 0L // zero latency for fast deterministic tests
    }

    @Test
    fun testMockBroker_NormalExecution_FillsOrderAndUpdatePositions() = runBlocking {
        val request = BrokerOrderRequest(
            symbol = "NIFTY 25000 CE",
            side = OrderSide.BUY,
            quantity = 50,
            orderType = OrderType.MARKET,
            pricePaise = 12000L, // ₹120.00
            idempotencyKey = "test_key_01"
        )

        val result = broker.placeOrder(request)

        assertTrue(result.success)
        assertEquals(OrderStatus.FILLED, result.status)
        assertEquals(50, result.executedQuantity)
        assertEquals(12000L, result.averagePricePaise)

        // Check positions in broker book
        val positions = broker.getPositions()
        assertEquals(1, positions.size)
        assertEquals("NIFTY 25000 CE", positions[0].symbol)
        assertEquals(50, positions[0].quantity)

        // Check cash update (initial 1,00,000 - (50 * 120 = 6,000) = 94,000)
        val funds = broker.getFunds()
        assertEquals(9400000L, funds.availableCashPaise)
    }

    @Test
    fun testMockBroker_FailureMode_Timeout() = runBlocking {
        broker.failureMode = SimulationFailureMode.TIMEOUT

        val request = BrokerOrderRequest(
            symbol = "NIFTY 25000 CE",
            side = OrderSide.BUY,
            quantity = 25,
            orderType = OrderType.MARKET,
            idempotencyKey = "test_key_timeout"
        )

        val result = broker.placeOrder(request)

        assertFalse(result.success)
        assertEquals(OrderStatus.FAILED, result.status)
        assertTrue(result.rejectionReason!!.contains("timeout"))
    }

    @Test
    fun testMockBroker_FailureMode_Rejection() = runBlocking {
        broker.failureMode = SimulationFailureMode.REJECTION

        val request = BrokerOrderRequest(
            symbol = "NIFTY 25000 CE",
            side = OrderSide.BUY,
            quantity = 25,
            orderType = OrderType.MARKET,
            idempotencyKey = "test_key_rej"
        )

        val result = broker.placeOrder(request)

        assertFalse(result.success)
        assertEquals(OrderStatus.REJECTED, result.status)
        assertTrue(result.rejectionReason!!.contains("Circuit limit reached"))
    }

    @Test
    fun testMockBroker_FailureMode_PartialFill() = runBlocking {
        broker.failureMode = SimulationFailureMode.PARTIAL_FILL

        val request = BrokerOrderRequest(
            symbol = "NIFTY 25000 CE",
            side = OrderSide.BUY,
            quantity = 50,
            orderType = OrderType.MARKET,
            pricePaise = 10000L,
            idempotencyKey = "test_key_partial"
        )

        val result = broker.placeOrder(request)

        assertTrue(result.success)
        assertEquals(OrderStatus.PARTIALLY_FILLED, result.status)
        assertEquals(25, result.executedQuantity) // 50% fill
    }

    @Test
    fun testReconciliation_BalancedState() = runBlocking {
        // Place order to create position in broker book
        broker.placeOrder(
            BrokerOrderRequest(
                symbol = "NIFTY 25000 CE",
                side = OrderSide.BUY,
                quantity = 50,
                orderType = OrderType.MARKET,
                pricePaise = 10000L,
                idempotencyKey = "k1"
            )
        )

        // Same position in internal DB
        val internalPositions = listOf(
            PositionEntity(
                followerId = "f1",
                brokerAccountId = "b1",
                strategyId = "s1",
                symbol = "NIFTY 25000 CE",
                underlying = UnderlyingIndex.NIFTY,
                optionType = OptionType.CE,
                strikePaise = 2500000L,
                expiry = "2026-10-29",
                side = OrderSide.BUY,
                quantity = 50,
                ltpPaise = 10000L,
                status = PositionStatus.OPEN
            )
        )

        val recon = broker.reconcilePositions(internalPositions)
        assertTrue(recon.isBalanced)
        assertEquals(0, recon.discrepancies.size)
    }

    @Test
    fun testReconciliation_DetectsQuantityMismatch() = runBlocking {
        // Broker has 50 units
        broker.placeOrder(
            BrokerOrderRequest(
                symbol = "NIFTY 25000 CE",
                side = OrderSide.BUY,
                quantity = 50,
                orderType = OrderType.MARKET,
                pricePaise = 10000L,
                idempotencyKey = "k2"
            )
        )

        // Internal records 25 units (quantity drift)
        val internalPositions = listOf(
            PositionEntity(
                followerId = "f1",
                brokerAccountId = "b1",
                strategyId = "s1",
                symbol = "NIFTY 25000 CE",
                underlying = UnderlyingIndex.NIFTY,
                optionType = OptionType.CE,
                strikePaise = 2500000L,
                expiry = "2026-10-29",
                side = OrderSide.BUY,
                quantity = 25, // MISMATCH
                ltpPaise = 10000L,
                status = PositionStatus.OPEN
            )
        )

        val recon = broker.reconcilePositions(internalPositions)
        assertFalse(recon.isBalanced)
        assertEquals(1, recon.discrepancies.size)
        assertEquals(DiscrepancyType.QUANTITY_MISMATCH, recon.discrepancies[0].discrepancyType)
    }
}
