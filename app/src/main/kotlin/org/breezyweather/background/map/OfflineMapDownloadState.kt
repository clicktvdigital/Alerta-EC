/*
 * Part of Alerta EC, based on Breezy Weather.
 * Licensed under the GNU Lesser General Public License v3.0.
 */

package org.breezyweather.background.map

sealed interface OfflineMapDownloadState {

    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long?,
    ) : OfflineMapDownloadState {

        val progressPercent: Int?
            get() = totalBytes
                ?.takeIf { it > 0L }
                ?.let { total ->
                    ((bytesDownloaded * 100L) / total)
                        .coerceIn(0L, 100L)
                        .toInt()
                }
    }

    data class Completed(
        val bytesDownloaded: Long,
    ) : OfflineMapDownloadState

    data class Failed(
        val message: String,
    ) : OfflineMapDownloadState
}
