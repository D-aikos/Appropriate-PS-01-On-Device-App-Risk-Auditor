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
import com.appropriate.riskauditor.app.theme.*
import com.appropriate.riskauditor.core.model.RiskLevel
import com.appropriate.riskauditor.core.storage.ScanAppSummaryEntity
import com.appropriate.riskauditor.core.storage.ScanHistoryEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
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
                        colors = ButtonDefaults.buttonColors(containerColor = RiskCriticalColor)
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

        // Scan Detail Modal BottomSheet / Dialog
        selectedScanForDetail?.let { scan ->
            val appsForScan by viewModel.getAppsForScan(scan.scanId).collectAsState(initial = emptyList())

            ModalBottomSheet(
                onDismissRequest = { selectedScanForDetail = null },
                containerColor = DarkSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val dateFormat = SimpleDateFormat("MMM dd, yyyy  •  HH:mm", Locale.getDefault())
                            Text(
                                text = "Scan Snapshot: ${dateFormat.format(Date(scan.timestampEpochMillis))}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${appsForScan.size} apps captured in this scan",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        IconButton(onClick = { selectedScanForDetail = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(appsForScan, key = { it.packageName }) { appItem ->
                            HistoricalAppItemRow(appItem = appItem)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = TextMuted, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "No Previous Scans Recorded", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Run an on-device audit from the Dashboard to record scan logs.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(historyList, key = { it.scanId }) { scanItem ->
                    HistoryItemCard(
                        item = scanItem,
                        onClick = { selectedScanForDetail = scanItem },
                        onDeleteClick = { viewModel.deleteScan(scanItem.scanId) }
                    )
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    item: ScanHistoryEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy  •  HH:mm:ss", Locale.getDefault())
    val dateStr = dateFormat.format(Date(item.timestampEpochMillis))

    val level = RiskLevel.fromScore(item.averageRiskScore)
    val levelColor = when (level) {
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
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = dateStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "Scan ID: ${item.scanId.take(8)} • Tap to inspect apps", style = MaterialTheme.typography.labelSmall, color = PrimaryLightBlue)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${item.totalAppsScanned} Apps Audited (${item.userAppsScanned} User, ${item.systemAppsScanned} System) • Avg Score: ${item.averageRiskScore}/100",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SmallPill("Crit: ${item.criticalCount}", RiskCriticalColor)
                SmallPill("High: ${item.highCount}", RiskHighColor)
                SmallPill("Med: ${item.mediumCount}", RiskMediumColor)
                SmallPill("Low: ${item.lowCount}", RiskLowColor)
            }
        }
    }
}

@Composable
fun HistoricalAppItemRow(appItem: ScanAppSummaryEntity) {
    val levelColor = when (appItem.riskLevel) {
        "CRITICAL" -> RiskCriticalColor
        "HIGH" -> RiskHighColor
        "MEDIUM" -> RiskMediumColor
        else -> RiskLowColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appItem.appLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = appItem.topFindingTitle ?: "${appItem.findingsCount} findings",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(levelColor.copy(alpha = 0.15f))
                .border(1.dp, levelColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${appItem.riskScore} [${appItem.riskLevel.take(4)}]",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = levelColor
            )
        }
    }
}

@Composable
fun SmallPill(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}
