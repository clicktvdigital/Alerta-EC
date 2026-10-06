package org.breezyweather.background.location

import breezyweather.domain.location.model.LocationAddressInfo

object EcuadorAddressMapper {

    fun fromAddressInfo(address: LocationAddressInfo): DetailedLocationAddress {
        val isEcuador = address.countryCode.equals("EC", ignoreCase = true)

        val neighborhood = firstNonBlank(
            address.neighborhood,
            address.quarter,
            address.district,
        )

        val canton = if (isEcuador) {
            firstNonBlank(
                address.city,
                address.admin3,
                address.admin2,
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
            parish = null,
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
