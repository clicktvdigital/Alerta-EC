package org.breezyweather.alertaec.info

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class OfficialInfoCacheTest {
    @Test
    fun savesReadsAndDeduplicatesBySourceAndBulletin() {
        val directory = Files.createTempDirectory("official-info-cache").toFile()
        val cache = OfficialInfoCache(directory)
        val result = cache.save(listOf(item(title = "primero"), item(title = "reemplazo")))
        assertTrue(result.success)
        assertEquals(1, result.savedCount)
        assertEquals("reemplazo", cache.read().single().title)
    }

    @Test
    fun saveReplacesTheWholeCollectionAndHonorsExactRecordLimit() {
        val directory = Files.createTempDirectory("official-info-cache").toFile()
        val cache = OfficialInfoCache(directory, OfficialInfoCacheLimits(maxRecords = 100, maxBytes = 1_048_576))
        val items = (0..100).map { item(bulletinId = "bulletin-$it") }
        val result = cache.save(items)
        assertTrue(result.success)
        assertEquals(100, result.savedCount)
        assertEquals(100, cache.read().size)
        assertTrue(cache.save(listOf(item(bulletinId = "only"))).success)
        assertEquals(listOf("only"), cache.read().map { it.bulletinId })
    }

    @Test
    fun rejectsInvalidRecordsAndInvalidLimits() {
        val directory = Files.createTempDirectory("official-info-cache").toFile()
        val invalid = item(title = " ")
        val cache = OfficialInfoCache(directory, OfficialInfoCacheLimits(maxRecords = 1, maxBytes = 600))
        val result = cache.save(listOf(invalid, item(bulletinId = "second", body = "contenido")))
        assertTrue(result.success)
        assertTrue(result.rejectedCount >= 1)
        assertTrue(result.bytesWritten <= 600)
        val invalidLimits = OfficialInfoCache(directory, OfficialInfoCacheLimits(0, 0))
        assertFalse(invalidLimits.save(listOf(item())).success)
        assertEquals(emptyList<OfficialInfoItem>(), invalidLimits.read())
    }

    @Test
    fun exactByteLimitIsAcceptedAndOversizedFileIsRejected() {
        val directory = Files.createTempDirectory("official-info-cache").toFile()
        val source = OfficialInfoCache(directory)
        val written = source.save(listOf(item())).bytesWritten
        val exact = OfficialInfoCache(directory, OfficialInfoCacheLimits(maxBytes = written, maxRecords = 100))
        assertEquals(1, exact.read().size)
        directory.resolve("official-info-cache-v1.dat").appendText("x".repeat(2))
        assertEquals(emptyList<OfficialInfoItem>(), exact.read())
    }

    @Test
    fun unknownVersionTruncatedAndMalformedRecordsAreIgnored() {
        val directory = Files.createTempDirectory("official-info-cache").toFile()
        val file = directory.resolve("official-info-cache-v1.dat")
        file.writeText("UNKNOWN\n")
        assertEquals(emptyList<OfficialInfoItem>(), OfficialInfoCache(directory).read())
        file.writeText("ALERTA_EC_OFFICIAL_INFO_CACHE_V1\n0a\n")
        assertEquals(emptyList<OfficialInfoItem>(), OfficialInfoCache(directory).read())
        file.writeText("ALERTA_EC_OFFICIAL_INFO_CACHE_V1\nzz\n")
        assertEquals(emptyList<OfficialInfoItem>(), OfficialInfoCache(directory).read())
    }

    @Test
    fun unicodeAndNewlinesRoundTripWithoutChangingTheSchema() {
        val directory = Files.createTempDirectory("official-info-cache").toFile()
        val value = item(title = "Lluvia ágil", body = "Línea uno\nLínea dos — sintético")
        val cache = OfficialInfoCache(directory)
        assertTrue(cache.save(listOf(value)).success)
        assertEquals(value, cache.read().single())
    }

    @Test
    fun archivedFreshnessIsPreservedWhenReading() {
        val directory = Files.createTempDirectory("official-info-cache").toFile()
        val cache = OfficialInfoCache(directory)
        assertTrue(cache.save(listOf(item())).success)
        assertEquals(OfficialInfoFreshness.ARCHIVED, cache.readWithFreshness(3_000L, OfficialInfoArchivePolicy(retainExpiredForMillis = 1_000L)).single().freshness)
    }

    @Test
    fun nonexistentDirectoryAndSequentialAccessAreSafe() {
        val directory = Files.createTempDirectory("official-info-parent").toFile().resolve("nested")
        val cache = OfficialInfoCache(directory)
        assertTrue(cache.save(listOf(item())).success)
        assertEquals(1, cache.read().size)
        assertTrue(cache.save(listOf(item(bulletinId = "next"))).success)
        assertEquals("next", cache.read().single().bulletinId)
    }

    @Test
    fun sharedLockPreventsCorruptConcurrentWritesBetweenInstances() {
        val directory = Files.createTempDirectory("official-info-cache").toFile()
        val first = OfficialInfoCache(directory)
        val second = OfficialInfoCache(directory)
        val ready = CountDownLatch(2)
        val executor = Executors.newFixedThreadPool(2)
        try {
            listOf(first to "one", second to "two").forEach { (cache, id) ->
                executor.submit {
                    ready.countDown()
                    ready.await()
                    cache.save(listOf(item(bulletinId = id)))
                }
            }
            executor.shutdown()
            while (!executor.isTerminated) Thread.yield()
            assertTrue(first.read().size <= 1)
            assertTrue(first.read().all { it.isValid() })
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun staleOwnedTemporaryFilesAreCleanedButFreshOnesRemain() {
        val directory = Files.createTempDirectory("official-info-cache").toFile()
        val stale = directory.resolve("official-info-stale.tmp")
        val fresh = directory.resolve("official-info-active.tmp")
        stale.writeText("old")
        fresh.writeText("active")
        val old = System.currentTimeMillis() - 2L * 24L * 60L * 60L * 1000L
        stale.setLastModified(old)
        assertTrue(OfficialInfoCache(directory).save(listOf(item())).success)
        assertFalse(stale.exists())
        assertTrue(fresh.exists())
    }

    @Test
    fun incompleteTemporaryWriteDoesNotReplaceExistingCache() {
        val directory = Files.createTempDirectory("official-info-cache").toFile()
        val cache = OfficialInfoCache(directory)
        assertTrue(cache.save(listOf(item(title = "durable"))).success)
        directory.resolve("official-info-interrupted.tmp").writeText("partial")
        assertEquals("durable", cache.read().single().title)
        assertFalse(directory.resolve("official-info-cache-v1.dat").readText().contains("partial"))
    }

    private fun item(
        sourceId: String = "synthetic",
        bulletinId: String = "bulletin-1",
        title: String = "Aviso sintético",
        body: String = "Contenido sintético"
    ) = OfficialInfoItem(
        sourceId, bulletinId, OfficialInfoCategory.ALERT, title, "Resumen sintético", body,
        "Ecuador", "Pichincha", "Quito", "Sierra", 1_000L, 1_000L, 1_100L, 2_000L,
        OfficialInfoVerification.UNVERIFIED, "https://example.test/bulletin/1"
    )
}
