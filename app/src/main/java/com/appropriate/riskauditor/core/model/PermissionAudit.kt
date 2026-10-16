package com.appropriate.riskauditor.core.model

/**
 * Audit record for a single permission requested or held by an application.
 * Distinguishes declared vs granted state.
 */
data class PermissionAudit(
    val name: String,
    val simpleName: String = name.substringAfterLast('.'),
    val isDeclared: Boolean = true,
    val grantState: GrantState,
    val category: RiskCategory,
    val protectionLevel: String? = null,
    val protection: ProtectionLevel = ProtectionLevel.UNKNOWN
)
