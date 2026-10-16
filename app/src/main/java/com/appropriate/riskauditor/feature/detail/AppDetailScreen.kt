package com.appropriate.riskauditor.feature.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appropriate.riskauditor.core.model.*
import com.appropriate.riskauditor.core.platform.SystemNavigator
import com.appropriate.riskauditor.feature.common.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    packageName: String,
    viewModel: AppDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val appReport = remember(packageName) { viewModel.getAppReport(packageName) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showUninstallConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "App Risk Assessment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" }
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            if (appReport != null) {
                // Section 7: Sticky Bottom Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    color = DarkSurface,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (appReport.inventory.isSystemApp) {
                            Text(
                                text = "System apps can't be removed here",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Open App Settings Button
                            OutlinedButton(
                                onClick = {
                                    val opened = SystemNavigator.openAppDetails(context, appReport.inventory.packageName)
                                    if (!opened) {
                                        scope.launch { snackbarHostState.showSnackbar("Unable to launch system settings.") }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .semantics { contentDescription = "Open app system settings" },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(PrimaryLightBlue)
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("App Settings", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }

                            // Uninstall Button (Hidden for system apps)
                            if (!appReport.inventory.isSystemApp) {
                                Button(
                                    onClick = { showUninstallConfirmDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .semantics { contentDescription = "Uninstall app" },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BandCriticalColor)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Uninstall", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground
    ) { paddingValues ->
        // Uninstall Confirmation Dialog
        if (showUninstallConfirmDialog && appReport != null) {
            AlertDialog(
                onDismissRequest = { showUninstallConfirmDialog = false },
                title = { Text("Uninstall ${appReport.inventory.appLabel}?", color = TextPrimary) },
                text = {
                    Text(
                        text = "The system will ask you to confirm before removing this application from your device.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showUninstallConfirmDialog = false
                            val opened = SystemNavigator.promptUninstall(context, appReport.inventory.packageName)
                            if (!opened) {
                                scope.launch { snackbarHostState.showSnackbar("Could not start uninstallation dialog.") }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BandCriticalColor)
                    ) {
                        Text("Continue to Uninstall")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUninstallConfirmDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = DarkSurface
            )
        }

        if (appReport == null) {
            EmptyState(
                icon = Icons.Default.Info,
                title = "App audit report not found in active session",
                body = "Please return to the dashboard and perform a scan.",
                actionText = "Back to Dashboard",
                onActionClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
        ) {
            // Section 1: Header (64dp icon, label, package, version name (code), large RiskBadge)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colorForRiskLevel(appReport.riskLevel).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(packageName = appReport.inventory.packageName, size = 64.dp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = appReport.inventory.appLabel,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = appReport.inventory.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val verName = appReport.inventory.versionName ?: "Unknown"
                                val verCode = appReport.inventory.versionCode ?: 0L
                                Text(
                                    text = "Version $verName ($verCode)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Security Risk Score:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            RiskBadge(
                                level = appReport.riskLevel,
                                score = appReport.riskScore,
                                isLarge = true
                            )
                        }
                    }
                }
            }

            // Section 2: Why Flagged (Q2)
            item {
                SectionCard(
                    title = "Why flagged",
                    subtitle = "Evidence-backed security reasons for this score"
                ) {
                    // Capped for evidence notification if score was capped to 49
                    val isCapped = appReport.riskScore <= 49 && appReport.findings.size < 3 && appReport.riskScore >= 25
                    if (isCapped) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BandMediumColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                            color = BandMediumColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = BandMediumColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Score limited to Medium: fewer than three independent reasons were found.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (appReport.findings.isEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, DarkBorder, RoundedCornerShape(10.dp)),
                            color = DarkSurfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = BandLowColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "No risky access found. This does not prove the app is safe.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            appReport.findings.forEach { finding ->
                                FindingCard(finding = finding)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    // Mandatory footer note
                    Text(
                        text = "Permissions alone do not prove an app is malicious.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Section 3: What you can do now (Q3)
            item {
                val distinctActions = appReport.findings
                    .map { it.recommendation }
                    .distinct()

                SectionCard(
                    title = "What you can do now",
                    subtitle = "Actionable steps to reduce privilege exposure"
                ) {
                    if (distinctActions.isEmpty()) {
                        Text(
                            text = "No immediate privilege mitigations needed. Standard app permissions apply.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            distinctActions.forEach { recommendation ->
                                ActionRecommendationCard(
                                    recommendation = recommendation,
                                    packageName = appReport.inventory.packageName,
                                    onActionInvoked = { actionName, success ->
                                        if (!success) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Couldn't open that screen. Opening app settings instead.")
                                            }
                                            SystemNavigator.openAppDetails(context, appReport.inventory.packageName)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: Special Access (6 rows)
            item {
                SectionCard(
                    title = "Special access capabilities",
                    subtitle = "System-level capabilities carrying elevated privilege weight"
                ) {
                    val specialTypes = listOf(
                        SpecialAccessType.ACCESSIBILITY_SERVICE,
                        SpecialAccessType.DEVICE_ADMIN,
                        SpecialAccessType.NOTIFICATION_LISTENER,
                        SpecialAccessType.SYSTEM_ALERT_WINDOW,
                        SpecialAccessType.REQUEST_INSTALL_PACKAGES,
                        SpecialAccessType.USAGE_STATS
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        specialTypes.forEach { type ->
                            val audit = appReport.specialAccess.find { it.type == type }
                            val state = audit?.state ?: SpecialAccessState.NOT_DECLARED
                            SpecialAccessRow(
                                type = type,
                                state = state,
                                detail = audit?.detail
                            )
                        }
                    }
                }
            }

            // Section 5: Permissions by category (9 + Other, collapsible)
            item {
                SectionCard(
                    title = "Permissions by category",
                    subtitle = "Detailed manifest declarations and runtime grant states"
                ) {
                    val allCategories = RiskCategory.values().toList()

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allCategories.forEach { category ->
                            val perms = appReport.permissions.filter { it.category == category }
                            CategoryPermissionsExpandable(
                                category = category,
                                permissions = perms
                            )
                        }
                    }
                }
            }

            // Section 6: App Info
            item {
                SectionCard(
                    title = "App information & install origin",
                    subtitle = "Package provenance and platform metadata"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoRow(label = "Version", value = "${appReport.inventory.versionName ?: "N/A"} (${appReport.inventory.versionCode ?: 0})")
                        InfoRow(label = "Target SDK", value = "Android ${appReport.inventory.targetSdk ?: "Unknown"} (API ${appReport.inventory.targetSdk ?: 0})")
                        InfoRow(label = "First Installed", value = formatDate(appReport.inventory.firstInstallTimeEpochMillis ?: 0L))
                        InfoRow(label = "Last Updated", value = formatDate(appReport.inventory.lastUpdateTimeEpochMillis ?: 0L))
                        InfoRow(
                            label = "Install Source",
                            value = installSourceDisplayName(
                                isSideloaded = appReport.inventory.isSideloaded,
                                installerPackage = appReport.inventory.installerPackage,
                                isSystemApp = appReport.inventory.isSystemApp
                            )
                        )
                        InfoRow(label = "Package Type", value = if (appReport.inventory.isSystemApp) "Pre-installed System App" else "User-Installed Application")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }
}

@Composable
private fun FindingCard(finding: RiskFinding) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = finding.title,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = BandCriticalColor.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BandCriticalColor.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "+${finding.points} pts",
                        color = Color(0xFFEF9A9A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Plain language explanation (<= 2 sentences)
            Text(
                text = finding.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            // Evidence Chips
            if (finding.evidence.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Evidence:", fontSize = 11.sp, color = TextMuted)
                    StateChip(text = finding.evidence, kind = StateChipKind.NEUTRAL)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Confidence & Heuristic Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Confidence: ${finding.confidence.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                // If heuristic rule (e.g. context mismatch or combination)
                if (finding.ruleId.contains("CTX") || finding.ruleId.contains("COMBO")) {
                    Text(
                        text = "Based on a guess about app's purpose",
                        fontSize = 10.sp,
                        color = BandMediumColor,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRecommendationCard(
    recommendation: String,
    packageName: String,
    onActionInvoked: (actionName: String, success: Boolean) -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PrimaryBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        color = DarkSurfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = recommendation,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val opened = when {
                        recommendation.contains("accessibility", ignoreCase = true) ->
                            SystemNavigator.openAccessibilitySettings(context, packageName)
                        recommendation.contains("notification", ignoreCase = true) ->
                            SystemNavigator.openNotificationListenerSettings(context, packageName)
                        recommendation.contains("overlay", ignoreCase = true) || recommendation.contains("display over", ignoreCase = true) ->
                            SystemNavigator.openOverlaySettings(context, packageName)
                        recommendation.contains("install", ignoreCase = true) ->
                            SystemNavigator.openInstallSourcesSettings(context, packageName)
                        recommendation.contains("usage", ignoreCase = true) ->
                            SystemNavigator.openUsageAccessSettings(context, packageName)
                        recommendation.contains("admin", ignoreCase = true) ->
                            SystemNavigator.openDeviceAdminSettings(context, packageName)
                        else ->
                            SystemNavigator.openAppDetails(context, packageName)
                    }
                    onActionInvoked("action", opened)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Icon(imageVector = Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        recommendation.contains("accessibility", ignoreCase = true) -> "Open Accessibility Settings"
                        recommendation.contains("notification", ignoreCase = true) -> "Open Notification Listener Settings"
                        recommendation.contains("overlay", ignoreCase = true) -> "Manage Overlay Permissions"
                        recommendation.contains("install", ignoreCase = true) -> "Review Install Unknown Apps"
                        recommendation.contains("usage", ignoreCase = true) -> "Open Usage Access Settings"
                        recommendation.contains("admin", ignoreCase = true) -> "Review Device Administrators"
                        else -> "Open App Settings"
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun SpecialAccessRow(
    type: SpecialAccessType,
    state: SpecialAccessState,
    detail: String?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp)),
        color = DarkSurfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = type.displayName(),
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))

                val chipKind = when (state) {
                    SpecialAccessState.ENABLED -> StateChipKind.POSITIVE
                    SpecialAccessState.DISABLED -> StateChipKind.NEGATIVE
                    SpecialAccessState.NOT_DECLARED -> StateChipKind.NEUTRAL
                    SpecialAccessState.UNKNOWN -> StateChipKind.WARNING
                    SpecialAccessState.NOT_SUPPORTED -> StateChipKind.NEUTRAL
                }

                StateChip(
                    text = state.displayName(),
                    kind = chipKind
                )
            }

            if (detail != null && detail.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun CategoryPermissionsExpandable(
    category: RiskCategory,
    permissions: List<PermissionAudit>
) {
    val askedCount = permissions.size
    val allowedCount = permissions.count { it.grantState == GrantState.GRANTED }

    // Expanded by default if there are allowed permissions in this category
    var expanded by remember { mutableStateOf(allowedCount > 0) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp)),
        color = DarkSurfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.displayName(),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$askedCount asked, $allowedCount allowed",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (allowedCount > 0) PrimaryLightBlue else TextMuted
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = TextSecondary
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                if (permissions.isEmpty()) {
                    Text(
                        text = "No permissions requested in this category.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        permissions.forEach { perm ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = permissionShortName(perm.name),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    StateChip(text = "Asked for", kind = StateChipKind.NEUTRAL)
                                    val grantKind = when (perm.grantState) {
                                        GrantState.GRANTED -> StateChipKind.POSITIVE
                                        GrantState.DENIED -> StateChipKind.NEGATIVE
                                        GrantState.UNKNOWN -> StateChipKind.WARNING
                                        else -> StateChipKind.NEUTRAL
                                    }
                                    StateChip(text = perm.grantState.displayName(), kind = grantKind)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
