package com.landpoint.app.map.offline

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The one refusal the main header suite does not reach: a header whose own
 * offsets are negative. No real archive has one — the value would mean a
 * position past 2^63 — and the check exists so the bounds arithmetic below it
 * can never be fed nonsense. As with that suite, the test writes its own bytes
 * and shares no layout constant with the code.
 */
class PmtilesHeaderGuardTest {

    @get:Rule
    val temp = TemporaryFolder()

    /** A v3-shaped header with whatever longs are asked for written into it. */
    private fun header(
        rootOffset: Long = 127L,
        rootLength: Long = 73L,
        tileDataOffset: Long = 200L,
        tileDataLength: Long = 800L
    ): ByteArray {
        val raw = ByteArray(PmtilesHeader.HEADER_BYTES)
        "PMTiles".toByteArray(Charsets.US_ASCII).copyInto(raw)
        raw[7] = 3.toByte()

        val buffer = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putLong(8, rootOffset)
        buffer.putLong(16, rootLength)
        buffer.putLong(56, tileDataOffset)
        buffer.putLong(64, tileDataLength)
        buffer.putLong(72, 4_096L)
        return raw
    }

    private fun archive(head: ByteArray): File {
        val file = temp.newFile()
        file.writeBytes(head + ByteArray(1_000))
        return file
    }

    @Test
    fun `a negative root offset is refused as not an archive`() {
        assertEquals(
            PmtilesResult.NotPmtiles,
            PmtilesHeader.read(archive(header(rootOffset = -1L)))
        )
    }

    @Test
    fun `a negative tile data length is refused as not an archive`() {
        assertEquals(
            PmtilesResult.NotPmtiles,
            PmtilesHeader.read(archive(header(tileDataLength = -5L)))
        )
    }
}
