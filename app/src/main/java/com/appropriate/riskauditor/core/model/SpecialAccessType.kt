package com.appropriate.riskauditor.core.model

/**
 * Types of special access capabilities audited on Android.
 */
enum class SpecialAccessType(val title: String) {
    ACCESSIBILITY_SERVICE("Accessibility Service"),
    DEVICE_ADMIN("Active Device Administrator"),
    NOTIFICATION_LISTENER("Notification Listener"),
    SYSTEM_ALERT_WINDOW("Draw Over Other Apps (Overlay)"),
    REQUEST_INSTALL_PACKAGES("Install Unknown Apps"),
    USAGE_STATS("Usage Access")
}
