# Security Model --- On-Device App Risk Auditor

## 1. Security Architecture & Threat Model

The **On-Device App Risk Auditor** operates under a zero-trust, privacy-first threat model. Because it assesses other installed applications, it must maintain rigorous internal defenses against tampering, information leakage, and misrepresentation.

---

## 2. Core Security Assets

1. **User Privacy & Device State**:
   - The application audits app metadata and permission grants only.
   - It **NEVER** inspects, reads, or parses private user communications (SMS text bodies, contact cards, call logs, photos, media contents, or notification payloads).
2. **Audit Integrity**:
   - Deterministic risk scoring algorithms that produce identical scores across executions on identical inputs.
   - Transparent, evidence-backed explanations for every risk flag.
3. **Local Scan History**:
   - Stored in a sandboxed, private SQLite database via Room with zero external exposure.

---

## 3. Trust Boundaries

```
[ External / Uncontrolled ]
Installed 3rd-Party Apps & Sideloaded APKs
          │
          │ Read-only metadata via Android Platform APIs
          ▼
[ Android OS Sandboxed Framework ]
PackageManager │ DevicePolicyManager │ Settings.Secure │ AppOpsManager
          │
          │ Normalization through platform adapters
          ▼
[ App Boundary: com.appropriate.riskauditor ]
  ├── Scanner & Normalizer (Explicit Unknown States)
  ├── Pure Kotlin Rules Engine (Deterministic Scoring)
  ├── Room Database (Private App Storage: /data/data/...)
  └── PDF Report Generator (Android PdfDocument)
          │
          │ Read-Only Content URI via FileProvider
          ▼
[ System Share Sheet / External Document Viewer ]
```

---

## 4. Threat Mitigation Matrix

| Threat | Description | Mitigation in Implementation |
|---|---|---|
| **Data Exfiltration / Cloud Leak** | Malicious inspection tools transmitting user device inventory or behavioral metadata to remote servers. | **Zero Network Access**: The app completely omits the `android.permission.INTERNET` permission from `AndroidManifest.xml`. No HTTP client, telemetry SDK, or remote analytics exists in the codebase. |
| **False Sense of Security** | Labeling unobservable platform signals as "safe" or "false" when the OS restricts access. | **Explicit State Enums**: States default to `UNKNOWN` or `NOT_SUPPORTED` when APIs are restricted or null. The UI explicitly alerts the user to unobservable states. |
| **False Positives / Panic Induction** | Brandishing normal permissions (e.g. Camera in a video app) as "malware". | **Evidence & Heuristic Transparency**: Findings are strictly classified by confidence (`LOW`, `MEDIUM`, `HIGH`) and labeled as heuristic indicators. Remediation steps instruct users to review in Android Settings. |
| **Unintended Privilege Escalation** | Rogue apps attempting to invoke internal auditor components via exported activities or services. | **Zero Exported Attack Surface**: All components (`AppDatabase`, internal services, ViewModels) are unexported. `MainActivity` is the only exported activity with explicit `android:exported="true"` for the launcher filter. |
| **Unauthorized Action Execution** | Silently revoking permissions, disabling apps, or uninstalling without user consent. | **No Silent Mutations**: The app uses only user-mediated Android intents (`ACTION_APPLICATION_DETAILS_SETTINGS` and `ACTION_DELETE`). The system UI always prompts the user for final confirmation. |
| **Report Injection / Layout Overflow** | An app with a malicious package name or label attempting to exploit PDF generation or inject malformed canvas instructions. | **Text Sanitization & Bounds Checking**: `PdfReportGenerator` sanitizes non-printable characters, strictly bounds string lengths, and wraps lines gracefully. |
| **Insecure File Sharing** | Exposing reports via public world-readable storage. | **Android FileProvider**: PDF reports are saved to `context.cacheDir/reports` and shared using `androidx.core.content.FileProvider` with temporary `FLAG_GRANT_READ_URI_PERMISSION`. |

---

## 5. Android Permissions & Policy Compliance

### Manifest Permissions Declared:
1. `android.permission.QUERY_ALL_PACKAGES`:
   - **Justification**: As a dedicated on-device security auditor, the app must enumerate all installed packages to evaluate system risk.
   - **Google Play Policy Note**: Under Google Play's Package Visibility policy, `QUERY_ALL_PACKAGES` is restricted to antivirus, security, and device-management apps. For public release on Google Play, a declaration form specifying this security auditing core functionality must be submitted.

### Permissions NOT Requested (Least Privilege):
- **NO `android.permission.INTERNET`**: Complete offline assurance.
- **NO `android.permission.READ_SMS` / `READ_CONTACTS` / `READ_CALL_LOG`**: We audit whether *other* apps have these permissions, but the auditor itself requires none.
- **NO Root or SU binaries**: Adheres strictly to the standard Android security architecture.
