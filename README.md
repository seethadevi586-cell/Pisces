# TradeMirror - Indian Options Copy-Trading Platform

TradeMirror is a production-oriented prototype of an Indian options copy-trading platform inspired by the technical concept of leader/follower trade mirroring.

> **CRITICAL DISCLAIMER**:
> This is an engineering prototype and technical research project, not investment advice. No profits are promised or implied. It operates exclusively in an isolated paper-trading sandbox (`MockBrokerAdapter`) with strict pre-trade risk validation and emergency controls.

---

## 1. Quick Start & Setup
1. **Prerequisites**: Android SDK 36, JDK 17/21, Gradle.
2. **Environment Variables**: See `.env.example`.
3. **Database Setup**: Embedded Room SQLite database with full migration fallback, foreign keys, and unique indexes.
4. **Paper Trading Sandbox**: Built-in `MockBrokerAdapter` with in-memory order books, realistic latency, and failure injection.
5. **Run Applet**: Built with Kotlin and Jetpack Compose.
6. **Switch Roles**: Use the interactive Role Switcher in the top bar to switch between **Customer**, **Leader**, **Admin**, and **Dev Test Panel**.
7. **Run Tests**: Execute `gradle :app:testDebugUnitTest`.

---

## 2. Key Architecture Components
- **Customer Portal**: Strategy marketplace, one-click copy allocation, live options positions, P&L, emergency stop.
- **Leader Dashboard**: Strategy performance analytics, interactive options signal generation studio, follower tracking.
- **Admin Compliance Suite**: Global execution kill switch, cross-account order blotter, risk alerts, automated position reconciliation.
- **MockBrokerAdapter**: Simulated paper broker supporting Zerodha, Upstox, Angel One, and Groww API abstractions.
- **Dev Test Panel**: Inject timeouts, rejections, partial fills, and connection drops to verify risk and copy engine resilience.
