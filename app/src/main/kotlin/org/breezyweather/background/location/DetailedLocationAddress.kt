package org.breezyweather.background.location

data class DetailedLocationAddress(
    val road: String? = null,
    val houseNumber: String? = null,
    val crossStreet: String? = null,
    val reference: String? = null,
    val neighborhood: String? = null,
    val sector: String? = null,
    /** Locality, village, community or hamlet returned by the geocoder. */
    val locality: String? = null,
    val parish: String? = null,
    val ecclesiasticalParish: String? = null,
    val educationalDistrict: String? = null,
    val administrativeZone: String? = null,
    val administrativeDistrict: String? = null,
    val canton: String? = null,
    val province: String? = null,
    val postalCode: String? = null,
    val postalCodeConfidence: PostalCodeConfidence = PostalCodeConfidence.NONE,
    val postalCodeProvenance: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
) {
    val streetLine: String?
        get() = listOfNotNull(
            road?.takeIf { it.isNotBlank() },
            houseNumber?.takeIf { it.isNotBlank() },
        ).takeIf { it.isNotEmpty() }?.joinToString(" ")

    val completeAddressLines: List<String>
        get() = buildList {
            streetLine?.takeIf { it.isNotBlank() }?.let(::add)
            crossStreet?.takeIf { it.isNotBlank() }?.let { add("Intersección: $it") }
            postalCode?.takeIf { it.isNotBlank() }?.let { add("Código postal: $it") }
            sector?.takeIf { it.isNotBlank() }?.let { add("Sector: $it") }
            neighborhood?.takeIf { it.isNotBlank() }?.let { add("Barrio: $it") }
            locality?.takeIf { it.isNotBlank() }?.let { add("Localidad/comunidad: $it") }
            ecclesiasticalParish?.takeIf { it.isNotBlank() }?.let { add("Parroquia eclesiástica: $it") }
            parish?.takeIf { it.isNotBlank() }?.let { add("Parroquia civil: $it") }
            educationalDistrict?.takeIf { it.isNotBlank() }?.let { add("Distrito educativo: $it") }
            administrativeZone?.takeIf { it.isNotBlank() }?.let { add("Administración zonal: $it") }
            administrativeDistrict?.takeIf { it.isNotBlank() }?.let { add("Distrito administrativo: $it") }
            canton?.takeIf { it.isNotBlank() }?.let { add("Cantón: $it") }
            province?.takeIf { it.isNotBlank() }?.let { add("Provincia: $it") }
            country?.takeIf { it.isNotBlank() }?.let { add("País: $it") }
            reference?.takeIf { it.isNotBlank() }?.let { add("Referencia: $it") }
        }

    fun mergePostalCode(candidate: PostalCodeCandidate): DetailedLocationAddress {
        if (!EcuadorPostalCodeAdapter.isValidCode(candidate.code) ||
            candidate.provenance.isBlank() || candidate.confidence == PostalCodeConfidence.NONE
        ) return this
        if (postalCode != null && candidate.confidence.rank <= postalCodeConfidence.rank) return this
        return copy(
            postalCode = candidate.code,
            postalCodeConfidence = candidate.confidence,
            postalCodeProvenance = candidate.provenance,
        )
    }

    val administrativeLine: String?
        get() = listOfNotNull(
            sector,
            neighborhood,
            locality,
            ecclesiasticalParish,
            parish,
            educationalDistrict,
            administrativeZone,
            administrativeDistrict,
            canton,
            province,
            country,
        ).filter { it.isNotBlank() }
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
}
