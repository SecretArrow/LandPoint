package com.landpoint.app.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.landpoint.app.LandPointApp
import com.landpoint.app.data.SettingsRepository
import com.landpoint.app.data.model.Land
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The share sheet is built off a plain string rather than a structured intent
 * extra, so the format a recipient sees is exactly what [ShareUtils.landAsText]
 * produces. Each optional field is a separate branch — description, parcel,
 * altitude, accuracy, address, notes — and a regression that drops one of them
 * shows up here as a missing line in the output, not as a crash on the device.
 *
 * The function does not touch `startActivity`, so it can be exercised fully
 * under Robolectric without faking a chooser; the launcher functions
 * (`shareLand`, `shareFile`, `navigateTo`, `openFile`) all funnel through
 * `startActivity` and are covered by instrumentation tests instead.
 */
@RunWith(RobolectricTestRunner::class)
class ShareUtilsTest {

    private lateinit var context: Context

    private val bandungLat = -6.914744
    private val bandungLon = 107.609810

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<LandPointApp>()
        Localization.setActive(SettingsRepository.Language.SYSTEM)
    }

    @After
    fun tearDown() {
        Localization.setActive(SettingsRepository.Language.SYSTEM)
    }

    /** A land with every optional field populated, to walk every positive branch. */
    private fun fullLand() = Land(
        id = "land-1",
        name = "Sawah Cibadak",
        description = "Rice field by the river",
        notes = "Eastern fence needs replacing",
        latitude = bandungLat,
        longitude = bandungLon,
        altitude = 768.4,
        accuracy = 4.2f,
        address = "Cibadak, Bandung",
        parcelNumber = "P-1001",
        areaSqm = 2500.0,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
        photos = emptyList()
    )

    /** A land with nothing but the required fields, to walk every negative branch. */
    private fun bareLand() = Land(
        id = "land-2",
        name = "Untitled",
        description = "",
        notes = "",
        latitude = bandungLat,
        longitude = bandungLon,
        altitude = null,
        accuracy = null,
        address = null,
        parcelNumber = null,
        areaSqm = null,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
        photos = emptyList()
    )

    @Test
    fun `a full land prints name, description, coords, parcel, altitude, accuracy, address, notes and link`() {
        Localization.setActive(SettingsRepository.Language.ENGLISH)
        val text = ShareUtils.landAsText(context, fullLand())

        assertTrue("name must lead", text.startsWith("Sawah Cibadak"))
        assertTrue("description follows name", text.contains("Rice field by the river"))
        assertTrue("parcel line is present", text.contains("Parcel: P-1001"))
        assertTrue("altitude line is present", text.contains("Altitude: 768 m"))
        assertTrue("accuracy line is present", text.contains("Accuracy: ±4 m"))
        assertTrue("address is present", text.contains("Cibadak, Bandung"))
        assertTrue("notes are present", text.contains("Eastern fence needs replacing"))
        assertTrue("google maps link closes the message", text.contains(GeoUtils.googleMapsLink(bandungLat, bandungLon)))
        // Decimal coordinates, not DMS, by default.
        assertTrue(text.contains(GeoUtils.formatDecimal(bandungLat, bandungLon)))
    }

    @Test
    fun `a bare land prints only name, coords and link`() {
        Localization.setActive(SettingsRepository.Language.ENGLISH)
        val text = ShareUtils.landAsText(context, bareLand())

        assertTrue("name must lead", text.startsWith("Untitled"))
        assertFalse("no parcel line", text.contains("Parcel:"))
        assertFalse("no altitude line", text.contains("Altitude:"))
        assertFalse("no accuracy line", text.contains("Accuracy:"))
        assertFalse("no address line", text.contains("Cibadak"))
        assertTrue("google maps link is present", text.contains(GeoUtils.googleMapsLink(bandungLat, bandungLon)))
    }

    @Test
    fun `dms true writes the sexagesimal form`() {
        Localization.setActive(SettingsRepository.Language.ENGLISH)
        val text = ShareUtils.landAsText(context, fullLand(), dms = true)

        // The degree symbol only appears in the DMS form, never in decimal
        // coordinates or the Google Maps link, so its presence is the cleanest
        // signal that the dms=true branch was taken.
        assertTrue("expected DMS form, got: $text", text.contains("°"))
        // Bandung is south of the equator and east of Greenwich, so the
        // hemisphere markers S and E must both appear somewhere in the DMS line.
        val dmsLine = text.lineSequence().firstOrNull { it.contains("°") }
        assertTrue("expected an S hemisphere marker in $dmsLine", dmsLine?.contains("S") == true)
        assertTrue("expected an E hemisphere marker in $dmsLine", dmsLine?.contains("E") == true)
        // And the decimal form, which is the only other coordinate format
        // produced, must not appear.
        assertFalse(
            "decimal form must not appear when dms=true",
            text.contains(GeoUtils.formatDecimal(bandungLat, bandungLon))
        )
    }

    @Test
    fun `blank-only optionals are skipped as if they were null`() {
        // The takeIf { isNotBlank() } guards mean a whitespace-only parcel or
        // address prints nothing, rather than "Parcel:   " or an empty line.
        // The land name is kept free of the address substring so the absence
        // check is unambiguous.
        Localization.setActive(SettingsRepository.Language.ENGLISH)
        val blankish = fullLand().copy(
            name = "Plot 7",
            description = "   ",
            parcelNumber = "   ",
            address = "   ",
            notes = "   "
        )
        val text = ShareUtils.landAsText(context, blankish)

        assertFalse("blank description skipped", text.contains("Rice field"))
        assertFalse("blank parcel skipped", text.contains("Parcel:"))
        assertFalse("blank address skipped", text.contains("Cibadak, Bandung"))
        assertFalse("blank notes skipped", text.contains("fence"))
    }

    @Test
    fun `the parcel line follows the in-app language`() {
        // Share text reads through Localization.wrap, so an Indonesian user
        // with the phone in English still gets the localised label.
        Localization.setActive(SettingsRepository.Language.INDONESIAN)
        val text = ShareUtils.landAsText(context, fullLand())

        assertTrue("expected 'Persil:', got: $text", text.contains("Persil: P-1001"))
        assertFalse("english label must not appear", text.contains("Parcel:"))
    }

    @Test
    fun `the leading and trailing blank lines are trimmed`() {
        // A recipient pasting this into a chat gets the trimmed form; the
        // buildString leaves a trailing newline that trim must remove.
        Localization.setActive(SettingsRepository.Language.ENGLISH)
        val text = ShareUtils.landAsText(context, bareLand())

        assertFalse("must not start with a blank line", text.startsWith("\n"))
        assertFalse("must not end with a blank line", text.endsWith("\n"))
    }
}
