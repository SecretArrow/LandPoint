package com.landpoint.app.location

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The parts of averaging the existing suite does not reach: which provider a
 * provider name names, the per-radio sigma floors and fallbacks, and altitude
 * travelling through the weighted mean. Expected figures are worked from the
 * implementation, not asserted loosely — a sigma that drifts quietly reweights
 * every fix the app reports.
 */
class LocationAveragingSigmaTest {

    private val lat = -6.9
    private val lon = 107.6

    // ---- FixSource.of -------------------------------------------------------

    @Test
    fun `the platform provider names map onto their sources`() {
        assertEquals(FixSource.GPS, FixSource.of("gps"))
        assertEquals(FixSource.NETWORK, FixSource.of("network"))
    }

    // ---- sigmaOf ------------------------------------------------------------

    @Test
    fun `a radio's own floor wins over an optimistic accuracy`() {
        // 2 m claimed by a GPS radio is under its 3 m floor; a fused fix is
        // floored higher because there is no way to ask what blended it; a
        // cell-tower fix can never be trusted below 30 m.
        assertEquals(3.0, LocationAveraging.sigmaOf(GeoSample(lat, lon, accuracy = 2f, source = FixSource.GPS)), 1e-9)
        assertEquals(5.0, LocationAveraging.sigmaOf(GeoSample(lat, lon, accuracy = 2f, source = FixSource.FUSED)), 1e-9)
        assertEquals(10.0, LocationAveraging.sigmaOf(GeoSample(lat, lon, accuracy = 2f, source = FixSource.OTHER)), 1e-9)
    }

    @Test
    fun `a sample with no accuracy gets its source's fallback`() {
        assertEquals(30.0, LocationAveraging.sigmaOf(GeoSample(lat, lon, accuracy = null, source = FixSource.GPS)), 1e-9)
        assertEquals(50.0, LocationAveraging.sigmaOf(GeoSample(lat, lon, accuracy = null, source = FixSource.FUSED)), 1e-9)
    }

    @Test
    fun `an accuracy that cannot be believed is treated as absent`() {
        // Zero and NaN are both refused by the takeIf, so both fall through to
        // the source's fallback figure.
        assertEquals(30.0, LocationAveraging.sigmaOf(GeoSample(lat, lon, accuracy = 0f, source = FixSource.GPS)), 1e-9)
        assertEquals(
            50.0,
            LocationAveraging.sigmaOf(GeoSample(lat, lon, accuracy = Float.NaN, source = FixSource.FUSED)),
            1e-9
        )
    }

    // ---- average, altitude --------------------------------------------------

    @Test
    fun `altitude is carried as a weighted mean`() {
        // Equal sigmas mean equal weights, so two readings 100 m and 200 m up
        // average to 150 m exactly.
        val fix = LocationAveraging.average(
            listOf(
                GeoSample(lat, lon, altitude = 100.0, accuracy = 5f, source = FixSource.GPS),
                GeoSample(lat, lon, altitude = 200.0, accuracy = 5f, source = FixSource.GPS)
            )
        )!!

        assertEquals(150.0, fix.altitude!!, 1e-9)
        assertEquals(2, fix.used)
        assertEquals(0, fix.rejected)
        assertEquals(setOf(FixSource.GPS), fix.sources)
        // Equal weights and zero scatter: the textbook figure 1/sqrt(sum of
        // weights) with each weight 1/3².
        assertEquals(2.121, fix.accuracy, 1e-3)
    }

    @Test
    fun `a sample without altitude contributes position but not elevation`() {
        val fix = LocationAveraging.average(
            listOf(
                GeoSample(lat, lon, altitude = 100.0, accuracy = 5f, source = FixSource.GPS),
                GeoSample(lat, lon, altitude = null, accuracy = 5f, source = FixSource.GPS),
                GeoSample(lat, lon, altitude = 200.0, accuracy = 5f, source = FixSource.GPS)
            )
        )!!

        // Only the two readings that carry an altitude take part in the mean.
        assertEquals(150.0, fix.altitude!!, 1e-9)
        assertEquals(3, fix.used)
    }
}
