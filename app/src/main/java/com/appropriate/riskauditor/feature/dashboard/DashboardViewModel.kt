package com.appropriate.riskauditor.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appropriate.riskauditor.RiskAuditorApp
import com.appropriate.riskauditor.core.model.DeviceScanSummary
import com.appropriate.riskauditor.core.scanner.ScanState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

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

    fun exportPdfReport(summary: DeviceScanSummary): Boolean {
        return app.reportExporter.exportAndShareReport(summary)
    }
}
