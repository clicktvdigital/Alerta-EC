package org.breezyweather.background.location

import breezyweather.domain.location.model.LocationAddressInfo
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class EcuadorAddressMapperTest {
    @Test
    fun cityAndParishAreNeverInventedNeighborhoods() {
        val result = EcuadorAddressMapper.fromAddressInfo(
            LocationAddressInfo(countryCode = "EC", city = "Quito", cityDistrict = "Cochapamba")
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
                cityDistrict = "Parroquia de prueba", city = "Ciudad de prueba"
            )
        )
        result.neighborhood shouldBe "Barrio de prueba"
        result.sector shouldBe "Sector de prueba"
        result.parish shouldBe "Parroquia de prueba"
    }
}
