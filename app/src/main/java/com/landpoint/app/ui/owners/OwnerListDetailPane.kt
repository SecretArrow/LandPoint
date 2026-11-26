package com.landpoint.app.ui.owners

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.landpoint.app.R
import com.landpoint.app.ui.navigation.Routes

/**
 * The pane's own "nothing chosen" destination.
 *
 * Private, and never handed to the app's own NavController: it exists so the
 * pane has something to show before a choice is made, and so closing a record
 * has somewhere to go back to.
 */
private const val OWNER_NOTHING_CHOSEN = "owner_pane_nothing_chosen"

/**
 * Share of the width the list keeps.
 *
 * The same split the land pane strikes: the record is the thing being read —
 * a person, their details, the parcels they hold — so it takes the larger
 * half; the list only has to stay wide enough for a name and a parcel count
 * on one line.
 */
private const val LIST_SHARE = 0.4f

/**
 * The owner book with the chosen record beside it, for a window wide enough.
 *
 * [com.landpoint.app.ui.lands.LandListDetailPane] explains why the pane runs a
 * navigation host of its own rather than holding the chosen id in a piece of
 * state; the reasoning carries over whole — [OwnerDetailScreen]'s ViewModel
 * reads the owner id out of its navigation entry, so a navigation entry is
 * what it has to be given, and that entry is what keeps the choice alive
 * across rotation and process death. What differs is the door the user came
 * in by: Parcels is a tab the user never leaves, while Owners is a pushed
 * screen (ADR-002), so the list keeps [OwnerListScreen]'s up arrow and
 * [onBack] still means "leave the screen". The record's back is the pane's
 * own: it closes the record and empties the pane, because the list never went
 * anywhere.
 *
 * Adding an owner, editing one, and opening a land from the record all stay
 * full-screen jobs on the app's own back stack: they are work with an end,
 * and a job with an end wants the whole window whatever size it is.
 *
 * Insets are left to the screens, as in the land pane: each column carries
 * its own Scaffold whose top app bar pads itself by the status-bar inset
 * exactly once, and the pane between them applies none — so neither column
 * can double the inset and neither can slide beneath it.
 */
@Composable
fun OwnerListDetailPane(
    onNewOwner: () -> Unit,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenLand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val paneNav = rememberNavController()
    val ownerIdArg = listOf(navArgument(Routes.ARG_OWNER_ID) { type = NavType.StringType })

    Row(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(LIST_SHARE)) {
            OwnerListScreen(
                onOpenOwner = { id -> paneNav.showOwnerInPane(Routes.ownerDetail(id)) },
                onNewOwner = onNewOwner,
                onBack = onBack
            )
        }

        VerticalDivider()

        NavHost(
            navController = paneNav,
            startDestination = OWNER_NOTHING_CHOSEN,
            modifier = Modifier.weight(1f - LIST_SHARE)
        ) {
            composable(OWNER_NOTHING_CHOSEN) { OwnerNothingChosen() }

            composable(Routes.OWNER_DETAIL, arguments = ownerIdArg) {
                OwnerDetailScreen(
                    // The arrow does not go back anywhere here — the list never
                    // left. It closes the record and leaves the pane empty.
                    onBack = { paneNav.popBackStack() },
                    onEdit = onEdit,
                    onOpenLand = onOpenLand
                )
            }
        }
    }
}

/**
 * Shows one record, replacing whatever was on show.
 *
 * Popping back to the placeholder first is what keeps the pane's back stack two
 * entries deep at most, so Back always means "close this record" rather than
 * walking backwards through every owner that has been glanced at.
 */
private fun NavHostController.showOwnerInPane(route: String) {
    navigate(route) {
        popUpTo(OWNER_NOTHING_CHOSEN)
        launchSingleTop = true
    }
}

/** The pane before a choice has been made: says what the empty half is for. */
@Composable
private fun OwnerNothingChosen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 40.dp)
            ) {
                Icon(
                    Icons.Outlined.People,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Text(
                    stringResource(R.string.pane_nothing_chosen_owner_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(R.string.pane_nothing_chosen_owner_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
