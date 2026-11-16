package com.landpoint.app.data

import com.landpoint.app.data.db.LandDao
import com.landpoint.app.data.model.AuditActions
import com.landpoint.app.data.model.AuditEntities
import com.landpoint.app.data.model.AuditEntity
import com.landpoint.app.data.model.AuditEntry
import com.landpoint.app.data.model.Land
import com.landpoint.app.data.model.LandEntity
import com.landpoint.app.data.model.Owner
import com.landpoint.app.data.model.Photo
import com.landpoint.app.data.model.PhotoEntity
import com.landpoint.app.data.model.toDomain
import com.landpoint.app.data.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class LandRepository(private val dao: LandDao, private val photoStore: PhotoStore) {

    /** Thirty days in millis — how long a binned parcel stays recoverable. */
    private companion object {
        const val RETENTION_DAYS = 30
        const val DAY_MS = 24L * 60 * 60 * 1000
    }

    // ------------------------------------------------------------------
    // Lands
    // ------------------------------------------------------------------

    fun observeLands(): Flow<List<Land>> = combine(dao.observeAll(), dao.observeOwners()) { rows, owners ->
        val names = owners.associate { it.id to it.fullName }
        rows.map { it.toDomain().copy(ownerName = it.land.ownerId?.let(names::get)) }
    }

    fun observeLand(id: String): Flow<Land?> = combine(dao.observeById(id), dao.observeOwners()) { row, owners ->
        row?.let {
            val name = it.land.ownerId?.let { oid -> owners.firstOrNull { o -> o.id == oid }?.fullName }
            it.toDomain().copy(ownerName = name)
        }
    }

    fun observeCount(): Flow<Int> = dao.observeCount()

    /**
     * The recently-deleted bin: parcels whose [LandEntity.deletedAt] is set,
     * most recently deleted first.
     */
    fun recentlyDeleted(): Flow<List<Land>> = dao.recentlyDeleted().map { rows -> rows.map { it.toDomain() } }

    /** How many parcels are currently in the bin, for the settings entry's count. */
    fun observeRecentlyDeletedCount(): Flow<Int> = dao.observeDeletedCount()

    suspend fun getLand(id: String): Land? = dao.getById(id)?.toDomain()

    suspend fun getAllLands(): List<Land> = dao.getAllOnce().map { it.toDomain() }

    suspend fun save(land: LandEntity) {
        // Upsert semantics: the editor saves new and existing records through
        // the same call, so the audit entry has to ask which of the two it was.
        val existed = dao.getById(land.id) != null
        dao.upsert(land)
        audit(
            AuditEntities.LAND,
            land.id,
            if (existed) AuditActions.UPDATE else AuditActions.CREATE,
            land.name
        )
    }

    suspend fun update(land: LandEntity) {
        dao.update(land.copy(updatedAt = System.currentTimeMillis()))
        audit(
            AuditEntities.LAND,
            land.id,
            AuditActions.UPDATE,
            land.name
        )
    }

    /**
     * Moves the parcel to the recently-deleted bin instead of erasing it.
     *
     * Only `deleted_at` is written: the row, its boundary and its photo files
     * all stay exactly as they were, so [restore] is a one-column update and a
     * mistaken delete costs nothing. The photo files are deliberately NOT
     * removed here — they must survive the restore — and go only when a purge
     * decides the parcel is really gone.
     */
    suspend fun delete(id: String) {
        val row = dao.getById(id)
        dao.softDeleteById(id, System.currentTimeMillis())
        audit(
            AuditEntities.LAND,
            id,
            AuditActions.DELETE,
            row?.land?.name ?: id
        )
    }

    /**
     * Soft-deletes a batch, the bulk path behind the list screen's undo window.
     *
     * One timestamp for the whole batch: they were deleted together, so they
     * age out together. The ids are audited individually to keep the trail
     * readable per parcel.
     */
    suspend fun deleteMany(ids: List<String>) {
        if (ids.isEmpty()) return
        val deletedAt = System.currentTimeMillis()
        dao.softDeleteByIds(ids, deletedAt)
        ids.forEach { id -> audit(AuditEntities.LAND, id, AuditActions.DELETE, id) }
    }

    /**
     * Bins every live parcel instead of erasing them.
     *
     * Deliberately changed from a hard wipe: "clear all" is the most destructive
     * tap in the app, and a mistake there deserves the same thirty-day window
     * every other delete gets. The photo files and the audit log are untouched —
     * the rows still exist, so their photos must, and history that can be
     * undone should not be scrubbed. Only a purge empties the bin for good.
     */
    suspend fun deleteAll() {
        dao.softDeleteAll(System.currentTimeMillis())
    }

    /**
     * Puts a binned parcel back among the live ones.
     *
     * Nothing else has to happen: the row was never altered and the photo files
     * were never touched, so clearing `deleted_at` is the whole restore.
     */
    suspend fun restore(id: String) {
        val row = dao.getDeletedById(id)
        dao.restoreById(id)
        audit(
            AuditEntities.LAND,
            id,
            AuditActions.RESTORE,
            row?.name ?: id
        )
    }

    /**
     * Finishes what a delete started: removes the parcel, its photo rows and
     * their files for good. This is the only path that deletes photo files.
     *
     * Files go before rows, as [delete] used to — a file deleted after a failed
     * row delete would leave a photo pointing at nothing, and the reverse
     * failure (rows gone, files left) merely orphans bytes, not records.
     */
    suspend fun purge(id: String) {
        val row = dao.getDeletedById(id) ?: dao.getById(id)?.land
        dao.getPhotosFor(id).forEach { photoStore.delete(it.filePath) }
        dao.purgeById(id)
        audit(
            AuditEntities.LAND,
            id,
            AuditActions.PURGE,
            row?.name ?: id
        )
    }

    /**
     * Hard-deletes every binned parcel older than the retention window and
     * their photo files, returning how many parcels went.
     *
     * Deterministic and idempotent: the cutoff is `now - retentionDays`, and a
     * parcel is expired when its `deleted_at` is strictly older than that — a
     * parcel deleted exactly [retentionDays] ago is still inside its window,
     * with the last second of grace. Called once at app start and daily by
     * [com.landpoint.app.data.RecentlyDeletedWorker], so the bin empties itself
     * whether or not the app gets opened every day.
     *
     * @param now the clock reading to expire against; injectable so callers and
     *   tests can pin it instead of depending on wall-clock drift.
     * @param retentionDays how long a binned parcel stays recoverable.
     */
    suspend fun purgeExpired(now: Long = System.currentTimeMillis(), retentionDays: Int = RETENTION_DAYS): Int {
        val cutoff = now - retentionDays * DAY_MS
        val expired = dao.getDeletedOlderThan(cutoff)
        if (expired.isEmpty()) return 0
        expired.forEach { row ->
            dao.getPhotosFor(row.id).forEach { photoStore.delete(it.filePath) }
            audit(AuditEntities.LAND, row.id, AuditActions.PURGE, row.name)
        }
        return dao.purgeExpiredOlderThan(cutoff)
    }

    suspend fun addPhoto(landId: String, filePath: String, caption: String = "", cornerId: String? = null) {
        dao.insertPhoto(
            PhotoEntity(landId = landId, filePath = filePath, caption = caption, cornerId = cornerId)
        )
    }

    /**
     * Re-binds photos whose corner no longer exists to general photos instead of
     * deleting them. Called on save after the boundary is known.
     */
    suspend fun detachPhotosFromMissingCorners(landId: String, cornerIds: Set<String>) {
        dao.getPhotosFor(landId)
            .filter { it.cornerId != null && it.cornerId !in cornerIds }
            .forEach { dao.updatePhotoCorner(it.id, null) }
    }

    suspend fun setPhotoCaption(photoId: String, caption: String) {
        dao.updatePhotoCaption(photoId, caption)
    }

    suspend fun setPhotoCorner(photoId: String, cornerId: String?) {
        dao.updatePhotoCorner(photoId, cornerId)
    }

    suspend fun removePhoto(photo: Photo) {
        photoStore.delete(photo.filePath)
        dao.deletePhotoById(photo.id)
    }

    suspend fun removePhotoById(photoId: String, filePath: String) {
        photoStore.delete(filePath)
        dao.deletePhotoById(photoId)
    }

    suspend fun findExistingIds(ids: List<String>): Set<String> = if (ids.isEmpty()) emptySet() else dao.findExistingIds(ids).toSet()

    suspend fun findDuplicateByCoordinates(name: String, lat: Double, lon: Double): LandEntity? =
        dao.findByNameAndCoordinates(name, lat, lon)

    suspend fun upsertAll(lands: List<LandEntity>) = dao.upsertAll(lands)

    /** Every photo path currently referenced by a land, used by restore cleanup. */
    suspend fun allPhotoPaths(): Set<String> = dao.getAllPhotos().map { it.filePath }.toSet()

    // ------------------------------------------------------------------
    // Owners
    // ------------------------------------------------------------------

    fun observeOwners(): Flow<List<Owner>> = combine(dao.observeOwners(), dao.observeAll()) { owners, lands ->
        val counts = lands.groupingBy { it.land.ownerId ?: "" }.eachCount()
        owners.map { it.toDomain(counts[it.id] ?: 0) }
    }

    fun observeOwner(id: String): Flow<Owner?> = combine(dao.observeOwners(), dao.observeAll()) { owners, lands ->
        owners.firstOrNull { it.id == id }?.let { owner ->
            owner.toDomain(lands.count { it.land.ownerId == id })
        }
    }

    fun observeOwnerCount(): Flow<Int> = dao.observeOwnerCount()

    suspend fun getOwner(id: String): Owner? = dao.getOwnerById(id)?.toDomain()

    suspend fun getOwners(): List<Owner> = dao.getAllOwners().map { it.toDomain() }

    /** Finds an owner by NIK, for duplicate prevention at entry time. */
    suspend fun findOwnerByNik(nik: String): Owner? = dao.findOwnerByNik(nik)?.toDomain()

    suspend fun saveOwner(owner: Owner) {
        dao.upsertOwner(owner.toEntity())
        audit(AuditEntities.OWNER, owner.id, AuditActions.CREATE, owner.fullName)
    }

    suspend fun updateOwner(owner: Owner) {
        dao.upsertOwner(owner.toEntity().copy(updatedAt = System.currentTimeMillis()))
        audit(AuditEntities.OWNER, owner.id, AuditActions.UPDATE, owner.fullName)
    }

    /**
     * Deletes the owner and un-assigns their parcels. The parcels themselves,
     * their boundaries and their photos are untouched — a person leaving the
     * book must not take the land with them.
     */
    suspend fun deleteOwner(owner: Owner) {
        dao.detachLandsFromOwner(owner.id, System.currentTimeMillis())
        owner.photoPath?.let { photoStore.delete(it) }
        dao.deleteOwner(owner.toEntity())
        audit(AuditEntities.OWNER, owner.id, AuditActions.DELETE, owner.fullName)
    }

    /** Parcels of one owner, newest first. */
    fun observeLandsOfOwner(ownerId: String): Flow<List<Land>> = observeLands().map { list -> list.filter { it.ownerId == ownerId } }

    /** Every owner-photo path (identity-card photos), used by restore cleanup. */
    suspend fun allOwnerPhotoPaths(): Set<String> = dao.getAllOwners().mapNotNull { it.photoPath }.toSet()

    suspend fun upsertOwners(owners: List<com.landpoint.app.data.model.OwnerEntity>) = dao.upsertOwners(owners)

    // ------------------------------------------------------------------
    // Audit log
    // ------------------------------------------------------------------

    fun observeAudit(limit: Int = 200): Flow<List<AuditEntry>> = dao.observeAudit(limit).map { list -> list.map { it.toDomain() } }

    fun observeAuditFor(entityType: String, entityId: String): Flow<List<AuditEntry>> =
        dao.observeAuditFor(entityType, entityId).map { list -> list.map { it.toDomain() } }

    private suspend fun audit(entityType: String, entityId: String, action: String, name: String) {
        dao.insertAudit(
            AuditEntity(
                entityType = entityType,
                entityId = entityId,
                action = action,
                detail = name
            )
        )
    }
}
