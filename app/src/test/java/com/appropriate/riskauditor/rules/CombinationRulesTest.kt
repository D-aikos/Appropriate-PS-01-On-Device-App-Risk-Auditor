package com.appropriate.riskauditor.rules

import com.appropriate.riskauditor.core.model.*
import com.appropriate.riskauditor.core.rules.CombinationRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinationRulesTest {

    private fun createBaseInventory(
        installerPackage: String? = "com.android.vending",
        targetSdk: Int = 34
    ) = AppInventoryRecord(
        packageName = "com.example.testapp",
        appLabel = "Test App",
        versionName = "1.0",
        versionCode = 1L,
        targetSdk = targetSdk,
        isSystemApp = false,
        installerPackage = installerPackage,
        firstInstallTimeEpochMillis = 1000L,
        lastUpdateTimeEpochMillis = 1000L
    )

    @Test
    fun testSpywareCombinationRule() {
        val inventory = createBaseInventory()
        val permissions = listOf(
            PermissionAudit("android.permission.RECEIVE_BOOT_COMPLETED", "RECEIVE_BOOT_COMPLETED", true, GrantState.GRANTED, RiskCategory.BACKGROUND_AND_PERSISTENCE),
            PermissionAudit("android.permission.READ_CONTACTS", "READ_CONTACTS", true, GrantState.GRANTED, RiskCategory.CONTACTS_AND_CALLS),
            PermissionAudit("android.permission.INTERNET", "INTERNET", true, GrantState.GRANTED, RiskCategory.NETWORK)
        )
        val specialAccess = emptyList<SpecialAccessAudit>()

        val findings = CombinationRules.evaluate(inventory, permissions, specialAccess)
        assertTrue(findings.any { it.ruleId == "RULE_COMBO_SPYWARE" })
    }

    @Test
    fun testTollFraudCombinationRule() {
        val inventory = createBaseInventory()
        val permissions = listOf(
            PermissionAudit("android.permission.SEND_SMS", "SEND_SMS", true, GrantState.GRANTED, RiskCategory.SMS),
            PermissionAudit("android.permission.INTERNET", "INTERNET", true, GrantState.GRANTED, RiskCategory.NETWORK)
        )
        val specialAccess = emptyList<SpecialAccessAudit>()

        val findings = CombinationRules.evaluate(inventory, permissions, specialAccess)
        assertTrue(findings.any { it.ruleId == "RULE_COMBO_TOLL_FRAUD" })
    }

    @Test
    fun testAudioSurveillanceCombinationRule() {
        val inventory = createBaseInventory()
        val permissions = listOf(
            PermissionAudit("android.permission.RECORD_AUDIO", "RECORD_AUDIO", true, GrantState.GRANTED, RiskCategory.CAMERA_AND_MIC),
            PermissionAudit("android.permission.FOREGROUND_SERVICE", "FOREGROUND_SERVICE", true, GrantState.GRANTED, RiskCategory.BACKGROUND_AND_PERSISTENCE),
            PermissionAudit("android.permission.INTERNET", "INTERNET", true, GrantState.GRANTED, RiskCategory.NETWORK)
        )
        val specialAccess = emptyList<SpecialAccessAudit>()

        val findings = CombinationRules.evaluate(inventory, permissions, specialAccess)
        assertTrue(findings.any { it.ruleId == "RULE_COMBO_AUDIO_SURVEILLANCE" })
    }

    @Test
    fun testOverlayStealerCombinationRule() {
        val inventory = createBaseInventory()
        val permissions = listOf(
            PermissionAudit("android.permission.READ_SMS", "READ_SMS", true, GrantState.GRANTED, RiskCategory.SMS)
        )
        val specialAccess = listOf(
            SpecialAccessAudit(SpecialAccessType.SYSTEM_ALERT_WINDOW, SpecialAccessState.ENABLED, true, "Overlay active")
        )

        val findings = CombinationRules.evaluate(inventory, permissions, specialAccess)
        assertTrue(findings.any { it.ruleId == "RULE_COMBO_OVERLAY_STEALER" })
    }

    @Test
    fun testMaliciousDropperCombinationRule() {
        val inventory = createBaseInventory()
        val permissions = listOf(
            PermissionAudit("android.permission.WRITE_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE", true, GrantState.GRANTED, RiskCategory.STORAGE_AND_MEDIA),
            PermissionAudit("android.permission.INTERNET", "INTERNET", true, GrantState.GRANTED, RiskCategory.NETWORK)
        )
        val specialAccess = listOf(
            SpecialAccessAudit(SpecialAccessType.REQUEST_INSTALL_PACKAGES, SpecialAccessState.ENABLED, true, "Install packages active")
        )

        val findings = CombinationRules.evaluate(inventory, permissions, specialAccess)
        assertTrue(findings.any { it.ruleId == "RULE_COMBO_MALICIOUS_DROPPER" })
    }

    @Test
    fun testSideloadedHighRiskRule() {
        // Null installer = Sideloaded
        val inventory = createBaseInventory(installerPackage = null)
        val permissions = listOf(
            PermissionAudit("android.permission.ACCESS_FINE_LOCATION", "ACCESS_FINE_LOCATION", true, GrantState.GRANTED, RiskCategory.LOCATION),
            PermissionAudit("android.permission.RECORD_AUDIO", "RECORD_AUDIO", true, GrantState.GRANTED, RiskCategory.CAMERA_AND_MIC)
        )
        val specialAccess = emptyList<SpecialAccessAudit>()

        val findings = CombinationRules.evaluate(inventory, permissions, specialAccess)
        assertTrue(findings.any { it.ruleId == "RULE_HEURISTIC_SIDELOADED_HIGH_RISK" })
    }

    @Test
    fun testOutdatedTargetSdkRule() {
        val inventory = createBaseInventory(targetSdk = 26) // Android 8.0 < 29
        val permissions = emptyList<PermissionAudit>()
        val specialAccess = emptyList<SpecialAccessAudit>()

        val findings = CombinationRules.evaluate(inventory, permissions, specialAccess)
        assertTrue(findings.any { it.ruleId == "RULE_HEURISTIC_OUTDATED_TARGET_SDK" })
    }
}
