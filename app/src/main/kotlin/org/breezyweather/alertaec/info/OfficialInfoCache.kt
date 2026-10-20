package org.breezyweather.alertaec.info

import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/**
 * Small, source-neutral cache for use below an app-private files directory.
 *
 * Android callers must obtain this through [OfficialInfoCacheFactory], which restricts
 * storage to Context.noBackupFilesDir or Context.filesDir. The core class remains
 * source-neutral for JVM tests. Access is serialized between instances in this process;
 * this class does not provide inter-process locking. [save] replaces the whole collection
 * (it does not merge implicitly). Temporary-file replacement is best effort: platforms
 * may differ in whether renameTo can atomically replace an existing file.
 */
class OfficialInfoCache(
    private val storageDir: File,
    private val limits: OfficialInfoCacheLimits = OfficialInfoCacheLimits()
) {
    private val cacheFile: File
        get() = File(storageDir, FILE_NAME)

    fun save(items: Iterable<OfficialInfoItem>): OfficialInfoCacheWriteResult =
        synchronized(lockForCache()) {
            cleanupStaleTemporaryFiles()
            saveUnlocked(items)
        }

    fun read(): List<OfficialInfoItem> = synchronized(lockForCache()) {
        readUnlocked()
    }

    fun readWithFreshness(
        nowMillis: Long,
        archivePolicy: OfficialInfoArchivePolicy = OfficialInfoArchivePolicy()
    ): List<OfficialInfoCachedItem> = read().map { item ->
        OfficialInfoCachedItem(item, OfficialInfoFreshnessEvaluator.evaluate(item, nowMillis, archivePolicy))
    }

    private fun saveUnlocked(items: Iterable<OfficialInfoItem>): OfficialInfoCacheWriteResult {
        if (!limits.isValid()) return OfficialInfoCacheWriteResult(false, 0, 0, 0)
        val selected = selectValid(items)
        val bytes = encode(selected.items)
        if (bytes.size > limits.maxBytes) {
            return OfficialInfoCacheWriteResult(false, 0, selected.rejectedCount, 0)
        }
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            return OfficialInfoCacheWriteResult(false, 0, selected.rejectedCount, 0)
        }
        return runCatching {
            val temporary = File.createTempFile(TEMP_PREFIX, TEMP_SUFFIX, storageDir)
            try {
                FileOutputStream(temporary).use { output ->
                    output.write(bytes)
                    output.flush()
                    output.fd.sync()
                }
                if (!temporary.renameTo(cacheFile)) {
                    OfficialInfoCacheWriteResult(false, 0, selected.rejectedCount, 0)
                } else {
                    OfficialInfoCacheWriteResult(true, selected.items.size, selected.rejectedCount, bytes.size)
                }
            } finally {
                if (temporary.exists()) temporary.delete()
            }
        }.getOrElse {
            OfficialInfoCacheWriteResult(false, 0, selected.rejectedCount, 0)
        }
    }

    private fun readUnlocked(): List<OfficialInfoItem> {
        if (!limits.isValid()) return emptyList()
        val file = cacheFile
        if (!file.isFile || file.length() > limits.maxBytes) return emptyList()
        return runCatching {
            val lines = file.readLines(StandardCharsets.UTF_8)
            if (lines.firstOrNull() != HEADER) return emptyList()
            val byKey = linkedMapOf<String, OfficialInfoItem>()
            lines.drop(1).forEach { line ->
                decode(line)?.let { item ->
                    if (item.isValid()) {
                        byKey.remove(keyOf(item))
                        byKey[keyOf(item)] = item
                    }
                }
            }
            byKey.values.take(limits.maxRecords)
        }.getOrDefault(emptyList())
    }

    private fun cleanupStaleTemporaryFiles() {
        val cutoff = System.currentTimeMillis() - TEMP_MAX_AGE_MILLIS
        runCatching {
            storageDir.listFiles { file ->
                file.isFile && file.name.startsWith(TEMP_PREFIX) && file.name.endsWith(TEMP_SUFFIX) &&
                    file.lastModified() in 1..cutoff
            }?.forEach { it.delete() }
        }
    }

    private fun lockForCache(): Any {
        val path = runCatching { cacheFile.canonicalPath }.getOrElse { cacheFile.absolutePath }
        return LOCKS.computeIfAbsent(path) { Any() }
    }

    private fun selectValid(items: Iterable<OfficialInfoItem>): Selection {
        val byKey = linkedMapOf<String, OfficialInfoItem>()
        var rejected = 0
        items.forEach { item ->
            if (!item.isValid()) {
                rejected++
            } else {
                val key = keyOf(item)
                byKey.remove(key)
                byKey[key] = item
            }
        }
        while (byKey.size > limits.maxRecords) {
            byKey.remove(byKey.keys.first())
            rejected++
        }
        var encoded = encode(byKey.values)
        while (encoded.size > limits.maxBytes && byKey.isNotEmpty()) {
            byKey.remove(byKey.keys.first())
            rejected++
            encoded = encode(byKey.values)
        }
        return Selection(byKey.values.toList(), rejected)
    }

    private fun encode(items: Collection<OfficialInfoItem>): ByteArray = buildString {
        append(HEADER).append('\n')
        items.forEach { item -> append(encode(item)).append('\n') }
    }.toByteArray(StandardCharsets.UTF_8)

    private fun encode(item: OfficialInfoItem): String = listOf(
        item.sourceId, item.bulletinId, item.category.name, item.title, item.summary, item.body,
        item.country, item.province, item.canton, item.region, item.publishedAtMillis,
        item.updatedAtMillis, item.fetchedAtMillis, item.expiresAtMillis, item.verification.name,
        item.sourceUrl
    ).joinToString("\t") { value -> encodeValue(value) }

    private fun decode(line: String): OfficialInfoItem? = runCatching {
        val fields = line.split('\t')
        if (fields.size != FIELD_COUNT) return null
        OfficialInfoItem(
            sourceId = decodeRequired(fields[0]), bulletinId = decodeRequired(fields[1]),
            category = OfficialInfoCategory.valueOf(decodeRequired(fields[2])),
            title = decodeRequired(fields[3]), summary = decodeRequired(fields[4]),
            body = decodeRequired(fields[5]), country = decodeNullable(fields[6]),
            province = decodeNullable(fields[7]), canton = decodeNullable(fields[8]),
            region = decodeNullable(fields[9]), publishedAtMillis = decodeLong(fields[10]),
            updatedAtMillis = decodeLong(fields[11]), fetchedAtMillis = decodeRequired(fields[12]).toLong(),
            expiresAtMillis = decodeLong(fields[13]),
            verification = OfficialInfoVerification.valueOf(decodeRequired(fields[14])),
            sourceUrl = decodeNullable(fields[15])
        )
    }.getOrNull()

    private fun encodeValue(value: Any?): String = when (value) {
        null -> NULL
        else -> {
            val bytes = value.toString().toByteArray(StandardCharsets.UTF_8)
            val digits = "0123456789abcdef"
            val chars = CharArray(bytes.size * 2)
            bytes.forEachIndexed { index, byte ->
                val unsigned = byte.toInt() and 0xff
                chars[index * 2] = digits[unsigned ushr 4]
                chars[index * 2 + 1] = digits[unsigned and 0x0f]
            }
            String(chars)
        }
    }

    private fun decodeRequired(value: String): String = decodeValue(value) ?: error("missing value")
    private fun decodeNullable(value: String): String? = decodeValue(value)
    private fun decodeLong(value: String): Long? = decodeNullable(value)?.toLong()

    private fun decodeValue(value: String): String? {
        if (value == NULL) return null
        if (value.length % 2 != 0) error("invalid encoding")
        val bytes = ByteArray(value.length / 2)
        for (index in bytes.indices) {
            val high = hexDigit(value[index * 2])
            val low = hexDigit(value[index * 2 + 1])
            bytes[index] = ((high shl 4) or low).toByte()
        }
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun keyOf(item: OfficialInfoItem): String = item.sourceId + KEY_SEPARATOR + item.bulletinId
    private fun hexDigit(value: Char): Int = when (value) {
        in '0'..'9' -> value - '0'
        in 'a'..'f' -> value - 'a' + 10
        in 'A'..'F' -> value - 'A' + 10
        else -> error("invalid encoding")
    }

    private data class Selection(val items: List<OfficialInfoItem>, val rejectedCount: Int)

    companion object {
        private const val FILE_NAME = "official-info-cache-v1.dat"
        private const val HEADER = "ALERTA_EC_OFFICIAL_INFO_CACHE_V1"
        private const val NULL = "-"
        private const val FIELD_COUNT = 16
        private const val KEY_SEPARATOR = '\u0000'
        private const val TEMP_PREFIX = "official-info-"
        private const val TEMP_SUFFIX = ".tmp"
        private const val TEMP_MAX_AGE_MILLIS = 24L * 60L * 60L * 1000L
        private val LOCKS = ConcurrentHashMap<String, Any>()
    }
}

data class OfficialInfoCacheLimits(
    val maxRecords: Int = 100,
    val maxBytes: Int = 1_048_576
) {
    fun isValid(): Boolean = maxRecords > 0 && maxBytes > 0
}

data class OfficialInfoCacheWriteResult(
    val success: Boolean,
    val savedCount: Int,
    val rejectedCount: Int,
    val bytesWritten: Int
)

data class OfficialInfoCachedItem(
    val item: OfficialInfoItem,
    val freshness: OfficialInfoFreshness
)
