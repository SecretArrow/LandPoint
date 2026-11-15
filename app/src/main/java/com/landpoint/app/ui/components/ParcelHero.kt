package com.landpoint.app.ui.components

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.landpoint.app.data.SettingsRepository
import com.landpoint.app.data.model.LandStatus
import com.landpoint.app.ui.theme.Elevation
import com.landpoint.app.ui.theme.LandPointText
import com.landpoint.app.ui.theme.LandPointTheme
import com.landpoint.app.ui.theme.LocalLandColors
import com.landpoint.app.ui.theme.Motion
import com.landpoint.app.ui.theme.Spacing
import com.landpoint.app.util.GeoPoint
import kotlin.math.cos
import kotlin.math.min

/**
 * How far the drawn shape keeps from the hero's edges, as a fraction of the
 * canvas' smaller side. The same breathing room the map's own overlays keep,
 * so the thumbnail reads as the same shape at the same confidence.
 */
private const val HERO_INSET_FRACTION = 0.12f

/** Outline width, as a fraction of the canvas' smaller side. */
private const val STROKE_WIDTH_FRACTION = 0.006f

/** Corner marker radius, as a fraction of the canvas' smaller side. */
private const val CORNER_RADIUS_FRACTION = 0.014f

/** Corner core radius relative to its white ring — matches the map's markers. */
private const val CORNER_INNER_RADIUS_FRACTION = 0.75f

/** Resting height of the polygon canvas while the hero is fully expanded. */
private val HeroPolygonHeight = 112.dp

/**
 * The parcel preview hero (docs/DESIGN_SYSTEM.md §6.3, parcel hero card).
 *
 * One card that says what is being looked at before anything else on the
 * screen is read: the parcel's name prominent, its area beneath it in the
 * coordinate figures, its status as a [StatusChip], and the shape itself —
 * drawn on a [Canvas] from the corner coordinates, no map engine involved.
 *
 * The outline draws in over [Motion.slow] (350 ms, FastOutSlowIn): the fill
 * and each corner marker fade in as the line reaches them, so the shape is
 * seen being drawn rather than appearing. Reduced motion (the system
 * animator scale at 0) skips straight to the finished drawing; fractional
 * scales are honoured by Compose's own duration scaling, which reads the
 * same system flag.
 *
 * Collapsing-header capable: [collapseFraction] takes the hero from fully
 * expanded (`1f`) to collapsed (`0f`). The name and the action row stay put;
 * the area fades out and the polygon canvas shrinks with the fraction, so a
 * screen that scrolls can tuck the hero under a collapsing header without
 * this component owning any scroll state of its own. This screen passes the
 * default — a preview is short, and the shape is the point of it.
 *
 * @param title    the parcel's name, shown prominent (headlineSmall).
 * @param areaText pre-localised area summary; null hides the line.
 * @param status   parcel status for the chip; pass a [statusLabel] when the
 *   record has no status yet (a draft) so the chip still announces itself.
 * @param corners  the corners to draw; empty draws nothing and skips the
 *   draw-in animation.
 * @param collapseFraction 1f expanded → 0f collapsed.
 * @param actions  quick actions rendered under the polygon (the §6.3 quick
 *   actions bar); null omits the row.
 */
@Composable
fun ParcelHero(
    title: String,
    status: LandStatus?,
    corners: List<GeoPoint>,
    modifier: Modifier = Modifier,
    areaText: String? = null,
    statusLabel: String? = null,
    collapseFraction: Float = 1f,
    actions: (@Composable () -> Unit)? = null
) {
    // The draw-in's lifecycle. Reduced motion snaps to the finished drawing;
    // otherwise every new set of corners draws in again (the draft can change
    // while the preview is open).
    val context = LocalContext.current
    val reduceMotion = remember { readAnimatorScale(context) <= 0f }
    val drawIn = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(corners, reduceMotion) {
        if (corners.isEmpty() || reduceMotion) {
            drawIn.snapTo(1f)
        } else {
            drawIn.snapTo(0f)
            drawIn.animateTo(targetValue = 1f, animationSpec = Motion.tweenSlow())
        }
    }

    val collapse = collapseFraction.coerceIn(0f, 1f)

    // A card floating over the map carries the floating-toolbar elevation
    // (docs/DESIGN_SYSTEM.md §2.5, E3) — tonal lift plus the shadow that
    // keeps it readable over satellite imagery.
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Elevation.E3.tonal,
        shadowElevation = Elevation.E3.tonal
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    // The area is one of the figures CoordinateStyle exists
                    // for (§2.2: lat/lng, area, accuracy); it fades with the
                    // collapse while the name holds.
                    if (areaText != null && collapse > 0f) {
                        LandPointText.Coordinate(
                            text = areaText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alpha(collapse)
                        )
                    }
                }
                StatusChip(status = status, label = statusLabel)
            }

            val polygonHeight = HeroPolygonHeight * collapse
            if (polygonHeight > 0.dp && corners.isNotEmpty()) {
                ParcelHeroPolygon(
                    corners = corners,
                    progress = drawIn.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(polygonHeight)
                )
            }

            actions?.invoke()
        }
    }
}

/**
 * The hero's shape, painted by hand (docs/DESIGN_SYSTEM.md §6.6, polygon
 * draw-in). A simple linear normalisation of the corner bounds through
 * [ParcelHeroGeometry.project] — no MapLibre, so the hero renders before the
 * basemap has anything to say.
 *
 * Colours come from the map-overlay tokens ([LocalLandColors]) rather than
 * ad-hoc mixes, so this thumbnail and the map's own polygon are the same ink
 * in both themes.
 */
@Composable
private fun ParcelHeroPolygon(corners: List<GeoPoint>, progress: Float, modifier: Modifier = Modifier) {
    val landColors = LocalLandColors.current
    Canvas(modifier = modifier) {
        val inset = size.minDimension * HERO_INSET_FRACTION
        val projected = ParcelHeroGeometry.project(
            points = corners,
            width = size.width,
            height = size.height,
            inset = inset
        )
        if (projected.isEmpty()) return@Canvas

        val path = Path()
        projected.forEachIndexed { index, (x, y) ->
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        if (corners.size >= 3) path.close()

        // Faint fill so a closed polygon reads as a shape, not just a line;
        // it fades in with the outline so the fill never appears ahead of it.
        if (corners.size >= 3 && progress > 0f) {
            drawPath(
                path = path,
                color = landColors.mapOverlayFill.copy(alpha = landColors.mapOverlayFill.alpha * progress)
            )
        }

        // Outline clipped to progress × totalLength — the "draws in" effect.
        val measure = PathMeasure().apply { setPath(path, false) }
        val totalLen = measure.length
        if (totalLen > 0f && progress > 0f) {
            val partial = Path()
            measure.getSegment(0f, totalLen * progress, partial, true)
            drawPath(
                path = partial,
                color = landColors.mapOverlayLine,
                style = Stroke(width = size.minDimension * STROKE_WIDTH_FRACTION)
            )
        }

        // Corner markers pop in sequentially: marker i begins fading at
        // progress = i / cornerCount and is fully visible one window later.
        // White ring then vertex core — the map's own corner style (§2.1,
        // map.overlay.vertex), so the thumbnail reads as the same markers.
        val cornerCount = projected.size
        val fadeWindow = 1f / cornerCount
        val cornerR = size.minDimension * CORNER_RADIUS_FRACTION
        projected.forEachIndexed { index, (x, y) ->
            val fade = ((progress - index.toFloat() / cornerCount) / fadeWindow).coerceIn(0f, 1f)
            if (fade <= 0f) return@forEachIndexed
            drawCircle(
                color = landColors.mapOverlayVertexRing.copy(alpha = fade),
                radius = cornerR,
                center = Offset(x, y)
            )
            drawCircle(
                color = landColors.mapOverlayVertex.copy(alpha = fade),
                radius = cornerR * CORNER_INNER_RADIUS_FRACTION,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * The projection behind the hero canvas, kept pure so the bounds
 * normalisation is unit-testable ([com.landpoint.app.ui.components.ParcelHeroGeometryTest]).
 *
 * Fits the corners' bounding box into a `width` × `height` box, `inset` in
 * from each edge, preserving aspect ratio; longitudes are compressed by
 * cos(latitude) so a shape drawn near the equator and one near the poles
 * keep their real proportions on screen. Screen y grows downwards while
 * latitude grows north, so the axis is flipped here — north is up.
 */
object ParcelHeroGeometry {

    fun project(points: List<GeoPoint>, width: Float, height: Float, inset: Float): List<Pair<Float, Float>> {
        if (points.isEmpty()) return emptyList()
        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLon = points.minOf { it.longitude }
        val maxLon = points.maxOf { it.longitude }
        val midLatRad = Math.toRadians((minLat + maxLat) / 2.0)
        val lonScale = cos(midLatRad).coerceAtLeast(0.01)
        // Degenerate bounds (a lone corner, or corners stacked on one spot)
        // coerce to a hair of span rather than dividing by zero.
        val spanY = (maxLat - minLat).coerceAtLeast(1e-9)
        val spanX = ((maxLon - minLon) * lonScale).coerceAtLeast(1e-9)
        val boxW = (width - inset * 2).coerceAtLeast(1f)
        val boxH = (height - inset * 2).coerceAtLeast(1f)
        val scale = min(boxW / spanX, boxH / spanY)
        val offsetX = inset + (boxW - spanX * scale).toFloat() / 2f
        val offsetY = inset + (boxH - spanY * scale).toFloat() / 2f
        return points.map { point ->
            val x = offsetX + ((point.longitude - minLon) * lonScale * scale).toFloat()
            val y = offsetY + ((maxLat - point.latitude) * scale).toFloat()
            x to y
        }
    }
}

/**
 * The system animator scale, the reduced-motion flag (docs/DESIGN_SYSTEM.md
 * §2.6). Compose's own animations read the same flag for their duration
 * scaling; the value here only decides whether the draw-in runs at all.
 */
private fun readAnimatorScale(context: Context): Float = try {
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE
    )
} catch (e: Settings.SettingNotFoundException) {
    1f
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun ParcelHeroLightPreview() {
    LandPointTheme(darkMode = SettingsRepository.DarkMode.LIGHT) {
        PreviewParcelHero(status = LandStatus.ACTIVE)
    }
}

@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF0F1511)
@Composable
private fun ParcelHeroDarkPreview() {
    LandPointTheme(darkMode = SettingsRepository.DarkMode.DARK) {
        PreviewParcelHero(status = LandStatus.DISPUTED)
    }
}

@Preview(name = "Collapsed", showBackground = true)
@Composable
private fun ParcelHeroCollapsedPreview() {
    LandPointTheme(darkMode = SettingsRepository.DarkMode.LIGHT) {
        PreviewParcelHero(status = LandStatus.ACTIVE, collapseFraction = 0f)
    }
}

/** A Bandung-shaped sample plot, shared by the previews above. */
@Composable
private fun PreviewParcelHero(status: LandStatus, collapseFraction: Float = 1f) {
    val corners = listOf(
        GeoPoint(latitude = -6.914744, longitude = 107.609810, accuracyM = 3.2),
        GeoPoint(latitude = -6.914044, longitude = 107.609810, accuracyM = 4.1),
        GeoPoint(latitude = -6.914044, longitude = 107.610810, accuracyM = 2.8),
        GeoPoint(latitude = -6.914744, longitude = 107.610810)
    )
    ParcelHero(
        title = "Sawah Cibadak",
        status = status,
        corners = corners,
        areaText = "1,204 m²",
        statusLabel = null,
        collapseFraction = collapseFraction
    )
}
