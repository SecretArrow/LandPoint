package com.landpoint.app.ui.preview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.landpoint.app.R
import com.landpoint.app.data.SettingsRepository
import com.landpoint.app.data.model.CornerPoint
import com.landpoint.app.data.model.GeometryCodec
import com.landpoint.app.ui.components.CoachMarkOverlay
import com.landpoint.app.ui.components.LandMap
import com.landpoint.app.ui.components.MapAttribution
import com.landpoint.app.ui.components.MapCorner
import com.landpoint.app.ui.components.MapScaleBar
import com.landpoint.app.ui.components.MapShape
import com.landpoint.app.ui.components.MapSideControls
import com.landpoint.app.ui.components.MapTopChrome
import com.landpoint.app.ui.components.PARCEL_ZOOM
import com.landpoint.app.ui.components.ParcelHero
import com.landpoint.app.ui.components.rememberCoachMark
import com.landpoint.app.ui.components.rememberLandMapController
import com.landpoint.app.ui.components.rememberMapPrefs
import com.landpoint.app.ui.theme.Elevation
import com.landpoint.app.ui.theme.LandPointText
import com.landpoint.app.ui.theme.LocalLandColors
import com.landpoint.app.ui.theme.Spacing
import com.landpoint.app.util.AreaFormat
import com.landpoint.app.util.GeoPoint
import com.landpoint.app.util.GeoUtils
import com.landpoint.app.util.Localization
import com.landpoint.app.util.PolygonMath
import com.landpoint.app.util.ShareUtils
import kotlin.math.roundToInt

/**
 * How much of the window the bottom panel may claim, so a draft with many
 * corners scrolls inside the panel instead of pushing Save and Discard off
 * the screen. The map keeps at least the other half.
 */
private const val PANEL_MAX_HEIGHT_FRACTION = 0.5f

/**
 * Preview of the unsaved shape on its own screen.
 *
 * Lines connect the corners in order automatically (closed when >= 3), so what
 * is seen here is what saving will store. From here the user can save, go back
 * to keep editing (add photos, move corners), or discard — every destructive
 * choice asks first.
 *
 * The redesign (docs/IMPLEMENTATION_PLAN.md, Phase 2) leads with the parcel
 * hero ([ParcelHero]): the name, the area, the status chip and the shape's
 * draw-in live in one card at the top, with the quick actions — navigate,
 * share, export, edit — as assist chips beneath the drawing (§6.3). The map
 * keeps the middle of the screen, and the corner cards at the bottom list
 * every corner's coordinates and accuracy.
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
    val context = LocalContext.current
    // The map's polygon outline draws with the map.overlay.line token rather
    // than raw primary, so it follows §2.1's overlay colours in both themes.
    val overlayLine = LocalLandColors.current.mapOverlayLine.toArgb()

    var showDiscard by remember { mutableStateOf(false) }

    // The first-visit explanation of the quick-actions bar. It hangs in the top
    // column directly below the hero that carries the bar, so it points at the
    // row it describes; it never floats over the Save button (docs/IMPLEMENTATION_PLAN.md, Phase 4).
    val actionsCoach = rememberCoachMark(SettingsRepository.CoachMark.PREVIEW_ACTIONS)
    BackHandler { onBackToEdit() }

    val area = if (points.size >= 3) PolygonMath.areaSqm(points) else null
    val perimeter = when {
        points.size >= 3 -> PolygonMath.perimeterM(points)
        points.size == 2 -> PolygonMath.pathLengthM(points)
        else -> null
    }
    val title = draft.name.ifBlank { stringResource(R.string.preview_title) }
    val areaText = area?.let { stringResource(areaUnit.valueRes, AreaFormat.value(it, areaUnit)) }
    val cornerText = pluralStringResource(R.plurals.boundary_corners, points.size, points.size)
    val perimeterText = perimeter?.let {
        stringResource(R.string.boundary_perimeter, it.roundToInt().toString())
    }
    val summaryLine = if (perimeterText == null) cornerText else "$perimeterText · $cornerText"
    val cornerTemplate = stringResource(R.string.boundary_corner_number)
    val anchor = remember(points) { PolygonMath.centroid(points) ?: points.firstOrNull() }
    val shareBody = remember(draft.corners, title, areaText, summaryLine, cornerTemplate, anchor) {
        draftShareText(title, areaText, summaryLine, anchor, points, cornerTemplate)
    }
    // Export hands the geometry itself to whatever the user picks — the same
    // GeoJSON the save will store, so what leaves the screen is what lands in
    // the database. A draft too short to encode exports as text instead.
    val exportBody = remember(draft.corners, shareBody) {
        GeometryCodec.encodeCorners(draft.corners) ?: shareBody
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
            LandMap(
                style = prefs.style,
                controller = controller,
                accentColour = overlayLine,
                modifier = Modifier.fillMaxSize(),
                shapes = shapes,
                corners = corners,
                contentDescription = stringResource(R.string.preview_title)
            )
        }

        // The hero at the top (§6.3). The chrome's title steps aside while the
        // hero is up — the name is already prominent right below it — and the
        // unsaved draft announces itself through the chip rather than a status
        // it does not have yet.
        Column(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()) {
            MapTopChrome(
                onBack = onBackToEdit,
                title = if (points.isEmpty()) stringResource(R.string.preview_title) else null
            )
            if (points.isNotEmpty()) {
                ParcelHero(
                    title = title,
                    status = null,
                    corners = points,
                    areaText = areaText,
                    statusLabel = stringResource(R.string.preview_status_draft),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    actions = {
                        PreviewQuickActions(
                            onNavigate = {
                                anchor?.let {
                                    ShareUtils.navigateTo(
                                        context = context,
                                        latitude = it.latitude,
                                        longitude = it.longitude,
                                        label = draft.name.takeIf { name -> name.isNotBlank() }
                                    )
                                }
                            },
                            onShare = { ShareUtils.shareText(context, title, shareBody) },
                            onExport = { ShareUtils.shareText(context, title, exportBody) },
                            onEdit = onBackToEdit
                        )
                    }
                )
                if (actionsCoach.visible) {
                    CoachMarkOverlay(
                        title = stringResource(R.string.coachmark_preview_actions_title),
                        body = stringResource(R.string.coachmark_preview_actions_body),
                        dismissLabel = stringResource(R.string.coachmark_dismiss),
                        dontShowAgainLabel = stringResource(R.string.coachmark_dont_show_again),
                        onDismiss = actionsCoach.dismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                    )
                }
            }
        }

        MapSideControls(
            modifier = Modifier.align(Alignment.CenterEnd),
            onZoomIn = controller::zoomIn,
            onZoomOut = controller::zoomOut,
            onMyLocation = null
        )

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
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
                    .padding(horizontal = Spacing.md, vertical = Spacing.md),
                shape = MaterialTheme.shapes.large,
                tonalElevation = Elevation.E2.tonal,
                shadowElevation = Elevation.E3.tonal
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * PANEL_MAX_HEIGHT_FRACTION)
                        .padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    // Figures in the coordinate face (§2.2) so the digits line up.
                    LandPointText.Coordinate(
                        text = summaryLine,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                    Text(
                        stringResource(R.string.preview_auto_line),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (draft.corners.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            itemsIndexed(
                                draft.corners,
                                key = { _, corner -> corner.id }
                            ) { index, corner ->
                                CornerCard(
                                    index = index,
                                    corner = corner,
                                    photoCount = draft.photoCounts[corner.id] ?: 0
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
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
 * One corner of the draft, as a card: its number, its coordinates in the
 * monospace coordinate face (§2.2), and its GPS accuracy when the corner was
 * measured rather than typed. The camera badge keeps the per-corner photo
 * count the previous list row carried.
 */
@Composable
private fun CornerCard(index: Int, corner: CornerPoint, photoCount: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    text = (index + 1).toString(),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.preview_corner_title, index + 1),
                    style = MaterialTheme.typography.titleSmall
                )
                LandPointText.Coordinate(
                    text = GeoUtils.formatDecimal(corner.latitude, corner.longitude),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                corner.accuracyM?.let { accuracy ->
                    LandPointText.Coordinate(
                        text = stringResource(
                            R.string.boundary_corner_accuracy,
                            GeoUtils.formatAccuracy(accuracy)
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (photoCount > 0) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = pluralStringResource(
                        R.plurals.corner_photo_count,
                        photoCount,
                        photoCount
                    ),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * The quick-actions bar (§6.3) — navigate, share, export, edit — as assist
 * chips (§6.2), so every action reads as a word as well as an icon. The row
 * scrolls horizontally because four labelled chips do not fit a 360dp phone.
 */
@Composable
private fun PreviewQuickActions(onNavigate: () -> Unit, onShare: () -> Unit, onExport: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        QuickActionChip(
            icon = Icons.Outlined.Directions,
            label = stringResource(R.string.action_navigate),
            onClick = onNavigate
        )
        QuickActionChip(
            icon = Icons.Filled.Share,
            label = stringResource(R.string.action_share),
            onClick = onShare
        )
        QuickActionChip(
            icon = Icons.Outlined.Upload,
            label = stringResource(R.string.action_export),
            onClick = onExport
        )
        QuickActionChip(
            icon = Icons.Filled.Edit,
            label = stringResource(R.string.action_edit),
            onClick = onEdit
        )
    }
}

@Composable
private fun QuickActionChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    )
}

/**
 * The draft as plain text for the share sheet. The layout mirrors
 * [ShareUtils.landAsText] — name, area, the corner count, then each corner on
 * its own line, then a maps link — so a recipient reads either the same way.
 */
private fun draftShareText(
    title: String,
    areaText: String?,
    summaryLine: String,
    anchor: GeoPoint?,
    corners: List<GeoPoint>,
    cornerTemplate: String
): String = buildString {
    appendLine(title)
    areaText?.let { appendLine(it) }
    appendLine(summaryLine)
    appendLine()
    corners.forEachIndexed { index, corner ->
        appendLine(
            cornerTemplate.format(
                Localization.numberLocale(),
                index + 1,
                GeoUtils.formatDecimal(corner.latitude, corner.longitude)
            )
        )
    }
    appendLine()
    anchor?.let { appendLine(GeoUtils.googleMapsLink(it.latitude, it.longitude)) }
}.trim()
