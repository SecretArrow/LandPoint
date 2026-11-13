package com.landpoint.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.landpoint.app.data.model.AuditEntity
import com.landpoint.app.data.model.LandEntity
import com.landpoint.app.data.model.OwnerEntity
import com.landpoint.app.data.model.PhotoEntity

/**
 * 1 → 2: photos gain a nullable `corner_id` binding a photo to a stable
 * [CornerPoint.id]. Old photos keep NULL (= general land photo).
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE photos ADD COLUMN corner_id TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_photos_corner_id ON photos(corner_id)")
    }
}

/**
 * 2 → 3: land ownership and registry bookkeeping.
 *
 * - the `owners` table (who holds which parcel),
 * - the `audit_log` table (what changed and when),
 * - six new columns on `lands`: owner link, land right, status with a default,
 *   certificate number, SPPT number and acquisition date.
 *
 * Every statement is written to match the schema Room derives from the
 * entities, so validation passes on the first migration and not after a
 * repair round trip.
 */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `owners` (
                `id` TEXT NOT NULL,
                `full_name` TEXT NOT NULL,
                `nik` TEXT,
                `address` TEXT NOT NULL,
                `phone` TEXT,
                `email` TEXT,
                `photo_path` TEXT,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_owners_created_at` ON `owners` (`created_at`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `audit_log` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `entity_type` TEXT NOT NULL,
                `entity_id` TEXT NOT NULL,
                `action` TEXT NOT NULL,
                `detail` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_audit_log_entity_type_entity_id` " +
                "ON `audit_log` (`entity_type`, `entity_id`)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_log_timestamp` ON `audit_log` (`timestamp`)")

        db.execSQL("ALTER TABLE lands ADD COLUMN owner_id TEXT")
        db.execSQL("ALTER TABLE lands ADD COLUMN land_right TEXT")
        db.execSQL("ALTER TABLE lands ADD COLUMN status TEXT NOT NULL DEFAULT 'aktif'")
        db.execSQL("ALTER TABLE lands ADD COLUMN certificate_number TEXT")
        db.execSQL("ALTER TABLE lands ADD COLUMN sppt_number TEXT")
        db.execSQL("ALTER TABLE lands ADD COLUMN acquisition_date INTEGER")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_lands_owner_id` ON `lands` (`owner_id`)")
    }
}

@Database(
    entities = [LandEntity::class, PhotoEntity::class, OwnerEntity::class, AuditEntity::class],
    version = 3,
    exportSchema = true
)
abstract class LandDatabase : RoomDatabase() {

    abstract fun landDao(): LandDao

    companion object {
        private const val NAME = "landpoint.db"

        @Volatile
        private var instance: LandDatabase? = null

        fun get(context: Context): LandDatabase = instance ?: synchronized(this) {
            instance ?: build(context).also { instance = it }
        }

        private fun build(context: Context): LandDatabase {
            val app = context.applicationContext
            return Room.databaseBuilder(app, LandDatabase::class.java, NAME)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .apply {
                    // Null on a phone that cannot manage encryption at all, which
                    // leaves Room on its own opener rather than leaving the user
                    // unable to reach their own records. Settings reports which of
                    // the two happened.
                    DatabaseCipher.factoryFor(app, NAME)?.let(::openHelperFactory)
                }
                .build()
        }
    }
}
