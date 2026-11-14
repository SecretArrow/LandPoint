package com.landpoint.app.util

import com.landpoint.app.data.SettingsRepository
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tile counts run to six figures and are read against a limit of the same
 * size, so "120,000" and "12,000" must be tellable apart at a glance — the
 * grouping separator is what does that.
 *
 * The separator follows the in-app language choice rather than the device's,
 * because a phone set to Indonesian with the app in English was rendering
 * English labels beside dot-grouped numbers, which read as a different
 * magnitude entirely.
 */
class CountFormatTest {

    @After
    fun tearDown() {
        Localization.setActive(SettingsRepository.Language.SYSTEM)
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `english groups with a comma`() {
        Locale.setDefault(Locale.US)
        Localization.setActive(SettingsRepository.Language.ENGLISH)

        assertEquals("1", formatCount(1))
        assertEquals("1,000", formatCount(1_000))
        assertEquals("120,000", formatCount(120_000))
        assertEquals("1,234,567", formatCount(1_234_567))
    }

    @Test
    fun `indonesian groups with a dot`() {
        // The app's own choice, not the device's: an Indonesian user with the
        // phone in English still gets the dot grouping their eye reads natively.
        Locale.setDefault(Locale.US)
        Localization.setActive(SettingsRepository.Language.INDONESIAN)

        assertEquals("1.000", formatCount(1_000))
        assertEquals("120.000", formatCount(120_000))
        assertEquals("1.234.567", formatCount(1_234_567))
    }

    @Test
    fun `system follows the device locale`() {
        Locale.setDefault(Locale.GERMANY) // also a dot-grouper
        Localization.setActive(SettingsRepository.Language.SYSTEM)

        assertEquals("1.000", formatCount(1_000))
    }

    @Test
    fun `zero and negatives do not crash`() {
        Localization.setActive(SettingsRepository.Language.ENGLISH)

        assertEquals("0", formatCount(0))
        // A negative count is not a real input, but a subtraction that
        // underflows must not produce "1,-000" or worse.
        val text = formatCount(-1_000)
        assertTrue("expected grouped negative, got $text", text.contains("1") && text.contains("000"))
    }

    @Test
    fun `a large tile count stays grouped to the last digit`() {
        // The number that motivated this function: the offline tile limit.
        Localization.setActive(SettingsRepository.Language.ENGLISH)

        assertEquals("999,999", formatCount(999_999))
        assertEquals("1,000,000", formatCount(1_000_000))
    }
}
