package com.appropriate.riskauditor.feature.settings

import androidx.lifecycle.ViewModel
import com.appropriate.riskauditor.RiskAuditorApp
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val app: RiskAuditorApp = RiskAuditorApp.instance
) : ViewModel() {

    val includeSystemApps: StateFlow<Boolean> = app.settingsManager.includeSystemApps

    fun setIncludeSystemApps(include: Boolean) {
        app.settingsManager.setIncludeSystemApps(include)
    }
}
