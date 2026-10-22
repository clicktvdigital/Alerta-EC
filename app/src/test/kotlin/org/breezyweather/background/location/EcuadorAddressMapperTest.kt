package org.breezyweather.background.location

import breezyweather.domain.location.model.LocationAddressInfo
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class EcuadorAddressMapperTest {
    @Test
    fun cityAndParishAreNeverInventedNeighborhoods() {
        val result = EcuadorAddressMapper.fromAddressInfo(
            LocationAddressInfo(countryCode = "EC", city = "Quito", admin3 = "Cochapamba")
        )
        result.neighborhood shouldBe null
        result.sector shouldBe null
        result.parish shouldBe "Cochapamba"
    }

    @Test
    fun explicitNeighborhoodAndSectorRemainSeparateFromParish() {
        val result = EcuadorAddressMapper.fromAddressInfo(
            LocationAddressInfo(
                countryCode = "EC", neighborhood = "Barrio de prueba", quarter = "Sector de prueba",
                admin3 = "Parroquia de prueba", city = "Ciudad de prueba"
            )
        )
        result.neighborhood shouldBe "Barrio de prueba"
        result.sector shouldBe "Sector de prueba"
        result.parish shouldBe "Parroquia de prueba"
    }

    @Test
    fun preservesProviderLocalityWithoutTurningItIntoNeighborhood() {
        val result = EcuadorAddressMapper.fromAddressInfo(
            LocationAddressInfo(
                countryCode = "EC",
                locality = "Comunidad de prueba",
                neighborhood = null,
                quarter = "Sector de prueba",
            )
        )

        result.locality shouldBe "Comunidad de prueba"
        result.neighborhood shouldBe null
        result.completeAddressLines shouldBe listOf(
            "Sector: Sector de prueba",
            "Localidad/comunidad: Comunidad de prueba",
        )
    }
    @Test
    fun parsesPostalResponseAndRejectsMalformedOrEmptyData() {
        val parsed = EcuadorPostalCodeAdapter.parse(
            "{\"data\":[{\"codigo_postal\":\"123456\",\"provincia\":\"SINTETICA\"}]}",
            provenance = "authorized-test-source",
        )
        parsed shouldBe PostalCodeParseResult.Success(
            PostalCodeCandidate(
                code = "123456",
                provenance = "authorized-test-source",
                confidence = PostalCodeConfidence.AUTHORIZED_OFFICIAL,
            )
        )
        EcuadorPostalCodeAdapter.parse("", "authorized-test-source") shouldBe PostalCodeParseResult.Empty
        EcuadorPostalCodeAdapter.parse("{\"data\":[]}", "authorized-test-source") shouldBe PostalCodeParseResult.Empty
        EcuadorPostalCodeAdapter.parse("{\"codigo_postal\":\"123456\"}", "")
            .let { it is PostalCodeParseResult.Invalid } shouldBe true
        EcuadorPostalCodeAdapter.parse("{\"codigo_postal\":\"12345\"}", "authorized-test-source")
            .let { it is PostalCodeParseResult.Invalid } shouldBe true
        EcuadorPostalCodeAdapter.parse("not-json", "authorized-test-source")
            .let { it is PostalCodeParseResult.Invalid } shouldBe true
        EcuadorPostalCodeAdapter.parse(
            "{\"data\":[{\"codigo_postal\":\"123456\"}]}texto",
            "authorized-test-source",
        ).let { it is PostalCodeParseResult.Invalid } shouldBe true
    }

    @Test
    fun authorizedPostalCodeWinsAndAddressOrderRemainsControlled() {
        val base = EcuadorAddressMapper.fromAddressInfo(
            LocationAddressInfo(
                countryCode = "EC", road = "Calle principal", houseNumber = "10",
                neighborhood = "Barrio sintético", quarter = "Sector sintético",
                locality = "Comunidad sintética", postalCode = "654321",
            )
        )
        val candidate = PostalCodeCandidate(
            code = "123456", provenance = "authorized-test-source",
            confidence = PostalCodeConfidence.AUTHORIZED_OFFICIAL,
        )
        val merged = base.mergePostalCode(candidate)
        merged.postalCode shouldBe "123456"
        merged.postalCodeProvenance shouldBe "authorized-test-source"
        merged.completeAddressLines shouldBe listOf(
            "Calle principal 10",
            "Código postal: 123456",
            "Sector: Sector sintético",
            "Barrio: Barrio sintético",
            "Localidad/comunidad: Comunidad sintética",
        )
        merged.mergePostalCode(
            PostalCodeCandidate("999999", "lower-confidence", PostalCodeConfidence.REVERSE_GEOCODER)
        ).postalCode shouldBe "123456"
        merged.mergePostalCode(
            PostalCodeCandidate("111111", "", PostalCodeConfidence.AUTHORIZED_OFFICIAL)
        ).postalCode shouldBe "123456"
        merged.mergePostalCode(
            PostalCodeCandidate("222222", "unverified", PostalCodeConfidence.NONE)
        ).postalCode shouldBe "123456"
    }

}
