package com.appropriate.riskauditor

import android.app.Application
import com.appropriate.riskauditor.core.report.ReportExporter
import com.appropriate.riskauditor.core.scanner.AppScanner
import com.appropriate.riskauditor.core.scanner.ScanManager
import com.appropriate.riskauditor.core.storage.AppDatabase
import com.appropriate.riskauditor.core.storage.ScanHistoryRepository
import com.appropriate.riskauditor.feature.settings.SettingsManager

/**
 * Application entry point providing local database, repository, scanner,
 * settings, and report dependencies without external service locator or dependency bloat.
 */
class RiskAuditorApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val historyRepository by lazy { ScanHistoryRepository(database.scanHistoryDao()) }
    val appScanner by lazy { AppScanner(this) }
    val reportExporter by lazy { ReportExporter(this) }
    val settingsManager by lazy { SettingsManager(this) }
    val scanManager by lazy { ScanManager(appScanner, historyRepository) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: RiskAuditorApp
            private set
    }
}
