# LandPoint — Architectural Decision Records

**Status:** Living document
**Audience:** Contributors to the LandPoint Android app
**Scope:** ADRs for the UI/UX redesign and the engineering choices the redesign rests on.

This file records architectural decisions made during the redesign rollout (see `docs/IMPLEMENTATION_PLAN.md`) and the decisions the redesign inherits from the existing codebase. Each ADR follows the standard format: **Context / Decision / Consequences / Status**. Decisions are not reversed silently — a superseded ADR is marked `Superseded by ADR-NNN` and the new ADR explains why.

The redesign inherits several decisions already documented in `README.md` ("Architecture — Deliberate choices worth knowing") and `SECURITY.md`. Those decisions are recorded here as ADRs for the same reason every other decision is: so a future contributor finds them in one place, with the trade-off stated plainly, instead of having to infer them from the code.

---

## ADR-001 — Keep Kotlin + Jetpack Compose + Material 3 as the stack

**Status:** Accepted
**Date:** Carried over from the original build; reconfirmed for the redesign.

### Context

The app is Kotlin throughout, Jetpack Compose for the UI, Material 3 for the component and theming layer, MVVM layered UI → ViewModel → Repository → Room. `app/build.gradle.kts` pins `compileSdk = 35`, `minSdk = 24`, `targetSdk = 35`. There is no XML view code, no AppCompat theme, no Fragment-based screen.

The redesign is a visual and IA refresh, not a rewrite. Throwing out the stack to rewrite in, for example, a different UI toolkit would discard the existing test coverage (`app/src/test/`) and the CI pipeline that has been green since `v1.0.0`.

### Decision

Keep Kotlin + Compose + Material 3. The redesign is delivered as Compose code on top of the existing `LandPointTheme` (`app/src/main/java/com/landpoint/app/ui/theme/Theme.kt`), extending `Color.kt`, `Type.kt`, `Shape.kt` with new tokens rather than replacing them.

### Consequences

- **Positive.** No rewrite risk. Existing unit tests and Maestro e2e flows keep running. The Material 3 color role system is the foundation the redesign's semantic tokens plug into.
- **Positive.** Compose's `MaterialTheme` indirection means a token change in `Color.kt` propagates everywhere — the redesign's high-contrast variant is one more `ColorScheme`, not a fork of every screen.
- **Negative.** Material 3 has no named spacing or motion scale; the redesign has to add `Spacing.kt` and `Motion.kt` itself (see `docs/DESIGN_SYSTEM.md` §2.3, §2.6).
- **Negative.** Some M3 components the redesign needs (speed-dial, coach marks) are not in the library and must be built on primitives.

### References

`README.md` (Architecture), `app/build.gradle.kts`, `docs/DESIGN_SYSTEM.md` §2, §8.

---

## ADR-002 — Compose Navigation, four-tab bottom bar with central Add speed-dial; tablet = nav rail + two-pane

**Status:** Accepted
**Supersedes:** the current five-tab structure (Home / Map / Lands / Owners / Reports).

### Context

The app today (`app/src/main/java/com/landpoint/app/ui/LandPointNavHost.kt`) uses Compose Navigation with five tabs — `HOME`, `MAP`, `LANDS`, `OWNERS`, `REPORTS` — and a bottom bar on phones that becomes a `NavigationRail` from 600dp (see `WindowLayout.kt`). From 840dp the land list keeps its detail beside it (`LandListDetailPane`).

The redesign collapses Home and Reports into the other tabs: Home's content (parcel count, recent activity) moves to a header on the Parcels tab; Reports moves into the Tools tab as one of its cards. This leaves four top-level destinations with one clear primary action — Add — which the design system calls out as the central speed-dial (see `docs/DESIGN_SYSTEM.md` §6.1).

### Decision

- **Phone (compact width, < 600dp).** Bottom bar with four tabs: **Map · Parcels · Tools · Settings**. A central Add speed-dial sits above the bar; expanding it offers "GPS point / Walk boundary / Type coordinates / Tap map".
- **Tablet (medium width, ≥ 600dp).** Same four destinations in a `NavigationRail`. The Add speed-dial docks at the bottom of the rail.
- **Two-pane (expanded width, ≥ 840dp).** List-detail on the Parcels tab; the Map tab keeps the map full-bleed with a floating tools panel beside it.
- **System-follow by default** for navigation bar/rail styling (already handled by `WindowCompat` in `Theme.kt`).

### Consequences

- **Positive.** The Add action is one tap from anywhere — the most common task in a land-recording app.
- **Positive.** Four tabs fit comfortably on even the narrowest supported phone (360dp) without truncation.
- **Negative.** Existing `Routes.HOME` and `Routes.OWNERS` callers must be migrated to the new tab structure. `HomeScreen` is removed; `HomeViewModel` is repurposed as the Parcels tab's header.
- **Negative.** Five current Maestro e2e flows (`.maestro/flows/01–05`) touch the bottom bar; they will need updating when the tab set changes. Scheduled in Phase 5 of the implementation plan.

### References

`app/src/main/java/com/landpoint/app/ui/LandPointNavHost.kt`, `app/src/main/java/com/landpoint/app/ui/navigation/Routes.kt`, `app/src/main/java/com/landpoint/app/ui/WindowLayout.kt`, `docs/DESIGN_SYSTEM.md` §6.1, `docs/IMPLEMENTATION_PLAN.md` Phase 5.

---

## ADR-003 — Hand-rolled `AppContainer` for DI; Hilt deferred

**Status:** Accepted
**Date:** Carried over from the original build.

### Context

`README.md` states the trade-off plainly: *"the graph is small enough that Hilt would cost more in build time and APK size than it returns."* The graph today (`app/src/main/java/com/landpoint/app/AppContainer.kt`) is roughly a dozen `by lazy` properties — repository, settings, location tracker, walk recorder, import/export manager, backup manager, offline download coordinator, PDF exporter, pending deletes.

### Decision

Keep `AppContainer` as the single DI graph. ViewModels obtain dependencies through `viewModel(factory = ...)` and a `CreationExt.kt`-style helper that pulls the container from the application. No Hilt, no Koin.

### Consequences

- **Positive.** No annotation processor, faster clean builds, smaller APK. The graph is debuggable in one file.
- **Positive.** The redesign's new components (speed-dial, coach-mark state, recently-deleted bin) slot into `AppContainer` as additional `by lazy` properties — no migration.
- **Negative.** Every new ViewModel has to repeat the factory boilerplate. Acceptable while the graph stays small.
- **Reversal trigger.** If the graph grows past ~25 entries or scoping becomes hard to reason about, revisit. Recorded as Phase 10 of the implementation plan.

### References

`app/src/main/java/com/landpoint/app/AppContainer.kt`, `README.md` (Architecture — Hand-rolled DI), `docs/IMPLEMENTATION_PLAN.md` Phase 10.

---

## ADR-004 — Camera via intent (`ACTION_IMAGE_CAPTURE`); CameraX migration deferred

**Status:** Accepted, with a deferred follow-up.

### Context

Photos attached to a parcel today are taken through the system camera via an intent — `ACTION_IMAGE_CAPTURE` handed to `android.intent.action.IMAGE` resolution. The app stamps the captured image with coordinates (`PhotoStamper.kt`) and stores it in app-private storage (`PhotoStore.kt`). This requires no camera permission of its own (the system camera app handles that) and works on every supported device.

The redesign's Phase 9 calls for in-app patok photo capture: a viewfinder inside the app, tap-to-focus, on-screen coordinate overlay so the photo's metadata and the framing match what the user sees. That is not possible with an intent.

### Decision

- **Now (Phase 1).** Keep intent-based capture. No change to `PhotoStore`, `PhotoStamper`, or the camera permission flow.
- **Phase 9 (v1.4.1).** Migrate to CameraX for the in-app patok capture path only. The intent path stays as a fallback for users who decline the camera permission or whose device has no CameraX-capable rear camera.

### Consequences

- **Positive.** No new permission prompts in the redesign's early phases. The existing camera permission rationale in onboarding remains accurate.
- **Positive.** CameraX is a Jetpack library with no Play Services dependency — keeps the app installable on devices without Google services, which `README.md` calls out as a requirement.
- **Negative.** Two code paths in Phase 9: intent-based and CameraX. The fallback is tested explicitly.
- **Negative.** CameraX adds ~2MB to the APK for the camera2 implementation. Accepted for the feature.

### References

`app/src/main/java/com/landpoint/app/data/PhotoStore.kt`, `app/src/main/java/com/landpoint/app/data/PhotoStamper.kt`, `app/src/main/AndroidManifest.xml` (camera permission), `docs/IMPLEMENTATION_PLAN.md` Phase 9.

---

## ADR-005 — MapLibre Native (OpenGL variant) for maps

**Status:** Accepted
**Date:** Carried over from the original build.

### Context

`README.md` documents this in detail: MapLibre Native instead of the Google Maps SDK — no API key, no Play Services dependency, vector tiles the app styles itself. The **OpenGL variant** is used deliberately: *"the default artifact is the Vulkan renderer, and Vulkan on Android 9 is patchy."* `minSdk = 24` means Android 9 devices are in scope.

Every map style is a JSON document generated or bundled locally by `MapStyleFactory` (`app/src/main/java/com/landpoint/app/map/MapStyleFactory.kt`) — never fetched at startup. Boundaries are drawn with core `GeoJsonSource` + fill/line/symbol layers; no annotation plugin (the annotation plugin still resolves an older SDK).

### Decision

Stay on MapLibre Native OpenGL variant. The redesign's map overlay tokens (`map.overlay.*` in `docs/DESIGN_SYSTEM.md` §2.1) are wired through `LandMapOverlay.kt`'s existing layer structure, not through a new abstraction.

### Consequences

- **Positive.** Offline-first works without compromise: an imported `.pmtiles` archive and a downloaded area draw with exactly the same ink as the online map.
- **Positive.** No API key to manage, no quota, no Play Services dependency.
- **Negative.** No map Compose component from Google — the app uses `LandMapView` (`app/src/main/java/com/landpoint/app/ui/components/LandMapView.kt`) wrapping the MapLibre Android view in `AndroidView`. Compose recomposition and MapLibre's own lifecycle have to be reconciled manually; this is already working and not changed.
- **Negative.** The Vulkan variant's performance gains are not available while Android 9 is in scope. Revisit when `minSdk` rises to 30 or higher.

### References

`app/src/main/java/com/landpoint/app/map/MapLibreInit.kt`, `app/src/main/java/com/landpoint/app/map/MapStyleFactory.kt`, `app/src/main/java/com/landpoint/app/ui/components/LandMapView.kt`, `app/src/main/java/com/landpoint/app/ui/components/LandMapOverlay.kt`, `README.md` (Architecture — MapLibre Native).

---

## ADR-006 — Room + SQLCipher for the database

**Status:** Accepted
**Date:** Carried over from the original build.

### Context

The database is Room over SQLCipher; the key is 32 random bytes wrapped by an AES key in the hardware keystore (StrongBox where present). `DatabaseCipher.kt` and `DatabaseKey.kt` own the key management; `LandDatabase.kt` owns the Room database. The current schema is version 1 (`app/schemas/com.landpoint.app.data.db.LandDatabase/1.json`).

`README.md` states the privacy trade-off in detail: a phone that cannot manage encryption opens the database unencrypted rather than refusing to start; a key locked to one phone cannot be exported; a backup archive (JSON + photos) is the only way off the device.

### Decision

Keep Room + SQLCipher. The redesign's Phase 3 adds a soft-delete (recently-deleted bin) which requires a schema migration to v2 — `deletedAt: Long?` on `LandEntity`. The migration copies rows to a `recently_deleted_lands` table on purge, with a 30-day TTL enforced by a daily `WorkManager` cleanup.

### Consequences

- **Positive.** Encryption at rest remains; the soft-delete migration is additive (no data loss on downgrade).
- **Positive.** Room's migration framework is already wired (the schema JSON is committed) — the v2 migration is a `Migration(1, 2)` plus a `fallbackToDestructiveMigrationOnDowngrade` only.
- **Negative.** SQLCipher adds ~4MB to the APK. Already accepted.
- **Negative.** Testing cipher behavior requires Robolectric shadows for the keystore; `DatabaseCipherTest.kt` already does this and the migration adds cases to it.

### References

`app/src/main/java/com/landpoint/app/data/db/LandDatabase.kt`, `app/src/main/java/com/landpoint/app/data/db/DatabaseCipher.kt`, `app/src/main/java/com/landpoint/app/data/db/DatabaseKey.kt`, `app/schemas/com.landpoint.app.data.db.LandDatabase/1.json`, `app/src/test/java/com/landpoint/app/data/db/DatabaseCipherTest.kt`, `README.md` (Privacy — Encryption at rest), `docs/IMPLEMENTATION_PLAN.md` Phase 3.

---

## ADR-007 — Use of Android engineering skills as agent guidance, not committed artifacts

**Status:** Accepted

### Context

The redesign is being delivered by an agent that has access to a set of reusable Android engineering skills (skill folders under the agent's working directory). These skills package up guidance for concerns the redesign touches: adaptive layouts, Compose performance, navigation-3, testing setup, R8 shrinking analysis, permissions & security, and CameraX. They are guidance — checklists, recipes, anti-patterns — not code.

The repository is private, ships an APK from CI, and has a strict policy against committing anything that does not belong to the app's source tree (see `SECURITY.md` and the existing `SPEC_AUDIT.md` methodology).

### Decision

Apply the following skills as guidance to the redesign, phase by phase. **Their contents are not committed to the repo.** They inform the code written; they do not appear as files.

| Skill | Where it applies | Phase |
|---|---|---|
| **navigation-3** | The four-tab restructure (ADR-002) and the parcel preview → editor flow | Phase 1, Phase 2 |
| **adaptive** | Tablet two-pane layout, nav-rail vs bottom-bar thresholds, foldable support | Phase 6 |
| **jetpack-compose-performance** | Polygon draw-in animation, list scrolling, skeleton loaders, the component gallery | Phase 2, Phase 7, Phase 8 |
| **testing-setup** | Screenshot tests in light/dark + en/in, Maestro flow updates | Phase 7 |
| **r8-analyzer** | APK size gates, keeping SQLCipher/MapLibre rules correct, baseline profile | Phase 8 |
| **permissions-security** | Permission rationale screens in onboarding, CameraX permission flow | Phase 4, Phase 9 |
| **camerax** | In-app patok capture viewfinder, tap-to-focus, coordinate overlay | Phase 9 |

### Consequences

- **Positive.** The agent does not reinvent well-known guidance for each concern; the skills package it once.
- **Positive.** The repo stays clean — no `skills/` folder, no markdown guidance committed alongside code.
- **Negative.** A future human contributor reading the repo will not see the skill guidance that informed a decision. This `DECISIONS.md` file and the inline comments in the affected files are where the reasoning is recorded for them.
- **Note.** Skills are versioned by the agent workspace, not by the repo. A skill's guidance may change between commits without a corresponding change in this file; the ADR records the *decision* the skill informed, not the skill's text.

### References

`docs/IMPLEMENTATION_PLAN.md` (phase-by-phase scope), inline comments at the affected call sites.

---

## ADR-008 — E2E strategy: Maestro flows with bilingual (en + in) assertions, `continue-on-error` until stabilized

**Status:** Accepted
**Date:** Carried over from the existing CI (`ci.yml`).

### Context

Maestro flows live in `.maestro/flows/` and run on an API 30 emulator in CI (`.github/workflows/ci.yml`). There are five flows today: record-land-manual-coordinates, search-filter, export-csv, backup-restore, owner-add. Each flow uses bilingual regex assertions — e.g. `^(Skip|Lewati)$`, `^(Save land|Simpan tanah)$` — so the same flow passes whether the device locale is English or Indonesian. This is deliberate: the app supports both locales from day one (`values/strings.xml` and `values-in/strings.xml`) and a flow that only worked in one would silently mask a regression in the other.

The Maestro job in CI sets `continue-on-error: true` with the comment *"Maestro flows currently depend on a flaky CI emulator."* Until the emulator harness is stabilized, a red Maestro job does not fail the build.

### Decision

- Keep Maestro as the e2E layer. Bilingual assertions stay mandatory for every new flow.
- Keep `continue-on-error: true` while flow assertions are stabilized; remove it once the flows have been green on `develop` for two consecutive weeks.
- New flows added during the redesign (corner marking toolbar, undo snackbar, recently deleted bin, bulk select, onboarding redesign, settings redesign, tablet two-pane, CameraX patok capture) follow the same bilingual pattern.
- Each flow's header comment names what it covers, the preconditions it assumes, and any locale-conditional behavior — same convention as the existing five.

### Consequences

- **Positive.** A regression in either locale is caught by a single flow, not two.
- **Positive.** Stabilization can happen without blocking merges; the unit-test and lint gates remain the hard gates.
- **Negative.** While `continue-on-error` is set, a real e2e regression can land unnoticed. Mitigated by running the flows locally before a PR and by the nightly job (`nightly.yml`) surfacing results.
- **Negative.** Maestro's regex assertions are string-based; renaming a label in `strings.xml` silently breaks a flow. The screenshot tests in Phase 7 are the second line of defense.

### References

`.maestro/flows/`, `.github/workflows/ci.yml` (lines ~204, ~219, ~247–301), `app/src/main/res/values/strings.xml`, `app/src/main/res/values-in/strings.xml`, `docs/IMPLEMENTATION_PLAN.md` Phase 7.

---

## ADR-009 — Branch protection is not enforceable; manual PR discipline via `develop`

**Status:** Accepted
**Date:** Carried over.

### Context

`README.md` states `main` is *"protected: only pull requests whose CI is green may merge into it."* In practice the repository is a **private** repo on a **free** GitHub plan, and branch protection rules (required status checks, required reviews, dismissal of stale reviews) require a paid plan or a public repo. The current setup relies on contributor discipline: feature branches PR into `develop`, `develop` PRs into `main`, and a human checks that CI is green before merging.

`SPEC_AUDIT.md` records this as a known deviation from the spec's CI/CD section.

### Decision

- Continue the manual discipline: `feature/*` → `develop` → `main`, PRs only, no direct pushes to `develop` or `main`.
- CI is the gate: `ci.yml` runs ktlint, detekt, Android Lint, unit tests with Kover, and Maestro on every PR; merges wait on those jobs.
- The redesign's documentation branch (`feature/design-system-docs`) follows the same flow: PR into `develop`, not directly into `main`.
- If the repo moves to a paid plan or becomes public, enable GitHub's branch protection rules for `main` and `develop` with required status checks (`ci.yml`'s `lint`, `unit-tests`, `maestro` jobs) and a single required review.

### Consequences

- **Positive.** No cost; works on a free private repo.
- **Negative.** A contributor can technically push to `main`. Mitigated by reviewer discipline and by CI running on every push (so a bad direct push is at least flagged red).
- **Negative.** No enforced required reviews. The redesign's larger phases (Phase 2 hero, Phase 6 tablet) warrant at least one human review before merge; this is a process rule, not a tooling rule.

### References

`README.md` (Building from source — Branching and releases), `SPEC_AUDIT.md`, `.github/workflows/ci.yml`.

---

## ADR-010 — Performance budgets: cold start, frame time, APK size, enforced in CI

**Status:** Accepted (budgets to be set in Phase 8).

### Context

The app today has no enforced performance budget. `gradle.properties` is tuned for a machine with limited RAM (1 GB Gradle heap, serial GC, single-threaded); CI runs unit tests and Maestro but does not measure cold start, frame time, or APK size against a gate. Release APKs are produced per-ABI plus a universal APK and an AAB.

`README.md` notes that `minSdk = 24` puts Android 9 devices in scope, and that the OpenGL variant of MapLibre is chosen over Vulkan for exactly that reason. The redesign's Phase 2 adds an animated polygon draw-in, Phase 6 adds two-pane layouts, and Phase 8 adds a Macrobenchmark + Baseline Profile to CI. Without budgets these additions land unmeasured.

### Decision

Establish three budgets, enforced in CI from Phase 8 (v1.4.0) onward:

| Budget | Metric | Gate | Tool |
|---|---|---|---|
| Cold start | `MainActivity` reportedFullyDrawn, p95 | ≤ 800ms on a Pixel 6 API 33 emulator | Macrobenchmark `ColdStartupBenchmark` |
| Frame time | P90 frame duration while scrolling the parcel list | ≤ 16ms (60Hz) / ≤ 8ms where 120Hz is reported | Macrobenchmark `ScrollFramesBenchmark` |
| APK size | Universal debug APK uncompressed size | ≤ current `main` + 2MB | `./gradlew assembleDebug` + a size-check job |

The Baseline Profile (generated by `Macrobenchmark`'s `BaselineProfileGenerator`) ships in `app/src/main/baseline-prof.txt` and is consumed by the `androidx.profileinstaller` dependency. R8 full mode stays on for release builds; the `r8-analyzer` skill (ADR-007) is used to keep the SQLCipher and MapLibre keep rules minimal.

Budgets are enforced as CI **hard failures** on `develop` and `main`. A regression past the gate blocks the merge; a deliberate budget increase is a separate PR that updates the gate value with a stated reason in the commit message.

### Consequences

- **Positive.** The redesign's heavier features (animation, two-pane, in-app camera) are gated against measurable regressions, not against a reviewer's impression.
- **Positive.** The Baseline Profile gives a one-time cold-start improvement of typically 15–25% on first launch after install.
- **Negative.** Macrobenchmark jobs need an emulator with Google APIs (not the plain AOSP image the current Maestro job uses) — an additional CI runner type and a few minutes per run.
- **Negative.** A budget that is too tight blocks legitimate feature work; the gate values above are starting points, to be revisited after Phase 8's first week of data.

### References

`README.md` (Building from source — Build memory, Architecture), `gradle.properties`, `app/build.gradle.kts`, `docs/IMPLEMENTATION_PLAN.md` Phase 8, ADR-007 (r8-analyzer skill).

---

## How to add a new ADR

1. Copy the format above: **Status / Date / Context / Decision / Consequences / References**.
2. Number sequentially from the last ADR in this file. Do not reuse a retired number.
3. If an ADR is superseded, leave it in place with `**Status:** Superseded by ADR-NNN (date)` and a one-line reason. Do not delete it.
4. Link the new ADR from the relevant phase in `docs/IMPLEMENTATION_PLAN.md` and from any code file whose behavior it changes.
5. Keep the language plain. The trade-off is the point; the prose around it is not.
