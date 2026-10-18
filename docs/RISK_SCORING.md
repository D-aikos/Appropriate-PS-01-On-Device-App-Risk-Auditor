# PS-01 On-Device App Risk Auditor: Scoring Policy & Rubric

This document defines the deterministic scoring policy implemented in `com.appropriate.riskauditor.core.rules`. It is completely on-device, offline, and self-contained.

---

## 1. Multipliers (Signal Verification State)

The multiplier reflects certainty that a capability is active and accessible:

| State of the Signal | Multiplier |
|---|---|
| **GRANTED / ENABLED** | 1.00 |
| **NOT_RUNTIME** (normal/install-time permission, auto-granted) | 1.00 |
| **UNKNOWN** (could not inspect/verify) | 0.50 |
| **DECLARED only** (DENIED / DISABLED) | 0.30 |
| **NOT_DECLARED / NOT_SUPPORTED** | 0.00 |

---

## 2. Single-Signal Points (Base Multiplier 1.0)

Special access permissions are evaluated directly via `SA_*` rules and never double-counted in category findings.
Category finding points = `min(cap, round(Σ weight × multiplier))`. No finding is generated if rounded points equal 0.

| Rule ID | Signal | Points | Category Cap |
|---|---|---|---|
| `SA_ACCESSIBILITY` | Accessibility service | 30 | — |
| `SA_DEVICE_ADMIN` | Device admin (0 if device/profile owner) | 25 | — |
| `SA_NOTIF_LISTENER` | Notification listener | 22 | — |
| `SA_OVERLAY` | Draw over other apps (`SYSTEM_ALERT_WINDOW`) | 18 | — |
| `SA_INSTALL_PACKAGES` | Install unknown apps (`REQUEST_INSTALL_PACKAGES`) | 16 | — |
| `SA_USAGE_STATS` | Usage access (`PACKAGE_USAGE_STATS`) | 10 | — |
| `CAT_SMS` | READ_SMS 14, RECEIVE_SMS 10, SEND_SMS 12, RECEIVE_MMS 3, RECEIVE_WAP_PUSH 3 | Σ | 30 |
| `CAT_CONTACTS_CALLS` | READ_CONTACTS 8, WRITE_CONTACTS 4, READ_CALL_LOG 10, WRITE_CALL_LOG 6, PROCESS_OUTGOING_CALLS 6 | Σ | 18 |
| `CAT_CAMERA_MIC` | CAMERA 7, RECORD_AUDIO 9 | Σ | 16 |
| `CAT_LOCATION` | COARSE 3, FINE 6, BACKGROUND 8 | Σ | 14 |
| `CAT_PHONE_IDENTITY` | READ_PHONE_STATE 4, READ_PHONE_NUMBERS 4, CALL_PHONE 5, ANSWER_PHONE_CALLS 3, GET_ACCOUNTS 3 | Σ | 10 |
| `CAT_STORAGE_MEDIA` | MANAGE_EXTERNAL_STORAGE 12, READ_MEDIA_IMAGES 5, READ_MEDIA_VIDEO 5, READ/WRITE_EXTERNAL_STORAGE 5 (once), READ_MEDIA_AUDIO 2, VISUAL_USER_SELECTED 2 | Σ | 14 |
| `CAT_BACKGROUND` | RECEIVE_BOOT_COMPLETED 3, FOREGROUND_SERVICE* 2, IGNORE_BATTERY_OPT 4, WAKE_LOCK 1, SCHEDULE_EXACT_ALARM 1, USE_FULL_SCREEN_INTENT 3 | Σ | 10 |
| (network) | `INTERNET` / `ACCESS_NETWORK_STATE` | **0 alone**; precondition for combinations | — |

---

## 3. Combination Rules

Bonus points = `round(base × lowest multiplier among required signals)`. Each fires at most once.

| Rule ID | Requires | Base | Confidence | Heuristic | Action |
|---|---|---|---|---|---|
| `COMBO_ACCESS_OVERLAY` | accessibility + overlay | 25 | High | no | `OPEN_ACCESSIBILITY_SETTINGS` |
| `COMBO_SMS_NET_BOOT_SIDELOAD` | (READ_SMS or RECEIVE_SMS) + INTERNET + RECEIVE_BOOT_COMPLETED + install source SIDELOADED. (If source is UNKNOWN: base 15, confidence Medium) | 25 | High | no | `UNINSTALL` |
| `COMBO_ADMIN_NON_MGMT` | active/declared device admin, not device/profile owner, package not in MDM allowlist | 18 | Medium | **yes** | `OPEN_DEVICE_ADMIN_SETTINGS` |
| `COMBO_NOTIF_NET` | notification listener + INTERNET | 12 | Medium | no | `OPEN_NOTIFICATION_LISTENER_SETTINGS` |
| `COMBO_DROPPER` | REQUEST_INSTALL_PACKAGES + SIDELOADED + (accessibility or overlay) | 15 | Medium | no | `OPEN_INSTALL_SOURCES_SETTINGS` |
| `COMBO_SURVEILLANCE` | (CAMERA or RECORD_AUDIO) + ACCESS_BACKGROUND_LOCATION + INTERNET + RECEIVE_BOOT_COMPLETED | 12 | Medium | no | `OPEN_APP_SETTINGS` |
| `COMBO_HARVEST` | (READ_CONTACTS or READ_CALL_LOG) + (READ_SMS or RECEIVE_SMS) + INTERNET | 10 | Medium | no | `OPEN_APP_SETTINGS` |
| `CTX_MISMATCH` | label whole-word matches {torch, flashlight, calculator, wallpaper, ringtone, compass, qr, barcode, ruler, level}, not system app, AND holds SMS, contacts, accessibility, or admin | 15 | Low (if declared only), Medium otherwise | **yes** | `OPEN_APP_SETTINGS` |

### Provenance Finding (`PROV_SIDELOADED`)
- **Points:** +8 (Medium confidence, action `OPEN_INSTALL_SOURCES_SETTINGS`).
- **Condition:** Installed outside a store (`SIDELOADED`), not a system app, and holds at least one GRANTED dangerous permission or ENABLED special access. Source UNKNOWN gives 0. Store apps receive no discount (no negative points).

---

## 4. Final Score, Risk Bands, and Three-Reason Rule

- **Final Score:** $\text{clamp}(\sum \text{finding points}, 0, 100)$.
- **Four Risk Bands:**
  - **LOW:** 0 – 24
  - **MEDIUM:** 25 – 49
  - **HIGH:** 50 – 74
  - **CRITICAL:** 75 – 100
- **Three-Reason Enforcement Policy:**
  If the raw score would fall into High (50–74) or Critical (75–100), but fewer than 3 distinct evidence-backed findings (with points > 0) are present, the final score is strictly capped at **49 (Medium)**, and `cappedForEvidence = true`. Findings are never fabricated to reach 3.
- **Sorting:** Findings are sorted deterministically by points descending, then `ruleId` ascending.

---

## 5. Worked Examples

### Example 1 (Vector T2 — Store App with Accessibility Enabled)
- **Input:** Store installed (`com.android.vending`), Accessibility Service `ENABLED`.
- **Calculations:**
  - `SA_ACCESSIBILITY`: $30 \times 1.0 = 30$ points.
- **Score:** 30 $\to$ **MEDIUM**, 1 finding, `cappedForEvidence = false` (score is in Medium band, capping rule only applies to raw scores $\ge 50$).

### Example 2 (Vector T3 — Sideloaded App with Accessibility + Overlay Enabled)
- **Input:** Sideloaded origin, Accessibility Service `ENABLED`, System Overlay `ENABLED`.
- **Calculations:**
  - `SA_ACCESSIBILITY`: $30 \times 1.0 = 30$ points.
  - `SA_OVERLAY`: $18 \times 1.0 = 18$ points.
  - `COMBO_ACCESS_OVERLAY`: $\text{round}(25 \times \min(1.0, 1.0)) = 25$ points.
  - `PROV_SIDELOADED`: Sideloaded + active special access = 8 points.
- **Score:** $30 + 18 + 25 + 8 = \mathbf{81}$ $\to$ **CRITICAL**, 4 distinct findings.

### Example 3 (Vector T6 — Store Messenger App with Broad Runtime Permissions)
- **Input:** Store installed, granted `READ_CONTACTS`, `CAMERA`, `RECORD_AUDIO`, `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`, `READ_PHONE_STATE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `READ_MEDIA_IMAGES`.
- **Calculations:**
  - `CAT_CONTACTS_CALLS`: $8 \times 1.0 = 8$ points.
  - `CAT_CAMERA_MIC`: $7 + 9 = 16$ (capped at 16) points.
  - `CAT_LOCATION`: $3 + 6 = 9$ points.
  - `CAT_PHONE_IDENTITY`: $4 \times 1.0 = 4$ points.
  - `CAT_BACKGROUND`: $3 + 2 = 5$ points.
  - `CAT_STORAGE_MEDIA`: $5 \times 1.0 = 5$ points.
- **Score:** $8 + 16 + 9 + 4 + 5 + 5 = \mathbf{47}$ $\to$ **MEDIUM**, 6 distinct findings. Demonstrates that legitimate multi-featured store apps remain in Medium risk without unwarranted alert fatigue.

---

## 6. What This Score Is Not

**Permissions alone do not prove an application is malicious.** This score measures structural capability leverage, sensitive exposure, and attack-surface combinations on this specific device. It never asserts definitive criminal intent or replaces platform security sandboxing.
