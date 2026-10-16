package com.appropriate.riskauditor.feature.apps

import androidx.lifecycle.ViewModel
import com.appropriate.riskauditor.RiskAuditorApp
import com.appropriate.riskauditor.core.model.AppRiskReport

class AppDetailViewModel(
    private val app: RiskAuditorApp = RiskAuditorApp.instance
) : ViewModel() {

    fun getAppReport(packageName: String): AppRiskReport? {
        return app.scanManager.getAppReport(packageName)
    }
}
