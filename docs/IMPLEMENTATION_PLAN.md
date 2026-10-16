# PS-01 Implementation Plan v2 — On-Device App Risk Auditor

Native Android (Kotlin + Jetpack Compose). 4 developers, 6 hours, APK installed on the judging device by hour 5.
This plan replaces the previous one. It is written so each task can be pasted into an Antigravity agent as-is.

**Changes from v1 (read this first)**
1. The scanner always inventories *all* apps (user + system). The "include system apps" toggle is a UI filter, so it works instantly with no rescan.
2. Findings are produced **per category / per special-access type**, not per permission. This prevents double counting and makes "at least 3 reasons" testable.
3. Declared-vs-granted is now part of the score through a fixed multiplier table (Section 7.2).
4. Ownership of storage, PDF, install monitoring and fixtures was ambiguous in v1. It is now explicit (Section 3).
5. Hash lookup (stretch 11) is **deliberately cut**: it needs the INTERNET permission and would contradict the offline-first/privacy story. The APK ships with **no INTERNET permission**, which is a demo talking point.

---

## 0. How to run this plan with Antigravity

1. Open the shared repo as the workspace. Put the block in **Section 0.1** into `AGENTS.md` at the repo root (and as a workspace rule if your Antigravity build supports it). Every agent run then inherits the same facts.
2. **One task = one agent run.** Paste only the task text (e.g. "Task D2-3") plus "Follow AGENTS.md". Small, file-scoped tasks are what keeps a fast model accurate.
3. Every task ends with a **Done when** line containing a command. Tell the agent to run it and paste the output. No output, not done.
4. Commit after every task. Branch per dev: `dev1-ui`, `dev2-scanner`, `dev3-rules`, `dev4-integration`. Merge to `main` through Dev 4 only, at the sync points in Section 9.
5. Agents must never edit files outside the owner's list (Section 3). If a contract has to change, stop and tell Dev 4; Dev 4 edits the contract and pings everyone.
6. Review every diff yourself before merging. Models invent Android APIs. If a call doesn't compile, fix it from the "API facts" below, not by guessing.

### 0.1 `AGENTS.md` content (paste verbatim)

```
# Project: PS-01 On-Device App Risk Auditor (Android, Kotlin, Compose)
Package: com.ps01.riskauditor   minSdk 26   targetSdk 34   compileSdk 35   JDK 17

## Rules
- Only edit files you own (see docs/IMPLEMENTATION_PLAN.md Section 3). If you need a change elsewhere, stop and report.
- Do not change anything under core/model/ or the interfaces in core/contract/. Those are frozen contracts.
- No network code. The app declares NO INTERNET permission. No analytics, no cloud, no LLM calls.
- Never log package names, app lists, or scan results. No Log.d of inventory data.
- Unknown is not safe. Use UNKNOWN states; never default an unreadable value to DISABLED/DENIED/safe.
- Never crash a scan because one app fails: wrap per-app work in try/catch, add a warning, continue.
- Never block the main thread. Scanning runs on Dispatchers.Default/IO.
- Scoring is deterministic and lives only in core/rules. UI never computes scores. Scanner never computes scores.
- After each task run the command in "Done when" and show its output.
- Kotlin only. No Hilt/Dagger (manual DI via AppContainer). No Java.

## Android API facts (use these, do not improvise)
- Installed apps: PackageManager.getInstalledPackages(PackageInfoFlags.of(GET_PERMISSIONS.toLong())) on API 33+;
  deprecated getInstalledPackages(int) below 33. Branch on Build.VERSION.SDK_INT.
- Declared permissions: packageInfo.requestedPermissions (nullable). Granted: packageInfo.requestedPermissionsFlags[i]
  and PackageInfo.REQUESTED_PERMISSION_GRANTED (value 2). Declared != granted.
- System app: (flags and FLAG_SYSTEM) != 0 && (flags and FLAG_UPDATED_SYSTEM_APP) == 0.
- Installer: API 30+ pm.getInstallSourceInfo(pkg).installingPackageName; below 30 pm.getInstallerPackageName(pkg).
- Protection level: pm.getPermissionInfo(name, 0); API 28+ use .protection (PROTECTION_NORMAL/DANGEROUS/SIGNATURE) ;
  NameNotFoundException => UNKNOWN. Cache results per permission name for the whole scan.
- Accessibility: AccessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK) = enabled;
  AccessibilityManager.getInstalledAccessibilityServiceList() = declared. Package = it.resolveInfo.serviceInfo.packageName.
- Device admin: DevicePolicyManager.getActiveAdmins() (nullable) = active. isDeviceOwnerApp(pkg)/isProfileOwnerApp(pkg) = managed.
  Declared: queryBroadcastReceivers(Intent("android.app.action.DEVICE_ADMIN_ENABLED")) with receiver permission BIND_DEVICE_ADMIN.
- Notification listener: NotificationManagerCompat.getEnabledListenerPackages(context) = enabled.
  Declared: queryIntentServices(Intent("android.service.notification.NotificationListenerService")).
- Overlay / install-unknown-apps / usage-stats / all-files access for OTHER apps: AppOpsManager
  (unsafeCheckOpNoThrow on API 29+, checkOpNoThrow below), op strings "android:system_alert_window",
  "android:request_install_packages", "android:get_usage_stats", "android:manage_external_storage".
  MODE_ALLOWED => ENABLED; MODE_IGNORED/MODE_ERRORED => DISABLED; MODE_DEFAULT or any exception => UNKNOWN.
- Settings.canDrawOverlays / canRequestPackageInstalls only answer for the calling app. Never use them for other apps.
- Uninstall: Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")); needs REQUEST_DELETE_PACKAGES in manifest.
  Always user-confirmed by the system dialog. Never silent.
- App settings: Settings.ACTION_APPLICATION_DETAILS_SETTINGS with Uri "package:$pkg".
- Special-access screens: ACTION_ACCESSIBILITY_SETTINGS, ACTION_NOTIFICATION_LISTENER_SETTINGS, ACTION_USAGE_ACCESS_SETTINGS,
  ACTION_MANAGE_OVERLAY_PERMISSION (+package uri), ACTION_MANAGE_UNKNOWN_APP_SOURCES (+package uri), ACTION_SECURITY_SETTINGS (device admin).
  Wrap every startActivity in try/catch(ActivityNotFoundException) and fall back to app details.
- Icons: pm.getApplicationIcon(pkg) on a background dispatcher; convert with Drawable.toBitmap(96, 96). Never store icons in the DB.
- PDF: android.graphics.pdf.PdfDocument, A4 = 595x842 pt. Share with FileProvider + FLAG_GRANT_READ_URI_PERMISSION.
```

---

## 1. Requirement traceability (problem statement → owner → proof)

| Brief item | Owner | Where | Proof |
|---|---|---|---|
| Q1 harm ranking / Q2 why / Q3 act now | Dev 1 | Dashboard "Riskiest apps"; Detail "Why flagged"; Detail "What you can do now" | Demo script (Section 11) |
| 01 Inventory: name, icon, package, version, install date, installer, target SDK, system toggle | Dev 2 + Dev 1 | `AndroidAppScanner`, `AppListScreen` | D2 instrumented test, D1 screenshot |
| 02 Declared vs granted + protection level | Dev 2 | `PermissionExtractor` | Unit tests D2-2 |
| 03 Nine categories per app + device-wide summary | Dev 3 + Dev 1 | `PermissionCategoryMapper`, `DeviceSummaryCalculator`, Dashboard | Unit tests D3-1, D3-6 |
| 04 Special access (a11y, admin, listeners, overlay, install-packages) | Dev 2 | `SpecialAccessCollector` | Fixture app on emulator |
| 05 Score 0–100, 4 bands, ≥3 reasons for High/Critical | Dev 3 | `RuleBasedRiskEngine` | Golden tests D3-5 |
| 06 Detail screen: Why flagged, settings button, uninstall | Dev 1 + Dev 2 | `AppDetailScreen`, `SettingsLauncher` | Manual test M-12 |
| 07 Combination rules | Dev 3 | `COMBO_*` rules | Golden tests |
| 08 Context mismatch | Dev 3 | `CTX_MISMATCH` | Golden test T7 |
| 09 Install monitoring + 10 Permission diff | Dev 4 (+ Dev 3 differ) | `PackageChangeMonitor`, `PermissionDiffer` | Fixture install/update |
| 14 PDF report | Dev 3 | `core/report` | Manual test M-15 |
| 11 Hash lookup | — | **Cut** (needs INTERNET) | Documented in README |
| 12 Manifest checks / 13 ML model | — | Not planned | — |
| Deliverable 2: one-page rubric | Dev 3 writes, Dev 4 formats | `docs/RISK_SCORING.md` | ≤1 printed page |
| Deliverable 4: 3-min demo on organizer kit | Dev 4 leads | Section 11 | Two rehearsals |

---

## 2. Locked technical decisions

| Topic | Decision |
|---|---|
| Language/UI | Kotlin 2.0.20, Compose BOM 2024.09.00, Material 3, Navigation-Compose 2.8.0 |
| Build | AGP 8.5.2, Gradle 8.9, KSP 2.0.20-1.0.25, JDK 17. If the Android Studio template picks other versions, keep the template's and only add libraries |
| State | `ViewModel` + `StateFlow`, `collectAsStateWithLifecycle` |
| DI | Manual: `AppContainer` created in `RiskAuditorApp` |
| Storage | Room 2.6.1; each app result stored as JSON text (kotlinx-serialization 1.7.2) plus indexed columns for sorting |
| Background | WorkManager 2.9.1 (periodic rescan) + dynamic broadcast receiver while the process is alive |
| Modules | **Single Gradle module `:app`** plus one extra module `:fixtures` (benign test apps, never shipped in `:app`) |
| History retention | Last 10 scans, pruned on save |
| Policy version string | `"ps01-rules-1"` stored in every assessment |

### 2.1 Manifest permissions (the complete list; nothing else)

```xml
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"/>   <!-- allowed by brief; Play-policy caveat in README -->
<uses-permission android:name="android.permission.REQUEST_DELETE_PACKAGES"/> <!-- needed for ACTION_DELETE on API 28+ -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>   <!-- only for install-monitor alerts; requested at runtime on API 33+ -->
<!-- NO INTERNET. NO storage. NO contacts/SMS. -->
```

Components: `MainActivity` (exported, launcher), `FileProvider` (`exported=false`, `grantUriPermissions=true`, authority `${applicationId}.files`, path = `cache/reports/`). No other exported components. Set `android:allowBackup="false"` and `android:usesCleartextTraffic="false"`.

---

## 3. Ownership and file map (one file, one owner)

```
app/src/main/java/com/ps01/riskauditor/
├── MainActivity.kt                         D4 (scaffold) → D1 afterwards
├── RiskAuditorApp.kt, AppContainer.kt      D4
├── Prefs.kt                                D4
├── core/model/Models.kt                    FROZEN contract (D4 commits, nobody edits alone)
├── core/contract/Contracts.kt              FROZEN contract (interfaces)
├── core/fake/FakeData.kt, FakeScanRepository.kt   D4 (hour 0), used by D1
├── core/platform/                          D2
│   ├── InstalledPackagesReader.kt, PermissionExtractor.kt, InstallSourceResolver.kt
│   ├── SpecialAccessCollector.kt (+ per-type readers), SettingsLauncher.kt, IconLoader.kt
├── core/scanner/                           D2
│   ├── AndroidAppScanner.kt, ScanOrchestrator.kt
├── core/rules/                             D3
│   ├── PermissionCategoryMapper.kt, RiskPolicy.kt, RuleBasedRiskEngine.kt
│   ├── Rules.kt, Explanations.kt, DeviceSummaryCalculator.kt, PermissionDiffer.kt
├── core/report/                            D3
│   ├── PdfReportExporter.kt, PdfTextUtil.kt
├── core/storage/                           D4
│   ├── AppDatabase.kt, Entities.kt, Daos.kt, RoomScanRepository.kt, Converters.kt
├── core/monitor/                           D4
│   ├── PackageChangeMonitor.kt, PackageChangeWorker.kt, NotificationHelper.kt
└── feature/                                D1
    ├── common/ (Labels.kt, Components.kt, AppIcon.kt, Theme)
    ├── dashboard/, apps/, detail/, history/, settings/
app/src/test/                               each dev owns tests for their package
app/src/androidTest/                        D2 (scanner), D4 (storage, e2e)
fixtures/                                   D4
docs/                                       D3: RISK_SCORING.md · D4: everything else
```

Dev 4 is integration lead and the only person who merges to `main`.

---

## 4. Frozen contracts (Dev 4 commits these verbatim in the first 20 minutes)

### 4.1 `core/model/Models.kt`

```kotlin
package com.ps01.riskauditor.core.model

import kotlinx.serialization.Serializable

enum class GrantState { GRANTED, DENIED, NOT_RUNTIME, UNKNOWN, NOT_SUPPORTED }
enum class SpecialAccessState { ENABLED, DISABLED, NOT_DECLARED, UNKNOWN, NOT_SUPPORTED }
enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }
enum class Confidence { LOW, MEDIUM, HIGH }
enum class ProtectionLevel { NORMAL, DANGEROUS, SIGNATURE, SPECIAL, UNKNOWN }
enum class InstallSource { PLAY_STORE, OTHER_STORE, SIDELOADED, UNKNOWN }
enum class ScanStatus { RUNNING, COMPLETE, PARTIAL, CANCELLED }

enum class PermissionCategory {
    SPECIAL_ACCESS, SMS, CONTACTS_CALLS, CAMERA_MIC, LOCATION,
    PHONE_IDENTITY, STORAGE_MEDIA, BACKGROUND, NETWORK, OTHER   // OTHER = shown, never scored
}

enum class SpecialAccessType {
    ACCESSIBILITY, DEVICE_ADMIN, NOTIFICATION_LISTENER, OVERLAY, INSTALL_PACKAGES, USAGE_STATS
}

enum class RecommendedAction {
    OPEN_APP_SETTINGS, OPEN_ACCESSIBILITY_SETTINGS, OPEN_NOTIFICATION_LISTENER_SETTINGS,
    OPEN_OVERLAY_SETTINGS, OPEN_DEVICE_ADMIN_SETTINGS, OPEN_INSTALL_SOURCES_SETTINGS,
    OPEN_USAGE_ACCESS_SETTINGS, UNINSTALL, NONE
}

/** Every PermissionAudit is a DECLARED permission by definition. */
@Serializable
data class PermissionAudit(
    val name: String,                 // full name, e.g. android.permission.READ_SMS
    val category: PermissionCategory,
    val protection: ProtectionLevel,
    val grantState: GrantState
)

@Serializable
data class SpecialAccessAudit(
    val type: SpecialAccessType,
    val state: SpecialAccessState,
    val detail: String? = null        // what was observed, e.g. "Listed in enabled accessibility services"
)

@Serializable
data class AppAuditRecord(
    val packageName: String,
    val appLabel: String,
    val versionName: String?,
    val versionCode: Long,
    val targetSdk: Int,
    val isSystemApp: Boolean,
    val installSource: InstallSource,
    val installerPackage: String?,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val appCategory: Int?,            // ApplicationInfo.category (API 26+), often undefined; low reliability
    val isDeviceOrProfileOwner: Boolean,
    val permissions: List<PermissionAudit>,
    val specialAccess: List<SpecialAccessAudit>,   // always exactly 6 entries, one per SpecialAccessType
    val collectionWarnings: List<String> = emptyList()
)

@Serializable
data class RiskFinding(
    val ruleId: String,
    val title: String,
    val points: Int,
    val category: PermissionCategory,
    val confidence: Confidence,
    val isHeuristic: Boolean,
    val evidence: List<String>,       // concrete observed items, e.g. "READ_SMS (granted)"
    val explanation: String,          // plain language, ≤ 2 sentences
    val recommendation: String,       // plain language, what to do
    val action: RecommendedAction
)

@Serializable
data class RiskAssessment(
    val score: Int,                   // 0..100
    val level: RiskLevel,
    val findings: List<RiskFinding>,  // sorted by points desc
    val cappedForEvidence: Boolean,   // true if score was capped to 49 because < 3 reasons
    val policyVersion: String
)

@Serializable
data class ScoredApp(val record: AppAuditRecord, val assessment: RiskAssessment)

data class ScanSummary(
    val id: Long, val startedAt: Long, val finishedAt: Long?,
    val status: ScanStatus, val appCount: Int, val warnings: List<String>
)

data class CategoryStat(val category: PermissionCategory, val appsWithAccess: Int, val appsDeclaring: Int)
data class DeviceSummary(
    val totalApps: Int,
    val bandCounts: Map<RiskLevel, Int>,
    val categoryStats: List<CategoryStat>,   // all 9 scored categories, fixed order
    val topRisky: List<ScoredApp>
)
```

### 4.2 `core/contract/Contracts.kt`

```kotlin
package com.ps01.riskauditor.core.contract

import com.ps01.riskauditor.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface RiskEngine { fun evaluate(record: AppAuditRecord): RiskAssessment }          // pure, deterministic

sealed interface ScanEvent {
    data class Progress(val done: Int, val total: Int, val currentLabel: String?) : ScanEvent
    data class AppScanned(val record: AppAuditRecord) : ScanEvent
    data class Warning(val message: String) : ScanEvent
}
interface AppScanner {
    fun scan(): Flow<ScanEvent>                                  // ALL apps (user + system); cooperative cancellation
    suspend fun scanOne(packageName: String): AppAuditRecord?    // for install monitoring; null if not installed
}

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data class Running(val done: Int, val total: Int, val currentLabel: String?) : ScanUiState
    data class Finished(val scanId: Long, val status: ScanStatus, val warnings: List<String>) : ScanUiState
    data class Failed(val message: String) : ScanUiState
}
interface ScanController {
    val state: StateFlow<ScanUiState>
    fun start()
    fun cancel()
}

interface ScanRepository {
    suspend fun beginScan(): Long
    suspend fun saveApp(scanId: Long, app: ScoredApp)
    suspend fun finishScan(scanId: Long, status: ScanStatus, warnings: List<String>)
    fun observeScans(): Flow<List<ScanSummary>>                   // newest first
    fun observeLatestScan(): Flow<ScanSummary?>
    fun observeApps(scanId: Long): Flow<List<ScoredApp>>          // sorted by score desc
    suspend fun getApp(scanId: Long, packageName: String): ScoredApp?
    suspend fun latestAppRecord(packageName: String): ScoredApp?  // most recent stored result for diffing
}

interface ReportExporter {
    suspend fun export(scan: ScanSummary, apps: List<ScoredApp>): File   // writes to cacheDir/reports/
}

interface SettingsLauncher {                                       // implemented by D2
    fun open(action: RecommendedAction, packageName: String): Boolean   // false => fallback failed
    fun uninstall(packageName: String): Boolean
}
interface IconLoader { suspend fun load(packageName: String): android.graphics.Bitmap? }
```

### 4.3 Stubs Dev 4 also commits (so everything compiles on day one)

- `PermissionCategoryMapper` → `fun categoryOf(permission: String) = PermissionCategory.OTHER` (D3 replaces)
- `RuleBasedRiskEngine` → returns `RiskAssessment(0, LOW, emptyList(), false, "stub")` (D3 replaces)
- `AppContainer` exposes `val repository: ScanRepository` (initially `FakeScanRepository`), `val scanController: ScanController` (initially a fake that emits Running → Finished over 3 s), `val settingsLauncher`, `val iconLoader`, `val reportExporter`, `val prefs`.
- `FakeData.kt` with **12 synthetic `ScoredApp`s** covering: Critical (a11y+overlay, sideloaded), Critical (torch with SMS), High, 3× Medium, 3× Low, one with `collectionWarnings`, one system app, one with UNKNOWN special-access states, one with a 60-character label.

---

## 5. Developer 1 — UI (Compose)

**Goal:** a non-technical user answers Q1/Q2/Q3 in under 30 seconds. D1 builds everything against `FakeScanRepository` until Sync 2, then flips one line in `AppContainer` (Dev 4 does the flip).

**D1 must not:** compute scores, call `PackageManager`, edit models/contracts, parse permission names for logic (display only).

### Navigation
Bottom bar with 4 destinations: `dashboard`, `apps`, `history`, `settings`. Extra routes: `detail/{scanId}/{packageName}`, `scan/{scanId}` (a past scan, reuses the apps list). Use `NavHost` in `feature/AppNavHost.kt`. Package names in routes must be `Uri.encode`d.

### Task D1-1 — Theme, labels, shared components (0:25–0:55)
Files: `feature/common/Theme.kt`, `Labels.kt`, `Components.kt`, `AppIcon.kt`.
- Band colours (use the same hue for icon + text + chip, and always show the **word** too, never colour alone): Low = green `#2E7D32`, Medium = amber `#B26A00`, High = deep orange `#D84315`, Critical = red `#B71C1C`. Support light and dark.
- `Labels.kt`: `PermissionCategory.displayName()` (e.g. "Special access", "SMS", "Contacts & call logs", "Camera & microphone", "Location", "Phone & identity", "Storage & media", "Background & persistence", "Network", "Other"); `RiskLevel.displayName()`; `GrantState.displayName()` ("Allowed", "Not allowed", "Not a runtime permission", "Unknown", "Not supported on this Android version"); `SpecialAccessState.displayName()` ("On", "Off", "Not requested", "Couldn't check", "Not supported"); `InstallSource.displayName()` ("Play Store", "Other app store", "Installed outside a store", "Unknown"); `fun permissionShortName(name: String) = name.substringAfterLast('.')`; `fun formatDate(epochMs: Long)`.
- `Components.kt`: `RiskBadge(level, score)`, `StateChip(text, kind)` where kind ∈ {POSITIVE, NEGATIVE, NEUTRAL, WARNING}, `SectionCard(title, content)`, `EmptyState(icon, title, body)`, `ErrorBanner(text)`, `PartialBanner(warnings)`.
- `AppIcon.kt`: `@Composable fun AppIcon(packageName: String, loader: IconLoader, size: Dp)` using `produceState` + `loader.load()`; fallback = Android icon placeholder.
Done when: `./gradlew :app:assembleDebug` succeeds.

### Task D1-2 — Dashboard (0:55–1:55)
Files: `feature/dashboard/DashboardScreen.kt`, `DashboardViewModel.kt`.
```kotlin
data class DashboardUiState(
    val scan: ScanUiState = ScanUiState.Idle,
    val latestScan: ScanSummary? = null,
    val summary: DeviceSummary? = null,       // computed from apps filtered by includeSystem
    val includeSystem: Boolean = false
)
```
ViewModel combines `container.scanController.state`, `repository.observeLatestScan()`, flatMapLatest to `observeApps(scanId)`, `prefs.includeSystem` → `DeviceSummaryCalculator.calculate(filtered)`.
Layout, top to bottom:
1. Title + subtitle "Everything is checked on this phone. Nothing is uploaded."
2. Primary button "Scan my apps" (disabled while running; becomes "Cancel scan" outlined button while running). While running show `LinearProgressIndicator(progress = done/total)` and text "Checking {currentLabel} (done of total)".
3. If last scan `PARTIAL` or has warnings: `PartialBanner` "Some apps could not be fully checked" + expandable list.
4. Four band cards in a 2×2 grid: count + word (Critical, High, Medium, Low). Tap → Apps tab pre-filtered to that band (pass via nav argument `?band=`).
5. **"Apps that could do the most harm"** (Q1): top 5 rows (icon, label, `RiskBadge`, one-line top finding title). Tap → detail.
6. **"What apps can access on this phone"**: 9 rows, one per scored category in fixed order: name, "{appsWithAccess} apps use it", horizontal bar = appsWithAccess/totalApps, tiny text "{appsDeclaring} ask for it". Tap → Apps tab filtered by category.
7. Footer link "Privacy & limits" → Settings tab.
States: never scanned (EmptyState + big Scan button, no zeros presented as results), running, finished, failed (`ErrorBanner` + retry).
Done when: with fake data the dashboard shows all 7 blocks; screenshot saved to `docs/screens/dashboard.png`.

### Task D1-3 — App list (1:55–2:40)
Files: `feature/apps/AppListScreen.kt`, `AppListViewModel.kt`.
- Source: latest scan apps, or `scanId` arg for a past scan. Filter by `prefs.includeSystem`.
- Controls: search field (label or package, case-insensitive); `FilterChip`s Critical/High/Medium/Low (multi-select, empty = all); category dropdown (optional filter, from dashboard); sort menu: Risk (default, desc), Name A–Z, Recently installed, Recently updated; "Include system apps" `Switch` (writes `prefs.includeSystem`, instant, no rescan).
- Row: icon, label (1 line ellipsis), package (small, 1 line), `RiskBadge`, top-2 finding titles as small text, "Installed outside a store" tag when `installSource == SIDELOADED`.
- Use `LazyColumn` with `key = packageName`. Result count text "Showing N of M apps".
- Empty-filter state: "No apps match these filters" with a Clear filters button. Zero third-party apps: "No user-installed apps found."
Done when: filters, search, sort and toggle all work on fake data (record a short screen capture or list manual steps tried).

### Task D1-4 — App detail (2:40–4:00) *(most important screen)*
Files: `feature/detail/AppDetailScreen.kt`, `AppDetailViewModel.kt`.
Sections in order:
1. **Header:** icon 64dp, label, package, version name (code), `RiskBadge` large with score "72 / 100" and band word.
2. **Why flagged** (Q2): for each finding (already sorted): card with title, "+{points} points", `explanation`, evidence chips (one chip per evidence string), confidence text ("Confidence: Medium"), and a "Based on a guess about the app's purpose" tag when `isHeuristic`. If `assessment.cappedForEvidence`: info card "Score limited to Medium: fewer than three independent reasons were found." If findings empty: "No risky access found. This does not prove the app is safe." Always show the footer: "Permissions alone do not prove an app is malicious."
3. **What you can do now** (Q3): dedupe findings by `action`; one button per distinct action with the finding's `recommendation` text under it. Buttons call `settingsLauncher.open(action, pkg)`. If it returns false show a Snackbar "Couldn't open that screen. Opening app settings instead." and call `OPEN_APP_SETTINGS`.
4. **Special access:** 6 rows (name, state chip, `detail` small text). `UNKNOWN` uses WARNING kind and the text "Couldn't check" — never green.
5. **Permissions by category:** 9 + Other, collapsible (expanded for categories with granted access). Per permission row: `permissionShortName`, protection chip, then **two** labelled chips "Asked for" (always) and `GrantState.displayName()`. Count in category header: "3 asked, 2 allowed".
6. **App info:** version, target SDK, installed date, last updated, install source + installer package, system/user, warnings from `collectionWarnings`.
7. Sticky bottom bar: "Open app settings" (always) and "Uninstall" (hidden for system apps; for system apps show text "System apps can't be removed here"). Uninstall opens the system dialog via `settingsLauncher.uninstall` — add our own confirm dialog first: "The system will ask you to confirm."
Accessibility: every chip and button has `contentDescription`/`semantics { stateDescription }`; min touch target 48dp; no information by colour alone.
Done when: Critical and Low fake apps both render without clipping; 60-char label wraps cleanly; unknown-state app shows "Couldn't check".

### Task D1-5 — History, Settings, polish (4:00–5:00)
Files: `feature/history/*`, `feature/settings/*`.
- History: list of scans (date/time, status chip, app count, counts of High+Critical). Tap → `scan/{scanId}` (apps list for that scan). Swipe-free; no delete required. Empty state "No scans yet."
- Settings: switch "Include system apps in results"; switch "Alert me when a new app is risky" (on enable: request `POST_NOTIFICATIONS` on API 33+, then call `prefs.monitoring = true` and `container.monitor.enable()`); button "Export PDF report" (calls `reportExporter.export`, then launches `ACTION_SEND` chooser with FileProvider URI, MIME `application/pdf`, shows spinner meanwhile, error Snackbar on failure); section "Privacy & limits" with the static text from Section 12 (copy it).
- Final pass: dark mode check, font scale 1.5×, TalkBack labels on all icons, landscape doesn't crash (don't need to be pretty).
Done when: `./gradlew :app:assembleDebug` passes; Dev 4 checks the settings toggle persists across process death.

---

## 6. Developer 2 — Scanner and platform adapters

**Goal:** truthful, crash-proof data about every installed app plus real special-access states.
**D2 must not:** score, categorize beyond calling `PermissionCategoryMapper`, touch UI, request any extra permission, read notifications/SMS/contacts/files.

### Task D2-1 — `InstalledPackagesReader` + `InstallSourceResolver` (0:25–1:00)
Files: `core/platform/InstalledPackagesReader.kt`, `InstallSourceResolver.kt`.
- `fun readAll(): List<PackageInfo>`: flags `GET_PERMISSIONS`; branch on API 33. Catch `RuntimeException` (e.g. `DeadObjectException`/transaction too large): fall back to `getInstalledApplications` list then `getPackageInfo` per package, adding a warning.
- `isSystem(applicationInfo)` per AGENTS.md rule.
- `InstallSourceResolver.resolve(pkg, isSystem): Pair<InstallSource, String?>`:
  - exception while querying → `(UNKNOWN, null)`
  - installer `com.android.vending` → `PLAY_STORE`
  - installer in `{com.sec.android.app.samsungapps, com.amazon.venezia, com.huawei.appmarket, com.xiaomi.mipicks, com.xiaomi.market, com.heytap.market, com.oppo.market, org.fdroid.fdroid}` → `OTHER_STORE`
  - system app with null installer → `(OTHER_STORE, null)` (preinstalled; never "sideloaded")
  - anything else, including null, `com.google.android.packageinstaller`, `com.android.packageinstaller`, `com.android.shell`, browsers/file managers → `SIDELOADED`
  - Put the lists in one `companion object` so they're documented and testable. Document the false-positive: enterprise/MDM installs look sideloaded.
- Unit tests (JVM, pure functions): extract the string-matching part into `fun classify(installer: String?, isSystem: Boolean): InstallSource` and test all branches.
Done when: `./gradlew :app:testDebugUnitTest --tests "*InstallSource*"` passes.

### Task D2-2 — `PermissionExtractor` (1:00–1:45)
File: `core/platform/PermissionExtractor.kt`.
```kotlin
class PermissionExtractor(private val pm: PackageManager) {
    fun extract(info: PackageInfo): List<PermissionAudit>
}
```
Rules:
- Iterate `requestedPermissions` (null ⇒ empty list). Granted bit from `requestedPermissionsFlags` (null or shorter array ⇒ `UNKNOWN`).
- Protection: cached `getPermissionInfo`. Map base type to `NORMAL/DANGEROUS/SIGNATURE`. If name ∈ `SPECIAL_PERMISSIONS` → `SPECIAL`: `SYSTEM_ALERT_WINDOW, REQUEST_INSTALL_PACKAGES, PACKAGE_USAGE_STATS, MANAGE_EXTERNAL_STORAGE, WRITE_SETTINGS, BIND_ACCESSIBILITY_SERVICE, BIND_DEVICE_ADMIN, BIND_NOTIFICATION_LISTENER_SERVICE, SCHEDULE_EXACT_ALARM, USE_FULL_SCREEN_INTENT`. `NameNotFoundException` ⇒ `UNKNOWN` (custom permission; not a failure).
- Grant state: granted bit set ⇒ `GRANTED`; protection SPECIAL ⇒ `NOT_RUNTIME` (real state comes from special-access adapters); protection DANGEROUS and not granted ⇒ `DENIED`; NORMAL/SIGNATURE and bit not set ⇒ `DENIED`; flags unavailable ⇒ `UNKNOWN`. Permission introduced in a newer API than the device (e.g. `POST_NOTIFICATIONS` on API 30) ⇒ `NOT_SUPPORTED` when `getPermissionInfo` fails *and* name is in a small `NEWER_API_PERMISSIONS` map `{POST_NOTIFICATIONS:33, READ_MEDIA_IMAGES:33, READ_MEDIA_VIDEO:33, READ_MEDIA_AUDIO:33, NEARBY_WIFI_DEVICES:33, READ_MEDIA_VISUAL_USER_SELECTED:34}`.
- Category from `PermissionCategoryMapper.categoryOf(name)`. De-duplicate names (some manifests repeat).
Test (JVM with a small seam): make `fun mapGrant(flagsBit: Boolean?, protection: ProtectionLevel): GrantState` pure and test the table above.
Done when: `./gradlew :app:testDebugUnitTest --tests "*PermissionExtractor*"` passes.

### Task D2-3 — Special-access readers (1:45–3:00)
Files: `core/platform/SpecialAccessCollector.kt` plus one small class per type in the same file or siblings.
```kotlin
class SpecialAccessSnapshot { fun auditsFor(info: PackageInfo): List<SpecialAccessAudit> }   // always 6 entries
class SpecialAccessCollector(private val context: Context) { fun snapshot(): SpecialAccessSnapshot }
```
`snapshot()` is computed **once per scan**, not per app. Per-type derivation:

| Type | Declared when | ENABLED when | DISABLED when | UNKNOWN when |
|---|---|---|---|---|
| ACCESSIBILITY | package in installed a11y service list | in enabled a11y list | declared but not enabled | list call throws or returns null |
| DEVICE_ADMIN | package has a receiver with `BIND_DEVICE_ADMIN` | in `getActiveAdmins()` | declared, not active | `getActiveAdmins()` throws |
| NOTIFICATION_LISTENER | package has a `NotificationListenerService` service | in enabled listener packages | declared, not enabled | call throws |
| OVERLAY | requests `SYSTEM_ALERT_WINDOW` | AppOps `MODE_ALLOWED` | `MODE_IGNORED`/`ERRORED` | `MODE_DEFAULT` or exception |
| INSTALL_PACKAGES | requests `REQUEST_INSTALL_PACKAGES` | AppOps allowed | ignored/errored | default or exception |
| USAGE_STATS | requests `PACKAGE_USAGE_STATS` | AppOps allowed | ignored/errored | default or exception |

- Not declared ⇒ `NOT_DECLARED` (never UNKNOWN).
- Fill `detail` with exactly what was observed, e.g. `"Listed in the phone's enabled accessibility services"`, `"Declared in manifest; Android did not report whether it is turned on"`.
- `isDeviceOrProfileOwner` on the record comes from `dpm.isDeviceOwnerApp(pkg) || dpm.isProfileOwnerApp(pkg)` inside try/catch (false on failure + warning).
- Hour-2 emulator check: install `:fixtures` apps (Dev 4) and confirm the table works. **If AppOps returns UNKNOWN on the judging device for overlay/install/usage, that is acceptable and honest** (Dev 3's multiplier handles it). Write the observed behaviour into `docs/SECURITY_MODEL.md` section "What we can and cannot observe" (give Dev 4 two sentences).
Done when: instrumented test on emulator asserts the snapshot returns 6 entries for the app's own package and `NOT_DECLARED` for each (the app declares none), and does not throw.

### Task D2-4 — `AndroidAppScanner` (3:00–3:45)
File: `core/scanner/AndroidAppScanner.kt`.
- `scan(): Flow<ScanEvent>` via `flow { }.flowOn(Dispatchers.Default)`. Steps: read all packages → snapshot special access once → loop: `ensureActive()`, build `AppAuditRecord`, emit `Progress` then `AppScanned`. Total known up front.
- Label: `pm.getApplicationLabel(appInfo).toString()`, fallback to packageName; truncate to 120 chars. Version code: `longVersionCode` (API 28+) else `versionCode.toLong()`.
- Per-app `try/catch(Exception)`: on failure emit `Warning("Could not fully read an app")` **without the package name in the message** (privacy), and still emit a minimal record only if package name + label are known; otherwise skip.
- Skip own package? **No.** Include it (judges like self-audit); it must score Low.
- `scanOne(pkg)`: same builder, fresh snapshot.
Done when: instrumented test on emulator finishes < 15 s, count > 0, own package present, no exceptions.

### Task D2-5 — `ScanOrchestrator`, `SettingsLauncher`, `IconLoader` (3:45–4:30)
Files: `core/scanner/ScanOrchestrator.kt`, `core/platform/AndroidSettingsLauncher.kt`, `AndroidIconLoader.kt`.
- `ScanOrchestrator(scanner, engine, repository, scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)) : ScanController`.
  - `start()` ignored if already running. `beginScan()` → collect events: `Progress` → state `Running`; `AppScanned` → `engine.evaluate(record)` → `repository.saveApp`; `Warning` → collect. On completion: `COMPLETE` if no warnings else `PARTIAL`; on cancel: `finishScan(CANCELLED)` in a `NonCancellable` block; on unexpected throwable: `finishScan(PARTIAL)` + `Failed` state only if zero apps saved.
  - Save apps in batches inside the repository (Dev 4) — orchestrator just calls `saveApp` per app.
- `AndroidSettingsLauncher`: implements the intents in AGENTS.md, adds `FLAG_ACTIVITY_NEW_TASK` when context isn't an Activity, falls back to app details, returns false if even that fails. `uninstall()` uses `ACTION_DELETE`.
- `AndroidIconLoader`: `withContext(Dispatchers.IO)`, `LruCache<String, Bitmap>` of 150 entries, 96×96.
Done when: unit test of `ScanOrchestrator` with fake scanner/engine/repository proves: state sequence Idle→Running→Finished, cancellation yields `CANCELLED`, a warning yields `PARTIAL`.

### Task D2-6 — Hardening and handoff (4:30–5:00)
- Run the scan on 2 different API levels (e.g. API 30 and API 34 emulators) and a real device if available; list per-API differences in `docs/SECURITY_MODEL.md` via Dev 4.
- Android 11+ package visibility: confirm `QUERY_ALL_PACKAGES` actually returns all apps (compare count to `adb shell pm list packages | wc -l`, within a few).
- Profile the scan; if > 10 s on a 300-app device, cache `getPermissionInfo` per name (already required) and process apps in 4 parallel chunks.

---

## 7. Developer 3 — Rules engine, summary, PDF

**Goal:** a deterministic, explainable score. D3 starts in **pure Kotlin** (no Android imports in `core/rules`) so it can begin at minute 0, before the project exists.
**D3 must not:** call Android APIs in `core/rules`, use any LLM/cloud, edit scanner or UI.

### 7.1 Task D3-1 — `PermissionCategoryMapper` (0:00–0:40)
`object PermissionCategoryMapper { fun categoryOf(permission: String): PermissionCategory }` keyed on the short name (after last `.`), matching only `android.permission.*` names (third-party/custom names → `OTHER`).

| Category | Permissions |
|---|---|
| SPECIAL_ACCESS | SYSTEM_ALERT_WINDOW, REQUEST_INSTALL_PACKAGES, PACKAGE_USAGE_STATS, BIND_ACCESSIBILITY_SERVICE, BIND_DEVICE_ADMIN, BIND_NOTIFICATION_LISTENER_SERVICE, WRITE_SETTINGS |
| SMS | READ_SMS, RECEIVE_SMS, SEND_SMS, RECEIVE_MMS, RECEIVE_WAP_PUSH |
| CONTACTS_CALLS | READ_CONTACTS, WRITE_CONTACTS, READ_CALL_LOG, WRITE_CALL_LOG, PROCESS_OUTGOING_CALLS |
| CAMERA_MIC | CAMERA, RECORD_AUDIO |
| LOCATION | ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, ACCESS_BACKGROUND_LOCATION |
| PHONE_IDENTITY | READ_PHONE_STATE, READ_PHONE_NUMBERS, CALL_PHONE, ANSWER_PHONE_CALLS, GET_ACCOUNTS |
| STORAGE_MEDIA | READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO, READ_MEDIA_VISUAL_USER_SELECTED, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE |
| BACKGROUND | RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE (and any `FOREGROUND_SERVICE_*`), REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, WAKE_LOCK, SCHEDULE_EXACT_ALARM, USE_FULL_SCREEN_INTENT |
| NETWORK | INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, CHANGE_NETWORK_STATE |

Note: `MANAGE_EXTERNAL_STORAGE` is in STORAGE_MEDIA per the brief's table even though it is a special grant. Unit test: every permission above maps correctly, unknown ⇒ OTHER, `com.foo.READ_SMS` ⇒ OTHER.
Done when: `./gradlew :app:testDebugUnitTest --tests "*CategoryMapper*"` passes. **Push by 0:40; D2 needs it.**

### 7.2 Scoring policy (this is also the one-page rubric; copy into `docs/RISK_SCORING.md`)

**Multipliers** (how sure we are the capability is live):

| State of the signal | Multiplier |
|---|---|
| GRANTED / ENABLED | 1.00 |
| UNKNOWN (couldn't check) | 0.50 |
| DECLARED only (DENIED / DISABLED) | 0.30 |
| NOT_DECLARED / NOT_SUPPORTED | 0 |
| NOT_RUNTIME (normal/install-time permission, auto-granted) | 1.00 |

**Single-signal points** (at multiplier 1.0):

| Rule ID | Signal | Points | Category cap |
|---|---|---|---|
| `SA_ACCESSIBILITY` | Accessibility service | 30 | — |
| `SA_DEVICE_ADMIN` | Device admin (0 if device/profile owner) | 25 | — |
| `SA_NOTIF_LISTENER` | Notification listener | 22 | — |
| `SA_OVERLAY` | Draw over other apps | 18 | — |
| `SA_INSTALL_PACKAGES` | Install unknown apps | 16 | — |
| `SA_USAGE_STATS` | Usage access | 10 | — |
| `CAT_SMS` | READ_SMS 14, RECEIVE_SMS 10, SEND_SMS 12, RECEIVE_MMS 3, RECEIVE_WAP_PUSH 3 | Σ | 30 |
| `CAT_CONTACTS_CALLS` | READ_CONTACTS 8, WRITE_CONTACTS 4, READ_CALL_LOG 10, WRITE_CALL_LOG 6, PROCESS_OUTGOING_CALLS 6 | Σ | 18 |
| `CAT_CAMERA_MIC` | CAMERA 7, RECORD_AUDIO 9 | Σ | 16 |
| `CAT_LOCATION` | COARSE 3, FINE 6, BACKGROUND 8 | Σ | 14 |
| `CAT_PHONE_IDENTITY` | READ_PHONE_STATE 4, READ_PHONE_NUMBERS 4, CALL_PHONE 5, ANSWER_PHONE_CALLS 3, GET_ACCOUNTS 3 | Σ | 10 |
| `CAT_STORAGE_MEDIA` | MANAGE_EXTERNAL_STORAGE 12, READ_MEDIA_IMAGES 5, READ_MEDIA_VIDEO 5, READ/WRITE_EXTERNAL_STORAGE 5 (once), READ_MEDIA_AUDIO 2, VISUAL_USER_SELECTED 2 | Σ | 14 |
| `CAT_BACKGROUND` | RECEIVE_BOOT_COMPLETED 3, FOREGROUND_SERVICE* 2, IGNORE_BATTERY_OPT 4, WAKE_LOCK 1, SCHEDULE_EXACT_ALARM 1, USE_FULL_SCREEN_INTENT 3 | Σ | 10 |
| (network) | INTERNET / ACCESS_NETWORK_STATE | **0 alone**; used only as a precondition for combinations | — |

A category finding = `min(cap, round(Σ weight × multiplier))`; no finding if it rounds to 0. `SPECIAL_ACCESS` permissions are scored through the `SA_*` rules, never through a category finding (no double counting).

**Combination rules** (bonus = base × the **lowest** multiplier among its required signals; each fires at most once):

| Rule ID | Requires | Base | Confidence | Heuristic | Action |
|---|---|---|---|---|---|
| `COMBO_ACCESS_OVERLAY` | accessibility + overlay | 25 | High | no | OPEN_ACCESSIBILITY_SETTINGS |
| `COMBO_SMS_NET_BOOT_SIDELOAD` | (READ_SMS or RECEIVE_SMS) + INTERNET + RECEIVE_BOOT_COMPLETED + install source SIDELOADED. If source is UNKNOWN: base 15, confidence Medium | 25 | High | no | UNINSTALL |
| `COMBO_ADMIN_NON_MGMT` | active/declared device admin, not device/profile owner, package not in `SECURITY_OR_MDM_PACKAGES` = {`com.google.android.gms`, `com.google.android.apps.work.clouddpc`, `com.microsoft.windowsintune.companyportal`, `com.airwatch.androidagent`} | 18 | Medium | **yes** | OPEN_DEVICE_ADMIN_SETTINGS |
| `COMBO_NOTIF_NET` | notification listener + INTERNET | 12 | Medium | no | OPEN_NOTIFICATION_LISTENER_SETTINGS |
| `COMBO_DROPPER` | REQUEST_INSTALL_PACKAGES + SIDELOADED + (accessibility or overlay) | 15 | Medium | no | OPEN_INSTALL_SOURCES_SETTINGS |
| `COMBO_SURVEILLANCE` | (CAMERA or RECORD_AUDIO) + ACCESS_BACKGROUND_LOCATION + INTERNET + RECEIVE_BOOT_COMPLETED | 12 | Medium | no | OPEN_APP_SETTINGS |
| `COMBO_HARVEST` | (READ_CONTACTS or READ_CALL_LOG) + (READ_SMS or RECEIVE_SMS) + INTERNET | 10 | Medium | no | OPEN_APP_SETTINGS |
| `CTX_MISMATCH` | label whole-word matches {torch, flashlight, calculator, wallpaper, ringtone, compass, qr, barcode, ruler, level} (case-insensitive), **not system app**, AND holds any of: SMS permission, contacts/call-log permission, accessibility, declared device admin | 15 | Low if only declared, Medium otherwise | **yes** | OPEN_APP_SETTINGS |

`PROV_SIDELOADED` (8 points, Medium confidence, action OPEN_INSTALL_SOURCES_SETTINGS): installed outside a store, not a system app, **and** the app has at least one GRANTED dangerous permission or ENABLED special access. Source UNKNOWN gives nothing. Store-installed apps get **no discount** (no negative points).

**Final score** = clamp(Σ all finding points, 0, 100). **Bands:** Low 0–24 · Medium 25–49 · High 50–74 · Critical 75–100.
**Evidence rule:** if band would be High/Critical but fewer than 3 findings have points > 0, cap score at 49, set `cappedForEvidence = true`. Never invent a finding to reach 3.
Rounding: `Math.round` on each finding independently; findings sorted by points desc, then `ruleId` asc (stable, deterministic).

### 7.3 Task D3-2 — Policy + engine skeleton (0:40–1:30)
Files: `RiskPolicy.kt` (all numbers as `val` constants/maps with comments pointing at the table above), `RuleBasedRiskEngine.kt` implementing `RiskEngine`.
Pipeline in `evaluate(record)`: 1) build a `SignalView` (helper: `state(permissionShortName)`, `special(type)`, `multiplier(...)`) — private class; 2) run single-signal rules; 3) run combo rules; 4) provenance; 5) sum, clamp, band, evidence cap; 6) sort. No randomness, no clocks, no I/O.
Done when: compiles; `evaluate` on an empty record returns score 0 / LOW / no findings.

### 7.4 Task D3-3 — Explanations (1:30–2:15)
File: `Explanations.kt` — one function per rule returning `(title, explanation, recommendation)` given the evidence. Style: second person, no jargon, ≤ 2 sentences each, mention `{app}` by label, say what it *could* do, not what it *does*. Examples (follow this tone for all rules):
- `SA_ACCESSIBILITY` (enabled): title "Can read and control your screen"; explanation "{app} has an accessibility service turned on. This lets an app read what is on your screen and tap buttons for you, which banking trojans abuse."; recommendation "If you don't need {app} for accessibility, turn it off in Accessibility settings."
- `SA_NOTIF_LISTENER`: "Can read your notifications" / "…This includes one-time codes sent by banks and messaging apps."
- `COMBO_ACCESS_OVERLAY`: "Can draw fake screens over other apps" / "{app} can both see your screen and draw on top of other apps. Together this is how fake login screens are shown over real banking apps."
- `COMBO_SMS_NET_BOOT_SIDELOAD`: "Can read texts, send them out, and restart itself" / "{app} was installed outside an app store, can read your SMS, connect to the internet and start itself after a reboot. This combination is typical of one-time-code theft."
- `CAT_*`: "Can access {category in lowercase}" + list evidence in plain words (e.g. "your contacts and call history").
- Declared-only variants must say "asks for" not "has": "{app} asks for … but you haven't allowed it."
- UNKNOWN variants must say "Android did not tell us whether this is on."
Done when: unit test asserts no explanation string contains the words "malware", "virus", or "definitely", every one contains the app label, and every one ≤ 240 chars.

### 7.5 Task D3-4 — Rules and combinations (2:15–3:15)
File: `Rules.kt`. Implement every rule from 7.2 exactly, each as a small function `(SignalView) -> RiskFinding?`. Evidence strings: `"READ_SMS (allowed)"`, `"Accessibility service (on)"`, `"Installed outside a store"`, `"Device admin (couldn't check)"`.
Done when: all rules compile; `./gradlew :app:testDebugUnitTest --tests "*Rules*"` passes.

### 7.6 Task D3-5 — Golden tests (3:15–3:50)
File: `app/src/test/.../RuleBasedRiskEngineTest.kt` with a `record(...)` builder. These vectors are the contract (compute carefully, they must pass exactly):

| # | Scenario | Expected |
|---|---|---|
| T1 | "Super Torch", sideloaded, not system; READ_SMS, READ_CONTACTS granted; INTERNET, RECEIVE_BOOT_COMPLETED | CAT_SMS 14 + CAT_CONTACTS 8 + CAT_BACKGROUND 3 + PROV 8 + COMBO_SMS_NET_BOOT 25 + COMBO_HARVEST 10 + CTX_MISMATCH 15 = **83 CRITICAL**, 7 findings |
| T2 | Store app, accessibility ENABLED only | **30 MEDIUM**, 1 finding, `cappedForEvidence=false` |
| T3 | Sideloaded, accessibility + overlay ENABLED | 30+18+25 combo+8 prov = **81 CRITICAL**, 4 findings |
| T4 | Same as T3 but both DISABLED (declared only) | 9 + 5 + 8 (combo 25×0.3=7.5→8) = **22 LOW**; no PROV (nothing granted/enabled) |
| T5 | Store app, notification listener ENABLED + INTERNET | 22 + 12 = **34 MEDIUM** |
| T6 | Store messenger: CONTACTS, CAMERA, MIC, COARSE+FINE location, PHONE_STATE, BOOT, FGS, READ_MEDIA_IMAGES (all granted) | contacts 8, cam/mic 16 (cap), location 9, phone 4, bg 5, storage 5 = **47 MEDIUM** |
| T7 | Store "Calculator Plus" with READ_CONTACTS granted | CAT_CONTACTS 8 + CTX_MISMATCH 15 = **23 LOW** (documents that context alone doesn't convict) |
| T8 | Accessibility ENABLED + device admin ACTIVE (non-MDM), 2 independent signals + COMBO_ADMIN 18 | 30+25+18 = 73 **HIGH** (3 findings) |
| T9 | Device admin ACTIVE only, device owner = true | 0 for SA_DEVICE_ADMIN, **LOW** |
| T10 | UNKNOWN accessibility state | 15, finding present, explanation contains "did not tell us", never "disabled" |
| T11 | Case that would score ≥ 50 with only 2 findings (construct: SA_DEVICE_ADMIN active + SA_ACCESSIBILITY enabled, device owner false, package in MDM allowlist so COMBO off) | 55 → capped to **49 MEDIUM**, `cappedForEvidence=true` |
| T12 | Determinism: evaluate T1 twice | equal, same finding order |
| T13 | Empty permissions + all special NOT_DECLARED | **0 LOW**, no findings |
| T14 | Max-out record (everything granted, sideloaded) | score **100**, never > 100 |
| T15 | Boundaries | 24 → LOW, 25 → MEDIUM, 49 → MEDIUM, 50 → HIGH, 74 → HIGH, 75 → CRITICAL (test `bandFor(score)` directly) |
| T16 | Duplicate permission names in record | counted once |

If a vector disagrees with the table, fix the code **or** raise it with Dev 4 to correct the vector, but never silently change one side.
Done when: `./gradlew :app:testDebugUnitTest` all green.

### 7.7 Task D3-6 — `DeviceSummaryCalculator` + `PermissionDiffer` (3:50–4:15)
- `DeviceSummaryCalculator.calculate(apps, top = 5)`: `bandCounts` always has all 4 keys; `categoryStats` always 9 entries in enum order excluding OTHER; `appsWithAccess` = apps with any GRANTED permission in that category (for SPECIAL_ACCESS: any special access ENABLED); `appsDeclaring` = any declared (SPECIAL_ACCESS: state ≠ NOT_DECLARED). `topRisky` sorted by score desc, ties by label.
- `PermissionDiffer.diff(old: AppAuditRecord?, new: AppAuditRecord): List<String>` returns plain-language lines for newly declared permissions in categories {SPECIAL_ACCESS, SMS, CONTACTS_CALLS, CAMERA_MIC, LOCATION} and special access that moved to ENABLED. `old == null` ⇒ empty list.
Done when: unit tests cover empty list, system filter handled by caller, and a diff with one new SMS permission.

### 7.8 Task D3-7 — `PdfReportExporter` (4:15–5:00)
Files: `core/report/PdfReportExporter.kt`, `PdfTextUtil.kt` implementing `ReportExporter`.
- `PdfDocument`, A4 595×842, margins 40. Fonts via `Paint` (Typeface.DEFAULT / DEFAULT_BOLD).
- Page 1: title, scan date, device model (`Build.MODEL`), Android version, band counts, 9-category summary table (text-based), disclaimer "Generated on this device. Permissions alone do not prove an app is malicious."
- Following pages: apps sorted by score desc (**user apps only unless the list passed in contains system apps**; caller decides), per app: label, package, score + band, up to 5 finding titles with one-line explanation. Page footer "Page n". Auto-paginate by tracking `y`.
- `PdfTextUtil`: `sanitize()` strips control characters (below 0x20 except space) and bidi override chars (U+202A–U+202E, U+2066–U+2069); `ellipsize(text, paint, maxWidth)`; `wrap(text, paint, maxWidth): List<String>`; cap labels at 60 chars.
- Output: `File(context.cacheDir, "reports/ps01-report-${yyyyMMdd-HHmm}.pdf")`, create dir, delete reports older than 7 days.
Done when: unit test for `PdfTextUtil`; instrumented/manual export of a 12-app fake list opens in the device PDF viewer; the 60-char label and an RTL-override label render without overflow.

### 7.9 Task D3-8 — `docs/RISK_SCORING.md` (anytime after 3:50; finalize by 5:00)
One printed page: Section 7.2 tables condensed, band table, evidence rule, three worked examples (T2, T3, T6), and a "What this score is not" line. Hand to Dev 4 for the final format check.

---

## 8. Developer 4 — Integration lead, storage, monitoring, fixtures, QA

**Goal:** `main` is always buildable; the judging-device run cannot surprise us.

### Task D4-1 — Scaffold and contracts (0:00–0:25) **blocks everyone**
1. Create the Android Studio project (Empty Activity / Compose), package `com.ps01.riskauditor`, min SDK 26. Add dependencies: compose BOM, material3, navigation-compose, lifecycle-runtime-compose, lifecycle-viewmodel-compose, room-runtime/ktx/compiler (KSP), kotlinx-serialization-json + plugin, coroutines, work-runtime-ktx, androidx.core.
2. Commit `AGENTS.md` (Section 0.1), `Models.kt`, `Contracts.kt`, the stubs and `FakeData.kt` (Section 4), manifest per 2.1, `RiskAuditorApp`, `AppContainer`, `MainActivity` showing "PS-01" text.
3. Create 4 branches, push, message the team.
Done when: fresh clone → `./gradlew :app:assembleDebug` succeeds and the app launches on an emulator. **Sync 0.**

### Task D4-2 — Room storage (0:25–1:30)
Files in `core/storage/`.
- `ScanEntity(id PK auto, startedAt, finishedAt?, status: String, appCount, warningsJson: String)`.
- `AppResultEntity(id PK auto, scanId FK→ScanEntity ON DELETE CASCADE, packageName, label, riskScore: Int, riskLevel: String, isSystem: Boolean, recordJson: String, assessmentJson: String)`, indices `(scanId, riskScore)`, unique `(scanId, packageName)`.
- DAOs: insert app (REPLACE), insert/update scan, `observeScans()` ordered by `startedAt DESC`, `observeApps(scanId)` ordered by `riskScore DESC, label ASC`, `getApp(scanId, pkg)`, `latestForPackage(pkg)` = join on newest scan with a result for that package, `pruneOld(keep = 10)`.
- `RoomScanRepository` implements `ScanRepository`; JSON via `Json { ignoreUnknownKeys = true; encodeDefaults = true }`; decoding failure for one row ⇒ skip row, never crash. `finishScan` computes `appCount` from the DB. `fallbackToDestructiveMigration()` is acceptable for the event.
- Do not store icons, notification data, or anything outside `AppAuditRecord`/`RiskAssessment`.
Tests (`androidTest`): insert 3 scans/20 apps, order by score, prune keeps 10, process-restart persistence (reopen DB), corrupt JSON row is skipped.
Done when: `./gradlew :app:connectedDebugAndroidTest --tests "*RoomScanRepository*"` passes.

### Task D4-3 — Fixture apps (1:30–2:15)
Module `:fixtures` with 5 **benign** flavors (distinct `applicationId`s, each a blank activity, nothing runs, no network code):

| Flavor id suffix | Label | Declares |
|---|---|---|
| `.torch` | "Mega Torch Light" | READ_SMS, RECEIVE_SMS, READ_CONTACTS, INTERNET, RECEIVE_BOOT_COMPLETED, CAMERA |
| `.overlay` | "Fixture Overlay" | SYSTEM_ALERT_WINDOW, REQUEST_INSTALL_PACKAGES, INTERNET |
| `.a11y` | "Fixture Accessibility" | an empty `AccessibilityService` subclass with `BIND_ACCESSIBILITY_SERVICE` and a minimal config xml (does nothing), plus SYSTEM_ALERT_WINDOW |
| `.admin` | "Fixture Admin" | an empty `DeviceAdminReceiver` with `BIND_DEVICE_ADMIN` and a policies xml (`<uses-policies/>`) |
| `.notif` | "Fixture Listener" | an empty `NotificationListenerService` subclass with `BIND_NOTIFICATION_LISTENER_SERVICE`, INTERNET |

Provide `scripts/install_fixtures.sh` (`adb install -r` all, for **team use only**; the end user never needs ADB) and a manual checklist `docs/FIXTURE_STEPS.md`: how to turn each special access on in Settings. These are the only apps used to verify special-access detection. **No real malware, ever.**
Done when: all 5 install on the emulator and show up in Settings → Apps.

### Task D4-4 — Sync 2: real data end to end (≈2:30)
Flip `AppContainer` to `RoomScanRepository`, `AndroidAppScanner`, `ScanOrchestrator`, `RuleBasedRiskEngine`, `AndroidSettingsLauncher`, `AndroidIconLoader`. Merge dev branches in order D3 → D2 → D4 → D1 and fix compile errors yourself (contract mismatches go back to the owner). Run a real scan with fixtures installed.
Done when: dashboard shows a real scan stored in Room; process death → data persists.

### Task D4-5 — Install monitoring + permission diff (3:00–4:00)
Files in `core/monitor/`; `Prefs.kt`.
- `Prefs` (SharedPreferences): `includeSystem` (default false), `monitoringEnabled` (default false), `lastKnownPackages: Map<pkg, lastUpdateTime>` stored as JSON.
- `PackageChangeMonitor.enable()/disable()`: registers a dynamic `BroadcastReceiver` for `ACTION_PACKAGE_ADDED` and `ACTION_PACKAGE_REPLACED` (data scheme `package`) in `RiskAuditorApp` while the process lives, and schedules `PackageChangeWorker` as unique periodic work (15 min). Do not rely on a manifest receiver for these actions on API 26+.
- Handler (shared by both paths): `scanOne(pkg)` → `engine.evaluate` → `previous = repository.latestAppRecord(pkg)` → `PermissionDiffer.diff` → store result into the latest scan row → notify if `level ∈ {HIGH, CRITICAL}` **or** diff non-empty. The worker also compares `PackageInfo.lastUpdateTime` against `lastKnownPackages` to catch changes while the process was dead.
- `NotificationHelper`: channel `"app_risk_alerts"` (importance default), `PendingIntent.FLAG_IMMUTABLE` deep-linking to the app detail. Notification text names the app and band only: "New app 'X' scored High risk" (no permission lists in the notification). Skip when `POST_NOTIFICATIONS` is not granted.
Done when: install the `.torch` fixture while monitoring is on → notification within 20 s with the app open or recently used; update scenario by reinstalling a fixture with an added permission → diff notification.

### Task D4-6 — Security review, docs, README (4:00–5:00)
- `docs/SECURITY_MODEL.md`: data collected (metadata only), where it's stored (app-private Room DB), no network, what is never accessed, what we can/can't observe (special-access table + AppOps caveat from D2), known false positives (MDM installs, enterprise admins), the QUERY_ALL_PACKAGES caveat, API-level differences.
- `docs/PROBLEM_STATEMENT_MAPPING.md`: Section 1 table, final.
- `README.md`: build/run, architecture diagram (text), how to run tests, limitations, cut features (hash lookup, ML, manifest checks) and why.
- Run the checklist in Section 10.2.

### Task D4-7 — Regression, release APK, rehearsal (5:00–6:00)
- Clean checkout build, `./gradlew clean :app:assembleDebug :app:testDebugUnitTest`.
- Install the **APK on the judging device/emulator at hour 5** (deliverable 1). Keep `app-debug.apk` plus a `SHA-256` text file in `/release/`.
- Run full test matrix (Section 10.1) on the judging device if available; log results in `docs/TEST_RESULTS.md` with real pass/fail, never aspirational.
- Two timed demo rehearsals.

---

## 9. Schedule and sync points

| Time | Dev 1 | Dev 2 | Dev 3 | Dev 4 | Sync |
|---|---|---|---|---|---|
| 0:00–0:25 | Read plan; sketch screens; prepare strings | Read API facts; plan emulator set-up (API 30 + 34) | **D3-1** mapper + tests (pure Kotlin, scratch module OK) | **D4-1** scaffold + contracts | **S0 @0:25**: everyone pulls `main`, builds |
| 0:25–1:00 | D1-1 theme/components | D2-1 reader + installer source | D3-1 push @0:40, then D3-2 start | D4-2 Room | |
| 1:00–1:45 | D1-2 dashboard | D2-2 permission extractor | D3-2 engine skeleton, D3-3 start | D4-2 finish | **S1 @1:30**: D3 mapper integrated in D2; D4 storage merged |
| 1:45–2:40 | D1-3 app list | D2-3 special access | D3-3 explanations, D3-4 rules start | D4-3 fixtures | |
| 2:30 | — | — | — | **D4-4 / S2**: flip to real data | **S2 @2:30**: real scan renders (even if rough) |
| 2:40–4:00 | D1-4 detail screen | D2-4 scanner, D2-5 orchestrator | D3-4 rules, D3-5 golden tests, D3-6 | D4-5 monitoring (from 3:00) | |
| 4:00–5:00 | D1-5 history/settings/polish | D2-6 hardening | D3-7 PDF, D3-8 rubric | D4-6 docs/security | **S3 @4:30**: feature freeze except bug fixes |
| 5:00 | All: stop new features | | | **Install APK on judging device** | **S4 @5:00 deliverable 1** |
| 5:00–6:00 | Bug fixes, demo walkthrough | Bug fixes on judging device | Rubric printout, demo narration | D4-7 regression, rehearsal | Final build @5:45 |

**If time slips, cut in this order (last = cut first):** PDF export → monitoring/diff → history UI → context mismatch → categories bars on dashboard. **Never cut:** inventory → declared/granted → special access → score + reasons → detail screen → APK on device.

---

## 10. Test plan

### 10.1 Manual/device matrix (record real results in `docs/TEST_RESULTS.md`)

| ID | Scenario | Expected |
|---|---|---|
| M-01 | Fresh install, open, never scanned | Empty state, no zeros as results |
| M-02 | Scan on device with ~100+ apps | Progress shown, finishes < 15 s, no crash |
| M-03 | System toggle off → on | List grows instantly; summary recomputes; no rescan |
| M-04 | Fixture `.torch` installed | Critical or High; ≥ 3 reasons; "Installed outside a store" visible |
| M-05 | `.a11y` installed, accessibility **off** | Accessibility = Off; low score |
| M-06 | `.a11y` accessibility turned **on** | State On after rescan; score up; Open accessibility settings button works |
| M-07 | `.overlay` installed with overlay permission off then on | Declared vs enabled shown separately, or "Couldn't check" if AppOps is unreadable (acceptable) |
| M-08 | `.admin` activated | Device admin On; COMBO_ADMIN_NON_MGMT fires |
| M-09 | `.notif` listener turned on | Listener On; correct finding |
| M-10 | App with permission declared but denied | "Asked for" + "Not allowed", lower points |
| M-11 | Kill app, relaunch | Latest scan and history still there |
| M-12 | Detail → Open app settings / Uninstall | Correct system screen; uninstall needs system confirmation; no silent action |
| M-13 | Airplane mode | Scan, scoring, history, PDF all work |
| M-14 | Cancel scan midway | Status Cancelled, UI recovers, rescan works |
| M-15 | Export PDF with long/odd labels | Opens, readable, no overflow |
| M-16 | Install app while monitoring on | Alert for High/Critical only |
| M-17 | Dark mode + font 1.5× + TalkBack on detail | No clipping; elements announced |
| M-18 | Two API levels (30 and 34) | No crash; differences recorded |
| M-19 | Own package in list | Scores Low |
| M-20 | Rotate during scan | Scan continues; state preserved |

### 10.2 Security checklist (Dev 4 signs off)
- [ ] `aapt dump permissions app-debug.apk` shows **no INTERNET**; only the three permissions in 2.1.
- [ ] `grep -r "Log\.\(d\|i\|v\|w\|e\)" app/src/main` reviewed: no inventory/result data logged.
- [ ] No hard-coded keys/URLs; no `http://` strings.
- [ ] Only `MainActivity` exported; `android:exported` set on everything with an intent filter; FileProvider not exported.
- [ ] `allowBackup=false`, no cleartext config.
- [ ] Notification `PendingIntent`s are `FLAG_IMMUTABLE`.
- [ ] PDF shared via FileProvider content URI, never a file path.
- [ ] Reports in cache dir only, old ones deleted.
- [ ] Every UNKNOWN/NOT_SUPPORTED state is rendered as not-safe.
- [ ] No fixture code or fixture APKs inside `:app`; no real malware anywhere.

---

## 11. Three-minute demo script (organizer test kit — their apps, not ours)

| Time | Action | Says (Q) |
|---|---|---|
| 0:00–0:15 | App already open on dashboard. "Everything runs on this phone; there is no internet permission." Tap **Scan my apps** | Privacy |
| 0:15–0:45 | Progress → results. Point at band cards and **"Apps that could do the most harm"** | **Q1** |
| 0:45–1:15 | Show the 9-category summary; tap one (e.g. Special access) to filter | Device-wide categories |
| 1:15–2:15 | Open the top-risk app. Scroll **Why flagged**: read the top 3 reasons, show evidence chips, toggle to special access states, point to "Asked for" vs "Allowed" | **Q2** |
| 2:15–2:40 | **What you can do now**: tap the settings shortcut, come back; show Uninstall (system confirm) | **Q3** |
| 2:40–3:00 | Show history scan and the scoring rubric one-pager, mention PDF export and install alerts if built | Rubric / extras |

Rehearse twice on the real kit. Have Dev 1 drive, Dev 3 narrate scoring if asked, Dev 2 answer API-limits questions ("AppOps can't always read overlay state; we show 'Couldn't check' instead of guessing"), Dev 4 handles the device.

---

## 12. Static text for Settings → Privacy & limits (D1 copies verbatim)

> **Everything stays on this phone.** This app reads which apps are installed and what access they ask for or have. It never reads your messages, contacts, photos, notifications, or file contents, and it has no internet permission, so nothing can be uploaded.
> **What the score means.** The score is a rule-based estimate of how much harm an app *could* do with the access it has. It is not proof that an app is malicious.
> **What we can't see.** Some Android settings can't be read for other apps. When that happens we show "Couldn't check" instead of guessing. Apps installed by your company or school may appear as "installed outside a store."
> **Package visibility.** To list all apps, this build declares a special permission (QUERY_ALL_PACKAGES) that Google Play restricts for published apps. It is allowed for this hackathon.

---

## 13. Definition of done

- [ ] Debug APK builds from a clean clone and installs on the judging device by hour 5
- [ ] Inventory has name, icon, package, version, install date, installer source, target SDK; system apps toggle works
- [ ] Declared vs granted shown with protection level
- [ ] All nine categories per app and device-wide
- [ ] Accessibility, device admin, notification listener, overlay, install-packages (and usage access) checked or shown as "Couldn't check"
- [ ] Score 0–100, four bands, deterministic, ≥ 3 reasons on every High/Critical (or capped to Medium with an explanation)
- [ ] Combination rules and context mismatch fire on fixtures (T1, T3, T8)
- [ ] Detail screen: Why flagged, app-settings button, uninstall button
- [ ] History persists across restart; offline works
- [ ] Unit tests green (`testDebugUnitTest`), manual matrix documented honestly
- [ ] `docs/RISK_SCORING.md` one page, `README.md`, `SECURITY_MODEL.md`
- [ ] Demo rehearsed twice; no INTERNET permission confirmed with `aapt`
