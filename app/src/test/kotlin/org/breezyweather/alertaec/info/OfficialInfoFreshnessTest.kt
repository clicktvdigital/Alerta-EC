package org.breezyweather.alertaec.info

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OfficialInfoFreshnessTest {
    private val published = 1_000L
    private val expires = 2_000L
    private val fetched = 1_100L

    @Test
    fun activeUntilButNotAtExpiry() {
        val item = item()
        assertEquals(OfficialInfoFreshness.ACTIVE, OfficialInfoFreshnessEvaluator.evaluate(item, 1_999L))
        assertEquals(OfficialInfoFreshness.EXPIRED, OfficialInfoFreshnessEvaluator.evaluate(item, expires))
    }

    @Test
    fun injectedClockControlsResult() {
        val item = item()
        assertEquals(OfficialInfoFreshness.ACTIVE, OfficialInfoFreshnessEvaluator.evaluate(item, 1_500L))
        assertEquals(OfficialInfoFreshness.EXPIRED, OfficialInfoFreshnessEvaluator.evaluate(item, 2_001L))
    }

    @Test
    fun missingExpiryIsUnknownAndArchiveRequiresExplicitPolicy() {
        val item = item(expiresAtMillis = null)
        assertEquals(OfficialInfoFreshness.UNKNOWN, OfficialInfoFreshnessEvaluator.evaluate(item, 1_500L))
        assertEquals(
            OfficialInfoFreshness.ARCHIVED,
            OfficialInfoFreshnessEvaluator.evaluate(
                item(),
                2_500L,
                OfficialInfoArchivePolicy(retainExpiredForMillis = 1_000L)
            )
        )
    }

    @Test
    fun inconsistentTimestampsAreRejectedWithoutThrowing() {
        val item = item(updatedAtMillis = 900L)
        assertFalse(item.isValid())
        assertEquals(OfficialInfoFreshness.UNKNOWN, OfficialInfoFreshnessEvaluator.evaluate(item, 1_500L))
    }

    @Test
    fun missingPublicationEvidenceIsUnknown() {
        val item = item(publishedAtMillis = null, updatedAtMillis = null)
        assertEquals(OfficialInfoFreshness.UNKNOWN, OfficialInfoFreshnessEvaluator.evaluate(item, 1_500L))
    }

    @Test
    fun futurePublicationIsUnknown() {
        val item = item(updatedAtMillis = 2_500L, fetchedAtMillis = 3_000L)
        assertEquals(OfficialInfoFreshness.UNKNOWN, OfficialInfoFreshnessEvaluator.evaluate(item, 2_000L))
    }

    @Test
    fun invalidUrlIsRejected() {
        val item = item(sourceUrl = "http://example.test/bulletin")
        assertTrue(item.validationErrors().contains(OfficialInfoValidationError.INVALID_SOURCE_URL))
        assertEquals(OfficialInfoFreshness.UNKNOWN, OfficialInfoFreshnessEvaluator.evaluate(item, 1_500L))
    }

    @Test
    fun localHttpsDestinationsAreRejected() {
        assertTrue(item(sourceUrl = "https://localhost/bulletin").validationErrors().contains(OfficialInfoValidationError.INVALID_SOURCE_URL))
        assertTrue(item(sourceUrl = "https://192.168.1.20/bulletin").validationErrors().contains(OfficialInfoValidationError.INVALID_SOURCE_URL))
    }

    @Test
    fun impossiblePublicationOrExpiryOrderIsRejected() {
        assertFalse(item(publishedAtMillis = 2_000L, updatedAtMillis = null, fetchedAtMillis = 1_500L).isValid())
        assertFalse(item(expiresAtMillis = 900L).isValid())
    }

    @Test
    fun noPreciseCoordinatesAreRepresentable() {
        val item = item(country = "Ecuador", province = "Pichincha", canton = "Quito", region = "Sierra")
        assertTrue(item.isValid())
        assertEquals(null, item::class.java.declaredFields.firstOrNull { it.name.contains("lat", true) })
        assertEquals(null, item::class.java.declaredFields.firstOrNull { it.name.contains("lon", true) })
    }

    private fun item(
        expiresAtMillis: Long? = expires,
        publishedAtMillis: Long? = published,
        updatedAtMillis: Long? = published,
        fetchedAtMillis: Long = fetched,
        sourceUrl: String? = "https://example.test/bulletin/1",
        country: String? = "Ecuador",
        province: String? = "Pichincha",
        canton: String? = "Quito",
        region: String? = "Sierra"
    ) = OfficialInfoItem(
        sourceId = "synthetic",
        bulletinId = "bulletin-1",
        category = OfficialInfoCategory.ALERT,
        title = "Aviso sintético",
        summary = "Resumen sintético",
        body = "Contenido sintético",
        country = country,
        province = province,
        canton = canton,
        region = region,
        publishedAtMillis = publishedAtMillis,
        updatedAtMillis = updatedAtMillis,
        fetchedAtMillis = fetchedAtMillis,
        expiresAtMillis = expiresAtMillis,
        verification = OfficialInfoVerification.UNVERIFIED,
        sourceUrl = sourceUrl
    )
}
