package com.landpoint.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * File sizes are read against each other — the size an import archive reports
 * and the size of a download in progress on the same screen — so "1.2 GB"
 * where another line says "1229 MB" reads as two different numbers. The unit
 * boundary and the decimal precision both matter for that comparison.
 *
 * Locale.US on purpose: this is a size, not a measurement the user recorded,
 * so the app's own decimal-comma rules do not apply to it. A device set to
 * Indonesian must still see a dot here or the figure looks like a different
 * number than the file manager it sits next to.
 */
class ByteFormatTest {

    @Test
    fun `zero and negative sizes print as zero MB`() {
        // The branch is `bytes <= 0`, so both edges are checked: a missing
        // file's length() is -1, and an empty file's is 0.
        assertEquals("0 MB", formatBytes(0))
        assertEquals("0 MB", formatBytes(-1))
        assertEquals("0 MB", formatBytes(-1024))
    }

    @Test
    fun `sub-kilobyte sizes stay in KB`() {
        assertEquals("0.5 KB", formatBytes(512))
        assertEquals("1.0 KB", formatBytes(1024))
        // 1 KiB minus one byte is still KB, not "0 MB".
        assertEquals("1.0 KB", formatBytes(1023))
    }

    @Test
    fun `a megabyte rounds to one decimal`() {
        assertEquals("1.0 MB", formatBytes(1024L * 1024))
        // 1.5 MiB
        assertEquals("1.5 MB", formatBytes(1024L * 1024 + 512L * 1024))
        // The GB branch starts at 1 GiB; one byte less than that is still MB.
        assertEquals("1024.0 MB", formatBytes(1024L * 1024 * 1024 - 1))
    }

    @Test
    fun `a gigabyte rounds to two decimals`() {
        assertEquals("1.00 GB", formatBytes(1024L * 1024 * 1024))
        // 1.5 GiB
        assertEquals("1.50 GB", formatBytes(1024L * 1024 * 1024 + 512L * 1024 * 1024))
        // A multi-gigabyte offline archive.
        val text = formatBytes(5L * 1024 * 1024 * 1024)
        assertTrue("expected ~5.00 GB, got $text", text.startsWith("5.0") && text.endsWith("GB"))
    }

    @Test
    fun `the separator is a dot regardless of the default locale`() {
        // A device set to a comma-decimal locale must still print a dot, or
        // "1.5 MB" reads as a different number than the file manager's "1.5 MB"
        // next to it.
        val previous = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.GERMANY)
            assertEquals("1.5 MB", formatBytes(1024L * 1024 + 512L * 1024))
            assertEquals("1.50 GB", formatBytes(1024L * 1024 * 1024 + 512L * 1024 * 1024))
        } finally {
            java.util.Locale.setDefault(previous)
        }
    }
}
