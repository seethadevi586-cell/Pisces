# TradeMirror Database Schema

## 1. Monetary Storage Convention
All monetary values (prices, available margins, realized P&L, subscription fees, strike prices) are stored strictly as 64-bit integers in **Paise** (1 INR = 100 Paise).
This eliminates floating-point rounding errors and ensures auditability.

## 2. Entity Schemas & Tables

### `users`
- `id` (UUID PK): Unique identifier
- `name` (String): Full legal name
- `email` (String UNIQUE): Contact email
- `phone` (String): Mobile number for OTP
- `role` (Enum): `CUSTOMER`, `LEADER`, `ADMIN`
- `status` (Enum): `ACTIVE`, `SUSPENDED`, `PENDING_KYC`
- `panMasked` (String): Masked Indian PAN for tax & KYC
- `onboardingCompleted` (Boolean): Onboarding workflow state

### `broker_accounts`
- `id` (UUID PK): Account identifier
- `userId` (UUID FK): References `users.id`
- `brokerCode` (Enum): `ZERODHA`, `UPSTOX`, `ANGEL_ONE`, `GROWW`, `PAPER_BROKER`
- `brokerClientId` (String): External client code
- `status` (Enum): `CONNECTED`, `DISCONNECTED`, `TOKEN_EXPIRED`
- `availableFundsPaise` (Long): Current usable cash balance
- `usedMarginPaise` (Long): Margin utilized by open positions
- `isPaperMode` (Boolean): Paper trading flag

### `strategies`
- `id` (UUID PK): Strategy identifier
- `leaderId` (UUID FK): Strategy creator
- `name` (String): Strategy title
- `description` (String): Strategy thesis
- `underlying` (Enum): `NIFTY`, `BANKNIFTY`, `FINNIFTY`, `SENSEX`
- `minCapitalPaise` (Long): Recommended capital requirement
- `winRatePct` (Double): Historical win rate percentage
- `totalReturnPct` (Double): Cumulative return percentage
- `sharpeRatio` (Double): Risk-adjusted return metric
- `maxDrawdownPct` (Double): Maximum peak-to-trough drop
- `monthlySubscriptionFeePaise` (Long): Monthly fee in Paise
- `activeFollowersCount` (Int): Count of active copiers
- `status` (Enum): `ACTIVE`, `PAUSED`, `ARCHIVED`

### `strategy_followers`
- `id` (UUID PK)
- `strategyId` (UUID FK): Target strategy
- `followerId` (UUID FK): Customer user
- `brokerAccountId` (UUID FK): Trading account
- `allocationType` (Enum): `FIXED_AMOUNT` | `PERCENTAGE`
- `allocationValuePaise` (Long): Fixed amount cap
- `allocationPct` (Double): Percentage allocation
- `maxMultiplier` (Int): Risk multiplier cap (default 1)
- `isPaused` (Boolean): Follower temporary pause switch
- `subscriptionActive` (Boolean): Subscription validity

### `risk_limits`
- `id` (UUID PK)
- `followerId` (UUID FK UNIQUE): References user
- `maxDailyLossPaise` (Long): Hard stop limit for daily loss
- `maxOpenPositions` (Int): Limit of concurrent active trades
- `maxTradeAmountPaise` (Long): Limit of capital in a single trade
- `maxQuantityLots` (Int): Limit on lot count
- `isEmergencyStopped` (Boolean): Emergency stop flag
- `dailyRealizedPnlPaise` (Long): Today's realized P&L

### `signals`
- `id` (UUID PK): Signal identifier
- `strategyId` (UUID FK): Originating strategy
- `symbol` (String): e.g. "NIFTY 25000 CE"
- `underlying` (Enum): `NIFTY`, `BANKNIFTY`, `FINNIFTY`
- `instrumentType` (Enum): `OPTIDX`
- `expiry` (String): Option expiry date (YYYY-MM-DD)
- `strikePaise` (Long): Strike price in Paise
- `optionType` (Enum): `CE`, `PE`
- `side` (Enum): `BUY`, `SELL`
- `quantityLots` (Int): Quantity in lots
- `orderType` (Enum): `MARKET`, `LIMIT`
- `limitPricePaise` (Long Nullable)
- `signalType` (Enum): `ENTRY`, `EXIT`, `ADJUST`
- `timestamp` (Long): Signal generation time
- `status` (Enum): `COMPLETED`, `CANCELLED`

### `copy_orders`
- `id` (UUID PK): Mirror order ID
- `signalId` (UUID FK): Master signal
- `strategyId` (UUID FK)
- `followerId` (UUID FK)
- `brokerAccountId` (UUID FK)
- `symbol` (String)
- `side` (Enum): `BUY`, `SELL`
- `requestedQuantity` (Int): Calculated lot-sized quantity
- `executedQuantity` (Int): Executed broker quantity
- `requestedPricePaise` (Long)
- `averagePricePaise` (Long): Filled price
- `brokerOrderId` (String Nullable): Broker order reference
- `status` (Enum): `CREATED`, `VALIDATING`, `QUEUED`, `SUBMITTING`, `ACCEPTED`, `PARTIALLY_FILLED`, `FILLED`, `REJECTED`, `CANCELLED`, `FAILED`
- `rejectionReason` (String Nullable)
- `idempotencyKey` (String UNIQUE): `strategyId + signalId + followerId + executionVersion`
- `executionLatencyMs` (Long): Roundtrip execution time

### `positions`
- `id` (UUID PK)
- `followerId` (UUID FK)
- `brokerAccountId` (UUID FK)
- `symbol` (String)
- `quantity` (Int): Net open quantity
- `averageBuyPricePaise` (Long)
- `ltpPaise` (Long): Last traded price
- `unrealizedPnlPaise` (Long)
- `realizedPnlPaise` (Long)
- `status` (Enum): `OPEN`, `CLOSED`

### `audit_logs`
- `id` (UUID PK)
- `correlationId` (String)
- `userId` (String)
- `action` (String)
- `entityType` (String)
- `entityId` (String)
- `detailsJson` (String)
- `timestamp` (Long)
