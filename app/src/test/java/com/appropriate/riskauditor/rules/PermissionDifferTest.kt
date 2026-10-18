package com.appropriate.riskauditor.rules

import com.appropriate.riskauditor.core.model.*
import com.appropriate.riskauditor.core.rules.PermissionDiffer
import org.junit.Assert.*
import org.junit.Test

class PermissionDifferTest {

    private fun createApp(
        permissions: List<PermissionAudit> = emptyList(),
        specialAccess: List<SpecialAccessAudit> = emptyList()
    ): AppRiskReport {
        return AppRiskReport(
            inventory = AppInventoryRecord(
                packageName = "com.diff.test",
                appLabel = "Diff Test App",
                versionName = "1.0",
                versionCode = 1L,
                targetSdk = 34,
                isSystemApp = false,
                installerPackage = "com.android.vending",
                firstInstallTimeEpochMillis = 1000L,
                lastUpdateTimeEpochMillis = 1000L
            ),
            permissions = permissions,
            specialAccess = specialAccess,
            findings = emptyList(),
            riskScore = 10,
            riskLevel = RiskLevel.LOW,
            scannedAtEpochMillis = 1000L
        )
    }

    @Test
    fun testDiffWithNullOldReturnsEmpty() {
        val newApp = createApp(
            permissions = listOf(
                PermissionAudit("android.permission.READ_SMS", grantState = GrantState.GRANTED, category = RiskCategory.SMS)
            )
        )
        val diff = PermissionDiffer.diff(null, newApp)
        assertTrue(diff.isEmpty())
    }

    @Test
    fun testDiffWithNewSmsPermission() {
        val oldApp = createApp(
            permissions = listOf(
                PermissionAudit("android.permission.INTERNET", grantState = GrantState.GRANTED, category = RiskCategory.NETWORK)
            )
        )
        val newApp = createApp(
            permissions = listOf(
                PermissionAudit("android.permission.INTERNET", grantState = GrantState.GRANTED, category = RiskCategory.NETWORK),
                PermissionAudit("android.permission.SEND_SMS", grantState = GrantState.GRANTED, category = RiskCategory.SMS)
            )
        )

        val diff = PermissionDiffer.diff(oldApp, newApp)
        assertEquals(1, diff.size)
        assertTrue(diff[0].contains("SEND_SMS"))
        assertTrue(diff[0].contains("SMS"))
    }

    @Test
    fun testDiffWithSpecialAccessMovedToEnabled() {
        val oldApp = createApp(
            specialAccess = listOf(
                SpecialAccessAudit(SpecialAccessType.ACCESSIBILITY_SERVICE, SpecialAccessState.DISABLED, isDeclared = true)
            )
        )
        val newApp = createApp(
            specialAccess = listOf(
                SpecialAccessAudit(SpecialAccessType.ACCESSIBILITY_SERVICE, SpecialAccessState.ENABLED, isDeclared = true)
            )
        )

        val diff = PermissionDiffer.diff(oldApp, newApp)
        assertEquals(1, diff.size)
        assertTrue(diff[0].contains("Special access turned on"))
        assertTrue(diff[0].contains("Accessibility Service"))
    }
}
