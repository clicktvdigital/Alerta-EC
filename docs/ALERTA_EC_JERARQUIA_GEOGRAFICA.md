# Jerarquía geográfica de ALERTA EC

Revisión del 17 de octubre de 2026. Se conservaron los cambios locales previos.

## Evidencia del proveedor

Se consultaron búsqueda y geocodificación inversa de Nominatim con un punto público del lugar, no con coordenadas privadas del usuario:
https://nominatim.openstreetmap.org/reverse?lat=-0.1289647&lon=-78.5136987&format=jsonv2&addressdetails=1&zoom=18

Campos observados: `village=Atucucho`, `city_district=Cochapamba`, `county=Quito`, `quarter=San José`, `state=Pichincha`, `country_code=ec`. No apareció `neighbourhood`. Datos © OpenStreetMap contributors, ODbL 1.0, https://osm.org/copyright.

## Comportamiento

Para Ecuador, cuando el proveedor identifica parroquia y cantón, `village` se conserva como localidad subordinada, no como barrio confirmado ni ciudad. Ciudad: `city`, luego `town`, luego `county`. Se prioriza un `neighbourhood` explícito sobre la localidad y el sector. La parroquia se conserva en `admin4`, campo persistente existente; no se necesita migración de base de datos. El formato común es barrio/localidad, parroquia, ciudad, omitiendo blancos y duplicados. En la respuesta observada produce Atucucho, Cochapamba, Quito. No hay nombres geográficos fijos en producción.

La etiqueta personalizada existente sustituye únicamente el primer componente visible y conserva parroquia y ciudad. Puede borrarse para recuperar el nombre del proveedor. La ayuda existente advierte que es manual y debe revisarse al desplazarse. No modifica GPS ni meteorología. Pantalla principal, notificaciones meteorológicas y alertas comparten `geographicName`. Las ubicaciones ya guardadas recuperarán la parroquia en su siguiente geocodificación.

No se cambiaron permisos, registros, caché, frecuencia de consultas, precisión GPS, versiones, firma ni secretos. No se usó el GPS privado para la comprobación externa.

## Verificación

`git diff --check`: correcto. Pruebas de regresión añadidas en `LocationAddressPriorityTest`: jerarquía observada, prioridad de barrio explícito, datos ausentes, etiqueta manual, coordenadas y meteorología conservadas, formato fuera de Ecuador. La ejecución offline de `:app:testBasicDebugUnitTest` no llegó a compilar: falta el plugin `org.gradle.kotlin.kotlin-dsl:6.7.3` en caché. El segundo intento con conexión se interrumpió durante la resolución de dependencias, sin llegar a ejecutar pruebas. Pendientes compilación, ejecución de pruebas y validación Android visual.

## Archivos modificados en esta revisión

- `app/src/main/kotlin/org/breezyweather/sources/nominatim/NominatimService.kt`
- `domain/src/main/kotlin/breezyweather/domain/location/model/LocationAddressInfo.kt`
- `domain/src/main/kotlin/breezyweather/domain/location/model/Location.kt`
- `app/src/main/kotlin/org/breezyweather/domain/location/model/Location.kt`
- `app/src/main/kotlin/org/breezyweather/alertaec/AlertaEcWeatherAdapter.kt`
- `app/src/main/kotlin/org/breezyweather/remoteviews/presenters/notification/NotificationForecastSummary.kt`
- `app/src/test/kotlin/org/breezyweather/background/location/LocationAddressPriorityTest.kt`
- `docs/ALERTA_EC_JERARQUIA_GEOGRAFICA.md`

## Requisito obligatorio: distrito educativo

ALERTA EC debe incluir el distrito educativo y su codigo oficial en la ficha territorial, despues de la administracion zonal municipal. La identificacion debe realizarse mediante fuentes oficiales del Ministerio de Educacion y la ubicacion GPS real. Si no puede verificarse, mostrar Distrito educativo: pendiente de verificar. No asignar codigos por aproximacion ni fijar los datos de Quito para todo Ecuador. Mantener Nominatim, GPS, pronosticos, notificaciones y demas mejoras sin sustituciones. Este requisito debe implementarse y probarse antes de publicar la siguiente version estable.
