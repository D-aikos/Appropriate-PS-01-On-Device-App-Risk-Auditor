package com.appropriate.riskauditor.core.model

/**
 * Standard classification of application installer source.
 */
enum class InstallSource {
    PLAY_STORE,
    OTHER_STORE,
    SIDELOADED,
    UNKNOWN;

    fun displayName(): String = when (this) {
        PLAY_STORE -> "Play Store"
        OTHER_STORE -> "Other app store"
        SIDELOADED -> "Installed outside a store"
        UNKNOWN -> "Unknown source"
    }
}
