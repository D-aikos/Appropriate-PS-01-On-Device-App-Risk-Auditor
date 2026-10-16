package com.appropriate.riskauditor.core.rules

import com.appropriate.riskauditor.core.model.*

/**
 * Evaluates high-risk permission and capability combinations that indicate potential
 * attack vectors (e.g. spyware, toll fraud, dropper, screen hijack, ransomware).
 */
object CombinationRules {

    fun evaluate(
        inventory: AppInventoryRecord,
        permissions: List<PermissionAudit>,
        specialAccess: List<SpecialAccessAudit>
    ): List<RiskFinding> {
        val findings = mutableListOf<RiskFinding>()

        val grantedPerms = permissions.filter { it.grantState == GrantState.GRANTED }.map { it.name.uppercase() }.toSet()
        val declaredPerms = permissions.map { it.name.uppercase() }.toSet()
        val enabledAccess = specialAccess.filter { it.state == SpecialAccessState.ENABLED }.map { it.type }.toSet()
        val declaredAccess = specialAccess.filter { it.isDeclared }.map { it.type }.toSet()

        val hasInternet = "ANDROID.PERMISSION.INTERNET" in declaredPerms || "ANDROID.PERMISSION.INTERNET" in grantedPerms
        val hasBoot = "ANDROID.PERMISSION.RECEIVE_BOOT_COMPLETED" in grantedPerms
        val hasFgService = "ANDROID.PERMISSION.FOREGROUND_SERVICE" in grantedPerms || declaredPerms.any { it.contains("FOREGROUND_SERVICE") }
        val hasPersistence = hasBoot || hasFgService

        val hasSmsAccess = grantedPerms.any { it.contains("SMS") || it.contains("MMS") }
        val hasContactsOrCalls = grantedPerms.any { it.contains("CONTACTS") || it.contains("CALL_LOG") }
        val hasLocation = grantedPerms.any { it.contains("LOCATION") }
        val hasMic = "ANDROID.PERMISSION.RECORD_AUDIO" in grantedPerms
        val hasOverlay = SpecialAccessType.SYSTEM_ALERT_WINDOW in enabledAccess || "ANDROID.PERMISSION.SYSTEM_ALERT_WINDOW" in grantedPerms
        val hasAccessibility = SpecialAccessType.ACCESSIBILITY_SERVICE in enabledAccess
        val hasInstallPackages = SpecialAccessType.REQUEST_INSTALL_PACKAGES in enabledAccess || "ANDROID.PERMISSION.REQUEST_INSTALL_PACKAGES" in grantedPerms
        val hasDeviceAdmin = SpecialAccessType.DEVICE_ADMIN in enabledAccess
        val hasNotificationListener = SpecialAccessType.NOTIFICATION_LISTENER in enabledAccess
        val hasStorage = grantedPerms.any { it.contains("STORAGE") || it.contains("MANAGE_EXTERNAL_STORAGE") }

        // 1. Spyware Pattern: Persistence + Sensitive Data + Internet
        if (hasPersistence && (hasSmsAccess || hasContactsOrCalls || hasLocation) && hasInternet) {
            val harvested = buildList {
                if (hasSmsAccess) add("SMS")
                if (hasContactsOrCalls) add("Contacts/Calls")
                if (hasLocation) add("Location")
            }.joinToString(", ")

            findings.add(
                RiskFinding(
                    ruleId = "RULE_COMBO_SPYWARE",
                    title = "Potential Spyware Data-Exfiltration Pattern",
                    severity = FindingSeverity.HIGH,
                    points = RiskPolicy.PTS_COMBO_SPYWARE,
                    category = RiskCategory.SPECIAL_ACCESS,
                    evidence = "Background Persistence (Boot/Foreground) + Sensitive Data ($harvested) + Internet Access",
                    explanation = "The app can start automatically at device boot or run persistent background services while accessing personal private records and transmitting data across the internet.",
                    confidence = FindingConfidence.HIGH,
                    recommendation = "Review whether this app legitimately requires continuous background access to sensitive personal data and revoke unnecessary permissions in system settings."
                )
            )
        }

        // 2. Toll Fraud / SMS Exfiltration Pattern: Send SMS + Internet + Background
        val canSendSms = "ANDROID.PERMISSION.SEND_SMS" in grantedPerms
        if (canSendSms && hasInternet) {
            findings.add(
                RiskFinding(
                    ruleId = "RULE_COMBO_TOLL_FRAUD",
                    title = "Potential Toll Fraud / SMS Hijacking Pattern",
                    severity = FindingSeverity.HIGH,
                    points = RiskPolicy.PTS_COMBO_TOLL_FRAUD,
                    category = RiskCategory.SMS,
                    evidence = "Granted SEND_SMS permission + Network communication capability",
                    explanation = "The app has active permission to transmit SMS messages alongside network access, enabling silent premium-rate messaging or authentication interception.",
                    confidence = FindingConfidence.HIGH,
                    recommendation = "Verify if this application is your default SMS client. If not, immediately revoke SMS permissions in Android Settings."
                )
            )
        }

        // 3. Ambient Audio Surveillance Pattern: Microphone + Background + Network
        if (hasMic && hasPersistence && hasInternet) {
            findings.add(
                RiskFinding(
                    ruleId = "RULE_COMBO_AUDIO_SURVEILLANCE",
                    title = "Continuous Audio Surveillance Risk",
                    severity = FindingSeverity.HIGH,
                    points = RiskPolicy.PTS_COMBO_AUDIO_SURVEILLANCE,
                    category = RiskCategory.CAMERA_AND_MIC,
                    evidence = "Granted RECORD_AUDIO + Background Persistence + Network communication",
                    explanation = "The application holds active microphone recording privileges while retaining persistent background execution and outbound internet connectivity.",
                    confidence = FindingConfidence.HIGH,
                    recommendation = "Restrict microphone permission to 'While using the app' or revoke it entirely if background recording is not expected."
                )
            )
        }

        // 4. Screen Hijack / Credential Stealer Pattern: Overlay + (Accessibility OR Notification Listener OR SMS)
        if (hasOverlay && (hasAccessibility || hasNotificationListener || hasSmsAccess || hasContactsOrCalls)) {
            val intercepted = buildList {
                if (hasAccessibility) add("Accessibility")
                if (hasNotificationListener) add("Notification Listener")
                if (hasSmsAccess) add("SMS/2FA")
                if (hasContactsOrCalls) add("Contacts")
            }.joinToString(", ")

            findings.add(
                RiskFinding(
                    ruleId = "RULE_COMBO_OVERLAY_STEALER",
                    title = "Phishing / Overlay Tapjacking Threat",
                    severity = FindingSeverity.CRITICAL,
                    points = RiskPolicy.PTS_COMBO_OVERLAY_STEALER,
                    category = RiskCategory.SPECIAL_ACCESS,
                    evidence = "Active Display Overlay + High Privilege Interception ($intercepted)",
                    explanation = "The app can draw arbitrary floating windows over other apps while intercepting user inputs, 2FA notification codes, or private records.",
                    confidence = FindingConfidence.HIGH,
                    recommendation = "Disable 'Display over other apps' and Accessibility access for this application unless strictly required by a trusted tool."
                )
            )
        }

        // 5. Malicious Dropper Pattern: Install Packages + Network + (Storage OR Persistence)
        if (hasInstallPackages && hasInternet && (hasStorage || hasPersistence)) {
            findings.add(
                RiskFinding(
                    ruleId = "RULE_COMBO_MALICIOUS_DROPPER",
                    title = "Potential Application Dropper Vector",
                    severity = FindingSeverity.HIGH,
                    points = RiskPolicy.PTS_COMBO_MALICIOUS_DROPPER,
                    category = RiskCategory.SPECIAL_ACCESS,
                    evidence = "Package Installation Capability + Internet Download + Storage/Persistence",
                    explanation = "The app has permission to request external APK installations, retrieve payload files from remote servers, and store them locally.",
                    confidence = FindingConfidence.HIGH,
                    recommendation = "Revoke 'Install unknown apps' permission for this application in Special App Access settings."
                )
            )
        }

        // 6. Ransomware Pattern: Manage External Storage + Device Admin/Overlay + Persistence
        val hasAllFilesAccess = "ANDROID.PERMISSION.MANAGE_EXTERNAL_STORAGE" in grantedPerms
        if (hasAllFilesAccess && (hasDeviceAdmin || hasOverlay) && hasPersistence) {
            findings.add(
                RiskFinding(
                    ruleId = "RULE_COMBO_RANSOMWARE_PATTERN",
                    title = "Ransomware-Like Locking & Mass Storage Access",
                    severity = FindingSeverity.CRITICAL,
                    points = RiskPolicy.PTS_COMBO_RANSOMWARE,
                    category = RiskCategory.SPECIAL_ACCESS,
                    evidence = "All-Files Management Access + Device Admin/Overlay Lock capability + Background Auto-start",
                    explanation = "The application possesses broad un-sandboxed filesystem write permissions paired with lock-screen or administrative controls, presenting a severe integrity risk.",
                    confidence = FindingConfidence.HIGH,
                    recommendation = "Disable Device Administrator rights immediately and consider uninstalling this application."
                )
            )
        }

        // 7. Sideloaded High Risk Context: Unknown Installer + Multiple Sensitive Permissions
        val sensitiveCount = listOf(hasSmsAccess, hasContactsOrCalls, hasLocation, hasMic, hasOverlay, hasAccessibility, hasInstallPackages).count { it }
        if (inventory.isSideloaded && sensitiveCount >= 2) {
            findings.add(
                RiskFinding(
                    ruleId = "RULE_HEURISTIC_SIDELOADED_HIGH_RISK",
                    title = "Sideloaded Origin with Elevated Capabilities",
                    severity = FindingSeverity.MEDIUM,
                    points = RiskPolicy.PTS_HEURISTIC_SIDELOADED_HIGH_RISK,
                    category = RiskCategory.SPECIAL_ACCESS,
                    evidence = "Sideloaded APK (Installer: ${inventory.installerPackage ?: "Unknown"}) holding $sensitiveCount sensitive capabilities",
                    explanation = "This app was installed from outside the official Google Play Store and possesses multiple high-privilege access permissions.",
                    confidence = FindingConfidence.MEDIUM,
                    recommendation = "Verify that the APK was obtained from a verified and reputable source."
                )
            )
        }

        // 8. Outdated Target SDK Heuristic: targetSdk < 29
        val target = inventory.targetSdk
        if (target != null && target > 0 && target < 29) {
            findings.add(
                RiskFinding(
                    ruleId = "RULE_HEURISTIC_OUTDATED_TARGET_SDK",
                    title = "Legacy Target SDK (Permission Sandbox Bypass)",
                    severity = FindingSeverity.MEDIUM,
                    points = RiskPolicy.PTS_HEURISTIC_OUTDATED_TARGET_SDK,
                    category = RiskCategory.SPECIAL_ACCESS,
                    evidence = "Target SDK is $target (Android 9 Pie or older)",
                    explanation = "Apps targeting legacy Android versions can bypass modern scoped storage and granular runtime permission prompt dialogs.",
                    confidence = FindingConfidence.HIGH,
                    recommendation = "Check for updated versions of this application in the app store that support modern Android security standards."
                )
            )
        }

        return findings
    }
}
