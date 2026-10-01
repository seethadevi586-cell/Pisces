# DevSecOps & Security Architecture

## 1. Zero Secrets in Client Code
- No API secrets, broker trading passwords, or private keys exist in the repository or client code.
- Broker credentials use OAuth authorization code flows and short-lived session tokens.

## 2. Idempotency Guarantees
Every trade execution derives a deterministic idempotency key:
`key = strategyId + "_" + signalId + "_" + followerId + "_v1"`
Database enforces a UNIQUE constraint on `copy_orders(idempotencyKey)`.
If a signal is broadcast multiple times (e.g. network retry), duplicate order placement is mathematically prevented.

## 3. Pre-Trade Risk Engine
The risk engine acts as a non-bypassable barrier. Any order that violates lot sizes, margin limits, daily loss limits, or emergency kill switches is immediately flagged `REJECTED` and audited.

## 4. Role-Based Access Control (RBAC)
- `CUSTOMER`: Can only inspect their own positions, orders, broker settings, and risk limits.
- `LEADER`: Can create/manage their strategy, view followers, broadcast signals, and check execution performance.
- `ADMIN`: Has full visibility into system health, audit blotter, reconciliation status, and controls the Global Kill Switch.
