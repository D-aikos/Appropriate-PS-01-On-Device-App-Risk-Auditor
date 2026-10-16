package com.appropriate.riskauditor

import android.app.Application
import com.appropriate.riskauditor.core.platform.AndroidIconLoader
import com.appropriate.riskauditor.core.platform.AndroidSettingsLauncher
import com.appropriate.riskauditor.core.report.ReportExporter
import com.appropriate.riskauditor.core.scanner.AndroidAppScanner
import com.appropriate.riskauditor.core.scanner.AppScanner
import com.appropriate.riskauditor.core.scanner.ScanManager
import com.appropriate.riskauditor.core.scanner.ScanOrchestrator
import com.appropriate.riskauditor.core.storage.AppDatabase
import com.appropriate.riskauditor.core.storage.ScanHistoryRepository
import com.appropriate.riskauditor.feature.settings.SettingsManager

/**
 * Application entry point providing local database, repository, scanner,
 * platform adapters, settings, and report dependencies.
 */
class RiskAuditorApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val historyRepository by lazy { ScanHistoryRepository(database.scanHistoryDao()) }
    val androidAppScanner by lazy { AndroidAppScanner(this) }
    val scanOrchestrator by lazy { ScanOrchestrator(androidAppScanner, historyRepository) }
    val iconLoader by lazy { AndroidIconLoader(this) }
    val settingsLauncher by lazy { AndroidSettingsLauncher(this) }
    val reportExporter by lazy { ReportExporter(this) }
    val settingsManager by lazy { SettingsManager(this) }
    val scanManager by lazy { ScanManager(AppScanner(this), historyRepository) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: RiskAuditorApp
            private set
    }
}
