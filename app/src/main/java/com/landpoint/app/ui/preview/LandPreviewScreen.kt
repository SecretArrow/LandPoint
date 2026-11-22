package com.landpoint.app.ui.preview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.landpoint.app.ui.components.MapControl
import com.landpoint.app.ui.components.MapCorner
import com.landpoint.app.ui.components.MapScaleBar
import com.landpoint.app.ui.components.MapShape
import com.landpoint.app.ui.components.MapSideControls
import com.landpoint.app.ui.components.MapTopChrome
import com.landpoint.app.ui.components.PARCEL_ZOOM
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
 * The quick-action column hangs this far below the top chrome, and the coach
 * mark that explains the column is inset from the end edge by the same
 * amount, so both keep clear of the column itself and of the map's own zoom
 * controls on the right edge.
 */
private val QUICK_ACTIONS_CLEARANCE = 64.dp

/**
 * Preview of the unsaved shape on its own screen.
 *
 * The redesign leads with the map: the canvas fills the whole window so the
 * boundary can be judged against the ground, and every chrome element floats
 * over it rather than claiming a slice of the screen. The parcel hero card is
 * gone from here on purpose: it repeated the name, the area and a second,
 * smaller copy of the very shape the map already draws at full size, and the
 * card covered the ground the user came here to judge. The draft's name now
 * rides in the chrome's title chip instead; the quick actions stay a vertical
 * column of floating controls under the top chrome, where a thumb reaches;
 * and the corner list lives in a bottom sheet that opens on demand instead of
 * a panel that always claims half the screen. The two actions that matter —
 * Save and Edit — sit in a sticky bar at the bottom with Save taking twice the
 * room, so the primary is dominant and the secondary is unmistakable.
 *
 * Lines connect the corners in order automatically (closed when >= 3), so what
 * is seen here is what saving will store. From here the user can save, go back
 * to keep editing (add photos, move corners), or discard — every destructive
 * choice asks first. Discard moved out of the bottom bar into the top chrome
 * (a trash icon), so the bar holds only the two actions a draft normally ends
 * with, and the destructive one is not one missed tap away from Save.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    var showCornerSheet by remember { mutableStateOf(false) }

    // The first-visit explanation of the quick-actions column. It hangs off the
    // top chrome directly beside the column it describes; it never floats over
    // the Save button.
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

    if (showCornerSheet && draft.corners.isNotEmpty()) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCornerSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = summaryLine,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    stringResource(R.string.preview_auto_line),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.5f),
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
        }
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
                contentDescription = stringResource(R.string.preview_title),
                // The preview is a thumbnail of the shape, not a measurement:
                // the per-side metre figures crowd it, and the shape is the
                // thing the user came here to read at a glance. The shape
                // summary screen keeps the labels, where the map is full-size.
                showEdgeLabels = false
            )
        }

        // Top chrome: back, the title, and the destructive Discard as an icon —
        // moved out of the bottom bar so the bar holds only the two actions a
        // draft normally ends with, and Discard is never one missed tap away
        // from Save.
        Column(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()) {
            MapTopChrome(
                onBack = onBackToEdit,
                // With the hero card gone the draft's name lives in the
                // chrome's title chip — the one line of context a preview
                // needs, and nothing that covers the map.
                title = if (points.isEmpty()) stringResource(R.string.preview_title) else title,
                actions = {
                    if (points.isNotEmpty()) {
                        MapControl(
                            icon = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.discard_throw),
                            onClick = { showDiscard = true }
                        )
                    }
                }
            )
            if (points.isNotEmpty() && actionsCoach.visible) {
                CoachMarkOverlay(
                    title = stringResource(R.string.coachmark_preview_actions_title),
                    body = stringResource(R.string.coachmark_preview_actions_body),
                    dismissLabel = stringResource(R.string.coachmark_dismiss),
                    dontShowAgainLabel = stringResource(R.string.coachmark_dont_show_again),
                    onDismiss = actionsCoach.dismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                        // Inset past the quick-action column so the card sits
                        // beside the column it describes, not underneath its
                        // buttons.
                        .padding(end = QUICK_ACTIONS_CLEARANCE)
                )
            }
        }

        // The right-hand controls. The map's own zoom column stays at the
        // vertical centre; the quick actions hang below the top chrome on the
        // same edge, so a thumb travelling up the right side meets every
        // floating control in order without crossing the map's title chip.
        if (points.isNotEmpty()) {
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
                onCorners = { showCornerSheet = true },
                cornerCount = draft.corners.size,
                cornerListLabel = cornerText,
                onEdit = onBackToEdit,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = QUICK_ACTIONS_CLEARANCE, end = Spacing.sm)
            )
        }

        MapSideControls(
            modifier = Modifier.align(Alignment.CenterEnd),
            onZoomIn = controller::zoomIn,
            onZoomOut = controller::zoomOut,
            onMyLocation = null
        )

        // The bottom bar holds the two actions a draft normally ends with —
        // Edit (outlined, secondary) and Save (filled, primary, twice the
        // weight) — plus the scale and attribution the map needs. The corner
        // list is gone from here: it lives in the bottom sheet opened from the
        // quick-actions column, so the bar is one row, not a panel.
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
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                shape = MaterialTheme.shapes.large,
                tonalElevation = Elevation.E3.tonal,
                shadowElevation = Elevation.E3.tonal
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBackToEdit,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.preview_back_edit)) }
                    Button(
                        onClick = onSave,
                        enabled = points.isNotEmpty(),
                        modifier = Modifier.weight(2f)
                    ) { Text(stringResource(R.string.preview_save)) }
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
 * The quick-actions column — navigate, share, export, the corner list, and
 * edit — as floating map controls ([MapControl]) stacked vertically on the
 * right edge, where a thumb reaches after the zoom column. The chips the old
 * layout scrolled horizontally became these because four labelled chips never
 * fit a 360dp phone, while five labelled controls stacked in the gutter
 * always do; each still reads as a word in its content description even when
 * only the icon is visible. The corner-list control carries the count as a
 * badge so the draft's size is visible without opening the sheet.
 */
@Composable
private fun PreviewQuickActions(
    onNavigate: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onCorners: () -> Unit,
    onEdit: () -> Unit,
    cornerCount: Int,
    cornerListLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        QuickActionControl(
            icon = Icons.Outlined.Directions,
            contentDescription = stringResource(R.string.action_navigate),
            onClick = onNavigate
        )
        QuickActionControl(
            icon = Icons.Filled.Share,
            contentDescription = stringResource(R.string.action_share),
            onClick = onShare
        )
        QuickActionControl(
            icon = Icons.Outlined.Upload,
            contentDescription = stringResource(R.string.action_export),
            onClick = onExport
        )
        QuickActionControl(
            icon = Icons.Filled.List,
            contentDescription = cornerListLabel,
            onClick = onCorners,
            badge = cornerCount.takeIf { it > 0 }?.toString()
        )
        Spacer(modifier = Modifier.size(Spacing.xs))
        QuickActionControl(
            icon = Icons.Filled.Edit,
            contentDescription = stringResource(R.string.action_edit),
            onClick = onEdit
        )
    }
}

/**
 * One floating quick-action control: a [MapControl] with an optional count
 * badge pinned to its top-right corner. The badge is how the corner-list
 * control shows the draft's size without opening the sheet.
 */
@Composable
private fun QuickActionControl(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null
) {
    Box(modifier = modifier) {
        MapControl(
            icon = icon,
            contentDescription = contentDescription,
            onClick = onClick
        )
        if (badge != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = Spacing.xs)
                )
            }
        }
    }
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
