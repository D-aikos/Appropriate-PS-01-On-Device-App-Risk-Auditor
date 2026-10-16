# Master Prompt --- Android On-Device App Risk Auditor

> Use this prompt in Antigravity with the repository opened at its root.
> Replace `TEAM_NAME` with your team's name before initializing the
> repository.

## Repository identity

Create the repository with this exact naming pattern:

`Appropriate-PS-01-On-Device-App-Risk-Auditor`

Use the problem statement title as the project name: **On-Device App
Risk Auditor**.

## Role and objective

Act as a senior Android security engineer, Kotlin architect,
application-security reviewer, and pragmatic hackathon technical lead.
Initialize and implement a secure, maintainable Android application that
satisfies the uploaded **PS-01: On-Device App Risk Auditor** problem
statement.

This is a native Android app. Use Kotlin, Jetpack Compose, Android
platform APIs, coroutines/Flow where useful, and Room for local scan
history. Follow current stable versions supported by the installed
Android Studio/Gradle environment; do not invent dependency versions.
Inspect the existing project and toolchain first.

## Non-negotiable requirements

1.  **On-device and offline-first:** scanning, risk scoring,
    explanations, history, and reports work locally. Do not send
    inventory, package names, permission records, device details, scan
    results, APKs, or hashes to a cloud model/service. No Gemini API or
    remote agent in the runtime architecture.
2.  **No root or ADB required for end users.** Use supported Android
    APIs only. Do not attempt to bypass Android sandboxing, package
    visibility, signature permissions, or user-consent boundaries.
3.  **Honest visibility:** declare `QUERY_ALL_PACKAGES` only for this
    security-auditing use case and document distribution-policy
    implications. Never claim a check succeeded when Android does not
    expose the state. Represent unavailable or unobservable data
    explicitly as `UNKNOWN`/`NOT_SUPPORTED`, not `false`.
4.  **Declared vs granted:** distinguish manifest-declared permissions
    from runtime grants and from special access. Do not infer that a
    permission is granted because it is declared.
5.  **Required categories:** Special access (highest weight), SMS,
    Contacts and call logs, Camera and microphone, Location, Phone and
    identity, Storage and media, Background and persistence, Network.
6.  **Required inventory:** app label, icon where available, package
    name, version name/code, install/update metadata where available,
    installer source where available, target SDK, and a toggle to
    include system apps.
7.  **Required special-access checks:** enabled accessibility services,
    active device administrators, enabled notification listeners, and
    declared overlay/install-packages capability. Check usage access
    where feasible. Clearly distinguish declared capability from actual
    enabled/granted access.
8.  **Explainable risk score:** deterministic local score from 0--100,
    bands Low/Medium/High/Critical, documented scoring rubric, and at
    least three distinct evidence-backed reasons for each High/Critical
    result. Add combination rules, context mismatch heuristics, and
    sideloading context when reliable signals are available. Avoid
    double counting correlated findings.
9.  **App detail:** a "Why flagged" section, safe shortcut to the app's
    system details/settings page, and uninstall intent/action where
    supported. Never silently uninstall, disable, or change another
    app's settings.
10. **Local history:** store timestamped scan summaries and findings in
    Room. Store the minimum needed. No sensitive telemetry or logging of
    full inventories.
11. **PDF export:** share/save a readable report through Android's
    supported document/share APIs. Escape and sanitize text used in
    reports.
12. **Secure defaults:** least privilege, no exported components unless
    needed, explicit component export declarations, immutable
    PendingIntents where applicable, no hard-coded secrets, no WebView
    unless essential, no dynamic code loading, no unsafe
    deserialization, no unnecessary network permission, and no cleartext
    networking.
13. **Testing:** unit tests for scoring and categorization; instrumented
    tests for repository/UI paths where practical; test fixtures must be
    synthetic. Never include or distribute live malware.
14. **No fake security claims:** label heuristics as heuristics. A
    suspicious permission combination is not proof of malware. Explain
    evidence, confidence/limitations, and a user action for every
    finding.
15. **Do not silently replace the problem statement with a broader
    product.** Implement the required deliverable first; mark optional
    features as optional.

## Architecture

Use a simple, testable, layered architecture; do not over-engineer a
six-hour hackathon project.

-   `app/`: Android application entry point, navigation, Compose
    screens, theme.
-   `core/model/`: typed domain models and enums.
-   `core/platform/`: Android API adapters for PackageManager,
    permission state, special access, package settings intents.
-   `core/scanner/`: inventory collection and normalization.
-   `core/rules/`: deterministic scoring, category mapping, combination
    rules, explanations.
-   `core/storage/`: Room entities, DAO, database, repositories for scan
    history.
-   `core/report/`: PDF/report generation and safe sharing.
-   `feature/dashboard/`: device-wide overview and risk distribution.
-   `feature/apps/`: app list, filters, sort, app details.
-   `feature/history/`: past scans and comparison-ready summaries.
-   `feature/settings/`: include-system-apps toggle and
    privacy/limitations.
-   `docs/`: problem-statement mapping, security assumptions, scoring
    rubric, team implementation plan.
-   `prompts/`: master prompt and optional task prompts.
-   `app/src/test/`: unit tests.
-   `app/src/androidTest/`: Android instrumentation tests.

Prefer Gradle modules only if the existing build and timeline support
them. If a multi-module setup would risk the deadline, preserve these
boundaries as packages in one Android app module. Explain the choice in
`docs/ARCHITECTURE.md`.

## Data model requirements

Use typed Kotlin models internally. JSON is an export/debug/interchange
format, not the source of truth. Each app record should include:

-   identity: label, package name, version, target SDK, system/user app
    classification;
-   provenance: install source and install/update timestamps when
    available;
-   permissions: name, declared, granted state (`GRANTED`, `DENIED`,
    `NOT_RUNTIME`, `UNKNOWN`), protection level if available, category;
-   special access: each check represented as `ENABLED`, `DISABLED`,
    `NOT_DECLARED`, `UNKNOWN`, or `NOT_SUPPORTED` as appropriate;
-   findings: stable rule ID, title, severity, points, evidence,
    explanation, confidence, recommendation;
-   scan metadata: scan ID, timestamp, OS/API version, scanner version.

Do not put invented placeholder values in production scan results. Make
nullability and state enums explicit.

## Risk engine design

-   Scoring is deterministic and local. The same input fixture must
    yield the same score and findings.
-   Keep weights and thresholds in a documented `RiskPolicy`/ruleset,
    not scattered through UI code.
-   Apply category weights and explicit combination rules. Deduplicate
    overlapping evidence and cap the final score at 100.
-   Suggested initial bands (editable only with a documented rationale):
    Low 0--24, Medium 25--49, High 50--74, Critical 75--100.
-   Special access has the highest weight, as the problem statement
    requires.
-   Context mismatch is a heuristic; do not assume app label alone
    establishes app purpose. Use known metadata only when available.
-   Every High/Critical result must have at least three distinct
    reasons. If fewer than three independently supported reasons exist,
    either cap severity or explicitly explain the policy exception in
    tests/docs.
-   Add unit tests for boundary scores, duplicates, unknown states,
    combinations, and three-reason enforcement.

## Android implementation cautions

-   Check API-level availability and behavior for each Android API;
    guard version-specific calls.
-   `PackageManager` inventory/permission information is not equivalent
    to runtime grants or special access.
-   Accessibility and notification-listener state must be derived from
    supported system APIs/settings and must not expose private content.
-   Device-admin detection should use supported `DevicePolicyManager`
    APIs.
-   Overlay and package-install capabilities may have separate
    declaration and runtime/special-access semantics; represent these
    separately.
-   Some install timestamps, installer-source details, permission
    protection levels, and app metadata may be unavailable or
    restricted. Return unknown rather than fabricate.
-   Provide direct links to Android app details/settings where
    supported; do not try to toggle permissions programmatically without
    user action.
-   `QUERY_ALL_PACKAGES` is sensitive under Google Play policy. Document
    that a public Play release needs policy review; a hackathon build
    may follow the supplied brief.
-   Do not scan file contents, messages, photos, contacts, or user data.
    The task is metadata and access auditing.

## Build process --- follow in order

1.  Inspect repository, installed Android Studio/Gradle/JDK versions,
    and existing files. Preserve useful existing work.
2.  Create/update the repo structure and documentation. Do not overwrite
    user files without inspection.
3.  Write `docs/PROBLEM_STATEMENT_MAPPING.md`, mapping every requirement
    in the supplied PS-01 to a code area and test.
4.  Write `docs/ARCHITECTURE.md`, `docs/SECURITY_MODEL.md`, and
    `docs/RISK_SCORING.md`.
5.  Implement compilable vertical slices: app shell → inventory →
    permissions/special access → rules engine → dashboard/details →
    history → PDF.
6.  Add tests with each slice; use synthetic fixtures only.
7.  Run Gradle tests and assemble a debug APK using commands supported
    by the repo. Fix failures rather than claiming success.
8.  Review manifest, permissions, exported components, network
    configuration, logging, storage, and report sharing.
9.  End with a concise summary: files changed, features completed,
    commands run and actual results, known API/device limitations, and
    remaining work. Never claim a build or test passed unless it was
    actually run.

## Team workflow

Four people: - Developer 1: Frontend/dashboard and app details. -
Developer 2: Backend scanning and Android API adapters. - Developer 3:
Risk engine/"agent" (local deterministic rules, explanations,
scoring). - Developer 4: Android Studio integration, testing, and
validation.

Keep shared domain models and interfaces stable. Create task boundaries
and integration contracts before parallel implementation. Avoid
simultaneous edits to the same files. Developer 4 must be able to build
the project from a clean checkout and test on an emulator/device.

## Interaction rules for the coding agent

-   Work in small, verifiable steps. Start by inspecting the repository,
    then create a plan before editing.
-   Ask a question only if a missing choice blocks implementation;
    otherwise choose a conservative default and document it.
-   Do not generate a huge unverified code dump. Implement, compile,
    test, and iterate.
-   Do not remove security checks merely to make a build pass.
-   Do not use cloud agents, remote APIs, telemetry, or online
    threat-intelligence lookups in the MVP.
-   If the installed tool/model cannot complete the entire project in
    one pass, prioritize a compiling, demonstrable MVP and clearly list
    unfinished tasks.
