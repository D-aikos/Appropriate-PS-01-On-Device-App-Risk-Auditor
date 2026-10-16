package com.appropriate.riskauditor.feature.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appropriate.riskauditor.app.theme.*
import com.appropriate.riskauditor.core.model.AppRiskReport
import com.appropriate.riskauditor.core.model.RiskLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    onNavigateToAppDetail: (String) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedSort by viewModel.selectedSort.collectAsState()
    val apps by viewModel.filteredApps.collectAsState()
    val counts by viewModel.filterCounts.collectAsState()
    val includeSystemApps by viewModel.includeSystemApps.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }

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
                            text = "${counts.all} total apps audited",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort", tint = TextPrimary)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        SortOrder.values().forEach { sort ->
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
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search by app name or package…", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderStrokeColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Filter Chips Row with Live Count Badges
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChipWithCount(
                        title = "All",
                        count = counts.all,
                        isSelected = selectedFilter == RiskFilter.ALL,
                        onClick = { viewModel.selectFilter(RiskFilter.ALL) }
                    )
                }
                item {
                    FilterChipWithCount(
                        title = "Critical",
                        count = counts.critical,
                        isSelected = selectedFilter == RiskFilter.CRITICAL,
                        badgeColor = RiskCriticalColor,
                        onClick = { viewModel.selectFilter(RiskFilter.CRITICAL) }
                    )
                }
                item {
                    FilterChipWithCount(
                        title = "High",
                        count = counts.high,
                        isSelected = selectedFilter == RiskFilter.HIGH,
                        badgeColor = RiskHighColor,
                        onClick = { viewModel.selectFilter(RiskFilter.HIGH) }
                    )
                }
                item {
                    FilterChipWithCount(
                        title = "Medium",
                        count = counts.medium,
                        isSelected = selectedFilter == RiskFilter.MEDIUM,
                        badgeColor = RiskMediumColor,
                        onClick = { viewModel.selectFilter(RiskFilter.MEDIUM) }
                    )
                }
                item {
                    FilterChipWithCount(
                        title = "Low",
                        count = counts.low,
                        isSelected = selectedFilter == RiskFilter.LOW,
                        badgeColor = RiskLowColor,
                        onClick = { viewModel.selectFilter(RiskFilter.LOW) }
                    )
                }
                item {
                    FilterChipWithCount(
                        title = "Sideloaded",
                        count = counts.sideloaded,
                        isSelected = selectedFilter == RiskFilter.SIDELOADED,
                        badgeColor = RiskHighColor,
                        onClick = { viewModel.selectFilter(RiskFilter.SIDELOADED) }
                    )
                }
            }

            // Results Counter & Quick Scope Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${apps.size} matching applications",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                Text(
                    text = "Sorted by: ${selectedSort.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryLightBlue
                )
            }

            // Apps Lazy List
            if (apps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No matching applications found", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Try selecting a different filter or clearing search.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(apps, key = { it.inventory.packageName }) { appReport ->
                        AppListItemRow(
                            appReport = appReport,
                            onClick = { onNavigateToAppDetail(appReport.inventory.packageName) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
fun FilterChipWithCount(
    title: String,
    count: Int,
    isSelected: Boolean,
    badgeColor: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title)
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeColor?.copy(alpha = 0.2f) ?: DarkSurfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "$count",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor ?: TextSecondary
                    )
                }
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = DarkSurface,
            labelColor = TextSecondary,
            selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f),
            selectedLabelColor = PrimaryLightBlue
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = if (isSelected) PrimaryLightBlue else BorderStrokeColor
        )
    )
}

@Composable
fun AppListItemRow(appReport: AppRiskReport, onClick: () -> Unit) {
    val levelColor = when (appReport.riskLevel) {
        RiskLevel.CRITICAL -> RiskCriticalColor
        RiskLevel.HIGH -> RiskHighColor
        RiskLevel.MEDIUM -> RiskMediumColor
        RiskLevel.LOW -> RiskLowColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(1.dp, BorderStrokeColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Avatar / Initials
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .border(1.dp, BorderStrokeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appReport.inventory.appLabel.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryLightBlue
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // App Label and Package
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = appReport.inventory.appLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (appReport.inventory.isSideloaded) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(RiskHighColor.copy(alpha = 0.15f))
                                .border(1.dp, RiskHighColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("Sideloaded", fontSize = 9.sp, color = RiskHighColor, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (appReport.inventory.isSystemApp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("System", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = appReport.inventory.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))
                val topFinding = appReport.findings.firstOrNull()
                if (topFinding != null) {
                    Text(
                        text = "• ${topFinding.title}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryLightBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "${appReport.grantedPermissionCount} granted • Target SDK ${appReport.inventory.targetSdk ?: "N/A"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Risk Score Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(levelColor.copy(alpha = 0.15f))
                    .border(1.dp, levelColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${appReport.riskScore}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = levelColor
                    )
                    Text(
                        text = appReport.riskLevel.name.take(4),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = levelColor
                    )
                }
            }
        }
    }
}
