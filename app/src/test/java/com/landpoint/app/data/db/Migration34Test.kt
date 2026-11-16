package com.landpoint.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The 3 → 4 migration against real schema files, not mocks: the v3 endpoint is
 * built from the exported `3.json` (the shape every existing install carries),
 * the v4 endpoint is validated against the schema Room derives from the
 * entities today. A migration that drifts from either side fails here rather
 * than on somebody's phone with their only land records in it.
 */
@RunWith(RobolectricTestRunner::class)
class Migration34Test {

    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LandDatabase::class.java
    )

    @Test
    fun `a v3 database migrates, keeps its rows live and accepts soft deletes`() = runTest {
        // A v3 install: one parcel, written with only the columns a v3 build knew.
        helper.createDatabase(DB_NAME, 3).use { v3 ->
            v3.execSQL(
                "INSERT INTO lands " +
                    "(id, name, description, notes, latitude, longitude, geometry_type, status, created_at, updated_at) " +
                    "VALUES ('old-row', 'Sawah lama', '', '', -6.9, 107.6, 'Point', 'aktif', 1000, 1000)"
            )
        }

        // Room validates the result against the derived v4 schema while running
        // the migration, so a mismatched column fails before anything is asserted.
        helper.runMigrationsAndValidate(DB_NAME, 4, true, MIGRATION_3_4).use { migrated ->
            migrated.query("SELECT deleted_at FROM lands WHERE id = 'old-row'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue("an upgraded parcel is live, not binned", cursor.isNull(0))
            }
            migrated.execSQL("UPDATE lands SET deleted_at = 500 WHERE id = 'old-row'")
            migrated.query("SELECT deleted_at FROM lands WHERE id = 'old-row'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(500L, cursor.getLong(0))
            }
        }

        // Room opens the migrated file at v4 and the bin machinery works on it.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.databaseBuilder(context, LandDatabase::class.java, DB_NAME)
            .addMigrations(MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.landDao()
            assertEquals(listOf("Sawah lama"), dao.recentlyDeleted().first().map { it.name })
            assertTrue("a binned parcel must not read as live", dao.getAllOnce().isEmpty())

            dao.restoreById("old-row")
            assertEquals("Sawah lama", dao.getById("old-row")?.land?.name)
            assertTrue(dao.recentlyDeleted().first().isEmpty())
        } finally {
            db.close()
        }
    }

    private companion object {
        const val DB_NAME = "migration-3-4-test.db"
    }
}
