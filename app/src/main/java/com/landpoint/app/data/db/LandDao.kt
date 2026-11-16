package com.landpoint.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.landpoint.app.data.model.AuditEntity
import com.landpoint.app.data.model.LandEntity
import com.landpoint.app.data.model.LandWithPhotos
import com.landpoint.app.data.model.OwnerEntity
import com.landpoint.app.data.model.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LandDao {

    /** Live parcels only — the bin's rows must not resurface in the list. */
    @Transaction
    @Query("SELECT * FROM lands WHERE deleted_at IS NULL ORDER BY created_at DESC")
    fun observeAll(): Flow<List<LandWithPhotos>>

    @Transaction
    @Query("SELECT * FROM lands WHERE id = :id AND deleted_at IS NULL")
    fun observeById(id: String): Flow<LandWithPhotos?>

    /**
     * Live rows only, for the same reason the flows are filtered: a binned
     * parcel has left the app as far as its user is concerned, so the detail
     * reads and the save-time existence check must not see it either.
     * [getDeletedById] is the bin-side counterpart.
     */
    @Transaction
    @Query("SELECT * FROM lands WHERE id = :id AND deleted_at IS NULL")
    suspend fun getById(id: String): LandWithPhotos?

    /**
     * Live rows only: backups, exports, reports and the settings statistics all
     * read through this, and none of them should write a parcel the user has
     * already deleted back into a file.
     */
    @Transaction
    @Query("SELECT * FROM lands WHERE deleted_at IS NULL")
    suspend fun getAllOnce(): List<LandWithPhotos>

    @Query("SELECT COUNT(*) FROM lands WHERE deleted_at IS NULL")
    fun observeCount(): Flow<Int>

    /** Used by import to detect duplicates that carry the same stable id. */
    @Query("SELECT id FROM lands WHERE id IN (:ids) AND deleted_at IS NULL")
    suspend fun findExistingIds(ids: List<String>): List<String>

    /**
     * Coordinate-based duplicate detection for imports of files that have no
     * stable id (e.g. hand-made CSV). Compares to ~1e-5 deg (about 1 m).
     */
    @Query(
        """
        SELECT * FROM lands
        WHERE deleted_at IS NULL
            AND ABS(latitude - :lat) < 0.00001 AND ABS(longitude - :lon) < 0.00001
        LIMIT 1
        """
    )
    suspend fun findByCoordinates(lat: Double, lon: Double): LandEntity?

    @Query(
        "SELECT * FROM lands WHERE deleted_at IS NULL " +
            "AND name = :name AND ABS(latitude - :lat) < 0.00001 AND ABS(longitude - :lon) < 0.00001 LIMIT 1"
    )
    suspend fun findByNameAndCoordinates(name: String, lat: Double, lon: Double): LandEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(land: LandEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(land: LandEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(lands: List<LandEntity>)

    @Update
    suspend fun update(land: LandEntity)

    @Delete
    suspend fun delete(land: LandEntity)

    @Query("DELETE FROM lands WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM lands WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM lands")
    suspend fun deleteAll()

    // ------------------------------------------------------------------
    // Recently deleted (the 30-day bin)
    // ------------------------------------------------------------------

    /** Binned parcels, the most recently deleted first. */
    @Query("SELECT * FROM lands WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    fun recentlyDeleted(): Flow<List<LandEntity>>

    /** The binned parcel with this id, or null if it is live or absent. */
    @Query("SELECT * FROM lands WHERE id = :id AND deleted_at IS NOT NULL LIMIT 1")
    suspend fun getDeletedById(id: String): LandEntity?

    /** Moves the parcel into the bin without touching anything else on it. */
    @Query("UPDATE lands SET deleted_at = :deletedAt WHERE id = :id")
    suspend fun softDeleteById(id: String, deletedAt: Long)

    @Query("UPDATE lands SET deleted_at = :deletedAt WHERE id IN (:ids)")
    suspend fun softDeleteByIds(ids: List<String>, deletedAt: Long)

    /** Bins every live parcel; parcels already in the bin keep their older timestamp. */
    @Query("UPDATE lands SET deleted_at = :deletedAt WHERE deleted_at IS NULL")
    suspend fun softDeleteAll(deletedAt: Long)

    /** Puts a binned parcel back among the live ones, unchanged in every other column. */
    @Query("UPDATE lands SET deleted_at = NULL WHERE id = :id")
    suspend fun restoreById(id: String)

    /**
     * Hard-deletes one parcel and its photo rows in one transaction.
     *
     * The photo FILES are not this method's business — the repository removes
     * them first, because a file deleted after a failed row delete would leave
     * a photo row pointing at nothing.
     */
    @Transaction
    suspend fun purgeById(id: String) {
        deletePhotosFor(id)
        deleteById(id)
    }

    /** The binned parcels old enough to be purged at [cutoff] (epoch millis). */
    @Query("SELECT * FROM lands WHERE deleted_at IS NOT NULL AND deleted_at < :cutoff")
    suspend fun getDeletedOlderThan(cutoff: Long): List<LandEntity>

    @Query(
        "DELETE FROM photos WHERE land_id IN " +
            "(SELECT id FROM lands WHERE deleted_at IS NOT NULL AND deleted_at < :cutoff)"
    )
    suspend fun purgePhotosForExpired(cutoff: Long)

    /** Hard-deletes binned parcels older than [cutoff]; returns how many went. */
    @Query("DELETE FROM lands WHERE deleted_at IS NOT NULL AND deleted_at < :cutoff")
    suspend fun purgeOlderThan(cutoff: Long): Int

    /**
     * The expired purge as one unit: parcel rows and their photo rows leave
     * together, so a crash between the two statements cannot leave photos
     * pointing at parcels the next sweep is about to erase anyway.
     */
    @Transaction
    suspend fun purgeExpiredOlderThan(cutoff: Long): Int {
        purgePhotosForExpired(cutoff)
        return purgeOlderThan(cutoff)
    }

    // ---- photos ----

    @Query("SELECT * FROM photos WHERE land_id = :landId ORDER BY created_at ASC")
    suspend fun getPhotosFor(landId: String): List<PhotoEntity>

    @Query("SELECT * FROM photos")
    suspend fun getAllPhotos(): List<PhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    /**
     * Writes a caption onto a photo that is already stored.
     *
     * Only the one column: re-inserting the row would mean carrying its path and
     * timestamp through the UI just to hand them back unchanged.
     */
    @Query("UPDATE photos SET caption = :caption WHERE id = :id")
    suspend fun updatePhotoCaption(id: String, caption: String)

    @Query("UPDATE photos SET corner_id = :cornerId WHERE id = :id")
    suspend fun updatePhotoCorner(id: String, cornerId: String?)

    @Query("SELECT * FROM photos WHERE land_id = :landId AND corner_id = :cornerId ORDER BY created_at ASC")
    suspend fun getPhotosForCorner(landId: String, cornerId: String): List<PhotoEntity>

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deletePhotoById(id: String)

    @Query("DELETE FROM photos WHERE land_id = :landId")
    suspend fun deletePhotosFor(landId: String)

    // ------------------------------------------------------------------
    // Owners
    // ------------------------------------------------------------------

    @Query("SELECT * FROM owners ORDER BY full_name COLLATE NOCASE ASC")
    fun observeOwners(): Flow<List<OwnerEntity>>

    @Query("SELECT * FROM owners ORDER BY full_name COLLATE NOCASE ASC")
    suspend fun getAllOwners(): List<OwnerEntity>

    @Query("SELECT * FROM owners WHERE id = :id")
    suspend fun getOwnerById(id: String): OwnerEntity?

    @Query("SELECT * FROM owners WHERE nik = :nik LIMIT 1")
    suspend fun findOwnerByNik(nik: String): OwnerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOwner(owner: OwnerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOwners(owners: List<OwnerEntity>)

    @Delete
    suspend fun deleteOwner(owner: OwnerEntity)

    @Query("SELECT COUNT(*) FROM owners")
    fun observeOwnerCount(): Flow<Int>

/**
     * Un-assigns every parcel of an owner about to be deleted. There is no
     * database-level foreign key (SQLite cannot add one to an existing table),
     * so this is the whole of the rule: the parcels stay, the link goes.
     */
    @Query("UPDATE lands SET owner_id = NULL, updated_at = :now WHERE owner_id = :ownerId")
    suspend fun detachLandsFromOwner(ownerId: String, now: Long)

    @Query("SELECT COUNT(*) FROM lands WHERE owner_id = :ownerId")
    suspend fun countLandsForOwner(ownerId: String): Int

    @Query("SELECT owner_id AS ownerId, COUNT(*) AS n FROM lands WHERE owner_id IS NOT NULL GROUP BY owner_id")
    suspend fun countLandsByOwner(): List<OwnerParcelCount>

/** Owner id -> parcel count, for the owner list. */
    data class OwnerParcelCount(val ownerId: String, val n: Int)

// ------------------------------------------------------------------
// Audit log
// ------------------------------------------------------------------

    @Insert
    suspend fun insertAudit(entry: AuditEntity)

    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT :limit")
    fun observeAudit(limit: Int = 200): Flow<List<AuditEntity>>

    @Query("SELECT * FROM audit_log WHERE entity_type = :type AND entity_id = :id ORDER BY timestamp DESC LIMIT :limit")
    fun observeAuditFor(type: String, id: String, limit: Int = 100): Flow<List<AuditEntity>>

    @Query("DELETE FROM audit_log")
    suspend fun clearAudit()
}
