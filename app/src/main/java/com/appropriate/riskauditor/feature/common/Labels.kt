package com.appropriate.riskauditor.feature.common

import com.appropriate.riskauditor.core.model.GrantState
import com.appropriate.riskauditor.core.model.RiskCategory
import com.appropriate.riskauditor.core.model.RiskLevel
import com.appropriate.riskauditor.core.model.SpecialAccessState
import com.appropriate.riskauditor.core.model.SpecialAccessType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Standard user-facing string representations required by Section 5 of the Implementation Plan.
 * Formats enum values and metadata into clear, non-technical plain language.
 */

fun RiskCategory.displayName(): String = when (this) {
    RiskCategory.SPECIAL_ACCESS -> "Special access"
    RiskCategory.SMS -> "SMS"
    RiskCategory.CONTACTS_AND_CALLS -> "Contacts & call logs"
    RiskCategory.CAMERA_AND_MIC -> "Camera & microphone"
    RiskCategory.LOCATION -> "Location"
    RiskCategory.PHONE_AND_IDENTITY -> "Phone & identity"
    RiskCategory.STORAGE_AND_MEDIA -> "Storage & media"
    RiskCategory.BACKGROUND_AND_PERSISTENCE -> "Background & persistence"
    RiskCategory.NETWORK -> "Network"
}

fun RiskLevel.displayName(): String = when (this) {
    RiskLevel.LOW -> "Low"
    RiskLevel.MEDIUM -> "Medium"
    RiskLevel.HIGH -> "High"
    RiskLevel.CRITICAL -> "Critical"
}

fun GrantState.displayName(): String = when (this) {
    GrantState.GRANTED -> "Allowed"
    GrantState.DENIED -> "Not allowed"
    GrantState.NOT_RUNTIME -> "Not a runtime permission"
    GrantState.UNKNOWN -> "Unknown"
    GrantState.NOT_SUPPORTED -> "Not supported on this Android version"
}

fun SpecialAccessState.displayName(): String = when (this) {
    SpecialAccessState.ENABLED -> "On"
    SpecialAccessState.DISABLED -> "Off"
    SpecialAccessState.NOT_DECLARED -> "Not requested"
    SpecialAccessState.UNKNOWN -> "Couldn't check"
    SpecialAccessState.NOT_SUPPORTED -> "Not supported"
}

fun SpecialAccessType.displayName(): String = when (this) {
    SpecialAccessType.ACCESSIBILITY_SERVICE -> "Accessibility service"
    SpecialAccessType.DEVICE_ADMIN -> "Device administrator"
    SpecialAccessType.NOTIFICATION_LISTENER -> "Notification listener"
    SpecialAccessType.SYSTEM_ALERT_WINDOW -> "Display over other apps"
    SpecialAccessType.REQUEST_INSTALL_PACKAGES -> "Install unknown apps"
    SpecialAccessType.USAGE_STATS -> "Usage access"
}

fun installSourceDisplayName(isSideloaded: Boolean, installerPackage: String?, isSystemApp: Boolean): String {
    if (isSystemApp) return "Pre-installed system app"
    return when {
        installerPackage == "com.android.vending" -> "Play Store"
        installerPackage in listOf(
            "com.sec.android.app.samsungapps",
            "com.amazon.venezia",
            "com.huawei.appmarket",
            "com.xiaomi.mipicks",
            "org.fdroid.fdroid"
        ) -> "Other app store"
        isSideloaded -> "Installed outside a store"
        installerPackage != null -> "Installed via $installerPackage"
        else -> "Unknown source"
    }
}

fun permissionShortName(name: String): String = name.substringAfterLast('.')

fun formatDate(epochMs: Long): String {
    if (epochMs <= 0) return "Unknown"
    val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    return sdf.format(Date(epochMs))
}

fun formatDateShort(epochMs: Long): String {
    if (epochMs <= 0) return "Unknown"
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(epochMs))
}
