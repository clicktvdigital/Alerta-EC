/*
 * Part of Alerta EC, based on Breezy Weather.
 * Licensed under the GNU Lesser General Public License v3.0.
 */

package org.breezyweather.background.map

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.breezyweather.common.extensions.withIOContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
private class InvalidPartialDownloadException(message: String) : IOException(message)

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
        val partialBytes = OfflineMapStorage.temporarySizeBytes(context, regionId)

        try {
            val requestBuilder = Request.Builder()
                .url(url)
                .get()

            if (partialBytes > 0L) {
                requestBuilder.header("Range", "bytes=$partialBytes-")
            }

            val request = requestBuilder.build()

            val call = client.newCall(request)
            val coroutineJob = currentCoroutineContext().job
            val cancellationHandle = coroutineJob.invokeOnCompletion { cause ->
                if (cause is CancellationException) {
                    call.cancel()
                }
            }

            currentCoroutineContext().ensureActive()

            try {
                call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}")
                }

                val body = response.body
                val contentRange = response.header("Content-Range")
                val contentRangeMatch = contentRange
                    ?.let { Regex("""bytes\s+(\d+)-(\d+)/(\d+|\*)""").matchEntire(it) }
                val contentRangeStart = contentRangeMatch?.groupValues?.get(1)?.toLongOrNull()
                val contentRangeEnd = contentRangeMatch?.groupValues?.get(2)?.toLongOrNull()
                val contentRangeTotal = contentRangeMatch?.groupValues?.get(3)
                    ?.takeUnless { it == "*" }
                    ?.toLongOrNull()

                val responseBytes = body.contentLength().takeIf { it >= 0L }
                val rangeLengthMatches = contentRangeStart != null &&
                    contentRangeEnd != null &&
                    contentRangeEnd >= contentRangeStart &&
                    (responseBytes == null || responseBytes == contentRangeEnd - contentRangeStart + 1L)

                val isValidResume = partialBytes > 0L &&
                    response.code == 206 &&
                    contentRangeStart == partialBytes &&
                    rangeLengthMatches &&
                    (contentRangeTotal == null || contentRangeEnd!! < contentRangeTotal)

                if (partialBytes > 0L && response.code == 206 && !isValidResume) {
                    throw InvalidPartialDownloadException(
                        "El servidor respondió con un rango incompatible: $contentRange",
                    )
                }

                val totalBytes = if (isValidResume) {
                    contentRangeTotal ?: responseBytes?.let { partialBytes + it }
                } else {
                    responseBytes
                }

                if (responseBytes != null) {
                    val safetyReserveBytes = 100L * 1024L * 1024L
                    val availableBytes = temporary.parentFile?.usableSpace ?: 0L
                    val requiredAdditionalBytes = responseBytes + safetyReserveBytes

                    if (availableBytes < requiredAdditionalBytes) {
                        throw IOException(
                            "Espacio insuficiente: se requieren al menos $requiredAdditionalBytes bytes adicionales " +
                                "y hay $availableBytes bytes disponibles.",
                        )
                    }
                }

                val resumedBytes = if (isValidResume) partialBytes else 0L
                var downloadedBytes = resumedBytes
                var nextStorageCheckBytes = downloadedBytes + 8L * 1024L * 1024L
                val minimumFreeSpaceBytes = 100L * 1024L * 1024L

                body.byteStream().use { input ->
                    FileOutputStream(temporary, isValidResume).buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

                        while (true) {
                            currentCoroutineContext().ensureActive()

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
                    throw InvalidPartialDownloadException("El archivo descargado no es PMTiles v3 válido.")
                }

                if (!OfflineMapStorage.installDownloadedFile(context, regionId)) {
                    throw IOException("No se pudo instalar el mapa descargado.")
                }

                OfflineMapDownloadState.Completed(
                    bytesDownloaded = downloadedBytes,
                ).also(onState)
                }
            } finally {
                cancellationHandle.dispose()
            }
        } catch (e: CancellationException) {
            // Keep the partial PMTiles file so a later request can resume it.
            throw e
        } catch (e: InvalidPartialDownloadException) {
            // Never resume a partial file whose range or PMTiles structure is invalid.
            OfflineMapStorage.discardTemporaryFile(context, regionId)

            OfflineMapDownloadState.Failed(
                message = e.message ?: "La descarga parcial no es válida.",
            ).also(onState)
        } catch (e: Exception) {
            // Keep recoverable partial downloads (for example, a temporary network failure).
            OfflineMapDownloadState.Failed(
                message = e.message ?: "Error desconocido al descargar el mapa.",
            ).also(onState)
        }
    }
}
