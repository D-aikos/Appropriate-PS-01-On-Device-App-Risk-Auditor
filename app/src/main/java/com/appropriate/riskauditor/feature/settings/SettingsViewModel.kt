package com.appropriate.riskauditor.feature.settings

import androidx.lifecycle.ViewModel
import com.appropriate.riskauditor.RiskAuditorApp
import com.appropriate.riskauditor.core.model.DeviceScanSummary
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val app: RiskAuditorApp = RiskAuditorApp.instance
) : ViewModel() {

    val includeSystemApps: StateFlow<Boolean> = app.settingsManager.includeSystemApps
    val riskAlertsEnabled: StateFlow<Boolean> = app.settingsManager.riskAlertsEnabled

    fun setIncludeSystemApps(include: Boolean) {
        app.settingsManager.setIncludeSystemApps(include)
    }

    fun setRiskAlertsEnabled(enabled: Boolean) {
        app.settingsManager.setRiskAlertsEnabled(enabled)
    }

    fun exportPdfReport(): Boolean {
        val summary = app.scanManager.getLatestSummary() ?: return false
        return app.reportExporter.exportAndShareReport(summary)
    }

    fun hasScanData(): Boolean {
        return app.scanManager.getLatestSummary() != null
    }
}
