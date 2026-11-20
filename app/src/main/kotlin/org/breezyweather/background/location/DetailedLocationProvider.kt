package org.breezyweather.background.location

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Observable
import org.breezyweather.BreezyWeather
import org.breezyweather.sources.SourceManager
import javax.inject.Inject

class DetailedLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sourceManager: SourceManager,
) {

    // La corrección del usuario es LOCAL a un punto; no se aplica a todo Quito.
    // Nunca se presenta como dirección certificada por el geocodificador.
    private fun applyUserVerifiedZone(
        latitude: Double, longitude: Double, result: DetailedLocationAddress
    ): DetailedLocationAddress {
        val prefs = context.getSharedPreferences("alerta_ec_direccion_usuario", Context.MODE_PRIVATE)
        val storedLat = prefs.getString("lat", null)?.toDoubleOrNull() ?: return result
        val storedLon = prefs.getString("lon", null)?.toDoubleOrNull() ?: return result
        if (!storedLat.isFinite() || !storedLon.isFinite() ||
            !latitude.isFinite() || !longitude.isFinite()) return result
        val distance = FloatArray(1)
        android.location.Location.distanceBetween(storedLat, storedLon, latitude, longitude, distance)
        // 300 m (neighbourhood scale): on a TV the position comes from IP or typed coordinates and rarely falls
        // within 75 m of the saved point, so the geocoder's sector («San José») came back (2026-11-20).
        if (distance[0] > USER_ZONE_RADIUS_M) return result
        val postal = prefs.getString("postal", null)?.trim().orEmpty()
        val sector = prefs.getString("sector", null)?.trim().orEmpty()
        if (!Regex("[0-9]{6}").matches(postal) || sector.isBlank()) return result
        val cross = prefs.getString("cruce", null)?.trim()?.takeIf { it.isNotEmpty() }
        val localReference = prefs.getString("referencia", null)?.trim()?.takeIf { it.isNotEmpty() }
        val churchParish = prefs.getString("parroquia_eclesiastica", null)?.trim()?.takeIf { it.isNotEmpty() }
        val position = prefs.getString("posicion", null)?.trim()?.takeIf { it.isNotEmpty() }
        // The first two digits of an Ecuadorian postal code are the province (17 = Pichincha). A user code from
        // another province than the official zone of this point is a typo: keep the
        // official code, but still show the user's sector, cross street and local reference.
        val keepOfficial = result.postalCodeConfidence == PostalCodeConfidence.AUTHORIZED_OFFICIAL &&
            result.postalCode?.take(2) != postal.take(2)
        return result.copy(
            sector = sector,
            // The user's sector is the local truth: the geocoder neighbourhood (OSM «San José») is not shown with it.
            neighborhood = null,
            ecclesiasticalParish = churchParish ?: result.ecclesiasticalParish,
            polarReference = position ?: result.polarReference,
            crossStreet = cross ?: result.crossStreet,
            reference = localReference ?: result.reference,
            postalCode = if (keepOfficial) result.postalCode else postal,
            postalCodeProvenance = if (keepOfficial) {
                result.postalCodeProvenance
            } else {
                "Aportado por el usuario (ubicación cercana)"
            },
        )
    }

    fun request(
        latitude: Double,
        longitude: Double,
        sourceId: String = "nominatim",
    ): Observable<DetailedLocationAddress> {
        val source = sourceManager.getReverseGeocodingSourceOrDefault(sourceId)

        return source.requestNearestLocation(
            context = context,
            latitude = latitude,
            longitude = longitude,
        ).map { results ->
            results.firstOrNull()?.let(EcuadorAddressMapper::fromAddressInfo)
                ?: DetailedLocationAddress()
        }.map { address -> withOfficialPostalZone(latitude, longitude, address) }
            .map { address -> applyUserVerifiedZone(latitude, longitude, address) }
    }

    /**
     * In Ecuador, replaces the geocoder postcode with the official postal zone of the point (codigopostal.gob.ec)
     * and adds the planning district and the Spanish polar reference. If the official service is unreachable,
     * the geocoder postcode stays, marked with its lower confidence.
     */
    private fun withOfficialPostalZone(
        latitude: Double,
        longitude: Double,
        address: DetailedLocationAddress,
    ): DetailedLocationAddress {
        if (!EcuadorBounds.contains(latitude, longitude)) return address
        val polar = EcuadorPolarReference.describe(latitude, longitude)?.text
        val zone = EcuadorPostalZoneService.find(latitude, longitude, BreezyWeather.instance.userAgent)
            ?: return address.copy(polarReference = polar)
        return address
            .mergePostalCode(
                PostalCodeCandidate(
                    code = zone.postalCode,
                    provenance = EcuadorPostalZoneService.PROVENANCE,
                    confidence = PostalCodeConfidence.AUTHORIZED_OFFICIAL,
                )
            ).copy(
                canton = address.canton ?: zone.canton,
                province = address.province ?: zone.province,
                planningDistrict = zone.district,
                polarReference = polar,
            )
    }

    companion object {
        /** Distance within which the user's own zone correction applies (meters). */
        const val USER_ZONE_RADIUS_M = 300f
    }
}
