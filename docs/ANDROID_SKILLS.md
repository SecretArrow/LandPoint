# Android Skills Survey

Date: 2026-11-13

## Purpose

Google's official Android skills are AI-optimized `SKILL.md` instruction sets
(Apache-2.0) for workflows where LLMs usually underperform. This doc summarizes
which skills apply to the LandPoint UI/UX redesign and how they're being
applied. The skills are guidance for the coding agent, **NOT** committed app
code.

## Credit

Android skills (Apache-2.0, https://github.com/android/skills)

## Inventory

The full skills repo contains **36 skills** (one `SKILL.md` per skill folder).
The table below covers the 10 skills the redesign considered, plus the XML
migration skill that was ruled out. Skill paths are relative to the skills repo
root.

| Skill path | Status | Applies to redesign? | How it's applied |
|---|---|---|---|
| `navigation/navigation-3/SKILL.md` | exists | yes | Bottom bar + two-pane + deep links; replaces the hand-rolled `Scaffold` + `NavigationBar`/`NavigationRail` swap and the hand-rolled `LandListDetailPane` |
| `jetpack-compose/adaptive/SKILL.md` | exists | yes | Tablet/foldable/landscape adaptation via `NavigationSuiteScaffold` + Nav3 `ListDetailSceneStrategy`; replaces the hand-rolled `WindowLayout` thresholds |
| `jetpack-compose/jetpack-compose-performance/SKILL.md` | exists | yes | Map overlay recomposition: defer `LandMapController.cameraState` reads to Layout/Draw so panning the redesigned map screen does not recompose per frame |
| `testing/testing-setup/SKILL.md` | exists | yes (deferred) | Add Compose Preview Screenshot Testing across form factors; fill the empty `androidTest` sourceset; no screenshot tests exist today |
| `performance/r8-analyzer/SKILL.md` | exists | yes (deferred) | Audit `app/proguard-rules.pro` for redundant/broad keeps (the `-keep class com.landpoint.app.data.model.** { *; }` rule is a candidate) and shrink the release APK |
| `performance/anr-audit-and-fix/SKILL.md` | exists | yes | Static ANR audit of GPS callbacks landing on `Looper.getMainLooper()` in `LocationProvider` and the `Dispatchers.Main.immediate` scope in `WalkTrackService` |
| `security/android-permissions-security/SKILL.md` | exists | yes | Verify the redesigned screens' location/camera permission gates match the 3-state flow playbook; confirm the `FileProvider` URI grants stay scoped |
| `security/android-intent-security/SKILL.md` | exists | yes (deferred) | Harden share/export/import once `navigation-3` deep links land: `IntentSanitizer` on the deep-link entry, `FLAG_IMMUTABLE` on the walk notification's Finish action |
| `camera/camerax/SKILL.md` | exists | yes (deferred) | Replace `ACTION_IMAGE_CAPTURE` + `FileProvider` handoff in `PhotoStore.newCameraTarget()` with in-app CameraX capture for the redesigned photo-attach flow |
| `devtools/android-cli/SKILL.md` | exists | tooling only | This is the skill *manager* (`android skills add ...`), not a redesign skill. Covered in the Installation note |
| `jetpack-compose/migration/migrate-xml-views-to-jetpack-compose/SKILL.md` | exists | no | LandPoint is already 100% Compose; there is no XML layout under `app/src/main/res/layout/` to migrate. See "Skills not applied" |

Summary of the 10 considered skills: **all 10 exist** in the inventory (none
missing). Of those, **5 are applied in the current round (v1.0.2)**,
**4 are deferred to a follow-up phase**, and **1 is tooling** (skill
management, not a redesign skill).

## Per-skill detail

### navigation-3

**Path:** `navigation/navigation-3/SKILL.md`

**Key takeaways:**

- Migrates from Navigation 2 (string routes, `NavHost`) to Navigation 3
  (type-safe `NavKey`s, `NavDisplay`, back stack saved by default).
- Ships recipes for the three things the redesign needs: a bottom navigation
  bar with **multiple back stacks** (`recipes/multiple-backstacks.md`),
  **deep links** with synthetic back stacks
  (`recipes/deeplinks-syntheticbackstack.md`), and **two-pane / list-detail
  Scenes** (`recipes/scenes-twopane.md`, `recipes/material-listdetail.md`).
- Scenes (`ListDetailSceneStrategy`, `SupportingPaneSceneStrategy`) are the
  supported way to do multi-pane; the skill says **do not** use
  `ListDetailPaneScaffold` or `SupportingPaneScaffold`.
- Modular navigation code recipes exist for both Hilt and Koin; LandPoint uses
  a hand-rolled `AppContainer` (no DI framework), so neither applies directly.
- Returning results as events vs. state (`recipes/results-event.md`,
  `recipes/results-state.md`) matters for the editor → preview → save flow.

**How it applies to LandPoint:**

- `app/src/main/java/com/landpoint/app/ui/LandPointNavHost.kt` currently hand-rolls
  a `Scaffold` with `NavigationBar` (portrait phone) or `NavigationRail`
  (tablet/landscape) and swaps between them by reading `windowWidthDp()`. Nav3
  replaces this with `NavDisplay` + a `NavigationSuiteScaffold`-style top-level
  container.
- `app/src/main/java/com/landpoint/app/ui/lands/LandListDetailPane.kt` is a
  hand-rolled `Row` + nested `NavHost` for the list-beside-detail layout. Nav3's
  `ListDetailSceneStrategy` replaces it and handles the compact/expanded
  transition automatically.
- `app/src/main/java/com/landpoint/app/ui/navigation/Routes.kt` carries the
  string routes; these become type-safe `NavKey` data classes.
- Deep links (`landpoint://land/<id>`, share-sheet "open in LandPoint") land
  here once the migration is done — `recipes/deeplinks-syntheticbackstack.md`
  is the recipe.
- LandPoint is on `androidx.navigation:navigation-compose` 2.8.5 (Navigation 2);
  the migration guide at `references/android/guide/navigation/navigation-3/migration-guide.md`
  is the path.

**Phase:** applied in v1.0.2. This is the spine of the redesign.

---

### adaptive

**Path:** `jetpack-compose/adaptive/SKILL.md`

**Key takeaways:**

- Prerequisites: the app must use Compose for all screens (LandPoint does) and
  Jetpack Navigation 3 (LandPoint does not yet — see `navigation-3` above).
- 8-step workflow: verify current UI with screenshot tests → make the nav bar
  adaptive → add multi-pane layouts → make vertical lists adaptive by column
  count → hide app bars on scroll → preserve UI state across config changes →
  add desktop experiences → support foldable tabletop posture.
- Step 2: replace a `Scaffold` + `NavigationBar`/`NavigationRail` swap with
  `NavigationSuiteScaffold`, which picks bottom bar / rail / drawer from
  `WindowSizeClass` automatically.
- Step 3: multi-pane layouts **must** use Nav3 `SceneStrategy`
  (`ListDetailSceneStrategy`, `SupportingPaneSceneStrategy`), not
  `ListDetailPaneScaffold` / `SupportingPaneScaffold`.
- Step 6: **never** add `android:configChanges` or `android:screenOrientation`
  to suppress Activity recreation; instead audit every `remember` and replace
  user-input/scroll state with `rememberSaveable` / `rememberLazyListState()` /
  `rememberScrollState()`.
- Step 4: `LazyVerticalGrid` with `GridCells.Adaptive(<min-dp>)` for lists that
  gain columns at larger widths.

**How it applies to LandPoint:**

- `app/src/main/java/com/landpoint/app/ui/WindowLayout.kt` is a hand-rolled
  two-threshold decision (`RAIL_FROM_DP = 600`, `TWO_PANE_FROM_DP = 840`) read
  from `LocalConfiguration.current.screenWidthDp`. The adaptive skill replaces
  it with `WindowSizeClass` / `currentWindowAdaptiveInfo()` and
  `NavigationSuiteScaffold`, which handle the compact/medium/expanded
  transitions and the bar/rail/drawer choice without custom thresholds.
- `LandListDetailPane.kt` becomes a Nav3 `ListDetailSceneStrategy` (see
  `navigation-3` above) instead of a `Row` + `NavHost`.
- The `LANDS` list screen (`app/src/main/java/com/landpoint/app/ui/lands/LandListScreen.kt`)
  gains `LazyVerticalGrid` + `GridCells.Adaptive(...)` so a tablet shows two or
  three columns of `LandCard` instead of one.
- `AndroidManifest.xml` was checked: it has **no** `android:configChanges` and
  **no** `android:screenOrientation` — already compliant with Step 6's hard
  rule.
- `LocationPermission.kt` already uses `rememberSaveable` for `asked` — the
  pattern Step 6 asks for; the redesign audits the rest of the screens
  (`MapScreen`, `LandEditScreen`, `CompassScreen`) for the same.
- Foldable tabletop posture (Step 8) is relevant for the redesigned map screen:
  a phone half-folded on a table could show the map on the upright half and
  the walk telemetry panel on the flat half (`references/foldable-tabletop.md`).

**Phase:** applied in v1.0.2. Pairs with `navigation-3`.

---

### jetpack-compose-performance

**Path:** `jetpack-compose/jetpack-compose-performance/SKILL.md`

**Key takeaways:**

- Foundational prerequisites: never profile a debuggable build, keep R8 on,
  generate baseline profiles for critical journeys.
- 7-category audit workflow; **mandatory** that the agent read the matching
  `references/*.md` before flagging or fixing any issue in a category.
- Category 1 (state & stability): wrap frequently-changing state reads in
  lambdas (`Modifier.offset { ... }`, `Modifier.graphicsLayer { ... }`,
  `Modifier.drawWithCache`) so reads happen in Layout/Draw, not Composition;
  prohibit backwards writes; do **not** reflexively add `@Stable`/`@Immutable`
  (Strong Skipping Mode already handles `List` via instance equality).
- Category 3 (lazy layouts): always specify both primitive `key`s and
  `contentType`s for lazy items; hoist shared `Flow`s to the parent instead of
  collecting per item.
- Category 6 (graphics): replace decorative `Canvas` nodes with
  `Modifier.drawWithCache` to cache `Path`/`Paint`/`Brush`; prefer Coil's
  `AsyncImage` over `Image`.
- Category 7: migrate `Modifier.composed` to `ModifierNodeElement` +
  `Modifier.Node` to kill per-node subcomposition overhead.

**How it applies to LandPoint:**

- `app/src/main/java/com/landpoint/app/ui/components/LandMapView.kt` exposes a
  `LandMapController` whose `cameraState: MutableState<CameraPosition?>` is
  written on every camera frame during pan/zoom (`cameraState.value =
  map.cameraPosition`). Any composable that reads `cameraState.value` (or the
  derived `cameraZoom`, `cameraLatitude`, `metresPerPixel`) in its body will
  recompose per frame. The redesign defers those reads into
  `Modifier.drawWithCache` / `Modifier.graphicsLayer` so the scale bar
  (`MapScaleBar.kt`), the telemetry panel (`TelemetryPanel.kt`), and the chrome
  (`MapChrome.kt`) update in Draw/Layout without recomposing the map screen.
- `app/src/main/java/com/landpoint/app/ui/components/LandMapOverlay.kt` already
  does the right thing at the MapLibre level (six GeoJSON sources + ten layers,
  one `setGeoJson` call per update) — the performance skill's graphics
  guidance confirms this is preferable to per-feature view objects.
- `app/src/main/java/com/landpoint/app/ui/lands/LandListScreen.kt` /
  `LandRow.kt` / `LandCard.kt` need primitive `key`s + `contentType`s on any
  `LazyColumn`/`LazyVerticalGrid` of land records (Category 3).
- Coil is already a dependency (`libs.coil.compose`), so the
  `AsyncImage`-over-`Image` guidance is actionable for `LandCard` thumbnails
  and `OwnerDetailScreen` photos.
- Compose BOM is 2024.12.01 (Compose ~1.14), so `computedStateOf` (Compose
  1.13+) is available for filtered scroll-derived state on the map screen.

**Phase:** applied in v1.0.2. The map screen is the centerpiece of the
redesign and recomposition jank during pan/zoom would undo the visual work.

---

### anr-audit-and-fix

**Path:** `performance/anr-audit-and-fix/SKILL.md`

**Key takeaways:**

- Two execution paths: **Path A** = static codebase ANR risk audit (source
  scan); **Path B** = trace-driven diagnosis of an actual ANR stack dump.
- Path A follows `references/risk_analysis_guide.md` step by step: list
  candidates, **prove the executing thread** (trace callers when the thread
  depends on them), **prove the call blocks**, then attribute and grade.
  Report only candidates that pass all three checks; list dismissed ones
  separately with a one-line reason.
- Evaluate off-main-thread mutations of attached Views not just as
  `CalledFromWrongThreadException` crash risks but as **Sync Barrier Leak**
  ANR risks with their own proof (off-main execution + an attached View).
- Path B proposes app fixes **only** for `APP_DEFECT` or
  `APP_RESOURCE_OVERLOAD` findings, never for `EXTERNAL_BOTTLENECK`.
- Scope: ANRs only — not general crashes, dropped frames, or Perfetto traces
  (those belong to the `profilers/android-profiler` skill).

**How it applies to LandPoint:**

- Path A audit on the GPS path: `LocationProvider.kt` calls
  `locationManager.requestLocationUpdates(..., Looper.getMainLooper())` in
  three places (lines ~189, ~269, ~360) and `GnssSignal.kt` registers a
  `GnssStatusCallback` on a `Handler(Looper.getMainLooper())`. Every GPS fix
  and every GNSS status event arrives on the main thread. If the callbacks do
  averaging (`LocationAveraging.kt`), polygon math (`PolygonMath.kt`), or
  `LandDao` writes on the main thread, that is a graded ANR risk against the
  redesigned walk screen.
- `WalkTrackService.kt` runs its scope on `Dispatchers.Main.immediate`
  (`SupervisorJob() + Dispatchers.Main.immediate`). The audit proves whether
  the foreground service's work (writing the walk track to the encrypted
  Room/SQLCipher database) actually blocks the main thread or is correctly
  dispatched to `Dispatchers.IO`.
- `ImportExportManager.kt` already offloads to `Dispatchers.IO` — likely a
  non-issue, but the audit formally dismisses it with a one-line reason rather
  than assuming.
- The audit output feeds directly into the redesigned walk screen's UX: if a
  graded risk is found, the fix (move averaging/DB writes off the main thread)
  lands in the same v1.0.2 round so the redesigned walk telemetry panel does
  not stutter on a fix arrival.

**Phase:** applied in v1.0.2 for the GPS/walk path (directly affects the
redesigned walk screen's responsiveness). The import/export/DB portion of the
audit is deferred to a follow-up hardening pass.

---

### android-permissions-security

**Path:** `security/android-permissions-security/SKILL.md`

**Key takeaways:**

- Section 1 is a vulnerability-area playbook table: weak custom permissions,
  overly broad URI grants, spoofable caller identity, bound-service bypass,
  missing method-level checks, flawed runtime-permission flow, simultaneous
  location requests, broad storage permissions for media, cached permission
  state, insecure broadcasts.
- **Flawed runtime-permission flow** row: implement the 3-state flow — check
  grant → check `shouldShowRequestPermissionRationale()` → launch contract. On
  permanent denial, redirect to
  `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`.
- **Simultaneous location requests** row: never request `ACCESS_FINE_LOCATION`
  + `ACCESS_BACKGROUND_LOCATION` together (rejected on Android 11+). Request
  foreground first; background only after foreground is granted, in a separate
  user step.
- **Broad storage permissions for media** row: prefer the zero-permission
  Photo Picker (`ActivityResultContracts.PickVisualMedia`) over
  `READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES`.
- **Cached permission state** row: never cache permission status in a `var`;
  query `ContextCompat.checkSelfPermission()` dynamically at invocation time
  and wrap protected calls in try/catch for `SecurityException`.
- **Overly broad URI permission grants** row: replace
  `<grant-uri-permission android:pathPrefix="/" />` with an explicit scoped
  subpath (e.g. `pathPrefix="/shared/"`); keep `grantUriPermissions="true"`.

**How it applies to LandPoint:**

- `app/src/main/java/com/landpoint/app/ui/components/LocationPermission.kt`
  already implements the 3-state flow correctly: `rememberLocationPermission`
  tracks `granted` + `asked`, computes `blocked` from
  `shouldShowRequestPermissionRationale`, and `LocationPermissionCard` routes
  to `openAppSettings(context)` on permanent denial. The redesign keeps this
  pattern and reuses the card on any redesigned screen that needs location.
- `AndroidManifest.xml` requests `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION`
  together (foreground only) and does **not** request
  `ACCESS_BACKGROUND_LOCATION` — compliant with the simultaneous-requests
  rule. The walk is powered by a `foregroundServiceType="location"` service
  with `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_LOCATION`, which is the
  correct path (not background location).
- The `CAMERA` permission is declared because `PhotoStore.newCameraTarget()`
  uses `ACTION_IMAGE_CAPTURE`. The skill's Photo Picker guidance is about
  *selecting existing media*, not *capturing new* — so it does not force a
  change here, but it does apply if the redesigned photo-attach flow adds an
  "pick from gallery" entry alongside the camera.
- `LocationPermission.kt` re-checks `context.hasLocationPermission()` on
  `ON_RESUME` (not a cached `var`) — compliant with the cached-state rule.
- `app/src/main/res/xml/file_paths.xml` scopes the `FileProvider` to
  `cache-path name="images" path="images/"` and `cache-path name="pdf"
  path="pdf/"` — no wildcard `pathPrefix="/"`. Compliant; the redesign keeps
  it scoped when adding any new share path.
- The `FileProvider` is `exported="false"` with `grantUriPermissions="true"`
  — the skill's granular-URI-grants guidance applies when the redesigned
  share/export flow hands a `content://` URI to a chooser: attach
  `FLAG_GRANT_READ_URI_PERMISSION` (already done in `ShareUtils.shareFile`)
  rather than widening the manifest grant.

**Phase:** applied in v1.0.2. The redesigned screens reuse the verified
permission card and keep the manifest/FileProvider configuration scoped.

---

### testing-setup

**Path:** `testing/testing-setup/SKILL.md`

**Key takeaways:**

- 14-step workflow: analyze current setup → set up DI testing → install
  frameworks → refactor for fakes → unit tests → UI tests → DB tests →
  screenshot tests → UI behavior tests → navigation tests → window-size
  simulation → end-to-end → instrumented screenshots → JaCoCo coverage.
- Step 8 screenshot-test rule: every screen tested in **nine sizes**
  (compact/medium/expanded × 400/610/900dp width × 400/500/1000dp height),
  plus a 400×500 mobile shot at font scale 1.5 and one per alternative theme.
  Component-level tests vary theme + font scale.
- Step 3: respect the current stack. If no testing frameworks exist, install
  JUnit4, JaCoCo, Robolectric, Compose Testing APIs (for fully-Compose apps),
  Compose Preview Screenshot Testing, and Mockk only if clearly necessary.
  For screenshot testing, pick the setup by AGP version: AGP ≥ 9.5.0-alpha03
  uses `testOptions { screenshotTests.create(...) }` test suites; lower AGP
  uses the **legacy standalone plugin** `com.android.compose.screenshot` and
  should note test suites are a future, separate decision. Don't combine both.
- Step 9: Compose UI behavior tests should prefer semantic matchers first, use
  `testTag` only when a matcher needs more than three matchers, and always
  verify state restoration.
- Step 12: end-to-end tests should be ~5% of the total, covering big user
  journeys; use UIAutomator only when platform features (notifications, system
  UI) are involved.

**How it applies to LandPoint:**

- Current stack (`gradle/libs.versions.toml` + `app/build.gradle.kts`): JUnit4
  4.13.2, Robolectric 4.14.1, `androidx.test.core`, `kotlinx.coroutines.test`,
  `androidx.room.testing`, `testInstrumentationRunner` set,
  `testOptions.unitTests.isIncludeAndroidResources = true`. androidTest deps
  (`androidx.test.junit`, `espresso.core`) are declared but the `androidTest`
  sourceset is **empty**. No mocking framework (correct — the skill says only
  install Mockk if necessary). No DI framework (hand-rolled `AppContainer`).
- No screenshot tests exist — the highest-value gap. AGP is 8.7.3 (below
  9.5.0-alpha03), so the **legacy standalone plugin** setup applies; the skill
  says to note test suites as a future, separate decision and **not** to
  upgrade AGP as part of setting up screenshot tests.
- Coverage: `kover` 0.8.3 is already wired in (the Kotlin-native equivalent of
  JaCoCo), so Step 14 is effectively satisfied — the doc should note Kover
  instead of adding JaCoCo.
- The `test/` sourceset has solid ViewModel/DAO/util coverage
  (`LandListViewModelTest`, `OwnerDaoTest`, `PolygonMathTest`, etc.) but no
  Compose UI behavior tests and no screenshot tests for the redesigned
  screens — both are blocking for shipping the redesign.

**Phase:** deferred to a parallel testing-track phase that must precede the
redesign release. The screenshot tests in particular are how the redesign
verifies it did not regress the phone layout while adding tablet/foldable
support.

---

### r8-analyzer

**Path:** `performance/r8-analyzer/SKILL.md`

**Key takeaways:**

- Analyzes R8 keep rules to find redundancies, broad package-wide rules, and
  rules that subsume library consumer keep rules. Not for general Gradle build
  errors, Compose UI performance, or runtime CPU profiling.
- Step 1 setup check: inspect `build.gradle(.kts)` and `gradle.properties`;
  verify `android.enableR8.fullMode=false` is **not** present; if AGP < 9.3,
  suggest migration to 9.3 for the simplified `optimization {}` DSL and the
  standalone Configuration Analyzer task.
- Step 2 picks the analysis path by AGP version: **Path A** (AGP ≥ 9.3.0) runs
  `./gradlew :<module>:analyze<Variant>R8Config`; **Path B** (AGP < 9.3.0)
  temporarily pins R8 9.4.34+ in `settings.gradle(.kts)`, generates the proto
  report, converts to JSON, analyzes, and **reverts** the temporary pin.
- Both paths require Python + `protobuf` ≥ 6.33.4. Output is a strict
  Markdown report following `references/REPORT_FORMAT.md`; no placeholder
  `N/A%` scores; no conversational filler.
- Hard constraint: research and suggest only — **no code changes**. Write only
  under `tmp/`. The one exception is the temporary R8 classpath pin in
  `settings.gradle(.kts)` that Path B needs, which must be reverted.

**How it applies to LandPoint:**

- AGP is 8.7.3 → **Path B** applies (the standalone `analyze<Variant>R8Config`
  task requires AGP ≥ 9.3.0).
- `app/proguard-rules.pro` has a candidate broad rule:
  `-keep class com.landpoint.app.data.model.** { *; }` for Room entities. The
  analyzer would score this and tell whether narrowing it (e.g. to
  `@Entity`-annotated classes only, or trusting Room's consumer rules like the
  file already does for MapLibre and SQLCipher) would shrink the release APK.
- The file's own comment for MapLibre/SQLCipher ("Nothing needed here on
  purpose. Both ... ship consumer rules inside their AAR that R8 reads
  automatically") is exactly the philosophy the analyzer promotes — the audit
  would confirm that trust is justified and flag any rule that subsumes a
  library's consumer keep.
- Release APK size matters for the redesign because new Compose adaptive /
  Nav3 dependencies (`androidx.compose.material3.adaptive:adaptive-navigation3`,
  Nav3 runtime) add to the method/field count.

**Phase:** deferred to a follow-up release-prep phase. Run the analyzer on the
`release` variant once the v1.0.2 dependency set is final, before tagging.

---

### android-intent-security

**Path:** `security/android-intent-security/SKILL.md`

**Key takeaways:**

- Secures Activities, Services, Receivers, ContentProviders and the Intents
  they handle — prevents Intent Redirection, PendingIntent mutability abuse,
  and unauthorized component access. Not for network/TLS security or runtime
  permission flows (those belong to `android-permissions-security`).
- **Intent redirection**: if a component receives a nested `Intent` as an extra,
  use `IntentSanitizer` (AndroidX Core 1.9.0+) with an explicit allowlist of
  components/actions/data/extras, or manually strip URI-permission grant flags
  + `resolveActivity()` + verify `packageName`/`className` against an
  allowlist. **Never** launch a nested intent directly.
- **PendingIntent**: `FLAG_IMMUTABLE` by default. A mutable `PendingIntent`
  **must** have an explicit target component set on the base `Intent`.
- **ContentProvider**: internal-only providers `exported="false"`; exported
  providers need `readPermission`/`writePermission` and
  `grantUriPermissions="false"` unless temporary URL access is strictly
  required. Parameterize all `query`/`update` selection — never string
  concatenation.
- **onNewIntent**: apply the same security controls as `onCreate`; call
  `setIntent(newIntent)` before processing.
- Quality checklist: set `android:exported="false"` for components that don't
  need external communication; protect exported components with
  `signature`-level custom permissions for family-app IPC; use
  `RECEIVER_NOT_EXPORTED` for dynamic receivers; declare the
  `${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` signature
  permission + uses-permission when `minSdk < 33`.

**How it applies to LandPoint:**

- `AndroidManifest.xml` has exactly one exported component — `MainActivity`
  (launcher, no permission, which is correct for a launcher activity).
  `WalkTrackService` is `exported="false"`. The `FileProvider` is
  `exported="false"` with `grantUriPermissions="true"` (scoped paths in
  `file_paths.xml`). No custom actions, no bound service exposed to other
  apps, no dynamic receivers. So the manifest is already tight.
- `app/src/main/java/com/landpoint/app/util/ShareUtils.kt` builds only
  **outgoing** intents (`ACTION_SEND`, `ACTION_VIEW` with `geo:`/`http` URIs)
  and attaches `FLAG_GRANT_READ_URI_PERMISSION` to the share intent — the
  intent-security skill is about **incoming** nested intents, so ShareUtils
  itself is not a finding.
- The skill becomes relevant once `navigation-3` deep links land: a
  `landpoint://land/<id>` deep link is an incoming intent, and if it ever
  carries a nested `Intent` extra (e.g. "share back to caller"), the deep-link
  entry must run it through `IntentSanitizer`. AndroidX Core is 1.15.0 (≥
  1.9.0), so `IntentSanitizer` is available.
- `WalkTrackService` posts a walk notification with a Finish action — that
  action's `PendingIntent` must use `FLAG_IMMUTABLE` (the skill's default).
  The audit verifies this when the redesigned walk notification is built.
- Import flow (`ImportExportManager.import(uri, ...)`) receives a `content://`
  URI from `ACTION_OPEN_DOCUMENT`; it is read directly, not forwarded as a
  nested intent, so the redirection rule does not apply — but the audit
  formally dismisses it.

**Phase:** deferred to a follow-up hardening pass that lands alongside the
`navigation-3` deep-link work. The `PendingIntent.FLAG_IMMUTABLE` check on the
walk notification is a quick win that can land in v1.0.2 even before the full
deep-link sanitization.

---

### camerax

**Path:** `camera/camerax/SKILL.md`

**Key takeaways:**

- Procedural guidance for CameraX: `PreviewView`/`CameraXViewfinder`,
  `ImageCapture`, `VideoCapture`, `ImageAnalysis`, `Camera2Interop`, Media3
  effects, ML Kit analysis.
- **Immutable API pattern**: CameraX `VideoCapture` and friends use a fluent
  builder-like pattern that returns a new instance — failing to reassign means
  the setting is silently ignored (`pending.withAudioEnabled()` must be chained
  or reassigned, not called as a standalone statement).
- Migration paths: `camera1-to-camerax.md` and `camera2-to-camerax.md`.
- API discovery: prefer `MlKitAnalyzer` over manual `ImageAnalysis.Analyzer`;
  `Media3Effect` for filters/effects; `ConcurrentCamera` for dual-stream.
- Code quality: fakes over mocks (especially for `ImageProxy`); Google Truth
  `assertThat`; explicit `@RunWith`; `semantics { mergeDescendants = true }` on
  custom Compose camera controls so screen readers announce them as one unit.
- Common pitfalls: check `isRecording` before stop/pause; handle
  `VideoRecordEvent.Start` for UI state; dispatch UI updates from camera
  callbacks to the main thread; check `CAMERA` permission (and `RECORD_AUDIO`
  specifically when audio is enabled).

**How it applies to LandPoint:**

- `app/src/main/java/com/landpoint/app/data/PhotoStore.kt` currently captures
  photos by handing a `FileProvider` URI to a third-party camera app via
  `ACTION_IMAGE_CAPTURE` (`newCameraTarget()` returns `Pair<File, Uri>`). This
  works but has known pain points the redesign can fix with CameraX:
  - The result depends on whatever camera app is installed (different UI, no
    guaranteed EXIF orientation, no consistent shutter behavior).
  - `PhotoStamper.kt` already stamps the returned file with location + heading
    after the fact; with CameraX `ImageCapture` the stamp could happen in-line
    and the capture UX could match the rest of the redesigned app.
- The redesigned `LandEditScreen` / `LandDetailScreen` photo-attach entry point
  would host the CameraX viewfinder (`PreviewView` via `AndroidView`, or the
  newer `CameraXViewfinder`). The `CAMERA` permission is already declared.
- Foldable posture (`references/foldables.md`) matters because a tablet/fold
  redesign should let the user hold the device in a way that does not block the
  camera — the skill's foldable guidance pairs with `adaptive` Step 8.
- The `semantics { mergeDescendants = true }` rule applies to any custom
  shutter button the redesign draws.

**Phase:** deferred to a follow-up feature phase. In-app capture is a feature
addition, not strictly a UI/UX redesign of existing screens — but the
redesigned detail/edit screen is where the CameraX entry point would live, so
the redesign does not preclude it.

---

## Skills not applied

These skills were considered (or discovered in the inventory) but do not fit
the LandPoint UI/UX redesign. Listed with the reason so future rounds know
they were evaluated and skipped, not forgotten.

- **`jetpack-compose/migration/migrate-xml-views-to-jetpack-compose/SKILL.md`** —
  Not applied. LandPoint is already 100% Jetpack Compose. There is no
  `app/src/main/res/layout/` directory and no `<layout>` XML in the project;
  every screen is a `@Composable` (`HomeScreen.kt`, `MapScreen.kt`,
  `LandListScreen.kt`, `LandDetailScreen.kt`, `LandEditScreen.kt`,
  `CompassScreen.kt`, `ReportsScreen.kt`, `OwnerListScreen.kt`, etc.). The
  skill's 10-step XML-to-Compose migration workflow has nothing to operate on.
- **`jetpack-compose/ink/ink-app-builder/SKILL.md`** and
  **`jetpack-compose/ink/ink-custom-brush-builder/SKILL.md`** — Not applied.
  LandPoint has no stylus/ink drawing surface; the boundary editor
  (`LandShapeScreen.kt`, `CornerPickerScreen.kt`) works by tapping corner
  points and walking a GPS track, not freehand drawing.
- **`system/glance-appwidget-updates-and-memory/SKILL.md`** and
  **`system/glance-canonical-layouts/SKILL.md`** — Not applied. LandPoint
  ships no home-screen widget; the only background surface is the
  `WalkTrackService` foreground notification.
- **`system/edge-to-edge/SKILL.md`** — Not applied in this round. The app
  already draws edge-to-edge where it matters (the map screen fills every
  edge; the nav host applies only a bottom inset for the bar). Worth a
  separate pass to verify inset handling on the redesigned screens, but not a
  redesign driver.
- **`wear/wear-compose-m3/SKILL.md`** and **`wear/wear-widgets/SKILL.md`** —
  Not applied. LandPoint has no Wear OS target.
- **`tv/leanback-to-compose-tv-migration/SKILL.md`** — Not applied. LandPoint
  has no Android TV target.
- **`cars/android-for-cars-parked-apps/SKILL.md`** — Not applied. LandPoint
  has no Android Auto / Automotive target.
- **`xr/display-glasses-with-jetpack-compose-glimmer/SKILL.md`** and
  **`xr/projected-hardware/SKILL.md`** — Not applied. LandPoint has no XR
  target.
- **`device-ai/*`** (appfunctions, litert, ml-kit-genai-prompt-api,
  mlkit-agent-api) — Not applied. The redesign does not add on-device AI
  features.
- **`identity/restore-credentials/SKILL.md`** and
  **`identity/verified-email/SKILL.md`** — Not applied. LandPoint has no
  sign-in flow; data is local and encrypted with SQLCipher.
- **`play/*`** (engage-sdk-integration, play-billing-library-version-upgrade,
  play-policy-insights) — Not applied. LandPoint is not on Play Billing and
  does not integrate Engage schemas.
- **`media/media3-cast-integration/SKILL.md`** — Not applied. LandPoint has no
  media playback to cast.
- **`build-system/agp/agp-9-upgrade/SKILL.md`** — Not applied in this round.
  LandPoint is on AGP 8.7.3; upgrading to AGP 9 is a separate, build-system
  decision that affects the `testing-setup` screenshot-test setup path and the
  `r8-analyzer` Path A vs. Path B choice. Noted but out of scope for the
  UI/UX redesign.
- **`profilers/android-profiler/SKILL.md`** — Not applied directly, but
  complementary to `jetpack-compose-performance`: the redesign's map-overlay
  recomposition work could be verified with a Perfetto/jank trace recorded via
  this skill's `recording/workflows/perfetto-trace-recording/` workflow. A
  follow-up verification step, not a redesign skill.
- **`navigation/navigation-event/SKILL.md`** — Not applied. This is the
  back-button/dispatcher companion to `navigation-3`; its guidance is folded
  into the `navigation-3` migration (the Nav3 recipes for back handling and
  lifecycle-owner cover the same surface).

## Installation note

The intended installation path is the `android` CLI, documented in
**`devtools/android-cli/SKILL.md`** (`android skills add <name> --project=.`
or `android skills add --all`). The CLI was **not available in this
environment** (no `android` binary on `PATH`, and installing it would have
required network access to `dl.google.com` outside the task scope).

As a fallback, the skills repo was obtained by a **shallow clone** to a
temporary location **outside** the LandPoint repo:

```
cd /tmp && rm -rf android-skills && \
  git clone --depth 1 https://github.com/android/skills.git android-skills
```

The clone lives at `/tmp/android-skills/` and is **not** committed to the
LandPoint repo. No skill folder contents (`SKILL.md`, `references/`,
`scripts/`, `resources/`) are copied into the LandPoint tree, and no
`.gitignore` entry is needed because the skills directory is entirely outside
the repo. The skills serve as **guidance for the coding agent** — the agent
reads the relevant `SKILL.md` (and its `references/*.md` when the skill
mandates it) before making a change, then applies the guidance to LandPoint's
own source files. The skills themselves never become part of the app's code or
build.

To re-obtain the skills in a fresh environment, either install the `android`
CLI and run `android skills add --all`, or repeat the shallow clone above. The
inventory (36 skills as of the clone date) may have grown by then — re-run
`find /tmp/android-skills -name SKILL.md -type f` to enumerate.
