package com.appropriate.riskauditor.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appropriate.riskauditor.app.theme.*
import com.appropriate.riskauditor.core.model.RiskLevel
import com.appropriate.riskauditor.core.storage.ScanHistoryEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val historyList by viewModel.scanHistory.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Scan History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
                title = { Text("Clear Scan History?", color = TextPrimary) },
                text = { Text("This will permanently remove all saved local audit logs from Room database.", color = TextSecondary) },
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
                    Text(text = "Perform a device audit on the Dashboard to save history.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
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
                        onDeleteClick = { viewModel.deleteScan(scanItem.scanId) }
                    )
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
fun HistoryItemCard(item: ScanHistoryEntity, onDeleteClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy  •  HH:mm", Locale.getDefault())
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
                    Text(text = "Scan ID: ${item.scanId.take(8)}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Total Apps: ${item.totalAppsScanned} (${item.userAppsScanned} User, ${item.systemAppsScanned} System)",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))
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
fun SmallPill(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}
