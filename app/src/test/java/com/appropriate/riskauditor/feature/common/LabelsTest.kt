package com.appropriate.riskauditor.feature.common

import com.appropriate.riskauditor.core.model.GrantState
import com.appropriate.riskauditor.core.model.RiskCategory
import com.appropriate.riskauditor.core.model.RiskLevel
import com.appropriate.riskauditor.core.model.SpecialAccessState
import com.appropriate.riskauditor.core.model.SpecialAccessType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelsTest {

    @Test
    fun testRiskLevelDisplayNames() {
        assertEquals("Low", RiskLevel.LOW.displayName())
        assertEquals("Medium", RiskLevel.MEDIUM.displayName())
        assertEquals("High", RiskLevel.HIGH.displayName())
        assertEquals("Critical", RiskLevel.CRITICAL.displayName())
    }

    @Test
    fun testGrantStateDisplayNames() {
        assertEquals("Allowed", GrantState.GRANTED.displayName())
        assertEquals("Not allowed", GrantState.DENIED.displayName())
        assertEquals("Not a runtime permission", GrantState.NOT_RUNTIME.displayName())
        assertEquals("Unknown", GrantState.UNKNOWN.displayName())
        assertEquals("Not supported on this Android version", GrantState.NOT_SUPPORTED.displayName())
    }

    @Test
    fun testSpecialAccessStateDisplayNames() {
        assertEquals("On", SpecialAccessState.ENABLED.displayName())
        assertEquals("Off", SpecialAccessState.DISABLED.displayName())
        assertEquals("Not requested", SpecialAccessState.NOT_DECLARED.displayName())
        assertEquals("Couldn't check", SpecialAccessState.UNKNOWN.displayName())
        assertEquals("Not supported", SpecialAccessState.NOT_SUPPORTED.displayName())
    }

    @Test
    fun testSpecialAccessTypeDisplayNames() {
        assertEquals("Accessibility service", SpecialAccessType.ACCESSIBILITY_SERVICE.displayName())
        assertEquals("Device administrator", SpecialAccessType.DEVICE_ADMIN.displayName())
        assertEquals("Notification listener", SpecialAccessType.NOTIFICATION_LISTENER.displayName())
        assertEquals("Display over other apps", SpecialAccessType.SYSTEM_ALERT_WINDOW.displayName())
        assertEquals("Install unknown apps", SpecialAccessType.REQUEST_INSTALL_PACKAGES.displayName())
        assertEquals("Usage access", SpecialAccessType.USAGE_STATS.displayName())
    }

    @Test
    fun testRiskCategoryDisplayNames() {
        assertEquals("Special access", RiskCategory.SPECIAL_ACCESS.displayName())
        assertEquals("SMS", RiskCategory.SMS.displayName())
        assertEquals("Contacts & call logs", RiskCategory.CONTACTS_AND_CALLS.displayName())
        assertEquals("Camera & microphone", RiskCategory.CAMERA_AND_MIC.displayName())
        assertEquals("Location", RiskCategory.LOCATION.displayName())
        assertEquals("Phone & identity", RiskCategory.PHONE_AND_IDENTITY.displayName())
        assertEquals("Storage & media", RiskCategory.STORAGE_AND_MEDIA.displayName())
        assertEquals("Background & persistence", RiskCategory.BACKGROUND_AND_PERSISTENCE.displayName())
        assertEquals("Network", RiskCategory.NETWORK.displayName())
    }

    @Test
    fun testInstallSourceDisplayNames() {
        assertEquals("Play Store", installSourceDisplayName(isSideloaded = false, installerPackage = "com.android.vending", isSystemApp = false))
        assertEquals("Other app store", installSourceDisplayName(isSideloaded = false, installerPackage = "org.fdroid.fdroid", isSystemApp = false))
        assertEquals("Installed outside a store", installSourceDisplayName(isSideloaded = true, installerPackage = null, isSystemApp = false))
        assertEquals("Pre-installed system app", installSourceDisplayName(isSideloaded = false, installerPackage = null, isSystemApp = true))
    }

    @Test
    fun testPermissionShortName() {
        assertEquals("READ_SMS", permissionShortName("android.permission.READ_SMS"))
        assertEquals("SYSTEM_ALERT_WINDOW", permissionShortName("android.permission.SYSTEM_ALERT_WINDOW"))
        assertEquals("ACCESS_FINE_LOCATION", permissionShortName("android.permission.ACCESS_FINE_LOCATION"))
    }

    @Test
    fun testFormatDate() {
        val formatted = formatDate(1700000000000L)
        assertTrue(formatted.contains("2023"))
    }
}
