package com.appropriate.riskauditor.rules

import com.appropriate.riskauditor.core.model.*
import com.appropriate.riskauditor.core.rules.RiskScoringEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskScoringEngineTest {

    private fun createBaseInventory(pkg: String = "com.test.app") = AppInventoryRecord(
        packageName = pkg,
        appLabel = "Test Application",
        versionName = "1.0",
        versionCode = 1L,
        targetSdk = 34,
        isSystemApp = false,
        installerPackage = "com.android.vending",
        firstInstallTimeEpochMillis = 1000L,
        lastUpdateTimeEpochMillis = 1000L
    )

    @Test
    fun testDeterministicEvaluation() {
        val inventory = createBaseInventory()
        val permissions = listOf(
            PermissionAudit("android.permission.CAMERA", "CAMERA", true, GrantState.GRANTED, RiskCategory.CAMERA_AND_MIC),
            PermissionAudit("android.permission.RECORD_AUDIO", "RECORD_AUDIO", true, GrantState.GRANTED, RiskCategory.CAMERA_AND_MIC),
            PermissionAudit("android.permission.INTERNET", "INTERNET", true, GrantState.GRANTED, RiskCategory.NETWORK)
        )
        val specialAccess = listOf(
            SpecialAccessAudit(SpecialAccessType.SYSTEM_ALERT_WINDOW, SpecialAccessState.ENABLED, true, "Overlay")
        )

        val run1 = RiskScoringEngine.evaluate(inventory, permissions, specialAccess, scannedAtEpochMillis = 12345L)
        val run2 = RiskScoringEngine.evaluate(inventory, permissions, specialAccess, scannedAtEpochMillis = 12345L)

        assertEquals(run1.riskScore, run2.riskScore)
        assertEquals(run1.riskLevel, run2.riskLevel)
        assertEquals(run1.findings.size, run2.findings.size)
        assertEquals(run1.findings.map { it.ruleId }, run2.findings.map { it.ruleId })
    }

    @Test
    fun testScoreClampingBetweenZeroAndOneHundred() {
        val inventory = createBaseInventory()
        // Accumulate overwhelming access
        val permissions = listOf(
            PermissionAudit("android.permission.SEND_SMS", "SEND_SMS", true, GrantState.GRANTED, RiskCategory.SMS),
            PermissionAudit("android.permission.READ_SMS", "READ_SMS", true, GrantState.GRANTED, RiskCategory.SMS),
            PermissionAudit("android.permission.READ_CONTACTS", "READ_CONTACTS", true, GrantState.GRANTED, RiskCategory.CONTACTS_AND_CALLS),
            PermissionAudit("android.permission.READ_CALL_LOG", "READ_CALL_LOG", true, GrantState.GRANTED, RiskCategory.CONTACTS_AND_CALLS),
            PermissionAudit("android.permission.CAMERA", "CAMERA", true, GrantState.GRANTED, RiskCategory.CAMERA_AND_MIC),
            PermissionAudit("android.permission.RECORD_AUDIO", "RECORD_AUDIO", true, GrantState.GRANTED, RiskCategory.CAMERA_AND_MIC),
            PermissionAudit("android.permission.ACCESS_FINE_LOCATION", "ACCESS_FINE_LOCATION", true, GrantState.GRANTED, RiskCategory.LOCATION),
            PermissionAudit("android.permission.RECEIVE_BOOT_COMPLETED", "RECEIVE_BOOT_COMPLETED", true, GrantState.GRANTED, RiskCategory.BACKGROUND_AND_PERSISTENCE),
            PermissionAudit("android.permission.INTERNET", "INTERNET", true, GrantState.GRANTED, RiskCategory.NETWORK)
        )
        val specialAccess = listOf(
            SpecialAccessAudit(SpecialAccessType.DEVICE_ADMIN, SpecialAccessState.ENABLED, true, "Active Admin"),
            SpecialAccessAudit(SpecialAccessType.ACCESSIBILITY_SERVICE, SpecialAccessState.ENABLED, true, "Accessibility Active"),
            SpecialAccessAudit(SpecialAccessType.SYSTEM_ALERT_WINDOW, SpecialAccessState.ENABLED, true, "Overlay Active"),
            SpecialAccessAudit(SpecialAccessType.REQUEST_INSTALL_PACKAGES, SpecialAccessState.ENABLED, true, "Install Packages Active")
        )

        val report = RiskScoringEngine.evaluate(inventory, permissions, specialAccess)
        assertTrue("Score must not exceed 100", report.riskScore <= 100)
        assertTrue("Score must be at least 0", report.riskScore >= 0)
        assertEquals(RiskLevel.CRITICAL, report.riskLevel)
    }

    @Test
    fun testBenignAppScoresInLowBand() {
        val inventory = createBaseInventory()
        // App with only INTERNET and ACCESS_NETWORK_STATE
        val permissions = listOf(
            PermissionAudit("android.permission.INTERNET", "INTERNET", true, GrantState.GRANTED, RiskCategory.NETWORK),
            PermissionAudit("android.permission.ACCESS_NETWORK_STATE", "ACCESS_NETWORK_STATE", true, GrantState.GRANTED, RiskCategory.NETWORK)
        )
        val specialAccess = emptyList<SpecialAccessAudit>()

        val report = RiskScoringEngine.evaluate(inventory, permissions, specialAccess)
        assertEquals(RiskLevel.LOW, report.riskLevel)
        assertTrue(report.riskScore in 0..24)
    }

    @Test
    fun testDeclaredOnlyPermissionsReceiveMinimalWeight() {
        val inventory = createBaseInventory()
        // Declared but NOT granted permissions
        val permissions = listOf(
            PermissionAudit("android.permission.SEND_SMS", "SEND_SMS", true, GrantState.DENIED, RiskCategory.SMS),
            PermissionAudit("android.permission.CAMERA", "CAMERA", true, GrantState.DENIED, RiskCategory.CAMERA_AND_MIC),
            PermissionAudit("android.permission.ACCESS_FINE_LOCATION", "ACCESS_FINE_LOCATION", true, GrantState.DENIED, RiskCategory.LOCATION)
        )
        val specialAccess = emptyList<SpecialAccessAudit>()

        val report = RiskScoringEngine.evaluate(inventory, permissions, specialAccess)
        // Score should stay in LOW band because none are granted
        assertEquals(RiskLevel.LOW, report.riskLevel)
        assertTrue(report.riskScore < 25)
    }
}
