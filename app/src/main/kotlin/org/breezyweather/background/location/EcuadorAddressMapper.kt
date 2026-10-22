package org.breezyweather.background.location

import breezyweather.domain.location.model.LocationAddressInfo

object EcuadorAddressMapper {

    fun fromAddressInfo(
        address: LocationAddressInfo,
        postalCodeCandidate: PostalCodeCandidate? = null,
    ): DetailedLocationAddress {
        val isEcuador = address.countryCode.equals("EC", ignoreCase = true)

        // A city or ambiguous district is not evidence of a neighborhood.
        val neighborhood = firstNonBlank(address.neighborhood)
        val sector = firstNonBlank(address.quarter)
        val locality = firstNonBlank(address.locality)

        val parish = if (isEcuador) {
            firstNonBlank(address.admin3)
        } else {
            null
        }

        val canton = if (isEcuador) {
            firstNonBlank(
                address.admin2,
                address.city,
            )
        } else {
            firstNonBlank(address.city, address.admin2)
        }

        val base = DetailedLocationAddress(
            road = address.road,
            houseNumber = address.houseNumber,
            crossStreet = null,
            reference = address.reference,
            neighborhood = neighborhood,
            sector = sector,
            locality = locality,
            parish = parish,
            canton = canton,
            province = address.admin1,
            postalCode = address.postalCode?.takeIf(EcuadorPostalCodeAdapter::isValidCode),
            postalCodeConfidence = address.postalCode
                ?.takeIf(EcuadorPostalCodeAdapter::isValidCode)
                ?.let { PostalCodeConfidence.REVERSE_GEOCODER }
                ?: PostalCodeConfidence.NONE,
            postalCodeProvenance = address.postalCode
                ?.takeIf(EcuadorPostalCodeAdapter::isValidCode)
                ?.let { "reverse-geocoder" },
            country = address.country,
            countryCode = address.countryCode,
        )
        return postalCodeCandidate?.let(base::mergePostalCode) ?: base
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }
}
