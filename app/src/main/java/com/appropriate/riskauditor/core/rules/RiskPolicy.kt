package com.appropriate.riskauditor.core.rules

/**
 * Centralized policy defining scoring weights, category caps, and thresholds.
 * Adheres to the mandate that Special Access has the highest weight.
 */
object RiskPolicy {

    // Special Access Points (Highest Weight)
    const val PTS_SPECIAL_ACCESSIBILITY_ENABLED = 25
    const val PTS_SPECIAL_ACCESSIBILITY_DECLARED = 5
    const val PTS_SPECIAL_DEVICE_ADMIN_ACTIVE = 30
    const val PTS_SPECIAL_DEVICE_ADMIN_DECLARED = 5
    const val PTS_SPECIAL_OVERLAY_GRANTED = 18
    const val PTS_SPECIAL_OVERLAY_DECLARED = 4
    const val PTS_SPECIAL_NOTIFICATION_LISTENER_ENABLED = 20
    const val PTS_SPECIAL_NOTIFICATION_LISTENER_DECLARED = 4
    const val PTS_SPECIAL_INSTALL_PACKAGES_GRANTED = 18
    const val PTS_SPECIAL_INSTALL_PACKAGES_DECLARED = 4
    const val PTS_SPECIAL_USAGE_STATS_GRANTED = 12

    // Category Permission Grant Weights (Per granted permission)
    const val PTS_PERM_SMS_GRANTED = 8
    const val MAX_CAT_SMS = 16

    const val PTS_PERM_CONTACTS_CALLS_GRANTED = 6
    const val MAX_CAT_CONTACTS_CALLS = 14

    const val PTS_PERM_CAMERA_MIC_GRANTED = 6
    const val MAX_CAT_CAMERA_MIC = 12

    const val PTS_PERM_LOCATION_GRANTED = 6
    const val MAX_CAT_LOCATION = 12

    const val PTS_PERM_PHONE_IDENTITY_GRANTED = 5
    const val MAX_CAT_PHONE_IDENTITY = 10

    const val PTS_PERM_STORAGE_MEDIA_GRANTED = 5
    const val MAX_CAT_STORAGE_MEDIA = 10

    const val PTS_PERM_BG_PERSISTENCE_GRANTED = 4
    const val MAX_CAT_BG_PERSISTENCE = 8

    const val PTS_PERM_NETWORK_GRANTED = 2
    const val MAX_CAT_NETWORK = 4

    // Declared-only permission weight (nominal)
    const val PTS_PERM_DECLARED_ONLY = 1
    const val MAX_CAT_DECLARED_ONLY = 3

    // Combination Rules Points
    const val PTS_COMBO_SPYWARE = 20
    const val PTS_COMBO_TOLL_FRAUD = 18
    const val PTS_COMBO_AUDIO_SURVEILLANCE = 16
    const val PTS_COMBO_OVERLAY_STEALER = 22
    const val PTS_COMBO_MALICIOUS_DROPPER = 20
    const val PTS_COMBO_RANSOMWARE = 25

    // Context / Heuristic Points
    const val PTS_HEURISTIC_SIDELOADED_HIGH_RISK = 15
    const val PTS_HEURISTIC_OUTDATED_TARGET_SDK = 10

    // High / Critical Enforcement
    const val MIN_DISTINCT_REASONS_FOR_HIGH_OR_CRITICAL = 3
    const val CAP_SCORE_FEWER_REASONS = 49 // Upper boundary of Medium band
}
