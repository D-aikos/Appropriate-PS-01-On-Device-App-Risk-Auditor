package com.appropriate.riskauditor.feature.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appropriate.riskauditor.RiskAuditorApp
import com.appropriate.riskauditor.core.model.AppRiskReport
import com.appropriate.riskauditor.core.model.RiskLevel
import com.appropriate.riskauditor.core.scanner.ScanState
import kotlinx.coroutines.flow.*

enum class RiskFilter(val displayName: String) {
    ALL("All"),
    CRITICAL("Critical"),
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low"),
    SIDELOADED("Sideloaded")
}

enum class SortOrder(val displayName: String) {
    RISK_DESC("Highest Risk"),
    NAME_ASC("App Name"),
    INSTALL_TIME_DESC("Recently Updated")
}

data class FilterCounts(
    val all: Int = 0,
    val critical: Int = 0,
    val high: Int = 0,
    val medium: Int = 0,
    val low: Int = 0,
    val sideloaded: Int = 0
)

class AppListViewModel(
    private val app: RiskAuditorApp = RiskAuditorApp.instance
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(RiskFilter.ALL)
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _selectedSort = MutableStateFlow(SortOrder.RISK_DESC)
    val selectedSort = _selectedSort.asStateFlow()

    val scanState: StateFlow<ScanState> = app.scanManager.scanState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScanState.Idle)

    val includeSystemApps: StateFlow<Boolean> = app.settingsManager.includeSystemApps

    val filterCounts: StateFlow<FilterCounts> = scanState.map { state ->
        if (state !is ScanState.Success) return@map FilterCounts()
        val list = state.summary.appReports
        FilterCounts(
            all = list.size,
            critical = list.count { it.riskLevel == RiskLevel.CRITICAL },
            high = list.count { it.riskLevel == RiskLevel.HIGH },
            medium = list.count { it.riskLevel == RiskLevel.MEDIUM },
            low = list.count { it.riskLevel == RiskLevel.LOW },
            sideloaded = list.count { it.inventory.isSideloaded }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FilterCounts())

    val filteredApps: StateFlow<List<AppRiskReport>> = combine(
        scanState,
        _searchQuery,
        _selectedFilter,
        _selectedSort
    ) { state, query, filter, sort ->
        if (state !is ScanState.Success) return@combine emptyList()

        var list = state.summary.appReports

        // 1. Text Search Filter
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.inventory.appLabel.lowercase().contains(q) ||
                        it.inventory.packageName.lowercase().contains(q)
            }
        }

        // 2. Risk / Sideload Filter
        list = when (filter) {
            RiskFilter.ALL -> list
            RiskFilter.CRITICAL -> list.filter { it.riskLevel == RiskLevel.CRITICAL }
            RiskFilter.HIGH -> list.filter { it.riskLevel == RiskLevel.HIGH }
            RiskFilter.MEDIUM -> list.filter { it.riskLevel == RiskLevel.MEDIUM }
            RiskFilter.LOW -> list.filter { it.riskLevel == RiskLevel.LOW }
            RiskFilter.SIDELOADED -> list.filter { it.inventory.isSideloaded }
        }

        // 3. Sorting
        when (sort) {
            SortOrder.RISK_DESC -> list.sortedByDescending { it.riskScore }
            SortOrder.NAME_ASC -> list.sortedBy { it.inventory.appLabel.lowercase() }
            SortOrder.INSTALL_TIME_DESC -> list.sortedByDescending { it.inventory.lastUpdateTimeEpochMillis ?: 0L }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFilter(filter: RiskFilter) {
        _selectedFilter.value = filter
    }

    fun selectSort(sort: SortOrder) {
        _selectedSort.value = sort
    }

    fun toggleIncludeSystemApps(include: Boolean) {
        app.settingsManager.setIncludeSystemApps(include)
        app.scanManager.startScan(include)
    }
}
