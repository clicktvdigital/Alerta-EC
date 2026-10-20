package org.breezyweather.alertaec.support

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.os.Build
import org.breezyweather.common.extensions.hasNotificationPermission
import org.breezyweather.common.extensions.hasPermission
import org.breezyweather.common.extensions.isOnline

/**
 * Diagnosticos locales sin cambios de permisos ni datos del usuario.
 * No ejecuta reparaciones ni envia informacion fuera del dispositivo.
 */
internal object AutoSupportDiagnostics {

    fun run(context: Context): List<AutoSupportCheck> {
        val appContext = context.applicationContext
        return listOf(
            check(
                id = "internet",
                description = "Conexion a Internet",
                ok = appContext.isOnline(),
                success = "Android confirma una conexion a Internet validada.",
                failure = "No se detecta una conexion validada. No se puede activar Wi-Fi o datos moviles sin intervencion del usuario.",
            ),
            check(
                id = "location",
                description = "Permiso de ubicacion",
                ok = appContext.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                    appContext.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION),
                success = "La aplicacion dispone de permiso de ubicacion.",
                failure = "Falta el permiso de ubicacion. Android requiere autorizacion del usuario.",
            ),
            check(
                id = "gps_enabled",
                description = "Servicios de ubicacion",
                ok = locationServicesEnabled(appContext),
                success = "Los servicios de ubicacion del dispositivo estan activados.",
                failure = "Los servicios de ubicacion estan desactivados o no se pueden verificar. Android requiere revisar sus ajustes.",
            ),
            check(
                id = "notifications",
                description = "Permiso de notificaciones",
                ok = appContext.hasNotificationPermission,
                success = "La aplicacion dispone de permiso para notificaciones.",
                failure = "Las notificaciones requieren autorizacion o revision de ajustes del sistema.",
            ),
        )
    }

    private fun locationServicesEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                manager.isLocationEnabled
            } else {
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }
        } catch (_: SecurityException) {
            false
        }
    }

    private fun check(
        id: String,
        description: String,
        ok: Boolean,
        success: String,
        failure: String,
    ) = AutoSupportCheck(
        id = id,
        description = description,
        result = if (ok) AutoSupportResult.CORRECTO else AutoSupportResult.PENDIENTE,
        detail = if (ok) success else failure,
    )
}
