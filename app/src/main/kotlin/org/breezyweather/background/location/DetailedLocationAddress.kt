package org.breezyweather.background.location

data class DetailedLocationAddress(
    val road: String? = null,
    val houseNumber: String? = null,
    val crossStreet: String? = null,
    val reference: String? = null,
    val neighborhood: String? = null,
    val parish: String? = null,
    val canton: String? = null,
    val province: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
) {
    val streetLine: String?
        get() = listOfNotNull(road?.takeIf { it.isNotBlank() }, houseNumber?.takeIf { it.isNotBlank() }).takeIf { it.isNotEmpty() }?.joinToString(" ")

    val administrativeLine: String?
        get() = listOfNotNull(neighborhood, parish, canton, province).filter { it.isNotBlank() }.distinct().takeIf { it.isNotEmpty() }?.joinToString(", ")
}
