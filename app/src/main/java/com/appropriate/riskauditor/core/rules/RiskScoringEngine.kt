package com.appropriate.riskauditor.core.rules

import com.appropriate.riskauditor.core.model.*

/**
 * Deterministic on-device risk scoring engine.
 *
 * Implements:
 * 1. Special access scoring (highest weight).
 * 2. Declared vs granted permission evaluation with category-specific caps.
 * 3. Compound combination rule execution.
 * 4. Permission deduplication (e.g. coarse + fine location).
 * 5. Strict Three-Reason Enforcement Policy for High (50-74) and Critical (75-100) scores.
 * 6. Score clamping to [0, 100].
 */
object RiskScoringEngine {

    fun evaluate(
        inventory: AppInventoryRecord,
        permissions: List<PermissionAudit>,
        specialAccess: List<SpecialAccessAudit>,
        scannedAtEpochMillis: Long = System.currentTimeMillis()
    ): AppRiskReport {
        val findings = mutableListOf<RiskFinding>()
        var rawScore = 0

        // 1. Evaluate Special Access (Highest Weight)
        for (access in specialAccess) {
            when (access.type) {
                SpecialAccessType.ACCESSIBILITY_SERVICE -> {
                    if (access.state == SpecialAccessState.ENABLED) {
                        rawScore += RiskPolicy.PTS_SPECIAL_ACCESSIBILITY_ENABLED
                        findings.add(
                            RiskFinding(
                                ruleId = "RULE_SPECIAL_ACCESSIBILITY_ENABLED",
                                title = "Active Accessibility Service",
                                severity = FindingSeverity.HIGH,
                                points = RiskPolicy.PTS_SPECIAL_ACCESSIBILITY_ENABLED,
                                category = RiskCategory.SPECIAL_ACCESS,
                                evidence = access.detail ?: "Accessibility service component actively enabled",
                                explanation = "Accessibility services possess extensive device control, including screen reading, key event monitoring, and simulating user gestures.",
                                confidence = FindingConfidence.HIGH,
                                recommendation = "Disable Accessibility access in System Settings > Accessibility if not essential for assistive purposes."
                            )
                        )
                    } else if (access.isDeclared) {
                        rawScore += RiskPolicy.PTS_SPECIAL_ACCESSIBILITY_DECLARED
                        findings.add(
                            RiskFinding(
                                ruleId = "RULE_SPECIAL_ACCESSIBILITY_DECLARED",
                                title = "Declared Accessibility Service Component",
                                severity = FindingSeverity.LOW,
                                points = RiskPolicy.PTS_SPECIAL_ACCESSIBILITY_DECLARED,
                                category = RiskCategory.SPECIAL_ACCESS,
                                evidence = access.detail ?: "Accessibility service declared in manifest (currently disabled)",
                                explanation = "App defines an accessibility service component that is not currently enabled by the user.",
                                confidence = FindingConfidence.HIGH,
                                recommendation = "No immediate action required while service remains disabled."
                            )
                        )
                    }
                }

                SpecialAccessType.DEVICE_ADMIN -> {
                    if (access.state == SpecialAccessState.ENABLED) {
                        rawScore += RiskPolicy.PTS_SPECIAL_DEVICE_ADMIN_ACTIVE
                        findings.add(
                            RiskFinding(
                                ruleId = "RULE_SPECIAL_DEVICE_ADMIN_ACTIVE",
                                title = "Active Device Administrator Privileges",
                                severity = FindingSeverity.CRITICAL,
                                points = RiskPolicy.PTS_SPECIAL_DEVICE_ADMIN_ACTIVE,
                                category = RiskCategory.SPECIAL_ACCESS,
                                evidence = access.detail ?: "Active Device Administrator policy binding",
                                explanation = "Device administrator access grants system-level privileges such as remote device wiping, screen lock alteration, and preventing uninstallation.",
                                confidence = FindingConfidence.HIGH,
                                recommendation = "Deactivate this device administrator in Settings > Security > Device Admin Apps if not recognized."
                            )
                        )
                    } else if (access.isDeclared) {
                        rawScore += RiskPolicy.PTS_SPECIAL_DEVICE_ADMIN_DECLARED
                        findings.add(
                            RiskFinding(
                                ruleId = "RULE_SPECIAL_DEVICE_ADMIN_DECLARED",
                                title = "Declared Device Administrator Receiver",
                                severity = FindingSeverity.LOW,
                                points = RiskPolicy.PTS_SPECIAL_DEVICE_ADMIN_DECLARED,
                                category = RiskCategory.SPECIAL_ACCESS,
                                evidence = access.detail ?: "DeviceAdminReceiver declared in manifest",
                                explanation = "App requests capability to act as a device admin, but administrator rights have not been activated.",
                                confidence = FindingConfidence.HIGH,
                                recommendation = "Do not grant Device Administrator rights if prompted by this app."
                            )
                        )
                    }
                }

                SpecialAccessType.SYSTEM_ALERT_WINDOW -> {
                    if (access.state == SpecialAccessState.ENABLED) {
                        rawScore += RiskPolicy.PTS_SPECIAL_OVERLAY_GRANTED
                        findings.add(
                            RiskFinding(
                                ruleId = "RULE_SPECIAL_OVERLAY_GRANTED",
                                title = "Granted System Overlay (Draw Over Apps)",
                                severity = FindingSeverity.MEDIUM,
                                points = RiskPolicy.PTS_SPECIAL_OVERLAY_GRANTED,
                                category = RiskCategory.SPECIAL_ACCESS,
                                evidence = "SYSTEM_ALERT_WINDOW is granted / canDrawOverlays is true",
                                explanation = "The app can draw floating dialogs or invisible views over other applications, potentially enabling clickjacking or overlay attacks.",
                                confidence = FindingConfidence.HIGH,
                                recommendation = "Disable 'Display over other apps' in Special App Access settings if floating windows are not expected."
                            )
                        )
                    } else if (access.isDeclared) {
                        rawScore += RiskPolicy.PTS_SPECIAL_OVERLAY_DECLARED
                        findings.add(
                            RiskFinding(
                                ruleId = "RULE_SPECIAL_OVERLAY_DECLARED",
                                title = "Declared System Overlay Permission",
                                severity = FindingSeverity.LOW,
                                points = RiskPolicy.PTS_SPECIAL_OVERLAY_DECLARED,
                                category = RiskCategory.SPECIAL_ACCESS,
                                evidence = "SYSTEM_ALERT_WINDOW declared in manifest (not granted)",
                                explanation = "App declares overlay permission but lacks active system grant.",
                                confidence = FindingConfidence.HIGH,
                                recommendation = "Deny overlay access if requested by the application."
                            )
                        )
                    }
                }

                SpecialAccessType.NOTIFICATION_LISTENER -> {
                    if (access.state == SpecialAccessState.ENABLED) {
                        rawScore += RiskPolicy.PTS_SPECIAL_NOTIFICATION_LISTENER_ENABLED
                        findings.add(
                            RiskFinding(
                                ruleId = "RULE_SPECIAL_NOTIFICATION_LISTENER_ENABLED",
                                title = "Active Notification Listener Access",
                                severity = FindingSeverity.HIGH,
                                points = RiskPolicy.PTS_SPECIAL_NOTIFICATION_LISTENER_ENABLED,
                                category = RiskCategory.SPECIAL_ACCESS,
                                evidence = access.detail ?: "Notification listener service enabled",
                                explanation = "The app can inspect all device notifications, potentially exposing two-factor SMS codes, private chat previews, and banking alerts.",
                                confidence = FindingConfidence.HIGH,
                                recommendation = "Revoke Notification Access in Settings > Special App Access > Device & app notifications."
                            )
                        )
                    } else if (access.isDeclared) {
                        rawScore += RiskPolicy.PTS_SPECIAL_NOTIFICATION_LISTENER_DECLARED
                        findings.add(
                            RiskFinding(
                                ruleId = "RULE_SPECIAL_NOTIFICATION_LISTENER_DECLARED",
                                title = "Declared Notification Listener Service",
                                severity = FindingSeverity.LOW,
                                points = RiskPolicy.PTS_SPECIAL_NOTIFICATION_LISTENER_DECLARED,
                                category = RiskCategory.SPECIAL_ACCESS,
                                evidence = access.detail ?: "NotificationListenerService declared in manifest",
                                explanation = "App includes a notification listener component that is currently inactive.",
                                confidence = FindingConfidence.HIGH,
                                recommendation = "Do not grant Notification Access unless this is a trusted companion app."
                            )
                        )
                    }
                }

                SpecialAccessType.REQUEST_INSTALL_PACKAGES -> {
                    if (access.state == SpecialAccessState.ENABLED) {
                        rawScore += RiskPolicy.PTS_SPECIAL_INSTALL_PACKAGES_GRANTED
                        findings.add(
                            RiskFinding(
                                ruleId = "RULE_SPECIAL_INSTALL_PACKAGES_GRANTED",
                                title = "Granted Install Unknown Apps Capability",
                                severity = FindingSeverity.HIGH,
                                points = RiskPolicy.PTS_SPECIAL_INSTALL_PACKAGES_GRANTED,
                                category = RiskCategory.SPECIAL_ACCESS,
                                evidence = "REQUEST_INSTALL_PACKAGES granted / canRequestPackageInstalls is true",
                                explanation = "The app has permission to prompt the user to install unverified external APK installation files.",
                                confidence = FindingConfidence.HIGH,
                                recommendation = "Revoke 'Install unknown apps' permission in Special App Access settings."
                            )
                        )
                    } else if (access.isDeclared) {
                        rawScore += RiskPolicy.PTS_SPECIAL_INSTALL_PACKAGES_DECLARED
                        findings.add(
                            RiskFinding(
                                ruleId = "RULE_SPECIAL_INSTALL_PACKAGES_DECLARED",
                                title = "Declared Install Unknown Apps Permission",
                                severity = FindingSeverity.LOW,
                                points = RiskPolicy.PTS_SPECIAL_INSTALL_PACKAGES_DECLARED,
                                category = RiskCategory.SPECIAL_ACCESS,
                                evidence = "REQUEST_INSTALL_PACKAGES declared in manifest",
                                explanation = "App requests the ability to install unknown APKs but is currently ungranted.",
                                confidence = FindingConfidence.HIGH,
                                recommendation = "Keep unknown source installation disabled for this app."
                            )
                        )
                    }
                }

                SpecialAccessType.USAGE_STATS -> {
                    if (access.state == SpecialAccessState.ENABLED) {
                        rawScore += RiskPolicy.PTS_SPECIAL_USAGE_STATS_GRANTED
                        findings.add(
                            RiskFinding(
                                ruleId = "RULE_SPECIAL_USAGE_STATS_GRANTED",
                                title = "Granted Device Usage Access",
                                severity = FindingSeverity.MEDIUM,
                                points = RiskPolicy.PTS_SPECIAL_USAGE_STATS_GRANTED,
                                category = RiskCategory.SPECIAL_ACCESS,
                                evidence = "PACKAGE_USAGE_STATS access active",
                                explanation = "The app can monitor how often and when you use other applications.",
                                confidence = FindingConfidence.HIGH,
                                recommendation = "Disable Usage Access in Settings > Special App Access > Usage Access."
                            )
                        )
                    }
                }
            }
        }

        // 2. Evaluate Permission Categories with Deduplication and Category Point Caps
        val (permScore, permFindings) = evaluatePermissionCategories(permissions)
        rawScore += permScore
        findings.addAll(permFindings)

        // 3. Evaluate Compound Combination Rules
        val comboFindings = CombinationRules.evaluate(inventory, permissions, specialAccess)
        for (combo in comboFindings) {
            rawScore += combo.points
            findings.add(combo)
        }

        // 4. Enforce Three-Reason Policy for High and Critical Scores
        val (finalScore, finalRiskLevel) = enforceThreeReasonPolicy(rawScore, findings)

        return AppRiskReport(
            inventory = inventory,
            permissions = permissions,
            specialAccess = specialAccess,
            findings = findings,
            riskScore = finalScore,
            riskLevel = finalRiskLevel,
            scannedAtEpochMillis = scannedAtEpochMillis
        )
    }

    private fun evaluatePermissionCategories(permissions: List<PermissionAudit>): Pair<Int, List<RiskFinding>> {
        val findings = mutableListOf<RiskFinding>()
        var totalPermPoints = 0

        // Deduplicate permissions:
        // If ACCESS_FINE_LOCATION is present and granted, ignore ACCESS_COARSE_LOCATION
        val filteredPerms = permissions.filterNot { perm ->
            perm.name.equals("android.permission.ACCESS_COARSE_LOCATION", ignoreCase = true) &&
                    permissions.any { it.name.equals("android.permission.ACCESS_FINE_LOCATION", ignoreCase = true) && it.grantState == GrantState.GRANTED }
        }

        val grouped = filteredPerms.groupBy { it.category }

        for ((category, catPerms) in grouped) {
            var catScore = 0
            val grantedInCat = catPerms.filter { it.grantState == GrantState.GRANTED }
            val declaredOnlyInCat = catPerms.filter { it.grantState != GrantState.GRANTED && it.isDeclared }

            when (category) {
                RiskCategory.SMS -> {
                    if (grantedInCat.isNotEmpty()) {
                        val pts = (grantedInCat.size * RiskPolicy.PTS_PERM_SMS_GRANTED).coerceAtMost(RiskPolicy.MAX_CAT_SMS)
                        catScore += pts
                        findings.add(
                            RiskFinding(
                                ruleId = "RULE_PERM_CAT_SMS",
                                title = "Granted SMS & Messaging Access",
                                severity = FindingSeverity.HIGH,
                                points = pts,
                                category = RiskCategory.SMS,
                                evidence = "Granted: " + grantedInCat.joinToString(", ") { it.simpleName },
                                explanation = "Active SMS access permits reading incoming messages (including OTPs) and sending messages that may incur carrier costs.",
                                confidence = FindingConfidence.HIGH,
                                recommendation = "Revoke SMS permissions in Android App Permissions if the app is not a primary messaging tool."
                            )
                        )
                    }
                }

                RiskCategory.CONTACTS_AND_CALLS -> {
                    if (grantedInCat.isNotEmpty()) {
                        val pts = (grantedInCat.size * RiskPolicy.PTS_PERM_CONTACTS_CALLS_GRANTED).coerceAtMost(RiskPolicy.MAX_CAT_CONTACTS_CALLS)
                        catScore += pts
                        findings.add(
                            RiskFinding(
                                ruleId = "RULE_PERM_CAT_CONTACTS_CALLS",
                                title = "Granted Contacts & Call Log Access",
                                severity = FindingSeverity.MEDIUM,
                                points = pts,
                                category = RiskCategory.CONTACTS_AND_CALLS,
                                evidence = "Granted: " + grantedInCat.joinToString(", ") { it.simpleName },
                                explanation = "Active contacts or call log permissions permit reading personal address books and recent communications history.",
                                confidence = FindingConfidence.HIGH,
                                recommendation = "Revoke Contacts or Call Log permissions in Settings if not required for application features."
                            )
                        )
                    }
                }

                RiskCategory.CAMERA_AND_MIC -> {
                    if (grantedInCat.isNotEmpty()) {
                        val pts = (grantedInCat.size * RiskPolicy.PTS_PERM_CAMERA_MIC_GRANTED).coerceAtMost(RiskPolicy.MAX_CAT_CAMERA_MIC)
                        catScore += pts
                        findings.add(
                            RiskFinding(
                                ruleId = "RULE_PERM_CAT_CAMERA_MIC",
                                title = "Granted Camera / Microphone Access",
                                severity = FindingSeverity.MEDIUM,
                                points = pts,
                                category = RiskCategory.CAMERA_AND_MIC,
                                evidence = "Granted: " + grantedInCat.joinToString(", ") { it.simpleName },
                                explanation = "The app has active access to capture camera images or ambient microphone audio.",
                                confidence = FindingConfidence.HIGH,
                                recommendation = "Switch permission to 'Only while using the app' in System Settings."
                            )
                        )
                    }
                }

                RiskCategory.LOCATION -> {
                    if (grantedInCat.isNotEmpty()) {
                        val pts = (grantedInCat.size * RiskPolicy.PTS_PERM_LOCATION_GRANTED).coerceAtMost(RiskPolicy.MAX_CAT_LOCATION)
                        catScore += pts
                        findings.add(
                            RiskFinding(
                                ruleId = "RULE_PERM_CAT_LOCATION",
                                title = "Granted Geographic Location Access",
                                severity = FindingSeverity.MEDIUM,
                                points = pts,
                                category = RiskCategory.LOCATION,
                                evidence = "Granted: " + grantedInCat.joinToString(", ") { it.simpleName },
                                explanation = "Active location access allows the application to monitor your physical whereabouts.",
                                confidence = FindingConfidence.HIGH,
                                recommendation = "Set location access to 'Approximate' or 'While using app' in App Permissions."
                            )
                        )
                    }
                }

                RiskCategory.PHONE_AND_IDENTITY -> {
                    if (grantedInCat.isNotEmpty()) {
                        val pts = (grantedInCat.size * RiskPolicy.PTS_PERM_PHONE_IDENTITY_GRANTED).coerceAtMost(RiskPolicy.MAX_CAT_PHONE_IDENTITY)
                        catScore += pts
                        findings.add(
                            RiskFinding(
                                ruleId = "RULE_PERM_CAT_PHONE_IDENTITY",
                                title = "Granted Phone State & Identity Access",
                                severity = FindingSeverity.LOW,
                                points = pts,
                                category = RiskCategory.PHONE_AND_IDENTITY,
                                evidence = "Granted: " + grantedInCat.joinToString(", ") { it.simpleName },
                                explanation = "Permits reading device identifiers, phone numbers, or active call status.",
                                confidence = FindingConfidence.HIGH,
                                recommendation = "Review whether phone state access is necessary for this application."
                            )
                        )
                    }
                }

                RiskCategory.STORAGE_AND_MEDIA -> {
                    if (grantedInCat.isNotEmpty()) {
                        val pts = (grantedInCat.size * RiskPolicy.PTS_PERM_STORAGE_MEDIA_GRANTED).coerceAtMost(RiskPolicy.MAX_CAT_STORAGE_MEDIA)
                        catScore += pts
                        findings.add(
                            RiskFinding(
                                ruleId = "RULE_PERM_CAT_STORAGE_MEDIA",
                                title = "Granted Storage & Media Access",
                                severity = FindingSeverity.LOW,
                                points = pts,
                                category = RiskCategory.STORAGE_AND_MEDIA,
                                evidence = "Granted: " + grantedInCat.joinToString(", ") { it.simpleName },
                                explanation = "Permits reading or writing shared files, photos, or media on external storage.",
                                confidence = FindingConfidence.HIGH,
                                recommendation = "Ensure the app only has access to photos/media it legitimately requires."
                            )
                        )
                    }
                }

                RiskCategory.BACKGROUND_AND_PERSISTENCE -> {
                    if (grantedInCat.isNotEmpty()) {
                        val pts = (grantedInCat.size * RiskPolicy.PTS_PERM_BG_PERSISTENCE_GRANTED).coerceAtMost(RiskPolicy.MAX_CAT_BG_PERSISTENCE)
                        catScore += pts
                        findings.add(
                            RiskFinding(
                                ruleId = "RULE_PERM_CAT_BG_PERSISTENCE",
                                title = "Background Auto-Start / Persistence",
                                severity = FindingSeverity.LOW,
                                points = pts,
                                category = RiskCategory.BACKGROUND_AND_PERSISTENCE,
                                evidence = "Granted: " + grantedInCat.joinToString(", ") { it.simpleName },
                                explanation = "App can execute tasks at boot or run persistent foreground services in the background.",
                                confidence = FindingConfidence.HIGH,
                                recommendation = "Restrict battery usage to 'Optimized' or 'Restricted' if background running is unwanted."
                            )
                        )
                    }
                }

                RiskCategory.NETWORK -> {
                    if (grantedInCat.isNotEmpty()) {
                        val pts = (grantedInCat.size * RiskPolicy.PTS_PERM_NETWORK_GRANTED).coerceAtMost(RiskPolicy.MAX_CAT_NETWORK)
                        catScore += pts
                        findings.add(
                            RiskFinding(
                                ruleId = "RULE_PERM_CAT_NETWORK",
                                title = "Network & Internet Capability",
                                severity = FindingSeverity.INFO,
                                points = pts,
                                category = RiskCategory.NETWORK,
                                evidence = "Granted: " + grantedInCat.joinToString(", ") { it.simpleName },
                                explanation = "Standard outbound network connectivity permission.",
                                confidence = FindingConfidence.HIGH,
                                recommendation = "Standard permission for connected applications."
                            )
                        )
                    }
                }

                RiskCategory.SPECIAL_ACCESS -> {
                    // Special access handled separately
                }
            }

            // Declared only (denied) in category gives nominal visibility points
            if (declaredOnlyInCat.isNotEmpty() && catScore == 0) {
                val pts = (declaredOnlyInCat.size * RiskPolicy.PTS_PERM_DECLARED_ONLY).coerceAtMost(RiskPolicy.MAX_CAT_DECLARED_ONLY)
                catScore += pts
            }

            totalPermPoints += catScore
        }

        return Pair(totalPermPoints, findings)
    }

    /**
     * Enforces the PS-01 Three-Reason Enforcement Policy:
     * Every High (50-74) or Critical (75-100) result MUST be supported by at least
     * three distinct, evidence-backed findings. If fewer than 3 reasons exist,
     * the score is capped at 49 (Medium band).
     */
    fun enforceThreeReasonPolicy(rawScore: Int, findings: List<RiskFinding>): Pair<Int, RiskLevel> {
        val clampedScore = rawScore.coerceIn(0, 100)
        val distinctRuleCount = findings.map { it.ruleId }.distinct().size

        val finalScore = if (clampedScore >= RiskLevel.HIGH.minScore &&
            distinctRuleCount < RiskPolicy.MIN_DISTINCT_REASONS_FOR_HIGH_OR_CRITICAL
        ) {
            // Capped at upper boundary of Medium band
            RiskPolicy.CAP_SCORE_FEWER_REASONS
        } else {
            clampedScore
        }

        val finalRiskLevel = RiskLevel.fromScore(finalScore)
        return Pair(finalScore, finalRiskLevel)
    }
}
