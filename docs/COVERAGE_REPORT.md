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

- **CI run**: https://github.com/SecretArrow/LandPoint/actions/runs/43456361814
- **Branch / SHA**: `main` @ `b043eacd171389683a9957eb4d370cbb21eb4b4f`
  ("Merge pull request #17 from SecretArrow/develop")
- **Run conclusion**: success
- **Artifact**: `coverage-report` (1,871,204 bytes, not expired)
- **XML report**: `app/build/reports/kover/reportDebug.xml` (JaCoCo XML format)

The XML report was parsed with the standard JaCoCo `<counter type="LINE">`
elements (and `INSTRUCTION` as a cross-check).

## Results

### Overall (`:app` module, all packages)

| Counter      | Missed   | Covered | Total   | Coverage |
|--------------|----------|---------|---------|----------|
| LINE         | 10,181   | 3,952   | 14,133  | **27.96%** |
| INSTRUCTION  | 122,468  | 28,307  | 150,775 | 18.77%  |
| BRANCH       | 4,370    | 1,577   | 5,947   | 26.52%  |
| METHOD       | 1,702    | 700     | 2,402   | 29.14%  |
| CLASS        | 865      | 248     | 1,113   | 22.28%  |

**Overall line coverage: 27.96%** — **does NOT meet the 60% target.**

### Domain logic — `com.landpoint.app.util`

| Counter      | Missed | Covered | Total | Coverage |
|--------------|--------|---------|-------|----------|
| LINE         | 95     | 361     | 456   | **79.17%** |
| INSTRUCTION  | 647    | 2,116   | 2,763 | 76.53%  |

**Domain (`util`) line coverage: 79.17%** — **does NOT meet the 80% target.**

It misses by 0.83 percentage points (5 covered lines short of the 360 needed
to clear 80%, or stated differently: ~5 of the 95 missed lines would have to
be covered to push `util` over the bar).

## Verdict

Neither target is currently met by the latest `main` build.

- The `util` package is *just* under the 80% domain gate (79.17% vs 80%).
  Closing it is a tractable, single-package task: 5 additional covered lines
  in `com.landpoint.app.util` would clear the bar.
- The overall gate has a much larger gap (27.96% vs 60%). Most of the missed
  lines are concentrated in the `com.landpoint.app.ui.*` packages, many of
  which currently sit at 0.00% line coverage because they are Compose screens
  exercised only by Maestro e2e flows, not by unit tests. Lifting the overall
  number to 60% would require either unit-test coverage of those screens or a
  conscious decision to exclude Compose-UI packages from the overall gate.

## Per-package breakdown (LINE coverage, sorted by coverage %)

| Package                            | Missed | Covered | Total | Line % |
|------------------------------------|--------|---------|-------|--------|
| com/landpoint/app/ui/theme         | 3      | 215     | 218   | 98.62% |
| com/landpoint/app/data/model       | 30     | 252     | 282   | 89.36% |
| com/landpoint/app/map              | 34     | 187     | 221   | 84.62% |
| com/landpoint/app/util (domain)    | 95     | 361     | 456   | 79.17% |
| com/landpoint/app                  | 24     | 89      | 113   | 78.76% |
| com/landpoint/app/data             | 97     | 290     | 387   | 74.94% |
| com/landpoint/app/data/export      | 305    | 794     | 1,099 | 72.25% |
| com/landpoint/app/ui/onboarding    | 51     | 88      | 139   | 63.31% |
| com/landpoint/app/data/db          | 840    | 846     | 1,686 | 50.18% |
| com/landpoint/app/location         | 363    | 401     | 764   | 52.49% |
| com/landpoint/app/ui/lands         | 422    | 179     | 601   | 29.78% |
| com/landpoint/app/map/offline      | 411    | 155     | 566   | 27.39% |
| com/landpoint/app/ui/security      | 72     | 18      | 90    | 20.00% |
| com/landpoint/app/ui/components    | 1,201  | 59      | 1,260 | 4.68%  |
| com/landpoint/app/ui/owners        | 580    | 16      | 596   | 2.68%  |
| com/landpoint/app/ui               | 160    | 2       | 162   | 1.23%  |
| com/landpoint/app/ui/edit          | 2,524  | 0       | 2,524 | 0.00%  |
| com/landpoint/app/ui/preview       | 146    | 0       | 146   | 0.00%  |
| com/landpoint/app/ui/shape         | 246    | 0       | 246   | 0.00%  |
| com/landpoint/app/ui/settings      | 766    | 0       | 766   | 0.00%  |
| com/landpoint/app/ui/reports       | 253    | 0       | 253   | 0.00%  |
| com/landpoint/app/ui/offline       | 475    | 0       | 475   | 0.00%  |
| com/landpoint/app/ui/home          | 222    | 0       | 222   | 0.00%  |
| com/landpoint/app/ui/detail        | 566    | 0       | 566   | 0.00%  |
| com/landpoint/app/ui/map           | 126    | 0       | 126   | 0.00%  |
| com/landpoint/app/ui/navigation    | 6      | 0       | 6     | 0.00%  |
| com/landpoint/app/ui/compass       | 163    | 0       | 163   | 0.00%  |

## How to reproduce locally

```bash
./gradlew :app:testDebugUnitTest :app:koverXmlReportDebug
# XML:  app/build/reports/kover/reportDebug.xml
# HTML: app/build/reports/kover/htmlDebug/index.html
```

The XML report's top-level `<report>` element carries the overall counters;
each `<package>` element carries its own. The `LINE` counter is what this
report uses to match the spec.
