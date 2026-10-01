package com.example.engine

import com.example.broker.BrokerAdapter
import com.example.broker.model.*
import com.example.data.local.dao.TradingDao
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.NotificationEntity
import com.example.domain.model.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

class ReconciliationEngine(
    private val dao: TradingDao,
    private val brokerAdapter: BrokerAdapter
) {

    /**
     * Executes a full reconciliation audit comparing internal DB positions vs broker book.
     */
    suspend fun runReconciliationAudit(correlationId: String = UUID.randomUUID().toString()): ReconciliationResult = withContext(Dispatchers.IO) {
        val internalPositions = dao.getAllOpenPositions().first()
        val result = brokerAdapter.reconcilePositions(internalPositions)

        val detailsJson = if (result.isBalanced) {
            """{"status":"BALANCED","audited":${result.totalPositionsAudited},"discrepancies":0}"""
        } else {
            val items = result.discrepancies.joinToString(",") {
                """{"symbol":"${it.symbol}","type":"${it.discrepancyType}","internal":${it.internalQuantity},"broker":${it.brokerQuantity}}"""
            }
            """{"status":"DISCREPANCIES_FOUND","audited":${result.totalPositionsAudited},"items":[$items]}"""
        }

        // Persist Audit Log
        dao.insertAuditLog(
            AuditLogEntity(
                correlationId = correlationId,
                userId = "SYSTEM_RECONCILIATION",
                action = if (result.isBalanced) "RECONCILIATION_BALANCED" else "RECONCILIATION_DISCREPANCY_ALERT",
                entityType = "PositionBook",
                entityId = "ALL",
                detailsJson = detailsJson
            )
        )

        // Send alert if discrepancies found
        if (!result.isBalanced) {
            dao.insertNotification(
                NotificationEntity(
                    userId = "ADMIN",
                    title = "Reconciliation Discrepancy Alert",
                    message = "Audit detected ${result.discrepancies.size} position discrepancies against broker book.",
                    type = NotificationType.RISK
                )
            )
        }

        result
    }
}
