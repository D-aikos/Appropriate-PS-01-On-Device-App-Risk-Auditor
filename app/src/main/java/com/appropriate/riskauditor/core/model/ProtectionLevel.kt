package com.appropriate.riskauditor.core.model

/**
 * Android permission protection level classification.
 */
enum class ProtectionLevel {
    NORMAL,
    DANGEROUS,
    SIGNATURE,
    SPECIAL,
    UNKNOWN;

    fun displayName(): String = when (this) {
        NORMAL -> "Normal"
        DANGEROUS -> "Dangerous"
        SIGNATURE -> "Signature"
        SPECIAL -> "Special"
        UNKNOWN -> "Unknown"
    }
}
