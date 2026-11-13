package com.landpoint.app.data.model

import androidx.annotation.StringRes
import com.landpoint.app.R

/**
 * Legal status of a parcel, following the categories the land registry uses.
 * Kept as an enum rather than free text so filtering stays exact; anything the
 * presets do not cover is expressed through [LandRight], which IS free text.
 */
enum class LandStatus(val key: String, @StringRes val labelRes: Int) {
    ACTIVE("aktif", R.string.land_status_active),
    DISPUTED("sengketa", R.string.land_status_disputed),
    SOLD("terjual", R.string.land_status_sold),
    TRANSFERRED("dialihkan", R.string.land_status_transferred);

    companion object {
        fun fromKey(key: String?): LandStatus = entries.firstOrNull { it.key == key } ?: ACTIVE
    }
}

/**
 * The land-right presets offered in the editor. The stored value is free text:
 * a right not in this list ("Hak Milik Adat", a lease, a stewardship) is as
 * valid as any preset, so the enum exists only to seed the dropdown.
 */
object LandRights {
    /** Preset keys, shown localized in the UI; anything else is custom text. */
    val PRESETS = listOf(
        "Hak Milik",
        "Hak Guna Usaha",
        "Hak Guna Bangunan",
        "Hak Pakai"
    )

    /** True when [value] is one of the presets or a sensible custom string. */
    fun isCustom(value: String): Boolean = value.isNotBlank() && value !in PRESETS
}
