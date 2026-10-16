package com.appropriate.riskauditor.core.model

/**
 * Standard four-band risk classification according to PS-01 scoring rubric.
 */
enum class RiskLevel(val minScore: Int, val maxScore: Int, val displayName: String) {
    LOW(0, 24, "Low Risk"),
    MEDIUM(25, 49, "Medium Risk"),
    HIGH(50, 74, "High Risk"),
    CRITICAL(75, 100, "Critical Risk");

    companion object {
        fun fromScore(score: Int): RiskLevel {
            val clamped = score.coerceIn(0, 100)
            return when {
                clamped >= CRITICAL.minScore -> CRITICAL
                clamped >= HIGH.minScore -> HIGH
                clamped >= MEDIUM.minScore -> MEDIUM
                else -> LOW
            }
        }
    }
}
