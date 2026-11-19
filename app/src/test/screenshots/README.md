# Screenshot baselines

These PNGs are the committed baselines for the visual regression gate
(`.github/workflows/screenshot.yml`, Phase 7 of `docs/IMPLEMENTATION_PLAN.md`).
Every test class in `../java/com/landpoint/app/ui/screenshot/` renders one or
more surfaces on the JVM through Roborazzi + Robolectric Native Graphics and
compares what it draws against the image of the same name here. A PR that
changes what a screen looks like fails the job until the baseline is refreshed
— which is the point.

## File naming

`<surface>_<light|dark>_<en|in>.png` — theme and locale are baked into the
name because they are baked into the qualifiers each test runs under
(`@Config(qualifiers = ...)`). Composites with caller-provided strings are
locale-independent by construction and carry only `_en`.

## Recording a refreshed baseline

```bash
./gradlew recordRoborazziDebug
```

That runs the same tests in write mode and overwrites these files with the
current look. Review the diff, and commit the PNGs together with the code
change. CI can also do it: run the **Screenshots** workflow with
`mode = record` and copy the uploaded artifact over this directory.

## What the map area shows

Robolectric cannot load MapLibre's native renderer, so on the JVM every map
bearing screen draws the app's own fallback message (see `MapLibreInit` and
`LandMap`) instead of tiles. That fallback is exactly what gets captured: the
gate covers the chrome around the map — sheets, bars, cards, buttons — while
tile rendering itself stays with the Maestro e2e job on a real emulator.

## Determinism

- Animations are frozen (`settleAnimations()` puts the animator scale at 0,
  the same "reduced motion" path the app honours), so a capture is always the
  settled frame.
- The preview's coach mark is marked seen before the first frame; otherwise
  its asynchronous DataStore read could land on either side of a capture.
- Same Robolectric version, same SDK level (34), same qualifiers — the bytes
  are reproducible. A robolectric upgrade is expected to change rendering
  enough to require a one-time baseline refresh.
