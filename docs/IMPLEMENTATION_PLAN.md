# LandPoint — UI/UX Redesign Implementation Plan

**Status:** Living document
**Audience:** Contributors to the LandPoint Android app
**Scope:** The phase-by-phase rollout of the redesign described in `docs/DESIGN_SYSTEM.md` and the ADRs in `docs/DECISIONS.md`.

This file breaks the redesign into ten tagged releases. Each phase is a `v*` tag on `main` (per the branching rules in `README.md`), and each phase ships independently — a user on `v1.0.2` and a user on `v1.1.0` see the app get better between releases, not all at once. The current round of work is **Phase 1**.

Each phase entry below carries: **Scope** (what changes for the user), **Files touched** (what changes in the repo), **Risk level** (low / medium / high, with the reason), and **CI implications** (what jobs change, what new jobs appear). Phases are sequenced so that the highest-value, lowest-risk work lands first.

---

## Conventions

- **Phase = tagged release.** A phase is done when its tag is cut on `main` and the release workflow (`.github/workflows/release.yml`) has produced the APKs.
- **One feature branch per phase, or per cohesive sub-deliverable.** Long-lived feature branches are not used; each merges into `develop` by PR before the tag.
- **CI is the gate.** ktlint, detekt, Android Lint, unit tests with Kover, and Maestro e2e must be green on `develop` before a tag is cut. Phase 8 onward adds Macrobenchmark and screenshot-test gates.
- **Maestro flows are bilingual (en + in).** See ADR-008. Every new flow follows the `^(English|Indonesian)$` assertion pattern.
- **No direct pushes to `main` or `develop`.** See ADR-009.

---

## Phase 1 — v1.0.2 (current round)

### Scope

Foundational documentation and the first visible redesign work:

1. **Design system docs** — `docs/DESIGN_SYSTEM.md`, `docs/DECISIONS.md`, `docs/IMPLEMENTATION_PLAN.md` (this file).
2. **Corner marking toolbar redesign** — the source picker in `CornerPickerScreen.kt` (today a row of buttons: GPS / Tap map / Walk / Type / Distance-bearing / Copy) is rebuilt as a bottom sheet with icon-led entries and a primary "GPS" affordance. The active mark's state ("Corner 4", "Insert between 2 and 3") is shown above the sheet per `PendingMark.kt` and `CornerDraft.kt`.
3. **Undo snackbar for delete** — already wired through `PendingDeletes.kt`; this phase promotes the snackbar to the design-system spec (action label, duration) and adds it to the bulk-delete path.
4. **Android skills survey** — record which skills apply to which phase (ADR-007). No code change; the survey is `DECISIONS.md` §ADR-007 and the per-phase rows below.

### Files touched

| Path | Change |
|---|---|
| `docs/DESIGN_SYSTEM.md` | New |
| `docs/DECISIONS.md` | New |
| `docs/IMPLEMENTATION_PLAN.md` | New |
| `app/src/main/java/com/landpoint/app/ui/edit/CornerPickerScreen.kt` | Redesign source picker as bottom sheet |
| `app/src/main/java/com/landpoint/app/ui/components/LandMapOverlay.kt` | Document z-order constants (no behavior change) |
| `app/src/main/java/com/landpoint/app/PendingDeletes.kt` | Snackbar duration + action label per design system |
| `app/src/main/res/values/strings.xml` + `values-in/strings.xml` | New strings for the bottom-sheet source labels (en + in) |
| `.maestro/flows/01-record-land-manual-coordinates.yaml` | Update to match the new source picker |

### Risk level

**Low.** The corner picker change is local to one screen and one Maestro flow. The design-system docs are non-functional. No DB migration, no new permission, no new dependency.

### CI implications

None new. The existing `ci.yml` lint/test/maestro jobs cover the changes; the Maestro `01` flow is expected to need an update and is run with `continue-on-error: true` (ADR-008).

---

## Phase 2 — v1.1.0

### Scope

The parcel preview hero, component gallery, and the first wave of design-system tokens:

1. **Parcel preview hero** — `LandPreviewScreen.kt` is redesigned with a collapsing header, an animated polygon draw-in (`Motion.slow`, 350ms), corner cards listing each corner's coordinates (monospace, `CoordinateStyle`) and accuracy, and a quick-actions bar (Navigate / Share / Export / Edit).
2. **Component gallery screen** (debug builds only) — a new route reachable from a debug-only menu entry, showing every component in the design system in light and dark. Not shipped in release APKs.
3. **New theme tokens land** — `Spacing.kt`, `Elevation.kt`, `Motion.kt`, `TextStyleExt.kt`, `ParcelStatusVisual.kt`, and the `success` / `warning` / `info` / `accent` / `map.overlay.*` color tokens (see `docs/DESIGN_SYSTEM.md` §2, §8).

### Files touched

| Path | Change |
|---|---|
| `app/src/main/java/com/landpoint/app/ui/theme/Spacing.kt` | New |
| `app/src/main/java/com/landpoint/app/ui/theme/Elevation.kt` | New |
| `app/src/main/java/com/landpoint/app/ui/theme/Motion.kt` | New |
| `app/src/main/java/com/landpoint/app/ui/theme/TextStyleExt.kt` | New |
| `app/src/main/java/com/landpoint/app/ui/theme/ParcelStatusVisual.kt` | New |
| `app/src/main/java/com/landpoint/app/ui/theme/Color.kt` | Add `success` / `warning` / `info` / `accent` / `map.overlay.*` |
| `app/src/main/java/com/landpoint/app/ui/theme/Theme.kt` | Wire new tokens into light & dark schemes |
| `app/src/main/java/com/landpoint/app/ui/preview/LandPreviewScreen.kt` | Hero redesign |
| `app/src/main/java/com/landpoint/app/ui/components/ParcelHero.kt` | New |
| `app/src/main/java/com/landpoint/app/ui/components/StatusChip.kt` | New |
| `app/src/main/java/com/landpoint/app/ui/gallery/ComponentGalleryScreen.kt` | New (debug only) |
| `app/src/main/java/com/landpoint/app/ui/navigation/Routes.kt` + `LandPointNavHost.kt` | Debug-only gallery route |

### Risk level

**Medium.** The polygon draw-in animation and the collapsing header are the first motion-heavy features; they touch Compose performance for the first time and need profiling on a low-end device. The new tokens are referenced by new code only — existing screens keep working because the M3 defaults are unchanged.

### CI implications

- The `jetpack-compose-performance` skill (ADR-007) is applied here; the gallery screen doubles as a profiling target.
- A debug-APK-only screenshot of the gallery is captured by the nightly job for manual review.
- No new CI job yet; the screenshot gate lands in Phase 7.

---

## Phase 3 — v1.1.1

### Scope

Soft delete, bulk operations, and swipe gestures:

1. **Recently deleted bin** — a parcel deleted from detail or from the list is soft-deleted (`deletedAt: Long?`) and kept for 30 days. A "Recently deleted" entry in Settings lists them with restore and purge actions. A daily `WorkManager` cleanup purges entries older than 30 days.
2. **Bulk select** — long-press a parcel row to enter selection mode; a bulk action bar appears with Export selected / Delete selected / Clear selection. Strings already exist (`action_select_all`, `action_delete_selected`, `selection_count`, `msg_deleted`).
3. **Swipe actions on list rows** — swipe left to delete (with the Undo snackbar), swipe right to archive-into-recently-deleted directly.

### Files touched

| Path | Change |
|---|---|
| `app/src/main/java/com/landpoint/app/data/model/LandEntity.kt` | Add `deletedAt: Long?` |
| `app/schemas/com.landpoint.app.data.db.LandDatabase/2.json` | New schema |
| `app/src/main/java/com/landpoint/app/data/db/LandDatabase.kt` | Bump version, add `Migration(1, 2)` |
| `app/src/main/java/com/landpoint/app/data/db/LandDao.kt` | Queries filter `deletedAt IS NULL` by default; new `recentlyDeleted()` flow |
| `app/src/main/java/com/landpoint/app/data/LandRepository.kt` | Soft-delete + restore + purge methods |
| `app/src/main/java/com/landpoint/app/PendingDeletes.kt` | Interacts with soft-delete rather than hard-delete |
| `app/src/main/java/com/landpoint/app/ui/lands/LandListScreen.kt` + `LandRow.kt` | Selection mode, swipe actions |
| `app/src/main/java/com/landpoint/app/ui/lands/LandListViewModel.kt` | Selection state, bulk actions |
| `app/src/main/java/com/landpoint/app/ui/settings/SettingsScreen.kt` | "Recently deleted" entry |
| `app/src/main/java/com/landpoint/app/ui/settings/RecentlyDeletedScreen.kt` | New |
| `app/src/main/java/com/landpoint/app/data/RecentlyDeletedWorker.kt` | New `WorkManager` daily cleanup |
| `app/src/main/res/values/strings.xml` + `values-in/strings.xml` | Recently-deleted strings (en + in) |
| `.maestro/flows/` | New flow: delete → undo → recently-deleted restore |

### Risk level

**Medium-high.** A Room migration is irreversible once shipped; the v2 schema and the `Migration(1, 2)` must be tested against a v1 database fixture (already the pattern in `DatabaseCipherTest.kt`). The 30-day purge must be deterministic and idempotent.

### CI implications

- Unit tests cover: migration on a populated v1 DB, the `recentlyDeleted()` flow, the worker's idempotent purge, the bulk-delete + undo round-trip.
- A new Maestro flow covers the recently-deleted restore path (bilingual).

---

## Phase 4 — v1.2.0

### Scope

Onboarding, coach marks, and in-app help:

1. **Onboarding redesign** — three skippable steps (Welcome / Measure / Private) plus a permission-rationale screen for location, with "Grant" and "Not now" paths. The current `OnboardingScreen.kt` already has the content; this phase redesigns the chrome (stepper, illustrations, primary-button placement).
2. **Coach marks** — first-time-use overlays pointing at the corner picker's source buttons, the map's floating toolbar, and the parcel preview's quick-actions bar. Dismissable, with a "Don't show again" affordance persisted in `SettingsRepository` (a new `coachmark_*_seen` boolean per mark).
3. **In-app help center** — a static help screen with sections matching the app's features (Capture / Measure / Map / Offline / Privacy / Export). No network; content is a markdown asset bundled in the APK.

### Files touched

| Path | Change |
|---|---|
| `app/src/main/java/com/landpoint/app/ui/onboarding/OnboardingScreen.kt` | Redesign chrome |
| `app/src/main/java/com/landpoint/app/ui/components/CoachMark.kt` | New |
| `app/src/main/java/com/landpoint/app/ui/components/Stepper.kt` | New |
| `app/src/main/java/com/landpoint/app/ui/help/HelpCenterScreen.kt` | New |
| `app/src/main/assets/help/en.md` + `help/in.md` | New bundled markdown |
| `app/src/main/java/com/landpoint/app/data/SettingsRepository.kt` | `coachmark_*_seen` keys |
| `app/src/main/res/drawable/` | Onboarding illustrations (3, monochrome) |
| `app/src/main/res/values/strings.xml` + `values-in/strings.xml` | Onboarding chrome strings, coach-mark text, help-center entry |
| `.maestro/flows/01-record-land-manual-coordinates.yaml` | Update for new onboarding chrome |

### Risk level

**Medium.** Onboarding is the first screen a new user sees; a regression here is high-visibility. The permission-rationale screen must handle the "permission previously denied" and "permission permanently denied" branches correctly (the `permissions-security` skill applies — ADR-007).

### CI implications

- Existing Maestro `01` flow's onboarding-skip step may need updating; bilingual assertions stay.
- The `permissions-security` skill is applied to the rationale flow.

---

## Phase 5 — v1.2.1

### Scope

Settings redesign and Tools screen:

1. **Settings redesign** — grouped (Appearance / Location / Privacy & security / Data / Offline maps / About), searchable (a `SearchBar` at the top filters entries by keyword), with live previews for theme, units, coordinate format, and area unit. The current `SettingsScreen.kt` is a flat list; this phase restructures it.
2. **Tools screen** — replaces the Reports tab (ADR-002). A card grid: Export / Backup / Restore / Reports / Offline maps / Compass / Help / About. Each card is an `E1` card with an icon, a title, and a one-line summary.
3. **Bottom-bar tab restructure** — the four-tab structure (Map / Parcels / Tools / Settings) lands here, replacing the current five tabs (ADR-002).

### Files touched

| Path | Change |
|---|---|
| `app/src/main/java/com/landpoint/app/ui/settings/SettingsScreen.kt` | Redesign: grouped + searchable + previews |
| `app/src/main/java/com/landpoint/app/ui/settings/SettingsViewModel.kt` | Search state, live-preview state |
| `app/src/main/java/com/landpoint/app/ui/tools/ToolsScreen.kt` | New (card grid) |
| `app/src/main/java/com/landpoint/app/ui/LandPointNavHost.kt` | Four-tab restructure, central Add speed-dial |
| `app/src/main/java/com/landpoint/app/ui/components/AddSpeedDial.kt` | New |
| `app/src/main/java/com/landpoint/app/ui/navigation/Routes.kt` | Drop `HOME`, `REPORTS`; add `TOOLS` |
| `app/src/main/java/com/landpoint/app/ui/home/HomeScreen.kt` | Removed; content moves to Parcels tab header |
| `app/src/main/java/com/landpoint/app/ui/home/HomeViewModel.kt` | Repurposed as Parcels header |
| `.maestro/flows/02-search-filter.yaml`, `05-owner-add.yaml` | Update for new tab structure |
| `app/src/main/res/values/strings.xml` + `values-in/strings.xml` | Tools-screen strings, settings-group strings |

### Risk level

**Medium-high.** The tab restructure touches every Maestro flow (the bottom bar is the entry point for all of them) and is the most visible IA change in the redesign. Done here rather than earlier so that the Settings and Tools screens are ready to receive their new tab destinations.

### CI implications

- The `navigation-3` skill (ADR-007) is applied to the tab restructure.
- All five existing Maestro flows are re-validated; new flows added for Tools-screen entry points.
- The bilingual assertions stay; the bottom-bar labels change in both `strings.xml` and `values-in/strings.xml`.

---

## Phase 6 — v1.3.0

### Scope

Tablet and foldable support:

1. **Two-pane list-detail** on the Parcels tab from 840dp (already prototyped in `LandListDetailPane.kt`; this phase extends it to all detail-style screens).
2. **Adaptive edge-to-edge** — `enableEdgeToEdge()` is already on; this phase ensures every screen handles the system-bar insets correctly in two-pane (the detail pane's top app bar must not underlap the status bar's tinted icons).
3. **Foldable support** — half-opened hinge as a natural divider between list and detail; the map's floating toolbar reflows above the hinge. Uses `WindowInfoTracker` from `androidx.window`.

### Files touched

| Path | Change |
|---|---|
| `app/src/main/java/com/landpoint/app/ui/WindowLayout.kt` | Add foldable-aware layout decision |
| `app/src/main/java/com/landpoint/app/ui/lands/LandListDetailPane.kt` | Extend to all detail screens |
| `app/src/main/java/com/landpoint/app/ui/detail/LandDetailScreen.kt` | Two-pane-aware |
| `app/src/main/java/com/landpoint/app/ui/edit/LandEditScreen.kt` | Two-pane-aware |
| `app/src/main/java/com/landpoint/app/ui/map/MapScreen.kt` | Foldable-aware floating toolbar |
| `app/build.gradle.kts` | Add `androidx.window.window` dependency |
| `.maestro/flows/` | New flow running on a tablet-emulator matrix |

### Risk level

**Medium.** The two-pane prototype exists; the work is extending it. Foldable support is the new surface area; the `adaptive` skill (ADR-007) applies.

### CI implications

- A second emulator is added to the Maestro job matrix (API 33, 1024dp window). Maestro runs on both; the existing API 30 phone emulator stays.
- Unit tests in `WindowLayoutTest.kt` extend to cover the new foldable decision.

---

## Phase 7 — v1.3.1

### Scope

Visual regression in CI:

1. **Screenshot tests** for key screens in light + dark, en + in. Uses Paprika (or Compose `preview-screenshot` if available at the time). Screenshots are committed to the repo as a baseline; a PR that changes a screenshot surfaces the diff in the PR check.
2. **Covered screens:** Parcels list (empty + populated), parcel preview hero, corner picker, map (street + satellite), onboarding (3 steps), settings (top level), tools grid.
3. **Maestro stabilization** — the `continue-on-error` flag (ADR-008) is removed once the screenshot gate is in place, because the screenshot gate catches the label-rename regressions Maestro was tolerating.

### Files touched

| Path | Change |
|---|---|
| `app/src/test/java/com/landpoint/app/ui/screenshot/*ScreenshotTest.kt` | New screenshot tests |
| `app/src/main/java/com/landpoint/app/ui/theme/` | Screenshot-test previews (light/dark/en/in parameterized) |
| `.github/workflows/screenshot.yml` | New workflow: build, capture, diff, upload on PR |
| `.github/workflows/ci.yml` | Remove Maestro `continue-on-error` |
| `app/build.gradle.kts` | Screenshot-test plugin / dependency |

### Risk level

**Low-medium.** Pure tooling; no user-facing change. The risk is the screenshot flakiness — fonts, anti-aliasing, and emulator GPU drivers can produce non-deterministic diffs. Mitigated by running on the headless AOSP emulator image with software rendering.

### CI implications

- New `screenshot.yml` workflow runs on PRs touching `app/src/main/java/com/landpoint/app/ui/**`.
- CI runtime grows by ~5 minutes per PR for the screenshot capture.
- The `testing-setup` skill (ADR-007) applies.

---

## Phase 8 — v1.4.0

### Scope

Performance budgets enforced in CI:

1. **Macrobenchmark** — `ColdStartupBenchmark` and `ScrollFramesBenchmark` modules under `benchmark/`.
2. **Baseline Profile** — generated by `BaselineProfileGenerator`, shipped as `app/src/main/baseline-prof.txt`, consumed by `androidx.profileinstaller`.
3. **APK size gate** — `./gradlew assembleDebug` + a size-check job that fails if the universal debug APK exceeds `main` + 2MB (ADR-010).
4. **R8 keep-rule audit** — the `r8-analyzer` skill (ADR-007) is applied to confirm SQLCipher, MapLibre, and Room's generated classes have minimal, correct keep rules.

### Files touched

| Path | Change |
|---|---|
| `benchmark/build.gradle.kts` | New module |
| `benchmark/src/main/java/com/landpoint/app/benchmark/ColdStartupBenchmark.kt` | New |
| `benchmark/src/main/java/com/landpoint/app/benchmark/ScrollFramesBenchmark.kt` | New |
| `benchmark/src/main/java/com/landpoint/app/benchmark/BaselineProfileGenerator.kt` | New |
| `app/src/main/baseline-prof.txt` | New |
| `app/build.gradle.kts` | `profileinstaller` dependency, baseline-profile consumer |
| `app/proguard-rules.pro` | Audited keep rules |
| `.github/workflows/benchmark.yml` | New: run Macrobenchmark on `develop` + PRs touching `app/src/main/**`, upload results |
| `.github/workflows/ci.yml` | APK size gate job |

### Risk level

**Medium.** Macrobenchmark results are noisy on shared CI runners; the gates need a tolerance band and a few weeks of data to set realistic thresholds (ADR-010). The Baseline Profile itself is low risk.

### CI implications

- A new `benchmark.yml` workflow on a Google-APIs emulator (not the AOSP image used by Maestro).
- CI runtime grows by ~10 minutes per `develop` push.
- Gates (cold start ≤ 800ms p95, frame time ≤ 16ms p90, APK ≤ main + 2MB) become hard failures on `develop` and `main`.

---

## Phase 9 — v1.4.1

### Scope

CameraX migration for in-app patok photo capture (ADR-004):

1. **In-app viewfinder** — a `PreviewView` (CameraX `Preview` use case) inside a Compose `AndroidView`, with a coordinate overlay showing the current GPS fix, accuracy, and a target reticle.
2. **Tap-to-focus** — `FocusMeteringAction` triggered by a tap on the viewfinder.
3. **Capture path** — `ImageCapture` use case writes a JPEG to `PhotoStore`, which stamps it via `PhotoStamper` exactly as the intent path does today. The intent-based path stays as a fallback.
4. **Permission flow** — the camera permission rationale is added to onboarding (Phase 4 laid the groundwork) and re-requested on first use of the in-app capture.

### Files touched

| Path | Change |
|---|---|
| `app/src/main/java/com/landpoint/app/ui/capture/PatokCaptureScreen.kt` | New |
| `app/src/main/java/com/landpoint/app/ui/capture/PatokCaptureViewModel.kt` | New |
| `app/src/main/java/com/landpoint/app/ui/capture/CameraXController.kt` | New (wraps `ProcessCameraProvider`) |
| `app/src/main/java/com/landpoint/app/data/PhotoStore.kt` | Accept `ImageProxy`-to-JPEG path alongside the existing intent URI path |
| `app/src/main/java/com/landpoint/app/data/PhotoStamper.kt` | No change (already takes a file path) |
| `app/src/main/java/com/landpoint/app/ui/edit/LandEditScreen.kt` | "Add photo" affordance offers CameraX path |
| `app/src/main/AndroidManifest.xml` | Camera permission (already present for the intent path; no new permission needed) |
| `app/build.gradle.kts` | `androidx.camera:camera-*` dependencies |
| `.maestro/flows/` | New flow: in-app capture → stamped photo in detail |
| `app/src/test/java/com/landpoint/app/ui/capture/PatokCaptureViewModelTest.kt` | New |

### Risk level

**High.** CameraX on `minSdk = 24` has device-specific quirks (especially on Android 9, where Camera2's `LEGACY` provider is in scope for some devices). The fallback to the intent path must be tested. The `camerax` skill (ADR-007) applies.

### CI implications

- Maestro flow for capture runs on the API 30 emulator; CameraX works on the emulator's fake camera.
- Unit tests cover the `PatokCaptureViewModel` state and the `PhotoStore` acceptance of the new path.
- APK size budget (Phase 8) is monitored for the ~2MB CameraX addition.

---

## Phase 10 — v1.5.0

### Scope

Contingent migrations and new features, gated on the triggers below:

1. **Hilt migration** — only if the `AppContainer` graph (ADR-003) has grown past ~25 entries or scoping is hard to reason about. The trigger is revisited at the end of each phase; if not met, this item is deferred and the phase ships without it.
2. **Ink-app-builder for boundary agreement signature capture** — a user can sign a boundary agreement on screen (a sketch canvas with stroke capture); the signed sketch is attached to the parcel as a photo-like asset. Uses a lightweight ink-capture library, not a full PDF signing flow.
3. **Glance widget for the home screen** — a small widget showing the count of saved parcels and a tap-to-add shortcut. Read-only; no data leaves the app.

### Files touched

| Path | Change |
|---|---|
| `app/src/main/java/com/landpoint/app/AppContainer.kt` | (If Hilt) replaced by `@HiltAndroidApp` + `@Inject` annotations across the graph |
| `app/build.gradle.kts` | (If Hilt) Hilt plugin + dependency |
| `app/src/main/java/com/landpoint/app/ui/agreement/AgreementSketchScreen.kt` | New (ink capture) |
| `app/src/main/java/com/landpoint/app/data/AgreementStore.kt` | New (stores signed sketches alongside photos) |
| `app/src/main/java/com/landpoint/app/widget/LandPointWidget.kt` | New (Glance) |
| `app/src/main/java/com/landpoint/app/widget/WidgetReceiver.kt` | New |
| `app/src/main/AndroidManifest.xml` | `AppWidgetProvider` registration |
| `.maestro/flows/` | New flow: agreement sketch → saved asset |

### Risk level

**High.** Hilt migration (if it happens) is a sweeping change touching every `viewModel(factory = ...)`. The ink-capture and Glance widget are lower risk individually but each adds a new dependency and a new asset type that the export pipeline (`ImportExportManager`, `BackupManager`) must carry.

### CI implications

- If Hilt lands: Kover coverage threshold may drop briefly as the graph restructure settles; the gate is reviewed and re-tightened.
- The Glance widget is tested with a Robolectric shadow for `AppWidgetManager`.
- Backup/restore round-trip test extends to cover the agreement-asset type.

---

## Future considerations

Items not in any phase above, to be evaluated when the phases are done:

- **AGP 9 upgrade.** Track the Android Gradle Plugin release cadence; AGP 9 will require JDK 21 on the build host and may require changes to the `gradle.properties` memory tuning (currently 1 GB Gradle heap). Evaluate when AGP 9 is stable.
- **QR code sharing.** On the `README.md` roadmap. A parcel's centroid + name + area encoded as a QR; scanned by the receiving phone to import. The `zxing` library or the platform `BarcodeDetector` (Play Services ML Kit on devices that have it; a fallback for devices that do not) are candidates. The data model already stores everything the QR would carry.
- **Parcel numbering and grouping.** On the `README.md` roadmap. `parcelNumber` is already in `LandEntity` (unused). Grouping by parcel number is a query + a list-section change; the data layer needs no migration.
- **Hilt (revisited).** If Phase 10 defers Hilt and the graph keeps growing, re-evaluate at the next annual review. ADR-003 records the reversal trigger.
- **Vulkan MapLibre variant.** Revisit when `minSdk` rises to 30 or higher (ADR-005). The performance gain is material on large map areas.
- **Public-repo transition.** If the repo becomes public, CodeQL can be re-enabled (see `README.md` — "five-minute change") and branch protection rules become enforceable (ADR-009).

---

## Sequencing rationale

The phases are ordered by **value ÷ risk**, highest first:

- Phase 1 ships docs and a single-screen redesign — the foundation for everything else, with almost no risk.
- Phases 2–3 deliver the most user-visible improvements (parcel preview, undo/redo via the recently-deleted bin) before any IA change.
- Phase 4–5 redesign onboarding and settings — the surfaces a new user meets first — and Phase 5 is where the four-tab restructure lands.
- Phase 6 adds tablet support, which is a one-time layout investment that pays off for every later phase.
- Phases 7–8 are pure CI investment — screenshot tests and performance budgets — that *protect* the work shipped in Phases 1–6 before the riskiest phases land.
- Phases 9–10 are the high-risk, high-effort phases (CameraX, possible Hilt migration, new asset types) sequenced last so that the safety nets from Phases 7–8 are in place.

A phase may slip; the sequencing above means a slip in Phase 9 does not block the value already shipped in Phases 1–6.
