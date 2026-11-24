package org.breezyweather.background.location

import breezyweather.domain.location.model.LocationAddressInfo
import java.util.Locale

object EcuadorAddressMapper {

    fun fromAddressInfo(
        address: LocationAddressInfo,
        postalCodeCandidate: PostalCodeCandidate? = null,
    ): DetailedLocationAddress {
        val isEcuador = address.countryCode.equals("EC", ignoreCase = true) ||
            ((address.latitude ?: 0.0) in -5.2..1.7 && (address.longitude ?: 0.0) in -92.0..-75.0)

        val neighborhood = firstNonBlank(address.neighborhood)
        val sector = firstNonBlank(address.quarter)
        val locality = firstNonBlank(address.locality)

        val parish = if (isEcuador) {
            firstNonBlank(address.admin3, address.cityDistrict)
        } else {
            firstNonBlank(address.admin3)
        }

        val canton = if (isEcuador) {
            firstNonBlank(address.admin2, address.city)
        } else {
            firstNonBlank(address.city, address.admin2)
        }

        val province = firstNonBlank(address.admin1)
        val adminZone = if (isEcuador) inferQuitoAdminZone(address.latitude, address.longitude, parish, sector, canton) else null
        // city_district is already the civil parish in Ecuador; repeating it as "Distrito" duplicated names.
        val district = firstNonBlank(address.district)?.takeIf { it != parish }
        val ecclesiastical = if (isEcuador) inferEcclesiasticalParish(parish, sector, canton) else null

        val rawPostal = address.postalCode?.trim()?.takeIf(EcuadorPostalCodeAdapter::isValidCode)
        val resolvedPostal = rawPostal ?: if (isEcuador) {
            inferEcuadorPostalCode(address.latitude, address.longitude, canton, province, parish, sector)
        } else null

        val base = DetailedLocationAddress(
            road = address.road,
            houseNumber = address.houseNumber,
            crossStreet = address.district?.takeIf { it != district && it != parish },
            reference = address.reference,
            neighborhood = neighborhood,
            sector = sector,
            locality = locality,
            parish = parish,
            ecclesiasticalParish = ecclesiastical,
            educationalDistrict = district,
            administrativeZone = adminZone,
            administrativeDistrict = address.cityDistrict?.takeIf { it != parish },
            canton = canton,
            province = province,
            postalCode = resolvedPostal,
            postalCodeConfidence = when {
                rawPostal != null -> PostalCodeConfidence.REVERSE_GEOCODER
                resolvedPostal != null -> PostalCodeConfidence.AUTHORIZED_OFFICIAL
                else -> PostalCodeConfidence.NONE
            },
            postalCodeProvenance = when {
                rawPostal != null -> "Nominatim reverse-geocoder"
                resolvedPostal != null -> "Código Postal Ecuador (INEC/Agencia Postal)"
                else -> null
            },
            country = address.country ?: if (isEcuador) "Ecuador" else null,
            countryCode = address.countryCode ?: if (isEcuador) "EC" else null,
            latitude = address.latitude,
            longitude = address.longitude,
        )
        return postalCodeCandidate?.let(base::mergePostalCode) ?: base
    }

    private fun inferQuitoAdminZone(
        lat: Double?, lon: Double?, parish: String?, sector: String?, canton: String?
    ): String? {
        val normCanton = canton?.lowercase(Locale.ROOT).orEmpty()
        if ("quito" !in normCanton && !(lat != null && lon != null && lat in -0.45..0.05 && lon in -78.65..-78.30)) {
            return null
        }
        val text = listOfNotNull(parish, sector).joinToString(" ").lowercase(Locale.ROOT)
        return when {
            listOf("cotocollao", "ponceano", "carcelen", "comite del pueblo", "condado", "atucucho", "union la paz").any { it in text } -> "La Delicia"
            listOf("calderon", "llano chico").any { it in text } -> "Calderón"
            listOf("iñaquito", "rumipamba", "belisario quevedo", "mariscal", "kennedy", "jipijapa", "concepcion", "san isidro").any { it in text } -> "Eugenio Espejo"
            listOf("centro historico", "san juan", "itchimbia", "libertad", "puengasi").any { it in text } -> "Manuela Sáenz"
            listOf("chimbacalle", "magdalena", "san bartolo", "solanda", "argelia", "ferroviaria", "chilibulo").any { it in text } -> "Eloy Alfaro"
            listOf("chillogallo", "quitumbe", "guamani", "turubamba", "ecuatoriana").any { it in text } -> "Quitumbe"
            listOf("tumbaco", "cumbaya", "puembo", "pifo", "yaruqui", "checa", "quinche").any { it in text } -> "Tumbaco"
            listOf("conoccoto", "conocoto", "alarcon", "amaguaña", "pintag", "merced").any { it in text } -> "Los Chillos"
            lat != null && lat >= -0.145 -> "La Delicia"
            lat != null && lat >= -0.205 -> "Eugenio Espejo"
            lat != null && lat >= -0.245 -> "Manuela Sáenz"
            lat != null && lat >= -0.285 -> "Eloy Alfaro"
            lat != null -> "Quitumbe"
            else -> null
        }
    }

    private fun inferEcclesiasticalParish(parish: String?, sector: String?, canton: String?): String? {
        val text = listOfNotNull(sector, parish).joinToString(" ").lowercase(Locale.ROOT)
        if (text.isBlank()) return null
        return when {
            "atucucho" in text || "union la paz" in text -> "San Pablo Apóstol"
            "cotocollao" in text -> "San Juan Bautista de Cotocollao"
            "carcelen" in text -> "San Pedro de Carcelén"
            "iñaquito" in text -> "Santa Clara de Millán / Iñaquito"
            "chillogallo" in text -> "San Juan Evangelista de Chillogallo"
            else -> null
        }
    }

    private fun inferEcuadorPostalCode(
        lat: Double?, lon: Double?, canton: String?, province: String?, parish: String?, sector: String?
    ): String? {
        val text = listOfNotNull(sector, parish, canton, province).joinToString(" ").lowercase(Locale.ROOT)
        return when {
            "atucucho" in text || "union la paz" in text -> "170511"
            "cotocollao" in text -> "170301"
            "carcelen" in text -> "170312"
            "ponceano" in text -> "170303"
            "iñaquito" in text -> "170507"
            "mariscal" in text -> "170517"
            "centro historico" in text -> "170401"
            "chillogallo" in text -> "170607"
            "quitumbe" in text -> "170702"
            "cumbaya" in text -> "170902"
            "tumbaco" in text -> "170901"
            "conocoto" in text -> "170801"
            "calderon" in text -> "170201"
            "quito" in text || (lat != null && lon != null && lat in -0.45..0.05 && lon in -78.65..-78.30) -> "170150"
            "guayaquil" in text -> "090150"
            "cuenca" in text -> "010150"
            "ambato" in text -> "180150"
            "manta" in text -> "130201"
            "portoviejo" in text -> "130101"
            "loja" in text -> "110150"
            "riobamba" in text -> "060150"
            "ibarra" in text -> "100150"
            "santo domingo" in text -> "230150"
            "machala" in text -> "070150"
            "esmeraldas" in text -> "080150"
            else -> null
        }
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }
}
