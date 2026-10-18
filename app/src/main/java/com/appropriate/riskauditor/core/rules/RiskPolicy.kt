package com.appropriate.riskauditor.core.rules

/**
 * Scoring policy constants, weights, and category caps strictly matching
 * Implementation Plan v2 Section 7.2.
 */
object RiskPolicy {

    // Multipliers based on signal verification state (Section 7.2)
    const val MULTIPLIER_GRANTED = 1.00
    const val MULTIPLIER_ENABLED = 1.00
    const val MULTIPLIER_NOT_RUNTIME = 1.00
    const val MULTIPLIER_UNKNOWN = 0.50
    const val MULTIPLIER_DECLARED_ONLY = 0.30
    const val MULTIPLIER_NOT_DECLARED = 0.00
    const val MULTIPLIER_NOT_SUPPORTED = 0.00

    // Special Access Points (Base points at multiplier 1.0)
    const val PTS_SA_ACCESSIBILITY = 30
    const val PTS_SA_DEVICE_ADMIN = 25 // 0 if device or profile owner
    const val PTS_SA_NOTIF_LISTENER = 22
    const val PTS_SA_OVERLAY = 18
    const val PTS_SA_INSTALL_PACKAGES = 16
    const val PTS_SA_USAGE_STATS = 10

    // Legacy aliases for backward compatibility with existing tests
    const val PTS_SPECIAL_ACCESSIBILITY_ENABLED = PTS_SA_ACCESSIBILITY
    const val PTS_SPECIAL_ACCESSIBILITY_DECLARED = 9
    const val PTS_SPECIAL_DEVICE_ADMIN_ACTIVE = PTS_SA_DEVICE_ADMIN
    const val PTS_SPECIAL_DEVICE_ADMIN_DECLARED = 8
    const val PTS_SPECIAL_OVERLAY_GRANTED = PTS_SA_OVERLAY
    const val PTS_SPECIAL_OVERLAY_DECLARED = 5
    const val PTS_SPECIAL_NOTIFICATION_LISTENER_ENABLED = PTS_SA_NOTIF_LISTENER
    const val PTS_SPECIAL_NOTIFICATION_LISTENER_DECLARED = 7
    const val PTS_SPECIAL_INSTALL_PACKAGES_GRANTED = PTS_SA_INSTALL_PACKAGES
    const val PTS_SPECIAL_INSTALL_PACKAGES_DECLARED = 5
    const val PTS_SPECIAL_USAGE_STATS_GRANTED = PTS_SA_USAGE_STATS
    const val PTS_SPECIAL_USAGE_STATS_DECLARED = 3

    // Category Caps (Section 7.2)
    const val CAP_CAT_SMS = 30
    const val CAP_CAT_CONTACTS_CALLS = 18
    const val CAP_CAT_CAMERA_MIC = 16
    const val CAP_CAT_LOCATION = 14
    const val CAP_CAT_PHONE_IDENTITY = 10
    const val CAP_CAT_STORAGE_MEDIA = 14
    const val CAP_CAT_BACKGROUND = 10

    // Individual Permission Base Weights (Section 7.2)
    // SMS
    const val WEIGHT_READ_SMS = 14
    const val WEIGHT_RECEIVE_SMS = 10
    const val WEIGHT_SEND_SMS = 12
    const val WEIGHT_RECEIVE_MMS = 3
    const val WEIGHT_RECEIVE_WAP_PUSH = 3

    // Contacts & Calls
    const val WEIGHT_READ_CONTACTS = 8
    const val WEIGHT_WRITE_CONTACTS = 4
    const val WEIGHT_READ_CALL_LOG = 10
    const val WEIGHT_WRITE_CALL_LOG = 6
    const val WEIGHT_PROCESS_OUTGOING_CALLS = 6

    // Camera & Mic
    const val WEIGHT_CAMERA = 7
    const val WEIGHT_RECORD_AUDIO = 9

    // Location
    const val WEIGHT_ACCESS_COARSE_LOCATION = 3
    const val WEIGHT_ACCESS_FINE_LOCATION = 6
    const val WEIGHT_ACCESS_BACKGROUND_LOCATION = 8

    // Phone & Identity
    const val WEIGHT_READ_PHONE_STATE = 4
    const val WEIGHT_READ_PHONE_NUMBERS = 4
    const val WEIGHT_CALL_PHONE = 5
    const val WEIGHT_ANSWER_PHONE_CALLS = 3
    const val WEIGHT_GET_ACCOUNTS = 3

    // Storage & Media
    const val WEIGHT_MANAGE_EXTERNAL_STORAGE = 12
    const val WEIGHT_READ_MEDIA_IMAGES = 5
    const val WEIGHT_READ_MEDIA_VIDEO = 5
    const val WEIGHT_EXTERNAL_STORAGE_SHARED = 5 // READ/WRITE_EXTERNAL_STORAGE counted once
    const val WEIGHT_READ_MEDIA_AUDIO = 2
    const val WEIGHT_READ_MEDIA_VISUAL_USER_SELECTED = 2

    // Background & Persistence
    const val WEIGHT_RECEIVE_BOOT_COMPLETED = 3
    const val WEIGHT_FOREGROUND_SERVICE = 2 // and any FOREGROUND_SERVICE_*
    const val WEIGHT_IGNORE_BATTERY_OPT = 4
    const val WEIGHT_WAKE_LOCK = 1
    const val WEIGHT_SCHEDULE_EXACT_ALARM = 1
    const val WEIGHT_USE_FULL_SCREEN_INTENT = 3

    // Combination Rules Base Points
    const val BASE_COMBO_ACCESS_OVERLAY = 25
    const val BASE_COMBO_SMS_NET_BOOT_SIDELOAD = 25
    const val BASE_COMBO_SMS_NET_BOOT_UNKNOWN = 15
    const val BASE_COMBO_ADMIN_NON_MGMT = 18
    const val BASE_COMBO_NOTIF_NET = 12
    const val BASE_COMBO_DROPPER = 15
    const val BASE_COMBO_SURVEILLANCE = 12
    const val BASE_COMBO_HARVEST = 10
    const val BASE_CTX_MISMATCH = 15

    // Provenance Sideloaded
    const val PTS_PROV_SIDELOADED = 8

    // Known Enterprise / MDM Device Admin Packages (COMBO_ADMIN_NON_MGMT will not fire on these)
    val SECURITY_OR_MDM_PACKAGES = setOf(
        "com.google.android.gms",
        "com.google.android.apps.work.clouddpc",
        "com.microsoft.windowsintune.companyportal",
        "com.airwatch.androidagent"
    )

    // Context Mismatch Keyword triggers (whole word match)
    val CTX_MISMATCH_KEYWORDS = setOf(
        "torch", "flashlight", "calculator", "wallpaper", "ringtone",
        "compass", "qr", "barcode", "ruler", "level"
    )

    // Three-Reason Rule Thresholds
    const val MIN_DISTINCT_REASONS = 3
    const val MIN_DISTINCT_REASONS_FOR_HIGH_OR_CRITICAL = 3
    const val SCORE_MEDIUM_CAP = 49
    const val CAP_SCORE_FEWER_REASONS = 49

    // Legacy combination constants
    const val PTS_COMBO_SPYWARE = 20
    const val PTS_COMBO_TOLL_FRAUD = 20
    const val PTS_COMBO_AUDIO_SURVEILLANCE = 18
    const val PTS_COMBO_OVERLAY_STEALER = 25
    const val PTS_COMBO_MALICIOUS_DROPPER = 20
    const val PTS_COMBO_RANSOMWARE = 25
    const val PTS_HEURISTIC_SIDELOADED_HIGH_RISK = 15
    const val PTS_HEURISTIC_OUTDATED_TARGET_SDK = 10
}
