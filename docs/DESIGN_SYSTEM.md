# LandPoint Design System

**Status:** Reference / in progress
**Audience:** Contributors to the LandPoint Android app (Kotlin + Jetpack Compose + Material 3)
**Scope:** The single source of truth for the visual language of the redesign — color, type, spacing, radius, elevation, motion, z-order, theming rules, status semantics, iconography, components, and empty states.

This document defines the **target** design system for the UI/UX redesign rollout (see `docs/IMPLEMENTATION_PLAN.md`). Where a token is already in the codebase it is marked **defined**; where it is not yet, it is marked **to add** and points at the file it should live in. The existing theme files are the baseline and are not thrown away — they are extended.

---

## 1. Purpose

LandPoint is read at arm's length, outdoors, in daylight, by users standing on the land they are recording. The design system serves three constraints that desktop products never face:

1. **Legibility over polish.** A faint outline is invisible in direct sun; a 14sp label is unreadable from a meter away. Every token below is sized for that condition, not for a screenshot.
2. **One app, one ink.** Before the redesign, cards and outlines drew from Material's purple defaults because only a handful of roles were named in `Color.kt`. The full role set is now spelled out, and this document keeps it that way: any new surface draws from a named role or adds one here first.
3. **Color never carries meaning alone.** Parcel status is shown with color *and* icon *and* label, because the user base includes colorblind users and the app is used in lighting where red and green are indistinguishable.

The system is intentionally small. It is not Material 3 plus a brand layer; it is Material 3 with the LandPoint palette slotted into the named roles, a coordinate-specific monospace style, and a handful of map-overlay tokens Material does not define.

---

## 2. Design tokens

### 2.1 Color

All color lives in `app/src/main/java/com/landpoint/app/ui/theme/Color.kt` and is wired into the M3 color schemes in `app/src/main/java/com/landpoint/app/ui/theme/Theme.kt`. The window background only (the frame Android draws before Compose starts) lives in `app/src/main/res/values/colors.xml` and mirrors `Neutral98` / `Neutral6`.

#### Semantic roles

| Role | Light | Dark (OLED) | Status | Used for |
|---|---|---|---|---|
| `surface` | `Neutral98` `#FAFDF8` | `Neutral6` `#0F1511` | **defined** | App background, cards' base |
| `surfaceVariant` | `Neutral92` `#E9EBE7` | `Neutral30` `#424845` | **defined** | Inset backgrounds, input fields |
| `surfaceContainerLowest` | `#FFFFFF` | `#000000` | **defined** | Cards on a card |
| `surfaceContainerLow` | `Neutral96` | `Neutral10` | **defined** | Sheet & dialog scrims |
| `surfaceContainer` | `Neutral94` | `Neutral12` | **defined** | Bottom sheets |
| `surfaceContainerHigh` | `Neutral92` | `Neutral17` | **defined** | Floating toolbars |
| `surfaceContainerHighest` | `Neutral90` | `Neutral22` | **defined** | Active hover/press |
| `primary` | `Green40` `#1F6F4A` | `Green80` `#8CD9B3` | **defined** | Primary actions, FAB, selection |
| `onPrimary` / `primaryContainer` / `onPrimaryContainer` | White / `Green90` / `Green10` | `Green20` / `Green30` / `Green90` | **defined** | Filled-button text & containers |
| `secondary` | `Brown40` `#7A5A3A` | `Brown80` `#E8C39E` | **defined** | Tonal buttons, secondary chips |
| `tertiary` | `Sand40` `#5F6134` | `Sand80` `#C7CA92` | **defined** | Tertiary accents |
| `accent` | `Green40` (alias) | `Green80` (alias) | **to add** | Single highlight color for the *next* action — alias of primary, kept separate so a redesign can move it independently |
| `success` | `#2E7D32` | `#A5D6A7` | **to add** | Save-confirmed, sync OK, GPS fix good |
| `onSuccess` | `#FFFFFF` | `#003300` | **to add** | Text on success surfaces |
| `successContainer` | `#D7F4D9` | `#1B4A1F` | **to add** | Inline success banners |
| `warning` | `#B26A00` | `#FFCB80` | **to add** | GPS accuracy poor, pending action expiring |
| `onWarning` | `#FFFFFF` | `#3A2400` | **to add** | |
| `warningContainer` | `#FFE5B0` | `#4A3000` | **to add** | |
| `danger` | `ErrorRed` `#BA1A1A` | `ErrorRedDark` `#FFB4AB` | **defined** (as `error`) | Delete, destructive actions, validation error |
| `info` | `#0277BD` | `#7EC8E3` | **to add** | Neutral informational banners, "did you know" |
| `map.overlay.fill` | `Green40` @ 30% alpha | `Green80` @ 35% alpha | **to add** | Polygon fill on the map |
| `map.overlay.line` | `Green40` | `Green80` | **to add** | Polygon outline, walking-track line |
| `map.overlay.vertex` | `#FFFFFF` ring + `Green40` core | `#1B211D` ring + `Green80` core | **to add** | Numbered corner markers |
| `map.overlay.vertexActive` | `Brown40` | `Brown80` | **to add** | The corner being dragged / stepped |
| `map.overlay.accuracy` | `primary` @ 18% alpha | `primary` @ 22% alpha | **to add** | Accuracy circle around position marker |
| `map.overlay.crosshair` | `onSurface` @ 70% | `onSurface` @ 70% | **to add** | Drop-corner crosshair |

The `success` / `warning` / `info` triples do not exist in `Color.kt` yet — they will be added as a `// ---- Semantic ----` block. They are deliberately *not* derived from `tertiary` (the Sand scale) because Sand reads as a warning yellow-green at low luminance, which collides with `success` and `warning`.

#### Neutrals (already defined in `Color.kt`)

The `Neutral*` scale is warmed a few points toward green so white areas read as paper in sunlight, not as a blue-grey screen. The full scale is `Neutral6, 10, 12, 17, 20, 22, 24, 30, 50, 60, 80, 87, 90, 92, 94, 96, 98`. All roles above draw from this scale; do not introduce a `Grey` or `Slate` token.

#### Outlines (already defined)

`Outline` `#717972`, `OutlineDark` `#8B938C`, `OutlineVariant` `#C0C9C1`, `OutlineVariantDark` `#404943`. Used for input borders, dividers, and the unfocused state of segmented controls.

### 2.2 Typography

All type lives in `app/src/main/java/com/landpoint/app/ui/theme/Type.kt` as `LandPointTypography` (a Material 3 `Typography`) plus a standalone `CoordinateStyle`. The scale is **fully defined**; the redesign does not change sizes, only enforces their use.

| Style | Size / Line / Weight | Status | Used for |
|---|---|---|---|
| `displaySmall` | 34 / 42 / Normal | **defined** | Onboarding hero, empty-state title |
| `headlineMedium` | 27 / 34 / SemiBold | **defined** | Detail screen hero name |
| `headlineSmall` | 23 / 30 / SemiBold | **defined** | Section headers |
| `titleLarge` | 22 / 28 / SemiBold | **defined** | Top-app-bar title, dialog title |
| `titleMedium` | 16 / 22 / SemiBold | **defined** | Card titles, list-item primary |
| `titleSmall` | 14 / 20 / Medium | **defined** | Card subtitles, stepper labels |
| `bodyLarge` | 16 / 24 / Normal | **defined** | Body copy, descriptions |
| `bodyMedium` | 14 / 20 / Normal | **defined** | List-item secondary, helper text |
| `bodySmall` | 12 / 17 / Normal | **defined** | Captions, metadata |
| `labelLarge` | 14 / 20 / Medium | **defined** | Button text |
| `labelMedium` | 12 / 16 / Medium | **defined** | Chips, badges |
| `labelSmall` | 11 / 16 / Medium | **defined** | Time stamps, tiny tags |
| `CoordinateStyle` | 13sp monospace | **defined** | Lat/lng, area, accuracy — digits line up |

The redesign introduces three named composable wrappers (in a new `app/src/main/java/com/landpoint/app/ui/theme/TextStyleExt.kt`, **to add**) so screens stop hard-coding `MaterialTheme.typography.titleMedium` and instead call `LandPointText.Title`, `LandPointText.Body`, `LandPointText.Coordinate`. This keeps the indirection in one file if the scale ever changes.

A `displayLarge` and `displayMedium` are **to add** for the Phase 2 parcel-preview hero (animated polygon draw-in); they are not in `Type.kt` today because no screen used them.

### 2.3 Spacing

**To add** as `app/src/main/java/com/landpoint/app/ui/theme/Spacing.kt`. Material 3 has no named spacing scale; without one every `padding(16.dp)` is a magic number, and the redesign's component gallery cannot exist without it.

| Token | dp | Use |
|---|---|---|
| `Spacing.xs` | 4 | Icon-to-label gap inside a chip or button |
| `Spacing.sm` | 8 | Tight vertical rhythm inside a card |
| `Spacing.md` | 12 | Card content padding, list-item leading |
| `Spacing.lg` | 16 | Card-to-card gap, screen edge gutter |
| `Spacing.xl` | 24 | Section separation |
| `Spacing.xxl` | 32 | Empty-state illustration gap |
| `Spacing.fab` | 16 | FAB inset from screen edge |

4/8 grid base. `Spacing.md` (12) is the deliberate exception — it is what separates a card's title from its body and reads better than 8 or 16 at arm's length. Every `padding(...)` in new code references a token; raw `dp` literals are flagged by detekt.

### 2.4 Radius

**Defined** in `app/src/main/java/com/landpoint/app/ui/theme/Shape.kt`:

| Token | dp | Used for |
|---|---|---|
| `extraSmall` | 6 | Text-field corners, small chips |
| `small` | 12 | Cards in a dense list, segmented control tiles |
| `medium` | 16 | Standard cards, bottom-sheet content |
| `large` | 20 | Hero cards, FAB-adjacent surfaces |
| `extraLarge` | 28 | Bottom-sheet handle area, large dialogs |

Pill buttons (radius = half height) are **to add** as `Shapes.Pill = RoundedCornerShape(50)`, used for the central Add speed-dial chip and the floating map toolbar's segmented control. The redesign's card radius stays in the 12–20dp range as specified; `extraLarge` (28) is reserved for sheets.

### 2.5 Elevation / shadow

Material 3 tonal elevation is used, not shadows, except on map overlays where a shadow is the only thing that lifts a marker off a photograph. The tonal-elevation scale is **defined** through `surfaceContainer*` roles above. The shadow scale is **to add** as `app/src/main/java/com/landpoint/app/ui/theme/Elevation.kt`:

| Level | Tonal elevation (dp) | Shadow | Used for |
|---|---|---|---|
| `E0` | 0 | none | Cards in a list (separated by spacing, not shadow) |
| `E1` | 1 | none | Cards on a card |
| `E2` | 3 | none | Bottom sheet (resting) |
| `E3` | 6 | `0 1 3 0 black/20` | Floating map toolbar |
| `E4` | 8 | `0 4 8 0 black/24` | FAB, central Add speed-dial |
| `E5` | 12 | `0 8 12 0 black/26` | Dragged corner handle, modal bottom sheet (active) |

Map markers and corner handles use `E5` shadow + a white ring so they remain visible against satellite imagery and contour lines.

### 2.6 Motion

**To add** as `app/src/main/java/com/landpoint/app/ui/theme/Motion.kt`. Today the app has no motion tokens — the only animations are Compose defaults and a couple of `animate*AsState` calls without shared durations.

| Token | Duration | Easing | Used for |
|---|---|---|---|
| `Motion.instant` | 0ms | — | State swaps with no animation |
| `Motion.fast` | 100ms | `FastOutSlowInEasing` | Ripple, chip selection, press feedback |
| `Motion.standard` | 200ms | `FastOutSlowInEasing` | Sheet slide, FAB show/hide |
| `Motion.slow` | 350ms | `FastOutSlowInEasing` | Polygon draw-in on the parcel preview hero |
| `Motion.emphasized` | 250ms | `CubicBezierEasing(0.2, 0.0, 0.0, 1.0)` | Hero header collapse, page transition |
| `Motion.scrim` | 150ms | `LinearEasing` | Scrim fade behind dialogs |

Reduced-motion: respect `Settings.Global.ANIMATOR_DURATION_SCALE == 0` and fall back to `Motion.instant`. The Phase 2 hero animation is the only place this is user-visible.

### 2.7 Z-order (map overlays)

The map screen stacks overlays in a fixed order so a corner handle never disappears behind a sheet, and so the accuracy circle never sits on top of the position marker. **To add** as a documented constant set in `app/src/main/java/com/landpoint/app/ui/components/LandMapOverlay.kt` (already the file that owns overlays — the z-index constants are the missing piece):

| Layer (low → high) | Content | Owner |
|---|---|---|
| 0 | Basemap tiles (Street / Satellite / Terrain / Imported) | MapLibre |
| 1 | Boundary polygon fill | `LandMapOverlay` GeoJson fill layer |
| 2 | Boundary polygon outline | `LandMapOverlay` line layer |
| 3 | Walking-track line (while recording) | `LandMapOverlay` live line |
| 4 | Numbered corner markers (symbols) | `LandMapOverlay` symbol layer |
| 5 | Accuracy circle | `LandMapOverlay` GeoJson fill |
| 6 | Position marker (the blue dot) | `LandMapOverlay` symbol layer |
| 7 | Drop-corner crosshair | Compose `Canvas` above the map view |
| 8 | Floating map toolbar + segmented control | Compose `Box` |
| 9 | Snackbars | `Scaffold` snackbar host |
| 10 | Modal sheets / dialogs | Compose `ModalBottomSheet` / `Dialog` |

Layers 1–6 are MapLibre layers within one `Style`; 7–10 are Compose. The split is fixed: no Compose overlay is ever placed below the position marker, and no MapLibre layer is ever placed above the floating toolbar.

---

## 3. Theme specification

The app's theme entry point is `LandPointTheme` in `Theme.kt`. It already supports:

- **System-follow by default** — `SettingsRepository.DarkMode.SYSTEM` reads `isSystemInDarkTheme()`. The user may force `LIGHT` or `DARK` from Settings.
- **Light theme** — `LightColors`, paper-warm neutrals, `Green40` primary.
- **Dark theme** — `DarkColors`, using `Neutral6` `#0F1511` as the surface. This is near-black and reads as true dark on OLED panels; the redesign does not move to `#000000` because a 0-value surface makes tonal elevation invisible (cards on cards collapse). A `trueDark` variant that swaps `surface = #000000` and keeps containers at `Neutral6` is **to add** behind a settings toggle for users who want maximum battery saving on OLED, with the trade-off called out in the UI.
- **Dynamic color (Material You)** — supported on Android 12+ when the user opts in (`settings.dynamic_color`). Off by default; the LandPoint palette is the brand.
- **High-contrast theme** — **to add**. Triggered by `Settings.Global.TRANSITION_ANIMATION_SCALE` heuristics and a Settings toggle, this swaps outlines to `onSurface` (full contrast), removes tonal elevation (containers go to flat `surface`), and bumps corner handles to `E5` with a thicker ring. Required for outdoor use in direct sunlight.

### 3.1 Map style adapts

The map style is built by `MapStyleFactory` from `app/src/main/java/com/landpoint/app/map/MapStyleFactory.kt`. The dark-mode map recolouring is governed per-basemap by `BasemapMode.tintForNight`:

- **Street** and **Imported** vector styles are tinted for night (background and labels inverted to dark).
- **Satellite** and **Terrain** are *not* tinted — inverting a photograph makes vegetation magenta, and inverting hillshade turns every valley into a ridge.

The redesign does not change this. It adds overlay-color tokens (§2.1 `map.overlay.*`) that switch with the theme so a polygon outline is `Green80` on the dark map and `Green40` on the light map, regardless of basemap.

### 3.2 Splash and window background

`app/src/main/res/values/themes.xml` and `app/src/main/res/values-night/themes.xml` hold the framework-only theme — `windowBackground`, transparent system bars, splash configuration. `colors.xml` defines `window_background` and `window_background_dark` only, mirroring `Neutral98` and `Neutral6`. **These two values must stay in sync with `Color.kt`** — the comment in `colors.xml` says so, and the redesign preserves that contract.

---

## 4. Parcel status color mapping

Parcels carry a legal status from `LandStatus` (`app/src/main/java/com/landpoint/app/data/model/LandStatus.kt`): `ACTIVE`, `DISPUTED`, `SOLD`, `TRANSFERRED`. Today the enum exposes a string resource (`R.string.land_status_*`) and nothing else — status is shown as a label, not a color.

The redesign introduces a `ParcelStatusVisual` (in a new `app/src/main/java/com/landpoint/app/ui/theme/ParcelStatusVisual.kt`, **to add**) that maps each status to **color + icon + label**. **Color is never the sole signal** — this is the colorblind-safety rule.

| Status | Color token | Icon | Label (en / in) |
|---|---|---|---|
| `ACTIVE` | `success` | `check_circle` | Active / Aktif |
| `DISPUTED` | `warning` | `warning` (triangle) | Disputed / Sengketa |
| `SOLD` | `info` | `sell` (or `attach_money`) | Sold / Terjual |
| `TRANSFERRED` | `secondary` (Brown) | `swap_horiz` | Transferred / Dialihkan |
| (no status / unknown) | `outline` | `help_outline` | — |

Palette is colorblind-safe: success (green) and warning (orange) are distinguishable in deuteranopia and protanopia; `secondary` (brown) and `info` (blue) are distinguishable in tritanopia. The icons are distinct shapes (circle, triangle, currency, arrows), so even a user who sees none of the colors can read the status.

Status is rendered as a chip in three places: list rows, detail hero, and map popups. The chip always carries icon + label; the color is the chip's container fill at 12% alpha with full-saturation color for the icon and label text.

---

## 5. Iconography

**Default set:** Material Symbols Rounded. The app already uses `Icons.Outlined.*` and `Icons.Default.*` throughout (`HomeScreen.kt`, `LandPointNavHost.kt`).

**Custom icons (to add)** in `app/src/main/res/drawable/` — vector XML, 24dp grid, rounded joins:

| Icon | File | Used for |
|---|---|---|
| Corner marker (numbered pin) | `ic_corner_marker.xml` | Map corner handle, list-row shape preview |
| Patok (wooden boundary stake) | `ic_patok.xml` | The corner picker's "stand on the corner" affordance; culturally recognizable in Indonesia |
| Polygon (closed parcel outline) | `ic_polygon.xml` | "Measured boundary" vs single-point indicator |
| Ruler | `ic_ruler.xml` | Distance & bearing tool, area readout |
| Compass | `ic_compass.xml` | Compass mode + onboarding |

The existing `ic_launcher_foreground.xml` (the pin) and `ic_stat_walk.xml` (the walk notification icon) are kept as-is.

Custom icons use `MaterialTheme.colorScheme.onSurface` by default and `primary` when active, matching the rest of the system. No icon ships with a hard-coded color.

---

## 6. Component inventory

Everything below is built on Material 3 components from `androidx.compose.material3`. Where a component is already in use, the file that owns it is named. Where it is **to add**, the planned file is named.

### 6.1 Buttons

| Component | Status | Notes |
|---|---|---|
| Filled button | **defined** (M3 `Button`) | `primary` / `onPrimary` |
| Tonal button | **defined** (M3 `FilledTonalButton`) | `secondaryContainer` / `onSecondaryContainer` |
| Outline button | **defined** (M3 `OutlinedButton`) | Use for "cancel" / secondary actions in dialogs |
| Text button | **defined** (M3 `TextButton`) | Use for "skip" / "undo" in snackbars |
| Icon button | **defined** (M3 `IconButton`) | 48dp touch target, 24dp icon |
| FAB | **defined** (M3 `FloatingActionButton`, `ExtendedFloatingActionButton`) | Used by `HomeScreen` and `LandListScreen` |
| Speed-dial | **to add** | Central Add button that expands to "GPS point / Walk boundary / Type coordinates / Tap map". Replaces the single FAB on the Map tab. `app/src/main/java/com/landpoint/app/ui/components/AddSpeedDial.kt` |

### 6.2 Chips & selection

| Component | Status | Notes |
|---|---|---|
| Filter chip | **to add** (M3 `FilterChip`) | Sort & status filter on the list |
| Assist chip | **to add** (M3 `AssistChip`) | Quick actions on detail hero |
| Input chip | **to add** (M3 `InputChip`) | Owner tags in the editor |
| Status chip | **to add** | Custom — §4 color + icon + label |
| Segmented control | **to add** (M3 `SegmentedButton`) | Map style picker, coordinate format picker |

### 6.3 Cards & lists

| Component | Status | Owner |
|---|---|---|
| Card (filled) | **defined** (M3 `Card`) | `LandCard.kt` |
| List item | **defined** (M3 `ListItem`) | `LandRow.kt`, `HomeScreen.kt` |
| Parcel hero card | **to add** | `app/src/main/java/com/landpoint/app/ui/components/ParcelHero.kt` — collapsing header + animated polygon draw-in (Phase 2) |
| Corner card | **to add** | Numbered corner card in the parcel preview |
| Quick actions bar | **to add** | Row of icon buttons under the hero — navigate, share, export, edit |

### 6.4 Sheets & dialogs

| Component | Status | Notes |
|---|---|---|
| Bottom sheet | **to add** (M3 `ModalBottomSheet`) | Sort picker, filter picker, corner-source picker |
| Dialog (alert) | **defined** (M3 `AlertDialog`) | Delete confirmation, import duplicate handling |
| Snackbar | **defined** (M3 `SnackbarHost`) | **with Undo action** — already used for delete (`PendingDeletes.kt`) |
| Tooltip | **to add** (M3 `RichTooltip`) | First-use tooltips on the corner picker's source buttons |
| Coach mark | **to add** | First-time-use overlay pointing at a tool; non-blocking, dismissable |
| Stepper | **to add** | Onboarding (Phase 4) and the corner-position nudge pad |
| Slider | **to add** (M3 `Slider`) | Map style opacity, GPS averaging window |
| Badge | **to add** (M3 `Badge`) | Count of selected parcels in bulk mode |

### 6.5 Feedback

| Component | Status | Notes |
|---|---|---|
| Snackbar with Undo | **defined** | `PendingDeletes` keeps the deleted item in memory so the snackbar can restore it |
| Inline banner | **to add** | `success` / `warning` / `info` containers — for "GPS accuracy poor" on the map, "backup expired" in settings |
| Progress indicator | **defined** (M3 `LinearProgressIndicator`) | Offline download progress |
| Skeleton loader | **to add** | List rows while the DB query is cold-starting |
| Empty-state illustration | **to add** | See §7 |

### 6.6 Map-specific

| Component | Status | Owner |
|---|---|---|
| Map marker | **defined** | `LandMapOverlay.kt` |
| Corner handle (drag) | **defined** | `LandMapOverlay.kt` + `CornerPickerScreen.kt` |
| Floating map toolbar | **defined** | `MapChrome.kt` |
| Map scale bar | **defined** | `MapScaleBar.kt` |
| Telemetry panel | **defined** | `TelemetryPanel.kt` |
| Drop-corner crosshair | **to add** | Above the map at z-order 7 |
| Polygon draw-in animation | **to add** | Phase 2 parcel preview hero |

---

## 7. Empty-state illustrations

**To add** as vector assets in `app/src/main/res/drawable/` and a shared `EmptyState` composable in `app/src/main/java/com/landpoint/app/ui/components/EmptyState.kt`. Every empty state carries: an illustration, a title (`headlineSmall`), a body (`bodyMedium`), and a single primary action.

| State | Illustration | Title (en / in) | Body (en / in) | Action |
|---|---|---|---|---|
| No parcels saved | Empty field with one corner pin | No land saved yet / Belum ada tanah tersimpan | Tap Save land to record your first GPS location. / Tekan Simpan tanah untuk merekam koordinat GPS pertama Anda. | **Save land** (filled button) — strings already in `strings.xml` |
| No search results | Magnifier over a folded map | No matches / Tidak ditemukan | Try a different search term. / Coba kata pencarian lain. | **Clear search** (text button) |
| No GPS signal | Satellite with a slash | Could not get a GPS fix / Tidak dapat sinyal GPS | Try again outdoors. / Coba lagi di luar ruangan. | **Retry** (filled button) — `msg_no_fix` already in `strings.xml` |
| Location permission denied | Padlock with map | Location permission needed / Izin lokasi diperlukan | LandPoint needs location access to capture GPS coordinates. Nothing leaves your device. / (Indonesian equivalent) | **Grant permission** / **Open settings** — strings already in `strings.xml` |
| Offline | Phone with no bars over a map | No connection / Tidak ada koneksi | Cached tiles will still draw. Imported maps work fully offline. / (Indonesian equivalent) | **Open offline maps** (text button) |
| Import error | Broken file with a map | Import failed / Impor gagal | (Reason from the importer) | **Try again** / **Open help** |

Every illustration is monochrome (`onSurface` at 60% alpha) so it reads in both themes without a separate dark variant, and so the title underneath remains the visual anchor.

---

## 8. Reference: existing theme files

The redesign is built on top of these files. None is thrown away; each is extended.

| File | What it owns | Redesign change |
|---|---|---|
| `app/src/main/java/com/landpoint/app/ui/theme/Color.kt` | Primary / Secondary / Tertiary / Neutral / Outline / Error scales | **Add** `success`, `warning`, `info`, `accent`, `map.overlay.*` semantic tokens |
| `app/src/main/java/com/landpoint/app/ui/theme/Type.kt` | `LandPointTypography` + `CoordinateStyle` | **Add** `displayLarge`, `displayMedium`; wrap in `LandPointText.*` aliases |
| `app/src/main/java/com/landpoint/app/ui/theme/Shape.kt` | `LandPointShapes` (6/12/16/20/28) | **Add** `Pill` (50%) shape |
| `app/src/main/java/com/landpoint/app/ui/theme/Theme.kt` | `LandPointTheme`, light/dark color schemes, dynamic-color opt-in, system-bar icon tint | **Add** high-contrast variant; **add** `trueDark` toggle |
| `app/src/main/res/values/colors.xml` | `window_background`, `window_background_dark` | No change — keep mirroring `Neutral98` / `Neutral6` |
| `app/src/main/res/values/themes.xml` | Framework `Theme.LandPoint`, splash theme | No change |
| `app/src/main/res/values-night/themes.xml` | Dark framework theme | No change |

**New files (to add)** in `app/src/main/java/com/landpoint/app/ui/theme/`:

- `Spacing.kt` — the 4/8 grid
- `Elevation.kt` — tonal + shadow levels
- `Motion.kt` — duration + easing tokens
- `TextStyleExt.kt` — `LandPointText.*` aliases
- `ParcelStatusVisual.kt` — §4 status mapping

**New files (to add)** in `app/src/main/java/com/landpoint/app/ui/components/`:

- `AddSpeedDial.kt`, `EmptyState.kt`, `ParcelHero.kt`, `StatusChip.kt`, `InlineBanner.kt`, `SkeletonLoader.kt`, `CoachMark.kt`

**New assets (to add)** in `app/src/main/res/drawable/`:

- `ic_corner_marker.xml`, `ic_patok.xml`, `ic_polygon.xml`, `ic_ruler.xml`, `ic_compass.xml`, and one monochrome illustration per empty state in §7.

---

## 9. Glossary

- **Token** — a named, theme-aware value (color, dp, sp, duration). The only合法 way to set a visual property in new code.
- **Role** — a Material 3 semantic slot (`primary`, `surface`, etc.). Tokens fill roles; components consume roles.
- **Mark** — a corner that has not yet been accepted into the outline (per `PendingMark.kt` and `CornerDraft.kt`). The redesign uses the same word in UI copy.
- **Patok** — a wooden stake driven at a boundary corner (Indonesian). Culturally the right word for what a GPS corner represents on the ground; the custom `ic_patok` icon makes the onboarding "stand on the corner" instruction instantly recognizable.
