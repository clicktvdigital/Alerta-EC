package org.breezyweather.alertaec.support

/**
 * Ayuda disponible sin Internet, cuentas ni recopilacion de datos.
 */
internal object AutoSupportFaq {

    internal data class Entry(
        val question: String,
        val answer: String,
    )

    val entries: List<Entry> = listOf(
        Entry(
            "¿Por qué no recibo notificaciones?",
            "Compruebe el permiso de notificaciones, las preferencias de ALERTA EC y las restricciones de bateria de Android. La entrega tambien depende de la disponibilidad de datos y de la ejecucion en segundo plano.",
        ),
        Entry(
            "¿Por qué el pronostico no coincide con el tiempo que observo?",
            "Un pronostico es una estimacion, no una medicion exacta de su calle. Compruebe la ubicacion, la fuente y la fecha de actualizacion. La precision GPS no equivale a la resolucion del modelo meteorologico.",
        ),
        Entry(
            "¿Qué hago si aparece actualizando datos meteorologicos durante mucho tiempo?",
            "Ejecute el autosoporte para comprobar Internet, fechas de adquisicion y trabajos meteorologicos pendientes. Si una actualizacion sigue activa, no se iniciara otra automaticamente.",
        ),
        Entry(
            "¿Por qué no aparece mi barrio?",
            "El nombre depende de la cartografia disponible. Puede establecer una etiqueta manual para reconocer la ubicacion, pero esa etiqueta no certifica el barrio ni cambia las coordenadas GPS.",
        ),
        Entry(
            "¿ALERTA EC funciona sin Internet?",
            "La ayuda local puede consultarse sin conexion. Los nuevos pronosticos, mapas remotos y avisos de proveedores necesitan conectividad y disponibilidad de sus fuentes.",
        ),
        Entry(
            "¿ALERTA EC sustituye al ECU 911?",
            "No. ALERTA EC es una aplicacion independiente y no representa oficialmente al ECU 911. Ante una emergencia real en Ecuador, contacte directamente al ECU 911 mediante el 911 cuando sea posible.",
        ),
        Entry(
            "¿Puedo enviar una sugerencia o informar de un problema?",
            "Si. El envio de informes debe ser voluntario y mostrar el contenido antes de compartirlo. No incluya contraseñas, direcciones privadas ni datos sensibles.",
        ),
    )
}
