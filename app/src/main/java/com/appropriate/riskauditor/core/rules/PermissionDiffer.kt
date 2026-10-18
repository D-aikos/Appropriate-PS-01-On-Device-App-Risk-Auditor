package com.appropriate.riskauditor.core.rules

import com.appropriate.riskauditor.core.model.*

/**
 * Computes plain-language diffs between application audit snapshots adhering to Section 7.7.
 */
object PermissionDiffer {

    private val SENSITIVE_CATEGORIES = setOf(
        RiskCategory.SPECIAL_ACCESS,
        RiskCategory.SMS,
        RiskCategory.CONTACTS_AND_CALLS,
        RiskCategory.CAMERA_AND_MIC,
        RiskCategory.LOCATION
    )

    fun diff(old: AppRiskReport?, new: AppRiskReport): List<String> {
        if (old == null) return emptyList()

        val changes = mutableListOf<String>()

        // 1. Newly declared permissions in sensitive categories
        val oldPermNames = old.permissions.map { it.name.substringAfterLast('.').uppercase() }.toSet()
        for (perm in new.permissions) {
            val shortName = perm.name.substringAfterLast('.').uppercase()
            if (shortName !in oldPermNames && perm.category in SENSITIVE_CATEGORIES) {
                changes.add("Newly requested sensitive permission: $shortName (${perm.category.displayName})")
            }
        }

        // 2. Special access capabilities that moved to ENABLED
        val oldSpecialMap = old.specialAccess.associateBy { it.type }
        for (access in new.specialAccess) {
            val oldState = oldSpecialMap[access.type]?.state ?: SpecialAccessState.NOT_DECLARED
            if (access.state == SpecialAccessState.ENABLED && oldState != SpecialAccessState.ENABLED) {
                changes.add("Special access turned on: ${access.type.title}")
            }
        }

        return changes
    }
}
