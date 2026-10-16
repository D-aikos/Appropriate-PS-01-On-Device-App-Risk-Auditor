package com.appropriate.riskauditor.core.model

/**
 * Represents the observed grant state of a permission.
 * Distinguishes runtime grants from manifest-only declarations,
 * non-runtime permissions, and unobservable platform states.
 */
enum class GrantState {
    /** Permission is requested and currently granted by the user/system. */
    GRANTED,

    /** Runtime permission is requested but denied by user or revoked by OS. */
    DENIED,

    /** Normal or signature permission that does not have runtime grant semantics. */
    NOT_RUNTIME,

    /** State cannot be queried or determined due to platform limits. */
    UNKNOWN,

    /** Feature or query is not supported on this Android API level. */
    NOT_SUPPORTED
}
