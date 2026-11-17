package com.landpoint.app.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.landpoint.app.R
import com.landpoint.app.ui.components.LocationPermissionState
import com.landpoint.app.ui.components.Stepper
import com.landpoint.app.ui.components.openAppSettings
import com.landpoint.app.ui.components.rememberLocationPermission
import com.landpoint.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * The introduction's stops, in the order they are visited.
 *
 * The first three are the numbered steps the [Stepper] shows; [LOCATION] comes
 * after them and is deliberately not a fourth numbered step — the three steps
 * are things to read, the permission is a thing to decide, and the chrome says
 * so by showing every step done by the time the ask is on screen.
 */
enum class OnboardingStep {
    WELCOME,
    MEASURE,
    PRIVATE,
    LOCATION
}

/**
 * The next stop. At [OnboardingStep.LOCATION] it stays there rather than
 * wrapping: leaving the introduction is [OnboardingScreen]'s explicit buttons
 * (Start, or Not now), never an overflow of Next past the end.
 */
fun OnboardingStep.next(): OnboardingStep =
    OnboardingStep.entries[(ordinal + 1).coerceAtMost(OnboardingStep.LOCATION.ordinal)]

/**
 * The previous stop. At the first stop it stays there; the screen hides its
 * Back button on that page instead of relying on this to loop.
 */
fun OnboardingStep.back(): OnboardingStep =
    OnboardingStep.entries[(ordinal - 1).coerceAtLeast(OnboardingStep.WELCOME.ordinal)]

/** One of the three read-through steps: its illustration and its copy. */
private data class Intro(
    @DrawableRes val illustration: Int,
    @StringRes val title: Int,
    @StringRes val body: Int,
    val step: OnboardingStep
)

/**
 * Three things a first-time user cannot work out from the screens themselves:
 * what the app is for, that a boundary can be measured four different ways, and —
 * the one that costs money when it is missed — that there is no copy of these
 * records anywhere else.
 */
private val intros = listOf(
    Intro(
        R.drawable.ill_onboarding_welcome,
        R.string.onboarding_welcome_title,
        R.string.onboarding_welcome_body,
        OnboardingStep.WELCOME
    ),
    Intro(
        R.drawable.ill_onboarding_measure,
        R.string.onboarding_measure_title,
        R.string.onboarding_measure_body,
        OnboardingStep.MEASURE
    ),
    Intro(
        R.drawable.ill_onboarding_private,
        R.string.onboarding_private_title,
        R.string.onboarding_private_body,
        OnboardingStep.PRIVATE
    )
)

/**
 * The first run: what LandPoint does, then the location request with its reason
 * given first.
 *
 * The reason comes before the system dialog because that dialog says only
 * "Allow LandPoint to access this device's location?", and someone who has just
 * installed a land-records app has no way to know whether refusing it leaves them
 * with a working app. It does — coordinates can be typed and tapped — and the
 * last page says so, so the choice is a real one rather than a guess.
 *
 * The chrome is the redesign's (docs/IMPLEMENTATION_PLAN.md, Phase 4): a visible
 * stepper for the three read-through steps, every page skippable, and the
 * permission ask as its own stop with the refusal path spelled out. A refusal —
 * or a permission Android will no longer ask about — never loops: the page's
 * actions change with the state, and "Not now" always leads out of the
 * introduction to an app that works without the permission.
 *
 * [onFinish] is called once, whether the permission was granted, refused or left
 * alone. Nothing here is a gate.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val permission = rememberLocationPermission()
    val pager = rememberPagerState(pageCount = { intros.size + 1 })
    val scope = rememberCoroutineScope()
    val step = OnboardingStep.entries[pager.currentPage]

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Skippable from the first page. Someone reinstalling the app after a
            // new phone has read all this before and wants their records back.
            // The permission page hides it: "Not now" below plays the same part
            // there, and two doors out of one page read as uncertainty.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm),
                horizontalArrangement = Arrangement.End
            ) {
                if (step != OnboardingStep.LOCATION) {
                    TextButton(onClick = onFinish) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                }
            }

            // The visible three-step line. On the permission page every step
            // shows done — the reading is finished; what remains is the ask.
            Stepper(
                steps = listOf(
                    stringResource(R.string.onboarding_step_welcome),
                    stringResource(R.string.onboarding_step_measure),
                    stringResource(R.string.onboarding_step_private)
                ),
                currentStep = pager.currentPage,
                modifier = Modifier.padding(horizontal = Spacing.xl)
            )

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f)
            ) { page ->
                if (page < intros.size) {
                    IntroPage(intros[page])
                } else {
                    LocationPage(permission)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                if (step != OnboardingStep.WELCOME) {
                    TextButton(
                        onClick = {
                            scope.launch { pager.animateScrollToPage(step.back().ordinal) }
                        }
                    ) {
                        Text(stringResource(R.string.action_back))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                when (step) {
                    OnboardingStep.LOCATION -> LocationActions(permission, onFinish)
                    else -> Button(
                        onClick = {
                            scope.launch { pager.animateScrollToPage(step.next().ordinal) }
                        }
                    ) {
                        Text(stringResource(R.string.onboarding_next))
                    }
                }
            }
        }
    }
}

@Composable
private fun IntroPage(intro: Intro) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Monochrome per the design system (§7): the whole drawing takes one
        // tint, so it reads in both themes without a dark variant.
        Icon(
            painterResource(intro.illustration),
            contentDescription = null,
            modifier = Modifier.size(180.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            stringResource(intro.title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(intro.body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The ask, with the consequence of refusing stated rather than implied.
 *
 * Only the explanation lives here — the actions are in the bottom bar, where
 * every other page of the introduction keeps them.
 */
@Composable
private fun LocationPage(permission: LocationPermissionState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.GpsFixed,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            stringResource(R.string.onboarding_location_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(R.string.onboarding_location_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // The one line the current state needs: what was granted, why the
        // system will not ask again, or what refusing now still leaves open.
        when {
            permission.isGranted -> Text(
                stringResource(R.string.onboarding_location_granted),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            // The system will not put the dialog up again, so offering the ask
            // would be a button that does nothing.
            permission.isBlocked -> Text(
                stringResource(R.string.permission_blocked_body),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            else -> Text(
                stringResource(R.string.onboarding_location_later),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The permission stop's actions, one pair per state.
 *
 * Granted: nothing left to decide. Askable: grant here, or "Not now" — which
 * leaves the introduction, because the app genuinely works without the
 * permission and re-asking someone who just refused teaches them to ignore the
 * dialog. Permanently refused: the system settings page, the only door Android
 * still leaves open. Each state offers its way out, so no answer can strand a
 * user on this page.
 */
@Composable
private fun LocationActions(permission: LocationPermissionState, onFinish: () -> Unit) {
    val context = LocalContext.current
    when {
        permission.isGranted -> Button(onClick = onFinish) {
            Text(stringResource(R.string.onboarding_start))
        }

        else -> {
            TextButton(onClick = onFinish) {
                Text(stringResource(R.string.onboarding_not_now))
            }
            Button(
                onClick = {
                    if (permission.isBlocked) openAppSettings(context) else permission.request()
                }
            ) {
                Text(
                    stringResource(
                        if (permission.isBlocked) {
                            R.string.permission_open_settings
                        } else {
                            R.string.permission_grant
                        }
                    )
                )
            }
        }
    }
}
