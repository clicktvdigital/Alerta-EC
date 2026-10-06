/*
 * Part of Alerta EC, based on Breezy Weather.
 * Licensed under the GNU Lesser General Public License v3.0.
 */

package org.breezyweather.background.map

import android.content.Context
import org.breezyweather.common.extensions.withIOContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class OfflineMapDownloader @Inject constructor(
    @Named("MapDownloadClient")
    private val client: OkHttpClient,
) {

    suspend fun download(
        context: Context,
        regionId: String,
        url: String,
        onState: (OfflineMapDownloadState) -> Unit = {},
    ): OfflineMapDownloadState = withIOContext {
        if (!url.startsWith("https://", ignoreCase = true)) {
            return@withIOContext OfflineMapDownloadState.Failed(
                "La descarga del mapa requiere HTTPS.",
            ).also(onState)
        }

        val temporary = OfflineMapStorage.temporaryFile(context, regionId)
        OfflineMapStorage.discardTemporaryFile(context, regionId)

        try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}")
                }

                val body = response.body
                val totalBytes = body.contentLength().takeIf { it >= 0L }

                if (totalBytes != null) {
                    val safetyReserveBytes = 100L * 1024L * 1024L
                    val availableBytes = temporary.parentFile?.usableSpace ?: 0L
                    val requiredBytes = totalBytes + safetyReserveBytes

                    if (availableBytes < requiredBytes) {
                        throw IOException(
                            "Espacio insuficiente: se requieren al menos $requiredBytes bytes " +
                                "y hay $availableBytes bytes disponibles.",
                        )
                    }
                }

                var downloadedBytes = 0L
                var nextStorageCheckBytes = 8L * 1024L * 1024L
                val minimumFreeSpaceBytes = 100L * 1024L * 1024L

                body.byteStream().use { input ->
                    temporary.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break

                            output.write(buffer, 0, count)
                            downloadedBytes += count

                            if (downloadedBytes >= nextStorageCheckBytes) {
                                val freeBytes = temporary.parentFile?.usableSpace ?: 0L
                                if (freeBytes < minimumFreeSpaceBytes) {
                                    throw IOException(
                                        "Descarga detenida para conservar espacio libre en el dispositivo.",
                                    )
                                }
                                nextStorageCheckBytes = downloadedBytes + 8L * 1024L * 1024L
                            }

                            onState(
                                OfflineMapDownloadState.Downloading(
                                    bytesDownloaded = downloadedBytes,
                                    totalBytes = totalBytes,
                                ),
                            )
                        }

                        output.flush()
                    }
                }

                if (totalBytes != null && downloadedBytes != totalBytes) {
                    throw IOException(
                        "Descarga incompleta: $downloadedBytes de $totalBytes bytes",
                    )
                }

                if (!PmTilesValidator.isValidVersion3(temporary)) {
                    throw IOException("El archivo descargado no es PMTiles v3 válido.")
                }

                if (!OfflineMapStorage.installDownloadedFile(context, regionId)) {
                    throw IOException("No se pudo instalar el mapa descargado.")
                }

                OfflineMapDownloadState.Completed(
                    bytesDownloaded = downloadedBytes,
                ).also(onState)
            }
        } catch (e: Exception) {
            OfflineMapStorage.discardTemporaryFile(context, regionId)

            OfflineMapDownloadState.Failed(
                message = e.message ?: "Error desconocido al descargar el mapa.",
            ).also(onState)
        }
    }
}
