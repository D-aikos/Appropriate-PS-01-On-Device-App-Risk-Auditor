package com.appropriate.riskauditor.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appropriate.riskauditor.RiskAuditorApp
import com.appropriate.riskauditor.core.storage.ScanAppSummaryEntity
import com.appropriate.riskauditor.core.storage.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val app: RiskAuditorApp = RiskAuditorApp.instance
) : ViewModel() {

    val scanHistory: StateFlow<List<ScanHistoryEntity>> = app.historyRepository.allScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getAppsForScan(scanId: String): Flow<List<ScanAppSummaryEntity>> {
        return app.historyRepository.getAppsForScan(scanId)
    }

    fun deleteScan(scanId: String) {
        viewModelScope.launch {
            app.historyRepository.deleteScan(scanId)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            app.historyRepository.clearAllHistory()
        }
    }
}
