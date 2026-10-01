# Indian Regulatory & Compliance Constraints

## 1. Regulatory Context (SEBI Guidelines)
- SEBI (Securities and Exchange Board of India) maintains strict regulations regarding algorithmic and automated retail trading.
- TradeMirror is engineered strictly as an educational / paper-trading and technical evaluation prototype.
- No live broker orders are placed by default.
- No withdrawal permissions are ever requested or stored.
- Broker login passwords are never accepted or stored (only scoped session tokens via OAuth/TOTP abstractions).

## 2. Order Tagging & Audit Trails
- Every order placed via the platform carries an immutable `orderTag = "TradeMirror"` and unique `idempotencyKey`.
- SEBI algo audit trails mandate recording the exact microsecond timestamp, algorithm identifier, client code, and client IP address.

## 3. Derivative Risk Disclosures
- Standard SEBI mandatory disclosure: *9 out of 10 individual traders in equity Derivatives segment incur net losses.*
- Hard stop limits: Daily maximum loss limits and maximum open positions are enforced at the pre-trade validation layer before reaching the broker adapter.
