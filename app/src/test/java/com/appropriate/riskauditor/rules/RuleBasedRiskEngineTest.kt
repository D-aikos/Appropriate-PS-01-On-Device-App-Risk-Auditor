package com.appropriate.riskauditor.rules

import com.appropriate.riskauditor.core.model.*
import com.appropriate.riskauditor.core.rules.RiskPolicy
import com.appropriate.riskauditor.core.rules.RiskScoringEngine
import org.junit.Assert.*
import org.junit.Test

/**
 * Golden test suite implementing all 16 vectors (T1–T16) from
 * Implementation Plan v2 Section 7.6.
 */
class RuleBasedRiskEngineTest {

    private fun record(
        appLabel: String = "Test App",
        packageName: String = "com.test.app",
        isSystem: Boolean = false,
        installer: String? = "com.android.vending",
        permissions: List<PermissionAudit> = emptyList(),
        specialAccess: List<SpecialAccessAudit> = emptyList()
    ): AppRiskReport {
        val inventory = AppInventoryRecord(
            packageName = packageName,
            appLabel = appLabel,
            versionName = "1.0",
            versionCode = 1L,
            targetSdk = 34,
            isSystemApp = isSystem,
            installerPackage = installer,
            firstInstallTimeEpochMillis = 1000L,
            lastUpdateTimeEpochMillis = 1000L
        )
        return RiskScoringEngine.evaluate(inventory, permissions, specialAccess, scannedAtEpochMillis = 1000L)
    }

    private fun perm(
        name: String,
        grant: GrantState = GrantState.GRANTED,
        category: RiskCategory = com.appropriate.riskauditor.core.rules.CategoryMapper.mapPermission(name)
    ) = PermissionAudit(
        name = if (name.startsWith("android.permission.")) name else "android.permission.$name",
        grantState = grant,
        category = category
    )

    private fun special(
        type: SpecialAccessType,
        state: SpecialAccessState,
        isDeclared: Boolean = true,
        detail: String? = null
    ) = SpecialAccessAudit(
        type = type,
        state = state,
        isDeclared = isDeclared,
        detail = detail
    )

    @Test
    fun testT1_superTorchSideloaded() {
        val perms = listOf(
            perm("READ_SMS", GrantState.GRANTED, RiskCategory.SMS),
            perm("READ_CONTACTS", GrantState.GRANTED, RiskCategory.CONTACTS_AND_CALLS),
            perm("INTERNET", GrantState.GRANTED, RiskCategory.NETWORK),
            perm("RECEIVE_BOOT_COMPLETED", GrantState.GRANTED, RiskCategory.BACKGROUND_AND_PERSISTENCE)
        )
        val report = record(
            appLabel = "Super Torch",
            packageName = "com.torch.flashlight",
            isSystem = false,
            installer = null, // Sideloaded
            permissions = perms
        )

        // Expected: CAT_SMS 14 + CAT_CONTACTS 8 + CAT_BACKGROUND 3 + PROV 8 +
        //           COMBO_SMS_NET_BOOT 25 + COMBO_HARVEST 10 + CTX_MISMATCH 15 = 83 CRITICAL, 7 findings
        assertEquals(83, report.riskScore)
        assertEquals(RiskLevel.CRITICAL, report.riskLevel)
        assertEquals(7, report.findings.size)
        val ruleIds = report.findings.map { it.ruleId }.toSet()
        assertTrue(ruleIds.contains("CAT_SMS"))
        assertTrue(ruleIds.contains("CAT_CONTACTS_CALLS"))
        assertTrue(ruleIds.contains("CAT_BACKGROUND"))
        assertTrue(ruleIds.contains("PROV_SIDELOADED"))
        assertTrue(ruleIds.contains("COMBO_SMS_NET_BOOT_SIDELOAD"))
        assertTrue(ruleIds.contains("COMBO_HARVEST"))
        assertTrue(ruleIds.contains("CTX_MISMATCH"))
    }

    @Test
    fun testT2_storeAppAccessibilityOnly() {
        val report = record(
            installer = "com.android.vending",
            specialAccess = listOf(
                special(SpecialAccessType.ACCESSIBILITY_SERVICE, SpecialAccessState.ENABLED)
            )
        )
        // Expected: 30 MEDIUM, 1 finding, cappedForEvidence=false
        assertEquals(30, report.riskScore)
        assertEquals(RiskLevel.MEDIUM, report.riskLevel)
        assertEquals(1, report.findings.size)
        assertEquals("SA_ACCESSIBILITY", report.findings[0].ruleId)
        assertFalse(RiskScoringEngine.wasCappedForEvidence(report.riskScore, report.findings))
    }

    @Test
    fun testT3_sideloadedAccessibilityAndOverlayEnabled() {
        val report = record(
            installer = null, // Sideloaded
            specialAccess = listOf(
                special(SpecialAccessType.ACCESSIBILITY_SERVICE, SpecialAccessState.ENABLED),
                special(SpecialAccessType.SYSTEM_ALERT_WINDOW, SpecialAccessState.ENABLED)
            )
        )
        // Expected: 30 + 18 + 25 combo + 8 prov = 81 CRITICAL, 4 findings
        assertEquals(81, report.riskScore)
        assertEquals(RiskLevel.CRITICAL, report.riskLevel)
        assertEquals(4, report.findings.size)
        val ruleIds = report.findings.map { it.ruleId }.toSet()
        assertEquals(setOf("SA_ACCESSIBILITY", "SA_OVERLAY", "COMBO_ACCESS_OVERLAY", "PROV_SIDELOADED"), ruleIds)
    }

    @Test
    fun testT4_sideloadedAccessibilityAndOverlayDisabled() {
        val report = record(
            installer = null,
            specialAccess = listOf(
                special(SpecialAccessType.ACCESSIBILITY_SERVICE, SpecialAccessState.DISABLED),
                special(SpecialAccessType.SYSTEM_ALERT_WINDOW, SpecialAccessState.DISABLED)
            )
        )
        // Expected: 9 + 5 + 8 (combo 25*0.3=7.5->8) = 22 LOW; no PROV (nothing granted/enabled)
        assertEquals(22, report.riskScore)
        assertEquals(RiskLevel.LOW, report.riskLevel)
        assertEquals(3, report.findings.size)
        val ruleIds = report.findings.map { it.ruleId }.toSet()
        assertEquals(setOf("SA_ACCESSIBILITY", "SA_OVERLAY", "COMBO_ACCESS_OVERLAY"), ruleIds)
    }

    @Test
    fun testT5_storeAppNotificationListenerAndInternet() {
        val report = record(
            installer = "com.android.vending",
            permissions = listOf(
                perm("INTERNET", GrantState.GRANTED, RiskCategory.NETWORK)
            ),
            specialAccess = listOf(
                special(SpecialAccessType.NOTIFICATION_LISTENER, SpecialAccessState.ENABLED)
            )
        )
        // Expected: 22 + 12 = 34 MEDIUM
        assertEquals(34, report.riskScore)
        assertEquals(RiskLevel.MEDIUM, report.riskLevel)
        assertEquals(2, report.findings.size)
        val ruleIds = report.findings.map { it.ruleId }.toSet()
        assertEquals(setOf("SA_NOTIF_LISTENER", "COMBO_NOTIF_NET"), ruleIds)
    }

    @Test
    fun testT6_storeMessengerMultipleGrantedPermissions() {
        val perms = listOf(
            perm("READ_CONTACTS", GrantState.GRANTED, RiskCategory.CONTACTS_AND_CALLS),
            perm("CAMERA", GrantState.GRANTED, RiskCategory.CAMERA_AND_MIC),
            perm("RECORD_AUDIO", GrantState.GRANTED, RiskCategory.CAMERA_AND_MIC),
            perm("ACCESS_COARSE_LOCATION", GrantState.GRANTED, RiskCategory.LOCATION),
            perm("ACCESS_FINE_LOCATION", GrantState.GRANTED, RiskCategory.LOCATION),
            perm("READ_PHONE_STATE", GrantState.GRANTED, RiskCategory.PHONE_AND_IDENTITY),
            perm("RECEIVE_BOOT_COMPLETED", GrantState.GRANTED, RiskCategory.BACKGROUND_AND_PERSISTENCE),
            perm("FOREGROUND_SERVICE", GrantState.GRANTED, RiskCategory.BACKGROUND_AND_PERSISTENCE),
            perm("READ_MEDIA_IMAGES", GrantState.GRANTED, RiskCategory.STORAGE_AND_MEDIA)
        )
        val report = record(
            installer = "com.android.vending",
            permissions = perms
        )
        // Expected: contacts 8, cam/mic 16 (cap), location 9, phone 4, bg 5, storage 5 = 47 MEDIUM
        assertEquals(47, report.riskScore)
        assertEquals(RiskLevel.MEDIUM, report.riskLevel)
        assertEquals(6, report.findings.size)
    }

    @Test
    fun testT7_storeCalculatorPlusContactsGranted() {
        val report = record(
            appLabel = "Calculator Plus",
            installer = "com.android.vending",
            permissions = listOf(
                perm("READ_CONTACTS", GrantState.GRANTED, RiskCategory.CONTACTS_AND_CALLS)
            )
        )
        // Expected: CAT_CONTACTS 8 + CTX_MISMATCH 15 = 23 LOW
        assertEquals(23, report.riskScore)
        assertEquals(RiskLevel.LOW, report.riskLevel)
        assertEquals(2, report.findings.size)
        val ruleIds = report.findings.map { it.ruleId }.toSet()
        assertEquals(setOf("CAT_CONTACTS_CALLS", "CTX_MISMATCH"), ruleIds)
    }

    @Test
    fun testT8_accessibilityEnabledAndDeviceAdminActiveNonMdm() {
        val report = record(
            packageName = "com.untrusted.adminapp",
            specialAccess = listOf(
                special(SpecialAccessType.ACCESSIBILITY_SERVICE, SpecialAccessState.ENABLED),
                special(SpecialAccessType.DEVICE_ADMIN, SpecialAccessState.ENABLED, detail = "Active Device Admin")
            )
        )
        // Expected: 30 + 25 + 18 = 73 HIGH (3 findings)
        assertEquals(73, report.riskScore)
        assertEquals(RiskLevel.HIGH, report.riskLevel)
        assertEquals(3, report.findings.size)
        val ruleIds = report.findings.map { it.ruleId }.toSet()
        assertEquals(setOf("SA_ACCESSIBILITY", "SA_DEVICE_ADMIN", "COMBO_ADMIN_NON_MGMT"), ruleIds)
    }

    @Test
    fun testT9_deviceAdminActiveDeviceOwnerTrue() {
        val report = record(
            specialAccess = listOf(
                special(SpecialAccessType.DEVICE_ADMIN, SpecialAccessState.ENABLED, detail = "Device owner application")
            )
        )
        // Expected: 0 for SA_DEVICE_ADMIN, LOW
        assertEquals(0, report.riskScore)
        assertEquals(RiskLevel.LOW, report.riskLevel)
        assertEquals(0, report.findings.size)
    }

    @Test
    fun testT10_unknownAccessibilityState() {
        val report = record(
            appLabel = "Mystery App",
            specialAccess = listOf(
                special(SpecialAccessType.ACCESSIBILITY_SERVICE, SpecialAccessState.UNKNOWN)
            )
        )
        // Expected: 15, finding present, explanation contains "did not tell us", never "disabled"
        assertEquals(15, report.riskScore)
        assertEquals(1, report.findings.size)
        val finding = report.findings[0]
        assertEquals("SA_ACCESSIBILITY", finding.ruleId)
        assertTrue(finding.explanation.contains("did not tell us"))
        assertFalse(finding.explanation.contains("disabled", ignoreCase = true))
    }

    @Test
    fun testT11_threeReasonEvidenceCapping() {
        // Construct: SA_DEVICE_ADMIN active (25) + SA_ACCESSIBILITY enabled (30), device owner false,
        // package in MDM allowlist (com.google.android.gms) so COMBO_ADMIN is OFF.
        val report = record(
            packageName = "com.google.android.gms",
            specialAccess = listOf(
                special(SpecialAccessType.ACCESSIBILITY_SERVICE, SpecialAccessState.ENABLED),
                special(SpecialAccessType.DEVICE_ADMIN, SpecialAccessState.ENABLED, detail = "Active Admin")
            )
        )
        // Raw score is 55 (High) with only 2 findings.
        // Expected: 55 capped to 49 MEDIUM, wasCappedForEvidence=true
        assertEquals(49, report.riskScore)
        assertEquals(RiskLevel.MEDIUM, report.riskLevel)
        assertEquals(2, report.findings.size)
        assertTrue(RiskScoringEngine.wasCappedForEvidence(55, report.findings))
    }

    @Test
    fun testT12_determinism() {
        val perms = listOf(
            perm("READ_SMS", GrantState.GRANTED, RiskCategory.SMS),
            perm("READ_CONTACTS", GrantState.GRANTED, RiskCategory.CONTACTS_AND_CALLS),
            perm("INTERNET", GrantState.GRANTED, RiskCategory.NETWORK),
            perm("RECEIVE_BOOT_COMPLETED", GrantState.GRANTED, RiskCategory.BACKGROUND_AND_PERSISTENCE)
        )
        val run1 = record(
            appLabel = "Super Torch",
            packageName = "com.torch.flashlight",
            installer = null,
            permissions = perms
        )
        val run2 = record(
            appLabel = "Super Torch",
            packageName = "com.torch.flashlight",
            installer = null,
            permissions = perms
        )

        assertEquals(run1.riskScore, run2.riskScore)
        assertEquals(run1.riskLevel, run2.riskLevel)
        assertEquals(run1.findings.size, run2.findings.size)
        assertEquals(run1.findings.map { it.ruleId }, run2.findings.map { it.ruleId })
        assertEquals(run1.findings.map { it.points }, run2.findings.map { it.points })
    }

    @Test
    fun testT13_emptyPermissionsAndSpecialNotDeclared() {
        val report = record(
            permissions = emptyList(),
            specialAccess = SpecialAccessType.values().map {
                special(it, SpecialAccessState.NOT_DECLARED, isDeclared = false)
            }
        )
        // Expected: 0 LOW, no findings
        assertEquals(0, report.riskScore)
        assertEquals(RiskLevel.LOW, report.riskLevel)
        assertTrue(report.findings.isEmpty())
    }

    @Test
    fun testT14_maxOutRecordClampedAtOneHundred() {
        val perms = listOf(
            perm("READ_SMS", GrantState.GRANTED, RiskCategory.SMS),
            perm("SEND_SMS", GrantState.GRANTED, RiskCategory.SMS),
            perm("RECEIVE_SMS", GrantState.GRANTED, RiskCategory.SMS),
            perm("READ_CONTACTS", GrantState.GRANTED, RiskCategory.CONTACTS_AND_CALLS),
            perm("READ_CALL_LOG", GrantState.GRANTED, RiskCategory.CONTACTS_AND_CALLS),
            perm("CAMERA", GrantState.GRANTED, RiskCategory.CAMERA_AND_MIC),
            perm("RECORD_AUDIO", GrantState.GRANTED, RiskCategory.CAMERA_AND_MIC),
            perm("ACCESS_FINE_LOCATION", GrantState.GRANTED, RiskCategory.LOCATION),
            perm("ACCESS_BACKGROUND_LOCATION", GrantState.GRANTED, RiskCategory.LOCATION),
            perm("READ_PHONE_STATE", GrantState.GRANTED, RiskCategory.PHONE_AND_IDENTITY),
            perm("MANAGE_EXTERNAL_STORAGE", GrantState.GRANTED, RiskCategory.STORAGE_AND_MEDIA),
            perm("RECEIVE_BOOT_COMPLETED", GrantState.GRANTED, RiskCategory.BACKGROUND_AND_PERSISTENCE),
            perm("INTERNET", GrantState.GRANTED, RiskCategory.NETWORK)
        )
        val specials = listOf(
            special(SpecialAccessType.ACCESSIBILITY_SERVICE, SpecialAccessState.ENABLED),
            special(SpecialAccessType.DEVICE_ADMIN, SpecialAccessState.ENABLED),
            special(SpecialAccessType.SYSTEM_ALERT_WINDOW, SpecialAccessState.ENABLED),
            special(SpecialAccessType.REQUEST_INSTALL_PACKAGES, SpecialAccessState.ENABLED),
            special(SpecialAccessType.NOTIFICATION_LISTENER, SpecialAccessState.ENABLED),
            special(SpecialAccessType.USAGE_STATS, SpecialAccessState.ENABLED)
        )
        val report = record(
            appLabel = "Super Max Torch",
            installer = null, // Sideloaded
            permissions = perms,
            specialAccess = specials
        )

        assertEquals(100, report.riskScore)
        assertEquals(RiskLevel.CRITICAL, report.riskLevel)
        assertTrue(report.findings.size >= 3)
    }

    @Test
    fun testT15_boundaries() {
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(24))
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(25))
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(49))
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(50))
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(74))
        assertEquals(RiskLevel.CRITICAL, RiskLevel.fromScore(75))
    }

    @Test
    fun testT16_duplicatePermissionNamesCountedOnce() {
        val perms = listOf(
            perm("READ_SMS", GrantState.GRANTED, RiskCategory.SMS),
            perm("android.permission.READ_SMS", GrantState.GRANTED, RiskCategory.SMS),
            perm("READ_SMS", GrantState.DENIED, RiskCategory.SMS)
        )
        val report = record(
            installer = "com.android.vending",
            permissions = perms
        )

        // READ_SMS has weight 14, cap 30. Should be counted once with highest multiplier (1.0 = 14)
        assertEquals(14, report.riskScore)
        assertEquals(1, report.findings.size)
        assertEquals("CAT_SMS", report.findings[0].ruleId)
        assertEquals(14, report.findings[0].points)
    }
}
