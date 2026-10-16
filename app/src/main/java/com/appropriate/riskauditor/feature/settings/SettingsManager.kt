package com.appropriate.riskauditor.feature.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("auditor_settings", Context.MODE_PRIVATE)

    private val _includeSystemApps = MutableStateFlow(prefs.getBoolean(KEY_INCLUDE_SYSTEM_APPS, false))
    val includeSystemApps: StateFlow<Boolean> = _includeSystemApps.asStateFlow()

    private val _riskAlertsEnabled = MutableStateFlow(prefs.getBoolean(KEY_RISK_ALERTS, false))
    val riskAlertsEnabled: StateFlow<Boolean> = _riskAlertsEnabled.asStateFlow()

    fun setIncludeSystemApps(include: Boolean) {
        prefs.edit().putBoolean(KEY_INCLUDE_SYSTEM_APPS, include).apply()
        _includeSystemApps.value = include
    }

    fun setRiskAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RISK_ALERTS, enabled).apply()
        _riskAlertsEnabled.value = enabled
    }

    companion object {
        private const val KEY_INCLUDE_SYSTEM_APPS = "key_include_system_apps"
        private const val KEY_RISK_ALERTS = "key_risk_alerts"
    }
}
