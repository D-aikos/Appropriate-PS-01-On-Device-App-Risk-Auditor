package com.appropriate.riskauditor.core.model

/**
 * Complete evaluation report for a single application, including inventory,
 * permission grants, special access states, risk score, and evidence findings.
 */
data class AppRiskReport(
    val inventory: AppInventoryRecord,
    val permissions: List<PermissionAudit>,
    val specialAccess: List<SpecialAccessAudit>,
    val findings: List<RiskFinding>,
    val riskScore: Int,
    val riskLevel: RiskLevel,
    val scannedAtEpochMillis: Long
) {
    /** Group permissions by their respective risk category for UI matrix */
    val permissionsByCategory: Map<RiskCategory, List<PermissionAudit>>
        get() = permissions.groupBy { it.category }

    /** Count of active granted permissions */
    val grantedPermissionCount: Int
        get() = permissions.count { it.grantState == GrantState.GRANTED }

    /** Count of active special access capabilities */
    val enabledSpecialAccessCount: Int
        get() = specialAccess.count { it.state == SpecialAccessState.ENABLED }
}
