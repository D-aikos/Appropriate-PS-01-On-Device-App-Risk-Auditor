package com.appropriate.riskauditor.core.model

/**
 * Represents the observed state of a special platform capability.
 * Special access requires explicit user enablement in Android system settings.
 */
enum class SpecialAccessState {
    /** Capability is active and explicitly enabled/granted in Android settings. */
    ENABLED,

    /** Capability is declared in manifest but currently disabled by user. */
    DISABLED,

    /** Capability or component is not declared by the application. */
    NOT_DECLARED,

    /** Unable to observe or verify the state via available APIs. */
    UNKNOWN,

    /** Special access check is unsupported on this OS version. */
    NOT_SUPPORTED
}
