package org.breezyweather.background.location

import org.breezyweather.sources.nominatim.json.NominatimAddress

object EcuadorAddressMapper {

    fun fromNominatim(address: NominatimAddress): DetailedLocationAddress {
        val isEcuador = address.countryCode.equals("ec", ignoreCase = true)

        val neighborhood = firstNonBlank(
            address.neighbourhood,
            address.quarter,
            address.suburb,
            address.hamlet,
        )

        val canton = if (isEcuador) {
            firstNonBlank(
                address.city,
                address.municipality,
                address.county,
            )
        } else {
            firstNonBlank(address.city, address.town, address.county)
        }

        return DetailedLocationAddress(
            road = address.road,
            houseNumber = address.houseNumber,
            reference = address.amenity,
            neighborhood = neighborhood,
            parish = null,
            canton = canton,
            province = address.state,
            postalCode = address.postcode,
            country = address.country,
            countryCode = address.countryCode,
        )
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }
}
