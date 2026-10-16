# On-Device App Risk Auditor (PS-01)

> **Repository:** `Appropriate-PS-01-On-Device-App-Risk-Auditor`  
> **Application ID:** `com.appropriate.riskauditor`  
> **Platform:** Native Android (Kotlin, Jetpack Compose, Room, Android Platform APIs)

A privacy-preserving, 100% on-device Android security application that audits installed application metadata, declared vs. granted permissions, and platform-level special access states, producing deterministic, explainable risk scores and executive PDF reports without any cloud or network communication.

---

## Key Security Features

1. **100% On-Device & Offline-First**:
   - Zero network permissions declared (`android.permission.INTERNET` is completely omitted).
   - All package inventory auditing, permission resolution, risk scoring, local history, and PDF report generation occur strictly locally.
   - No data, package names, hashes, or telemetry leave the user's device.
2. **No Root or ADB Required**:
   - Built entirely on public, supported Android APIs (`PackageManager`, `DevicePolicyManager`, `Settings.Secure`, `AppOpsManager`).
   - Adheres strictly to Android sandboxing and user-consent security boundaries.
3. **Honest Visibility**:
   - Declares `QUERY_ALL_PACKAGES` strictly for the security auditing use case.
   - Distinguishes observable states from unobservable ones. Unobservable or restricted states default to `UNKNOWN` or `NOT_SUPPORTED`, never false negatives.
4. **Declared vs. Granted Differentiation**:
   - Distinguishes manifest declarations (`requestedPermissions`) from runtime user grants (`REQUESTED_PERMISSION_GRANTED`).
5. **Nine Mandatory Problem-Statement Categories**:
   - Special Access (Highest Weight)
   - SMS
   - Contacts & Call Logs
   - Camera & Microphone
   - Location
   - Phone & Identity
   - Storage & Media
   - Background & Persistence
   - Network
6. **Platform Special Access Checks**:
   - Actively enabled Accessibility Services (`Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`)
   - Active Device Administrator privileges (`DevicePolicyManager.isAdminActive`)
   - Enabled Notification Listeners (`Settings.Secure.enabled_notification_listeners`)
   - System Alert Window Overlay capability (`Settings.canDrawOverlays` & AppOps)
   - Install Unknown Packages capability (`canRequestPackageInstalls` & AppOps)
   - Device Usage Access (`AppOpsManager.OPSTR_GET_USAGE_STATS`)
7. **Explainable Deterministic Risk Engine (0–100)**:
   - Four discrete risk bands: **Low** (0–24), **Medium** (25–49), **High** (50–74), and **Critical** (75–100).
   - **Three-Reason Enforcement Policy**: Any application scoring in the High or Critical band must be supported by at least 3 distinct, evidence-backed findings; otherwise, its score is capped at Medium (49) to eliminate unexplainable false alarms.
   - Detects compound multi-vector attack patterns: Spyware (Boot + Sensitive Data + Internet), Toll Fraud, Audio Surveillance, Overlay Stealer / Phishing, Malicious Dropper, Ransomware pattern, Sideloading context, and Legacy Target SDK (< 29).
8. **App Detail & Safe Actions**:
   - Comprehensive "Why Flagged" breakdown with Rule ID, severity badge, points, evidence strings, and actionable recommendations.
   - Full categorized permissions matrix.
   - Safe system shortcuts: Direct links to Android App Settings (`ACTION_APPLICATION_DETAILS_SETTINGS`) and safe user-mediated uninstallation (`ACTION_DELETE`). The app never silently modifies another app's state.
9. **Local Audit History in Room**:
   - Local SQLite persistence via Android Room.
   - Stores audit summaries, timestamps, app counts, and risk distribution trends.
10. **Executive PDF Export**:
    - Native on-device PDF generation via Android `PdfDocument`.
    - Text sanitization and boundary checking to prevent canvas corruption.
    - Secure sharing via `FileProvider` and `Intent.ACTION_SEND`.

---

## Architecture & Directory Layout

```
Appropriate-PS-01-On-Device-App-Risk-Auditor/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/appropriate/riskauditor/
│       │   │   ├── MainActivity.kt               # Compose Navigation Entry
│       │   │   ├── RiskAuditorApp.kt             # Application & Singletons
│       │   │   ├── app/                          # Navigation & Theme
│       │   │   ├── core/model/                   # Pure Kotlin Domain Models
│       │   │   ├── core/platform/                # Android Platform Adapters
│       │   │   ├── core/rules/                   # Deterministic Scoring Engine
│       │   │   ├── core/scanner/                 # Inventory & Scan Orchestrator
│       │   │   ├── core/storage/                 # Room Database, DAO & Repository
│       │   │   ├── core/report/                  # PDF Generator & FileProvider Exporter
│       │   │   └── feature/
│       │   │       ├── dashboard/                # Device Posture & Risk Overview
│       │   │       ├── apps/                     # App List & Detail Screens
│       │   │       ├── history/                  # Scan History Timeline
│       │   │       └── settings/                 # System Apps Toggle & Policy
│       │   └── res/                              # Resources & Vector Icons
│       └── test/java/com/appropriate/riskauditor/ # Comprehensive Unit Tests
├── docs/
│   ├── PROBLEM_STATEMENT_MAPPING.md              # Requirement-to-Code Traceability
│   ├── ARCHITECTURE.md                           # System Layers & Data Flow
│   ├── SECURITY_MODEL.md                         # Zero-Trust Threat Model & Mitigations
│   ├── RISK_SCORING.md                           # Scoring Rubric & 3-Reason Algorithm
│   └── IMPLEMENTATION_PLAN.md                    # Team Ownership & Delivery Plan
├── prompts/
│   └── MASTER_PROMPT.md                          # Hackathon Master Prompt
├── build.gradle.kts                              # Root Gradle Build Script
├── settings.gradle.kts                           # Module Configuration
└── gradlew / gradlew.bat                         # Gradle Wrapper Scripts
```

---

## Building and Running

### In Android Studio:
1. Open Android Studio.
2. Select **Open** and choose the `Appropriate-PS-01-On-Device-App-Risk-Auditor` root directory.
3. Allow Gradle to sync dependencies.
4. Select target device or emulator (Android 8.0 Oreo / API 26 or higher recommended).
5. Click **Run 'app'**.

### From Command Line:
```bash
# Run all unit tests
./gradlew test

# Build debug APK
./gradlew assembleDebug
```
The compiled APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Documentation References

- [Problem Statement Mapping](docs/PROBLEM_STATEMENT_MAPPING.md)
- [Architecture & Data Flow](docs/ARCHITECTURE.md)
- [Security Model & Threat Mitigations](docs/SECURITY_MODEL.md)
- [Risk Scoring Rubric & Methodology](docs/RISK_SCORING.md)
