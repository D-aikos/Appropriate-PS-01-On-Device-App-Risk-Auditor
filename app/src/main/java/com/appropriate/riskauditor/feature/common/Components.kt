package com.appropriate.riskauditor.feature.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appropriate.riskauditor.core.model.RiskLevel

enum class StateChipKind {
    POSITIVE, // e.g. Allowed, On
    NEGATIVE, // e.g. Not allowed, Off
    NEUTRAL,  // e.g. Not requested, Not runtime
    WARNING   // e.g. Couldn't check, Unknown, Sideloaded
}

/**
 * Returns locked band color for a RiskLevel.
 */
fun colorForRiskLevel(level: RiskLevel): Color = when (level) {
    RiskLevel.LOW -> BandLowColor
    RiskLevel.MEDIUM -> BandMediumColor
    RiskLevel.HIGH -> BandHighColor
    RiskLevel.CRITICAL -> BandCriticalColor
}

/**
 * RiskBadge displaying both score and band word, ensuring accessibility without relying on color alone.
 */
@Composable
fun RiskBadge(
    level: RiskLevel,
    score: Int,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false
) {
    val bandColor = colorForRiskLevel(level)
    val word = level.displayName()

    if (isLarge) {
        Surface(
            modifier = modifier
                .semantics { contentDescription = "Risk level $word, score $score out of 100" }
                .border(1.5.dp, bandColor, RoundedCornerShape(12.dp)),
            color = bandColor.copy(alpha = 0.16f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(bandColor)
                )
                Text(
                    text = "$score / 100",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    text = word.uppercase(),
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = bandColor
                )
            }
        }
    } else {
        Surface(
            modifier = modifier
                .semantics { contentDescription = "Risk level $word, score $score" }
                .border(1.dp, bandColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
            color = bandColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(bandColor)
                )
                Text(
                    text = "$score",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
                Text(
                    text = word,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = bandColor
                )
            }
        }
    }
}

/**
 * StateChip presenting a state status with standard semantic kinds.
 */
@Composable
fun StateChip(
    text: String,
    kind: StateChipKind,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor) = when (kind) {
        StateChipKind.POSITIVE -> Triple(
            BandLowColor.copy(alpha = 0.15f),
            Color(0xFF81C784),
            BandLowColor.copy(alpha = 0.5f)
        )
        StateChipKind.NEGATIVE -> Triple(
            BandCriticalColor.copy(alpha = 0.15f),
            Color(0xFFE57373),
            BandCriticalColor.copy(alpha = 0.5f)
        )
        StateChipKind.WARNING -> Triple(
            BandMediumColor.copy(alpha = 0.18f),
            Color(0xFFFFB74D),
            BandMediumColor.copy(alpha = 0.6f)
        )
        StateChipKind.NEUTRAL -> Triple(
            DarkSurfaceVariant,
            TextSecondary,
            DarkBorder
        )
    }

    Surface(
        modifier = modifier
            .semantics { contentDescription = text }
            .border(1.dp, borderColor, RoundedCornerShape(6.dp)),
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * SectionCard wrapping UI sections in a standardized container.
 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                action?.invoke()
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

/**
 * EmptyState for zero results or initial empty screens.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(actionText, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * ErrorBanner for fatal or non-fatal scan failures with retry.
 */
@Composable
fun ErrorBanner(
    text: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BandCriticalColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        color = BandCriticalColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = BandCriticalColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
            if (onRetry != null) {
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BandCriticalColor))
                ) {
                    Text("Retry", fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * PartialBanner for scans with collection warnings or unreadable apps.
 */
@Composable
fun PartialBanner(
    warnings: List<String>,
    modifier: Modifier = Modifier
) {
    if (warnings.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BandMediumColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded },
        color = BandMediumColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = BandMediumColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Some apps could not be fully checked (${warnings.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse warnings" else "Expand warnings",
                    tint = TextSecondary
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    warnings.forEach { warning ->
                        Text(
                            text = "• $warning",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
