package com.appropriate.riskauditor.rules

import com.appropriate.riskauditor.core.model.*
import com.appropriate.riskauditor.core.rules.RiskPolicy
import com.appropriate.riskauditor.core.rules.RiskScoringEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreeReasonEnforcementTest {

    @Test
    fun testCapScoreWhenFewerThanThreeDistinctReasons() {
        // App with high raw score (e.g. 70, which would normally be HIGH)
        // but only 2 distinct findings.
        val findings = listOf(
            RiskFinding(
                ruleId = "RULE_SPECIAL_DEVICE_ADMIN_ACTIVE",
                title = "Device Admin",
                severity = FindingSeverity.CRITICAL,
                points = 30,
                category = RiskCategory.SPECIAL_ACCESS,
                evidence = "Active Admin",
                explanation = "Explanation 1",
                confidence = FindingConfidence.HIGH,
                recommendation = "Revoke admin"
            ),
            RiskFinding(
                ruleId = "RULE_SPECIAL_ACCESSIBILITY_ENABLED",
                title = "Accessibility",
                severity = FindingSeverity.HIGH,
                points = 25,
                category = RiskCategory.SPECIAL_ACCESS,
                evidence = "Active Accessibility",
                explanation = "Explanation 2",
                confidence = FindingConfidence.HIGH,
                recommendation = "Disable accessibility"
            )
        )

        val rawScore = 65
        val (finalScore, finalLevel) = RiskScoringEngine.enforceThreeReasonPolicy(rawScore, findings)

        // Must be capped at 49 (Medium band) because distinct reasons = 2 (< 3)
        assertEquals(RiskPolicy.CAP_SCORE_FEWER_REASONS, finalScore)
        assertEquals(RiskLevel.MEDIUM, finalLevel)
    }

    @Test
    fun testAllowHighScoreWhenThreeOrMoreDistinctReasons() {
        val findings = listOf(
            RiskFinding(
                ruleId = "RULE_SPECIAL_DEVICE_ADMIN_ACTIVE",
                title = "Device Admin",
                severity = FindingSeverity.CRITICAL,
                points = 30,
                category = RiskCategory.SPECIAL_ACCESS,
                evidence = "Active Admin",
                explanation = "Explanation 1",
                confidence = FindingConfidence.HIGH,
                recommendation = "Revoke admin"
            ),
            RiskFinding(
                ruleId = "RULE_SPECIAL_ACCESSIBILITY_ENABLED",
                title = "Accessibility",
                severity = FindingSeverity.HIGH,
                points = 25,
                category = RiskCategory.SPECIAL_ACCESS,
                evidence = "Active Accessibility",
                explanation = "Explanation 2",
                confidence = FindingConfidence.HIGH,
                recommendation = "Disable accessibility"
            ),
            RiskFinding(
                ruleId = "RULE_COMBO_SPYWARE",
                title = "Spyware Combo",
                severity = FindingSeverity.HIGH,
                points = 20,
                category = RiskCategory.SPECIAL_ACCESS,
                evidence = "Persistence + Sensitive + Net",
                explanation = "Explanation 3",
                confidence = FindingConfidence.HIGH,
                recommendation = "Review permissions"
            )
        )

        val rawScore = 75
        val (finalScore, finalLevel) = RiskScoringEngine.enforceThreeReasonPolicy(rawScore, findings)

        // 3 distinct reasons present: should NOT be capped, receives CRITICAL
        assertEquals(75, finalScore)
        assertEquals(RiskLevel.CRITICAL, finalLevel)
    }

    @Test
    fun testDuplicateFindingsDoNotCountAsDistinctReasons() {
        // App has 3 findings, but 2 have the SAME ruleId (only 2 distinct reasons)
        val findings = listOf(
            RiskFinding(
                ruleId = "RULE_PERM_CAT_SMS",
                title = "SMS",
                severity = FindingSeverity.HIGH,
                points = 16,
                category = RiskCategory.SMS,
                evidence = "SEND_SMS",
                explanation = "Explanation",
                confidence = FindingConfidence.HIGH,
                recommendation = "Revoke"
            ),
            RiskFinding(
                ruleId = "RULE_PERM_CAT_SMS", // duplicate rule ID
                title = "SMS",
                severity = FindingSeverity.HIGH,
                points = 8,
                category = RiskCategory.SMS,
                evidence = "RECEIVE_SMS",
                explanation = "Explanation",
                confidence = FindingConfidence.HIGH,
                recommendation = "Revoke"
            ),
            RiskFinding(
                ruleId = "RULE_SPECIAL_OVERLAY_GRANTED",
                title = "Overlay",
                severity = FindingSeverity.MEDIUM,
                points = 18,
                category = RiskCategory.SPECIAL_ACCESS,
                evidence = "canDrawOverlays",
                explanation = "Explanation",
                confidence = FindingConfidence.HIGH,
                recommendation = "Revoke"
            )
        )

        val rawScore = 60
        val (finalScore, finalLevel) = RiskScoringEngine.enforceThreeReasonPolicy(rawScore, findings)

        // Distinct rule count is 2 -> capped at 49 (Medium)
        assertEquals(49, finalScore)
        assertEquals(RiskLevel.MEDIUM, finalLevel)
    }
}
