package com.landpoint.app.ui.gallery

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.landpoint.app.R
import com.landpoint.app.data.SettingsRepository
import com.landpoint.app.data.model.LandStatus
import com.landpoint.app.ui.components.StatusChip
import com.landpoint.app.ui.theme.CoordinateStyle
import com.landpoint.app.ui.theme.Elevation
import com.landpoint.app.ui.theme.ElevationLevel
import com.landpoint.app.ui.theme.LandPointText
import com.landpoint.app.ui.theme.LandPointTheme
import com.landpoint.app.ui.theme.LandPointTypography
import com.landpoint.app.ui.theme.Motion
import com.landpoint.app.ui.theme.Spacing
import com.landpoint.app.ui.theme.darkLandExtendedColors
import com.landpoint.app.ui.theme.lightLandExtendedColors
import java.util.Locale

// ---- Pure token tables -----------------------------------------------------
//
// The gallery is a mirror of docs/DESIGN_SYSTEM.md §2, so it reads the tokens
// through the same pure helpers the theme uses (lightLandExtendedColors /
// darkLandExtendedColors) rather than re-listing Color.kt constants. Keeping
// the tables pure makes them unit-testable and lets the swatch grid label
// BOTH themes' values no matter which theme is currently displayed.

/** One row of the color grid: the design token and its light/dark values. */
internal data class GalleryColorSwatch(val token: String, val light: Color, val dark: Color)

/**
 * The semantic feedback colors (success / warning / info / accent) and the
 * map-overlay tokens, each paired across both themes, in the order
 * docs/DESIGN_SYSTEM.md §2.1 lists them.
 */
internal fun galleryColorSwatches(): List<GalleryColorSwatch> {
    val light = lightLandExtendedColors()
    val dark = darkLandExtendedColors()
    return listOf(
        GalleryColorSwatch("success", light.success, dark.success),
        GalleryColorSwatch("onSuccess", light.onSuccess, dark.onSuccess),
        GalleryColorSwatch("successContainer", light.successContainer, dark.successContainer),
        GalleryColorSwatch("warning", light.warning, dark.warning),
        GalleryColorSwatch("onWarning", light.onWarning, dark.onWarning),
        GalleryColorSwatch("warningContainer", light.warningContainer, dark.warningContainer),
        GalleryColorSwatch("info", light.info, dark.info),
        GalleryColorSwatch("accent", light.accent, dark.accent),
        GalleryColorSwatch("map.overlay.fill", light.mapOverlayFill, dark.mapOverlayFill),
        GalleryColorSwatch("map.overlay.line", light.mapOverlayLine, dark.mapOverlayLine),
        GalleryColorSwatch("map.overlay.vertex", light.mapOverlayVertex, dark.mapOverlayVertex),
        GalleryColorSwatch("map.overlay.vertexRing", light.mapOverlayVertexRing, dark.mapOverlayVertexRing),
        GalleryColorSwatch("map.overlay.vertexActive", light.mapOverlayVertexActive, dark.mapOverlayVertexActive),
        GalleryColorSwatch("map.overlay.accuracy", light.mapOverlayAccuracy, dark.mapOverlayAccuracy),
        GalleryColorSwatch("map.overlay.crosshair", light.mapOverlayCrosshair, dark.mapOverlayCrosshair)
    )
}

/** Packed-ARGB hex label, e.g. `#FF2E7D32` — the alpha byte shows on overlays. */
internal fun Color.toHexLabel(): String = String.format(Locale.ROOT, "#%08X", toArgb())

/** One row of the spacing scale: the token name and its dp value. */
internal data class GallerySpacingToken(val name: String, val size: Dp)

/** The named spacing scale, docs/DESIGN_SYSTEM.md §2.3, in order. */
internal fun gallerySpacingTokens(): List<GallerySpacingToken> = listOf(
    GallerySpacingToken("xs", Spacing.xs),
    GallerySpacingToken("sm", Spacing.sm),
    GallerySpacingToken("md", Spacing.md),
    GallerySpacingToken("lg", Spacing.lg),
    GallerySpacingToken("xl", Spacing.xl),
    GallerySpacingToken("xxl", Spacing.xxl),
    GallerySpacingToken("fab", Spacing.fab)
)

/** One row of the elevation scale: the level name and its tonal/shadow spec. */
internal data class GalleryElevationToken(val name: String, val level: ElevationLevel)

/** The elevation scale, docs/DESIGN_SYSTEM.md §2.5, E0 through E5. */
internal fun galleryElevationTokens(): List<GalleryElevationToken> = listOf(
    GalleryElevationToken("E0", Elevation.E0),
    GalleryElevationToken("E1", Elevation.E1),
    GalleryElevationToken("E2", Elevation.E2),
    GalleryElevationToken("E3", Elevation.E3),
    GalleryElevationToken("E4", Elevation.E4),
    GalleryElevationToken("E5", Elevation.E5)
)

/** One row of the motion demo: a named tween the box animates with. */
internal data class GalleryMotionToken(val name: String, val durationMillis: Int, val easing: Easing)

/** The replayable motion demo covers fast / standard / slow (§2.6). */
internal fun galleryMotionTokens(): List<GalleryMotionToken> = listOf(
    GalleryMotionToken("fast", Motion.fast, Motion.standardEasing),
    GalleryMotionToken("standard", Motion.standard, Motion.standardEasing),
    GalleryMotionToken("slow", Motion.slow, Motion.standardEasing)
)

/** One row of the type scale: the style name and its TextStyle. */
internal data class GalleryTextStyleToken(val name: String, val style: TextStyle)

/** Every style LandPointTypography defines, plus the Coordinate style (§2.2). */
internal fun galleryTextStyleTokens(): List<GalleryTextStyleToken> = listOf(
    GalleryTextStyleToken("displaySmall", LandPointTypography.displaySmall),
    GalleryTextStyleToken("headlineMedium", LandPointTypography.headlineMedium),
    GalleryTextStyleToken("headlineSmall", LandPointTypography.headlineSmall),
    GalleryTextStyleToken("titleLarge", LandPointTypography.titleLarge),
    GalleryTextStyleToken("titleMedium", LandPointTypography.titleMedium),
    GalleryTextStyleToken("titleSmall", LandPointTypography.titleSmall),
    GalleryTextStyleToken("bodyLarge", LandPointTypography.bodyLarge),
    GalleryTextStyleToken("bodyMedium", LandPointTypography.bodyMedium),
    GalleryTextStyleToken("bodySmall", LandPointTypography.bodySmall),
    GalleryTextStyleToken("labelLarge", LandPointTypography.labelLarge),
    GalleryTextStyleToken("labelMedium", LandPointTypography.labelMedium),
    GalleryTextStyleToken("labelSmall", LandPointTypography.labelSmall),
    GalleryTextStyleToken("CoordinateStyle", CoordinateStyle)
)

// ---- Screen ----------------------------------------------------------------

/**
 * The component gallery (docs/IMPLEMENTATION_PLAN.md, Phase 2 — debug builds
 * only). A single scrolling page of every design-system token and component,
 * with a segmented toggle at the top that re-themes the whole page between
 * system / light / dark so a reviewer never has to leave Settings to compare
 * the two palettes. The color grid additionally labels the light and dark
 * values of every token side by side, so both palettes are visible at once.
 *
 * Reachability is decided by DebugRoutes: no release build registers the
 * destination, so this composable is only ever entered from a debug APK.
 *
 * Specimen strings ("The quick brown fox…", sample coordinates) are literal
 * on purpose: they are typographic samples, not UI copy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentGalleryScreen(onBack: () -> Unit) {
    var darkMode by remember { mutableStateOf(SettingsRepository.DarkMode.SYSTEM) }

    // The gallery re-themes itself, nested inside the app theme, so the toggle
    // works regardless of what Settings chose. LandPointTheme also keeps the
    // system bars in step with whichever mode is displayed.
    LandPointTheme(darkMode = darkMode) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.gallery_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl)
            ) {
                GalleryThemeToggle(selected = darkMode, onSelect = { darkMode = it })
                GalleryColorsSection()
                GallerySpacingSection()
                GalleryElevationSection()
                GalleryMotionSection()
                GalleryTypographySection()
                GalleryStatusSection()
            }
        }
    }
}

@Composable
private fun GalleryThemeToggle(selected: SettingsRepository.DarkMode, onSelect: (SettingsRepository.DarkMode) -> Unit) {
    val modes = SettingsRepository.DarkMode.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
            ) {
                Text(stringResource(mode.labelRes))
            }
        }
    }
}

@Composable
private fun GallerySection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        content()
    }
}

// ---- Colors (§2.1) ---------------------------------------------------------

@Composable
private fun GalleryColorsSection() {
    val swatches = remember { galleryColorSwatches() }
    GallerySection(title = stringResource(R.string.gallery_section_colors)) {
        swatches.forEach { swatch ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(swatch.token, style = MaterialTheme.typography.titleSmall)
                    // The value pair is the point of the grid: both palettes
                    // stay readable whatever the toggle is set to.
                    Text(
                        stringResource(R.string.gallery_light_value, swatch.light.toHexLabel()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.gallery_dark_value, swatch.dark.toHexLabel()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                GallerySwatch(color = swatch.light)
                GallerySwatch(color = swatch.dark)
            }
        }
    }
}

@Composable
private fun GallerySwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(Spacing.xxl)
            .clip(MaterialTheme.shapes.small)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
            .background(color)
    )
}

// ---- Spacing (§2.3) --------------------------------------------------------

@Composable
private fun GallerySpacingSection() {
    val tokens = remember { gallerySpacingTokens() }
    GallerySection(title = stringResource(R.string.gallery_section_spacing)) {
        tokens.forEach { token ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    token.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.width(Spacing.xxl)
                )
                Text(
                    "${token.size.value.toInt()} dp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(Spacing.xxl)
                )
                // The bar is the token drawn at its own width; the two column
                // labels sit on fixed 32dp lanes so the bars start on one
                // shared baseline.
                Box(
                    modifier = Modifier
                        .width(token.size)
                        .height(Spacing.lg)
                        .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall)
                )
            }
        }
    }
}

// ---- Elevation (§2.5) ------------------------------------------------------

@Composable
private fun GalleryElevationSection() {
    val tokens = remember { galleryElevationTokens() }
    GallerySection(title = stringResource(R.string.gallery_section_elevation)) {
        // Every tile is a surface on `surface`, so the tonal ladder shows as
        // the M3 container shift and E3+ add the real shadow on top of it.
        tokens.forEach { token ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Surface(
                    modifier = Modifier.size(width = 88.dp, height = 48.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = token.level.tonal,
                    shadowElevation = token.level.tonal
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(token.name, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        "tonal ${token.level.tonal.value.toInt()} dp",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        shadowLabel(token.level),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun shadowLabel(level: ElevationLevel): String = level.shadow?.let { spec ->
    "shadow ${spec.offsetX.value.toInt()}/${spec.offsetY.value.toInt()}/" +
        "${spec.blurRadius.value.toInt()} @ ${(spec.color.alpha * 100).toInt()}%"
} ?: "no shadow"

// ---- Motion (§2.6) ---------------------------------------------------------

@Composable
private fun GalleryMotionSection() {
    // The replay counter drives every box at once: each press flips the
    // target and each tween re-runs at its own duration, so fast and slow
    // visibly diverge from the same start line.
    var replay by remember { mutableIntStateOf(0) }
    val target = if (replay % 2 == 0) 0f else 1f
    val tokens = remember { galleryMotionTokens() }

    GallerySection(title = stringResource(R.string.gallery_section_motion)) {
        OutlinedButton(onClick = { replay += 1 }) {
            Text(stringResource(R.string.gallery_replay))
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        tokens.forEach { token ->
            val progress by animateFloatAsState(
                targetValue = target,
                animationSpec = Motion.tweenSpec<Float>(
                    durationMillis = token.durationMillis,
                    easing = token.easing
                ),
                label = "gallery_motion_${token.name}"
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    "${token.name} · ${token.durationMillis} ms",
                    style = MaterialTheme.typography.titleSmall
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Spacing.xl)
                ) {
                    // A 200dp travel lane so the three durations are told
                    // apart at a glance from the same button press.
                    Box(
                        modifier = Modifier
                            .offset(x = (progress * 200).dp)
                            .size(Spacing.lg)
                            .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall)
                    )
                }
            }
        }
    }
}

// ---- Typography (§2.2) -----------------------------------------------------

@Composable
private fun GalleryTypographySection() {
    val tokens = remember { galleryTextStyleTokens() }
    GallerySection(title = stringResource(R.string.gallery_section_typography)) {
        tokens.forEach { token ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    token.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("The quick brown fox jumps over the lazy dog", style = token.style)
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            "LandPointText wrappers",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LandPointText.Title("LandPointText.Title — parcel name")
        LandPointText.Body("LandPointText.Body — one line of body copy.")
        LandPointText.Coordinate("−6.200013, 106.816676 · 1.24 ha")
    }
}

// ---- Components (§4, §6.2) -------------------------------------------------

@Composable
private fun GalleryStatusSection() {
    GallerySection(title = stringResource(R.string.gallery_section_status)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            LandStatus.entries.forEach { status ->
                StatusChip(status)
            }
            // The §4 unknown row: outline color, help icon, explicit label.
            StatusChip(status = null, label = stringResource(R.string.gallery_status_unknown))
        }
        Text(
            stringResource(R.string.gallery_components_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
