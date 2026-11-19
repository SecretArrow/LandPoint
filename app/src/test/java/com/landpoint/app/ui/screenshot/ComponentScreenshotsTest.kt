package com.landpoint.app.ui.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.takahirom.roborazzi.captureRoboImage
import com.landpoint.app.data.SettingsRepository
import com.landpoint.app.data.model.GeometryCodec
import com.landpoint.app.data.model.GeometryType
import com.landpoint.app.data.model.Land
import com.landpoint.app.data.model.LandStatus
import com.landpoint.app.ui.components.LandCard
import com.landpoint.app.ui.components.LandShapeThumb
import com.landpoint.app.ui.components.ParcelHero
import com.landpoint.app.ui.components.StatusChip
import com.landpoint.app.ui.components.Stepper
import com.landpoint.app.ui.theme.LandPointTheme
import com.landpoint.app.ui.theme.Spacing
import com.landpoint.app.util.GeoPoint
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The building blocks of the parcel surfaces, captured alone: the preview
 * hero, the list card (normal and selected/disputed), and the row parts that
 * the list, the detail hero and the map popups share — status chips, shape
 * thumbnails, the onboarding stepper.
 *
 * Composites rendered by these tests use caller-provided strings, so they are
 * locale-independent by construction; only string-resource-bearing screens
 * carry an `in` variant.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComponentScreenshotsTest {

    @Test
    @Config(sdk = [34], qualifiers = SCREENSHOT_QUALIFIERS)
    fun parcelHeroLight() {
        capture("parcel_hero_light_en.png") {
            ParcelHero(
                title = "Kebun Percobaan",
                status = LandStatus.ACTIVE,
                corners = shapeCorners(),
                areaText = "1,25 ha"
            )
        }
    }

    @Test
    @Config(sdk = [34], qualifiers = "$SCREENSHOT_QUALIFIERS-night")
    fun parcelHeroDark() {
        capture("parcel_hero_dark_en.png") {
            ParcelHero(
                title = "Kebun Percobaan",
                status = LandStatus.ACTIVE,
                corners = shapeCorners(),
                areaText = "1,25 ha"
            )
        }
    }

    @Test
    @Config(sdk = [34], qualifiers = SCREENSHOT_QUALIFIERS)
    fun parcelCardLight() {
        capture("parcel_card_light_en.png") {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                LandCard(
                    land = sampleLand(),
                    dms = false,
                    imperial = false,
                    areaUnit = SettingsRepository.AreaUnit.SQM,
                    onClick = {},
                    onNavigate = {}
                )
                LandCard(
                    land = sampleLand().copy(status = "sengketa"),
                    dms = true,
                    imperial = false,
                    areaUnit = SettingsRepository.AreaUnit.ARE,
                    onClick = {},
                    onNavigate = {},
                    selecting = true,
                    selected = true,
                    onToggleSelect = {}
                )
            }
        }
    }

    @Test
    @Config(sdk = [34], qualifiers = "$SCREENSHOT_QUALIFIERS-night")
    fun parcelCardDark() {
        capture("parcel_card_dark_en.png") {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                LandCard(
                    land = sampleLand(),
                    dms = false,
                    imperial = false,
                    areaUnit = SettingsRepository.AreaUnit.SQM,
                    onClick = {},
                    onNavigate = {}
                )
            }
        }
    }

    @Test
    @Config(sdk = [34], qualifiers = SCREENSHOT_QUALIFIERS)
    fun listRowPartsLight() {
        capture("list_row_parts_light_en.png") { rowParts() }
    }

    @Test
    @Config(sdk = [34], qualifiers = "$SCREENSHOT_QUALIFIERS-night")
    fun listRowPartsDark() {
        capture("list_row_parts_dark_en.png") { rowParts() }
    }

    @Composable
    private fun rowParts() {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StatusChip(LandStatus.ACTIVE)
                StatusChip(LandStatus.DISPUTED)
                StatusChip(LandStatus.SOLD)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LandShapeThumb(land = sampleLand())
                LandShapeThumb(
                    land = sampleLand().copy(
                        geometryType = GeometryType.POINT,
                        geometryJson = null
                    )
                )
            }
            Stepper(
                steps = listOf("Welcome", "Measure", "Private"),
                currentStep = 1
            )
        }
    }

    /**
     * A representative polygon parcel — four corners near Jakarta, an owner,
     * a distance and a real area — the same shape the e2e flows record.
     */
    private fun sampleLand(): Land = Land(
        id = "screenshot-1",
        name = "Kebun Percobaan",
        description = "Kebun sayur di tepi jalan",
        notes = "",
        latitude = -6.175,
        longitude = 106.827,
        altitude = null,
        accuracy = null,
        address = null,
        parcelNumber = "051/2024",
        areaSqm = 12500.0,
        geometryType = GeometryType.POLYGON,
        // encode() returns null only below three corners; the elvis keeps the
        // type non-null without asserting.
        geometryJson = GeometryCodec.encode(shapeCorners()) ?: "",
        status = "aktif",
        ownerName = "Bpk. Sutanto",
        distanceMeters = 250.0,
        createdAt = 1_700_000_000_000,
        updatedAt = 1_700_000_000_000,
        photos = emptyList()
    )

    private fun shapeCorners(): List<GeoPoint> = listOf(
        GeoPoint(-6.175, 106.827),
        GeoPoint(-6.1755, 106.8285),
        GeoPoint(-6.1762, 106.8278),
        GeoPoint(-6.1758, 106.8265)
    )

    private fun capture(fileName: String, content: @Composable () -> Unit) {
        settleAnimations()
        captureRoboImage("$SCREENSHOT_DIR$fileName") {
            LandPointTheme { content() }
        }
    }
}
