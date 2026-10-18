package com.appropriate.riskauditor.rules

import com.appropriate.riskauditor.core.model.*
import com.appropriate.riskauditor.core.rules.DeviceSummaryCalculator
import org.junit.Assert.*
import org.junit.Test

class DeviceSummaryCalculatorTest {

    private fun createApp(
        label: String,
        pkg: String,
        score: Int,
        level: RiskLevel,
        isSystem: Boolean = false,
        permissions: List<PermissionAudit> = emptyList(),
        specialAccess: List<SpecialAccessAudit> = emptyList()
    ): AppRiskReport {
        return AppRiskReport(
            inventory = AppInventoryRecord(
                packageName = pkg,
                appLabel = label,
                versionName = "1.0",
                versionCode = 1L,
                targetSdk = 34,
                isSystemApp = isSystem,
                installerPackage = "com.android.vending",
                firstInstallTimeEpochMillis = 1000L,
                lastUpdateTimeEpochMillis = 1000L
            ),
            permissions = permissions,
            specialAccess = specialAccess,
            findings = emptyList(),
            riskScore = score,
            riskLevel = level,
            scannedAtEpochMillis = 1000L
        )
    }

    @Test
    fun testEmptyAppList() {
        val summary = DeviceSummaryCalculator.calculate(emptyList())
        assertEquals(0, summary.totalApps)
        assertEquals(4, summary.bandCounts.size)
        assertEquals(0, summary.bandCounts[RiskLevel.LOW])
        assertEquals(0, summary.bandCounts[RiskLevel.MEDIUM])
        assertEquals(0, summary.bandCounts[RiskLevel.HIGH])
        assertEquals(0, summary.bandCounts[RiskLevel.CRITICAL])
        assertEquals(9, summary.categoryStats.size)
        assertTrue(summary.topRisky.isEmpty())
    }

    @Test
    fun testBandCountsAndTopRiskySorting() {
        val app1 = createApp("Alpha App", "com.alpha", 20, RiskLevel.LOW)
        val app2 = createApp("Beta App", "com.beta", 80, RiskLevel.CRITICAL)
        val app3 = createApp("Gamma App", "com.gamma", 60, RiskLevel.HIGH)
        val app4 = createApp("Delta App", "com.delta", 40, RiskLevel.MEDIUM)
        val app5 = createApp("Epsilon App", "com.epsilon", 60, RiskLevel.HIGH) // Tied with Gamma, 'E' < 'G'

        val summary = DeviceSummaryCalculator.calculate(listOf(app1, app2, app3, app4, app5), top = 3)
        assertEquals(5, summary.totalApps)
        assertEquals(1, summary.bandCounts[RiskLevel.LOW])
        assertEquals(1, summary.bandCounts[RiskLevel.MEDIUM])
        assertEquals(2, summary.bandCounts[RiskLevel.HIGH])
        assertEquals(1, summary.bandCounts[RiskLevel.CRITICAL])

        assertEquals(3, summary.topRisky.size)
        assertEquals("com.beta", summary.topRisky[0].inventory.packageName) // 80
        assertEquals("com.epsilon", summary.topRisky[1].inventory.packageName) // 60, Epsilon before Gamma
        assertEquals("com.gamma", summary.topRisky[2].inventory.packageName) // 60
    }

    @Test
    fun testCategoryStatsFixedOrderAndAccessCounting() {
        val smsPerm = PermissionAudit("android.permission.READ_SMS", grantState = GrantState.GRANTED, category = RiskCategory.SMS)
        val camPerm = PermissionAudit("android.permission.CAMERA", grantState = GrantState.DENIED, category = RiskCategory.CAMERA_AND_MIC)
        val a11yAccess = SpecialAccessAudit(SpecialAccessType.ACCESSIBILITY_SERVICE, SpecialAccessState.ENABLED, true)

        val app = createApp(
            label = "Test App",
            pkg = "com.test",
            score = 50,
            level = RiskLevel.HIGH,
            permissions = listOf(smsPerm, camPerm),
            specialAccess = listOf(a11yAccess)
        )

        val summary = DeviceSummaryCalculator.calculate(listOf(app))
        assertEquals(9, summary.categoryStats.size)

        // Fixed order: SPECIAL_ACCESS, SMS, CONTACTS, CAMERA_MIC, LOCATION, PHONE, STORAGE, BACKGROUND, NETWORK
        assertEquals(RiskCategory.SPECIAL_ACCESS, summary.categoryStats[0].category)
        assertEquals(1, summary.categoryStats[0].appsWithAccess)
        assertEquals(1, summary.categoryStats[0].appsDeclaring)

        assertEquals(RiskCategory.SMS, summary.categoryStats[1].category)
        assertEquals(1, summary.categoryStats[1].appsWithAccess)
        assertEquals(1, summary.categoryStats[1].appsDeclaring)

        assertEquals(RiskCategory.CAMERA_AND_MIC, summary.categoryStats[3].category)
        assertEquals(0, summary.categoryStats[3].appsWithAccess) // Denied
        assertEquals(1, summary.categoryStats[3].appsDeclaring) // Declared
    }
}
