package com.appropriate.riskauditor.core.scanner

import com.appropriate.riskauditor.core.model.AppRiskReport
import com.appropriate.riskauditor.core.model.DeviceScanSummary
import com.appropriate.riskauditor.core.storage.ScanHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScanState {
    object Idle : ScanState()
    data class Scanning(val current: Int, val total: Int, val appLabel: String) : ScanState()
    data class Success(val summary: DeviceScanSummary) : ScanState()
    data class Error(val message: String) : ScanState()
}

/**
 * Global on-device scan manager coordinating active scans, caching current results,
 * cooperative cancellation, and saving completed scans to local Room history.
 */
class ScanManager(
    private val appScanner: AppScanner,
    private val historyRepository: ScanHistoryRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var scanJob: Job? = null

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private var cachedSummary: DeviceScanSummary? = null

    fun getAppReport(packageName: String): AppRiskReport? {
        return cachedSummary?.appReports?.find { it.inventory.packageName == packageName }
    }

    fun getLatestSummary(): DeviceScanSummary? = cachedSummary

    fun startScan(includeSystemApps: Boolean = false) {
        if (_scanState.value is ScanState.Scanning) return

        scanJob = scope.launch {
            try {
                appScanner.scanDevice(includeSystemApps).collect { progress ->
                    when (progress) {
                        is ScanProgress.Progress -> {
                            _scanState.value = ScanState.Scanning(
                                current = progress.current,
                                total = progress.total,
                                appLabel = progress.appLabel
                            )
                        }
                        is ScanProgress.Completed -> {
                            cachedSummary = progress.summary
                            _scanState.value = ScanState.Success(progress.summary)
                            // Persist to Room local history
                            historyRepository.saveScan(progress.summary)
                        }
                        is ScanProgress.Error -> {
                            _scanState.value = ScanState.Error(progress.message)
                        }
                    }
                }
            } catch (e: Exception) {
                _scanState.value = ScanState.Idle
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        if (_scanState.value is ScanState.Scanning) {
            _scanState.value = ScanState.Idle
        }
    }
}
