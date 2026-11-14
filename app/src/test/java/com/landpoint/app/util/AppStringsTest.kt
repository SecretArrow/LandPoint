package com.landpoint.app.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.landpoint.app.LandPointApp
import com.landpoint.app.R
import com.landpoint.app.data.SettingsRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Resolves strings for code that runs outside composition — ViewModel snackbar
 * messages, PDF labels, share text — so the lookup must follow the in-app
 * language the user picked, not the device's. Resources are looked up per call,
 * so a language change is meant to take effect without anything being recreated.
 *
 * This also exercises [Localization.wrap] end-to-end: a wrapped [Context] whose
 * `getResources()` resolves in the chosen locale is what AppStrings reads from,
 * and a wrong wrapper shows up here as the wrong translation.
 */
@RunWith(RobolectricTestRunner::class)
class AppStringsTest {

    private lateinit var context: Context
    private lateinit var strings: AppStrings

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<LandPointApp>()
        strings = AppStrings(context)
        // The default state other tests leave behind is non-deterministic, so
        // pin it explicitly each time.
        Localization.setActive(SettingsRepository.Language.SYSTEM)
    }

    @After
    fun tearDown() {
        Localization.setActive(SettingsRepository.Language.SYSTEM)
    }

    @Test
    fun `a plain string resolves`() {
        assertEquals("LandPoint", strings.get(R.string.app_name))
    }

    @Test
    fun `a parameterised string substitutes its argument`() {
        assertEquals("Parcel: ABC-123", strings.get(R.string.share_parcel, "ABC-123"))
    }

    @Test
    fun `a plural picks the one form`() {
        // The strings file ships English plurals, so the default (SYSTEM with a
        // US device, or ENGLISH) must take the singular branch at count = 1.
        Localization.setActive(SettingsRepository.Language.ENGLISH)
        assertEquals("1 land", strings.plural(R.plurals.list_summary, 1))
        assertEquals("5 lands", strings.plural(R.plurals.list_summary, 5))
    }

    @Test
    fun `a string array resolves in order`() {
        // Cardinal directions are indexed by GeoUtils.cardinalIndex, so the
        // order here must line up with that function's mapping.
        val cardinals = strings.array(R.array.cardinal_directions)
        assertEquals(listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"), cardinals.toList())
    }

    @Test
    fun `a language change takes effect on the next lookup without recreation`() {
        // AppStrings is held by ViewModels that outlive a language toggle, so
        // the same instance must read the new translation on its next call.
        Localization.setActive(SettingsRepository.Language.ENGLISH)
        assertEquals("Parcel: P-1", strings.get(R.string.share_parcel, "P-1"))

        Localization.setActive(SettingsRepository.Language.INDONESIAN)
        assertEquals("Persil: P-1", strings.get(R.string.share_parcel, "P-1"))
    }

    @Test
    fun `wrap with system returns the context unchanged`() {
        // The SYSTEM choice has no locale to apply, so wrap is a no-op: the same
        // Context instance comes back. An eager wrapper that always rebuilds the
        // configuration would break this identity check.
        Localization.setActive(SettingsRepository.Language.SYSTEM)
        assertSame(context, Localization.wrap(context))
    }

    @Test
    fun `wrap with indonesian resolves a translated string`() {
        // End-to-end: the wrapped context's getResources must resolve in the
        // Indonesian locale. This is what the share sheet and PDF export call.
        Localization.setActive(SettingsRepository.Language.INDONESIAN)
        val wrapped = Localization.wrap(context)
        val res = wrapped.resources
        assertEquals("Persil: X", res.getString(R.string.share_parcel, "X"))
        assertEquals(
            "Ketinggian: 768 m",
            res.getString(R.string.share_altitude, 768)
        )
        // And the default app_name is the same in both, since it is not
        // translated.
        assertEquals("LandPoint", res.getString(R.string.app_name))
    }

    @Test
    fun `wrap with english resolves the default-string-table form`() {
        Localization.setActive(SettingsRepository.Language.ENGLISH)
        val res = Localization.wrap(context).resources
        assertEquals("Parcel: X", res.getString(R.string.share_parcel, "X"))
        assertEquals("Altitude: 768 m", res.getString(R.string.share_altitude, 768))
        // The toast for a missing share app is also in the default table.
        assertTrue(res.getString(R.string.toast_no_share_app).isNotEmpty())
    }
}
