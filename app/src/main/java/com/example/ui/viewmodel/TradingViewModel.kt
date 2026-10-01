package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.broker.model.*
import com.example.data.local.entity.*
import com.example.data.repository.Section33TestResult
import com.example.data.repository.SeedData
import com.example.data.repository.TradingRepository
import com.example.domain.model.*
import com.example.engine.SignalExecutionReport
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppRoleView {
    CUSTOMER,
    LEADER,
    ADMIN,
    DEV_TEST_PANEL
}

class TradingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TradingRepository.getInstance(application)

    // Splash & Auth State
    private val _isSplashVisible = MutableStateFlow(true)
    val isSplashVisible: StateFlow<Boolean> = _isSplashVisible.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> = _otpSent.asStateFlow()

    private val _loginInput = MutableStateFlow("arjun.mehta@pisces.trade")
    val loginInput: StateFlow<String> = _loginInput.asStateFlow()

    private val _otpInput = MutableStateFlow("")
    val otpInput: StateFlow<String> = _otpInput.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Current Role View
    private val _currentRole = MutableStateFlow(AppRoleView.CUSTOMER)
    val currentRole: StateFlow<AppRoleView> = _currentRole.asStateFlow()

    // Customer bottom nav tab (0: Dashboard, 1: Marketplace, 2: Positions, 3: Orders, 4: Profile)
    private val _customerTab = MutableStateFlow(0)
    val customerTab: StateFlow<Int> = _customerTab.asStateFlow()

    // Leader sub-tab (0: Overview, 1: Generate Signal, 2: Followers, 3: Strategies)
    private val _leaderTab = MutableStateFlow(0)
    val leaderTab: StateFlow<Int> = _leaderTab.asStateFlow()

    // Admin sub-tab (0: Overview, 1: Users, 2: Master Blotter, 3: Risk & Audit, 4: Reconciliation)
    private val _adminTab = MutableStateFlow(0)
    val adminTab: StateFlow<Int> = _adminTab.asStateFlow()

    // Selected Strategy for Details Modal
    private val _selectedStrategyDetails = MutableStateFlow<StrategyEntity?>(null)
    val selectedStrategyDetails: StateFlow<StrategyEntity?> = _selectedStrategyDetails.asStateFlow()

    // Emergency Freeze Confirmation State
    private val _showFreezeDialog = MutableStateFlow(false)
    val showFreezeDialog: StateFlow<Boolean> = _showFreezeDialog.asStateFlow()

    // Active user IDs
    val activeFollowerId = SeedData.DEMO_FOLLOWER_ME.id
    val activeLeaderId = SeedData.LEADER_USER.id

    // Observables
    val activeStrategies: StateFlow<List<StrategyEntity>> = repository.observeActiveStrategies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val leaderStrategies: StateFlow<List<StrategyEntity>> = repository.observeStrategiesByLeader(activeLeaderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerPositions: StateFlow<List<PositionEntity>> = repository.observeOpenPositions(activeFollowerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPositions: StateFlow<List<PositionEntity>> = repository.observeAllOpenPositions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerOrders: StateFlow<List<CopyOrderEntity>> = repository.observeOrdersForFollower(activeFollowerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrders: StateFlow<List<CopyOrderEntity>> = repository.observeAllOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val signals: StateFlow<List<SignalEntity>> = repository.observeSignals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerFollowings: StateFlow<List<StrategyFollowerEntity>> = repository.observeFollowings(activeFollowerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerBrokerAccounts: StateFlow<List<BrokerAccountEntity>> = repository.observeBrokerAccounts(activeFollowerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBrokerAccounts: StateFlow<List<BrokerAccountEntity>> = repository.observeAllBrokerAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val leaderTrades: StateFlow<List<LeaderTradeEventEntity>> = repository.observeLeaderTrades(activeLeaderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLeaderTrades: StateFlow<List<LeaderTradeEventEntity>> = repository.observeAllLeaderTrades()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val leaderBrokerAccounts: StateFlow<List<BrokerAccountEntity>> = repository.observeBrokerAccounts(activeLeaderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerRiskLimits: StateFlow<RiskLimitEntity?> = repository.observeRiskLimits(activeFollowerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.observeAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.observeNotifications(activeFollowerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val systemConfig: StateFlow<SystemConfigEntity?> = repository.observeSystemConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allUsers: StateFlow<List<UserEntity>> = repository.observeAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Transient States
    private val _reconciliationResult = MutableStateFlow<ReconciliationResult?>(null)
    val reconciliationResult: StateFlow<ReconciliationResult?> = _reconciliationResult.asStateFlow()

    private val _lastExecutionReport = MutableStateFlow<SignalExecutionReport?>(null)
    val lastExecutionReport: StateFlow<SignalExecutionReport?> = _lastExecutionReport.asStateFlow()

    private val _cujTestResult = MutableStateFlow<Section33TestResult?>(null)
    val cujTestResult: StateFlow<Section33TestResult?> = _cujTestResult.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeSeedDataIfNeeded()
            // Minimal splash display
            delay(1200)
            _isSplashVisible.value = false
        }
    }

    // Offline & Network State
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    // In-flight signal idempotency set
    private val inFlightSignalKeys = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun toggleOffline(offline: Boolean) {
        _isOffline.value = offline
        _statusMessage.value = if (offline) "Offline mode activated" else "Connection re-established"
    }

    fun dismissSplash() {
        _isSplashVisible.value = false
    }

    fun setLoginInput(input: String) {
        _loginInput.value = input
        _loginError.value = null
    }

    fun setOtpInput(input: String) {
        _otpInput.value = input
        _loginError.value = null
    }

    fun sendOtp() {
        if (_isBusy.value) return
        if (_loginInput.value.isBlank()) {
            _loginError.value = "Please enter a valid mobile number or email"
            return
        }
        _isBusy.value = true
        viewModelScope.launch {
            delay(500)
            _isBusy.value = false
            _otpSent.value = true
            _statusMessage.value = "6-digit OTP sent to ${_loginInput.value} (Demo code: 123456)"
        }
    }

    fun verifyOtp() {
        if (_isBusy.value) return
        if (_otpInput.value.length < 4) {
            _loginError.value = "Please enter the verification OTP"
            return
        }
        _isBusy.value = true
        viewModelScope.launch {
            delay(500)
            _isBusy.value = false
            _isLoggedIn.value = true
            _statusMessage.value = "Authenticated as Arjun Mehta (PISCES)"
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _otpSent.value = false
        _otpInput.value = ""
        _statusMessage.value = "Logged out from PISCES session"
    }

    fun selectRole(role: AppRoleView) {
        _currentRole.value = role
    }

    fun setCustomerTab(tab: Int) {
        if (_customerTab.value != tab) {
            _customerTab.value = tab
        }
    }

    fun setLeaderTab(tab: Int) {
        if (_leaderTab.value != tab) {
            _leaderTab.value = tab
        }
    }

    fun setAdminTab(tab: Int) {
        if (_adminTab.value != tab) {
            _adminTab.value = tab
        }
    }

    fun openStrategyDetails(strategy: StrategyEntity?) {
        _selectedStrategyDetails.value = strategy
    }

    fun promptEmergencyFreezeConfirmation() {
        _showFreezeDialog.value = true
    }

    fun dismissFreezeDialog() {
        _showFreezeDialog.value = false
    }

    fun confirmEmergencyFreeze(freeze: Boolean) {
        _showFreezeDialog.value = false
        toggleEmergencyStop(freeze)
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun followStrategy(
        strategyId: String,
        brokerAccountId: String,
        allocationType: AllocationType,
        allocationValuePaise: Long,
        allocationPct: Double,
        maxMultiplier: Int
    ) {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                repository.followStrategy(
                    followerId = activeFollowerId,
                    strategyId = strategyId,
                    brokerAccountId = brokerAccountId,
                    allocationType = allocationType,
                    allocationValuePaise = allocationValuePaise,
                    allocationPct = allocationPct,
                    maxMultiplier = maxMultiplier
                )
                _statusMessage.value = "Subscribed to strategy. Trade mirroring active."
            } catch (e: Exception) {
                _statusMessage.value = "Failed to subscribe: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun createStrategy(
        name: String,
        description: String,
        underlying: UnderlyingIndex,
        minCapitalRupees: Double,
        monthlyFeeRupees: Double,
        targetWinRatePct: Double = 68.0,
        expectedReturnPct: Double = 42.0
    ) {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                val cleanId = "strat_" + name.lowercase().replace(Regex("[^a-z0-9]"), "_").take(15) + "_${System.currentTimeMillis() % 10000}"
                val newStrategy = StrategyEntity(
                    id = cleanId,
                    leaderId = activeLeaderId,
                    leaderName = "Vikram Singhania",
                    name = name.trim(),
                    description = description.trim(),
                    underlying = underlying,
                    minCapitalPaise = (minCapitalRupees * 100).toLong().coerceAtLeast(1000000L),
                    winRatePct = targetWinRatePct,
                    totalReturnPct = expectedReturnPct,
                    sharpeRatio = 2.15,
                    maxDrawdownPct = 8.0,
                    monthlySubscriptionFeePaise = (monthlyFeeRupees * 100).toLong().coerceAtLeast(0L),
                    activeFollowersCount = 0,
                    status = StrategyStatus.ACTIVE,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                repository.createStrategy(newStrategy)
                _statusMessage.value = "Strategy '${name.trim()}' launched! Now available in Marketplace & Signal Studio."
            } catch (e: Exception) {
                _statusMessage.value = "Failed to create strategy: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun stopFollowing(strategyId: String) {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                repository.stopFollowing(activeFollowerId, strategyId)
                _statusMessage.value = "Unsubscribed from strategy."
            } catch (e: Exception) {
                _statusMessage.value = "Error: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun toggleFollowerPause(strategyId: String, isPaused: Boolean) {
        viewModelScope.launch {
            repository.toggleFollowerPause(activeFollowerId, strategyId, isPaused)
            _statusMessage.value = if (isPaused) "Copying paused for strategy." else "Copying resumed."
        }
    }

    fun toggleEmergencyStop(stopped: Boolean) {
        viewModelScope.launch {
            repository.toggleEmergencyStop(activeFollowerId, stopped)
            _statusMessage.value = if (stopped) "EMERGENCY FREEZE ENGAGED: Automated copy executions blocked." else "Emergency freeze released: Mirroring active."
        }
    }

    fun updateRiskLimits(maxDailyLossPaise: Long, maxPositions: Int, maxTradeAmountPaise: Long, maxLots: Int) {
        viewModelScope.launch {
            val current = customerRiskLimits.value ?: RiskLimitEntity(followerId = activeFollowerId)
            repository.updateRiskLimits(
                current.copy(
                    maxDailyLossPaise = maxDailyLossPaise,
                    maxOpenPositions = maxPositions,
                    maxTradeAmountPaise = maxTradeAmountPaise,
                    maxQuantityLots = maxLots
                )
            )
            _statusMessage.value = "Risk limits updated."
        }
    }

    fun broadcastSignal(
        strategyId: String,
        symbol: String,
        underlying: UnderlyingIndex,
        optionType: OptionType,
        strikePaise: Long,
        expiry: String,
        side: OrderSide,
        quantityLots: Int,
        orderType: OrderType,
        limitPricePaise: Long?
    ) {
        if (_isBusy.value) return
        val signalFingerprint = "$strategyId:$symbol:$side:$quantityLots:${System.currentTimeMillis() / 3000}"
        if (!inFlightSignalKeys.add(signalFingerprint)) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                val signal = SignalEntity(
                    strategyId = strategyId,
                    symbol = symbol,
                    underlying = underlying,
                    instrumentType = InstrumentType.OPTIDX,
                    expiry = expiry,
                    strikePaise = strikePaise,
                    optionType = optionType,
                    side = side,
                    quantityLots = quantityLots,
                    orderType = orderType,
                    limitPricePaise = limitPricePaise,
                    signalType = SignalType.ENTRY,
                    timestamp = System.currentTimeMillis(),
                    status = SignalStatus.COMPLETED
                )
                val report = repository.broadcastSignal(signal)
                _lastExecutionReport.value = report
                _statusMessage.value = "Signal processed: ${report.successfulExecutions} filled, ${report.rejectedExecutions} rejected across ${report.totalFollowersTargeted} followers."
            } catch (e: Exception) {
                _statusMessage.value = "Signal broadcast failed: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
                inFlightSignalKeys.remove(signalFingerprint)
            }
        }
    }

    fun toggleGlobalKillSwitch(isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleGlobalKillSwitch(isActive)
            _statusMessage.value = if (isActive) "GLOBAL KILL SWITCH ENGAGED: All platform order routing frozen." else "Global kill switch released."
        }
    }

    fun setSimulationControls(mode: SimulationFailureMode, latencyMs: Long) {
        viewModelScope.launch {
            repository.setDeveloperSimulationControls(mode, latencyMs)
            _statusMessage.value = "Paper simulation mode: ${mode.name} (${latencyMs}ms latency)"
        }
    }

    fun triggerReconciliation() {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                val res = repository.triggerReconciliationAudit()
                _reconciliationResult.value = res
                _statusMessage.value = if (res.isBalanced) "Audit passed: ${res.totalPositionsAudited} positions reconciled." else "Discrepancy: ${res.discrepancies.size} mismatches found."
            } catch (e: Exception) {
                _statusMessage.value = "Reconciliation audit failed: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun connectBroker(brokerCode: BrokerCode, clientId: String, token: String) {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                repository.connectBrokerAccount(activeFollowerId, brokerCode, clientId, token)
                _statusMessage.value = "Connected to ${brokerCode.displayName}."
            } catch (e: Exception) {
                _statusMessage.value = "Connection failed: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun disconnectBroker(brokerCode: BrokerCode) {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                repository.disconnectBrokerAccount(activeFollowerId, brokerCode)
                _statusMessage.value = "${brokerCode.name} disconnected."
            } catch (e: Exception) {
                _statusMessage.value = "Error disconnecting: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch {
            repository.markNotificationsRead(activeFollowerId)
        }
    }

    fun executeLeaderTrade(
        strategyId: String,
        brokerCode: BrokerCode,
        symbol: String,
        side: OrderSide,
        quantity: Int,
        orderType: OrderType = OrderType.MARKET,
        pricePaise: Long = 14500L
    ) {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                val report = repository.executeLeaderBrokerTrade(
                    leaderId = activeLeaderId,
                    strategyId = strategyId,
                    brokerCode = brokerCode,
                    symbol = symbol,
                    side = side,
                    quantity = quantity,
                    orderType = orderType,
                    pricePaise = pricePaise
                )
                _lastExecutionReport.value = report
                _statusMessage.value = "Leader trade executed in ${brokerCode.displayName}! Detected and copied to ${report.successfulExecutions} followers."
            } catch (e: Exception) {
                _statusMessage.value = "Leader execution failed: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun updateCopySettings(
        strategyId: String,
        copyEnabled: Boolean,
        mode: AllocationType,
        fixedQty: Int,
        ratio: Double,
        pct: Double,
        capAmountPaise: Long,
        maxQty: Int,
        maxCapPaise: Long,
        maxDailyLossPaise: Long,
        maxPositions: Int,
        emergencyFreeze: Boolean
    ) {
        viewModelScope.launch {
            repository.updateCustomerCopySettings(
                followerId = activeFollowerId,
                strategyId = strategyId,
                copyTradingEnabled = copyEnabled,
                allocationType = mode,
                fixedQuantityUnits = fixedQty,
                multiplierRatio = ratio,
                allocationPct = pct,
                allocationValuePaise = capAmountPaise,
                maxQuantityUnits = maxQty,
                maxCapitalPerTradePaise = maxCapPaise,
                maxDailyLossPaise = maxDailyLossPaise,
                maxOpenPositions = maxPositions,
                isEmergencyStopped = emergencyFreeze
            )
            _statusMessage.value = "Copy settings updated: Mode = ${mode.displayName}"
        }
    }

    fun runSection33Test() {
        if (_isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                val res = repository.runSection33CUJTest()
                _cujTestResult.value = res
                _statusMessage.value = if (res.isAllPassed) "Section 33 CUJ Test: 100% PASSED!" else "Section 33 CUJ Test completed with issues."
            } catch (e: Exception) {
                _statusMessage.value = "Test execution failed: ${e.localizedMessage}"
            } finally {
                _isBusy.value = false
            }
        }
    }
}
