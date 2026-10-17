package org.breezyweather.alertaec

/**
 * Origen y nivel de verificacion de una noticia.
 * Un reporte ciudadano no equivale a un comunicado oficial.
 */
enum class AlertaEcNewsVerification {
    OFFICIAL,
    VERIFIED,
    UNVERIFIED
}

data class AlertaEcNewsItem(
    val id: String,
    val title: String,
    val summary: String,
    val sourceName: String,
    val sourceUrl: String,
    val publishedAtMillis: Long,
    val occurredAtMillis: Long? = null,
    val updatedAtMillis: Long? = null,
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val verification: AlertaEcNewsVerification =
        AlertaEcNewsVerification.UNVERIFIED
)

/**
 * Orden cronologico: primero las publicaciones mas recientes.
 * No prioriza popularidad, reproducciones ni interacciones.
 */
object AlertaEcNewsFeed {
    fun mostRecent(items: List<AlertaEcNewsItem>): List<AlertaEcNewsItem> =
        items.distinctBy { it.id }
            .sortedWith(
                compareByDescending<AlertaEcNewsItem> { it.publishedAtMillis }
                    .thenBy { it.id }
            )
}
