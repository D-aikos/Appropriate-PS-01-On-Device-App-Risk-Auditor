package com.appropriate.riskauditor.feature.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appropriate.riskauditor.app.theme.*
import com.appropriate.riskauditor.core.model.AppRiskReport
import com.appropriate.riskauditor.core.model.DeviceScanSummary
import com.appropriate.riskauditor.core.model.RiskLevel
import com.appropriate.riskauditor.core.scanner.ScanState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAppDetail: (String) -> Unit,
    onNavigateToAppList: () -> Unit
) {
    val scanState by viewModel.scanState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Risk Auditor",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Hero Status Card / Scan Trigger
            item {
                when (val state = scanState) {
                    is ScanState.Idle -> {
                        IdleHeroCard(onScanClick = { viewModel.startScan() })
                    }
                    is ScanState.Scanning -> {
                        ScanningHeroCard(current = state.current, total = state.total, appLabel = state.appLabel)
                    }
                    is ScanState.Success -> {
                        SummaryHeroCard(
                            summary = state.summary,
                            onRescanClick = { viewModel.startScan() },
                            onExportPdfClick = { viewModel.exportPdfReport(state.summary) }
                        )
                    }
                    is ScanState.Error -> {
                        ErrorCard(message = state.message, onRetryClick = { viewModel.startScan() })
                    }
                }
            }

            // If we have a successful scan, show distribution & high priority alerts
            if (scanState is ScanState.Success) {
                val summary = (scanState as ScanState.Success).summary

                item {
                    RiskDistributionCard(summary = summary, onNavigateToAppList = onNavigateToAppList)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Priority Threat Flags",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        TextButton(onClick = onNavigateToAppList) {
                            Text("View All (${summary.totalAppsScanned})", color = PrimaryLightBlue)
                        }
                    }
                }

                val highPriorityApps = summary.highAndCriticalReports
                if (highPriorityApps.isEmpty()) {
                    item {
                        CleanDeviceCard()
                    }
                } else {
                    items(highPriorityApps.take(5)) { appReport ->
                        PriorityAppCard(
                            appReport = appReport,
                            onClick = { onNavigateToAppDetail(appReport.inventory.packageName) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun IdleHeroCard(onScanClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderStrokeColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = PrimaryLightBlue,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "On-Device Security Audit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Scan all installed applications locally to detect excessive permissions, special platform access, and potential spyware vectors.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onScanClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Full Device Audit", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ScanningHeroCard(current: Int, total: Int, appLabel: String) {
    val progress = if (total > 0) current.toFloat() / total else 0f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PrimaryBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(24.dp),
                    color = AccentGreen,
                    trackColor = DarkSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Auditing Applications… ($current / $total)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = AccentGreen,
                trackColor = DarkSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Inspecting: $appLabel",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SummaryHeroCard(
    summary: DeviceScanSummary,
    onRescanClick: () -> Unit,
    onExportPdfClick: () -> Unit
) {
    val postureColor = when (summary.overallRiskLevel) {
        RiskLevel.CRITICAL -> RiskCriticalColor
        RiskLevel.HIGH -> RiskHighColor
        RiskLevel.MEDIUM -> RiskMediumColor
        RiskLevel.LOW -> RiskLowColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, postureColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Device Posture",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = summary.overallRiskLevel.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = postureColor
                    )
                }

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(postureColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${summary.averageRiskScore}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = postureColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Scanned ${summary.totalAppsScanned} apps (${summary.userAppsScanned} user-installed, ${summary.systemAppsScanned} system apps).",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRescanClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Re-Audit")
                }

                Button(
                    onClick = onExportPdfClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF Report")
                }
            }
        }
    }
}

@Composable
fun RiskDistributionCard(summary: DeviceScanSummary, onNavigateToAppList: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderStrokeColor, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Risk Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DistributionPill("Critical", summary.criticalRiskCount, RiskCriticalColor, Modifier.weight(1f))
                DistributionPill("High", summary.highRiskCount, RiskHighColor, Modifier.weight(1f))
                DistributionPill("Medium", summary.mediumRiskCount, RiskMediumColor, Modifier.weight(1f))
                DistributionPill("Low", summary.lowRiskCount, RiskLowColor, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun DistributionPill(label: String, count: Int, color: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$count", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
fun PriorityAppCard(appReport: AppRiskReport, onClick: () -> Unit) {
    val levelColor = if (appReport.riskLevel == RiskLevel.CRITICAL) RiskCriticalColor else RiskHighColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(1.dp, levelColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appReport.inventory.appLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = appReport.inventory.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(levelColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${appReport.riskScore} [${appReport.riskLevel.displayName.take(4)}]",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = levelColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            val topFinding = appReport.findings.firstOrNull()
            if (topFinding != null) {
                Text(
                    text = "• ${topFinding.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryLightBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CleanDeviceCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AccentGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = "No High or Critical Threats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = "All audited apps fall within normal security baselines.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
fun ErrorCard(message: String, onRetryClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = RiskCriticalColor, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Audit Error", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(14.dp))
            Button(onClick = onRetryClick) { Text("Retry Scan") }
        }
    }
}
