package com.landpoint.app.ui.help

import android.content.Context
import java.io.IOException

/**
 * One block-level element of a help page, in document order.
 *
 * Deliberately smaller than Markdown: the bundled pages are written by this
 * repo, for this screen, so the parser carries only what they use — headings,
 * paragraphs, bullets, and `**bold**` inside a line. Everything else a full
 * renderer does (links, tables, images) is deliberately absent: a help page
 * that went looking for the network or opened a browser would break the rule
 * the help centre exists to keep, that reading the manual costs no signal.
 */
sealed interface HelpBlock {

    /** A heading; [level] is 1 for `#`, 2 for `##`, 3 for `###`. */
    data class Heading(val level: Int, val text: String) : HelpBlock

    /** A paragraph of inline runs. */
    data class Paragraph(val segments: List<HelpSegment>) : HelpBlock

    /** One list item of inline runs. */
    data class Bullet(val segments: List<HelpSegment>) : HelpBlock
}

/**
 * A run of text inside a paragraph or bullet — either plain or emphasised.
 * The renderer turns [bold] into a heavier span; the parser never touches
 * styling beyond noticing the markers.
 */
data class HelpSegment(val text: String, val bold: Boolean = false)

/**
 * The tiny Markdown reader behind the help centre.
 *
 * [parse] is pure and JVM-testable; [load] reads a bundled asset and is
 * exercised by a Robolectric test against the real bundled files, so a renamed
 * asset or a typo the parser cannot survive fails CI rather than the user's
 * afternoon in the field.
 */
object HelpMarkdown {

    /**
     * Parses the subset of Markdown the bundled pages use.
     *
     * Rules: `#`/`##`/`###` headings; `- ` or `* ` bullets; a blank line ends a
     * paragraph; consecutive plain lines join into one paragraph with spaces
     * (Markdown's soft wrap). `**bold**` inside any text becomes a [HelpSegment];
     * an unclosed `**` is left as literal text rather than swallowed.
     */
    fun parse(markdown: String): List<HelpBlock> {
        val blocks = mutableListOf<HelpBlock>()
        val paragraph = mutableListOf<String>()

        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                blocks.add(HelpBlock.Paragraph(parseInline(paragraph.joinToString(" "))))
                paragraph.clear()
            }
        }

        markdown.lines().forEach { raw ->
            val line = raw.trim()
            when {
                line.isEmpty() -> flushParagraph()
                line.startsWith("### ") -> {
                    flushParagraph()
                    blocks.add(HelpBlock.Heading(3, line.removePrefix("### ").trim()))
                }

                line.startsWith("## ") -> {
                    flushParagraph()
                    blocks.add(HelpBlock.Heading(2, line.removePrefix("## ").trim()))
                }

                line.startsWith("# ") -> {
                    flushParagraph()
                    blocks.add(HelpBlock.Heading(1, line.removePrefix("# ").trim()))
                }

                line.startsWith("- ") || line.startsWith("* ") -> {
                    flushParagraph()
                    blocks.add(HelpBlock.Bullet(parseInline(line.substring(2).trim())))
                }

                else -> paragraph.add(line)
            }
        }
        flushParagraph()
        return blocks
    }

    /**
     * Splits a line into plain and bold runs on `**bold**` pairs. An unmatched
     * opening marker is kept as text: a typo in an asset must stay visible to
     * whoever proofreads the page, not vanish between two spans.
     */
    fun parseInline(text: String): List<HelpSegment> {
        val segments = mutableListOf<HelpSegment>()
        var rest = text
        while (true) {
            val start = rest.indexOf("**")
            if (start < 0) break
            val end = rest.indexOf("**", start + 2)
            if (end < 0) break
            if (start > 0) segments.add(HelpSegment(rest.substring(0, start)))
            segments.add(HelpSegment(rest.substring(start + 2, end), bold = true))
            rest = rest.substring(end + 2)
        }
        if (rest.isNotEmpty()) segments.add(HelpSegment(rest))
        return segments
    }

    /**
     * The bundled page for a language tag, falling back to English. Only "in"
     * is special — the app ships exactly two languages, and Java's legacy tag
     * for Indonesian is what the configuration reports.
     */
    fun assetFor(languageTag: String?): String = if (languageTag == "in") "help/in.md" else "help/en.md"

    /**
     * Reads [assetPath] out of the APK and parses it. Throws [IOException] when
     * the asset is missing — the screen catches that and says so honestly
     * rather than showing an empty page.
     */
    fun load(context: Context, assetPath: String): List<HelpBlock> =
        context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { reader ->
            parse(reader.readText())
        }
}
