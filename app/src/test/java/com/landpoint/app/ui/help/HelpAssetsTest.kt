package com.landpoint.app.ui.help

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The bundled help pages themselves: both languages load out of the APK's
 * assets, both carry the six sections the help centre promises, and the
 * language-to-asset pick falls back to English for anything else.
 *
 * Robolectric reads the app's merged assets, so a renamed or mistyped asset
 * path fails here rather than on a phone.
 */
@RunWith(RobolectricTestRunner::class)
class HelpAssetsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `the english page loads and carries the six sections`() {
        val blocks = HelpMarkdown.load(context, "help/en.md")
        val sections = blocks.filterIsInstance<HelpBlock.Heading>()
            .filter { it.level == 2 }
            .map { it.text }

        assertEquals(listOf("Capture", "Measure", "Map", "Offline", "Privacy", "Export"), sections)
    }

    @Test
    fun `the indonesian page loads and carries the six sections`() {
        val blocks = HelpMarkdown.load(context, "help/in.md")
        val sections = blocks.filterIsInstance<HelpBlock.Heading>()
            .filter { it.level == 2 }
            .map { it.text }

        assertEquals(listOf("Merekam", "Mengukur", "Peta", "Luring", "Privasi", "Ekspor"), sections)
    }

    @Test
    fun `both pages open with the app's name`() {
        listOf("help/en.md", "help/in.md").forEach { asset ->
            val first = HelpMarkdown.load(context, asset).first()
            assertTrue(first is HelpBlock.Heading)
            assertTrue((first as HelpBlock.Heading).text.contains("LandPoint"))
        }
    }

    @Test
    fun `the asset follows the app language and falls back to english`() {
        assertEquals("help/in.md", HelpMarkdown.assetFor("in"))
        assertEquals("help/en.md", HelpMarkdown.assetFor("en"))
        assertEquals("help/en.md", HelpMarkdown.assetFor(null))
        assertEquals("help/en.md", HelpMarkdown.assetFor("fr"))
    }
}
