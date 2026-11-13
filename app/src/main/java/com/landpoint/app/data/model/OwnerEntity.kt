package com.landpoint.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Owner of one or more land parcels. Entirely local: no account, no sync, and
 * the NIK is optional precisely because the app never needs it — it is a
 * memory aid for whoever holds the phone.
 */
@Entity(
    tableName = "owners",
    indices = [Index(value = ["created_at"])]
)
data class OwnerEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "full_name")
    val fullName: String,

    /** 16-digit identity number, stored only when the user chose to type it. */
    @ColumnInfo(name = "nik")
    val nik: String? = null,

    @ColumnInfo(name = "address")
    val address: String = "",

    @ColumnInfo(name = "phone")
    val phone: String? = null,

    @ColumnInfo(name = "email")
    val email: String? = null,

    /** Optional identity-card photo, kept in the app's private image store. */
    @ColumnInfo(name = "photo_path")
    val photoPath: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/** Domain model exposed to the UI layer. */
data class Owner(
    val id: String,
    val fullName: String,
    val nik: String?,
    val address: String,
    val phone: String?,
    val email: String?,
    val photoPath: String?,
    val createdAt: Long,
    val updatedAt: Long,
    /** Filled in by the list layer: how many parcels this owner holds. */
    val parcelCount: Int = 0
)

fun OwnerEntity.toDomain(parcelCount: Int = 0): Owner = Owner(
    id = id,
    fullName = fullName,
    nik = nik,
    address = address,
    phone = phone,
    email = email,
    photoPath = photoPath,
    createdAt = createdAt,
    updatedAt = updatedAt,
    parcelCount = parcelCount
)

fun Owner.toEntity(): OwnerEntity = OwnerEntity(
    id = id,
    fullName = fullName,
    nik = nik,
    address = address,
    phone = phone,
    email = email,
    photoPath = photoPath,
    createdAt = createdAt,
    updatedAt = updatedAt
)
