# REST & WebSocket API Specification

## 1. Authentication Endpoints
- `POST /api/v1/auth/send-otp` -> Request 6-digit OTP to registered phone
- `POST /api/v1/auth/verify-otp` -> Verify OTP and issue JWT access token
- `POST /api/v1/auth/refresh` -> Refresh expired access token
- `POST /api/v1/auth/logout` -> Invalidate session tokens

## 2. Customer Endpoints
- `GET /api/v1/me` -> Profile, onboarding status, KYC state
- `GET /api/v1/me/dashboard` -> Summary of portfolio, daily P&L, active copy trades
- `GET /api/v1/brokers` -> List supported broker integrations
- `POST /api/v1/brokers/:brokerId/connect` -> Initiate broker OAuth/Token handshake
- `GET /api/v1/broker-accounts` -> Connected accounts, available funds, used margin
- `POST /api/v1/broker-accounts/:id/disconnect` -> Revoke broker session
- `GET /api/v1/strategies` -> Marketplace of active strategies
- `GET /api/v1/strategies/:id` -> Detailed statistics (Sharpe, Drawdown, Trades)
- `POST /api/v1/strategies/:id/follow` -> Configure copy settings and start mirroring
- `POST /api/v1/strategies/:id/stop` -> Cease copying strategy
- `GET /api/v1/orders` -> List mirror copy orders with lifecycle status
- `GET /api/v1/positions` -> Real-time open options positions
- `GET /api/v1/risk` -> Current risk limits and daily loss thresholds
- `PATCH /api/v1/risk` -> Update risk limits
- `POST /api/v1/risk/pause` -> Pause copy executions
- `POST /api/v1/risk/resume` -> Resume copy executions
- `POST /api/v1/risk/emergency-stop` -> Engage emergency freeze

## 3. Leader Endpoints
- `POST /api/v1/leader/strategies` -> Register a new algorithmic trading strategy
- `PATCH /api/v1/leader/strategies/:id` -> Update strategy parameters
- `POST /api/v1/leader/signals` -> Broadcast an options trading signal
- `GET /api/v1/leader/signals` -> Query history of published signals
- `GET /api/v1/leader/followers` -> List active copiers and allocation settings
- `GET /api/v1/leader/executions` -> Fan-out order execution audit trail

## 4. Admin & Compliance Endpoints
- `GET /api/v1/admin/users` -> Master directory of users and roles
- `GET /api/v1/admin/orders` -> Comprehensive cross-account order blotter
- `GET /api/v1/admin/positions` -> System-wide open options positions
- `GET /api/v1/admin/risk-alerts` -> Live feed of breached risk rules
- `GET /api/v1/admin/audit-logs` -> Structured logs with correlation IDs
- `POST /api/v1/admin/global-kill-switch` -> Global execution freeze / release
- `POST /api/v1/admin/reconciliation` -> Trigger full position reconciliation audit

## 5. WebSocket Event Channels
- `order.created`, `order.accepted`, `order.partially_filled`, `order.filled`, `order.rejected`
- `position.created`, `position.updated`, `position.closed`
- `signal.created`, `signal.completed`
- `risk.warning`, `risk.triggered`
- `broker.connected`, `broker.disconnected`
