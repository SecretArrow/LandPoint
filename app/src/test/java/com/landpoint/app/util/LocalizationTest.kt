package com.landpoint.app.util

import com.landpoint.app.data.SettingsRepository
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * The in-app language override mirrors the persisted setting for code that
 * builds user-facing text outside composition (PDF export, share text,
 * snackbars). The two pieces that have to behave deterministically without a
 * Context — which locale numbers should follow, and which language is currently
 * active — are checked here. [Localization.wrap] needs a Context, so its
 * behaviour is checked in [AppStringsTest] against a real resource lookup.
 */
class LocalizationTest {

    @After
    fun tearDown() {
        Localization.setActive(SettingsRepository.Language.SYSTEM)
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `system follows the device default`() {
        Locale.setDefault(Locale.GERMANY)
        Localization.setActive(SettingsRepository.Language.SYSTEM)

        assertEquals(Locale.GERMANY, Localization.numberLocale())
    }

    @Test
    fun `english pins the locale regardless of the device`() {
        Locale.setDefault(Locale.GERMANY) // a comma-decimal device
        Localization.setActive(SettingsRepository.Language.ENGLISH)

        assertEquals(Locale("en"), Localization.numberLocale())
    }

    @Test
    fun `indonesian pins the locale regardless of the device`() {
        Locale.setDefault(Locale.US) // a dot-decimal device
        Localization.setActive(SettingsRepository.Language.INDONESIAN)

        assertEquals(Locale("in"), Localization.numberLocale())
    }

    @Test
    fun `setting a language sticks until reset`() {
        // PDF export and the share sheet both read `active` more than once while
        // building a single document, so a stale or racing read shows up as a
        // half-localised string.
        Localization.setActive(SettingsRepository.Language.INDONESIAN)
        assertEquals(Locale("in"), Localization.numberLocale())
        assertEquals(Locale("in"), Localization.numberLocale())

        Localization.setActive(SettingsRepository.Language.ENGLISH)
        assertEquals(Locale("en"), Localization.numberLocale())
    }

    @Test
    fun `every language resolves to a distinct locale or the system fallback`() {
        // SYSTEM is the only null-locale entry; the rest must be distinct, or a
        // new translation silently falls back to the default's separator.
        val locales = SettingsRepository.Language.entries.map { it.locale }
        val nonNull = locales.filterNotNull()
        assertEquals(nonNull.size, nonNull.toSet().size)
        assertSame(null, SettingsRepository.Language.SYSTEM.locale)
        SettingsRepository.Language.entries.filter { it != SettingsRepository.Language.SYSTEM }.forEach {
            assertNotSame(null, it.locale)
        }
    }
}
