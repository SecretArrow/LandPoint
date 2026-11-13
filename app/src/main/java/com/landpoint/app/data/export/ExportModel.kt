package com.landpoint.app.data.export

import com.landpoint.app.data.model.GeometryType
import kotlinx.serialization.Serializable

/**
 * Stable JSON schema for backup/restore. uuid is the stable identifier for
 * duplicate detection across imports. Once exported, a land's uuid never changes.
 *
 * Every field added after version 1 carries a default, and the reader is
 * configured with `ignoreUnknownKeys`, so a file written by any version of the
 * app can still be read by any other.
 */
@Serializable
data class LandExportJson(
    val uuid: String,
    val name: String,
    val description: String = "",
    val notes: String = "",
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val address: String? = null,
    val parcelNumber: String? = null,
    val areaSqm: Double? = null,
    /**
     * Added in version 2. Without these two a boundary drawn by the user was
     * silently flattened to its single centre point on export — the mapped shape
     * was lost and could not be got back.
     */
    val geometryType: String = GeometryType.POINT,
    val geometryJson: String? = null,
    /** Added in version 2; only ever populated inside a .zip archive. */
    val photos: List<PhotoExportJson> = emptyList(),

    /** Added in version 3: ownership and registry bookkeeping. */
    val ownerId: String? = null,
    /** The owner's name as it was at export time; informational, never re-imported. */
    val ownerName: String? = null,
    val landRight: String? = null,
    val status: String = "aktif",
    val certificateNumber: String? = null,
    val spptNumber: String? = null,
    val acquisitionDate: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * A photo as it appears in a backup archive. [fileName] is the entry inside the
 * archive's `photos/` folder, never a path on the device it came from — the
 * originating phone's directory layout means nothing on the phone restoring it.
 */
@Serializable
data class PhotoExportJson(
    val uuid: String,
    val fileName: String,
    val caption: String = "",
    /** Stable corner id this photo proves; null for a general land photo. */
    val cornerId: String? = null,
    val createdAt: Long
)

/**
 * An owner as she appears in a backup archive. `photoFileName` points at an
 * entry inside the archive's `owners/` folder; the id is stable so lands can
 * reference their owner across a backup-and-restore round trip.
 */
@Serializable
data class OwnerExportJson(
    val uuid: String,
    val fullName: String,
    val nik: String? = null,
    val address: String = "",
    val phone: String? = null,
    val email: String? = null,
    val photoFileName: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class LandPointBackup(
    val version: Int = CURRENT_BACKUP_VERSION,
    val exportedAt: Long,
    val lands: List<LandExportJson>,
    /** Added in version 3; older files simply have none. */
    val owners: List<OwnerExportJson> = emptyList()
)

/** 1 = lands only. 2 = adds boundary geometry and photos. 3 = adds owners. */
const val CURRENT_BACKUP_VERSION = 3
