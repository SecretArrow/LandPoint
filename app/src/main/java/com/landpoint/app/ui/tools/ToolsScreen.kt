package com.landpoint.app.ui.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.landpoint.app.R
import com.landpoint.app.ui.theme.Elevation
import com.landpoint.app.ui.theme.LandPointText
import com.landpoint.app.ui.theme.Spacing

/**
 * One card of the Tools grid: an icon, a title and a one-line summary, drawn
 * at the design system's E1 (docs/DESIGN_SYSTEM.md §2.5) — the grid itself is
 * the E0 surface and each card lifts one tonal step off it.
 *
 * On one line, not a wrapped parameter list: ktlint's android_studio style
 * wants a signature on one line whenever it fits the project's 140 columns,
 * the same rule that collapsed buildEdgeFeatures in its day.
 */
private data class ToolCard(val icon: ImageVector, val titleRes: Int, val summaryRes: Int, val destination: ToolDestination)

/** Where a Tools card leads, resolved by the nav host (see [ToolsScreen]). */
private enum class ToolDestination {
    /** The export desk in `ui/reports` — every way records leave the phone. */
    EXPORT,

    /** The backup section of Settings. */
    BACKUP,

    /** The restore section of Settings. */
    RESTORE,

    /** The reports screen itself — the summary and the per-parcel PDF. */
    REPORTS,

    /** Downloaded map areas. */
    OFFLINE,

    /** The bundled help centre. */
    HELP,

    /** Version, privacy, licences. */
    ABOUT,

    /** The owners list, a former tab now living beside the parcels. */
    OWNERS
}

// The grid in the order the plan names it: Export / Backup / Restore / Reports
// / Offline maps / Help / About (docs/IMPLEMENTATION_PLAN.md Phase 5, item 2).
//
// Compass is deliberately absent. The app's compass is not a standalone
// instrument: it points from where the user stands to one saved parcel's pin
// (CompassViewModel does checkNotNull(savedStateHandle["landId"])), so there is
// nothing honest for a general card to open. It stays reachable where it makes
// sense — from a parcel's detail screen and the editor.
//
// Owners rides at the end. The four-tab restructure removed the Owners tab
// (ADR-002) and the design system names no new home for the list; the Tools
// grid is the catch-all for destinations that are neither map, parcels nor
// settings, so it keeps the list one named tap away (the Parcels header's
// owners stat opens the same screen for the opposite walking direction).
private val toolCards = listOf(
    ToolCard(Icons.Outlined.FileUpload, R.string.tools_export_title, R.string.tools_export_summary, ToolDestination.EXPORT),
    ToolCard(Icons.Outlined.Backup, R.string.tools_backup_title, R.string.tools_backup_summary, ToolDestination.BACKUP),
    ToolCard(Icons.Outlined.SettingsBackupRestore, R.string.tools_restore_title, R.string.tools_restore_summary, ToolDestination.RESTORE),
    ToolCard(Icons.Outlined.Assessment, R.string.nav_reports, R.string.tools_reports_summary, ToolDestination.REPORTS),
    ToolCard(Icons.Outlined.Map, R.string.tools_offline_title, R.string.tools_offline_summary, ToolDestination.OFFLINE),
    ToolCard(Icons.Outlined.HelpOutline, R.string.tools_help_title, R.string.settings_help_center_hint, ToolDestination.HELP),
    ToolCard(Icons.Outlined.Info, R.string.tools_about_title, R.string.tools_about_summary, ToolDestination.ABOUT),
    ToolCard(Icons.Outlined.People, R.string.nav_owners, R.string.tools_owners_summary, ToolDestination.OWNERS)
)

/**
 * The Tools tab (docs/DECISIONS.md ADR-002): the card grid that replaced the
 * Reports tab. Every card opens an existing destination — the screen owns no
 * behaviour of its own, it is a table of contents for the actions that used to
 * be scattered across Home, the Reports tab and Settings.
 *
 * Backup, Restore and About all land on Settings because that is where those
 * sections live; there is no nav argument to jump a reader to a section, so
 * the summaries say which one they are opening rather than pretending the
 * cards are screens of their own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onOpenReports: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenOfflineMaps: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenOwners: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_tools)) }) }
    ) { padding ->
        // Adaptive cells: one column holds a card at 160dp, so the grid is two
        // cards wide on the narrowest supported phone and grows columns on
        // tablets without a per-width branch to maintain.
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            items(toolCards, key = { it.titleRes }) { card ->
                ToolCardCell(card = card, onOpen = {
                    when (card.destination) {
                        ToolDestination.EXPORT, ToolDestination.REPORTS -> onOpenReports()
                        ToolDestination.BACKUP, ToolDestination.RESTORE, ToolDestination.ABOUT -> onOpenSettings()
                        ToolDestination.OFFLINE -> onOpenOfflineMaps()
                        ToolDestination.HELP -> onOpenHelp()
                        ToolDestination.OWNERS -> onOpenOwners()
                    }
                })
            }
        }
    }
}

/**
 * One E1 card of the grid. Tapped through `clickable` on the modifier rather
 * than the experimental Card(onClick) overload, matching how the rest of the
 * codebase wires tappable cards.
 */
@Composable
private fun ToolCardCell(card: ToolCard, onOpen: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.E1.tonal)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(
                card.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            LandPointText.Title(stringResource(card.titleRes))
            Text(
                stringResource(card.summaryRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
