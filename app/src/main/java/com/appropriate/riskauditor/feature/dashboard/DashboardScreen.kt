package com.appropriate.riskauditor.feature.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appropriate.riskauditor.app.theme.*
import com.appropriate.riskauditor.core.model.*
import com.appropriate.riskauditor.core.scanner.ScanState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAppDetail: (String) -> Unit,
    onNavigateToAppList: () -> Unit
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
                        label = { Text(if (includeSystemApps) "+ System Apps" else "User Apps Only", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DarkSurface,
                            labelColor = TextSecondary,
                            selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryLightBlue
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = includeSystemApps,
                            borderColor = if (includeSystemApps) PrimaryLightBlue else BorderStrokeColor
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

            // 1. Hero Status Card
            item {
                when (val state = scanState) {
                    is ScanState.Idle -> {
                        IdleHeroCard(onScanClick = { viewModel.startScan() }, includeSystem = includeSystemApps)
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

            // 2. Scan Results Sections (Risk Distribution, Category Breakdown & Threat Flags)
            if (scanState is ScanState.Success) {
                val summary = (scanState as ScanState.Success).summary

                // Risk Distribution Grid
                item {
                    RiskDistributionCard(summary = summary, onNavigateToAppList = onNavigateToAppList)
                }

                // Category Exposure Overview
                item {
                    CategoryOverviewCard(summary = summary)
                }

                // Header for Priority Threat Flags
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
                    items(highPriorityApps.take(6)) { appReport ->
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
fun IdleHeroCard(onScanClick: () -> Unit, includeSystem: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderStrokeColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.15f))
                    .border(1.dp, PrimaryBlue.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = PrimaryLightBlue,
                    modifier = Modifier.size(38.dp)
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
                text = "Audit installed packages, manifest declarations vs runtime grants, and special access capabilities (Accessibility, Device Admin, Overlays).",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Audit Scope Hint
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (includeSystem) "Scope: Auditing User and System apps" else "Scope: Auditing User-installed apps only",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onScanClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Full Device Audit", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun ScanningHeroCard(current: Int, total: Int, appLabel: String) {
    val progress = if (total > 0) current.toFloat() / total else 0f

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PrimaryBlue.copy(alpha = pulseAlpha), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Auditing Applications…",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "$current / $total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryLightBlue
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = AccentGreen,
                trackColor = DarkSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device Security Posture",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = summary.overallRiskLevel.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = postureColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${summary.totalAppsScanned} apps audited (${summary.userAppsScanned} user, ${summary.systemAppsScanned} system)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Custom Animated Circular Score Gauge
                ScoreGauge(
                    score = summary.averageRiskScore,
                    color = postureColor,
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRescanClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderStrokeColor))
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
fun ScoreGauge(score: Int, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            // Track arc
            drawArc(
                color = Color(0xFF334155),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Progress arc
            val sweep = (score / 100f) * 270f
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "AVG",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Risk Band Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Rubric 0-100",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DistributionPill("Critical\n(75-100)", summary.criticalRiskCount, RiskCriticalColor, Modifier.weight(1f))
                DistributionPill("High\n(50-74)", summary.highRiskCount, RiskHighColor, Modifier.weight(1f))
                DistributionPill("Medium\n(25-49)", summary.mediumRiskCount, RiskMediumColor, Modifier.weight(1f))
                DistributionPill("Low\n(0-24)", summary.lowRiskCount, RiskLowColor, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun CategoryOverviewCard(summary: DeviceScanSummary) {
    // Count apps requesting each of the key sensitive categories
    val specialAccessCount = summary.appReports.count { it.specialAccess.any { a -> a.state == SpecialAccessState.ENABLED } }
    val smsCount = summary.appReports.count { it.permissions.any { p -> p.category == RiskCategory.SMS && p.grantState == GrantState.GRANTED } }
    val locCount = summary.appReports.count { it.permissions.any { p -> p.category == RiskCategory.LOCATION && p.grantState == GrantState.GRANTED } }
    val camCount = summary.appReports.count { it.permissions.any { p -> p.category == RiskCategory.CAMERA_AND_MIC && p.grantState == GrantState.GRANTED } }
    val sideloadCount = summary.appReports.count { it.inventory.isSideloaded }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderStrokeColor, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sensitive Surface Exposure",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { CategoryBadge("Special Access", "$specialAccessCount apps", RiskCriticalColor) }
                item { CategoryBadge("SMS Access", "$smsCount apps", RiskHighColor) }
                item { CategoryBadge("Location", "$locCount apps", RiskMediumColor) }
                item { CategoryBadge("Camera/Mic", "$camCount apps", PrimaryLightBlue) }
                item { CategoryBadge("Sideloaded", "$sideloadCount apps", RiskHighColor) }
            }
        }
    }
}

@Composable
fun CategoryBadge(title: String, subtitle: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = subtitle, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

@Composable
fun DistributionPill(label: String, count: Int, color: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$count", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                color = TextSecondary
            )
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
            .border(1.dp, levelColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = appReport.inventory.appLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (appReport.inventory.isSideloaded) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(RiskHighColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Sideloaded", fontSize = 9.sp, color = RiskHighColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
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
                        .border(1.dp, levelColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
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
                    text = "• ${topFinding.title}: ${topFinding.evidence}",
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
            .border(1.dp, AccentGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
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
                Text(text = "All audited apps fall within standard baseline security permissions.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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
