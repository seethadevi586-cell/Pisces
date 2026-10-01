# TradeMirror Architecture

## 1. Overview
TradeMirror is a production-oriented prototype of an Indian options copy-trading platform engineered around the technical concept of leader/follower trade mirroring. It provides strict regulatory boundaries, paper trading sandbox isolation, pre-trade risk engines, idempotent execution queues, and position reconciliation.

## 2. Core Pillars
- **Zero Real-Money Risk Initially**: Integrates via a clean `BrokerAdapter` interface connected to an isolated paper trading simulator (`MockBrokerAdapter`).
- **Precision Indian Derivatives Handling**: Strictly handles Indian Options (NIFTY 50, BANK NIFTY, FINNIFTY, SENSEX) with exact lot sizing (25, 15, 25, 10 units), strike tick constraints, and integer Paise currency calculations (1 INR = 100 Paise).
- **Fan-Out Copy Engine**: When a strategy leader submits a signal, the Copy Engine computes exact allocation lots per active follower, runs 15+ pre-trade risk checks, builds deterministic idempotency keys, routes orders through the broker adapter, and persists atomic position changes.
- **Fail-Safe Emergency Controls**:
  - Global Kill Switch (Admin freeze on all orders)
  - Strategy Stop (Leader halt)
  - User Emergency Stop (Instant square-off & copy freeze)
- **Continuous Position Reconciliation**: Compares internal database positions with the broker’s execution book to catch missing positions, quantity drifts, or stale orders.

## 3. High-Level System Flow
```
[Leader Generates Signal]
          │
          ▼
   [Copy Engine]
          │
   ├─► Verify Strategy & User Status
   ├─► Fetch Active Followers & Subscriptions
   ├─► Compute Allocations (Fixed / Percentage)
   ├─► Run Risk Engine (15 Validation Rules)
   ├─► Build Idempotency Key (Strategy+Signal+Follower+v1)
          │
   ┌──────┴──────┐
[Risk PASS]  [Risk REJECT]
   │             │
   ▼             ▼
[BrokerAdapter] [Record Rejection & Notify]
   │
   ▼
[MockBrokerAdapter / Live Broker]
   │
   ├─► Receive Execution Result
   ├─► Update CopyOrder State (CREATED -> QUEUED -> SUBMITTING -> FILLED)
   ├─► Update Follower Position & Margin
   ├─► Record Audit Log (Correlation ID)
   └─► Emit Push Notification
```
