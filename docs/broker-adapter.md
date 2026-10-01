# Broker Adapter & Integration Boundary

## 1. Adapter Interface Specification
The system isolates all broker-specific mechanics behind the `BrokerAdapter` interface:

```kotlin
interface BrokerAdapter {
    suspend fun connect(clientId: String, authToken: String): BrokerConnectionResult
    suspend fun disconnect(): Boolean
    suspend fun getAccountStatus(): BrokerAccountStatus
    suspend fun getFunds(): BrokerFunds
    suspend fun getPositions(): List<BrokerPosition>
    suspend fun getOrders(): List<BrokerOrder>
    suspend fun placeOrder(request: BrokerOrderRequest): BrokerExecutionResult
    suspend fun cancelOrder(brokerOrderId: String): Boolean
    suspend fun getOrderStatus(brokerOrderId: String): OrderStatus
    suspend fun reconcilePositions(internalPositions: List<PositionEntity>): ReconciliationResult
}
```

## 2. MockBrokerAdapter (Paper Trading Sandbox)
The default adapter implements a high-fidelity local paper-trading simulation engine:
- Maintains simulated cash balances, margins, and collateral.
- Maintains in-memory position books and order blottes.
- Simulates realistic execution latency (default: 100ms).
- Provides a Developer Test Panel for injecting edge cases:
  1. `TIMEOUT`: Simulates HTTP 504 / broker socket drop.
  2. `REJECTION`: Simulates RMS / circuit limit refusal.
  3. `PARTIAL_FILL`: Simulates 50% liquidity fill.
  4. `BROKER_DISCONNECT`: Simulates sudden invalidation of JWT session token.
  5. `INSUFFICIENT_MARGIN`: Simulates unexpected leverage shrinkage.

## 3. Pluggability for Future Live Brokers
To connect an exchange-registered Indian broker (e.g., Zerodha Kite Connect, Upstox API, Angel One SmartAPI, Groww API):
1. Create a class implementing `BrokerAdapter` (e.g. `ZerodhaBrokerAdapter`).
2. Pass the broker's TOTP / OAuth2 access token securely.
3. Translate broker response codes to standard `OrderStatus` enums.
4. No changes are required in `CopyEngine`, `RiskEngine`, or `ReconciliationEngine`.
