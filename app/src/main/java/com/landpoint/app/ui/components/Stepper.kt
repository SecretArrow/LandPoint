package com.landpoint.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.landpoint.app.ui.theme.Spacing

/**
 * Where one step of a [Stepper] stands relative to the flow's position.
 *
 * A pure mapping rather than logic buried in the composable, so the contract a
 * flow relies on — everything before the current step is done, the current step
 * is active, everything after it is still ahead — is pinned by a test.
 */
enum class StepPosition {

    /** The step has been passed; it shows a check rather than its number. */
    DONE,

    /** The step on screen right now. */
    ACTIVE,

    /** A step still ahead of the user. */
    UPCOMING
}

/**
 * Maps a step's index and the flow's position onto its visual state.
 *
 * [currentStep] past the end — a flow whose last page is not itself a numbered
 * step, as onboarding's permission ask is not — means every numbered step is
 * done, which is what a reader of that last page should be told.
 */
fun stepPosition(index: Int, currentStep: Int, stepCount: Int): StepPosition = when {
    index < currentStep -> StepPosition.DONE
    index == currentStep -> StepPosition.ACTIVE
    else -> StepPosition.UPCOMING
}

/**
 * The numbered progress line for a flow that moves a step at a time
 * (docs/DESIGN_SYSTEM.md §6.4).
 *
 * Labels sit under the circles because the words carry the meaning — three
 * unlabelled dots are a pager indicator, not a map of where someone is. Each
 * step takes an equal share of the width, so the line reads the same on a
 * narrow phone and a wide one.
 *
 * @param steps the labels, in the order the flow visits them.
 * @param currentStep zero-based index of the step on screen; a value of
 *   [steps.size] or more marks every step done, per [stepPosition].
 */
@Composable
fun Stepper(steps: List<String>, currentStep: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        steps.forEachIndexed { index, label ->
            StepItem(
                number = index + 1,
                label = label,
                position = stepPosition(index, currentStep, steps.size),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StepItem(number: Int, label: String, position: StepPosition, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Box(
            modifier = Modifier.size(26.dp).background(
                when (position) {
                    StepPosition.DONE -> colors.primaryContainer
                    StepPosition.ACTIVE -> colors.primary
                    StepPosition.UPCOMING -> colors.surfaceVariant
                },
                CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            when (position) {
                StepPosition.DONE -> Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.onPrimaryContainer
                )

                else -> Text(
                    number.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (position == StepPosition.ACTIVE) colors.onPrimary else colors.onSurfaceVariant
                )
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = when (position) {
                StepPosition.UPCOMING -> colors.onSurfaceVariant
                else -> colors.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.xs)
        )
    }
}
