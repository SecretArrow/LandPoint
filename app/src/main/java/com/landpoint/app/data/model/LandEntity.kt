package com.landpoint.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Land location entity.
 *
 * Future-proof schema:
 * - geometryType + geometryJson allow polygon boundaries (roadmap item)
 * - parcelNumber for official land parcel IDs (roadmap item)
 * - areaSqm for calculated area when polygon is drawn (roadmap item)
 *
 * MVP stores single point (geometryType="Point", geometryJson=null).
 */
@Entity(
    tableName = "lands",
    indices = [Index(value = ["created_at"]), Index(value = ["owner_id"])]
)
data class LandEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "altitude")
    val altitude: Double? = null,

    @ColumnInfo(name = "accuracy")
    val accuracy: Float? = null,

    @ColumnInfo(name = "address")
    val address: String? = null,

    @ColumnInfo(name = "geometry_type")
    val geometryType: String = "Point",

    @ColumnInfo(name = "geometry_json")
    val geometryJson: String? = null,

    @ColumnInfo(name = "parcel_number")
    val parcelNumber: String? = null,

    @ColumnInfo(name = "area_sqm")
    val areaSqm: Double? = null,

    /**
     * Owner of the parcel; null when unassigned. No database-level foreign key: SQLite
     * cannot add one to an existing table, so LandRepository enforces the rule that
     * deleting an owner un-assigns the land rather than deleting it.
     */
    @ColumnInfo(name = "owner_id")
    val ownerId: String? = null,

    /** Land right as free text: a preset ("Hak Milik") or anything the user typed. */
    @ColumnInfo(name = "land_right")
    val landRight: String? = null,

    /** Registry status of the parcel; see [com.landpoint.app.data.model.LandStatus]. */
    @ColumnInfo(name = "status", defaultValue = "aktif")
    val status: String = "aktif",

    /** Certificate number of the registered right, when there is one. */
    @ColumnInfo(name = "certificate_number")
    val certificateNumber: String? = null,

    /** Tax-assessment (SPPT) number, when there is one. */
    @ColumnInfo(name = "sppt_number")
    val spptNumber: String? = null,

    /** When the parcel was acquired, in epoch millis; null when unknown. */
    @ColumnInfo(name = "acquisition_date")
    val acquisitionDate: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
