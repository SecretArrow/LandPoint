package com.landpoint.app.ui.theme

import androidx.compose.ui.graphics.Color

// Land / earth palette — green for growth, warm brown as the secondary.
//
// The full Material 3 role set is spelled out rather than left to
// lightColorScheme()'s purple defaults: only a handful of roles were named
// before, so every surface, outline and container in the app was drawing from
// a palette that had nothing to do with LandPoint.

// ---- Primary: growing green ----
val Green10 = Color(0xFF00210F)
val Green20 = Color(0xFF00391D)
val Green30 = Color(0xFF00522C)
val Green40 = Color(0xFF1F6F4A)
val Green80 = Color(0xFF8CD9B3)
val Green90 = Color(0xFFB6F2D4)

// ---- Secondary: worked earth ----
val Brown10 = Color(0xFF2A1806)
val Brown20 = Color(0xFF432B14)
val Brown30 = Color(0xFF5D4128)
val Brown40 = Color(0xFF7A5A3A)
val Brown80 = Color(0xFFE8C39E)
val Brown90 = Color(0xFFFFDCC0)

// ---- Tertiary: dry grass ----
val Sand10 = Color(0xFF1B1D00)
val Sand20 = Color(0xFF303300)
val Sand30 = Color(0xFF474A08)
val Sand40 = Color(0xFF5F6134)
val Sand80 = Color(0xFFC7CA92)
val Sand90 = Color(0xFFE3E6AC)

// ---- Neutrals ----
// Warmed a few points towards the primary so white areas read as paper under
// sunlight rather than as a blue-grey screen.
val Neutral6 = Color(0xFF0F1511)
val Neutral10 = Color(0xFF171D19)
val Neutral12 = Color(0xFF1B211D)
val Neutral17 = Color(0xFF252B27)
val Neutral20 = Color(0xFF2C322E)
val Neutral22 = Color(0xFF303631)
val Neutral24 = Color(0xFF343A36)
val Neutral30 = Color(0xFF424845)
val Neutral50 = Color(0xFF717874)
val Neutral60 = Color(0xFF8B928E)
val Neutral80 = Color(0xFFC1C9C4)
val Neutral87 = Color(0xFFDDE4DF)
val Neutral90 = Color(0xFFE1E3DF)
val Neutral92 = Color(0xFFE9EBE7)
val Neutral94 = Color(0xFFEFF1ED)
val Neutral96 = Color(0xFFF4F6F2)
val Neutral98 = Color(0xFFFAFDF8)

val Outline = Color(0xFF717972)
val OutlineDark = Color(0xFF8B938C)
val OutlineVariant = Color(0xFFC0C9C1)
val OutlineVariantDark = Color(0xFF404943)

// ---- Error ----
val ErrorRed = Color(0xFFBA1A1A)
val ErrorRedDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainer = Color(0xFFFFDAD6)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainer = Color(0xFF410002)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// ---- Semantic ----
// Feedback colors the M3 role set has no slot for. They are deliberately NOT
// derived from tertiary (the Sand scale): Sand reads as a warning yellow-green
// at low luminance, which collides with both success and warning. Dark
// variants follow the same `Xxx` / `XxxDark` convention as Error above.
val Success = Color(0xFF2E7D32)
val SuccessDark = Color(0xFFA5D6A7)
val OnSuccess = Color(0xFFFFFFFF)
val OnSuccessDark = Color(0xFF003300)
val SuccessContainer = Color(0xFFD7F4D9)
val SuccessContainerDark = Color(0xFF1B4A1F)

val Warning = Color(0xFFB26A00)
val WarningDark = Color(0xFFFFCB80)
val OnWarning = Color(0xFFFFFFFF)
val OnWarningDark = Color(0xFF3A2400)
val WarningContainer = Color(0xFFFFE5B0)
val WarningContainerDark = Color(0xFF4A3000)

val Info = Color(0xFF0277BD)
val InfoDark = Color(0xFF7EC8E3)

// Single highlight color for the *next* action. An alias of primary, kept
// separate so a redesign can move it independently of the brand green.
val AccentLight = Green40
val AccentDark = Green80

// ---- Map overlays ----
// §2.1 roles Material does not define. They switch with the theme so a polygon
// outline is Green80 on the dark map and Green40 on the light map, regardless
// of basemap (docs/DESIGN_SYSTEM.md §3.1). Fill and accuracy are primary at a
// fixed alpha so satellite imagery stays readable underneath.
val MapOverlayFill = Green40.copy(alpha = 0.30f)
val MapOverlayFillDark = Green80.copy(alpha = 0.35f)

val MapOverlayLine = Green40
val MapOverlayLineDark = Green80

// Numbered corner markers: a ring (light on light maps, near-black on dark)
// around the line color core, so a marker reads on any basemap.
val MapOverlayVertexRing = Color.White
val MapOverlayVertexRingDark = Neutral12
val MapOverlayVertex = Green40
val MapOverlayVertexDark = Green80

// The corner being dragged / stepped.
val MapOverlayVertexActive = Brown40
val MapOverlayVertexActiveDark = Brown80

// Accuracy circle around the position marker.
val MapOverlayAccuracy = Green40.copy(alpha = 0.18f)
val MapOverlayAccuracyDark = Green80.copy(alpha = 0.22f)

// Drop-corner crosshair: onSurface at 70% in both themes.
val MapOverlayCrosshair = Neutral10.copy(alpha = 0.70f)
val MapOverlayCrosshairDark = Neutral90.copy(alpha = 0.70f)
