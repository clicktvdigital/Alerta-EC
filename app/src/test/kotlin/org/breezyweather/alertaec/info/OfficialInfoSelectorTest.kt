package org.breezyweather.alertaec.info

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class OfficialInfoSelectorTest {

    @Test
    fun sourceVerifiedActiveEntryIsCurrent() {
        val directory = Files.createTempDirectory("official-info-selector-source-verified").toFile()
        val cache = OfficialInfoCache(directory)
        assertTrue(cache.save(listOf(item("source-verified", verification = OfficialInfoVerification.SOURCE_VERIFIED))).success)

        val result = OfficialInfoSelector(cache).select(1_500L)

        assertEquals(listOf("source-verified"), result.current.map { it.item.bulletinId })
        assertTrue(result.history.isEmpty())
    }


    @Test
    fun officialActiveEntryIsCurrent() {
        val directory = Files.createTempDirectory("official-info-selector-official").toFile()
        val cache = OfficialInfoCache(directory)
        assertTrue(cache.save(listOf(item("official", verification = OfficialInfoVerification.OFFICIAL))).success)

        val result = OfficialInfoSelector(cache).select(1_500L)

        assertEquals(listOf("official"), result.current.map { it.item.bulletinId })
        assertTrue(result.history.isEmpty())
    }


    @Test
    fun unverifiedActiveEntryIsExcludedFromCurrent() {
        val directory = Files.createTempDirectory("official-info-selector-unverified").toFile()
        val cache = OfficialInfoCache(directory)
        assertTrue(cache.save(listOf(item("unverified", verification = OfficialInfoVerification.UNVERIFIED))).success)

        val result = OfficialInfoSelector(cache).select(1_500L)

        assertTrue(result.current.isEmpty())
        assertEquals(listOf("unverified"), result.history.map { it.item.bulletinId })
    }

    @Test
    fun separatesVerifiedActiveEntriesFromAllHistoricalStates() {
        val directory = Files.createTempDirectory("official-info-selector").toFile()
        val cache = OfficialInfoCache(directory)
        val active = item("active", verification = OfficialInfoVerification.SOURCE_VERIFIED)
        val unverified = item("unverified", verification = OfficialInfoVerification.UNVERIFIED)
        val expired = item("expired", expiresAtMillis = 2_000L)
        val archived = item("archived", expiresAtMillis = 2_000L)
        val unknown = item("unknown", expiresAtMillis = null)
        assertTrue(cache.save(listOf(active, unverified, expired, archived, unknown)).success)

        val result = OfficialInfoSelector(cache, OfficialInfoArchivePolicy(1_000L)).select(3_000L)

        assertEquals(listOf("active"), result.current.map { it.item.bulletinId })
        assertEquals(
            setOf("unverified", "expired", "archived", "unknown"),
            result.history.map { it.item.bulletinId }.toSet()
        )
        assertTrue(result.history.all { it.freshness != OfficialInfoFreshness.ACTIVE || it.item.verification == OfficialInfoVerification.UNVERIFIED })
    }

    @Test
    fun exactExpiryWithoutArchiveRetentionIsHistoricalExpired() {
        val directory = Files.createTempDirectory("official-info-selector").toFile()
        val cache = OfficialInfoCache(directory)
        assertTrue(cache.save(listOf(item("boundary", expiresAtMillis = 2_000L))).success)

        val result = OfficialInfoSelector(cache).select(2_000L)

        assertEquals(emptyList<String>(), result.current.map { it.item.bulletinId })
        assertEquals(OfficialInfoFreshness.EXPIRED, result.history.single().freshness)
    }

    @Test
    fun archivedStateIsRetainedOnlyWithExplicitPolicy() {
        val directory = Files.createTempDirectory("official-info-selector").toFile()
        val cache = OfficialInfoCache(directory)
        assertTrue(cache.save(listOf(item("old", expiresAtMillis = 2_000L))).success)

        val result = OfficialInfoSelector(cache, OfficialInfoArchivePolicy(1_000L)).select(3_000L)

        assertEquals(OfficialInfoFreshness.ARCHIVED, result.history.single().freshness)
    }

    @Test
    fun orderingIsStableAndUsesExistingTimestamps() {
        val directory = Files.createTempDirectory("official-info-selector").toFile()
        val cache = OfficialInfoCache(directory)
        assertTrue(cache.save(listOf(item("b", publishedAtMillis = 1_000L), item("a", publishedAtMillis = 1_000L))).success)

        val result = OfficialInfoSelector(cache).select(1_500L)

        assertEquals(listOf("a", "b"), result.current.map { it.item.bulletinId })
    }

    @Test
    fun emptyOrMalformedCacheDoesNotCreateCurrentEntries() {
        // Una caché corrupta puede aparecer como EMPTY porque OfficialInfoCache ignora
        // registros ilegibles; READ_ERROR no garantiza detectar toda corrupción.
        val directory = Files.createTempDirectory("official-info-selector").toFile()
        val file = directory.resolve("official-info-cache-v1.dat")
        file.writeText("malformed\n")

        val result = OfficialInfoSelector(OfficialInfoCache(directory)).select(1_000L)

        assertEquals(OfficialInfoSelectionReadState.EMPTY, result.readState)
        assertEquals(emptyList<OfficialInfoCachedItem>(), result.current)
        assertEquals(emptyList<OfficialInfoCachedItem>(), result.history)
    }

    @Test
    fun noNetworkOrLocationIsNeededForSelection() {
        val directory = Files.createTempDirectory("official-info-selector").toFile()
        val cache = OfficialInfoCache(directory)
        assertTrue(cache.save(listOf(item("offline"))).success)

        val result = OfficialInfoSelector(cache).select(1_500L)

        assertEquals(listOf("offline"), result.current.map { it.item.bulletinId })
    }

    private fun item(
        bulletinId: String,
        verification: OfficialInfoVerification = OfficialInfoVerification.OFFICIAL,
        publishedAtMillis: Long = 1_000L,
        expiresAtMillis: Long? = 10_000L
    ) = OfficialInfoItem(
        sourceId = "synthetic",
        bulletinId = bulletinId,
        category = OfficialInfoCategory.ALERT,
        title = "Aviso $bulletinId",
        summary = "Resumen sintético",
        body = "Contenido sintético",
        country = "Ecuador",
        province = "Pichincha",
        canton = "Quito",
        region = "Sierra",
        publishedAtMillis = publishedAtMillis,
        updatedAtMillis = publishedAtMillis,
        fetchedAtMillis = publishedAtMillis + 100L,
        expiresAtMillis = expiresAtMillis,
        verification = verification,
        sourceUrl = "https://example.test/$bulletinId"
    )
}
