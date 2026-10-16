package com.appropriate.riskauditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.appropriate.riskauditor.feature.AppNavHost
import com.appropriate.riskauditor.feature.apps.AppListViewModel
import com.appropriate.riskauditor.feature.common.RiskAuditorTheme
import com.appropriate.riskauditor.feature.dashboard.DashboardViewModel
import com.appropriate.riskauditor.feature.detail.AppDetailViewModel
import com.appropriate.riskauditor.feature.history.HistoryViewModel
import com.appropriate.riskauditor.feature.settings.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val dashboardViewModel by viewModels<DashboardViewModel>()
    private val appListViewModel by viewModels<AppListViewModel>()
    private val appDetailViewModel by viewModels<AppDetailViewModel>()
    private val historyViewModel by viewModels<HistoryViewModel>()
    private val settingsViewModel by viewModels<SettingsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RiskAuditorTheme {
                AppNavHost(
                    dashboardViewModel = dashboardViewModel,
                    appListViewModel = appListViewModel,
                    appDetailViewModel = appDetailViewModel,
                    historyViewModel = historyViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}
