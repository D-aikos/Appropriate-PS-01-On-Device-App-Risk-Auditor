package com.appropriate.riskauditor.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
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
import com.appropriate.riskauditor.core.model.DeviceScanSummary
import com.appropriate.riskauditor.core.model.RiskCategory
import com.appropriate.riskauditor.core.model.RiskLevel
import com.appropriate.riskauditor.core.scanner.ScanState
import com.appropriate.riskauditor.feature.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAppDetail: (String) -> Unit,
    onNavigateToAppList: () -> Unit,
    onNavigateToAppListWithBand: (RiskLevel) -> Unit,
    onNavigateToAppListWithCategory: (RiskCategory) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val scanState by viewModel.scanState.collectAsState()
    val includeSystemApps by viewModel.includeSystemApps.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentGreen.copy(alpha = 0.15f))
                                .border(1.dp, AccentGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "App Risk Auditor",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "100% On-Device • Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentGreen
                            )
                        }
                    }
                },
                actions = {
                    FilterChip(
                        selected = includeSystemApps,
                        onClick = { viewModel.toggleIncludeSystemApps(!includeSystemApps) },
                        label = {
                            Text(
                                if (includeSystemApps) "+ System Apps" else "User Apps Only",
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DarkSurface,
                            labelColor = TextSecondary,
                            selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryLightBlue
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = includeSystemApps,
                            borderColor = if (includeSystemApps) PrimaryLightBlue else DarkBorder
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // Block 1: Title + Subtitle ("Everything is checked on this phone. Nothing is uploaded.")
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Device Security Assessment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Everything is checked on this phone. Nothing is uploaded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentGreen
                    )
                }
            }

            // Block 2: Primary Button "Scan my apps" / "Cancel scan" & Progress
            item {
                when (val state = scanState) {
                    is ScanState.Idle -> {
                        Button(
                            onClick = { viewModel.startScan() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan my apps", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    is ScanState.Scanning -> {
                        val progress = if (state.total > 0) state.current.toFloat() / state.total else 0f
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, PrimaryBlue.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Checking ${state.appLabel} (${state.current} of ${state.total})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.cancelScan() },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        border = ButtonDefaults.outlinedButtonBorder.copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(BandCriticalColor)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = BandCriticalColor)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Cancel", fontSize = 12.sp, color = BandCriticalColor)
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = PrimaryLightBlue,
                                    trackColor = DarkSurfaceVariant
                                )
                            }
                        }
                    }

                    is ScanState.Success -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.startScan() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rescan apps", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is ScanState.Error -> {
                        ErrorBanner(
                            text = state.message,
                            onRetry = { viewModel.startScan() }
                        )
                    }
                }
            }

            // When scanState is Idle and no previous scan has run
            if (scanState is ScanState.Idle) {
                item {
                    EmptyState(
                        icon = Icons.Default.Security,
                        title = "No audit scan performed yet",
                        body = "Tap 'Scan my apps' above to evaluate installed applications against on-device risk rules, permission combinations, and special access capabilities.",
                        actionText = "Start Scan",
                        onActionClick = { viewModel.startScan() }
                    )
                }
            }

            // Finished state with results
            if (scanState is ScanState.Success) {
                val fullSummary = (scanState as ScanState.Success).summary
                val filteredApps = viewModel.filterReports(fullSummary.appReports, includeSystemApps)

                // Block 3: PartialBanner if warnings exist
                val warnings = mutableListOf<String>()
                if (fullSummary.systemAppsScanned == 0 && includeSystemApps) {
                    warnings.add("System applications could not be fully enumerated.")
                }
                if (warnings.isNotEmpty()) {
                    item {
                        PartialBanner(warnings = warnings)
                    }
                }

                // Block 4: 2x2 Risk Band Grid (Critical, High, Medium, Low)
                item {
                    val criticalCount = filteredApps.count { it.riskLevel == RiskLevel.CRITICAL }
                    val highCount = filteredApps.count { it.riskLevel == RiskLevel.HIGH }
                    val mediumCount = filteredApps.count { it.riskLevel == RiskLevel.MEDIUM }
                    val lowCount = filteredApps.count { it.riskLevel == RiskLevel.LOW }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Risk Band Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Row 1: Critical & High
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BandCard(
                                level = RiskLevel.CRITICAL,
                                count = criticalCount,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToAppListWithBand(RiskLevel.CRITICAL) }
                            )
                            BandCard(
                                level = RiskLevel.HIGH,
                                count = highCount,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToAppListWithBand(RiskLevel.HIGH) }
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        // Row 2: Medium & Low
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BandCard(
                                level = RiskLevel.MEDIUM,
                                count = mediumCount,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToAppListWithBand(RiskLevel.MEDIUM) }
                            )
                            BandCard(
                                level = RiskLevel.LOW,
                                count = lowCount,
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigateToAppListWithBand(RiskLevel.LOW) }
                            )
                        }
                    }
                }

                // Block 5: "Apps that could do the most harm" (Q1): top 5 rows
                item {
                    val topRiskyApps = filteredApps.take(5)
                    SectionCard(
                        title = "Apps that could do the most harm",
                        subtitle = "Top priority applications by privilege exposure",
                        action = {
                            TextButton(onClick = onNavigateToAppList) {
                                Text("View all (${filteredApps.size})", color = PrimaryLightBlue, fontSize = 13.sp)
                            }
                        }
                    ) {
                        if (topRiskyApps.isEmpty()) {
                            Text(
                                text = "No apps audited matching current scope.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                topRiskyApps.forEach { appReport ->
                                    TopAppRow(
                                        appReport = appReport,
                                        onClick = { onNavigateToAppDetail(appReport.inventory.packageName) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Block 6: "What apps can access on this phone": 9 rows in fixed order
                item {
                    val totalApps = filteredApps.size.coerceAtLeast(1)
                    val categoryStats = viewModel.computeCategoryStats(filteredApps)

                    SectionCard(
                        title = "What apps can access on this phone",
                        subtitle = "Device-wide capability footprint across 9 audit categories"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            categoryStats.forEach { stat ->
                                CategoryAccessRow(
                                    stat = stat,
                                    totalApps = totalApps,
                                    onClick = { onNavigateToAppListWithCategory(stat.category) }
                                )
                            }
                        }
                    }
                }

                // Block 7: Footer link "Privacy & limits" -> Settings tab
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(onClick = onNavigateToSettings) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryLightBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Privacy & limits: How this on-device audit works",
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryLightBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun BandCard(
    level: RiskLevel,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bandColor = colorForRiskLevel(level)
    val word = level.displayName()

    Card(
        modifier = modifier
            .semantics { contentDescription = "$word risk band, $count apps. Tap to view list." }
            .border(1.5.dp, bandColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = word,
                    fontWeight = FontWeight.Bold,
                    color = bandColor,
                    fontSize = 15.sp
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(bandColor)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$count",
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    color = Color.White
                )
                Text(
                    text = if (count == 1) "app" else "apps",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TopAppRow(
    appReport: AppRiskReport,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = DarkSurfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(packageName = appReport.inventory.packageName, size = 44.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appReport.inventory.appLabel,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    RiskBadge(level = appReport.riskLevel, score = appReport.riskScore)
                }
                Spacer(modifier = Modifier.height(2.dp))
                val topFindingTitle = appReport.findings.firstOrNull()?.title ?: "Standard permission profile"
                Text(
                    text = topFindingTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CategoryAccessRow(
    stat: CategoryUsageStat,
    totalApps: Int,
    onClick: () -> Unit
) {
    val ratio = if (totalApps > 0) stat.appsWithAccess.toFloat() / totalApps else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stat.category.displayName(),
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 13.sp
                )
                Text(
                    text = "${stat.appsWithAccess} apps use it",
                    fontSize = 12.sp,
                    color = if (stat.appsWithAccess > 0) PrimaryLightBlue else TextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (stat.category == RiskCategory.SPECIAL_ACCESS) BandCriticalColor else PrimaryLightBlue,
                trackColor = DarkSurfaceVariant
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${stat.appsDeclaring} ask for it",
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}
