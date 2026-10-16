package com.appropriate.riskauditor.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appropriate.riskauditor.core.model.RiskLevel
import com.appropriate.riskauditor.core.storage.ScanAppSummaryEntity
import com.appropriate.riskauditor.core.storage.ScanHistoryEntity
import com.appropriate.riskauditor.feature.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToAppDetail: (String) -> Unit = {}
) {
    val historyList by viewModel.scanHistory.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedScanForDetail by remember { mutableStateOf<ScanHistoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Audit Scan History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Stored locally in sandboxed Room database",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                },
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear History", tint = TextSecondary)
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
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear All Scan History?", color = TextPrimary) },
                text = { Text("This will permanently remove all saved local audit snapshots from Room database.", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAll()
                            showClearDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BandCriticalColor)
                    ) {
                        Text("Clear All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = DarkSurface
            )
        }

        // Scan Detail Modal BottomSheet
        selectedScanForDetail?.let { scan ->
            val appsForScan by viewModel.getAppsForScan(scan.scanId).collectAsState(initial = emptyList())

            ModalBottomSheet(
                onDismissRequest = { selectedScanForDetail = null },
                containerColor = DarkSurface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = DarkBorder) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Scan Snapshot Details",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = formatDate(scan.timestampEpochMillis),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        IconButton(onClick = { selectedScanForDetail = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatPill("Total", "${scan.totalAppsScanned}", DarkSurfaceVariant)
                        StatPill("Critical", "${scan.criticalCount}", BandCriticalColor.copy(alpha = 0.25f))
                        StatPill("High", "${scan.highCount}", BandHighColor.copy(alpha = 0.25f))
                        StatPill("Medium", "${scan.mediumCount}", BandMediumColor.copy(alpha = 0.25f))
                        StatPill("Low", "${scan.lowCount}", BandLowColor.copy(alpha = 0.25f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Audited Applications (${appsForScan.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(appsForScan, key = { it.packageName }) { appItem ->
                            HistoricalAppRow(appItem = appItem)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (historyList.isEmpty()) {
            EmptyState(
                icon = Icons.Default.History,
                title = "No scans yet",
                body = "Completed on-device audits will be saved here automatically for offline review and diffing.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                items(historyList, key = { it.scanId }) { scanItem ->
                    ScanHistoryCard(
                        scan = scanItem,
                        onClick = { selectedScanForDetail = scanItem }
                    )
                }
            }
        }
    }
}

@Composable
fun ScanHistoryCard(
    scan: ScanHistoryEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatDate(scan.timestampEpochMillis),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${scan.totalAppsScanned} apps audited • Avg score: ${scan.averageRiskScore}/100",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                StateChip(text = "Complete", kind = StateChipKind.POSITIVE)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Risk count pill breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (scan.criticalCount > 0) {
                    StateChip(text = "${scan.criticalCount} Critical", kind = StateChipKind.NEGATIVE)
                }
                if (scan.highCount > 0) {
                    StateChip(text = "${scan.highCount} High", kind = StateChipKind.WARNING)
                }
                if (scan.mediumCount > 0) {
                    StateChip(text = "${scan.mediumCount} Medium", kind = StateChipKind.WARNING)
                }
                if (scan.lowCount > 0) {
                    StateChip(text = "${scan.lowCount} Low", kind = StateChipKind.POSITIVE)
                }
            }
        }
    }
}

@Composable
private fun HistoricalAppRow(appItem: ScanAppSummaryEntity) {
    val level = RiskLevel.valueOf(appItem.riskLevel)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp)),
        color = DarkSurfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appItem.appLabel,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = appItem.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            RiskBadge(level = level, score = appItem.riskScore)
        }
    }
}

@Composable
private fun RowScope.StatPill(label: String, value: String, bgColor: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Text(text = label, fontSize = 10.sp, color = TextSecondary)
        }
    }
}
