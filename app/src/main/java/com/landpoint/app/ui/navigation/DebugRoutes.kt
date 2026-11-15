package com.landpoint.app.ui.navigation

import com.landpoint.app.BuildConfig

/**
 * Destinations that exist only in debug builds (docs/IMPLEMENTATION_PLAN.md,
 * Phase 2 — "Component gallery screen (debug builds only)").
 *
 * The component gallery is developer tooling: it must never be reachable from
 * a release APK. Both the route registration in LandPointNavHost and the
 * Settings entry point gate on this object, so a release build gets an empty
 * list — no destination in the navigation graph, no menu row pointing at one,
 * and nothing in the release UI that can navigate to it even by deep link.
 */
object DebugRoutes {

    /**
     * The routes a build of the given type should register. Pure so the
     * gating decision itself is unit-testable; [active] is what production
     * code consumes.
     */
    fun routes(debugBuild: Boolean): List<String> = if (debugBuild) listOf(Routes.GALLERY) else emptyList()

    /** The routes to register in the build being run. */
    val active: List<String> by lazy { routes(BuildConfig.DEBUG) }
}
