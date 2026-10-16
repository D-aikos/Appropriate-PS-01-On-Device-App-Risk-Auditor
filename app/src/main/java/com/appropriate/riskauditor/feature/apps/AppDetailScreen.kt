package com.appropriate.riskauditor.feature.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appropriate.riskauditor.app.theme.*
import com.appropriate.riskauditor.core.model.*
import com.appropriate.riskauditor.core.platform.SystemNavigator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    packageName: String,
    viewModel: AppDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val appReport = remember(packageName) { viewModel.getAppReport(packageName) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Application Audit Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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
        if (appReport == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("App data not found. Please rescan.", color = TextSecondary)
            }
            return@Scaffold
        }

        val levelColor = when (appReport.riskLevel) {
            RiskLevel.CRITICAL -> RiskCriticalColor
            RiskLevel.HIGH -> RiskHighColor
            RiskLevel.MEDIUM -> RiskMediumColor
            RiskLevel.LOW -> RiskLowColor
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // 1. Identity & Risk Score Header Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, levelColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(DarkSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = appReport.inventory.appLabel.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryLightBlue
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = appReport.inventory.appLabel,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = appReport.inventory.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(levelColor.copy(alpha = 0.15f))
                                    .border(1.dp, levelColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${appReport.riskScore}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = levelColor
                                    )
                                    Text(
                                        text = appReport.riskLevel.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = levelColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = BorderStrokeColor, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Metadata Grid
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val installedStr = appReport.inventory.firstInstallTimeEpochMillis?.let { dateFormat.format(Date(it)) } ?: "Unknown"
                        val updatedStr = appReport.inventory.lastUpdateTimeEpochMillis?.let { dateFormat.format(Date(it)) } ?: "Unknown"

                        MetadataRow("Version", "${appReport.inventory.versionName ?: "N/A"} (Code ${appReport.inventory.versionCode ?: "N/A"})")
                        MetadataRow("Target SDK", "${appReport.inventory.targetSdk ?: "Unknown"}")
                        MetadataRow("Installer", appReport.inventory.installerPackage ?: "Sideloaded / Direct APK")
                        MetadataRow("First Installed", installedStr)
                        MetadataRow("Last Updated", updatedStr)
                        MetadataRow("App Type", if (appReport.inventory.isSystemApp) "Pre-installed System App" else "User-Installed App")
                    }
                }
            }

            // 2. Safe Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { SystemNavigator.openAppDetails(context, appReport.inventory.packageName) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("App Settings")
                    }

                    if (!appReport.inventory.isSystemApp) {
                        OutlinedButton(
                            onClick = { SystemNavigator.promptUninstall(context, appReport.inventory.packageName) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskCriticalColor),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(RiskCriticalColor.copy(alpha = 0.5f))),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Uninstall")
                        }
                    }
                }
            }

            // 3. Why Flagged Section (Findings & Evidence)
            item {
                Text(
                    text = "Why Flagged (${appReport.findings.size} Evidence Findings)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (appReport.findings.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderStrokeColor, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "No risk findings detected for this application.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(appReport.findings) { finding ->
                    FindingCard(finding = finding)
                }
            }

            // 4. Special Access Checks Section
            item {
                Text(
                    text = "Special Access Capabilities",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            items(appReport.specialAccess) { access ->
                SpecialAccessCard(access = access)
            }

            // 5. Permissions Matrix Grouped by Category
            item {
                Text(
                    text = "Permissions Matrix (${appReport.permissions.size} Total)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            for ((category, perms) in appReport.permissionsByCategory) {
                item {
                    CategoryPermissionsCard(category = category, permissions = perms)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

@Composable
fun FindingCard(finding: RiskFinding) {
    val sevColor = when (finding.severity) {
        FindingSeverity.CRITICAL -> RiskCriticalColor
        FindingSeverity.HIGH -> RiskHighColor
        FindingSeverity.MEDIUM -> RiskMediumColor
        FindingSeverity.LOW -> RiskLowColor
        FindingSeverity.INFO -> PrimaryLightBlue
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, sevColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(sevColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = finding.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(sevColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "+${finding.points} pts",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = sevColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Evidence: ${finding.evidence}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryLightBlue
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = finding.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = finding.recommendation,
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGreen
                )
            }
        }
    }
}

@Composable
fun SpecialAccessCard(access: SpecialAccessAudit) {
    val (statusText, statusColor) = when (access.state) {
        SpecialAccessState.ENABLED -> Pair("ENABLED", RiskCriticalColor)
        SpecialAccessState.DISABLED -> Pair("DECLARED (DISABLED)", RiskMediumColor)
        SpecialAccessState.NOT_DECLARED -> Pair("NOT DECLARED", TextMuted)
        SpecialAccessState.UNKNOWN -> Pair("UNKNOWN", RiskHighColor)
        SpecialAccessState.NOT_SUPPORTED -> Pair("NOT SUPPORTED", TextMuted)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderStrokeColor, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = access.type.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                if (access.detail != null) {
                    Text(
                        text = access.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun CategoryPermissionsCard(category: RiskCategory, permissions: List<PermissionAudit>) {
    var expanded by remember { mutableStateOf(false) }

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
                Text(
                    text = "${category.displayName} (${permissions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Show Details", color = PrimaryLightBlue)
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                for (perm in permissions) {
                    val (grantText, grantColor) = when (perm.grantState) {
                        GrantState.GRANTED -> Pair("GRANTED", AccentGreen)
                        GrantState.DENIED -> Pair("DENIED", RiskHighColor)
                        GrantState.NOT_RUNTIME -> Pair("NORMAL", TextMuted)
                        GrantState.UNKNOWN -> Pair("UNKNOWN", RiskMediumColor)
                        GrantState.NOT_SUPPORTED -> Pair("NOT SUPPORTED", TextMuted)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = perm.simpleName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(grantColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = grantText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = grantColor
                            )
                        }
                    }
                }
            }
        }
    }
}
