package com.appropriate.riskauditor.core.model

/**
 * Confidence level of an evidence-backed rule finding.
 * Direct platform checks (e.g. active device admin) have HIGH confidence,
 * while heuristic combinations have MEDIUM or LOW confidence.
 */
enum class FindingConfidence {
    LOW,
    MEDIUM,
    HIGH
}
