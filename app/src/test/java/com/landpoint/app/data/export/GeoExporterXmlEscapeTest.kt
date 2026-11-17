package com.landpoint.app.data.export

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * XML escaping in the two files other people's software reads. The main
 * exporter suite covers ampersand, angle brackets and quotes; this pins the
 * apostrophe branch — an owner's name like "Pak Budi's" is the ordinary case,
 * and a stray raw quote inside a GPX attribute is a silent importer rejection.
 */
class GeoExporterXmlEscapeTest {

    private val land = LandTestFactory.land(name = "Pak Budi's <plot> & \"fence\"")

    @Test
    fun `gpx escapes every xml special, apostrophe included`() {
        val gpx = GeoExporter.toGpx(listOf(land), "test disclaimer")

        assertTrue(gpx.contains("<name>Pak Budi&apos;s &lt;plot&gt; &amp; &quot;fence&quot;</name>"))
        // Nothing raw survives inside the element.
        assertFalse(gpx.contains("<name>Pak Budi's"))
    }

    @Test
    fun `kml escapes every xml special, apostrophe included`() {
        val kml = GeoExporter.toKml(listOf(land), "test disclaimer")

        assertTrue(kml.contains("<name>Pak Budi&apos;s &lt;plot&gt; &amp; &quot;fence&quot;</name>"))
        assertFalse(kml.contains("<name>Pak Budi's"))
    }
}
