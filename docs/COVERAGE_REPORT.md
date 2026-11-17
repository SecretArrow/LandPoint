# Coverage Report

This report records the current Kover code-coverage numbers for the `:app`
module, verified against the project's spec targets:

| Scope                       | Target        |
|-----------------------------|---------------|
| Domain logic (`com.landpoint.app.util`) | ≥ 80% line coverage |
| Overall                     | ≥ 60% line coverage |

## Source of the numbers

The numbers below were extracted from the `coverage-report` artifact produced
by the CI workflow's `unit-test` job (`.github/workflows/ci.yml`), which runs:

```
./gradlew :app:testDebugUnitTest :app:koverHtmlReportDebug :app:koverXmlReportDebug
```

- **CI run**: https://github.com/SecretArrow/LandPoint/actions/runs/44084228761
  (run #230)
- **Branch / SHA**: `main` @ `c538e7ac3ff926dac8faaf1a4ec963785f8fa2e3`
  ("Merge pull request #77 from SecretArrow/develop")
- **Run created**: 2026-11-17T12:33:18Z
- **Run conclusion**: success
- **Artifact**: `coverage-report` (2,104,846 bytes, not expired)
- **XML report**: `app/build/reports/kover/reportDebug.xml` (JaCoCo XML format,
  1,844,680 bytes)

The XML report was parsed with the standard JaCoCo `<counter type="LINE">`
elements (and `INSTRUCTION` as a cross-check).

## Results

### Overall (`:app` module, all packages)

| Counter      | Missed   | Covered | Total   | Coverage |
|--------------|----------|---------|---------|----------|
| LINE         | 11,111   | 5,046   | 16,157  | **31.23%** |
| INSTRUCTION  | 134,894  | 36,148  | 171,042 | 21.13%  |
| BRANCH       | 4,773    | 1,783   | 6,556   | 27.20%  |
| METHOD       | 1,828    | 923     | 2,751   | 33.55%  |
| CLASS        | 932      | 331     | 1,263   | 26.21%  |

**Overall line coverage: 31.23%** — **does NOT meet the 60% target.**

### Domain logic — `com.landpoint.app.util`

| Counter      | Missed | Covered | Total | Coverage |
|--------------|--------|---------|-------|----------|
| LINE         | 60     | 405     | 465   | **87.10%** |
| INSTRUCTION  | 353    | 3,316   | 3,669 | 90.38%  |
| BRANCH       | 42     | 312     | 354   | 88.14%  |

**Domain (`util`) line coverage: 87.10%** — **meets the 80% target.**

The previous report (run 43456361814, `main` @ `b043eacd`) had `util` at
79.17%, 5 covered lines short of the gate. The tests added since have closed
that gap with room to spare: +7.93 percentage points, 405 of 465 lines
covered. Overall moved 27.96% → 31.23% (+3.27 pp) over the same period.

## Verdict

The domain gate is met; the overall gate is not.

- `com.landpoint.app.util` cleared 80% for the first time (87.10%). The 60
  lines still missed there are concentrated in `ShareUtils` (41 lines, every
  method takes an `android.content.Context`), the `geoUri` label branch (needs
  `android.net.Uri.encode`), and `LocalizedContext.getAssets()` — Android
  framework surfaces that need Robolectric or a device rather than plain JVM
  tests. `BoundaryEdits.swapStrings` (6 lines) and the `GeoUtils` reporting
  helpers (`formatAccuracy`, `cardinalIndex`, the degree-carry branch of DMS,
  the unlabelled `geoUri`) are pure JVM and are the gaps this branch's new
  tests close.
- The overall gate has a larger gap (31.23% vs 60%). Most of the missed lines
  sit in `com.landpoint.app.ui.*` Compose screens that only the Maestro e2e
  flows exercise, and in the Room/SQLCipher glue that needs a device or
  Robolectric. Lifting overall to 60% would take either Compose UI tests or a
  conscious decision to exclude Compose-UI packages from the gate.

## Top 20 classes by missed lines

Where the uncovered code lives, class by class — the whole-module shortlist
for anyone picking up coverage work. Compose `*ScreenKt` files dominate.

| Missed | Total | Class | Package |
|--------|-------|-------|---------|
| 609 | 609 | `LandEditScreenKt` | ui/edit |
| 344 | 344 | `CornerPickerScreenKt` | ui/edit |
| 334 | 472 | `LandEditViewModel` (29.24%) | ui/edit |
| 230 | 230 | `OfflineMapsScreenKt` | ui/offline |
| 227 | 227 | `LandDetailScreenKt` | ui/detail |
| 214 | 214 | `SettingsScreenKt` | ui/settings |
| 173 | 173 | `LandMapViewKt` | ui/components |
| 164 | 164 | `LandListScreenKt` | ui/lands |
| 161 | 161 | `LandPreviewScreenKt` | ui/preview |
| 160 | 160 | `TelemetryPanelKt` | ui/components |
| 160 | 160 | `LandMapOverlay` | ui/components |
| 156 | 156 | `PdfExporter$RenderState` | data/export |
| 151 | 151 | `MapChromeKt` | ui/components |
| 149 | 149 | `LandShapeScreenKt` | ui/shape |
| 96 | 96 | `MapScreenKt` | ui/map |
| 95 | 95 | `SettingsViewModel` | ui/settings |
| 92 | 115 | `LandDatabase_Impl$1` (20.00%) | data/db |
| 91 | 91 | `ParcelHeroKt` | ui/components |
| 88 | 88 | `LandDao_Impl$47` | data/db |
| 86 | 86 | `OfflineDownloadCoordinator` | map/offline |

## Per-package breakdown (LINE coverage, sorted by coverage %)

| Package                            | Missed | Covered | Total | Line % |
|------------------------------------|--------|---------|-------|--------|
| com/landpoint/app/data/model       | 25     | 284     | 309   | 91.91% |
| com/landpoint/app/ui/theme         | 52     | 368     | 420   | 87.62% |
| com/landpoint/app/util (domain)    | 60     | 405     | 465   | 87.10% |
| com/landpoint/app/map              | 34     | 187     | 221   | 84.62% |
| com/landpoint/app                  | 23     | 93      | 116   | 80.17% |
| com/landpoint/app/data             | 103    | 332     | 435   | 76.32% |
| com/landpoint/app/data/export      | 305    | 794     | 1,099 | 72.25% |
| com/landpoint/app/ui/onboarding    | 64     | 97      | 161   | 60.25% |
| com/landpoint/app/location         | 321    | 443     | 764   | 57.98% |
| com/landpoint/app/data/db          | 920    | 1,163   | 2,083 | 55.83% |
| com/landpoint/app/ui/help          | 98     | 44      | 142   | 30.99% |
| com/landpoint/app/ui/lands         | 464    | 181     | 645   | 28.06% |
| com/landpoint/app/map/offline      | 411    | 155     | 566   | 27.39% |
| com/landpoint/app/ui/navigation    | 6      | 2       | 8     | 25.00% |
| com/landpoint/app/ui/gallery       | 210    | 57      | 267   | 21.35% |
| com/landpoint/app/ui/security      | 72     | 18      | 90    | 20.00% |
| com/landpoint/app/ui/edit          | 2,316  | 230     | 2,546 | 9.03%  |
| com/landpoint/app/ui/components    | 1,518  | 126     | 1,644 | 7.66%  |
| com/landpoint/app/ui/settings      | 995    | 49      | 1,044 | 4.69%  |
| com/landpoint/app/ui/owners        | 606    | 16      | 622   | 2.57%  |
| com/landpoint/app/ui               | 172    | 2       | 174   | 1.15%  |
| com/landpoint/app/ui/shape         | 246    | 0       | 246   | 0.00%  |
| com/landpoint/app/ui/reports       | 260    | 0       | 260   | 0.00%  |
| com/landpoint/app/ui/preview       | 274    | 0       | 274   | 0.00%  |
| com/landpoint/app/ui/offline       | 474    | 0       | 474   | 0.00%  |
| com/landpoint/app/ui/map           | 137    | 0       | 137   | 0.00%  |
| com/landpoint/app/ui/home          | 222    | 0       | 222   | 0.00%  |
| com/landpoint/app/ui/detail        | 560    | 0       | 560   | 0.00%  |
| com/landpoint/app/ui/compass       | 163    | 0       | 163   | 0.00%  |

## Gaps this branch closes

Ten new JVM test classes on `feature/coverage-refresh-tests` cover the
pure-JVM portion of the shortlist above — the classes whose missed lines are
arithmetic or state rather than Android framework:

| New test | Covers |
|----------|--------|
| `util/GeoUtilsReportingTest` | `formatAccuracy`, `cardinalIndex` (incl. staying in step with `bearingToCardinal`), unlabelled `geoUri`, the DMS degree-carry branch |
| `util/BoundaryEditsSwapTest` | `BoundaryEdits.swapStrings` — the id-list twin of the already-tested corner swap |
| `map/GeoBoundsTest` | `GeoBounds.of()` (empty, single, degenerate), centre accessors, constructor guards |
| `map/MapProviderTileZoomTest` | `tileMaxZoom` for all four tile specs, licence/network pass-through, `of()` and `resolved()` fallbacks |
| `ui/navigation/RoutesTest` | the six route builders, checked against their pattern constants |
| `ui/preview/PreviewDraftStoreTest` | draft set/read/clear on the editor→preview hand-off |
| `location/LocationAveragingSigmaTest` | `FixSource.of` provider names, `sigmaOf` per-source floors/fallbacks, altitude through `average()` |
| `data/model/CornerIdentityTest` | `GeoPoint.toCorner`, `List<CornerPoint>.toGeoPoints`, the malformed-position branch of `GeometryCodec.decodeCorners`, `Land.landStatus` |
| `map/offline/PmtilesHeaderGuardTest` | the negative-offset refusal in `PmtilesHeader.read` |
| `data/export/GeoExporterXmlEscapeTest` | apostrophe escaping in the GPX/KML name elements |

Still open and deliberately untouched here — they need Robolectric or a
device, not plain JVM tests: `ShareUtils` (41 missed, all `Context`-bound),
`geoUri`'s labelled branch (`android.net.Uri.encode`), `LocalizedContext`,
`LandRepository`'s DAO-bound paths (needs fake DAOs), and every Compose
screen.

### Measured on this branch

This branch's own CI run re-measured the report with the tests above in place
([run 44127652084](https://github.com/SecretArrow/LandPoint/actions/runs/44127652084),
`feature/coverage-refresh-tests` @ `bac246c7ac85d24af18754121dc3818f21f6679e`,
conclusion success — the e2e job's Maestro flake is `continue-on-error` by
design):

| Scope      | Before (run 44084228761) | After (run 44127652084) | Delta |
|------------|--------------------------|-------------------------|-------|
| Overall LINE | 31.23% (5,046/16,157) | **31.58%** (5,103/16,157) | +0.35 pp |
| `util` LINE  | 87.10% (405/465)      | **90.32%** (420/465)      | +3.22 pp |

Classes that went to 100% line coverage: `GeoUtils` (98.63% — one line left,
the `Uri.encode` label branch), `BoundaryEdits`, `GeoBounds` (both classes),
`MapProvider`, `Routes`, `PreviewDraftStore` (both classes),
`LocationAveraging`, `StoredGeometryKt`, `Land`, `GeoExporter`, and
`PmtilesHeader` (its `Companion.read` keeps one missed line: the
`IOException` guard, unreachable without a device-level read failure).
`map` moved 84.62% → 92.31%, `data/model` 91.91% → 93.53%,
`ui/navigation` 25.00% → 100%.

## How to reproduce locally

```bash
./gradlew :app:testDebugUnitTest :app:koverXmlReportDebug
# XML:  app/build/reports/kover/reportDebug.xml
# HTML: app/build/reports/kover/htmlDebug/index.html
```

The XML report's top-level `<report>` element carries the overall counters;
each `<package>` element carries its own. The `LINE` counter is what this
report uses to match the spec.
