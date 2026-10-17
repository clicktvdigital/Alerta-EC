package org.breezyweather.background.location

import breezyweather.domain.location.model.LocationAddressInfo

object EcuadorAddressMapper {

    fun fromAddressInfo(address: LocationAddressInfo): DetailedLocationAddress {
        val isEcuador = address.countryCode.equals("EC", ignoreCase = true)

        // A city or ambiguous district is not evidence of a neighborhood.
        val neighborhood = firstNonBlank(address.neighborhood)
        val sector = firstNonBlank(address.quarter)

        val parish = if (isEcuador) {
            firstNonBlank(address.cityDistrict, address.admin3)
        } else {
            null
        }

        val canton = if (isEcuador) {
            firstNonBlank(
                address.admin2,
                address.admin3,
                address.city,
            )
        } else {
            firstNonBlank(address.city, address.admin2)
        }

        return DetailedLocationAddress(
            road = address.road,
            houseNumber = address.houseNumber,
            crossStreet = null,
            reference = address.reference,
            neighborhood = neighborhood,
            sector = sector,
            parish = parish,
            canton = canton,
            province = address.admin1,
            postalCode = address.postalCode,
            country = address.country,
            countryCode = address.countryCode,
        )
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }
}
