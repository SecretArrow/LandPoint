package com.landpoint.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.landpoint.app.data.SettingsRepository
import com.landpoint.app.ui.container
import com.landpoint.app.ui.theme.Elevation
import com.landpoint.app.ui.theme.Spacing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * One first-time-use explanation of a control the screen itself does not name
 * (docs/DESIGN_SYSTEM.md §6.4): the corner picker's ways of marking a corner,
 * the map's floating controls, the parcel preview's quick actions.
 *
 * Every screen holds its own instance; they all read the same DataStore keys,
 * the same deal [MapPrefsViewModel] offers — so a mark dismissed in the corner
 * picker never returns on the map tab either.
 */
class CoachMarkViewModel(private val settings: SettingsRepository) : ViewModel() {

    /**
     * Whether [mark] has been dismissed. Reads the stored flag; the screen shows
     * the overlay while this is false.
     */
    fun seen(mark: SettingsRepository.CoachMark): Flow<Boolean> = settings.coachMarkSeen(mark)

    /**
     * Dismisses [mark] for good. Both of the overlay's actions end here: a mark
     * that returns after being closed is a nag, not a coach. The separate
     * "don't show again" label exists so the choice is a named control a screen
     * reader can find, not a tap nobody can discover.
     */
    fun dismiss(mark: SettingsRepository.CoachMark) {
        viewModelScope.launch { settings.setCoachMarkSeen(mark) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                CoachMarkViewModel(container().settings)
            }
        }
    }
}

/**
 * The visibility and dismissal of one coach mark, for one screen.
 *
 * [visible] defaults to false while the stored flag is still in flight — a mark
 * must not flash open for a frame at every start before its record arrives,
 * which is what defaulting to "not seen" would do.
 */
class CoachMarkHandle(val visible: Boolean, val dismiss: () -> Unit)

/** Reads [mark]'s record and hands the screen a [CoachMarkHandle] for it. */
@Composable
fun rememberCoachMark(mark: SettingsRepository.CoachMark): CoachMarkHandle {
    val viewModel: CoachMarkViewModel = viewModel(factory = CoachMarkViewModel.Factory)
    val seen by viewModel.seen(mark).collectAsStateWithLifecycle(initialValue = true)
    return CoachMarkHandle(visible = !seen, dismiss = { viewModel.dismiss(mark) })
}

/**
 * The mark itself: a floating card that explains a nearby control and offers
 * two ways out.
 *
 * Deliberately non-blocking — no scrim, nothing behind it is disabled — because
 * the control it explains stays usable underneath and the e2e flows must never
 * find a modal in their way. Position it against the control it describes; a
 * bottom-of-screen card is the honest fallback where exact placement is not
 * practical.
 *
 * [onDismiss] is called by either action. Callers persist the dismissal there
 * ([rememberCoachMark]'s handle does), which is what makes this first-time-use:
 * shown once, then never again.
 */
@Composable
fun CoachMarkOverlay(
    title: String,
    body: String,
    dismissLabel: String,
    dontShowAgainLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        // The floating toolbar's step of the elevation scale: lifted off whatever
        // is underneath, in the same way the map's controls are.
        tonalElevation = Elevation.E3.tonal,
        shadowElevation = Elevation.E3.tonal,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(dontShowAgainLabel)
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = onDismiss) {
                    Text(dismissLabel)
                }
            }
        }
    }
}
