package com.appropriate.riskauditor.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appropriate.riskauditor.RiskAuditorApp
import com.appropriate.riskauditor.core.model.AppRiskReport
import com.appropriate.riskauditor.core.model.DeviceScanSummary
import com.appropriate.riskauditor.core.model.GrantState
import com.appropriate.riskauditor.core.model.RiskCategory
import com.appropriate.riskauditor.core.model.RiskLevel
import com.appropriate.riskauditor.core.model.SpecialAccessState
import com.appropriate.riskauditor.core.scanner.ScanState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class CategoryUsageStat(
    val category: RiskCategory,
    val appsWithAccess: Int,
    val appsDeclaring: Int
)

class DashboardViewModel(
    private val app: RiskAuditorApp = RiskAuditorApp.instance
) : ViewModel() {

    val scanState: StateFlow<ScanState> = app.scanManager.scanState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScanState.Idle)

    val includeSystemApps: StateFlow<Boolean> = app.settingsManager.includeSystemApps

    fun toggleIncludeSystemApps(include: Boolean) {
        app.settingsManager.setIncludeSystemApps(include)
    }

    fun startScan() {
        val includeSystem = app.settingsManager.includeSystemApps.value
        app.scanManager.startScan(includeSystem)
    }

    fun cancelScan() {
        app.scanManager.cancelScan()
    }

    fun exportPdfReport(summary: DeviceScanSummary): Boolean {
        return app.reportExporter.exportAndShareReport(summary)
    }

    /**
     * Filters app reports according to current includeSystemApps preference.
     */
    fun filterReports(reports: List<AppRiskReport>, includeSystem: Boolean): List<AppRiskReport> {
        return if (includeSystem) reports else reports.filter { !it.inventory.isSystemApp }
    }

    /**
     * Computes the 9 scored category usage stats in fixed order for the dashboard bar matrix.
     */
    fun computeCategoryStats(apps: List<AppRiskReport>): List<CategoryUsageStat> {
        val fixedCategories = listOf(
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

        return fixedCategories.map { category ->
            var withAccess = 0
            var declaring = 0

            for (app in apps) {
                if (category == RiskCategory.SPECIAL_ACCESS) {
                    val hasSpecialDeclared = app.specialAccess.any { it.state != SpecialAccessState.NOT_DECLARED }
                    val hasSpecialEnabled = app.specialAccess.any { it.state == SpecialAccessState.ENABLED }
                    if (hasSpecialDeclared) declaring++
                    if (hasSpecialEnabled) withAccess++
                } else {
                    val perms = app.permissions.filter { it.category == category }
                    if (perms.isNotEmpty()) declaring++
                    if (perms.any { it.grantState == GrantState.GRANTED }) withAccess++
                }
            }

            CategoryUsageStat(
                category = category,
                appsWithAccess = withAccess,
                appsDeclaring = declaring
            )
        }
    }
}
