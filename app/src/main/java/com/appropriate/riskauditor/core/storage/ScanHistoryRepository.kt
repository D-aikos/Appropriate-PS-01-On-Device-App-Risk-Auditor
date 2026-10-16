package com.appropriate.riskauditor.core.storage

import com.appropriate.riskauditor.core.model.DeviceScanSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ScanHistoryRepository(private val dao: ScanHistoryDao) {

    val allScans: Flow<List<ScanHistoryEntity>> = dao.getAllScans()

    fun getAppsForScan(scanId: String): Flow<List<ScanAppSummaryEntity>> {
        return dao.getAppsForScan(scanId)
    }

    suspend fun saveScan(summary: DeviceScanSummary) = withContext(Dispatchers.IO) {
        val historyEntity = ScanHistoryEntity(
            scanId = summary.scanId,
            timestampEpochMillis = summary.timestampEpochMillis,
            totalAppsScanned = summary.totalAppsScanned,
            userAppsScanned = summary.userAppsScanned,
            systemAppsScanned = summary.systemAppsScanned,
            criticalCount = summary.criticalRiskCount,
            highCount = summary.highRiskCount,
            mediumCount = summary.mediumRiskCount,
            lowCount = summary.lowRiskCount,
            averageRiskScore = summary.averageRiskScore
        )

        val appEntities = summary.appReports.map { report ->
            ScanAppSummaryEntity(
                scanId = summary.scanId,
                packageName = report.inventory.packageName,
                appLabel = report.inventory.appLabel,
                versionName = report.inventory.versionName,
                riskScore = report.riskScore,
                riskLevel = report.riskLevel.name,
                findingsCount = report.findings.size,
                topFindingTitle = report.findings.firstOrNull()?.title,
                isSystemApp = report.inventory.isSystemApp
            )
        }

        dao.insertScanHistory(historyEntity)
        dao.insertAppSummaries(appEntities)
    }

    suspend fun deleteScan(scanId: String) = withContext(Dispatchers.IO) {
        dao.deleteScanAndApps(scanId)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        dao.clearAllHistory()
    }
}
