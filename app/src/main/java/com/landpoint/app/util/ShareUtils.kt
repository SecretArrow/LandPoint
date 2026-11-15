package com.landpoint.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.landpoint.app.R
import com.landpoint.app.data.model.Land

object ShareUtils {

    /** Human-readable record of a land, used for the share sheet and clipboard. */
    fun landAsText(context: Context, land: Land, dms: Boolean = false): String {
        val res = Localization.wrap(context).resources
        return buildString {
            appendLine(land.name)
            if (land.description.isNotBlank()) appendLine(land.description)
            appendLine()
            appendLine(
                if (dms) {
                    GeoUtils.formatDMS(land.latitude, land.longitude)
                } else {
                    GeoUtils.formatDecimal(land.latitude, land.longitude)
                }
            )
            land.parcelNumber?.takeIf { it.isNotBlank() }?.let {
                appendLine(res.getString(R.string.share_parcel, it))
            }
            land.altitude?.let {
                appendLine(res.getString(R.string.share_altitude, it.toInt()))
            }
            land.accuracy?.let {
                appendLine(res.getString(R.string.share_accuracy, it.toInt()))
            }
            land.address?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
            if (land.notes.isNotBlank()) {
                appendLine()
                appendLine(land.notes)
            }
            appendLine()
            appendLine(GeoUtils.googleMapsLink(land.latitude, land.longitude))
        }.trim()
    }

    /**
     * Shares a plain-text body with a subject.
     *
     * The generic form behind [shareLand]: records that are not saved lands
     * yet (the preview draft) share the same way, without a [Land] to build
     * the body from.
     */
    fun shareText(context: Context, subject: String, body: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        startChooserOrToast(context, intent)
    }

    fun shareLand(context: Context, land: Land, dms: Boolean = false) {
        shareText(context, land.name, landAsText(context, land, dms))
    }

    fun shareFile(context: Context, uri: Uri, mimeType: String, subject: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startChooserOrToast(context, intent)
    }

    /**
     * A chooser normally resolves even with nothing installed, but a device
     * with the system share UI stripped out throws instead — and a share that
     * fails must not cost the user the screen they were on.
     */
    private fun startChooserOrToast(context: Context, intent: Intent) {
        try {
            context.startActivity(Intent.createChooser(intent, null))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                Localization.wrap(context).getString(R.string.toast_no_share_app),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Hands off to whatever maps app the user has, for a position that is not
     * a saved land yet — a preview draft's centroid. Falls back to a browser
     * link when no geo: handler is installed.
     */
    fun navigateTo(context: Context, latitude: Double, longitude: Double, label: String?) {
        val geo = Intent(Intent.ACTION_VIEW, Uri.parse(GeoUtils.geoUri(latitude, longitude, label)))
        try {
            context.startActivity(geo)
        } catch (e: ActivityNotFoundException) {
            val web = Intent(Intent.ACTION_VIEW, Uri.parse(GeoUtils.googleMapsLink(latitude, longitude)))
            try {
                context.startActivity(web)
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    Localization.wrap(context).getString(R.string.toast_no_maps_app),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun navigateTo(context: Context, land: Land) {
        navigateTo(context, land.latitude, land.longitude, land.name)
    }

    fun openFile(context: Context, uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                Localization.wrap(context).getString(R.string.toast_no_file_handler),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
