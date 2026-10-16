# PS-01 Implementation Plan --- On-Device App Risk Auditor

## Purpose and constraints

Build a working native Android APK for the supplied PS-01 problem
statement. This plan is temporary team coordination documentation and
can be removed before final submission if desired; keep the
architecture/security/scoring documentation that the judges need.

**Hard constraints** - Native Android, Kotlin, supported Android APIs;
no root or end-user ADB. - Scanning, risk scoring, history and PDF work
locally/offline. - No app inventory or scan results sent to Gemini or
any cloud service. - Do not collect app content or personal user data;
inspect app metadata and access signals only. - Unknown/unavailable
states are not equivalent to "safe." - No live malware samples. Use
synthetic fixtures and benign test apps. - The brief allows
`QUERY_ALL_PACKAGES` for the hackathon but notes Play policy
restrictions.

## Team ownership

  -----------------------------------------------------------------------
  Role              Primary owner     Owns              Must not
                                                        independently
                                                        change
  ----------------- ----------------- ----------------- -----------------
  Frontend          Developer 1       Compose UI,       Domain enums,
                                      navigation,       scoring rules,
                                      dashboard, app    scanner contracts
                                      detail, history   
                                      screens           

  Backend scanning  Developer 2       Package           UI navigation,
                                      inventory,        score thresholds
                                      permission        
                                      states,           
                                      special-access    
                                      adapters, scan    
                                      orchestration     

  Backend agent /   Developer 3       Local             Android UI,
  rules                               deterministic     platform API
                                      risk engine,      collection
                                      category mapping, 
                                      combination       
                                      rules,            
                                      explanations,     
                                      scoring tests     

  Testing /         Developer 4       Clean build,      Shared interfaces
  integration                         emulator/device   without
                                      validation, test  coordinating
                                      matrix,           first
                                      regression        
                                      checks, bug       
                                      triage            
  -----------------------------------------------------------------------

One person should be designated integration lead (ideally Developer 4 or
whoever has the most Android Studio experience). Each owner commits to a
branch and opens a small PR/merge request. Do not have multiple
developers edit the same file concurrently.

## Repository and package layout

Suggested repository name: `Appropriate-PS-01-On-Device-App-Risk-Auditor`

Use one Gradle app module if time is tight; keep code boundaries as
packages. Split into Gradle modules only if the project already supports
it and the team can integrate them confidently.

``` text
.
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/<package>/
│       │   │   ├── MainActivity.kt
│       │   │   ├── app/                 # app shell, navigation, theme
│       │   │   ├── core/model/          # typed models and state enums
│       │   │   ├── core/platform/       # Android API adapters
│       │   │   ├── core/scanner/        # inventory and scan orchestration
│       │   │   ├── core/rules/          # local scoring and explanations
│       │   │   ├── core/storage/        # Room database, DAO, repositories
│       │   │   ├── core/report/         # PDF export and safe sharing
│       │   │   └── feature/
│       │   │       ├── dashboard/
│       │   │       ├── apps/
│       │   │       ├── history/
│       │   │       └── settings/
│       │   └── res/
│       ├── test/                        # unit tests
│       └── androidTest/                 # instrumentation tests
├── docs/
│   ├── IMPLEMENTATION_PLAN.md            # this temporary team plan
│   ├── ARCHITECTURE.md
│   ├── SECURITY_MODEL.md
│   ├── RISK_SCORING.md
│   └── PROBLEM_STATEMENT_MAPPING.md
├── prompts/
│   └── MASTER_PROMPT.md
└── README.md
```

Adjust the package path to the team's chosen reverse-domain ID. Never
use a placeholder package name in a release build.

## Shared contracts --- agree before parallel coding

### Core states

Use explicit enums instead of booleans for data that may be unavailable:

-   `GrantState`: `GRANTED`, `DENIED`, `NOT_RUNTIME`, `UNKNOWN`,
    `NOT_SUPPORTED`
-   `SpecialAccessState`: `ENABLED`, `DISABLED`, `NOT_DECLARED`,
    `UNKNOWN`, `NOT_SUPPORTED`
-   `RiskLevel`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
-   `FindingConfidence`: `LOW`, `MEDIUM`, `HIGH`

### Minimum model shape

``` kotlin
data class AppAuditRecord(
    val packageName: String,
    val appLabel: String,
    val versionName: String?,
    val versionCode: Long?,
    val targetSdk: Int?,
    val isSystemApp: Boolean,
    val installerPackage: String?,
    val firstInstallTime: Long?,
    val lastUpdateTime: Long?,
    val permissions: List<PermissionAudit>,
    val specialAccess: List<SpecialAccessAudit>,
    val findings: List<RiskFinding>,
    val riskScore: Int,
    val riskLevel: RiskLevel,
    val scannedAtEpochMillis: Long
)
```

Treat this as a contract sketch. Developer 2 should confirm actual API
availability and model nullability before implementation. Developer 3
consumes these models and must not query Android APIs directly.
Developer 1 consumes view models/UI state and must not implement
scoring.

## Workstream A --- Frontend (Developer 1)

### Deliverables

-   Compose app shell and navigation.
-   Dashboard with scan status/progress, total apps scanned, risk-band
    counts, category summary and highest-risk apps.
-   App list with search, include-system-apps toggle, sorting/filtering
    by severity.
-   App detail with score, band, "Why flagged" evidence list,
    permissions grouped into categories, special-access states,
    recommendations, "Open app settings" action, and uninstall action
    where supported.
-   Scan history list and scan detail.
-   Loading, empty, partial-results, permission limitations, error and
    scan-cancel states.
-   Privacy/limitations screen or section explaining local processing
    and visibility limits.

### UI rules

-   Do not label unknown checks as safe.
-   Never display a score without the reasons/findings that produced it.
-   Clearly distinguish a permission being declared from being granted.
-   Use accessible labels, readable contrast, sensible text sizes and
    state descriptions.
-   UI must not block the main thread during scanning.
-   Use `ViewModel` + `StateFlow` or the existing project's equivalent;
    collect lifecycle-aware.
-   UI invokes repository/use-case interfaces, not `PackageManager`
    directly.

### Acceptance criteria

-   Dashboard updates when scan completes.
-   A user can open an app and understand why it was flagged.
-   App settings shortcut opens the correct system details page where
    supported.
-   History survives process restart.
-   Empty/error/unknown states are visible and not misleading.

## Workstream B --- Backend scanning (Developer 2)

### Deliverables

-   `AppInventoryScanner`: user-installed apps by default, optional
    system apps.
-   Metadata: label, icon if available, package name, version, target
    SDK, installer source and install/update times where available.
-   Permission extraction: declared permissions, runtime grant state,
    protection level/category where retrievable.
-   Special-access adapters:
    -   enabled accessibility services using supported settings/service
        metadata;
    -   active device admins via supported `DevicePolicyManager` APIs;
    -   enabled notification listeners via supported settings;
    -   overlay declaration vs overlay access state;
    -   install-packages declaration vs special access state;
    -   usage access state where feasible.
-   Scan orchestration with progress, cancellation if practical, partial
    failure handling and timestamps.
-   Unit-testable adapters or mapper functions where possible.

### Security/API rules

-   Inspect Android API level before using version-specific methods.
-   `PackageManager` declaration data is not proof of a runtime grant.
-   Do not request unrelated permissions.
-   Do not read notifications, messages, contacts, photos or file
    contents.
-   Do not attempt to silently grant/revoke special access or uninstall
    apps.
-   Use stable package IDs; do not rely on labels as identifiers.
-   Keep raw exceptions out of UI and logs. Return typed error/unknown
    states.
-   Do not log full package inventories.
-   Do not assume installer source is always available or trustworthy.
-   Declare `QUERY_ALL_PACKAGES` only as required for the supplied audit
    use case; document Play policy limitations.
-   For each special access signal, document exactly what is observed
    and what cannot be inferred.

### Acceptance criteria

-   Correctly distinguishes declared, granted, denied, and unknown.
-   Includes system apps only when toggle is on.
-   Handles missing metadata, permission failures and Android-version
    differences.
-   Returns partial results rather than crashing the entire scan.

## Workstream C --- Backend agent / local rules engine (Developer 3)

"Agent" means a local deterministic analyzer in this version, not an LLM
or cloud service.

### Required categories

1.  Special access --- highest weight
2.  SMS
3.  Contacts and call logs
4.  Camera and microphone
5.  Location
6.  Phone and identity
7.  Storage and media
8.  Background and persistence
9.  Network

### Deliverables

-   `PermissionCategoryMapper`
-   `RiskPolicy` with documented weights and thresholds
-   `RiskEngine.evaluate(AppAuditRecord)` or equivalent pure function
-   Single-signal rules and combination rules
-   Finding title, stable rule ID, severity/points, evidence,
    plain-language explanation, confidence, and recommended next action
-   Rule/scoring documentation and unit tests
-   Optional app-context mismatch heuristics only if reliable metadata
    is available

### Starting scoring proposal

Tune and document during implementation; this is a proposed team rubric,
not a weight table supplied by the problem statement.

-   Special-access evidence: 10--20 points depending on access and
    context.
-   SMS/contacts/call-log access: 5--15 points.
-   Camera/microphone/location: 3--8 points.
-   Background/persistence: 3--8 points.
-   Network permission alone: low or zero points; raise concern only in
    combinations.
-   Combination rule bonuses: 10--20 points for well-supported
    combinations.
-   Clamp final score to 0--100 and avoid counting the same evidence
    twice.

Suggested bands: Low 0--24, Medium 25--49, High 50--74, Critical
75--100.

Required examples: - SMS read + internet + boot persistence + sideloaded
provenance where each signal is known. - Accessibility + overlay. -
Active device admin on an app with no apparent management/security
purpose, but label the context inference as heuristic. - A simple
utility app requesting unrelated sensitive permissions, only when app
category/purpose metadata is reliable.

### Evidence and anti-hallucination rules

-   The rules engine---not an LLM---produces score and evidence.
-   No score based solely on an app name or a single generic permission.
-   `UNKNOWN` must not be treated as `DISABLED`.
-   A declared special capability and an enabled special access are
    separate findings.
-   Deduplicate overlapping rules.
-   Every High/Critical result needs at least three distinct
    evidence-backed reasons. Test this requirement explicitly; do not
    manufacture reasons to meet the count.
-   State that suspicious permissions are not proof of malware.

### Acceptance criteria

-   Pure scoring tests run without Android framework dependencies.
-   Identical input produces identical output.
-   Boundaries, unknown states, duplicate evidence and combinations are
    tested.
-   Every finding has a useful recommendation and points to observed
    evidence.

## Workstream D --- Testing and integration (Developer 4)

### Build/integration

-   Create a clean checkout/build in Android Studio.
-   Check JDK, Gradle, Android Gradle Plugin and SDK compatibility.
-   Confirm manifest package visibility and runtime behavior on a real
    emulator/device.
-   Integrate feature branches and resolve contract mismatches.
-   Run unit tests and instrumentation tests; record actual results.
-   Verify APK installs and launches from a clean install.
-   Confirm no cloud/network dependency is needed for a scan.

### Test matrix

  -----------------------------------------------------------------------
  Scenario                            Expected result
  ----------------------------------- -----------------------------------
  No third-party apps / empty         Empty state, no crash
  filtered list                       

  System-app toggle off/on            Inventory changes predictably

  Permission declared but not granted Display declared + denied/not
                                      granted, not granted

  Permission not runtime or state     `NOT_RUNTIME`/`UNKNOWN`, not false
  unavailable                         safe

  Accessibility enabled               Correct signal if visible to the
                                      app

  Overlay declared but access         Declared and enabled state shown
  disabled                            separately

  App missing installer metadata      Unknown provenance; no crash

  App metadata query fails            Partial scan result with
                                      explanation

  Duplicate rule evidence             No accidental score inflation

  SMS + internet + boot persistence   Combination rule fires predictably
  fixture                             

  High/Critical result                At least three distinct supported
                                      reasons

  Unknown special-access state        No false clean verdict

  Repeated scans                      History timestamps and records are
                                      correct

  PDF with long/unusual app labels    No crash, layout remains readable,
                                      text handled safely

  Offline mode                        Scan, scoring, history and export
                                      still work

  App details/uninstall actions       Correct system flow; no silent
                                      action

  App installed/updated after prior   New scan reflects change;
  scan                                monitoring is optional if time
                                      remains
  -----------------------------------------------------------------------

### Security review checklist

-   [ ] No network permission unless a documented requirement truly
    needs it.
-   [ ] No API keys, tokens, or secrets in source.
-   [ ] No inventory/result logging.
-   [ ] No exported components without a reason.
-   [ ] `android:exported` explicitly set for components with intent
    filters.
-   [ ] No cleartext traffic config.
-   [ ] PendingIntents immutable where applicable.
-   [ ] Local database contains only required scan metadata.
-   [ ] PDF sharing uses a safe content URI/FileProvider or supported
    document API, not a raw file path.
-   [ ] App cannot crash if an OS signal is unavailable.
-   [ ] No live malware samples or APK upload.
-   [ ] README explains limitations and `QUERY_ALL_PACKAGES` policy
    caveat.

## Integration contract and branch strategy

1.  Agree on domain models, state enums, package names and function
    signatures before implementation.
2.  Developer 2 owns the scanner contract and shares a sample synthetic
    result early.
3.  Developer 3 writes the rules engine against synthetic records before
    scanner integration is complete.
4.  Developer 1 builds UI against fake repository data first, then
    connects to the real repository.
5.  Developer 4 maintains the main branch, integration notes and
    regression checklist.
6.  Merge small changes frequently; keep `main` buildable.
7.  Never merge a feature that claims completion without build/test
    evidence.

## Suggested six-hour schedule

  -----------------------------------------------------------------------
  Time                    Milestone               Integration checkpoint
  ----------------------- ----------------------- -----------------------
  0:00--0:30              Repo skeleton, shared   App launches
                          models, build check     

  0:30--1:45              Inventory/permission    Synthetic scan renders
                          scanner + mock UI +     
                          rules baseline          

  1:45--3:00              Special access +        Real device scan works
                          scoring combinations +  
                          details                 

  3:00--4:00              Dashboard/history       End-to-end result
                          persistence             stored

  4:00--5:00              PDF export, settings    Debug APK rebuilt and
                          shortcut, security      installed
                          review                  

  5:00--6:00              Regression,             Clean final build +
                          judging-device test,    known limitations
                          demo and rubric         
  -----------------------------------------------------------------------

If time slips, prioritize: inventory → declared/granted permissions →
special access → deterministic score/explanations → app detail →
build/install → history → PDF → optional install monitoring.

## Definition of done

-   [ ] Debug APK builds and installs from a clean checkout.
-   [ ] All nine categories are represented.
-   [ ] App inventory has the required fields where available.
-   [ ] Declared and granted permissions are distinct.
-   [ ] Required special-access signals are checked or explicitly marked
    unavailable.
-   [ ] Risk score is deterministic, capped at 100 and explained.
-   [ ] High/Critical results have at least three distinct
    evidence-backed reasons.
-   [ ] App detail screen explains flags and offers system
    settings/uninstall actions where supported.
-   [ ] Dashboard and timestamped local history work offline.
-   [ ] PDF export works if time permits; stretch goal in the brief.
-   [ ] Unit tests pass; instrumentation/manual test results are
    documented accurately.
-   [ ] One-page scoring rubric and three-minute demo are ready.
-   [ ] Privacy, Android API limitations and package visibility policy
    caveats are documented.
