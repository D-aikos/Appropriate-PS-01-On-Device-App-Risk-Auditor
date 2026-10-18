package com.appropriate.riskauditor.core.rules

import com.appropriate.riskauditor.core.model.*

/**
 * Deterministic on-device risk scoring engine strictly adhering to
 * Implementation Plan v2 Section 7.2 and 7.3.
 */
object RiskScoringEngine {

    fun evaluate(
        inventory: AppInventoryRecord,
        permissions: List<PermissionAudit>,
        specialAccess: List<SpecialAccessAudit>,
        scannedAtEpochMillis: Long = System.currentTimeMillis()
    ): AppRiskReport {
        val sig = SignalView(inventory, permissions, specialAccess)
        return evaluate(sig, scannedAtEpochMillis)
    }

    fun evaluate(
        sig: SignalView,
        scannedAtEpochMillis: Long = System.currentTimeMillis()
    ): AppRiskReport {
        val findings = mutableListOf<RiskFinding>()

        // 1. Single-signal Special Access rules
        Rules.ruleSaAccessibility(sig)?.let { findings.add(it) }
        Rules.ruleSaDeviceAdmin(sig)?.let { findings.add(it) }
        Rules.ruleSaNotifListener(sig)?.let { findings.add(it) }
        Rules.ruleSaOverlay(sig)?.let { findings.add(it) }
        Rules.ruleSaInstallPackages(sig)?.let { findings.add(it) }
        Rules.ruleSaUsageStats(sig)?.let { findings.add(it) }

        // 2. Single-signal Category rules
        Rules.ruleCatSms(sig)?.let { findings.add(it) }
        Rules.ruleCatContactsCalls(sig)?.let { findings.add(it) }
        Rules.ruleCatCameraMic(sig)?.let { findings.add(it) }
        Rules.ruleCatLocation(sig)?.let { findings.add(it) }
        Rules.ruleCatPhoneIdentity(sig)?.let { findings.add(it) }
        Rules.ruleCatStorageMedia(sig)?.let { findings.add(it) }
        Rules.ruleCatBackground(sig)?.let { findings.add(it) }

        // 3. Combination rules
        Rules.ruleComboAccessOverlay(sig)?.let { findings.add(it) }
        Rules.ruleComboSmsNetBootSideload(sig)?.let { findings.add(it) }
        Rules.ruleComboAdminNonMgmt(sig)?.let { findings.add(it) }
        Rules.ruleComboNotifNet(sig)?.let { findings.add(it) }
        Rules.ruleComboDropper(sig)?.let { findings.add(it) }
        Rules.ruleComboSurveillance(sig)?.let { findings.add(it) }
        Rules.ruleComboHarvest(sig)?.let { findings.add(it) }
        Rules.ruleCtxMismatch(sig)?.let { findings.add(it) }

        // 4. Provenance rule
        Rules.ruleProvSideloaded(sig)?.let { findings.add(it) }

        // 5. Score summation and Three-Reason Rule enforcement
        val rawScore = findings.sumOf { it.points }
        val (finalScore, finalLevel) = enforceThreeReasonPolicy(rawScore, findings)

        // 6. Deterministic sort: points descending, then ruleId ascending
        val sortedFindings = findings.sortedWith(
            compareByDescending<RiskFinding> { it.points }.thenBy { it.ruleId }
        )

        return AppRiskReport(
            inventory = sig.inventory,
            permissions = sig.permissions,
            specialAccess = sig.specialAccess,
            findings = sortedFindings,
            riskScore = finalScore,
            riskLevel = finalLevel,
            scannedAtEpochMillis = scannedAtEpochMillis
        )
    }

    /**
     * Enforces the PS-01 Three-Reason Enforcement Policy:
     * If the score would fall into High (50-74) or Critical (75-100) band,
     * but fewer than 3 distinct evidence-backed findings (with points > 0) exist,
     * the score is capped at 49 (Medium band).
     */
    fun enforceThreeReasonPolicy(rawScore: Int, findings: List<RiskFinding>): Pair<Int, RiskLevel> {
        val clampedScore = rawScore.coerceIn(0, 100)
        val distinctReasonCount = findings.filter { it.points > 0 }.map { it.ruleId }.distinct().size

        val finalScore = if (clampedScore >= RiskLevel.HIGH.minScore &&
            distinctReasonCount < RiskPolicy.MIN_DISTINCT_REASONS
        ) {
            RiskPolicy.SCORE_MEDIUM_CAP
        } else {
            clampedScore
        }

        val finalRiskLevel = RiskLevel.fromScore(finalScore)
        return Pair(finalScore, finalRiskLevel)
    }

    fun wasCappedForEvidence(rawScore: Int, findings: List<RiskFinding>): Boolean {
        val clamped = rawScore.coerceIn(0, 100)
        val distinct = findings.filter { it.points > 0 }.map { it.ruleId }.distinct().size
        return clamped >= RiskLevel.HIGH.minScore && distinct < RiskPolicy.MIN_DISTINCT_REASONS
    }
}

// Alias matching Section 7.3
typealias RuleBasedRiskEngine = RiskScoringEngine
