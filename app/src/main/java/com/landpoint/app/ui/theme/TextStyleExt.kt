// The file name is fixed by docs/DESIGN_SYSTEM.md §2.2 and §8 (`TextStyleExt.kt`
// holds the LandPointText wrappers), not by the single object it contains;
// detekt's MatchingDeclarationName is off for the same reason.
@file:Suppress("ktlint:standard:filename")

package com.landpoint.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * Named text wrappers (docs/DESIGN_SYSTEM.md §2.2).
 *
 * Screens stop hard-coding `MaterialTheme.typography.titleMedium` and call
 * these instead, so the indirection lives in one file if the scale ever
 * changes. There are deliberately only three: most text in the app is a
 * title, a line of body copy, or a coordinate.
 */
object LandPointText {

    /**
     * Card titles and list-item primary text ([Typography.titleMedium]).
     * Monospaced when [monospace] is set, so stacked digits line up.
     */
    @Composable
    fun Title(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = Color.Unspecified,
        monospace: Boolean = false,
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
        textAlign: TextAlign? = null
    ) {
        Text(
            text = text,
            modifier = modifier,
            style = MaterialTheme.typography.titleMedium.let {
                if (monospace) it.copy(fontFamily = FontFamily.Monospace) else it
            },
            color = color,
            maxLines = maxLines,
            overflow = overflow,
            textAlign = textAlign
        )
    }

    /**
     * Body copy and descriptions ([Typography.bodyLarge]).
     */
    @Composable
    fun Body(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = Color.Unspecified,
        maxLines: Int = Int.MAX_VALUE,
        overflow: TextOverflow = TextOverflow.Clip,
        textAlign: TextAlign? = null
    ) {
        Text(
            text = text,
            modifier = modifier,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            maxLines = maxLines,
            overflow = overflow,
            textAlign = textAlign
        )
    }

    /**
     * Lat/lng, area, accuracy — the 13sp monospace [CoordinateStyle], so
     * digits line up in a stack of corners.
     */
    @Composable
    fun Coordinate(
        text: String,
        modifier: Modifier = Modifier,
        color: Color = Color.Unspecified,
        maxLines: Int = 1,
        overflow: TextOverflow = TextOverflow.Ellipsis,
        textAlign: TextAlign? = null
    ) {
        Text(
            text = text,
            modifier = modifier,
            style = CoordinateStyle,
            color = color,
            maxLines = maxLines,
            overflow = overflow,
            textAlign = textAlign
        )
    }
}
