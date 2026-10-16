package com.appropriate.riskauditor.core.model

/**
 * Audit record for a special access capability.
 */
data class SpecialAccessAudit(
    val type: SpecialAccessType,
    val state: SpecialAccessState,
    val isDeclared: Boolean,
    val detail: String? = null
)
