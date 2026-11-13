package com.landpoint.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One line of the local audit trail: *what* changed and *when* — never *who*,
 * because there is no account to answer that question with.
 *
 * [detail] is a short, already-localized sentence recorded at the time of the
 * change ("Boundary redrawn, 6 corners"), so the history stays readable even
 * for records whose label words have since been renamed.
 */
@Entity(
    tableName = "audit_log",
    indices = [Index(value = ["entity_type", "entity_id"]), Index(value = ["timestamp"])]
)
data class AuditEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    /** "land" | "owner" — kept as text so the log survives schema changes. */
    @ColumnInfo(name = "entity_type")
    val entityType: String,

    @ColumnInfo(name = "entity_id")
    val entityId: String,

    /** "create" | "update" | "delete". */
    @ColumnInfo(name = "action")
    val action: String,

    @ColumnInfo(name = "detail")
    val detail: String
)

/** Domain model exposed to the UI layer. */
data class AuditEntry(
    val id: Long,
    val timestamp: Long,
    val entityType: String,
    val entityId: String,
    val action: String,
    val detail: String
)

fun AuditEntity.toDomain(): AuditEntry = AuditEntry(
    id = id,
    timestamp = timestamp,
    entityType = entityType,
    entityId = entityId,
    action = action,
    detail = detail
)

/** Entity-type constants; plain strings so old rows never stop matching. */
object AuditEntities {
    const val LAND = "land"
    const val OWNER = "owner"
}

/** Action constants for the same reason. */
object AuditActions {
    const val CREATE = "create"
    const val UPDATE = "update"
    const val DELETE = "delete"
}
