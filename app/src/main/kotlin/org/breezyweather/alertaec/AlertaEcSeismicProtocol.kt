package org.breezyweather.alertaec

/**
 * Extensible base for earthquake guidance: structured, data-driven advice by phase (before / during / after)
 * that scales with magnitude, distance and depth. No seismic feed is connected yet; a future source
 * (e.g. Instituto Geofísico - EPN) only has to build a [SeismicReport] and call [advise].
 * General guidance; it never replaces official instructions.
 */
internal object AlertaEcSeismicProtocol {
    enum class Phase { BEFORE, DURING, AFTER }

    enum class Category { PREPARATION, PROTECTION, SAFETY, INFORMATION }

    data class Advice(val phase: Phase, val category: Category, val text: String, val priority: Int)

    data class SeismicReport(
        val magnitude: Double,
        /** Distance from the user to the epicentre, km; null if unknown. */
        val distanceKm: Double?,
        /** Hypocentre depth, km; null if unknown. */
        val depthKm: Double?,
        /** True if the epicentre is at sea (tsunami check on the coast). */
        val offshore: Boolean = false,
        /** True if the user is in a coastal area. */
        val userOnCoast: Boolean = false,
    )

    /** How strongly the user is likely to feel it: 0 none, 1 light, 2 moderate, 3 strong. */
    fun feltLevel(report: SeismicReport): Int {
        if (!report.magnitude.isFinite() || report.magnitude <= 0.0) return 0
        val distance = report.distanceKm?.takeIf { it.isFinite() && it >= 0.0 } ?: return when {
            report.magnitude >= 6.5 -> 3
            report.magnitude >= 5.0 -> 2
            else -> 1
        }
        // Shallow quakes are felt more; deep ones less.
        val depth = report.depthKm?.takeIf { it.isFinite() && it >= 0.0 } ?: 30.0
        val effectiveKm = kotlin.math.hypot(distance, depth)
        val score = report.magnitude - kotlin.math.log10(effectiveKm.coerceAtLeast(1.0)) * 1.5
        return when {
            score >= 4.0 -> 3
            score >= 2.8 -> 2
            score >= 1.5 -> 1
            else -> 0
        }
    }

    /** Always-on preparation advice (no event needed). */
    val preparation = listOf(
        Advice(Phase.BEFORE, Category.PREPARATION,
            "Prepara una mochila de emergencia: agua, alimentos no perecibles, botiquín, linterna, radio, " +
                "documentos en bolsa plástica y cargador.", 1),
        Advice(Phase.BEFORE, Category.PREPARATION,
            "Identifica en casa los lugares seguros (junto a columnas o bajo mesas firmes) y las rutas de salida.", 2),
        Advice(Phase.BEFORE, Category.PREPARATION,
            "Fija a la pared muebles altos y aleja objetos pesados de camas y sillones.", 3),
        Advice(Phase.BEFORE, Category.INFORMATION,
            "Acuerda con tu familia un punto de encuentro y un contacto fuera de la ciudad.", 4),
    )

    /** Advice for a reported earthquake, adapted to how strongly it is likely felt. */
    fun advise(report: SeismicReport): List<Advice> {
        val level = feltLevel(report)
        val out = mutableListOf<Advice>()
        if (level >= 1) {
            out += Advice(Phase.DURING, Category.PROTECTION,
                "Agáchate, cúbrete y sujétate. Aléjate de ventanas, vidrios y objetos que puedan caer.", 1)
            out += Advice(Phase.DURING, Category.PROTECTION,
                "No uses ascensores ni corras hacia escaleras mientras tiembla.", 2)
        }
        if (level >= 2) {
            out += Advice(Phase.AFTER, Category.SAFETY,
                "Al terminar, revisa si hay heridos, cierra el gas y la llave de paso; no enciendas fósforos.", 3)
            out += Advice(Phase.AFTER, Category.SAFETY,
                "Sal con calma por rutas seguras y aléjate de edificios dañados, postes y cables.", 4)
            out += Advice(Phase.AFTER, Category.INFORMATION,
                "Espera réplicas. Usa mensajes de texto en vez de llamadas para no saturar las líneas.", 5)
        }
        if (level >= 3) {
            out += Advice(Phase.AFTER, Category.SAFETY,
                "No regreses a estructuras con grietas o daños hasta que las autoridades lo indiquen.", 6)
        }
        if (report.offshore && report.userOnCoast && report.magnitude >= 6.5) {
            out += Advice(Phase.AFTER, Category.SAFETY,
                "Sismo fuerte en el mar: aléjate de la playa y sube a zonas altas siguiendo rutas de evacuación; " +
                    "no esperes a ver el mar retirarse.", 0)
        }
        out += Advice(Phase.AFTER, Category.INFORMATION,
            "Sigue solo la información oficial (Instituto Geofísico, Secretaría de Gestión de Riesgos, ECU 911).", 9)
        return out.sortedBy { it.priority }
    }
}
