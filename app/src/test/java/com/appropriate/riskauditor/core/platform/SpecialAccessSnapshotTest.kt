package com.appropriate.riskauditor.core.platform

import android.content.pm.PackageInfo
import com.appropriate.riskauditor.core.model.SpecialAccessState
import com.appropriate.riskauditor.core.model.SpecialAccessType
import org.junit.Assert.assertEquals
import org.junit.Test

class SpecialAccessSnapshotTest {

    @Test
    fun testUndeclaredAppReturnsNotDeclaredForAllSix() {
        val snapshot = SpecialAccessSnapshot(
            enabledA11yPackages = emptySet(),
            declaredA11yPackages = emptySet(),
            a11yQueryFailed = false,
            activeAdminPackages = emptySet(),
            adminQueryFailed = false,
            enabledListenerPackages = emptySet(),
            listenerQueryFailed = false,
            appOpsManager = null,
            devicePolicyManager = null
        )

        val info = PackageInfo().apply {
            packageName = "com.example.benign"
            requestedPermissions = emptyArray()
            services = emptyArray()
            receivers = emptyArray()
        }

        val audits = snapshot.auditsFor(info)

        // Rule: always exactly 6 entries
        assertEquals(6, audits.size)

        // All 6 must be NOT_DECLARED since this app requests none
        for (audit in audits) {
            assertEquals(SpecialAccessState.NOT_DECLARED, audit.state)
        }
    }

    @Test
    fun testAccessibilityEnabled() {
        val snapshot = SpecialAccessSnapshot(
            enabledA11yPackages = setOf("com.example.assist"),
            declaredA11yPackages = setOf("com.example.assist"),
            a11yQueryFailed = false,
            activeAdminPackages = emptySet(),
            adminQueryFailed = false,
            enabledListenerPackages = emptySet(),
            listenerQueryFailed = false,
            appOpsManager = null,
            devicePolicyManager = null
        )

        val info = PackageInfo().apply {
            packageName = "com.example.assist"
        }

        val audits = snapshot.auditsFor(info)
        val a11yAudit = audits.first { it.type == SpecialAccessType.ACCESSIBILITY_SERVICE }
        assertEquals(SpecialAccessState.ENABLED, a11yAudit.state)
    }

    @Test
    fun testAccessibilityDeclaredButDisabled() {
        val snapshot = SpecialAccessSnapshot(
            enabledA11yPackages = emptySet(),
            declaredA11yPackages = setOf("com.example.assist"),
            a11yQueryFailed = false,
            activeAdminPackages = emptySet(),
            adminQueryFailed = false,
            enabledListenerPackages = emptySet(),
            listenerQueryFailed = false,
            appOpsManager = null,
            devicePolicyManager = null
        )

        val info = PackageInfo().apply {
            packageName = "com.example.assist"
        }

        val audits = snapshot.auditsFor(info)
        val a11yAudit = audits.first { it.type == SpecialAccessType.ACCESSIBILITY_SERVICE }
        assertEquals(SpecialAccessState.DISABLED, a11yAudit.state)
    }
}
