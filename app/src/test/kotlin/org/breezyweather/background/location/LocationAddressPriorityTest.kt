package org.breezyweather.background.location

import breezyweather.domain.location.model.Location
import breezyweather.domain.location.model.LocationAddressInfo
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Locale

class LocationAddressPriorityTest {
    @Test
    fun reverseGeocodingKeepsForecastCoordinatesAndManualLabel() {
        val original = Location(latitude = 0.1, longitude = -78.0, customName = "Etiqueta de prueba")
        val address = LocationAddressInfo(
            countryCode = "EC", latitude = 0.2, longitude = -78.1,
            neighborhood = "Barrio de prueba", cityDistrict = "Parroquia de prueba"
        )
        val result = original.toLocationWithAddressInfo(Locale.ROOT, address, overwriteCoordinates = false)
        result.latitude shouldBe original.latitude
        result.longitude shouldBe original.longitude
        result.customName shouldBe original.customName
        result.district shouldBe "Barrio de prueba"
        original.toLocationWithAddressInfo(
            Locale.ROOT, address.copy(neighborhood = " ", quarter = "Sector de prueba"), false
        ).district shouldBe "Sector de prueba"
        original.toLocationWithAddressInfo(
            Locale.ROOT, address.copy(neighborhood = null), false
        ).district shouldBe "Parroquia de prueba"
    }
    @Test
    fun localityNameDoesNotBecomeConfirmedNeighborhood() {
        // Synthetic point; the names reproduce the documented provider field roles, not a live lookup.
        val original = Location(latitude = 1.123456789, longitude = 2.987654321)
        val address = LocationAddressInfo(
            countryCode = "EC", city = "Atucucho", quarter = "San Jose", cityDistrict = "Cochapamba"
        )
        val result = original.toLocationWithAddressInfo(Locale.ROOT, address, false)
        result.city shouldBe "Atucucho"
        result.district shouldBe "San Jose"
        EcuadorAddressMapper.fromAddressInfo(address).neighborhood shouldBe null
        original.toLocationWithAddressInfo(Locale.ROOT, address.copy(quarter = null), false)
            .district shouldBe "Cochapamba"
        result.latitude shouldBe original.latitude
        result.longitude shouldBe original.longitude
    }
    @Test
    fun parishLocalityUsesProviderNamesAndKeepsGps() {
        val original = Location(latitude = 1.123456789, longitude = 2.987654321)
        // Fields observed in a public Nominatim reverse response, not the user's GPS.
        val address = LocationAddressInfo(
            countryCode = "ec", locality = "Atucucho", quarter = "San José",
            cityDistrict = "Cochapamba", city = "Quito", admin2 = "Quito"
        )
        val result = original.toLocationWithAddressInfo(Locale.ROOT, address, false)
        result.cityAndDistrict shouldBe "Atucucho, Cochapamba, Quito"
        result.geographicName shouldBe result.cityAndDistrict
        result.latitude shouldBe original.latitude
        result.longitude shouldBe original.longitude
        result.weather shouldBe original.weather
        result.copy(customName = "Mi barrio").geographicName shouldBe "Mi barrio, Cochapamba, Quito"
        result.copy(customName = " ").geographicName shouldBe result.cityAndDistrict
        EcuadorAddressMapper.fromAddressInfo(address).neighborhood shouldBe null
    }

    @Test
    fun missingLocalityDoesNotInventNeighborhoodAndExplicitNeighborhoodWins() {
        val original = Location()
        val address = LocationAddressInfo(countryCode = "EC", cityDistrict = "Parroquia", city = "Ciudad")
        original.toLocationWithAddressInfo(Locale.ROOT, address, false).geographicName shouldBe "Parroquia, Ciudad"
        original.toLocationWithAddressInfo(
            Locale.ROOT, address.copy(neighborhood = "Barrio", locality = "Localidad", quarter = "Sector"), false
        ).geographicName shouldBe "Barrio, Parroquia, Ciudad"
        original.toLocationWithAddressInfo(
            Locale.ROOT, address.copy(countryCode = "FR", district = "Distrito", cityDistrict = null), false
        ).cityAndDistrict shouldBe "Ciudad, Distrito"
    }

}
