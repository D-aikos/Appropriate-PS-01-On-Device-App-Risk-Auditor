package com.appropriate.riskauditor.core.rules

import com.appropriate.riskauditor.core.model.*
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Signal view over the application inventory, permissions, and special access states.
 * Handles permission deduplication (T16) and multiplier lookups.
 */
class SignalView(
    val inventory: AppInventoryRecord,
    val permissions: List<PermissionAudit>,
    val specialAccess: List<SpecialAccessAudit>
) {
    val appLabel: String = inventory.appLabel
    val packageName: String = inventory.packageName
    val isSystemApp: Boolean = inventory.isSystemApp

    val isSideloaded: Boolean = inventory.isSideloaded

    // Map permission simple name (uppercase) to highest multiplier observed.
    // Handles duplicate permission names in record by counting once with highest state (T16).
    private val permMultipliers: Map<String, Double> by lazy {
        val map = mutableMapOf<String, Double>()
        for (p in permissions) {
            val key = p.name.substringAfterLast('.').uppercase()
            val m = when (p.grantState) {
                GrantState.GRANTED -> RiskPolicy.MULTIPLIER_GRANTED
                GrantState.NOT_RUNTIME -> RiskPolicy.MULTIPLIER_NOT_RUNTIME
                GrantState.UNKNOWN -> RiskPolicy.MULTIPLIER_UNKNOWN
                GrantState.DENIED -> RiskPolicy.MULTIPLIER_DECLARED_ONLY
                GrantState.NOT_SUPPORTED -> RiskPolicy.MULTIPLIER_NOT_SUPPORTED
            }
            val existing = map[key] ?: 0.0
            if (m > existing) {
                map[key] = m
            }
        }
        map
    }

    private val specialMap: Map<SpecialAccessType, SpecialAccessAudit> by lazy {
        specialAccess.associateBy { it.type }
    }

    fun permMultiplier(shortName: String): Double = permMultipliers[shortName.uppercase()] ?: 0.0

    fun hasPermGranted(shortName: String): Boolean = (permMultipliers[shortName.uppercase()] ?: 0.0) >= 1.0

    fun isPermDeclared(shortName: String): Boolean = (permMultipliers[shortName.uppercase()] ?: 0.0) > 0.0

    fun specialState(type: SpecialAccessType): SpecialAccessState =
        specialMap[type]?.state ?: SpecialAccessState.NOT_DECLARED

    fun specialAudit(type: SpecialAccessType): SpecialAccessAudit? = specialMap[type]

    fun specialMultiplier(type: SpecialAccessType): Double = when (specialState(type)) {
        SpecialAccessState.ENABLED -> RiskPolicy.MULTIPLIER_ENABLED
        SpecialAccessState.UNKNOWN -> RiskPolicy.MULTIPLIER_UNKNOWN
        SpecialAccessState.DISABLED -> RiskPolicy.MULTIPLIER_DECLARED_ONLY
        SpecialAccessState.NOT_DECLARED -> RiskPolicy.MULTIPLIER_NOT_DECLARED
        SpecialAccessState.NOT_SUPPORTED -> RiskPolicy.MULTIPLIER_NOT_SUPPORTED
    }

    val isDeviceOrProfileOwner: Boolean
        get() {
            val adminAudit = specialMap[SpecialAccessType.DEVICE_ADMIN] ?: return false
            return adminAudit.detail?.contains("owner", ignoreCase = true) == true
        }

    fun hasAnyGrantedDangerousOrSpecial(): Boolean {
        if (specialAccess.any { it.state == SpecialAccessState.ENABLED }) return true
        val dangerousCategories = setOf(
            RiskCategory.SMS,
            RiskCategory.CONTACTS_AND_CALLS,
            RiskCategory.CAMERA_AND_MIC,
            RiskCategory.LOCATION,
            RiskCategory.PHONE_AND_IDENTITY,
            RiskCategory.STORAGE_AND_MEDIA
        )
        return permissions.any {
            it.grantState == GrantState.GRANTED && (
                it.protection == ProtectionLevel.DANGEROUS || it.category in dangerousCategories
            )
        }
    }
}

/**
 * Individual rule implementations strictly following Section 7.2 and 7.5.
 */
object Rules {

    // -------------------------------------------------------------
    // 1. Special Access Rules (SA_*)
    // -------------------------------------------------------------

    fun ruleSaAccessibility(sig: SignalView): RiskFinding? {
        val state = sig.specialState(SpecialAccessType.ACCESSIBILITY_SERVICE)
        val mult = sig.specialMultiplier(SpecialAccessType.ACCESSIBILITY_SERVICE)
        val points = (RiskPolicy.PTS_SA_ACCESSIBILITY * mult).roundToInt()
        if (points <= 0) return null

        val text = Explanations.saAccessibility(sig.appLabel, state)
        val stateStr = when (state) {
            SpecialAccessState.ENABLED -> "on"
            SpecialAccessState.UNKNOWN -> "couldn't check"
            else -> "off"
        }
        return RiskFinding(
            ruleId = "SA_ACCESSIBILITY",
            title = text.title,
            severity = if (state == SpecialAccessState.ENABLED) FindingSeverity.HIGH else FindingSeverity.LOW,
            points = points,
            category = RiskCategory.SPECIAL_ACCESS,
            evidence = "Accessibility service ($stateStr)",
            explanation = text.explanation,
            confidence = FindingConfidence.HIGH,
            recommendation = text.recommendation
        )
    }

    fun ruleSaDeviceAdmin(sig: SignalView): RiskFinding? {
        if (sig.isDeviceOrProfileOwner) return null
        val state = sig.specialState(SpecialAccessType.DEVICE_ADMIN)
        val mult = sig.specialMultiplier(SpecialAccessType.DEVICE_ADMIN)
        val points = (RiskPolicy.PTS_SA_DEVICE_ADMIN * mult).roundToInt()
        if (points <= 0) return null

        val text = Explanations.saDeviceAdmin(sig.appLabel, state)
        val stateStr = when (state) {
            SpecialAccessState.ENABLED -> "active"
            SpecialAccessState.UNKNOWN -> "couldn't check"
            else -> "inactive"
        }
        return RiskFinding(
            ruleId = "SA_DEVICE_ADMIN",
            title = text.title,
            severity = if (state == SpecialAccessState.ENABLED) FindingSeverity.HIGH else FindingSeverity.LOW,
            points = points,
            category = RiskCategory.SPECIAL_ACCESS,
            evidence = "Device admin ($stateStr)",
            explanation = text.explanation,
            confidence = FindingConfidence.HIGH,
            recommendation = text.recommendation
        )
    }

    fun ruleSaNotifListener(sig: SignalView): RiskFinding? {
        val state = sig.specialState(SpecialAccessType.NOTIFICATION_LISTENER)
        val mult = sig.specialMultiplier(SpecialAccessType.NOTIFICATION_LISTENER)
        val points = (RiskPolicy.PTS_SA_NOTIF_LISTENER * mult).roundToInt()
        if (points <= 0) return null

        val text = Explanations.saNotifListener(sig.appLabel, state)
        val stateStr = when (state) {
            SpecialAccessState.ENABLED -> "on"
            SpecialAccessState.UNKNOWN -> "couldn't check"
            else -> "off"
        }
        return RiskFinding(
            ruleId = "SA_NOTIF_LISTENER",
            title = text.title,
            severity = if (state == SpecialAccessState.ENABLED) FindingSeverity.HIGH else FindingSeverity.LOW,
            points = points,
            category = RiskCategory.SPECIAL_ACCESS,
            evidence = "Notification listener ($stateStr)",
            explanation = text.explanation,
            confidence = FindingConfidence.HIGH,
            recommendation = text.recommendation
        )
    }

    fun ruleSaOverlay(sig: SignalView): RiskFinding? {
        val state = sig.specialState(SpecialAccessType.SYSTEM_ALERT_WINDOW)
        val mult = sig.specialMultiplier(SpecialAccessType.SYSTEM_ALERT_WINDOW)
        val points = (RiskPolicy.PTS_SA_OVERLAY * mult).roundToInt()
        if (points <= 0) return null

        val text = Explanations.saOverlay(sig.appLabel, state)
        val stateStr = when (state) {
            SpecialAccessState.ENABLED -> "on"
            SpecialAccessState.UNKNOWN -> "couldn't check"
            else -> "off"
        }
        return RiskFinding(
            ruleId = "SA_OVERLAY",
            title = text.title,
            severity = FindingSeverity.MEDIUM,
            points = points,
            category = RiskCategory.SPECIAL_ACCESS,
            evidence = "Draw over other apps ($stateStr)",
            explanation = text.explanation,
            confidence = FindingConfidence.HIGH,
            recommendation = text.recommendation
        )
    }

    fun ruleSaInstallPackages(sig: SignalView): RiskFinding? {
        val state = sig.specialState(SpecialAccessType.REQUEST_INSTALL_PACKAGES)
        val mult = sig.specialMultiplier(SpecialAccessType.REQUEST_INSTALL_PACKAGES)
        val points = (RiskPolicy.PTS_SA_INSTALL_PACKAGES * mult).roundToInt()
        if (points <= 0) return null

        val text = Explanations.saInstallPackages(sig.appLabel, state)
        val stateStr = when (state) {
            SpecialAccessState.ENABLED -> "on"
            SpecialAccessState.UNKNOWN -> "couldn't check"
            else -> "off"
        }
        return RiskFinding(
            ruleId = "SA_INSTALL_PACKAGES",
            title = text.title,
            severity = FindingSeverity.MEDIUM,
            points = points,
            category = RiskCategory.SPECIAL_ACCESS,
            evidence = "Install unknown apps ($stateStr)",
            explanation = text.explanation,
            confidence = FindingConfidence.HIGH,
            recommendation = text.recommendation
        )
    }

    fun ruleSaUsageStats(sig: SignalView): RiskFinding? {
        val state = sig.specialState(SpecialAccessType.USAGE_STATS)
        val mult = sig.specialMultiplier(SpecialAccessType.USAGE_STATS)
        val points = (RiskPolicy.PTS_SA_USAGE_STATS * mult).roundToInt()
        if (points <= 0) return null

        val text = Explanations.saUsageStats(sig.appLabel, state)
        val stateStr = when (state) {
            SpecialAccessState.ENABLED -> "on"
            SpecialAccessState.UNKNOWN -> "couldn't check"
            else -> "off"
        }
        return RiskFinding(
            ruleId = "SA_USAGE_STATS",
            title = text.title,
            severity = FindingSeverity.MEDIUM,
            points = points,
            category = RiskCategory.SPECIAL_ACCESS,
            evidence = "Usage access ($stateStr)",
            explanation = text.explanation,
            confidence = FindingConfidence.HIGH,
            recommendation = text.recommendation
        )
    }

    // -------------------------------------------------------------
    // 2. Category Rules (CAT_*)
    // -------------------------------------------------------------

    fun ruleCatSms(sig: SignalView): RiskFinding? {
        val read = sig.permMultiplier("READ_SMS")
        val recv = sig.permMultiplier("RECEIVE_SMS")
        val send = sig.permMultiplier("SEND_SMS")
        val mms = sig.permMultiplier("RECEIVE_MMS")
        val wap = sig.permMultiplier("RECEIVE_WAP_PUSH")

        val sum = (RiskPolicy.WEIGHT_READ_SMS * read) +
            (RiskPolicy.WEIGHT_RECEIVE_SMS * recv) +
            (RiskPolicy.WEIGHT_SEND_SMS * send) +
            (RiskPolicy.WEIGHT_RECEIVE_MMS * mms) +
            (RiskPolicy.WEIGHT_RECEIVE_WAP_PUSH * wap)

        val rawPoints = sum.roundToInt()
        val points = min(RiskPolicy.CAP_CAT_SMS, rawPoints)
        if (points <= 0) return null

        val hasGranted = read >= 1.0 || recv >= 1.0 || send >= 1.0 || mms >= 1.0 || wap >= 1.0
        val evidenceList = mutableListOf<String>()
        if (read > 0) evidenceList.add("READ_SMS ${permStateLabel(read)}")
        if (recv > 0) evidenceList.add("RECEIVE_SMS ${permStateLabel(recv)}")
        if (send > 0) evidenceList.add("SEND_SMS ${permStateLabel(send)}")
        if (mms > 0) evidenceList.add("RECEIVE_MMS ${permStateLabel(mms)}")
        if (wap > 0) evidenceList.add("RECEIVE_WAP_PUSH ${permStateLabel(wap)}")

        val text = Explanations.catSms(sig.appLabel, hasGranted, evidenceList.joinToString(", "))
        return RiskFinding(
            ruleId = "CAT_SMS",
            title = text.title,
            severity = if (hasGranted) FindingSeverity.HIGH else FindingSeverity.LOW,
            points = points,
            category = RiskCategory.SMS,
            evidence = evidenceList.joinToString(", "),
            explanation = text.explanation,
            confidence = FindingConfidence.HIGH,
            recommendation = text.recommendation
        )
    }

    fun ruleCatContactsCalls(sig: SignalView): RiskFinding? {
        val readContacts = sig.permMultiplier("READ_CONTACTS")
        val writeContacts = sig.permMultiplier("WRITE_CONTACTS")
        val readCallLog = sig.permMultiplier("READ_CALL_LOG")
        val writeCallLog = sig.permMultiplier("WRITE_CALL_LOG")
        val procCalls = sig.permMultiplier("PROCESS_OUTGOING_CALLS")

        val sum = (RiskPolicy.WEIGHT_READ_CONTACTS * readContacts) +
            (RiskPolicy.WEIGHT_WRITE_CONTACTS * writeContacts) +
            (RiskPolicy.WEIGHT_READ_CALL_LOG * readCallLog) +
            (RiskPolicy.WEIGHT_WRITE_CALL_LOG * writeCallLog) +
            (RiskPolicy.WEIGHT_PROCESS_OUTGOING_CALLS * procCalls)

        val rawPoints = sum.roundToInt()
        val points = min(RiskPolicy.CAP_CAT_CONTACTS_CALLS, rawPoints)
        if (points <= 0) return null

        val hasGranted = readContacts >= 1.0 || writeContacts >= 1.0 || readCallLog >= 1.0 ||
            writeCallLog >= 1.0 || procCalls >= 1.0
        val evidenceList = mutableListOf<String>()
        if (readContacts > 0) evidenceList.add("READ_CONTACTS ${permStateLabel(readContacts)}")
        if (writeContacts > 0) evidenceList.add("WRITE_CONTACTS ${permStateLabel(writeContacts)}")
        if (readCallLog > 0) evidenceList.add("READ_CALL_LOG ${permStateLabel(readCallLog)}")
        if (writeCallLog > 0) evidenceList.add("WRITE_CALL_LOG ${permStateLabel(writeCallLog)}")
        if (procCalls > 0) evidenceList.add("PROCESS_OUTGOING_CALLS ${permStateLabel(procCalls)}")

        val text = Explanations.catContactsCalls(sig.appLabel, hasGranted, evidenceList.joinToString(", "))
        return RiskFinding(
            ruleId = "CAT_CONTACTS_CALLS",
            title = text.title,
            severity = if (hasGranted) FindingSeverity.HIGH else FindingSeverity.LOW,
            points = points,
            category = RiskCategory.CONTACTS_AND_CALLS,
            evidence = evidenceList.joinToString(", "),
            explanation = text.explanation,
            confidence = FindingConfidence.HIGH,
            recommendation = text.recommendation
        )
    }

    fun ruleCatCameraMic(sig: SignalView): RiskFinding? {
        val camera = sig.permMultiplier("CAMERA")
        val recordAudio = sig.permMultiplier("RECORD_AUDIO")

        val sum = (RiskPolicy.WEIGHT_CAMERA * camera) + (RiskPolicy.WEIGHT_RECORD_AUDIO * recordAudio)
        val rawPoints = sum.roundToInt()
        val points = min(RiskPolicy.CAP_CAT_CAMERA_MIC, rawPoints)
        if (points <= 0) return null

        val hasGranted = camera >= 1.0 || recordAudio >= 1.0
        val evidenceList = mutableListOf<String>()
        if (camera > 0) evidenceList.add("CAMERA ${permStateLabel(camera)}")
        if (recordAudio > 0) evidenceList.add("RECORD_AUDIO ${permStateLabel(recordAudio)}")

        val text = Explanations.catCameraMic(sig.appLabel, hasGranted, evidenceList.joinToString(", "))
        return RiskFinding(
            ruleId = "CAT_CAMERA_MIC",
            title = text.title,
            severity = if (hasGranted) FindingSeverity.HIGH else FindingSeverity.LOW,
            points = points,
            category = RiskCategory.CAMERA_AND_MIC,
            evidence = evidenceList.joinToString(", "),
            explanation = text.explanation,
            confidence = FindingConfidence.HIGH,
            recommendation = text.recommendation
        )
    }

    fun ruleCatLocation(sig: SignalView): RiskFinding? {
        val coarse = sig.permMultiplier("ACCESS_COARSE_LOCATION")
        val fine = sig.permMultiplier("ACCESS_FINE_LOCATION")
        val bg = sig.permMultiplier("ACCESS_BACKGROUND_LOCATION")

        val sum = (RiskPolicy.WEIGHT_ACCESS_COARSE_LOCATION * coarse) +
            (RiskPolicy.WEIGHT_ACCESS_FINE_LOCATION * fine) +
            (RiskPolicy.WEIGHT_ACCESS_BACKGROUND_LOCATION * bg)

        val rawPoints = sum.roundToInt()
        val points = min(RiskPolicy.CAP_CAT_LOCATION, rawPoints)
        if (points <= 0) return null

        val hasGranted = coarse >= 1.0 || fine >= 1.0 || bg >= 1.0
        val evidenceList = mutableListOf<String>()
        if (coarse > 0) evidenceList.add("ACCESS_COARSE_LOCATION ${permStateLabel(coarse)}")
        if (fine > 0) evidenceList.add("ACCESS_FINE_LOCATION ${permStateLabel(fine)}")
        if (bg > 0) evidenceList.add("ACCESS_BACKGROUND_LOCATION ${permStateLabel(bg)}")

        val text = Explanations.catLocation(sig.appLabel, hasGranted, evidenceList.joinToString(", "))
        return RiskFinding(
            ruleId = "CAT_LOCATION",
            title = text.title,
            severity = if (hasGranted) FindingSeverity.HIGH else FindingSeverity.LOW,
            points = points,
            category = RiskCategory.LOCATION,
            evidence = evidenceList.joinToString(", "),
            explanation = text.explanation,
            confidence = FindingConfidence.HIGH,
            recommendation = text.recommendation
        )
    }

    fun ruleCatPhoneIdentity(sig: SignalView): RiskFinding? {
        val phoneState = sig.permMultiplier("READ_PHONE_STATE")
        val phoneNumbers = sig.permMultiplier("READ_PHONE_NUMBERS")
        val callPhone = sig.permMultiplier("CALL_PHONE")
        val ansCalls = sig.permMultiplier("ANSWER_PHONE_CALLS")
        val getAccounts = sig.permMultiplier("GET_ACCOUNTS")

        val sum = (RiskPolicy.WEIGHT_READ_PHONE_STATE * phoneState) +
            (RiskPolicy.WEIGHT_READ_PHONE_NUMBERS * phoneNumbers) +
            (RiskPolicy.WEIGHT_CALL_PHONE * callPhone) +
            (RiskPolicy.WEIGHT_ANSWER_PHONE_CALLS * ansCalls) +
            (RiskPolicy.WEIGHT_GET_ACCOUNTS * getAccounts)

        val rawPoints = sum.roundToInt()
        val points = min(RiskPolicy.CAP_CAT_PHONE_IDENTITY, rawPoints)
        if (points <= 0) return null

        val hasGranted = phoneState >= 1.0 || phoneNumbers >= 1.0 || callPhone >= 1.0 ||
            ansCalls >= 1.0 || getAccounts >= 1.0
        val evidenceList = mutableListOf<String>()
        if (phoneState > 0) evidenceList.add("READ_PHONE_STATE ${permStateLabel(phoneState)}")
        if (phoneNumbers > 0) evidenceList.add("READ_PHONE_NUMBERS ${permStateLabel(phoneNumbers)}")
        if (callPhone > 0) evidenceList.add("CALL_PHONE ${permStateLabel(callPhone)}")
        if (ansCalls > 0) evidenceList.add("ANSWER_PHONE_CALLS ${permStateLabel(ansCalls)}")
        if (getAccounts > 0) evidenceList.add("GET_ACCOUNTS ${permStateLabel(getAccounts)}")

        val text = Explanations.catPhoneIdentity(sig.appLabel, hasGranted, evidenceList.joinToString(", "))
        return RiskFinding(
            ruleId = "CAT_PHONE_IDENTITY",
            title = text.title,
            severity = if (hasGranted) FindingSeverity.MEDIUM else FindingSeverity.LOW,
            points = points,
            category = RiskCategory.PHONE_AND_IDENTITY,
            evidence = evidenceList.joinToString(", "),
            explanation = text.explanation,
            confidence = FindingConfidence.HIGH,
            recommendation = text.recommendation
        )
    }

    fun ruleCatStorageMedia(sig: SignalView): RiskFinding? {
        val manage = sig.permMultiplier("MANAGE_EXTERNAL_STORAGE")
        val images = sig.permMultiplier("READ_MEDIA_IMAGES")
        val video = sig.permMultiplier("READ_MEDIA_VIDEO")
        val readExt = sig.permMultiplier("READ_EXTERNAL_STORAGE")
        val writeExt = sig.permMultiplier("WRITE_EXTERNAL_STORAGE")
        val sharedExt = maxOf(readExt, writeExt) // Counted once (Section 7.2)
        val audio = sig.permMultiplier("READ_MEDIA_AUDIO")
        val visual = sig.permMultiplier("READ_MEDIA_VISUAL_USER_SELECTED")

        val sum = (RiskPolicy.WEIGHT_MANAGE_EXTERNAL_STORAGE * manage) +
            (RiskPolicy.WEIGHT_READ_MEDIA_IMAGES * images) +
            (RiskPolicy.WEIGHT_READ_MEDIA_VIDEO * video) +
            (RiskPolicy.WEIGHT_EXTERNAL_STORAGE_SHARED * sharedExt) +
            (RiskPolicy.WEIGHT_READ_MEDIA_AUDIO * audio) +
            (RiskPolicy.WEIGHT_READ_MEDIA_VISUAL_USER_SELECTED * visual)

        val rawPoints = sum.roundToInt()
        val points = min(RiskPolicy.CAP_CAT_STORAGE_MEDIA, rawPoints)
        if (points <= 0) return null

        val hasGranted = manage >= 1.0 || images >= 1.0 || video >= 1.0 ||
            sharedExt >= 1.0 || audio >= 1.0 || visual >= 1.0
        val evidenceList = mutableListOf<String>()
        if (manage > 0) evidenceList.add("MANAGE_EXTERNAL_STORAGE ${permStateLabel(manage)}")
        if (images > 0) evidenceList.add("READ_MEDIA_IMAGES ${permStateLabel(images)}")
        if (video > 0) evidenceList.add("READ_MEDIA_VIDEO ${permStateLabel(video)}")
        if (sharedExt > 0) evidenceList.add("READ/WRITE_EXTERNAL_STORAGE ${permStateLabel(sharedExt)}")
        if (audio > 0) evidenceList.add("READ_MEDIA_AUDIO ${permStateLabel(audio)}")
        if (visual > 0) evidenceList.add("READ_MEDIA_VISUAL_USER_SELECTED ${permStateLabel(visual)}")

        val text = Explanations.catStorageMedia(sig.appLabel, hasGranted, evidenceList.joinToString(", "))
        return RiskFinding(
            ruleId = "CAT_STORAGE_MEDIA",
            title = text.title,
            severity = if (hasGranted) FindingSeverity.MEDIUM else FindingSeverity.LOW,
            points = points,
            category = RiskCategory.STORAGE_AND_MEDIA,
            evidence = evidenceList.joinToString(", "),
            explanation = text.explanation,
            confidence = FindingConfidence.HIGH,
            recommendation = text.recommendation
        )
    }

    fun ruleCatBackground(sig: SignalView): RiskFinding? {
        val boot = sig.permMultiplier("RECEIVE_BOOT_COMPLETED")

        // Any FOREGROUND_SERVICE* counted once (Section 7.2)
        var fgsMax = sig.permMultiplier("FOREGROUND_SERVICE")
        for (p in sig.permissions) {
            val short = p.name.substringAfterLast('.').uppercase()
            if (short.startsWith("FOREGROUND_SERVICE")) {
                val m = sig.permMultiplier(short)
                if (m > fgsMax) fgsMax = m
            }
        }

        val ignoreOpt = sig.permMultiplier("REQUEST_IGNORE_BATTERY_OPTIMIZATIONS")
        val wake = sig.permMultiplier("WAKE_LOCK")
        val alarm = sig.permMultiplier("SCHEDULE_EXACT_ALARM")
        val fullScreen = sig.permMultiplier("USE_FULL_SCREEN_INTENT")

        val sum = (RiskPolicy.WEIGHT_RECEIVE_BOOT_COMPLETED * boot) +
            (RiskPolicy.WEIGHT_FOREGROUND_SERVICE * fgsMax) +
            (RiskPolicy.WEIGHT_IGNORE_BATTERY_OPT * ignoreOpt) +
            (RiskPolicy.WEIGHT_WAKE_LOCK * wake) +
            (RiskPolicy.WEIGHT_SCHEDULE_EXACT_ALARM * alarm) +
            (RiskPolicy.WEIGHT_USE_FULL_SCREEN_INTENT * fullScreen)

        val rawPoints = sum.roundToInt()
        val points = min(RiskPolicy.CAP_CAT_BACKGROUND, rawPoints)
        if (points <= 0) return null

        val hasGranted = boot >= 1.0 || fgsMax >= 1.0 || ignoreOpt >= 1.0 ||
            wake >= 1.0 || alarm >= 1.0 || fullScreen >= 1.0
        val evidenceList = mutableListOf<String>()
        if (boot > 0) evidenceList.add("RECEIVE_BOOT_COMPLETED ${permStateLabel(boot)}")
        if (fgsMax > 0) evidenceList.add("FOREGROUND_SERVICE ${permStateLabel(fgsMax)}")
        if (ignoreOpt > 0) evidenceList.add("REQUEST_IGNORE_BATTERY_OPTIMIZATIONS ${permStateLabel(ignoreOpt)}")
        if (wake > 0) evidenceList.add("WAKE_LOCK ${permStateLabel(wake)}")
        if (alarm > 0) evidenceList.add("SCHEDULE_EXACT_ALARM ${permStateLabel(alarm)}")
        if (fullScreen > 0) evidenceList.add("USE_FULL_SCREEN_INTENT ${permStateLabel(fullScreen)}")

        val text = Explanations.catBackground(sig.appLabel, hasGranted, evidenceList.joinToString(", "))
        return RiskFinding(
            ruleId = "CAT_BACKGROUND",
            title = text.title,
            severity = if (hasGranted) FindingSeverity.MEDIUM else FindingSeverity.LOW,
            points = points,
            category = RiskCategory.BACKGROUND_AND_PERSISTENCE,
            evidence = evidenceList.joinToString(", "),
            explanation = text.explanation,
            confidence = FindingConfidence.HIGH,
            recommendation = text.recommendation
        )
    }

    // -------------------------------------------------------------
    // 3. Combination Rules (COMBO_*)
    // -------------------------------------------------------------

    fun ruleComboAccessOverlay(sig: SignalView): RiskFinding? {
        val a11y = sig.specialMultiplier(SpecialAccessType.ACCESSIBILITY_SERVICE)
        val overlay = sig.specialMultiplier(SpecialAccessType.SYSTEM_ALERT_WINDOW)
        if (a11y <= 0 || overlay <= 0) return null

        val lowestMult = minOf(a11y, overlay)
        val points = (RiskPolicy.BASE_COMBO_ACCESS_OVERLAY * lowestMult).roundToInt()
        if (points <= 0) return null

        val text = Explanations.comboAccessOverlay(sig.appLabel)
        return RiskFinding(
            ruleId = "COMBO_ACCESS_OVERLAY",
            title = text.title,
            severity = FindingSeverity.CRITICAL,
            points = points,
            category = RiskCategory.SPECIAL_ACCESS,
            evidence = "Accessibility (${sig.specialState(SpecialAccessType.ACCESSIBILITY_SERVICE)}) + Overlay (${sig.specialState(SpecialAccessType.SYSTEM_ALERT_WINDOW)})",
            explanation = text.explanation,
            confidence = FindingConfidence.HIGH,
            recommendation = text.recommendation
        )
    }

    fun ruleComboSmsNetBootSideload(sig: SignalView): RiskFinding? {
        val sms = maxOf(sig.permMultiplier("READ_SMS"), sig.permMultiplier("RECEIVE_SMS"))
        val net = sig.permMultiplier("INTERNET")
        val boot = sig.permMultiplier("RECEIVE_BOOT_COMPLETED")
        if (sms <= 0 || net <= 0 || boot <= 0) return null

        val isSideloaded = sig.isSideloaded
        val isUnknownSource = sig.inventory.installerPackage == null && sig.isSystemApp

        if (!isSideloaded && !isUnknownSource) return null

        val base = if (isSideloaded) RiskPolicy.BASE_COMBO_SMS_NET_BOOT_SIDELOAD else RiskPolicy.BASE_COMBO_SMS_NET_BOOT_UNKNOWN
        val lowestMult = minOf(sms, net, boot)
        val points = (base * lowestMult).roundToInt()
        if (points <= 0) return null

        val text = Explanations.comboSmsNetBootSideload(sig.appLabel, isSideloaded)
        val sourceStr = if (isSideloaded) "Installed outside a store" else "Unknown source"
        return RiskFinding(
            ruleId = "COMBO_SMS_NET_BOOT_SIDELOAD",
            title = text.title,
            severity = FindingSeverity.CRITICAL,
            points = points,
            category = RiskCategory.SMS,
            evidence = "SMS (allowed) + INTERNET (allowed) + RECEIVE_BOOT_COMPLETED (allowed) + $sourceStr",
            explanation = text.explanation,
            confidence = if (isSideloaded) FindingConfidence.HIGH else FindingConfidence.MEDIUM,
            recommendation = text.recommendation
        )
    }

    fun ruleComboAdminNonMgmt(sig: SignalView): RiskFinding? {
        val adminMult = sig.specialMultiplier(SpecialAccessType.DEVICE_ADMIN)
        if (adminMult <= 0) return null
        if (sig.isDeviceOrProfileOwner) return null
        if (sig.packageName in RiskPolicy.SECURITY_OR_MDM_PACKAGES) return null

        val points = (RiskPolicy.BASE_COMBO_ADMIN_NON_MGMT * adminMult).roundToInt()
        if (points <= 0) return null

        val text = Explanations.comboAdminNonMgmt(sig.appLabel)
        return RiskFinding(
            ruleId = "COMBO_ADMIN_NON_MGMT",
            title = text.title,
            severity = FindingSeverity.HIGH,
            points = points,
            category = RiskCategory.SPECIAL_ACCESS,
            evidence = "Device admin active/declared without device/profile owner management",
            explanation = text.explanation,
            confidence = FindingConfidence.MEDIUM,
            recommendation = text.recommendation
        )
    }

    fun ruleComboNotifNet(sig: SignalView): RiskFinding? {
        val notif = sig.specialMultiplier(SpecialAccessType.NOTIFICATION_LISTENER)
        val net = sig.permMultiplier("INTERNET")
        if (notif <= 0 || net <= 0) return null

        val lowestMult = minOf(notif, net)
        val points = (RiskPolicy.BASE_COMBO_NOTIF_NET * lowestMult).roundToInt()
        if (points <= 0) return null

        val text = Explanations.comboNotifNet(sig.appLabel)
        return RiskFinding(
            ruleId = "COMBO_NOTIF_NET",
            title = text.title,
            severity = FindingSeverity.HIGH,
            points = points,
            category = RiskCategory.SPECIAL_ACCESS,
            evidence = "Notification listener (${sig.specialState(SpecialAccessType.NOTIFICATION_LISTENER)}) + Internet access",
            explanation = text.explanation,
            confidence = FindingConfidence.MEDIUM,
            recommendation = text.recommendation
        )
    }

    fun ruleComboDropper(sig: SignalView): RiskFinding? {
        val installPkgs = sig.specialMultiplier(SpecialAccessType.REQUEST_INSTALL_PACKAGES)
        val a11y = sig.specialMultiplier(SpecialAccessType.ACCESSIBILITY_SERVICE)
        val overlay = sig.specialMultiplier(SpecialAccessType.SYSTEM_ALERT_WINDOW)
        val screenAccess = maxOf(a11y, overlay)

        if (installPkgs <= 0 || screenAccess <= 0 || !sig.isSideloaded) return null

        val lowestMult = minOf(installPkgs, screenAccess)
        val points = (RiskPolicy.BASE_COMBO_DROPPER * lowestMult).roundToInt()
        if (points <= 0) return null

        val text = Explanations.comboDropper(sig.appLabel)
        return RiskFinding(
            ruleId = "COMBO_DROPPER",
            title = text.title,
            severity = FindingSeverity.HIGH,
            points = points,
            category = RiskCategory.SPECIAL_ACCESS,
            evidence = "Install packages + Sideloaded origin + Screen access (accessibility or overlay)",
            explanation = text.explanation,
            confidence = FindingConfidence.MEDIUM,
            recommendation = text.recommendation
        )
    }

    fun ruleComboSurveillance(sig: SignalView): RiskFinding? {
        val camMic = maxOf(sig.permMultiplier("CAMERA"), sig.permMultiplier("RECORD_AUDIO"))
        val bgLoc = sig.permMultiplier("ACCESS_BACKGROUND_LOCATION")
        val net = sig.permMultiplier("INTERNET")
        val boot = sig.permMultiplier("RECEIVE_BOOT_COMPLETED")

        if (camMic <= 0 || bgLoc <= 0 || net <= 0 || boot <= 0) return null

        val lowestMult = minOf(camMic, bgLoc, net, boot)
        val points = (RiskPolicy.BASE_COMBO_SURVEILLANCE * lowestMult).roundToInt()
        if (points <= 0) return null

        val text = Explanations.comboSurveillance(sig.appLabel)
        return RiskFinding(
            ruleId = "COMBO_SURVEILLANCE",
            title = text.title,
            severity = FindingSeverity.HIGH,
            points = points,
            category = RiskCategory.LOCATION,
            evidence = "Camera/Audio + Background Location + Internet + Boot persistence",
            explanation = text.explanation,
            confidence = FindingConfidence.MEDIUM,
            recommendation = text.recommendation
        )
    }

    fun ruleComboHarvest(sig: SignalView): RiskFinding? {
        val contactsCalls = maxOf(sig.permMultiplier("READ_CONTACTS"), sig.permMultiplier("READ_CALL_LOG"))
        val sms = maxOf(sig.permMultiplier("READ_SMS"), sig.permMultiplier("RECEIVE_SMS"))
        val net = sig.permMultiplier("INTERNET")

        if (contactsCalls <= 0 || sms <= 0 || net <= 0) return null

        val lowestMult = minOf(contactsCalls, sms, net)
        val points = (RiskPolicy.BASE_COMBO_HARVEST * lowestMult).roundToInt()
        if (points <= 0) return null

        val text = Explanations.comboHarvest(sig.appLabel)
        return RiskFinding(
            ruleId = "COMBO_HARVEST",
            title = text.title,
            severity = FindingSeverity.HIGH,
            points = points,
            category = RiskCategory.CONTACTS_AND_CALLS,
            evidence = "Contacts/Call-log + SMS + Internet access",
            explanation = text.explanation,
            confidence = FindingConfidence.MEDIUM,
            recommendation = text.recommendation
        )
    }

    fun ruleCtxMismatch(sig: SignalView): RiskFinding? {
        if (sig.isSystemApp) return null

        val label = sig.appLabel.lowercase()
        val keywordMatch = RiskPolicy.CTX_MISMATCH_KEYWORDS.any { kw ->
            Regex("\\b${Regex.escape(kw)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(label)
        }
        if (!keywordMatch) return null

        val smsMult = maxOf(
            sig.permMultiplier("READ_SMS"),
            sig.permMultiplier("RECEIVE_SMS"),
            sig.permMultiplier("SEND_SMS"),
            sig.permMultiplier("RECEIVE_MMS"),
            sig.permMultiplier("RECEIVE_WAP_PUSH")
        )
        val contactsCallsMult = maxOf(
            sig.permMultiplier("READ_CONTACTS"),
            sig.permMultiplier("WRITE_CONTACTS"),
            sig.permMultiplier("READ_CALL_LOG"),
            sig.permMultiplier("WRITE_CALL_LOG"),
            sig.permMultiplier("PROCESS_OUTGOING_CALLS")
        )
        val a11yMult = sig.specialMultiplier(SpecialAccessType.ACCESSIBILITY_SERVICE)
        val adminMult = sig.specialMultiplier(SpecialAccessType.DEVICE_ADMIN)

        val maxTriggerMult = maxOf(smsMult, contactsCallsMult, a11yMult, adminMult)
        if (maxTriggerMult <= 0) return null

        val points = (RiskPolicy.BASE_CTX_MISMATCH * maxTriggerMult).roundToInt()
        if (points <= 0) return null

        val confidence = if (maxTriggerMult < 1.0) FindingConfidence.LOW else FindingConfidence.MEDIUM
        val text = Explanations.ctxMismatch(sig.appLabel)
        return RiskFinding(
            ruleId = "CTX_MISMATCH",
            title = text.title,
            severity = FindingSeverity.MEDIUM,
            points = points,
            category = RiskCategory.SPECIAL_ACCESS,
            evidence = "Utility app label matches mismatch keyword but holds sensitive permissions/access",
            explanation = text.explanation,
            confidence = confidence,
            recommendation = text.recommendation
        )
    }

    // -------------------------------------------------------------
    // 4. Provenance Rule (PROV_SIDELOADED)
    // -------------------------------------------------------------

    fun ruleProvSideloaded(sig: SignalView): RiskFinding? {
        if (!sig.isSideloaded || sig.isSystemApp) return null
        if (!sig.hasAnyGrantedDangerousOrSpecial()) return null

        val text = Explanations.provSideloaded(sig.appLabel)
        return RiskFinding(
            ruleId = "PROV_SIDELOADED",
            title = text.title,
            severity = FindingSeverity.MEDIUM,
            points = RiskPolicy.PTS_PROV_SIDELOADED,
            category = RiskCategory.SPECIAL_ACCESS,
            evidence = "Installed outside an official app store with active sensitive permissions",
            explanation = text.explanation,
            confidence = FindingConfidence.MEDIUM,
            recommendation = text.recommendation
        )
    }

    private fun permStateLabel(multiplier: Double): String = when {
        multiplier >= 1.0 -> "(allowed)"
        multiplier >= 0.5 -> "(couldn't check)"
        else -> "(denied)"
    }
}
