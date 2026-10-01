# Testing & Verification Strategy

## 1. Test Suite Architecture
TradeMirror features a rigorous automated test suite testing financial invariants:

### Unit Tests
- **`AllocationCalculatorTest`**: Verifies exact lot size compliance (e.g. NIFTY 25 lots, BANKNIFTY 15 lots), budget capping, fractional lot truncation, and multiplier limits.
- **`RiskEngineTest`**: Verifies all 15 pre-trade checks:
  - Daily loss threshold breach
  - Maximum open positions limit
  - Insufficient cash margin
  - Strategy pause / User emergency stop
  - Global Kill Switch enforcement
- **`IdempotencyTest`**: Asserts that sending the same signal multiple times never creates duplicate orders.
- **`OrderStateMachineTest`**: Tests state transitions from `CREATED -> VALIDATING -> QUEUED -> SUBMITTING -> ACCEPTED -> FILLED / REJECTED`.

### Integration & Robolectric Tests
- **`CopyEngineIntegrationTest`**: Tests the complete end-to-end fan-out pipeline from signal broadcast to broker execution and position updating.
- **`ReconciliationTest`**: Tests detection of missing positions, unexpected broker trades, and quantity drifts.

## 2. Failure Mode Injections
Using the built-in Developer Test Panel:
- Broker API Timeout (simulates 504 Gateway Timeout)
- Broker Rejection (simulates exchange freeze limit)
- Partial Fill (simulates 50% liquidity match)
- Connection Drop (simulates invalid token)
