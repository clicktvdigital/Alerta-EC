package org.breezyweather.background.location

import breezyweather.domain.location.model.LocationAddressInfo

object EcuadorAddressMapper {

    fun fromAddressInfo(address: LocationAddressInfo): DetailedLocationAddress {
        val isEcuador = address.countryCode.equals("EC", ignoreCase = true)

        val localCity = address.city?.takeUnless {
            isEcuador && it.equals(address.admin2, ignoreCase = true)
        }

        val neighborhood = if (isEcuador) {
            firstNonBlank(
                address.neighborhood,
                localCity,
                address.quarter,
                address.district,
            )
        } else {
            firstNonBlank(
                address.neighborhood,
                address.quarter,
                address.district,
            )
        }

        val sector = if (isEcuador) {
            firstNonBlank(address.quarter, address.district)
        } else {
            address.quarter
        }

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
