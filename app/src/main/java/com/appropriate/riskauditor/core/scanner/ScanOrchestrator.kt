package com.appropriate.riskauditor.core.scanner

import com.appropriate.riskauditor.core.model.AppRiskReport
import com.appropriate.riskauditor.core.model.DeviceScanSummary
import com.appropriate.riskauditor.core.model.RiskLevel
import com.appropriate.riskauditor.core.storage.ScanHistoryRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Orchestrates the full lifecycle of an on-device risk audit.
 * Connects the streaming AndroidAppScanner to Room persistence,
 * handles cancellation in a NonCancellable block, and exposes UI scan state.
 */
class ScanOrchestrator(
    private val scanner: AndroidAppScanner,
    private val historyRepository: ScanHistoryRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private var scanJob: Job? = null
    private var cachedSummary: DeviceScanSummary? = null

    fun getLatestSummary(): DeviceScanSummary? = cachedSummary

    fun getAppReport(packageName: String): AppRiskReport? {
        return cachedSummary?.appReports?.find { it.inventory.packageName == packageName }
    }

    fun start(includeSystemApps: Boolean = false) {
        if (_scanState.value is ScanState.Scanning) return

        scanJob = scope.launch {
            val scanId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            val collectedReports = mutableListOf<AppRiskReport>()
            val warnings = mutableListOf<String>()

            try {
                scanner.scan(includeSystemApps).collect { event ->
                    when (event) {
                        is ScanEvent.Progress -> {
                            _scanState.value = ScanState.Scanning(
                                current = event.done,
                                total = event.total,
                                appLabel = event.currentLabel
                            )
                        }
                        is ScanEvent.AppScanned -> {
                            collectedReports.add(event.report)
                        }
                        is ScanEvent.Warning -> {
                            warnings.add(event.message)
                        }
                    }
                }

                // Sorting: highest risk score first
                val sortedReports = collectedReports.sortedByDescending { it.riskScore }
                val criticalCount = sortedReports.count { it.riskLevel == RiskLevel.CRITICAL }
                val highCount = sortedReports.count { it.riskLevel == RiskLevel.HIGH }
                val mediumCount = sortedReports.count { it.riskLevel == RiskLevel.MEDIUM }
                val lowCount = sortedReports.count { it.riskLevel == RiskLevel.LOW }
                val userCount = sortedReports.count { !it.inventory.isSystemApp }
                val systemCount = sortedReports.count { it.inventory.isSystemApp }
                val avgScore = if (sortedReports.isNotEmpty()) {
                    sortedReports.sumOf { it.riskScore } / sortedReports.size
                } else {
                    0
                }

                val summary = DeviceScanSummary(
                    scanId = scanId,
                    timestampEpochMillis = timestamp,
                    appReports = sortedReports,
                    totalAppsScanned = sortedReports.size,
                    userAppsScanned = userCount,
                    systemAppsScanned = systemCount,
                    criticalRiskCount = criticalCount,
                    highRiskCount = highCount,
                    mediumRiskCount = mediumCount,
                    lowRiskCount = lowCount,
                    averageRiskScore = avgScore
                )

                cachedSummary = summary
                _scanState.value = ScanState.Success(summary)

                // Persist to Room local database
                withContext(NonCancellable + Dispatchers.IO) {
                    historyRepository.saveScan(summary)
                }

            } catch (e: CancellationException) {
                _scanState.value = ScanState.Idle
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "Audit scan encountered an error")
            }
        }
    }

    fun cancel() {
        scanJob?.cancel()
        scanJob = null
        if (_scanState.value is ScanState.Scanning) {
            _scanState.value = ScanState.Idle
        }
    }
}
