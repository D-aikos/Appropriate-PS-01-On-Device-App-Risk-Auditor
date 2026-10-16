package com.appropriate.riskauditor.feature.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appropriate.riskauditor.core.model.AppRiskReport
import com.appropriate.riskauditor.core.model.RiskCategory
import com.appropriate.riskauditor.core.model.RiskLevel
import com.appropriate.riskauditor.feature.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    onNavigateToAppDetail: (String) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedBands by viewModel.selectedBands.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedSort by viewModel.selectedSort.collectAsState()
    val apps by viewModel.filteredApps.collectAsState()
    val counts by viewModel.filterCounts.collectAsState()
    val includeSystemApps by viewModel.includeSystemApps.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Audited Applications",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Showing ${apps.size} of ${counts.total} apps",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    // Category Filter Action
                    IconButton(onClick = { showCategoryMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter by category",
                            tint = if (selectedCategory != null) PrimaryLightBlue else TextPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "All Categories",
                                    color = if (selectedCategory == null) PrimaryLightBlue else TextPrimary,
                                    fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                viewModel.selectCategory(null)
                                showCategoryMenu = false
                            }
                        )
                        RiskCategory.values().forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = category.displayName(),
                                        color = if (selectedCategory == category) PrimaryLightBlue else TextPrimary,
                                        fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.selectCategory(category)
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }

                    // Sort Action
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort apps", tint = TextPrimary)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        AppSortOrder.values().forEach { sort ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = sort.displayName,
                                        color = if (sort == selectedSort) PrimaryLightBlue else TextPrimary,
                                        fontWeight = if (sort == selectedSort) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.selectSort(sort)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by name or package...", color = TextMuted) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search", tint = TextMuted)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryLightBlue,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            // Band Filter Chips (Critical, High, Medium, Low) & System App Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bands = listOf(
                        RiskLevel.CRITICAL to counts.critical,
                        RiskLevel.HIGH to counts.high,
                        RiskLevel.MEDIUM to counts.medium,
                        RiskLevel.LOW to counts.low
                    )

                    items(bands) { (band, count) ->
                        val isSelected = selectedBands.contains(band)
                        val bandColor = colorForRiskLevel(band)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggleBand(band) },
                            label = {
                                Text(
                                    text = "${band.displayName()} ($count)",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkSurface,
                                labelColor = TextSecondary,
                                selectedContainerColor = bandColor.copy(alpha = 0.22f),
                                selectedLabelColor = bandColor
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) bandColor else DarkBorder
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Instant System Apps Toggle Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "System",
                        fontSize = 11.sp,
                        color = if (includeSystemApps) PrimaryLightBlue else TextMuted
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = includeSystemApps,
                        onCheckedChange = { viewModel.toggleIncludeSystemApps(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentGreen,
                            checkedTrackColor = PrimaryBlue
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            // Active category filter tag (if selected)
            if (selectedCategory != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = PrimaryBlue.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryLightBlue.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Category: ${selectedCategory?.displayName()}",
                                fontSize = 12.sp,
                                color = PrimaryLightBlue,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear category filter",
                                tint = PrimaryLightBlue,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { viewModel.selectCategory(null) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main App List or Empty States
            if (counts.total == 0) {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = if (includeSystemApps) "No apps found" else "No user-installed apps found",
                    body = "Try scanning again or toggling '+ System Apps' to include pre-installed OS applications.",
                    modifier = Modifier.weight(1f)
                )
            } else if (apps.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = "No apps match these filters",
                    body = "Try adjusting your search query, clearing risk band filters, or selecting all categories.",
                    actionText = "Clear Filters",
                    onActionClick = { viewModel.clearFilters() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
                ) {
                    items(apps, key = { it.inventory.packageName }) { appReport ->
                        AppRowItem(
                            appReport = appReport,
                            onClick = { onNavigateToAppDetail(appReport.inventory.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppRowItem(
    appReport: AppRiskReport,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${appReport.inventory.appLabel}, ${appReport.riskLevel.displayName()} risk score ${appReport.riskScore}"
            },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(packageName = appReport.inventory.packageName, size = 48.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appReport.inventory.appLabel,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = appReport.inventory.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                RiskBadge(level = appReport.riskLevel, score = appReport.riskScore)
            }

            // Top-2 finding titles as small text
            val findings = appReport.findings.take(2)
            if (findings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    findings.forEach { finding ->
                        Text(
                            text = "• ${finding.title}",
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Tags row: Sideloaded / System
            if (appReport.inventory.isSideloaded || appReport.inventory.isSystemApp) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (appReport.inventory.isSideloaded) {
                        StateChip(text = "Installed outside a store", kind = StateChipKind.WARNING)
                    }
                    if (appReport.inventory.isSystemApp) {
                        StateChip(text = "System app", kind = StateChipKind.NEUTRAL)
                    }
                }
            }
        }
    }
}
