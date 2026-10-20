package org.breezyweather.ui.map

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.ext.DefaultHandler2

internal object InamhiWmsTimes {
    private val networkMutex = Mutex()
    internal const val MAX_BYTES = 4 * 1024 * 1024
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    data class Catalog(
        val times: Map<String, List<Instant>>,
        val errors: Set<String>,
    )

    private const val MAX_TIMES = 10_000

    /** Solo fechas explícitas o pasos de duración fija; no adivinar `current` ni meses/años. */
    internal fun parseDimension(text: String): List<Instant> {
        if (text.isBlank()) return emptyList()
        require(text.length <= 1_000_000) { "WMS time dimension too large" }
        val times = sortedSetOf<Instant>()
        var count = 0
        text.split(',').forEach { value ->
            val parts = value.trim().split('/')
            when (parts.size) {
                1 -> {
                    require(++count <= MAX_TIMES) { "Too many WMS times" }
                    times.add(Instant.parse(parts[0]))
                }
                3 -> {
                    var time = Instant.parse(parts[0])
                    val end = Instant.parse(parts[1])
                    val step = Duration.parse(parts[2])
                    require(!step.isNegative && !step.isZero && !end.isBefore(time)) {
                        "Invalid WMS time interval"
                    }
                    while (!time.isAfter(end)) {
                        require(++count <= MAX_TIMES) { "Too many WMS times" }
                        times.add(time)
                        if (Duration.between(time, end) < step) break
                        time = time.plus(step)
                    }
                }
                else -> throw IllegalArgumentException("Unsupported WMS time dimension")
            }
        }
        return times.toList()
    }

    private data class Layer(
        val depth: Int,
        var name: String? = null,
        var dimension: String? = null,
    )

    /** El mismo parser XML real en Android y JVM; DTD y entidades externas prohibidas. */
    internal fun parseCapabilities(
        input: java.io.InputStream,
        checkActive: () -> Unit = {},
    ): Catalog {
        val layers = mutableListOf<Layer>()
        val result = mutableMapOf<String, List<Instant>>()
        val errors = mutableSetOf<String>()
        var depth = 0
        var events = 0
        var capabilities = false
        var text: StringBuilder? = null
        var textTag: String? = null
        val handler = object : DefaultHandler2() {
            private fun tick() {
                checkActive()
                require(++events <= 200_000) { "WMS capabilities too large" }
            }

            override fun startDTD(name: String?, publicId: String?, systemId: String?) {
                throw SAXException("WMS DTD not supported")
            }

            override fun resolveEntity(publicId: String?, systemId: String?): InputSource {
                throw SAXException("WMS external entity not supported")
            }

            override fun startElement(uri: String?, localName: String, qName: String?, attrs: Attributes) {
                tick()
                depth++
                require(depth <= 128) { "WMS XML too deep" }
                if (depth == 1) {
                    capabilities = localName in setOf("WMS_Capabilities", "WMT_MS_Capabilities")
                    require(capabilities) { "Not a WMS capabilities response" }
                }
                require(text == null) { "Nested WMS text element" }
                if (localName == "Layer") {
                    layers.add(Layer(depth, dimension = layers.lastOrNull()?.dimension))
                } else if (layers.lastOrNull()?.depth == depth - 1 &&
                    (localName == "Name" || localName in setOf("Dimension", "Extent") &&
                        attrs.getValue("name").equals("time", ignoreCase = true))) {
                    textTag = localName
                    text = StringBuilder()
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                tick()
                text?.let {
                    require(it.length + length <= 1_000_000) { "WMS text too large" }
                    it.append(ch, start, length)
                }
            }

            override fun endElement(uri: String?, localName: String, qName: String?) {
                tick()
                if (text != null && localName == textTag) {
                    val value = text.toString().trim()
                    if (localName == "Name") layers.last().name = value
                    else if (value.isNotEmpty()) layers.last().dimension = value
                    text = null
                    textTag = null
                }
                if (localName == "Layer") {
                    val layer = layers.removeAt(layers.lastIndex)
                    val key = layer.name
                    if (key != null && key in InamhiLayers.all) {
                        if (key in result || key in errors) {
                            result.remove(key)
                            errors.add(key)
                        } else {
                            try {
                                result[key] = parseDimension(layer.dimension.orEmpty())
                            } catch (error: java.time.DateTimeException) {
                                errors.add(key)
                            } catch (error: IllegalArgumentException) {
                                errors.add(key)
                            } catch (error: ArithmeticException) {
                                errors.add(key)
                            }
                        }
                    }
                }
                depth--
            }

            override fun error(e: org.xml.sax.SAXParseException) = throw e
            override fun fatalError(e: org.xml.sax.SAXParseException) = throw e
        }
        val reader = SAXParserFactory.newInstance().apply { isNamespaceAware = true }
            .newSAXParser().xmlReader
        reader.contentHandler = handler
        reader.errorHandler = handler
        reader.entityResolver = handler
        // Obligatorio: si la implementación no admite esta protección, falla sin analizar.
        reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler)
        reader.parse(InputSource(input))
        require(capabilities && layers.isEmpty()) { "Incomplete WMS capabilities" }
        return Catalog(result, errors)
    }

    /** Una descarga cancelable y acotada para todas las capas. */
    suspend fun catalogAsync(): Catalog = networkMutex.withLock {
        val bytes = download(client.newCall(Request.Builder().url(
            InamhiLayers.WMS_URL + "?service=WMS&version=1.3.0&request=GetCapabilities"
        ).build()))
        withContext(Dispatchers.IO) {
            val requestContext = coroutineContext
            parseCapabilities(bytes.inputStream()) { requestContext.ensureActive() }
        }
    }

    suspend fun availableAllAsync(): Map<String, List<Instant>> = catalogAsync().times

    internal suspend fun download(call: Call): ByteArray = suspendCancellableCoroutine { continuation ->
        // Se registra antes de enqueue: cancelar cierra también una lectura bloqueada.
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val bytes = response.use {
                        check(it.isSuccessful) { "WMS HTTP ${it.code}" }
                        val body = checkNotNull(it.body) { "Empty WMS response" }
                        require(body.contentLength() <= MAX_BYTES) { "WMS capabilities too large" }
                        body.byteStream().use { input ->
                            readLimited(input) { check(!call.isCanceled()) { "WMS cancelled" } }
                        }
                    }
                    continuation.resume(bytes)
                } catch (error: Exception) {
                    continuation.resumeWithException(error)
                }
            }
        })
    }

    internal fun readLimited(input: java.io.InputStream, checkActive: () -> Unit = {}): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            checkActive()
            val count = input.read(buffer, 0, minOf(buffer.size, MAX_BYTES - output.size() + 1))
            if (count == -1) return output.toByteArray()
            require(output.size() + count <= MAX_BYTES) { "WMS capabilities too large" }
            output.write(buffer, 0, count)
        }
    }

    suspend fun availableAsync(layerName: String): List<Instant> {
        require(layerName in InamhiLayers.all)
        val catalog = catalogAsync()
        check(layerName !in catalog.errors) { "Invalid WMS time dimension: $layerName" }
        return catalog.times[layerName].orEmpty()
    }

    fun selectUpcoming(
        times: List<Instant>,
        now: Instant = Instant.now(),
        horizonHours: Long = 6L,
    ): Instant? {
        require(horizonHours in 1L..48L)
        val limit = now.plusSeconds(horizonHours * 3600L)
        return times.filter { !it.isBefore(now) && !it.isAfter(limit) }.minOrNull()
    }

    internal fun horizonHoursFor(layerName: String): Long = when (layerName) {
        InamhiLayers.PRECIPITATION_24H -> 24L
        InamhiLayers.PRECIPITATION_3H,
        InamhiLayers.TEMPERATURE_3H -> 6L
        else -> throw IllegalArgumentException("Unsupported INAMHI layer: $layerName")
    }

    suspend fun nextAvailableAsync(layerName: String, now: Instant = Instant.now()): Instant? =
        selectUpcoming(availableAsync(layerName), now, horizonHoursFor(layerName))
}
