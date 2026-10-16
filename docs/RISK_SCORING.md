# Risk Scoring Engine Rubric & Methodology

## 1. Core Principles

The risk scoring engine is a **pure, deterministic Kotlin component**. Given an application's metadata, permission grants, and special-access state, it produces a reproducible integer risk score ranging from **0 to 100**.

1. **Highest Weight to Special Access**: In compliance with problem statement requirements, platform-level capabilities (Accessibility, Device Admin, Overlay, Package Install) represent the highest security leverage and receive the highest individual point allocations.
2. **Declared vs Granted Differentiation**: Declared permissions that are not currently granted receive minimal informational weighting (or 0), while active runtime grants receive full scoring weight.
3. **Combination Rules**: Correlated permissions that together enable an attack chain (e.g. Persistence + Sensitive Data Exfiltration + Network) generate compound findings with extra weighting.
4. **Deduplication**: Correlated or subsumed permissions (e.g. `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`) are deduplicated to prevent double-counting.
5. **Three-Reason Enforcement**: To prevent high-severity false alarms driven by a single noisy signal, **any app with a raw score $\ge 50$ (High or Critical) MUST have at least three distinct, evidence-backed findings**. If an app has fewer than three distinct findings, its final score is capped at **49 (Medium)**.
6. **Score Bounding**: The final accumulated score is strictly clamped to the interval $[0, 100]$.

---

## 2. Risk Bands

| Band | Score Range | Description & Action |
|---|---|---|
| **LOW** | 0 – 24 | Standard application with minimal or expected permissions. Routine monitoring. |
| **MEDIUM** | 25 – 49 | Elevated access (e.g. Camera/Mic, Background Persistence, or ungranted high permissions). Review recommended. |
| **HIGH** | 50 – 74 | Significant attack surface or high-risk combination (e.g. Accessibility + Network, Spyware pattern). Must have $\ge 3$ distinct reasons. Prompt user review. |
| **CRITICAL** | 75 – 100 | Severe exposure (e.g. Active Device Admin with Sideloaded origin, Overlay Hijacker with SMS access, or multiple malicious combination patterns). Must have $\ge 3$ distinct reasons. Immediate action advised. |

---

## 3. Weighting Matrix

### A. Special Access (Highest Weight)

| Special Access Check | State | Points | Rule ID | Confidence |
|---|---|---|---|---|
| **Accessibility Service** | `ENABLED` | +25 | `RULE_SPECIAL_ACCESSIBILITY_ENABLED` | HIGH |
| **Accessibility Service** | `DECLARED` (disabled) | +5 | `RULE_SPECIAL_ACCESSIBILITY_DECLARED` | MEDIUM |
| **Active Device Admin** | `ENABLED` | +30 | `RULE_SPECIAL_DEVICE_ADMIN_ACTIVE` | HIGH |
| **Active Device Admin** | `DECLARED` (inactive) | +5 | `RULE_SPECIAL_DEVICE_ADMIN_DECLARED` | MEDIUM |
| **System Overlay (`SYSTEM_ALERT_WINDOW`)** | `GRANTED` | +18 | `RULE_SPECIAL_OVERLAY_GRANTED` | HIGH |
| **System Overlay** | `DECLARED` (not granted) | +4 | `RULE_SPECIAL_OVERLAY_DECLARED` | MEDIUM |
| **Notification Listener** | `ENABLED` | +20 | `RULE_SPECIAL_NOTIFICATION_LISTENER_ENABLED`| HIGH |
| **Notification Listener** | `DECLARED` (disabled) | +4 | `RULE_SPECIAL_NOTIFICATION_LISTENER_DECLARED`| MEDIUM |
| **Request Install Packages** | `GRANTED` | +18 | `RULE_SPECIAL_INSTALL_PACKAGES_GRANTED` | HIGH |
| **Request Install Packages** | `DECLARED` (not granted) | +4 | `RULE_SPECIAL_INSTALL_PACKAGES_DECLARED` | MEDIUM |
| **Usage Stats Access** | `GRANTED` | +12 | `RULE_SPECIAL_USAGE_STATS_GRANTED` | HIGH |

---

### B. Standard Permission Categories (Runtime Granted)

| Category | Typical Permissions | Points per Grant (Max per Category) | Rule ID Prefix |
|---|---|---|---|
| **SMS** | `SEND_SMS`, `RECEIVE_SMS`, `READ_SMS`, `RECEIVE_MMS` | +8 per granted perm (Max: 16) | `RULE_PERM_SMS_` |
| **Contacts & Call Logs** | `READ_CONTACTS`, `WRITE_CONTACTS`, `READ_CALL_LOG` | +6 per granted perm (Max: 14) | `RULE_PERM_CONTACTS_` |
| **Camera & Microphone** | `CAMERA`, `RECORD_AUDIO` | +6 per granted perm (Max: 12) | `RULE_PERM_MEDIA_` |
| **Location** | `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` | +6 per granted perm (Max: 12) | `RULE_PERM_LOC_` |
| **Phone & Identity** | `READ_PHONE_STATE`, `CALL_PHONE`, `READ_PHONE_NUMBERS` | +5 per granted perm (Max: 10) | `RULE_PERM_PHONE_` |
| **Storage & Media** | `MANAGE_EXTERNAL_STORAGE`, `READ_EXTERNAL_STORAGE` | +5 per granted perm (Max: 10) | `RULE_PERM_STORAGE_` |
| **Background & Persistence**| `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE` | +4 per granted perm (Max: 8) | `RULE_PERM_BG_` |
| **Network** | `INTERNET`, `ACCESS_NETWORK_STATE` | +2 per granted perm (Max: 4) | `RULE_PERM_NET_` |

*Note: Declared-only permissions (denied or not granted) receive a nominal +1 point for visibility, capped at +3 per category.*

---

## 4. Compound Combination Rules

Combination rules fire when mutually reinforcing capabilities are present:

1. **`RULE_COMBO_SPYWARE` (+20 points)**:
   - *Condition*: (Background Persistence granted) AND (SMS OR Contacts OR Call Logs OR Location granted) AND (Internet declared/granted).
   - *Explanation*: App can start automatically at boot or run persistently in the background while harvesting sensitive personal data and transmitting it over the network.
2. **`RULE_COMBO_TOLL_FRAUD` (+18 points)**:
   - *Condition*: (`SEND_SMS` granted) AND (`INTERNET` granted/declared) AND (Background Persistence granted).
   - *Explanation*: App has capability to silently dispatch premium SMS messages and communicate with remote command-and-control servers.
3. **`RULE_COMBO_AUDIO_SURVEILLANCE` (+16 points)**:
   - *Condition*: (`RECORD_AUDIO` granted) AND (Background Persistence granted) AND (`INTERNET` granted/declared).
   - *Explanation*: App can capture ambient microphone audio in the background and transmit recordings over the network.
4. **`RULE_COMBO_OVERLAY_STEALER` (+22 points)**:
   - *Condition*: (System Overlay granted/enabled) AND (Accessibility enabled OR Notification Listener enabled OR SMS/Contacts granted).
   - *Explanation*: App can draw over other apps (tapjacking/phishing) while intercepting keystrokes, notifications (2FA tokens), or credential inputs.
5. **`RULE_COMBO_MALICIOUS_DROPPER` (+20 points)**:
   - *Condition*: (Request Install Packages granted) AND (`INTERNET` granted/declared) AND (Storage or Persistence granted).
   - *Explanation*: App can download arbitrary APK payloads from the internet and initiate silent or deceptive package installations.
6. **`RULE_COMBO_RANSOMWARE_PATTERN` (+25 points)**:
   - *Condition*: (`MANAGE_EXTERNAL_STORAGE` granted) AND (Active Device Admin OR Overlay) AND (Persistence granted).
   - *Explanation*: Broad file system access combined with device administrator or screen-locking capabilities resembles ransomware behavior.
7. **`RULE_HEURISTIC_SIDELOADED_HIGH_RISK` (+15 points)**:
   - *Condition*: App installer source is NOT Google Play Store (null, package installer, or unknown) AND App has at least 2 high-risk permissions or special access.
   - *Explanation*: Sideloaded application bypassing official app store verification possessing high-privilege access.
8. **`RULE_HEURISTIC_OUTDATED_TARGET_SDK` (+10 points)**:
   - *Condition*: App targets SDK $< 29$ (Android 9 or earlier).
   - *Explanation*: App targets an obsolete Android version, allowing it to bypass modern runtime permission enforcement and storage sandboxing.

---

## 5. Deduplication and Correlated Evidence

- **Location deduplication**: If `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` are both granted, only `ACCESS_FINE_LOCATION` generates a weighted finding; coarse location is collapsed into the same evidence string.
- **Storage deduplication**: If `MANAGE_EXTERNAL_STORAGE` is granted, legacy `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` findings are consolidated.

---

## 6. Three-Reason Enforcement Algorithm

```kotlin
fun finalizeAppRisk(rawScore: Int, findings: List<RiskFinding>): Pair<Int, RiskLevel> {
    val clampedScore = rawScore.coerceIn(0, 100)
    val distinctEvidenceCount = findings.map { it.ruleId }.distinct().size

    // If score falls into High (50-74) or Critical (75-100), verify at least 3 distinct findings
    val finalScore = if (clampedScore >= 50 && distinctEvidenceCount < 3) {
        // Cap at upper boundary of Medium band
        49
    } else {
        clampedScore
    }

    val finalLevel = when {
        finalScore >= 75 -> RiskLevel.CRITICAL
        finalScore >= 50 -> RiskLevel.HIGH
        finalScore >= 25 -> RiskLevel.MEDIUM
        else -> RiskLevel.LOW
    }

    return Pair(finalScore, finalLevel)
}
```
This guarantees zero unexplainable High or Critical ratings.
