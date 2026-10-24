package org.breezyweather.ui.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal object NoaaImageLoader {
    private val cache = object : android.util.LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.byteCount / 1024).coerceAtLeast(1)
    }
    private val loadMutex = Mutex()

    suspend fun load(url: String): Bitmap? = withContext(Dispatchers.IO) {
        if (!url.startsWith("https://cdn.star.nesdis.noaa.gov/GOES19/")) return@withContext null
        loadMutex.withLock {
            currentCoroutineContext().ensureActive()
            synchronized(cache) { cache.get(url) }?.let { return@withLock it }
            val connection = try {
                URL(url).openConnection() as HttpURLConnection
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                return@withLock null
            }
            connection.connectTimeout = 10000
            connection.readTimeout = 20000
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK ||
                    !connection.contentType.orEmpty().startsWith("image/")) return@withLock null
                if (connection.contentLengthLong > 6_000_000L) return@withLock null
                connection.inputStream.use { input ->
                    val bytes = input.readNBytes(6_000_001)
                    currentCoroutineContext().ensureActive()
                    if (bytes.size > 6_000_000) return@withLock null
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    if (options.outWidth <= 0 || options.outHeight <= 0) return@withLock null
                    val sample = generateSequence(1) { it * 2 }
                        .first { options.outWidth / it <= 2048 && options.outHeight / it <= 2048 }
                    options.inJustDecodeBounds = false
                    options.inSampleSize = sample
                    options.inPreferredConfig = Bitmap.Config.RGB_565
                    currentCoroutineContext().ensureActive()
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.also { bitmap ->
                        synchronized(cache) { cache.put(url, bitmap) }
                    }
                }
            } catch (error: IOException) {
                null
            } catch (error: RuntimeException) {
                if (error is CancellationException) throw error
                null
            } finally {
                connection.disconnect()
            }
        }
    }
}
