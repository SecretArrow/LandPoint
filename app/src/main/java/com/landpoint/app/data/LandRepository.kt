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

    /** Deletes the land row plus every photo file it owned. */
    suspend fun delete(id: String) {
        val row = dao.getById(id)
        dao.getPhotosFor(id).forEach { photoStore.delete(it.filePath) }
        dao.deleteById(id)
        audit(
            AuditEntities.LAND,
            id,
            AuditActions.DELETE,
            row?.land?.name ?: id
        )
    }

    suspend fun deleteMany(ids: List<String>) {
        ids.forEach { id ->
            dao.getPhotosFor(id).forEach { photoStore.delete(it.filePath) }
            audit(AuditEntities.LAND, id, AuditActions.DELETE, id)
        }
        dao.deleteByIds(ids)
    }

    suspend fun deleteAll() {
        dao.getAllPhotos().forEach { photoStore.delete(it.filePath) }
        dao.deleteAll()
        dao.clearAudit()
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
