package com.appropriate.riskauditor.rules

import com.appropriate.riskauditor.core.model.SpecialAccessState
import com.appropriate.riskauditor.core.rules.Explanations
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test verifying constraints in Section 7.4:
 * 1. No explanation contains "malware", "virus", or "definitely".
 * 2. Every explanation contains the app label.
 * 3. Every explanation <= 240 chars.
 */
class ExplanationsTest {

    private val testApp = "TestAuditorApp"

    @Test
    fun testAllExplanationsComplyWithToneAndLengthRules() {
        val texts = mutableListOf<Explanations.Text>()

        // Special Access
        for (state in listOf(SpecialAccessState.ENABLED, SpecialAccessState.UNKNOWN, SpecialAccessState.DISABLED)) {
            texts.add(Explanations.saAccessibility(testApp, state))
            texts.add(Explanations.saDeviceAdmin(testApp, state))
            texts.add(Explanations.saNotifListener(testApp, state))
            texts.add(Explanations.saOverlay(testApp, state))
            texts.add(Explanations.saInstallPackages(testApp, state))
            texts.add(Explanations.saUsageStats(testApp, state))
        }

        // Categories
        for (granted in listOf(true, false)) {
            texts.add(Explanations.catSms(testApp, granted, "READ_SMS (allowed)"))
            texts.add(Explanations.catContactsCalls(testApp, granted, "READ_CONTACTS (allowed)"))
            texts.add(Explanations.catCameraMic(testApp, granted, "CAMERA (allowed)"))
            texts.add(Explanations.catLocation(testApp, granted, "ACCESS_FINE_LOCATION (allowed)"))
            texts.add(Explanations.catPhoneIdentity(testApp, granted, "READ_PHONE_STATE (allowed)"))
            texts.add(Explanations.catStorageMedia(testApp, granted, "READ_MEDIA_IMAGES (allowed)"))
            texts.add(Explanations.catBackground(testApp, granted, "RECEIVE_BOOT_COMPLETED (allowed)"))
        }

        // Combinations
        texts.add(Explanations.comboAccessOverlay(testApp))
        texts.add(Explanations.comboSmsNetBootSideload(testApp, true))
        texts.add(Explanations.comboSmsNetBootSideload(testApp, false))
        texts.add(Explanations.comboAdminNonMgmt(testApp))
        texts.add(Explanations.comboNotifNet(testApp))
        texts.add(Explanations.comboDropper(testApp))
        texts.add(Explanations.comboSurveillance(testApp))
        texts.add(Explanations.comboHarvest(testApp))
        texts.add(Explanations.ctxMismatch(testApp))
        texts.add(Explanations.provSideloaded(testApp))

        for (item in texts) {
            val expl = item.explanation
            assertTrue("Explanation length must be <= 240 chars: '$expl' (${expl.length})", expl.length <= 240)
            assertTrue("Explanation must mention app label: '$expl'", expl.contains(testApp))

            val lower = expl.lowercase()
            assertFalse("Must not contain 'malware': '$expl'", lower.contains("malware"))
            assertFalse("Must not contain 'virus': '$expl'", lower.contains("virus"))
            assertFalse("Must not contain 'definitely': '$expl'", lower.contains("definitely"))
        }
    }
}
