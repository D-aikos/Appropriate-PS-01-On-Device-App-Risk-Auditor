# Problem Statement Mapping --- PS-01: On-Device App Risk Auditor

This document provides a comprehensive, requirement-by-requirement mapping from the **PS-01 Problem Statement** to its corresponding architecture, code packages, classes, and verification tests.

---

## 1. Traceability Matrix

| # | Requirement | Specification & Constraints | Implementation Component / File | Verification & Test |
|---|---|---|---|---|
| **R1** | **On-Device & Offline-First** | All inventory collection, permission auditing, risk scoring, history, and reporting must operate purely locally on the device without network connectivity, cloud APIs, telemetry, or remote LLMs. | `AndroidManifest.xml` (No `INTERNET` permission), `core/rules/`, `core/scanner/`, `core/report/` | Airplane mode test; Manifest audit for zero network permissions; Unit tests run offline. |
| **R2** | **No Root / No End-User ADB** | Use only supported public Android platform APIs (`PackageManager`, `DevicePolicyManager`, `Settings.Secure`, `AppOpsManager`). No sandboxing escapes or root shell execution. | `core/platform/AndroidPackageManagerAdapter.kt`, `core/platform/SpecialAccessAdapter.kt` | Tested on standard non-rooted emulator & physical Android device. |
| **R3** | **Honest Visibility & States** | Declare `QUERY_ALL_PACKAGES` strictly for security audit purposes. Never fabricate data or collapse unobservable states to `false`. Represent missing signals explicitly as `UNKNOWN` or `NOT_SUPPORTED`. | `core/model/GrantState.kt`, `core/model/SpecialAccessState.kt`, `core/platform/` | Unit tests verifying that inaccessible properties return `UNKNOWN` / `NOT_SUPPORTED` instead of `false`. |
| **R4** | **Declared vs. Granted Permissions** | Distinguish manifest-declared permissions (`requestedPermissions`) from actual runtime grants (`REQUESTED_PERMISSION_GRANTED` or `checkPermission`) and special access grants. | `core/model/PermissionAudit.kt`, `core/platform/AndroidPackageManagerAdapter.kt` | Unit tests with mock `PackageInfo` containing declared-but-revoked permissions. |
| **R5** | **Nine Required Categories** | Categorize permissions into: (1) Special Access (highest weight), (2) SMS, (3) Contacts & Call Logs, (4) Camera & Microphone, (5) Location, (6) Phone & Identity, (7) Storage & Media, (8) Background & Persistence, (9) Network. | `core/model/RiskCategory.kt`, `core/rules/CategoryMapper.kt` | `CategoryMapperTest.kt` verifying exhaustive mapping of dangerous and normal permissions to the 9 categories. |
| **R6** | **Required App Inventory** | Collect: App label, icon (where available), package name, version name, version code, target SDK, install timestamp, update timestamp, installer package source, and system app flag (`FLAG_SYSTEM`). | `core/model/AppInventoryRecord.kt`, `core/scanner/AppScanner.kt` | `AppScannerTest.kt` verifying normalization of install timestamps, target SDK, and installer package. |
| **R7** | **System Apps Toggle** | Allow user to filter inventory to include or exclude pre-installed system apps (`FLAG_SYSTEM`). | `feature/settings/SettingsPreferences.kt`, `feature/apps/AppListViewModel.kt` | UI toggle state test and scanner filtering tests. |
| **R8** | **Special Access Checks** | Audit: (1) Accessibility services (`ENABLED_ACCESSIBILITY_SERVICES`), (2) Device administrators (`DevicePolicyManager.isAdminActive`), (3) Notification listeners (`ENABLED_NOTIFICATION_LISTENERS`), (4) Overlay (`SYSTEM_ALERT_WINDOW` & `canDrawOverlays`), (5) Package install capability (`REQUEST_INSTALL_PACKAGES` & `canRequestPackageInstalls`), (6) Usage stats (`OPSTR_GET_USAGE_STATS`). | `core/platform/SpecialAccessAdapter.kt`, `core/model/SpecialAccessAudit.kt` | `SpecialAccessAdapterTest.kt` with synthetic platform states. |
| **R9** | **Explainable Risk Score (0-100)** | Deterministic local score with 4 discrete bands: Low (0–24), Medium (25–49), High (50–74), Critical (75–100). Clear mathematical rubric, deduplication of correlated permissions, and combination rules. | `core/rules/RiskScoringEngine.kt`, `core/rules/RiskPolicy.kt` | `RiskScoringEngineTest.kt` checking boundary values (0, 24, 25, 49, 50, 74, 75, 100) and reproducibility. |
| **R10** | **Three-Reason Enforcement** | Every High (50–74) or Critical (75–100) risk score MUST be backed by at least three distinct, evidence-based findings. If fewer than 3 reasons exist, cap severity at Medium. | `core/rules/RiskScoringEngine.kt` (`enforceThreeReasonPolicy`) | `RiskScoringEngineTest.kt` verifying that apps with high points but <3 distinct reasons are capped at Medium. |
| **R11** | **Combination Rules & Heuristics** | Detect high-risk combinations: Spyware pattern (Persistence + Sensitive Data + Network), Dropper (Install Packages + Network + Storage), Audio Surveillance (Microphone + Background + Network), Screen Hijack / Phishing (Accessibility / Overlay + Sensitive Access), Sideloading risk, Outdated SDK bypass. | `core/rules/CombinationRules.kt` | `CombinationRulesTest.kt` with synthetic profiles (e.g. Spyware, Dropper, Fake Flash Player). |
| **R12** | **App Detail Screen & "Why Flagged"** | Detailed view presenting: Risk score badge, 3+ evidence reasons in "Why Flagged", category breakdown, declared vs granted list, and special access status. | `feature/apps/AppDetailScreen.kt`, `feature/apps/AppDetailViewModel.kt` | Compose UI tests verifying display of findings, evidence strings, and recommendations. |
| **R13** | **Safe System Settings & Uninstall Links** | Provide direct shortcuts: (1) App details/settings (`ACTION_APPLICATION_DETAILS_SETTINGS`), (2) Safe uninstall intent (`ACTION_DELETE` / `ACTION_UNINSTALL_PACKAGE`). Never silently modify or uninstall. | `core/platform/SystemNavigator.kt`, `feature/apps/AppDetailScreen.kt` | Intent creation verification unit tests; verifying explicit user consent requirement. |
| **R14** | **Local Scan History in Room** | Store scan summaries, timestamped scan records, app risk totals, and finding counts in local SQLite via Room. No sensitive telemetry or full inventories persisted. | `core/storage/AppDatabase.kt`, `core/storage/ScanHistoryDao.kt`, `core/storage/ScanHistoryRepository.kt` | Room DAO unit tests testing persistence, retrieval, and cascade deletion. |
| **R15** | **Executive PDF Report Export** | Generate professional PDF audit report on-device using Android `PdfDocument`. Safely share via `FileProvider` and `ACTION_SEND`. Sanitize and escape all text to prevent layout abuse. | `core/report/PdfReportGenerator.kt`, `core/report/ReportExporter.kt`, `res/xml/file_paths.xml` | PDF generation test verifying file creation, non-zero byte size, text sanitization, and FileProvider URI generation. |
| **R16** | **Secure App Defaults** | No exported activities/services except `MainActivity`. Explicit `android:exported`. Immutable PendingIntents. Safe FileProvider path configuration. No cleartext traffic. Zero hardcoded credentials. | `AndroidManifest.xml`, `res/xml/file_paths.xml` | Android Lint security checks, Manifest review test. |

---

## 2. Verification Strategy

1. **Unit Tests (Pure Kotlin / JUnit 4)**:
   - `core/rules/RiskScoringEngineTest.kt`: Validates deterministic scoring, banding, point capping at 100, deduplication, and 3-reason enforcement.
   - `core/rules/CategoryMapperTest.kt`: Validates mapping of all platform permissions to the 9 problem-statement categories.
   - `core/rules/CombinationRulesTest.kt`: Validates specific threat patterns (Spyware, Dropper, Audio Surveillance, Overlay Stealer).
   - `core/platform/PermissionAuditTest.kt`: Validates resolution of `GrantState` (`GRANTED`, `DENIED`, `NOT_RUNTIME`, `UNKNOWN`).
   - `core/storage/ScanHistoryRepositoryTest.kt`: Validates Room entity mapping and scan aggregations.
   - `core/report/PdfSanitizerTest.kt`: Validates string truncation, layout escape, and null-safety during PDF rendering.

2. **Integration & Platform Tests**:
   - Platform adapter verification on real or emulated Android 8.0+ (API 26 to API 35).
   - Package manager query tests with `QUERY_ALL_PACKAGES`.
   - FileProvider document sharing with external viewer applications.
