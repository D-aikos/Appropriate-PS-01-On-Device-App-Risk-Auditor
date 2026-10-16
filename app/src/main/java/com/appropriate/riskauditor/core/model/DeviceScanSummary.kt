package com.appropriate.riskauditor.core.model

/**
 * Aggregated audit report across all scanned applications on the device.
 */
data class DeviceScanSummary(
    val scanId: String,
    val timestampEpochMillis: Long,
    val appReports: List<AppRiskReport>,
    val totalAppsScanned: Int,
    val userAppsScanned: Int,
    val systemAppsScanned: Int,
    val criticalRiskCount: Int,
    val highRiskCount: Int,
    val mediumRiskCount: Int,
    val lowRiskCount: Int,
    val averageRiskScore: Int
) {
    val overallRiskLevel: RiskLevel
        get() = when {
            criticalRiskCount > 0 -> RiskLevel.CRITICAL
            highRiskCount > 0 -> RiskLevel.HIGH
            mediumRiskCount > 0 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

    val highAndCriticalReports: List<AppRiskReport>
        get() = appReports.filter { it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.CRITICAL }
}
