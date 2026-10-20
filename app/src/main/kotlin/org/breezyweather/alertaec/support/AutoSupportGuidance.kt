package org.breezyweather.alertaec.support

/**
 * Recomendaciones locales. Ninguna instruccion implica que Android
 * permita reparar permisos o configuraciones sin autorizacion.
 */
internal object AutoSupportGuidance {

    fun recommendation(check: AutoSupportCheck): String? {
        if (check.result == AutoSupportResult.CORRECTO ||
            check.result == AutoSupportResult.REPARADO
        ) return null

        return when (check.id) {
            "internet" ->
                "Compruebe Wi-Fi o datos moviles y vuelva a ejecutar el autosoporte. No es necesario desactivar IPv6."
            "location" ->
                "Abra los ajustes de ALERTA EC y autorice la ubicacion. Android exige su consentimiento."
            "gps_enabled" ->
                "Active los servicios de ubicacion en los ajustes de Android y compruebe la precision disponible."
            "notifications" ->
                "Revise el permiso de notificaciones de ALERTA EC en los ajustes de Android."
            "weather_location" ->
                "Configure una ubicacion principal para consultar el pronostico."
            "weather_data", "weather_current", "weather_daily", "weather_hourly" ->
                "Compruebe la conexion y la fuente meteorologica. Si el problema continua, repita el diagnostico mas tarde."
            "weather_update_queue" ->
                "Espere a que termine la actualizacion existente. El autosoporte evita iniciar trabajos duplicados."
            "weather_partial_recovery" ->
                "Algunas secciones se actualizaron y otras no. Revise sus fechas de adquisicion y repita el diagnostico posteriormente."
            else ->
                "El problema sigue pendiente. Puede describirlo en el informe voluntario de soporte."
        }
    }
}
