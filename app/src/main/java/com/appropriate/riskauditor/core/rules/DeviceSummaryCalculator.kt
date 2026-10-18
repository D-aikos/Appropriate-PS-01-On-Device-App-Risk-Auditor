package com.appropriate.riskauditor.core.rules

import com.appropriate.riskauditor.core.model.*

data class CategoryStat(
    val category: RiskCategory,
    val appsWithAccess: Int,
    val appsDeclaring: Int
)

data class DeviceSummary(
    val totalApps: Int,
    val bandCounts: Map<RiskLevel, Int>,
    val categoryStats: List<CategoryStat>,
    val topRisky: List<AppRiskReport>
)

/**
 * Aggregates app-level audit reports into an overall device summary
 * adhering strictly to Section 7.7.
 */
object DeviceSummaryCalculator {

    val FIXED_CATEGORIES = listOf(
        RiskCategory.SPECIAL_ACCESS,
        RiskCategory.SMS,
        RiskCategory.CONTACTS_AND_CALLS,
        RiskCategory.CAMERA_AND_MIC,
        RiskCategory.LOCATION,
        RiskCategory.PHONE_AND_IDENTITY,
        RiskCategory.STORAGE_AND_MEDIA,
        RiskCategory.BACKGROUND_AND_PERSISTENCE,
        RiskCategory.NETWORK
    )

    fun calculate(apps: List<AppRiskReport>, top: Int = 5): DeviceSummary {
        // bandCounts always contains all 4 keys
        val bandCounts = mutableMapOf(
            RiskLevel.LOW to 0,
            RiskLevel.MEDIUM to 0,
            RiskLevel.HIGH to 0,
            RiskLevel.CRITICAL to 0
        )
        for (app in apps) {
            bandCounts[app.riskLevel] = (bandCounts[app.riskLevel] ?: 0) + 1
        }

        // categoryStats always 9 entries in fixed enum order excluding OTHER
        val categoryStats = FIXED_CATEGORIES.map { cat ->
            var withAccess = 0
            var declaring = 0
            for (app in apps) {
                if (cat == RiskCategory.SPECIAL_ACCESS) {
                    val hasDeclared = app.specialAccess.any { it.state != SpecialAccessState.NOT_DECLARED }
                    val hasEnabled = app.specialAccess.any { it.state == SpecialAccessState.ENABLED }
                    if (hasDeclared) declaring++
                    if (hasEnabled) withAccess++
                } else {
                    val inCat = app.permissions.filter { it.category == cat }
                    if (inCat.isNotEmpty()) declaring++
                    if (inCat.any { it.grantState == GrantState.GRANTED }) withAccess++
                }
            }
            CategoryStat(cat, appsWithAccess = withAccess, appsDeclaring = declaring)
        }

        // topRisky sorted by score desc, ties by label
        val topRisky = apps.sortedWith(
            compareByDescending<AppRiskReport> { it.riskScore }
                .thenBy { it.inventory.appLabel.lowercase() }
        ).take(top)

        return DeviceSummary(
            totalApps = apps.size,
            bandCounts = bandCounts,
            categoryStats = categoryStats,
            topRisky = topRisky
        )
    }
}
