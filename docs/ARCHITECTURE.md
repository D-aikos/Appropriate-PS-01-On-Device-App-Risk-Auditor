# Architecture Document --- On-Device App Risk Auditor

## 1. High-Level Overview

**On-Device App Risk Auditor** (`com.appropriate.riskauditor`) is engineered as a privacy-preserving, 100% on-device Android application. It performs static inventory auditing, permission resolution, special-access detection, deterministic risk scoring, local history persistence, and PDF reporting without contacting any remote cloud server or external AI model.

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Jetpack Compose UI                            │
│   DashboardScreen  │  AppListScreen  │  AppDetailScreen │ HistoryScreen│
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ StateFlow / Actions
┌────────────────────────────────────▼───────────────────────────────────┐
│                             View Models                                │
│   DashboardViewModel │ AppListViewModel │ AppDetailViewModel           │
└──────────────┬─────────────────────────────────────┬───────────────────┘
               │                                     │
┌──────────────▼────────────────────┐ ┌──────────────▼───────────────────┐
│        ScanOrchestrator           │ │       ScanHistoryRepository      │
│   Coordinates inventory collection│ │   Manages local Room persistence │
└──────────────┬────────────────────┘ └──────────────┬───────────────────┘
               │                                     │
   ┌───────────┴───────────┐                         │
   ▼                       ▼                         ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────────────────┐
│  Platform Layer  │ │   Rules Engine   │ │        Storage Layer         │
│ - PackageManager │ │ - CategoryMapper │ │ - AppDatabase (Room)         │
│ - SpecialAccess  │ │ - CombinationRule│ │ - ScanHistoryDao             │
│ - SystemSettings │ │ - RiskScorer     │ │ - ScanHistoryEntity          │
└──────────────────┘ └─────────┬────────┘ └──────────────────────────────┘
                               │
                               ▼
┌────────────────────────────────────────────────────────────────────────┐
│                             Report Layer                               │
│  - PdfReportGenerator (Android PdfDocument)                            │
│  - ReportExporter (FileProvider + Intent.ACTION_SEND)                  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Layered Responsibilities & Package Boundaries

The codebase adheres strictly to clean architecture principles encapsulated within a single, highly performant Android module to prevent build overhead while enforcing clear domain separations:

### `core/model/`
- Pure Kotlin domain models and state enums.
- Zero Android platform dependencies (permitting instantaneous JVM unit testing).
- Enums:
  - `GrantState`: `GRANTED`, `DENIED`, `NOT_RUNTIME`, `UNKNOWN`, `NOT_SUPPORTED`
  - `SpecialAccessState`: `ENABLED`, `DISABLED`, `NOT_DECLARED`, `UNKNOWN`, `NOT_SUPPORTED`
  - `RiskLevel`: `LOW` (0–24), `MEDIUM` (25–49), `HIGH` (50–74), `CRITICAL` (75–100)
  - `FindingConfidence`: `LOW`, `MEDIUM`, `HIGH`
  - `FindingSeverity`: `INFO`, `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
  - `RiskCategory`: 9 required problem statement categories.
- Core Data Classes:
  - `AppInventoryRecord`: Raw normalized app metadata.
  - `PermissionAudit`: Individual permission declared vs granted state.
  - `SpecialAccessAudit`: Evaluated special platform capabilities.
  - `RiskFinding`: Evidence-backed finding with rule ID, points, explanation, and recommendation.
  - `AppRiskReport`: Aggregated audit result for a single application.
  - `DeviceScanSummary`: Comprehensive audit result across all audited apps on the device.

### `core/platform/`
- Android platform adapters isolating Android API queries (`PackageManager`, `DevicePolicyManager`, `Settings.Secure`, `AppOpsManager`).
- `PackageManagerAdapter`: Interfaces with `PackageManager.getInstalledPackages` and `PackageInfoCompat`.
- `SpecialAccessAdapter`: Checks active accessibility services, notification listeners, device administrators, system overlays, install-package capabilities, and usage stats.
- `SystemNavigator`: Constructs safe Android intents to launch system app settings (`ACTION_APPLICATION_DETAILS_SETTINGS`) and safe uninstallation (`ACTION_DELETE`).

### `core/rules/`
- 100% deterministic, offline risk scoring engine.
- `CategoryMapper`: Maps raw Android manifest permissions to the 9 mandatory problem-statement categories.
- `RiskPolicy`: Centralized constants for category weights, base scoring, combination rules, and threshold cutoffs.
- `CombinationRules`: Detects correlated threat patterns (Spyware, Toll Fraud, Audio Surveillance, Overlay Hijacking, Dropper/Sideload, Outdated SDK).
- `RiskScoringEngine`: Applies weights, executes combination rules, deduplicates findings, enforces the **Three-Reason Enforcement Policy** for High/Critical results, and clamps total score between 0 and 100.

### `core/scanner/`
- `AppScanner`: Orchestrates the collection of inventory and permission audits.
- Filters system apps based on user preferences.
- Feeds normalized models to the `RiskScoringEngine` to produce `AppRiskReport` items.

### `core/storage/`
- Local persistence using Android Room.
- `AppDatabase`: Room SQLite database instance.
- `ScanHistoryDao`: Queries for recent scans, scan details, and app summaries.
- `ScanHistoryEntity` & `ScanAppSummaryEntity`: Lightweight storage models. Stores only audit summaries and scores; never collects or persists sensitive personal data.

### `core/report/`
- `PdfReportGenerator`: Uses standard `android.graphics.pdf.PdfDocument` to construct clean, multi-page vector PDF security reports.
- `ReportExporter`: Saves reports to the secure cache directory and exposes them via Android `FileProvider` with read-only permissions for sharing via `Intent.ACTION_SEND`.

### `feature/` (Jetpack Compose UI)
- `feature/dashboard/`: Executive summary showing device posture, risk score gauges, and high/critical alerts.
- `feature/apps/`: Filterable, searchable list of audited apps sorted by risk, category, or install time.
- `feature/apps/detail/`: Granular breakdown of a single application: "Why Flagged" with 3+ reasons, permission matrix, special access cards, and safe system action buttons.
- `feature/history/`: Historical audit timeline, scan comparisons, and trend tracking.
- `feature/settings/`: User configuration (e.g. Include System Apps toggle, scoring methodology details, privacy statement).

---

## 3. Data Flow

1. **Inventory Collection (Background Dispatcher)**:
   `AppScanner` queries `PackageManager` with `GET_PERMISSIONS` and `MATCH_UNINSTALLED_PACKAGES`.
2. **Platform Signal Extraction**:
   For each package, `SpecialAccessAdapter` checks accessibility, device admin, notification listener, overlay, and install capabilities.
3. **Permission Normalization**:
   Manifest permissions are matched against runtime grants (`REQUESTED_PERMISSION_GRANTED`) and classified into `GrantState` and `RiskCategory`.
4. **Deterministic Evaluation**:
   `RiskScoringEngine` evaluates each application against individual category weights and cross-permission combination rules.
5. **Three-Reason Enforcement**:
   Any app scoring in the High (50–74) or Critical (75–100) range is checked for $\ge 3$ distinct evidence findings. If fewer exist, the score is capped at Medium (49) to eliminate unexplainable false positives.
6. **Presentation & Persistence**:
   Results are emitted as a `StateFlow<ScanState>` to Compose UI, and saved as a `ScanHistoryEntity` in Room for audit trails.
7. **Export**:
   Upon user request, `PdfReportGenerator` renders the evaluated `DeviceScanSummary` into a PDF document and invokes the Android system share sheet.
