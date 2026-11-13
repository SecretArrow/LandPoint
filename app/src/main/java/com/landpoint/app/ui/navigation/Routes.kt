package com.landpoint.app.ui.navigation

object Routes {
    // The five tabs, in the order they appear in the bar.
    const val HOME = "home"
    const val MAP = "map"
    const val LIST = "lands"
    const val OWNERS = "owners"
    const val REPORTS = "reports"

    // Full-screen destinations reached from a tab.
    const val SETTINGS = "settings"
    const val OFFLINE = "offline_maps"

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
