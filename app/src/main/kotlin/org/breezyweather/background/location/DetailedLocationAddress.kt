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
    val ecclesiasticalJurisdiction: String? = null,
    val educationalDistrict: String? = null,
    val healthDistrict: String? = null,
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
            val seen = mutableSetOf<String>()
            fun addPart(label: String, value: String?) {
                val clean = value?.trim()?.takeIf { it.isNotEmpty() } ?: return
                if (seen.add(clean.lowercase(java.util.Locale.ROOT))) {
                    add(if (label.isEmpty()) clean else "$label: $clean")
                }
            }
            addPart("", streetLine)
            addPart("Calle secundaria/transversal", crossStreet)
            addPart("Código postal", postalCode)
            addPart("Sector", sector)
            addPart("Barrio", neighborhood)
            addPart("Localidad/comunidad", locality)
            addPart("Parroquia eclesiástica", ecclesiasticalParish)
            addPart("Jurisdicción eclesiástica", ecclesiasticalJurisdiction)
            addPart("Distrito educativo", educationalDistrict)
            addPart("Distrito de salud", healthDistrict)
            addPart("Parroquia civil", parish)
            addPart("Administración zonal", administrativeZone)
            addPart("Distrito administrativo", administrativeDistrict)
            addPart("Cantón", canton)
            addPart("Provincia", province)
            addPart("País", country)
            addPart("Referencia", reference)
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
            ecclesiasticalJurisdiction,
            educationalDistrict,
            healthDistrict,
            parish,
            administrativeZone,
            administrativeDistrict,
            canton,
            province,
            country,
        ).map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase(java.util.Locale.ROOT) }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
}
