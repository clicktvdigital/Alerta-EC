package org.breezyweather.background.location

import org.breezyweather.alertaec.AlertaEcMovementDirection
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Polar position of a point in Spanish: distance and one of the 8 compass directions (Norte, Nororiente,
 * Oriente, Suroriente, Sur, Suroccidente, Occidente, Noroccidente) from the nearest provincial capital of
 * Ecuador, e.g. "10,1 km al Norte de Quito (centro)". Works offline; only for points inside Ecuador.
 */
object EcuadorPolarReference {
    data class Capital(val name: String, val latitude: Double, val longitude: Double)

    /** Historic centre of each of the 24 provincial capitals (public reference points, ~100 m precision). */
    val capitals = listOf(
        Capital("Cuenca", -2.8974, -79.0045),
        Capital("Guaranda", -1.5926, -79.0010),
        Capital("Azogues", -2.7397, -78.8486),
        Capital("Tulcán", 0.8117, -77.7173),
        Capital("Riobamba", -1.6710, -78.6483),
        Capital("Latacunga", -0.9352, -78.6155),
        Capital("Machala", -3.2581, -79.9554),
        Capital("Esmeraldas", 0.9682, -79.6517),
        Capital("Puerto Baquerizo Moreno", -0.9017, -89.6103),
        Capital("Guayaquil", -2.1962, -79.8862),
        Capital("Ibarra", 0.3517, -78.1223),
        Capital("Loja", -3.9931, -79.2042),
        Capital("Babahoyo", -1.8022, -79.5344),
        Capital("Portoviejo", -1.0546, -80.4545),
        Capital("Macas", -2.3087, -78.1114),
        Capital("Tena", -0.9938, -77.8129),
        Capital("Puerto Francisco de Orellana", -0.4625, -76.9930),
        Capital("Puyo", -1.4924, -77.9993),
        Capital("Quito", -0.2201, -78.5123),
        Capital("Santa Elena", -2.2262, -80.8585),
        Capital("Santo Domingo", -0.2530, -79.1754),
        Capital("Nueva Loja", 0.0847, -76.8828),
        Capital("Ambato", -1.2491, -78.6168),
        Capital("Zamora", -4.0692, -78.9567),
    )

    data class Polar(val capital: Capital, val distanceKm: Double, val direction: String?) {
        /** "10,1 km al Norte de Quito (centro)"; "En el centro de Quito" when closer than 300 m. */
        val text: String
            get() = if (distanceKm < 0.3 || direction == null) {
                "En el centro de ${capital.name}"
            } else {
                String.format(Locale.forLanguageTag("es-EC"), "%.1f km al %s de %s (centro)", distanceKm, direction, capital.name)
            }
    }

    fun describe(latitude: Double, longitude: Double): Polar? {
        if (!EcuadorBounds.contains(latitude, longitude)) return null
        val nearest = capitals.minBy { distanceKm(it.latitude, it.longitude, latitude, longitude) }
        return Polar(
            capital = nearest,
            distanceKm = distanceKm(nearest.latitude, nearest.longitude, latitude, longitude),
            direction = AlertaEcMovementDirection.between(nearest.latitude, nearest.longitude, latitude, longitude),
        )
    }

    /** Great-circle distance (haversine, mean Earth radius). */
    internal fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * 6371.0088 * asin(sqrt(a))
    }
}
