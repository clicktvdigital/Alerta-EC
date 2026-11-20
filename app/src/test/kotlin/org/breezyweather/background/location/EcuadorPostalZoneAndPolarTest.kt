package org.breezyweather.background.location

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EcuadorPostalZoneAndPolarTest {
    // Trimmed real answer of the official WFS (arcp_cp:zona_postal) for a point in Atucucho, Quito.
    private val atucuchoResponse = """
        {"type": "FeatureCollection", "features": [{"type": "Feature", "properties": {"dpa_provin": "17",
        "dpa_despro": "PICHINCHA", "dpa_canton": "1701", "dpa_descan": "QUITO", "cod_distri": "17D05",
        "cod_postal": "170528"}}]}
    """.trimIndent()

    @Test
    fun parsesOfficialPostalZone() {
        val zone = EcuadorPostalZoneService.parse(atucuchoResponse)!!
        assertEquals("170528", zone.postalCode)
        assertEquals("Pichincha", zone.province)
        assertEquals("17", zone.provinceDpa)
        assertEquals("Quito", zone.canton)
        assertEquals("1701", zone.cantonDpa)
        assertEquals("17D05", zone.district)
    }

    @Test
    fun emptyOrInvalidResponsesGiveNoZone() {
        assertNull(EcuadorPostalZoneService.parse("""{"type":"FeatureCollection","features":[]}"""))
        assertNull(EcuadorPostalZoneService.parse("<ServiceExceptionReport/>"))
        assertNull(
            EcuadorPostalZoneService.parse(
                """{"features":[{"properties":{"cod_postal":"1505"}}]}"""
            )
        )
    }

    @Test
    fun titleCaseKeepsSpanishArticlesLowercase() {
        assertEquals("Santo Domingo de los Tsachilas", EcuadorPostalZoneService.titleCase("SANTO DOMINGO DE LOS TSACHILAS"))
        assertEquals("Morona Santiago", EcuadorPostalZoneService.titleCase("MORONA SANTIAGO"))
    }

    @Test
    fun atucuchoIsAboutTenKilometresNorthOfQuitoCentre() {
        val polar = EcuadorPolarReference.describe(-0.1292993, -78.5131694)!!
        assertEquals("Quito", polar.capital.name)
        assertEquals("Norte", polar.direction)
        assertTrue(polar.distanceKm in 9.8..10.4, "distance ${polar.distanceKm}")
        assertTrue(polar.text.endsWith("km al Norte de Quito (centro)"), polar.text)
    }

    @Test
    fun polarReferenceUsesTheNearestCapitalAndSpanishDirections() {
        // East of Guayaquil's centre.
        val polar = EcuadorPolarReference.describe(-2.1962, -79.80)!!
        assertEquals("Guayaquil", polar.capital.name)
        assertEquals("Oriente", polar.direction)
        // Galápagos is covered too.
        assertEquals("Puerto Baquerizo Moreno", EcuadorPolarReference.describe(-0.95, -89.60)!!.capital.name)
    }

    @Test
    fun pointsOutsideEcuadorAreIgnored() {
        assertNull(EcuadorPolarReference.describe(4.71, -74.07)) // Bogotá
        assertTrue(EcuadorBounds.contains(-0.1293, -78.5132))
    }

    @Test
    fun haversineDistanceIsSane() {
        // One degree of latitude ≈ 111,2 km.
        assertEquals(111.2, EcuadorPolarReference.distanceKm(0.0, -78.0, 1.0, -78.0), 0.2)
    }
}
