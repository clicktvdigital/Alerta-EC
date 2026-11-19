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
    // 12 MB (antes 24 MB): en TV de 1 GB la caché no debe competir con el mapa de MapLibre.
    private val cache = object : android.util.LruCache<String, Bitmap>(12 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.byteCount / 1024).coerceAtLeast(1)
    }
    private val loadMutex = Mutex()

    /** Loads [url]; with [crop], only that region is decoded (Ecuador out of a 3600 px frame). */
    suspend fun load(url: String, crop: NoaaCrop? = null): Bitmap? = withContext(Dispatchers.IO) {
        if (!url.startsWith("https://cdn.star.nesdis.noaa.gov/GOES19/")) return@withContext null
        loadMutex.withLock {
            currentCoroutineContext().ensureActive()
            val key = if (crop == null) url else "$url#$crop"
            synchronized(cache) { cache.get(key) }?.let { return@withLock it }
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
                    val region = crop?.let {
                        android.graphics.Rect(
                            (it.left * options.outWidth).toInt(),
                            (it.top * options.outHeight).toInt(),
                            (it.right * options.outWidth).toInt(),
                            (it.bottom * options.outHeight).toInt(),
                        )
                    }
                    val width = region?.width() ?: options.outWidth
                    val height = region?.height() ?: options.outHeight
                    // Máximo 1280x720: suficiente para el TV y mucho más liviano en memoria.
                    val sample = generateSequence(1) { it * 2 }
                        .first { width / it <= 1280 && height / it <= 720 }
                    options.inJustDecodeBounds = false
                    options.inSampleSize = sample
                    options.inPreferredConfig = Bitmap.Config.RGB_565
                    currentCoroutineContext().ensureActive()
                    val bitmap = if (region == null) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    } else {
                        // Region decode: only the Ecuador window is ever held in memory (≈0,5 MP).
                        val decoder = if (android.os.Build.VERSION.SDK_INT >= 31) {
                            android.graphics.BitmapRegionDecoder.newInstance(bytes, 0, bytes.size)
                        } else {
                            @Suppress("DEPRECATION")
                            android.graphics.BitmapRegionDecoder.newInstance(bytes, 0, bytes.size, false)
                        }
                        try {
                            decoder?.decodeRegion(region, options)
                        } finally {
                            decoder?.recycle()
                        }
                    }
                    bitmap?.also { synchronized(cache) { cache.put(key, it) } }
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
