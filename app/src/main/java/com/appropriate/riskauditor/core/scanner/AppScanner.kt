package com.appropriate.riskauditor.core.scanner

import android.content.Context
import com.appropriate.riskauditor.core.model.AppRiskReport
import com.appropriate.riskauditor.core.model.DeviceScanSummary
import com.appropriate.riskauditor.core.model.RiskLevel
import com.appropriate.riskauditor.core.platform.PackageManagerAdapter
import com.appropriate.riskauditor.core.platform.SpecialAccessAdapter
import com.appropriate.riskauditor.core.rules.RiskScoringEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.UUID

sealed class ScanProgress {
    data class Progress(val current: Int, val total: Int, val appLabel: String) : ScanProgress()
    data class Completed(val summary: DeviceScanSummary) : ScanProgress()
    data class Error(val message: String) : ScanProgress()
}

/**
 * Orchestrates full on-device risk auditing across installed applications.
 */
class AppScanner(
    context: Context,
    private val packageManagerAdapter: PackageManagerAdapter = PackageManagerAdapter(context),
    private val specialAccessAdapter: SpecialAccessAdapter = SpecialAccessAdapter(context)
) {

    fun scanDevice(includeSystemApps: Boolean = false): Flow<ScanProgress> = flow {
        val scanTimestamp = System.currentTimeMillis()
        val scanId = UUID.randomUUID().toString()

        val packages = packageManagerAdapter.getInstalledAppPackages(includeSystemApps)
        val total = packages.size

        if (total == 0) {
            val emptySummary = DeviceScanSummary(
                scanId = scanId,
                timestampEpochMillis = scanTimestamp,
                appReports = emptyList(),
                totalAppsScanned = 0,
                userAppsScanned = 0,
                systemAppsScanned = 0,
                criticalRiskCount = 0,
                highRiskCount = 0,
                mediumRiskCount = 0,
                lowRiskCount = 0,
                averageRiskScore = 0
            )
            emit(ScanProgress.Completed(emptySummary))
            return@flow
        }

        val reports = mutableListOf<AppRiskReport>()

        for (i in packages.indices) {
            val pkg = packages[i]
            try {
                val inventory = packageManagerAdapter.extractInventory(pkg)

                emit(ScanProgress.Progress(i + 1, total, inventory.appLabel))

                val permissions = packageManagerAdapter.extractPermissions(pkg)
                val specialAccess = specialAccessAdapter.auditSpecialAccess(pkg)

                val report = RiskScoringEngine.evaluate(
                    inventory = inventory,
                    permissions = permissions,
                    specialAccess = specialAccess,
                    scannedAtEpochMillis = scanTimestamp
                )
                reports.add(report)
            } catch (e: Exception) {
                // AGENTS.md rule: Never crash a scan because one app fails; continue scanning.
                continue
            }
        }

        // Sort reports by risk score descending (highest risk first)
        val sortedReports = reports.sortedByDescending { it.riskScore }

        val criticalCount = sortedReports.count { it.riskLevel == RiskLevel.CRITICAL }
        val highCount = sortedReports.count { it.riskLevel == RiskLevel.HIGH }
        val mediumCount = sortedReports.count { it.riskLevel == RiskLevel.MEDIUM }
        val lowCount = sortedReports.count { it.riskLevel == RiskLevel.LOW }
        val userAppsCount = sortedReports.count { !it.inventory.isSystemApp }
        val systemAppsCount = sortedReports.count { it.inventory.isSystemApp }
        val avgScore = if (sortedReports.isNotEmpty()) {
            sortedReports.sumOf { it.riskScore } / sortedReports.size
        } else {
            0
        }

        val summary = DeviceScanSummary(
            scanId = scanId,
            timestampEpochMillis = scanTimestamp,
            appReports = sortedReports,
            totalAppsScanned = sortedReports.size,
            userAppsScanned = userAppsCount,
            systemAppsScanned = systemAppsCount,
            criticalRiskCount = criticalCount,
            highRiskCount = highCount,
            mediumRiskCount = mediumCount,
            lowRiskCount = lowCount,
            averageRiskScore = avgScore
        )

        emit(ScanProgress.Completed(summary))
    }.flowOn(Dispatchers.IO)
}
