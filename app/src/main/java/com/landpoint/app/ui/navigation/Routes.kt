package com.landpoint.app.ui.navigation

object Routes {
    // The four tabs, in the order they appear in the bar (ADR-002). The list
    // route keeps its "lands" path from the five-tab days — a route string is
    // persisted state, and renaming it would strand a restoring back stack —
    // only the tab's label moved, to nav_parcels.
    const val MAP = "map"
    const val LIST = "lands"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"

    // Former tabs, demoted by ADR-002 to full-screen destinations. Owners is
    // reached from the Tools grid and the Parcels header's owners stat;
    // Reports is the Tools grid's Export and Reports cards' destination.
    const val OWNERS = "owners"
    const val REPORTS = "reports"

    // Full-screen destinations reached from a tab or a card.
    const val OFFLINE = "offline_maps"

    // Reached from settings: the recently-deleted bin, where a delete rests
    // for thirty days before a purge erases it for good.
    const val RECENTLY_DELETED = "recently_deleted"

    // Reached from settings: the help centre, whose pages are bundled markdown
    // assets rendered in-app — no network, like everything else it explains.
    const val HELP = "help"

    // Debug-only developer tooling (see DebugRoutes): the component gallery.
    // No release-visible UI navigates to it, and the destination is only
    // registered when BuildConfig.DEBUG — a release APK carries the string
    // and nothing else.
    const val GALLERY = "component_gallery"

    const val DETAIL = "land/{landId}"
    const val EDIT = "land/{landId}/edit"

    // Deliberately not "land/new" — that would also match the DETAIL pattern.
    const val NEW = "land_new"
    const val PREVIEW = "land_preview"
    const val COMPASS = "land/{landId}/compass"
    const val SHAPE = "land/{landId}/shape"

    const val OWNER_DETAIL = "owner/{ownerId}"
    const val OWNER_NEW = "owner_new"
    const val OWNER_EDIT = "owner/{ownerId}/edit"

    fun detail(landId: String) = "land/$landId"
    fun edit(landId: String) = "land/$landId/edit"
    fun compass(landId: String) = "land/$landId/compass"
    fun shape(landId: String) = "land/$landId/shape"
    fun ownerDetail(ownerId: String) = "owner/$ownerId"
    fun ownerEdit(ownerId: String) = "owner/$ownerId/edit"

    const val ARG_LAND_ID = "landId"
    const val ARG_OWNER_ID = "ownerId"
}
