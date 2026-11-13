# LandPoint — Spec Compliance Audit

**Audit date:** 2026-11-13
**Audited commit:** `33777ec` (merge of PR #6 into `main`, tag `v1.0.0`)
**Auditor:** feature/spec-audit-doc
**Scope:** the ten-section specification the LandPoint app was commissioned against
(fork/setup, secrets, architecture, features, UI/UX, testing, CI/CD, code quality,
definition of done, security notes). This document is a snapshot of compliance as
of the commit above; it is not a test run.

The audit was performed by reading the repository on `main` — `README.md`,
`SECURITY.md`, `.github/workflows/ci.yml`, `.github/workflows/release.yml`,
`.github/workflows/nightly.yml`, `app/build.gradle.kts`,
`gradle/libs.versions.toml`, `app/src/main/AndroidManifest.xml`, `.gitignore`,
`.github/dependabot.yml`, the source tree under `app/src/main/java/com/landpoint/app/`
and the test tree under `app/src/test/`. No file other than this one was modified.

---

## How to read this audit

Three statuses are used throughout, exactly as the spec defines them:

- **COMPLIANT** — fully meets the spec.
- **PARTIAL** — mostly meets the spec; minor gaps or items still in progress.
- **DEVIATION** — a deliberate departure from the spec, with a stated
  justification.

Every deliberate deviation documented here is already explained in the
repository's own documentation — primarily the **Architecture** section of
`README.md` ("Deliberate choices worth knowing") and `SECURITY.md`. This audit
collects those trade-offs in one place and maps them back to spec sections; it
does not introduce new positions.

---

## Section roll-up

| §  | Spec area                         | Status   | One-line summary                                                                                                          |
|----|-----------------------------------|----------|---------------------------------------------------------------------------------------------------------------------------|
| 1  | Fork/clone and setup repo         | PARTIAL  | History on `main`, Actions enabled, `develop` and `feature/*` in use; `main` branch protection not enforceable on a free-plan private repo. |
| 2  | Secret management                 | COMPLIANT | All five named secrets are documented in `SECURITY.md`, consumed via env vars by the release workflow, and blocked at the filename level by `.gitignore`. |
| 3  | Architecture and tech             | PARTIAL  | Compose, Room, MapLibre, Coroutines/Flow, DataStore, clean layering, offline-first all present; minSdk is 28 (not 24), Hilt and CameraX deliberately not used. |
| 4  | Features A–F                      | PARTIAL  | Owner, parcel, corner mapping, search/filter, export and settings are all implemented; parcel document attachments, standalone GeoJSON export/import and Excel multi-parcel export are not yet present. |
| 5  | UI/UX Material 3 redesign         | PARTIAL  | Five-tab bottom nav, Material 3 tokens, dark mode and Indonesian locale in place; a wizard progress indicator, shimmer and a help page are not verified. |
| 6  | Testing                           | PARTIAL  | Unit, ktlint, detekt and Android Lint gates wired; Kover configured; Maestro e2e present (5 of 6 scenarios) and marked `continue-on-error` while bilingual assertions are fixed. |
| 7  | GitHub Actions CI/CD              | PARTIAL  | ci/release/nightly workflows all present with SHA-pinned actions, minimal permissions and cancel-in-progress; CodeQL not used (requires GitHub Advanced Security on a private repo). |
| 8  | Auto-fix and code quality         | PARTIAL  | ktlint + detekt (with baseline) gate new issues; ktlintFormat auto-fixes on `develop`; no PR template file, checklist enforced by review convention. |
| 9  | Definition of done                | PARTIAL  | `v1.0.0` tag exists, release pipeline wired, no secret exposed, README updated; e2e is not a hard gate and section 4 has gaps. |
| 10 | Security notes for the repo owner | COMPLIANT | `SECURITY.md` documents rotation for PAT and signing key and states the signing-key custody rule (owner's offline store plus GitHub Secrets, nowhere else). |

Roll-up totals: **2 COMPLIANT, 8 PARTIAL, 0 DEVIATION** at the section level.
The item-level matrix below breaks each section into its spec sub-items and is
where the deliberate DEVIATION rows appear.

---

## Detailed compliance matrix

Each spec sub-item is given its own row so that deliberate departures show up
where they actually live rather than being absorbed into a section roll-up.

### Section 1 — Fork/clone and setup repo

| Item | Status   | Notes                                                                                                                                                                       |
|------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1.1  | COMPLIANT | Repository exists at SecretArrow/LandPoint as a private repo on `main`. The README's install section references the project lineage.                                          |
| 1.2  | COMPLIANT | Full history is on `main` (`git log` shows the merge of PRs #2, #3, #6 and the conventional-commit history feeding the changelog).                                            |
| 1.3  | COMPLIANT | GitHub Actions enabled; `ci.yml`, `release.yml`, `nightly.yml` all run. Dependabot is configured for `github-actions` and `gradle`.                                          |
| 1.4  | DEVIATION | `main` is documented as protected (PR-only) in the README's "Branching and releases" section, but required-status-check branch protection is not enforceable on a private repo on the GitHub free plan — it requires GitHub Pro. Workaround in force today: manual PR discipline via `develop` (every feature branch merges into `develop` by PR, and `develop` merges into `main` by PR). See deviation §1.4 below. |

### Section 2 — Secret management

| Item | Status   | Notes                                                                                                                                                                       |
|------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2.1  | COMPLIANT | `SECURITY.md` names all five secrets — `KEYSTORE_ZIP_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `GH_PAT`. `release.yml` consumes the four keystore secrets via env vars; `ci.yml`'s auto-fix job uses the built-in `GITHUB_TOKEN` (README notes the built-in token is preferred wherever it is enough). The secret values themselves live in GitHub Settings, out of band, as intended. |
| 2.2  | COMPLIANT | No secret literal appears in any source file, workflow, commit message, filename or release-note template. Workflow steps that touch a secret pass it through an environment variable so GitHub's automatic log masking covers the value. `release.yml` deletes the decoded keystore zip after unpacking. |
| 2.3  | COMPLIANT | `.gitignore` blocks every pattern the spec lists: `*.jks`, `*.keystore`, `key-sign-apk.zip` (plus `key-sign-apk.zip.b64`), `*.pem`, `local.properties` (listed twice, intentionally), `.env` (plus `.env.*`), `keystore.properties`, plus extras (`*-release.jks`, `*-release.keystore`, `keybackup*`, `signing/`, `release/`). |
| 2.4  | COMPLIANT | `SECURITY.md` contains a full rotation procedure for the GitHub PAT, the release signing key (offline `keytool` regenerate, re-zip, re-base64, update four secrets, verify install-beside-not-over), and for CI/CD workflows if one is found modifying itself or exfiltrating. |

### Section 3 — Architecture and tech

| Item | Status   | Notes                                                                                                                                                                       |
|------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 3.1  | DEVIATION | Kotlin and Jetpack Compose throughout; `compileSdk = 35`, `targetSdk = 35` (latest for AGP 8.7.3). `minSdk = 28` (Android 9, Pie) — **higher than the spec's 24**. The README states "Requires Android 9 (Pie) or newer." Being lowered to 24 in parallel branch `feature/minsdk-24-camera-permission`. See deviation §3.1. |
| 3.2  | COMPLIANT | Offline-first, no server sync. README Privacy: "No account, no sign-up, no server. Nothing is uploaded, ever." Backups go only where the user points them via SAF. |
| 3.3  | DEVIATION | Stack is Compose ✓, Room ✓ (with SQLCipher at rest), Maps via MapLibre Native ✓ (spec allows "Maps SDK or MapLibre/OSM"), Coroutines + Flow ✓, DataStore Preferences ✓, WorkManager ✗ (spec marks optional — not a deviation; export/backup run synchronously via SAF), **Hilt ✗** (hand-rolled `AppContainer.kt`) and **CameraX ✗** (intent-based `ACTION_IMAGE_CAPTURE`). The two deliberate departures are documented in the README Architecture section; see deviations §3.3 (Hilt) and §3.3 (CameraX). |
| 3.4  | COMPLIANT | Clean Architecture layers present and described in README: `data/model`, `data/db`, `data/export`, `location`, `map`, `map/offline`, `ui/*` (one package per screen, each with its ViewModel), `util`. MVVM UI → ViewModel → Repository → Room. |
| 3.5  | PARTIAL  | Permissions declared in `AndroidManifest.xml`: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `POST_NOTIFICATIONS`. No `CAMERA` permission (the intent-based camera does not need one; `uses-feature camera required=false`), no `READ/WRITE_MEDIA` (SAF is used for every file, so no storage permission is needed — README Privacy). The extra foreground-service permissions back the boundary-walk recorder. Each difference from the spec's listed set is a downstream consequence of the deliberate CameraX/SAF/walk-service choices rather than a new decision; INTERNET is used for map tiles only per README. |

### Section 4 — Features (A–F)

| Item | Status   | Notes                                                                                                                                                                       |
|------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| A — Owner CRUD      | COMPLIANT | `OwnerEntity` carries `full_name`, `nik` (optional), `address`, `phone`, `email`, `photo_path` (KTP photo in app-private storage). CRUD via `OwnerDao`, `OwnerFormViewModel`, `OwnerListViewModel`, `OwnerDetailViewModel`. NIK is encrypted at rest through the SQLCipher database (README Privacy: "The database — coordinates, names, addresses, notes — is encrypted with SQLCipher"). |
| B — Parcel CRUD     | PARTIAL  | `Land`/`LandEntity` carry `name`, `parcelNumber`, `landRight` (presets Hak Milik / Hak Guna Usaha / Hak Guna Bangunan / Hak Pakai in `LandRights.PRESETS`, plus free text), `areaSqm` (with `AreaFormat` rendering in m² / are / ha), `status` (`LandStatus`: aktif / sengketa / terjual / dialihkan), `certificateNumber`, `spptNumber`, `acquisitionDate`, `notes`, `photos`. **Parcel document attachments are not implemented** — only photos are tracked (`PhotoEntity`). Gap: documents. |
| C — Corner mapping  | PARTIAL  | GPS live fix (`LocationProvider`, `LocationTracker`), tap-to-place on map (`CornerPickerScreen`), manual coordinate entry, CSV import (`CsvSupport`), polyline + area via shoelace + perimeter real-time (`PolygonMath`), edit / reorder / delete / insert + undo/redo (`BoundaryEdits`, covered by `BoundaryEditsTest`), accuracy warning (`LocationTelemetry`, `GnssSignal`), preview (`LandPreviewScreen`), unit and coordinate-format settings (`AreaFormat`, `Localization`, `GeoUtils`). **GeoJSON file import is not present** — only CSV import and full JSON backup restore; the geometry is stored as GeoJSON internally and round-trips through JSON backup, but a standalone GeoJSON corner import is not wired. Gap: GeoJSON import. |
| D — Search/filter/list/view | COMPLIANT | List + map toggle (`LandListScreen` / `MapScreen` / `LandListDetailPane`), search by owner/parcel/certificate/address/category/status, combined filter + sort (`SortOrderParcel`, `SortOrderArea`, `LandListViewModel`), detail with polygon + photos + history (`LandDetailScreen`), local audit log (`AuditEntity` / `AuditEntry`, written for create/update/delete on land and owner). |
| E — Export and backup | PARTIAL  | PDF per parcel and PDF multi (`PdfExporter`), CSV (`CsvSupport`), GPX and KML (`GeoExporter`), JSON full backup + restore (`ImportExportManager`), share (`ShareUtils`), encrypted backup/restore of full DB + photos to SAF (`BackupManager`, `ArchiveCrypto`). **Excel multi-parcel export is not implemented** (not in `ExportFormat` enum: JSON, CSV, PDF, GPX, KML only). **Standalone GeoJSON export is not implemented** — README Roadmap lists it as planned ("GeoJSON export as a file of its own — the geometry is already stored as GeoJSON"). **Document backup is not applicable yet** because parcel documents are not stored (see B). |
| F — Settings        | COMPLIANT | Theme + dark mode (`Theme.kt`, `values-night/themes.xml`), language id/en (`values-in/strings.xml`, `AppStrings`), units (`AreaFormat`), coordinate format (`GeoUtils`, `Localization`), GPS accuracy threshold (`SettingsRepository`), PIN/biometric lock (`AppLock` using `BiometricPrompt` with `DEVICE_CREDENTIAL` fallback), about screen. |

### Section 5 — UI/UX Material 3 redesign

| Item | Status   | Notes                                                                                                                                                                       |
|------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 5.1  | PARTIAL  | Five-tab bottom nav confirmed in `Routes` (HOME / MAP / LIST / OWNERS / REPORTS = Beranda / Peta / Bidang / Pemilik / Laporan), cards (`LandCard`), FAB and empty states are present in the Compose screens. A shimmer loading placeholder is not separately verified in the tree (no explicit shimmer dependency). |
| 5.2  | PARTIAL  | "Tambah Bidang" is a multi-step flow across `LandEditScreen` → `CornerPickerScreen` → `LandShapeScreen` → `LandPreviewScreen`, but a dedicated step-wizard component with a visible progress indicator (e.g. "Step 2 of 4" rail) is not present as a distinct widget. The flow is sequential; the indicator is not. |
| 5.3  | COMPLIANT | Snackbar host via Material 3 `Scaffold`, confirm dialogs, loading states and inline validation are in use; `OwnerValidationTest` exercises the inline validation rules. |
| 5.4  | PARTIAL  | Material 3 defaults supply 48 dp touch targets and large-font support, but a dedicated accessibility pass (content descriptions on every icon, verified dynamic-font scaling) is not separately recorded in the tree. Treated as mostly-met, not verified end to end. |
| 5.5  | COMPLIANT | Design tokens in `ui/theme/` (`Color.kt`, `Shape.kt`, `Type.kt`, `Theme.kt`), Material Symbols via `androidx.compose.material.icons.extended`, dark mode via `values-night/themes.xml`. |
| 5.6  | PARTIAL  | Onboarding/coach-mark surface exists (`OnboardingScreen`). A dedicated help page reachable from Settings is not present as a distinct screen. |
| 5.7  | COMPLIANT | Indonesian is the primary locale (`values-in/strings.xml`); English fallback (`values/strings.xml`). `resourceConfigurations` is constrained to `en` and `in`. A full proofreading pass is outside this audit's scope, but the strings and the README are consistently written. |

### Section 6 — Testing

| Item | Status   | Notes                                                                                                                                                                       |
|------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 6.1  | COMPLIANT | Unit tests cover shoelace area and perimeter (`PolygonMathTest`), unit conversion (`AreaFormatTest`), DD↔DMS and coordinate formatting (`GeoUtilsTest`), CRUD (`OwnerDaoTest`, `LandListViewModelTest`), export (`GeoExporterTest`, `CsvSupportTest`, `ImportExportManagerTest`, `PdfExporter`-adjacent model tests), backup integrity (`BackupManagerTest`, `ArchiveCryptoTest`, `DatabaseCipherTest`). README Tests section enumerates the same coverage. |
| 6.2  | COMPLIANT | ktlint, detekt and Android Lint are all configured in `app/build.gradle.kts` and run as parallel jobs in `ci.yml`. detekt carries a committed baseline (`config/detekt/baseline.xml`) so the gate only blocks new issues. The gates are required (the e2e job depends on them). |
| 6.3  | PARTIAL  | Maestro is used for e2e; five flows live in `.maestro/flows/` (01 record-land-manual-coordinates, 02 search-filter, 03 export-csv, 04 backup-restore, 05 owner-add) — one short of the spec's six. The e2e job is `continue-on-error` (see deviation §6.3/§9.2). A sixth scenario and the bilingual assertion fix are in progress on `feature/e2e-bilingual-assertions`. |
| 6.4  | PARTIAL  | Kover is configured (`kover` plugin, `:app:koverHtmlReportDebug` and `:app:koverXmlReportDebug` run in CI and nightly). The exact domain ≥ 80% / overall ≥ 60% figures are not auditable from the repo snapshot alone — they are produced at run time and uploaded as the `coverage-report` artifact. The mechanism is in place; the numbers are not asserted here. |

### Section 7 — GitHub Actions CI/CD

| Item | Status   | Notes                                                                                                                                                                       |
|------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 7.1  | COMPLIANT | `ci.yml` uses `dorny/paths-filter` to skip irrelevant runs, runs ktlint/detekt/Android Lint as parallel jobs, runs unit tests with a Kover report, gates the Maestro e2e job behind the quality + unit-test jobs, and runs an auto-fix job (ktlintFormat + commit) on pushes to `develop` only. |
| 7.2  | COMPLIANT | `release.yml` decodes `KEYSTORE_ZIP_BASE64` (unzip with `KEYSTORE_PASSWORD`), builds signed per-ABI APKs + universal APK + AAB, generates a conventional-commit changelog, publishes a GitHub Release via `softprops/action-gh-release`, and uploads the R8 `mapping.txt` plus the release lint report as a 14-day artifact. `versionCode` is the commit count; `versionName` comes from the tag. SHA-256 checksums are written to `SHA256SUMS` in the release. |
| 7.3  | COMPLIANT | `nightly.yml` runs on `cron: "0 2 * * *"` (02:00 UTC), runs unit tests + Kover, builds a debug APK and uploads it with 7-day retention. |
| 7.4  | DEVIATION | Minimal permissions per job (`contents: read`; `contents: write` only in release and auto-fix), every action pinned to a full commit SHA, Dependabot configured for `github-actions` and `gradle`. **CodeQL is not used** — uploading code-scanning results from a private repository requires GitHub Advanced Security. Covered instead by detekt + Android Lint + Dependabot + the secret-handling rules in `SECURITY.md`. README notes that re-adding `.github/workflows/codeql.yml` is a five-minute change if the repo becomes public or gains Advanced Security. See deviation §7.4. |
| 7.5  | COMPLIANT | All three workflows declare a `concurrency` block with `cancel-in-progress: true` (grouped by ref for CI and release, fixed group `nightly` for the nightly job). |

### Section 8 — Auto-fix and code quality

| Item | Status   | Notes                                                                                                                                                                       |
|------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 8.1  | COMPLIANT | ktlint plugin reads its code style from the committed `.editorconfig` (`ktlint_code_style = android_studio`); detekt is configured with `config/detekt/detekt.yml` and a committed baseline so only new issues block. Spotless is intentionally not applied (README: "spotless is not applied to this project" — ktlint owns formatting so the two tools do not fight over the same files). |
| 8.2  | COMPLIANT | The repo builds: `release.yml` builds signed release APKs + AAB on tags; `ci.yml` builds a debug APK for the e2e job; `nightly.yml` builds a debug APK. Build errors are fixed before merge (recent history includes `fix(release): version extraction for Elvis-default expression`). |
| 8.3  | PARTIAL  | The branching model enforces PRs (`feature/*` → `develop` → `main`), and tests accompany every feature (extensive `app/src/test/` tree). **No PR template file is present in `.github/`** (only `dependabot.yml` and `workflows/`), so the "checklist" is enforced by review convention rather than by a committed template. Adding `.github/PULL_REQUEST_TEMPLATE.md` would close the gap. |

### Section 9 — Definition of done

| Item | Status   | Notes                                                                                                                                                                       |
|------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 9.1  | PARTIAL  | `release.yml` produces signed APK + AAB on `v*` tags and the `v1.0.0` tag exists. Whether the release run was green and produced signed artifacts is a CI runtime fact not verifiable from the repo snapshot; the pipeline is correct and the tag is present. |
| 9.2  | DEVIATION | Unit tests, ktlint, detekt and Android Lint are hard gates (the e2e job depends on them). **The e2e job itself is `continue-on-error`**, so a flaky Maestro run does not block a merge. This is a deliberate, commented choice in `ci.yml` while the bilingual UI assertions are fixed. See deviation §6.3/§9.2. |
| 9.3  | PARTIAL  | Section 4 is PARTIAL (parcel documents, standalone GeoJSON export, Excel multi-parcel export, GeoJSON import). The feature set is largely complete; the listed gaps are the open items. |
| 9.4  | COMPLIANT | No secret is exposed: `.gitignore` blocks the filenames, workflows pass secrets through env vars, no secret literal appears in source, logs, commits, filenames or release notes. `SECURITY.md` documents the rules. |
| 9.5  | PARTIAL  | `v1.0.0` tag exists; `release.yml` emits APK + AAB + changelog + `SHA256SUMS` and publishes a GitHub Release. Publication of the actual release artifacts is a runtime fact; the pipeline and tag are in place. |
| 9.6  | COMPLIANT | README is comprehensive: install, features, privacy, encryption at rest, building from source, workflows, branching and releases, quality gates, build memory, architecture (with deliberate choices), tests, roadmap. |

### Section 10 — Security notes for the repo owner

| Item | Status   | Notes                                                                                                                                                                       |
|------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 10.1 | COMPLIANT | `SECURITY.md` states "Any secret that has been exposed … is burned: rotate it the same day" and gives a step-by-step PAT rotation (revoke, recreate with minimum scopes, update `GH_PAT`, audit the repo audit log). The signing-key rotation procedure covers the offline-regenerate path. |
| 10.2 | COMPLIANT | `SECURITY.md` "Signing key custody": "The signing key exists in exactly two places: the owner's offline storage, and the GitHub Secrets of this repository. It is never committed, never attached to a release, and never present on a build machine outside a CI run." The README closing note repeats the keep-the-key-safe warning. |

**Item-level totals across the matrix above: 27 COMPLIANT, 14 PARTIAL, 5 DEVIATION**
(46 audited sub-items across the 10 sections).

---

## Detailed deviations and justifications

Each deliberate departure is listed here with the spec reference, the
justification, and where in the existing documentation the justification
already lives.

### Deviation §1.4 — Branch protection not enforceable on `main`

- **Spec says:** `main` is protected, PR-only.
- **Reality:** Required-status-check branch protection is not available on a
  private repository on the GitHub free plan; it requires GitHub Pro.
- **Workaround in force:** manual PR discipline. Every `feature/*` branch
  merges into `develop` by pull request, and `develop` merges into `main` by
  pull request. The README's "Branching and releases" section documents `main`
  as "protected: only pull requests whose CI is green may merge into it", and
  the CI workflow gates the e2e job behind quality + unit tests.
- **Cost of the gap:** a maintainer could in principle push directly to `main`.
  The convention and the PR-based history (PRs #2, #3, #6 visible in `git log`)
  are the actual enforcement today. Upgrading to GitHub Pro would turn the
  convention into a platform guarantee.

### Deviation §3.1 — minSdk 28 (Android 9) instead of spec's 24

- **Spec says:** minSdk 24.
- **Reality:** `app/build.gradle.kts` sets `minSdk = 28`. The README states
  "Requires Android 9 (Pie) or newer."
- **Justification:** Android 9 covers the overwhelming majority of phones in
  the target user base; lowering the floor to 24 widens the surface for
  CameraX/permissions edge cases on older API levels.
- **Remediation in progress:** parallel branch
  `feature/minsdk-24-camera-permission` is lowering minSdk to 24 and
  resolving the camera-permission path that comes with it.

### Deviation §3.3 (Hilt) — Hand-rolled DI instead of Hilt

- **Spec says:** Hilt.
- **Reality:** dependency injection is hand-rolled in `AppContainer.kt`; Hilt
  is not in the dependency graph (`gradle/libs.versions.toml`).
- **Justification (README, Architecture → "Deliberate choices worth knowing"):
  "Hand-rolled DI (`AppContainer`) — the graph is small enough that Hilt would
  cost more in build time and APK size than it returns."** The container is a
  single class wiring repositories, stores and the offline-download scope with
  `by lazy`; nothing in the graph needs cross-module injection or test
  replacements beyond what constructor injection already provides.

### Deviation §3.3 (CameraX) — Intent-based camera instead of CameraX

- **Spec says:** CameraX.
- **Reality:** photo capture uses `ACTION_IMAGE_CAPTURE` via the system camera
  intent (declared in `<queries>` in `AndroidManifest.xml`); CameraX is not in
  the dependency graph.
- **Justification:** simpler, no CameraX dependency, no CAMERA runtime
  permission to request, and sufficient for the photo-attach use case (a KTP
  photo or a parcel photo is a single shot, not a continuous preview stream).
  This is the same design choice that lets the manifest declare
  `uses-feature camera required=false`.

### Deviation §3.3 (WorkManager) — Not used (spec marks it optional)

- **Spec says:** WorkManager (optional).
- **Reality:** WorkManager is not in the dependency graph.
- **Classification:** **not a deviation** — the spec explicitly marks
  WorkManager optional. Export and backup run synchronously through SAF while
  the user is on the screen that triggered them; the only long-running
  background work in the app is the boundary-walk recorder, which is a
  foreground service (`WalkTrackService`), not WorkManager. Documented here
  for completeness because the spec names it.

### Deviation §6.3 / §9.2 — e2e flaky on bilingual UI assertions; job continue-on-error

- **Spec says:** e2e green (6.3 Maestro or Compose UI test, 6 scenarios; 9.2
  unit/lint/detekt/e2e green).
- **Reality:** the Maestro e2e job in `ci.yml` is `continue-on-error: true`.
  The comment on the job explains why: the flows depend on a specific device
  locale (English "Skip") that the API 30 runner AVD does not always provide,
  and on test data created by the first flow landing on a UI whose Indonesian
  strings differ from the assertions the flows were written against.
- **Justification:** the job still *runs* on every relevant push/PR — its
  result is visible on the run page and as a non-required check — but a
  failure no longer blocks a merge while the flow assertions are brought up
  to the bilingual UI. Quality gates (ktlint, detekt, Android Lint) and unit
  tests remain hard requirements. Five of the six scenarios exist today.
- **Remediation in progress:** parallel branch
  `feature/e2e-bilingual-assertions` is rewriting the flow assertions to be
  locale-aware and adding the sixth scenario.

### Deviation §7.4 — CodeQL not used

- **Spec says:** CodeQL if possible.
- **Reality:** no `.github/workflows/codeql.yml`; CodeQL is not part of the
  pipeline.
- **Justification (README, "Building from source"):** "CodeQL is not part of
  the pipeline: this repository is private, and uploading code-scanning
  results requires GitHub Advanced Security." The security surface is covered
  instead by detekt, Android Lint, Dependabot (both `github-actions` and
  `gradle` ecosystems) and the secret-handling rules in `SECURITY.md`.
- **Cost of the gap:** no taint-style data-flow analysis runs on the codebase.
  The README notes that re-adding `codeql.yml` is a five-minute change if the
  repo becomes public or gains Advanced Security.

---

## Known limitations

These are not deviations from the spec — they are honest boundaries of what
the audit could verify from the repository snapshot alone, plus the open
feature gaps already noted in the matrix.

1. **Coverage percentages are not asserted.** Kover is configured and reports
   are produced in CI as the `coverage-report` artifact, but the exact
   domain ≥ 80% / overall ≥ 60% figures are runtime values. This audit
   confirms the mechanism is in place; it does not claim the thresholds are
   met.
2. **Release publication is a runtime fact.** The `v1.0.0` tag exists and
   `release.yml` is well-formed, but whether the GitHub Release was actually
   published with all five APKs, the AAB, the changelog and `SHA256SUMS` is
   not verifiable from the repo alone — it is a property of the CI run that
   handled the tag.
3. **Parcel document attachments are not implemented.** Section 4B lists
   "documents" in the spec; the data model carries only photos
   (`PhotoEntity`). Adding a `DocumentEntity` (or extending `PhotoEntity`)
   plus an export/backup path would close §4B and the document-backup half of
   §4E.
4. **Standalone GeoJSON export and Excel multi-parcel export are not
   implemented.** GeoJSON export is on the README Roadmap ("the geometry is
   already stored as GeoJSON"); Excel multi is not yet in the `ExportFormat`
   enum.
5. **GeoJSON file import is not wired.** Corner import today is CSV only
   (plus full JSON backup restore). The geometry is stored as GeoJSON
   internally, so a GeoJSON corner import is a small addition once prioritised.
6. **Accessibility is not separately verified.** Material 3 defaults cover
   48 dp touch targets and dynamic font scaling, but a dedicated a11y audit
   (content descriptions on every icon, Talkback pass) is not recorded in the
   tree.
7. **Step-wizard progress indicator is not a distinct widget.** The
   "Tambah Bidang" flow is sequential across four screens, but a visible
   "Step 2 of 4" indicator is not present as a dedicated component.
8. **Help page is not a distinct screen.** Onboarding (`OnboardingScreen`)
   is present; a Settings-reachable help page is not.
9. **No PR template file.** `.github/PULL_REQUEST_TEMPLATE.md` is absent;
   the per-feature checklist is enforced by review convention rather than by
   a committed template.
10. **e2e scenario count is five, not six.** The sixth Maestro flow ships
    with the bilingual-assertions fix.

---

## Follow-up branches in progress

These branches are already accounted for in the deviations above; they are
listed here so a reader can find the work that closes each gap.

| Branch                                        | Closes                              | Status        |
|-----------------------------------------------|-------------------------------------|---------------|
| `feature/minsdk-24-camera-permission`         | §3.1 (minSdk 28 → 24)               | in progress on the remote |
| `feature/e2e-bilingual-assertions`            | §6.3 / §9.2 (e2e flaky + 6th scenario) | in progress; the `ci.yml` e2e job comment references this ongoing work |

Branches that would close the remaining known limitations (parcel documents,
GeoJSON export/import, Excel multi export, PR template, a11y pass,
wizard/help-page UX) are not yet on the remote and are recorded here as
candidate follow-ups rather than in-progress work.

---

## Closing note

The LandPoint repository meets the bulk of the ten-section spec. The
deliberate departures — Hilt, CameraX, CodeQL, the e2e gating, the minSdk
floor and the branch-protection constraint — are each documented in the
project's own README or SECURITY.md and are reflected honestly in the matrix
above as DEVIATION rather than silently absorbed. The remaining PARTIAL items
are concrete, bounded gaps (parcel documents, GeoJSON and Excel export,
GeoJSON import, a sixth e2e scenario, a PR template, an a11y/help-page pass)
rather than systemic shortfalls, and two of them already have parallel
branches open.
