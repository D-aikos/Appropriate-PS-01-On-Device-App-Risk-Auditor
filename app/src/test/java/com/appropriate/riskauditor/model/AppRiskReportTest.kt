package com.appropriate.riskauditor.model

import com.appropriate.riskauditor.core.model.*
import org.junit.Assert.*
import org.junit.Test

class AppRiskReportTest {

    @Test
    fun testAppInventorySideloadDetection() {
        val playStoreApp = AppInventoryRecord(
            packageName = "com.google.android.apps.maps",
            appLabel = "Maps",
            versionName = "11.0",
            versionCode = 100L,
            targetSdk = 34,
            isSystemApp = false,
            installerPackage = "com.android.vending",
            firstInstallTimeEpochMillis = 1000L,
            lastUpdateTimeEpochMillis = 2000L
        )
        assertFalse(playStoreApp.isSideloaded)

        val sideloadedApp = AppInventoryRecord(
            packageName = "com.suspicious.payload",
            appLabel = "Payload",
            versionName = "1.0",
            versionCode = 1L,
            targetSdk = 28,
            isSystemApp = false,
            installerPackage = null, // null installer indicates sideloaded APK
            firstInstallTimeEpochMillis = 1000L,
            lastUpdateTimeEpochMillis = 1000L
        )
        assertTrue(sideloadedApp.isSideloaded)

        val systemApp = AppInventoryRecord(
            packageName = "com.android.settings",
            appLabel = "Settings",
            versionName = "14",
            versionCode = 34L,
            targetSdk = 34,
            isSystemApp = true,
            installerPackage = null,
            firstInstallTimeEpochMillis = 1000L,
            lastUpdateTimeEpochMillis = 1000L
        )
        assertFalse("System apps are never considered sideloaded", systemApp.isSideloaded)
    }

    @Test
    fun testRiskLevelFromScore() {
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(0))
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(24))
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(25))
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(49))
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(50))
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(74))
        assertEquals(RiskLevel.CRITICAL, RiskLevel.fromScore(75))
        assertEquals(RiskLevel.CRITICAL, RiskLevel.fromScore(100))
    }

    @Test
    fun testDeviceScanSummaryOverallLevel() {
        val summaryWithCritical = DeviceScanSummary(
            scanId = "test-1",
            timestampEpochMillis = 1000L,
            appReports = emptyList(),
            totalAppsScanned = 10,
            userAppsScanned = 8,
            systemAppsScanned = 2,
            criticalRiskCount = 1,
            highRiskCount = 2,
            mediumRiskCount = 3,
            lowRiskCount = 4,
            averageRiskScore = 45
        )
        assertEquals(RiskLevel.CRITICAL, summaryWithCritical.overallRiskLevel)

        val summaryClean = DeviceScanSummary(
            scanId = "test-2",
            timestampEpochMillis = 1000L,
            appReports = emptyList(),
            totalAppsScanned = 10,
            userAppsScanned = 8,
            systemAppsScanned = 2,
            criticalRiskCount = 0,
            highRiskCount = 0,
            mediumRiskCount = 0,
            lowRiskCount = 10,
            averageRiskScore = 12
        )
        assertEquals(RiskLevel.LOW, summaryClean.overallRiskLevel)
    }

    @Test
    fun testAppRiskReportPermissionGrouping() {
        val inventory = AppInventoryRecord(
            packageName = "com.test.app",
            appLabel = "Test",
            versionName = "1.0",
            versionCode = 1L,
            targetSdk = 34,
            isSystemApp = false,
            installerPackage = "com.android.vending",
            firstInstallTimeEpochMillis = 1000L,
            lastUpdateTimeEpochMillis = 1000L
        )

        val perms = listOf(
            PermissionAudit("android.permission.SEND_SMS", "SEND_SMS", true, GrantState.GRANTED, RiskCategory.SMS),
            PermissionAudit("android.permission.READ_SMS", "READ_SMS", true, GrantState.DENIED, RiskCategory.SMS),
            PermissionAudit("android.permission.CAMERA", "CAMERA", true, GrantState.GRANTED, RiskCategory.CAMERA_AND_MIC)
        )

        val report = AppRiskReport(
            inventory = inventory,
            permissions = perms,
            specialAccess = emptyList(),
            findings = emptyList(),
            riskScore = 15,
            riskLevel = RiskLevel.LOW,
            scannedAtEpochMillis = 1000L
        )

        assertEquals(2, report.permissionsByCategory.size)
        assertEquals(2, report.permissionsByCategory[RiskCategory.SMS]?.size)
        assertEquals(1, report.permissionsByCategory[RiskCategory.CAMERA_AND_MIC]?.size)
        assertEquals(2, report.grantedPermissionCount)
    }
}
