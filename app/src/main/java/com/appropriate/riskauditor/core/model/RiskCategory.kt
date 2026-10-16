package com.appropriate.riskauditor.core.model

/**
 * The nine mandatory audit categories specified in Problem Statement PS-01.
 * Special access carries the highest security evaluation weight.
 */
enum class RiskCategory(val displayName: String, val baseWeightMultiplier: Double) {
    /** Accessibility, Device Admin, Notification Listener, System Overlay, Package Installer, Usage Stats */
    SPECIAL_ACCESS("Special Access", 2.0),

    /** SMS sending, reading, receiving, MMS, WAP */
    SMS("SMS", 1.5),

    /** Contacts, Accounts, Call logs */
    CONTACTS_AND_CALLS("Contacts & Call Logs", 1.4),

    /** Camera recording, Microphone audio */
    CAMERA_AND_MIC("Camera & Microphone", 1.3),

    /** Fine, Coarse, Background location tracking */
    LOCATION("Location", 1.3),

    /** Phone state, outgoing calls, phone numbers, SIP */
    PHONE_AND_IDENTITY("Phone & Identity", 1.2),

    /** Shared external storage, all files access, media */
    STORAGE_AND_MEDIA("Storage & Media", 1.1),

    /** Boot completed, foreground services, battery ignore, wake lock */
    BACKGROUND_AND_PERSISTENCE("Background & Persistence", 1.2),

    /** Internet, Network state, Wi-Fi configuration */
    NETWORK("Network", 1.0);
}
