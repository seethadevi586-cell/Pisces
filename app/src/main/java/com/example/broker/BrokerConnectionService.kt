package com.example.broker

import com.example.broker.adapter.AngelOneAdapter
import com.example.broker.adapter.UpstoxAdapter
import com.example.broker.model.*
import com.example.broker.security.CredentialEncryptor
import com.example.data.local.dao.TradingDao
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.BrokerAccountEntity
import com.example.domain.model.BrokerAccountStatus
import com.example.domain.model.BrokerCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * PISCES Broker Connection Service.
 * Manages official multi-broker connections (Angel One SmartAPI, Upstox Developer API, Paper Sandbox)
 * behind the common BrokerAdapter interface.
 * Handles encrypted credential persistence, token lifecycle, safe status reporting,
 * and positions/funds synchronization without leaking secrets to the client.
 */
class BrokerConnectionService(
    private val dao: TradingDao,
    private val mockAdapter: MockBrokerAdapter = MockBrokerAdapter()
) {
    // Cache active BrokerAdapter instances per broker code
    private val activeAdapters = ConcurrentHashMap<BrokerCode, BrokerAdapter>()

    init {
        activeAdapters[BrokerCode.PAPER_BROKER] = mockAdapter
        activeAdapters[BrokerCode.ANGEL_ONE] = AngelOneAdapter()
        activeAdapters[BrokerCode.UPSTOX] = UpstoxAdapter()
    }

    /**
     * Retrieve the common BrokerAdapter for a broker code.
     * The rest of the platform (Copy Engine, Risk Engine, UI) only talks to BrokerAdapter.
     */
    fun getAdapter(brokerCode: BrokerCode): BrokerAdapter {
        return activeAdapters.computeIfAbsent(brokerCode) { code ->
            when (code) {
                BrokerCode.ANGEL_ONE -> AngelOneAdapter()
                BrokerCode.UPSTOX -> UpstoxAdapter()
                else -> mockAdapter
            }
        }
    }

    /**
     * Retrieve the adapter for a specific broker account ID stored in the DB.
     */
    suspend fun getAdapterForAccount(brokerAccountId: String): BrokerAdapter = withContext(Dispatchers.IO) {
        val account = dao.getBrokerAccountById(brokerAccountId)
        val code = account?.brokerCode ?: BrokerCode.PAPER_BROKER
        getAdapter(code)
    }

    /**
     * Safely authenticates and stores credentials in the database.
     * Raw credentials are encrypted via AES-256-GCM.
     * No raw passwords or tokens are stored or sent to the frontend.
     */
    suspend fun connectAccount(
        userId: String,
        brokerCode: BrokerCode,
        clientId: String,
        credentials: BrokerCredentials
    ): BrokerAuthResult = withContext(Dispatchers.IO) {
        val adapter = getAdapter(brokerCode)

        // Set status to SYNCING first
        val existing = dao.getBrokerAccount(userId, brokerCode)
        if (existing != null) {
            dao.insertBrokerAccount(existing.copy(status = BrokerAccountStatus.SYNCING))
        }

        // Invoke BrokerAdapter authentication
        val authResult = try {
            adapter.authenticate(credentials)
        } catch (e: Exception) {
            BrokerAuthResult(
                success = false,
                message = "Broker authentication failed: ${e.message}",
                errorCode = "CONN_EXCEPTION"
            )
        }

        val now = System.currentTimeMillis()
        val safeStatus = when {
            authResult.success -> BrokerAccountStatus.CONNECTED
            authResult.errorCode == "TOKEN_EXPIRED" -> BrokerAccountStatus.TOKEN_EXPIRED
            authResult.errorCode == "AUTH_REQUIRED" -> BrokerAccountStatus.AUTHENTICATION_REQUIRED
            else -> BrokerAccountStatus.ERROR
        }

        // Securely encrypt credentials for storage
        val tokenToStore = authResult.accessToken.ifBlank { credentials.apiKey }
        val encryptedCreds = CredentialEncryptor.encrypt(tokenToStore)

        // Sync initial funds if authenticated
        var availableFunds = 50000000L // ₹5,00,000 default
        var usedMargin = 0L
        if (authResult.success) {
            try {
                val funds = adapter.getFunds()
                availableFunds = funds.availableCashPaise
                usedMargin = funds.usedMarginPaise
            } catch (_: Exception) {}
        }

        val accountToSave = existing?.copy(
            brokerClientId = clientId,
            status = safeStatus,
            encryptedCredentials = encryptedCreds,
            accessTokenReference = if (authResult.success) "enc_ref_${clientId.take(4)}" else "",
            refreshTokenReference = if (authResult.refreshToken.isNotBlank()) "enc_refresh_${clientId.take(4)}" else "",
            tokenExpiry = authResult.tokenExpiry,
            availableFundsPaise = availableFunds,
            usedMarginPaise = usedMargin,
            lastConnectedAt = if (authResult.success) now else existing.lastConnectedAt,
            lastSyncAt = now,
            updatedAt = now
        ) ?: BrokerAccountEntity(
            userId = userId,
            brokerCode = brokerCode,
            brokerClientId = clientId,
            status = safeStatus,
            encryptedCredentials = encryptedCreds,
            accessTokenReference = if (authResult.success) "enc_ref_${clientId.take(4)}" else "",
            refreshTokenReference = if (authResult.refreshToken.isNotBlank()) "enc_refresh_${clientId.take(4)}" else "",
            tokenExpiry = authResult.tokenExpiry,
            availableFundsPaise = availableFunds,
            usedMarginPaise = usedMargin,
            lastConnectedAt = if (authResult.success) now else 0L,
            lastSyncAt = now
        )

        dao.insertBrokerAccount(accountToSave)

        // Audit Log entry
        dao.insertAuditLog(
            AuditLogEntity(
                userId = userId,
                action = "BROKER_AUTHENTICATE",
                entityType = "BrokerAccount",
                entityId = accountToSave.id,
                detailsJson = """{"broker":"${brokerCode.name}","status":"${safeStatus.name}","clientCode":"$clientId"}"""
            )
        )

        authResult
    }

    /**
     * Refresh active session token if expired or expiring soon.
     */
    suspend fun refreshSession(userId: String, brokerCode: BrokerCode): BrokerAuthResult = withContext(Dispatchers.IO) {
        val adapter = getAdapter(brokerCode)
        val result = adapter.refreshSession()
        val existing = dao.getBrokerAccount(userId, brokerCode)
        if (existing != null) {
            val status = if (result.success) BrokerAccountStatus.CONNECTED else BrokerAccountStatus.TOKEN_EXPIRED
            dao.insertBrokerAccount(
                existing.copy(
                    status = status,
                    tokenExpiry = result.tokenExpiry,
                    lastSyncAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        result
    }

    /**
     * Disconnects broker session cleanly and logs out from broker API.
     */
    suspend fun disconnectAccount(userId: String, brokerCode: BrokerCode): Boolean = withContext(Dispatchers.IO) {
        val adapter = getAdapter(brokerCode)
        try {
            adapter.logout()
            adapter.disconnect()
        } catch (_: Exception) {}

        val existing = dao.getBrokerAccount(userId, brokerCode)
        if (existing != null) {
            val updated = existing.copy(
                status = BrokerAccountStatus.DISCONNECTED,
                updatedAt = System.currentTimeMillis()
            )
            dao.insertBrokerAccount(updated)
            dao.insertAuditLog(
                AuditLogEntity(
                    userId = userId,
                    action = "BROKER_DISCONNECT",
                    entityType = "BrokerAccount",
                    entityId = existing.id,
                    detailsJson = """{"broker":"${brokerCode.name}"}"""
                )
            )
        }
        true
    }

    /**
     * Returns sanitized safe status information for the client.
     * Guaranteed never to leak secrets or tokens.
     */
    suspend fun getSafeBrokerStatus(userId: String, brokerCode: BrokerCode): SafeBrokerStatus = withContext(Dispatchers.IO) {
        val existing = dao.getBrokerAccount(userId, brokerCode)
        if (existing == null) {
            SafeBrokerStatus(
                brokerCode = brokerCode,
                status = BrokerAccountStatus.DISCONNECTED,
                clientId = "",
                lastSyncAt = 0L,
                availableFundsPaise = 0L
            )
        } else {
            // Check if token has expired
            val isExpired = existing.tokenExpiry > 0L && existing.tokenExpiry < System.currentTimeMillis()
            val finalStatus = if (isExpired && existing.status == BrokerAccountStatus.CONNECTED) {
                BrokerAccountStatus.TOKEN_EXPIRED
            } else {
                existing.status
            }
            SafeBrokerStatus(
                brokerCode = existing.brokerCode,
                status = finalStatus,
                clientId = existing.brokerClientId,
                lastSyncAt = existing.lastSyncAt,
                availableFundsPaise = existing.availableFundsPaise
            )
        }
    }
}
