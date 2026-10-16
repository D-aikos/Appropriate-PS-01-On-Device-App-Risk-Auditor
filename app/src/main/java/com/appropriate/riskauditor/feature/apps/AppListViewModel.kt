package com.appropriate.riskauditor.feature.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appropriate.riskauditor.RiskAuditorApp
import com.appropriate.riskauditor.core.model.AppRiskReport
import com.appropriate.riskauditor.core.model.GrantState
import com.appropriate.riskauditor.core.model.RiskCategory
import com.appropriate.riskauditor.core.model.RiskLevel
import com.appropriate.riskauditor.core.model.SpecialAccessState
import com.appropriate.riskauditor.core.scanner.ScanState
import kotlinx.coroutines.flow.*

enum class AppSortOrder(val displayName: String) {
    RISK_DESC("Risk (Highest first)"),
    NAME_ASC("Name A–Z"),
    RECENTLY_INSTALLED("Recently installed"),
    RECENTLY_UPDATED("Recently updated")
}

data class FilterCounts(
    val total: Int = 0,
    val critical: Int = 0,
    val high: Int = 0,
    val medium: Int = 0,
    val low: Int = 0
)

class AppListViewModel(
    private val app: RiskAuditorApp = RiskAuditorApp.instance
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Multi-select risk bands (empty set means ALL)
    private val _selectedBands = MutableStateFlow<Set<RiskLevel>>(emptySet())
    val selectedBands = _selectedBands.asStateFlow()

    // Optional category filter
    private val _selectedCategory = MutableStateFlow<RiskCategory?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedSort = MutableStateFlow(AppSortOrder.RISK_DESC)
    val selectedSort = _selectedSort.asStateFlow()

    val scanState: StateFlow<ScanState> = app.scanManager.scanState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScanState.Idle)

    val includeSystemApps: StateFlow<Boolean> = app.settingsManager.includeSystemApps

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleBand(band: RiskLevel) {
        val current = _selectedBands.value
        _selectedBands.value = if (current.contains(band)) {
            current - band
        } else {
            current + band
        }
    }

    fun selectOnlyBand(band: RiskLevel) {
        _selectedBands.value = setOf(band)
    }

    fun selectCategory(category: RiskCategory?) {
        _selectedCategory.value = category
    }

    fun selectSort(sort: AppSortOrder) {
        _selectedSort.value = sort
    }

    fun toggleIncludeSystemApps(include: Boolean) {
        app.settingsManager.setIncludeSystemApps(include)
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedBands.value = emptySet()
        _selectedCategory.value = null
    }

    // Counts computed based on active includeSystemApps filter
    val filterCounts: StateFlow<FilterCounts> = combine(
        scanState,
        includeSystemApps
    ) { state, includeSystem ->
        if (state !is ScanState.Success) return@combine FilterCounts()
        val allApps = state.summary.appReports
        val scoped = if (includeSystem) allApps else allApps.filter { !it.inventory.isSystemApp }
        FilterCounts(
            total = scoped.size,
            critical = scoped.count { it.riskLevel == RiskLevel.CRITICAL },
            high = scoped.count { it.riskLevel == RiskLevel.HIGH },
            medium = scoped.count { it.riskLevel == RiskLevel.MEDIUM },
            low = scoped.count { it.riskLevel == RiskLevel.LOW }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FilterCounts())

    val filteredApps: StateFlow<List<AppRiskReport>> = combine(
        scanState,
        includeSystemApps,
        _searchQuery,
        _selectedBands,
        _selectedCategory,
        _selectedSort
    ) { state, includeSystem, query, bands, category, sort ->
        if (state !is ScanState.Success) return@combine emptyList()

        var list = state.summary.appReports

        // 1. System apps instant filter
        if (!includeSystem) {
            list = list.filter { !it.inventory.isSystemApp }
        }

        // 2. Search query filter
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.inventory.appLabel.lowercase().contains(q) ||
                        it.inventory.packageName.lowercase().contains(q)
            }
        }

        // 3. Multi-select Risk Band filter
        if (bands.isNotEmpty()) {
            list = list.filter { bands.contains(it.riskLevel) }
        }

        // 4. Category filter
        if (category != null) {
            list = list.filter { appReport ->
                if (category == RiskCategory.SPECIAL_ACCESS) {
                    appReport.specialAccess.any { it.state != SpecialAccessState.NOT_DECLARED }
                } else {
                    appReport.permissions.any { it.category == category }
                }
            }
        }

        // 5. Sorting
        when (sort) {
            AppSortOrder.RISK_DESC -> list.sortedByDescending { it.riskScore }
            AppSortOrder.NAME_ASC -> list.sortedBy { it.inventory.appLabel.lowercase() }
            AppSortOrder.RECENTLY_INSTALLED -> list.sortedByDescending { it.inventory.firstInstallTimeEpochMillis ?: 0L }
            AppSortOrder.RECENTLY_UPDATED -> list.sortedByDescending { it.inventory.lastUpdateTimeEpochMillis ?: 0L }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
