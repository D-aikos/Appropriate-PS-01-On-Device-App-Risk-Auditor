package com.appropriate.riskauditor.core.rules

import com.appropriate.riskauditor.core.model.RiskCategory

/**
 * Maps standard Android permissions to the nine problem-statement categories.
 */
object CategoryMapper {

    fun mapPermission(permissionName: String): RiskCategory {
        val perm = permissionName.uppercase()

        // 1. Special Access (Permissions related to platform capabilities)
        if (perm.contains("BIND_ACCESSIBILITY_SERVICE") ||
            perm.contains("BIND_DEVICE_ADMIN") ||
            perm.contains("BIND_NOTIFICATION_LISTENER_SERVICE") ||
            perm.contains("SYSTEM_ALERT_WINDOW") ||
            perm.contains("REQUEST_INSTALL_PACKAGES") ||
            perm.contains("PACKAGE_USAGE_STATS")
        ) {
            return RiskCategory.SPECIAL_ACCESS
        }

        // 2. SMS
        if (perm.contains("SMS") || perm.contains("MMS") || perm.contains("WAP_PUSH")) {
            return RiskCategory.SMS
        }

        // 3. Contacts and Call Logs
        if (perm.contains("CONTACTS") ||
            perm.contains("CALL_LOG") ||
            perm.contains("GET_ACCOUNTS") ||
            perm.contains("PROCESS_OUTGOING_CALLS")
        ) {
            return RiskCategory.CONTACTS_AND_CALLS
        }

        // 4. Camera and Microphone
        if (perm.contains("CAMERA") ||
            perm.contains("RECORD_AUDIO") ||
            perm.contains("CAPTURE_AUDIO_OUTPUT") ||
            perm.contains("CAPTURE_VIDEO_OUTPUT")
        ) {
            return RiskCategory.CAMERA_AND_MIC
        }

        // 5. Location
        if (perm.contains("LOCATION")) {
            return RiskCategory.LOCATION
        }

        // 6. Phone and Identity
        if (perm.contains("PHONE") ||
            perm.contains("CALL_PHONE") ||
            perm.contains("READ_PRIVILEGED_PHONE_STATE") ||
            perm.contains("USE_SIP") ||
            perm.contains("ADD_VOICEMAIL")
        ) {
            return RiskCategory.PHONE_AND_IDENTITY
        }

        // 7. Storage and Media
        if (perm.contains("STORAGE") ||
            perm.contains("READ_MEDIA_") ||
            perm.contains("ACCESS_MEDIA_LOCATION") ||
            perm.contains("MANAGE_EXTERNAL_STORAGE")
        ) {
            return RiskCategory.STORAGE_AND_MEDIA
        }

        // 8. Background and Persistence
        if (perm.contains("BOOT_COMPLETED") ||
            perm.contains("FOREGROUND_SERVICE") ||
            perm.contains("REQUEST_IGNORE_BATTERY_OPTIMIZATIONS") ||
            perm.contains("WAKE_LOCK") ||
            perm.contains("SCHEDULE_EXACT_ALARM") ||
            perm.contains("USE_FULL_SCREEN_INTENT")
        ) {
            return RiskCategory.BACKGROUND_AND_PERSISTENCE
        }

        // 9. Network
        if (perm.contains("INTERNET") ||
            perm.contains("NETWORK_STATE") ||
            perm.contains("WIFI_STATE") ||
            perm.contains("CHANGE_WIFI_STATE") ||
            perm.contains("CHANGE_NETWORK_STATE")
        ) {
            return RiskCategory.NETWORK
        }

        // Default heuristic based on keywords
        return when {
            perm.contains("AUDIO") -> RiskCategory.CAMERA_AND_MIC
            perm.contains("FILE") -> RiskCategory.STORAGE_AND_MEDIA
            perm.contains("CALL") -> RiskCategory.PHONE_AND_IDENTITY
            else -> RiskCategory.NETWORK
        }
    }
}
