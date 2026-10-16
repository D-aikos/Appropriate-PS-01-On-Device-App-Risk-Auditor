package com.appropriate.riskauditor.feature.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appropriate.riskauditor.app.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val includeSystemApps by viewModel.includeSystemApps.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Policy",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
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
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Audit Scope Configuration
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderStrokeColor, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Audit Scope Configuration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Include System Applications",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "When enabled, audits pre-installed OS system apps alongside user-installed apps. System apps typically hold elevated privileges by design.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = includeSystemApps,
                                onCheckedChange = { viewModel.setIncludeSystemApps(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AccentGreen,
                                    checkedTrackColor = PrimaryBlue
                                )
                            )
                        }
                    }
                }
            }

            // 2. Privacy & Security Model Guarantee
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AccentGreen.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = AccentGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "100% Offline & On-Device",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "• Zero Internet Permission: This application does not declare android.permission.INTERNET.\n" +
                                    "• No Cloud Transmission: Your app inventory, permissions, hashes, and audit logs are never uploaded to any cloud server or remote LLM.\n" +
                                    "• Sandboxed Local History: Scan results are stored exclusively in private SQLite database storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // 3. Scoring Rubric & Policy Details
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderStrokeColor, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = PrimaryLightBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Scoring Rubric & Three-Reason Rule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "• Special Access Priority: Accessibility, Device Admin, Overlay, and Package Install receive the highest point weighting.\n" +
                                    "• Declared vs Granted: Declared permissions that are not granted receive minimal points.\n" +
                                    "• Three-Reason Enforcement: Every High (50-74) or Critical (75-100) score must be supported by at least 3 distinct, evidence-backed findings to prevent false alarms.\n" +
                                    "• Heuristics are not Proof: A high score represents elevated privilege exposure, not conclusive malware.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // 4. Version & Build Information
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "On-Device App Risk Auditor v1.0.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = "PS-01 Native Android Security Auditing Project",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
