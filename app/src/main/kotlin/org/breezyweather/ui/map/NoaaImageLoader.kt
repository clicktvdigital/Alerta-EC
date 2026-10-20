package org.breezyweather.ui.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

internal object NoaaImageLoader {
    private val cache = object : android.util.LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.byteCount / 1024).coerceAtLeast(1)
    }

    suspend fun load(url: String): Bitmap? = withContext(Dispatchers.IO) {
        synchronized(cache) { cache.get(url) }?.let { return@withContext it }
        if (!url.startsWith("https://cdn.star.nesdis.noaa.gov/GOES19/")) return@withContext null
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 20000
        try {
            if (connection.responseCode != 200 ||
                !connection.contentType.orEmpty().startsWith("image/")) return@withContext null
            if (connection.contentLengthLong > 6_000_000L) return@withContext null
            connection.inputStream.use { input ->
                val bytes = input.readNBytes(6_000_001)
                if (bytes.size > 6_000_000) return@withContext null
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                if (options.outWidth <= 0 || options.outHeight <= 0) return@withContext null
                val sample = generateSequence(1) { it * 2 }
                    .first { options.outWidth / it <= 2048 && options.outHeight / it <= 2048 }
                options.inJustDecodeBounds = false
                options.inSampleSize = sample
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.also { bitmap ->
                    synchronized(cache) { cache.put(url, bitmap) }
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
