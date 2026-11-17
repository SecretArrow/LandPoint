package com.landpoint.app.ui.help

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the help parser on the subset of Markdown the bundled pages use
 * (docs/IMPLEMENTATION_PLAN.md, Phase 4): headings, bullets, soft-wrapped
 * paragraphs, and `**bold**` runs — with an unmatched marker left visible
 * rather than swallowed, so a typo in an asset stays proofreadable.
 */
class HelpMarkdownParseTest {

    @Test
    fun `heading levels map to their marker count`() {
        val blocks = HelpMarkdown.parse("# One\n\n## Two\n\n### Three")

        assertEquals(
            listOf(
                HelpBlock.Heading(1, "One"),
                HelpBlock.Heading(2, "Two"),
                HelpBlock.Heading(3, "Three")
            ),
            blocks
        )
    }

    @Test
    fun `a blank line ends a paragraph`() {
        val blocks = HelpMarkdown.parse("First paragraph.\n\nSecond paragraph.")

        assertEquals(
            listOf(
                HelpBlock.Paragraph(listOf(HelpSegment("First paragraph."))),
                HelpBlock.Paragraph(listOf(HelpSegment("Second paragraph.")))
            ),
            blocks
        )
    }

    @Test
    fun `consecutive plain lines join into one paragraph`() {
        val blocks = HelpMarkdown.parse("Go outdoors — roofs blur\nthe fix.")

        assertEquals(
            listOf(HelpBlock.Paragraph(listOf(HelpSegment("Go outdoors — roofs blur the fix.")))),
            blocks
        )
    }

    @Test
    fun `dashes and asterisks both start a bullet`() {
        val blocks = HelpMarkdown.parse("- One\n* Two")

        assertEquals(
            listOf(
                HelpBlock.Bullet(listOf(HelpSegment("One"))),
                HelpBlock.Bullet(listOf(HelpSegment("Two")))
            ),
            blocks
        )
    }

    @Test
    fun `bold runs split a line into plain and emphasised segments`() {
        val segments = HelpMarkdown.parseInline("Tap **Save land** to start.")

        assertEquals(
            listOf(
                HelpSegment("Tap "),
                HelpSegment("Save land", bold = true),
                HelpSegment(" to start.")
            ),
            segments
        )
    }

    @Test
    fun `bold at the ends of a line survives`() {
        val segments = HelpMarkdown.parseInline("**Both** ends **bold**")

        assertEquals(
            listOf(
                HelpSegment("Both", bold = true),
                HelpSegment(" ends "),
                HelpSegment("bold", bold = true)
            ),
            segments
        )
    }

    @Test
    fun `an unclosed marker stays literal text`() {
        val segments = HelpMarkdown.parseInline("Two asterisks ** stay visible")

        assertTrue(segments.single().text.contains("**"))
    }

    @Test
    fun `a paragraph ends without a trailing blank line`() {
        val blocks = HelpMarkdown.parse("# Title\n\nLast paragraph.")

        assertEquals(2, blocks.size)
        assertEquals(HelpBlock.Paragraph(listOf(HelpSegment("Last paragraph."))), blocks[1])
    }
}
