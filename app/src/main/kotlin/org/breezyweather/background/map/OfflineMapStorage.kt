/*
 * Part of Alerta EC, based on Breezy Weather.
 * Licensed under the GNU Lesser General Public License v3.0.
 */

package org.breezyweather.background.map

import android.content.Context
import java.io.File

object OfflineMapStorage {

    private const val DIRECTORY_NAME = "offline_maps"

    fun directory(context: Context): File {
        return File(context.filesDir, DIRECTORY_NAME).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    fun pmTilesFile(context: Context, regionId: String): File {
        val safeRegionId = regionId
            .lowercase()
            .replace(Regex("[^a-z0-9_-]"), "_")

        return File(directory(context), "$safeRegionId.pmtiles")
    }

    fun exists(context: Context, regionId: String): Boolean {
        val file = pmTilesFile(context, regionId)
        return file.isFile && file.length() > 0L
    }

    fun sizeBytes(context: Context, regionId: String): Long {
        return pmTilesFile(context, regionId)
            .takeIf { it.isFile }
            ?.length()
            ?: 0L
    }

    fun pmTilesUrl(context: Context, regionId: String): String? {
        val file = pmTilesFile(context, regionId)

        if (!file.isFile || file.length() == 0L) {
            return null
        }

        return "pmtiles://file://${file.absolutePath}"
    }

    fun delete(context: Context, regionId: String): Boolean {
        val file = pmTilesFile(context, regionId)
        return !file.exists() || file.delete()
    }
}
