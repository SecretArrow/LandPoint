package com.landpoint.app.ui.preview

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.landpoint.app.R
import com.landpoint.app.data.SettingsRepository
import com.landpoint.app.ui.components.LandMap
import com.landpoint.app.ui.components.MapAttribution
import com.landpoint.app.ui.components.MapCorner
import com.landpoint.app.ui.components.MapScaleBar
import com.landpoint.app.ui.components.MapShape
import com.landpoint.app.ui.components.MapSideControls
import com.landpoint.app.ui.components.MapTopChrome
import com.landpoint.app.ui.components.PARCEL_ZOOM
import com.landpoint.app.ui.components.rememberLandMapController
import com.landpoint.app.ui.components.rememberMapPrefs
import com.landpoint.app.util.AreaFormat
import com.landpoint.app.util.GeoPoint
import com.landpoint.app.util.GeoUtils
import com.landpoint.app.util.PolygonMath
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt

// Hero draw-in tuning (spec §4.3). The outline draws in over DRAW_IN_DURATION_MS,
// then the hero overlay cross-fades to the underlying map over REVEAL_DURATION_MS
// so the final state is the normal map-rendered polygon, not a second outline.
private const val DRAW_IN_DURATION_MS = 800
private const val REVEAL_DURATION_MS = 300
private const val HERO_INSET_FRACTION = 0.12f
private const val STROKE_WIDTH_FRACTION = 0.006f
private const val CORNER_RADIUS_FRACTION = 0.014f
private const val CORNER_INNER_RADIUS_FRACTION = 0.75f
private const val SHAPE_FILL_ALPHA = 0.22f

/**
 * Preview of the unsaved shape on its own screen.
 *
 * Lines connect the corners in order automatically (closed when >= 3), so what
 * is seen here is what saving will store. From here the user can save, go back
 * to keep editing (add photos, move corners), or discard — every destructive
 * choice asks first.
 */
@Composable
fun LandPreviewScreen(
    onBackToEdit: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    areaUnit: SettingsRepository.AreaUnit = SettingsRepository.AreaUnit.SQM
) {
    val draft = remember { PreviewDraftStore.draft }
    val points = remember(draft) { draft.corners.map { it.toGeoPoint() } }
    val prefs = rememberMapPrefs()
    val controller = rememberLandMapController()
    val accent = MaterialTheme.colorScheme.primary.toArgb()

    // Hero draw-in animation (spec §4.3). The polygon outline draws in over
    // DRAW_IN_DURATION_MS with FastOutSlowInEasing; corner markers pop in
    // sequentially as the outline reaches them; then the hero overlay
    // cross-fades to the underlying map. Reduced-motion users get the final
    // state immediately — the system animator scale is the same flag Compose's
    // own animations honour, so this stays consistent with the rest of the app.
    val context = LocalContext.current
    val reduceMotion = remember {
        val scale = try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE
            )
        } catch (e: Settings.SettingNotFoundException) {
            1f
        }
        scale <= 0f
    }
    val drawProgress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val reveal = remember { Animatable(if (reduceMotion) 1f else 0f) }

    var showDiscard by remember { mutableStateOf(false) }
    BackHandler { onBackToEdit() }

    val area = if (points.size >= 3) PolygonMath.areaSqm(points) else null
    val perimeter = when {
        points.size >= 3 -> PolygonMath.perimeterM(points)
        points.size == 2 -> PolygonMath.pathLengthM(points)
        else -> null
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_body)) },
            confirmButton = {
                TextButton(onClick = { showDiscard = false }) {
                    Text(stringResource(R.string.discard_keep))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDiscard = false
                    onDiscard()
                }) {
                    Text(stringResource(R.string.discard_throw))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (points.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.preview_empty))
            }
        } else {
            val shapes = remember(points) {
                listOf(MapShape(id = "preview", points = points))
            }
            val corners = remember(draft) {
                draft.corners.mapIndexed { index, corner ->
                    MapCorner(
                        id = corner.id,
                        latitude = corner.latitude,
                        longitude = corner.longitude,
                        label = (index + 1).toString(),
                        selected = false
                    )
                }
            }
            LaunchedEffect(points) {
                controller.frameOnce(points, PARCEL_ZOOM)
            }
            // Hero draw-in lifecycle. Re-runs if the draft changes while the
            // screen is open; reduced-motion jumps straight to the final state.
            LaunchedEffect(points, reduceMotion) {
                if (points.isEmpty()) return@LaunchedEffect
                if (reduceMotion) {
                    drawProgress.snapTo(1f)
                    reveal.snapTo(1f)
                } else {
                    drawProgress.snapTo(0f)
                    reveal.snapTo(0f)
                    drawProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = DRAW_IN_DURATION_MS,
                            easing = FastOutSlowInEasing
                        )
                    )
                    reveal.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = REVEAL_DURATION_MS,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            }
            LandMap(
                style = prefs.style,
                controller = controller,
                accentColour = accent,
                // The map fades in as the hero overlay fades out, so the user
                // sees the drawn-in outline cross-fade to the map's polygon.
                modifier = Modifier.fillMaxSize().alpha(reveal.value),
                shapes = shapes,
                corners = corners,
                contentDescription = stringResource(R.string.preview_title)
            )
            // Hero overlay sits above the map (so the map can cross-fade in
            // underneath it) and below the chrome — the top bar, side controls
            // and bottom sheet stay visible throughout the draw-in.
            val heroAlpha = 1f - reveal.value
            if (heroAlpha > 0f) {
                ParcelHeroDrawIn(
                    points = points,
                    progress = drawProgress.value,
                    outlineColor = MaterialTheme.colorScheme.primary,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxSize().alpha(heroAlpha)
                )
            }
        }

        Column(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()) {
            MapTopChrome(
                onBack = onBackToEdit,
                title = draft.name.ifBlank { stringResource(R.string.preview_title) }
            )
        }

        MapSideControls(
            modifier = Modifier.align(Alignment.CenterEnd),
            onZoomIn = controller::zoomIn,
            onZoomOut = controller::zoomOut,
            onMyLocation = null
        )

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                MapScaleBar(controller = controller, imperial = prefs.imperial)
                Box(modifier = Modifier.weight(1f))
                MapAttribution(prefs.mode)
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 3.dp,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (area != null) {
                        Text(
                            stringResource(areaUnit.valueRes, AreaFormat.value(area, areaUnit)),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    val cornerText = pluralStringResource(
                        R.plurals.boundary_corners,
                        points.size,
                        points.size
                    )
                    val periText = perimeter?.let {
                        stringResource(R.string.boundary_perimeter, it.roundToInt().toString())
                    }
                    Text(
                        if (periText == null) cornerText else "$periText · $cornerText",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.preview_auto_line),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (draft.corners.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            itemsIndexed(
                                draft.corners,
                                key = { _, corner -> corner.id }
                            ) { index, corner ->
                                val count = draft.photoCounts[corner.id] ?: 0
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.boundary_corner_number,
                                            index + 1,
                                            GeoUtils.formatDecimal(
                                                corner.latitude,
                                                corner.longitude
                                            )
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (count > 0) {
                                        Icon(
                                            Icons.Outlined.PhotoCamera,
                                            contentDescription = pluralStringResource(
                                                R.plurals.corner_photo_count,
                                                count,
                                                count
                                            ),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBackToEdit,
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.preview_back_edit)) }
                        Button(
                            onClick = onSave,
                            enabled = points.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.preview_save)) }
                    }
                    TextButton(onClick = { showDiscard = true }) {
                        Text(stringResource(R.string.discard_throw))
                    }
                }
            }
        }
    }
}

/**
 * The draw-in overlay itself: a [Canvas] that paints the polygon outline
 * progressively (clipped to `progress × totalLength` via [PathMeasure]) and
 * fades each corner marker in sequentially as the outline reaches it.
 *
 * The projection mirrors [com.landpoint.app.ui.components.LandShapeThumb]'s so
 * the shape the user sees draw in is the shape they will see on the map
 * underneath once the cross-fade completes.
 */
@Composable
private fun ParcelHeroDrawIn(
    points: List<GeoPoint>,
    progress: Float,
    outlineColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        // Hide the underlying map so the draw-in reads as the focal element
        // rather than a second outline painted over the map's.
        drawRect(color = backgroundColor)

        if (points.isEmpty()) return@Canvas

        val inset = size.minDimension * HERO_INSET_FRACTION
        val projected = projectParcelPoints(
            points = points,
            width = size.width,
            height = size.height,
            inset = inset
        )
        if (projected.isEmpty()) return@Canvas

        val path = Path()
        projected.forEachIndexed { index, (x, y) ->
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        if (points.size >= 3) path.close()

        // Faint fill so a closed polygon reads as a shape, not just a line; it
        // fades in with the outline so the fill never appears ahead of it.
        if (points.size >= 3 && progress > 0f) {
            drawPath(
                path = path,
                color = outlineColor.copy(alpha = SHAPE_FILL_ALPHA * progress)
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
                color = outlineColor,
                style = Stroke(width = size.minDimension * STROKE_WIDTH_FRACTION)
            )
        }

        // Corner markers pop in sequentially: corner i begins fading at
        // progress = i / cornerCount and is fully visible by (i + 1) / cornerCount.
        val cornerCount = projected.size
        val fadeWindow = if (cornerCount > 0) 1f / cornerCount else 1f
        val cornerR = size.minDimension * CORNER_RADIUS_FRACTION
        for (i in projected.indices) {
            val (x, y) = projected[i]
            val startFraction = i.toFloat() / cornerCount
            val fade = ((progress - startFraction) / fadeWindow).coerceIn(0f, 1f)
            if (fade <= 0f) continue
            // White ring then accent fill — matches the map's corner style
            // (LAYER_CORNER: accent fill with a white stroke) so the cross-fade
            // to the map is not a colour jump.
            drawCircle(
                color = Color.White.copy(alpha = fade),
                radius = cornerR,
                center = Offset(x, y)
            )
            drawCircle(
                color = outlineColor.copy(alpha = fade),
                radius = cornerR * CORNER_INNER_RADIUS_FRACTION,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * Projects geographic coordinates into a [width] × [height] box, [inset] in from
 * each edge, preserving aspect ratio and the cos(latitude) longitude correction.
 *
 * Mirrors [com.landpoint.app.ui.components.LandShapeThumb]'s projection so the
 * hero outline is recognisable as the same shape the map shows underneath.
 */
private fun projectParcelPoints(points: List<GeoPoint>, width: Float, height: Float, inset: Float): List<Pair<Float, Float>> {
    if (points.isEmpty()) return emptyList()
    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLon = points.minOf { it.longitude }
    val maxLon = points.maxOf { it.longitude }
    val midLatRad = Math.toRadians((minLat + maxLat) / 2.0)
    val lonScale = cos(midLatRad).coerceAtLeast(0.01)
    val spanY = (maxLat - minLat).coerceAtLeast(1e-9)
    val spanX = ((maxLon - minLon) * lonScale).coerceAtLeast(1e-9)
    val boxW = (width - inset * 2).coerceAtLeast(1f)
    val boxH = (height - inset * 2).coerceAtLeast(1f)
    val scale = min(boxW / spanX, boxH / spanY)
    val offsetX = inset + (boxW - spanX * scale).toFloat() / 2f
    val offsetY = inset + (boxH - spanY * scale).toFloat() / 2f
    return points.map { point ->
        val x = offsetX + ((point.longitude - minLon) * lonScale * scale).toFloat()
        // Screen y grows downwards; latitude grows north, so y is flipped.
        val y = offsetY + ((maxLat - point.latitude) * scale).toFloat()
        x to y
    }
}
