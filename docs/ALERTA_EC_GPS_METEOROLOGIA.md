# GPS, nombres de ubicación y meteorología — revisión 2026-10-17

Estado: cambios locales de código y revisión documental; sin compilación, sin ejecución Kotlin ni prueba en un teléfono. Se conservaron los cambios locales anteriores. No se modificaron versión, applicationId, firma ni secretos en esta revisión. No se ejecutaron commit, push, CI ni Release.

## Posición utilizada para el clima

- `AndroidLocationService` solicita alta precisión cuando existe permiso preciso; con permiso aproximado usa calidad equilibrada. Prefiere el proveedor fusionado habilitado, luego GPS (solo con permiso preciso), luego red. Android puede entregar una posición aproximada; no se promete una precisión determinada.
- Cada solicitud usa su propio listener y lo retira al recibir resultado, cancelar o agotar los 15 segundos. La última posición conocida solo se acepta con antigüedad monotónica máxima de 120 segundos. Se selecciona la más reciente entre los proveedores permitidos. No se abre un seguimiento permanente para actualizar el tiempo.
- Se conserva `Location.time` como hora de medición, además de precisión horizontal y altitud opcionales. No se sustituye por la hora de consulta. La hora procede de Android; no se certifica un reloj perfecto. La altitud conserva la referencia del proveedor (habitualmente elipsoidal en GPS), no se presume altura sobre el nivel del mar.
- `CurrentLocationStore` conserva las coordenadas como `Double` serializado sin redondeo y lee las antiguas preferencias `Float` como compatibilidad. Una nueva lectura recupera la precisión perdida anteriormente. No se inventan metadatos para registros antiguos o fuentes IP.
- El permiso de ubicación sigue siendo obligatorio. La adquisición nativa no depende de conectividad; clima y geocodificación remota sí la necesitan. El servicio de seguimiento opcional comprueba permisos antes de iniciar su notificación y selecciona proveedores habilitados según el permiso concedido.
- `RefreshHelper` conserva las coordenadas medidas también cuando falla la geocodificación. La conversión de dirección mantiene `overwriteCoordinates=false`; Open-Meteo recibe `location.latitude` y `location.longitude`. El centro del objeto OSM no se convierte en el punto de pronóstico.
- Un fallo del GPS puede dejar la última ubicación guardada en uso. Su hora se muestra sin rejuvenecerla. Algunas fuentes pueden fallar si no hay país válido después de un fallo de geocodificación; no se recupera un punto antiguo para ocultarlo.

En las preferencias de la ubicación actual se muestran coordenadas del clima, precisión estimada en metros, altitud disponible, hora UTC con segundos/fracción, antigüedad al abrir y proveedor. Los metadatos solo se asocian si corresponden exactamente al punto guardado; en caso contrario se informa su ausencia. La antigüedad se calcula al dibujar la vista, no es un reloj continuo. La resolución meteorológica se identifica como no informada y dependiente del modelo: una precisión de ubicación de ±8 m no significa que la lluvia se resuelva en 8 m.

## Cochapamba y Atucucho

Antecedente encontrado en `CONTINUAR_AQUI.md`: una consulta anterior describía `village=Atucucho`, `quarter=San Jose`, `city_district=Cochapamba`, `county=Quito`, `state=Pichincha`. No se dispone aquí de la respuesta original completa ni se repitió una consulta con coordenadas privadas. Es un antecedente documental, no una confirmación del resultado actual.

El convertidor conserva `village/town/city` como localidad (`city`), `neighbourhood` como barrio, `quarter` como sector y `city_district` como distrito/parroquia. La prioridad existente de barrio explícito → sector → distrito se mantiene. Si solo llega Cochapamba en el distrito, puede aparecer ese nombre; si Atucucho llega en `village`, se conserva como localidad, **no se convierte automáticamente en barrio confirmado**. Android Geocoder y Natural Earth pueden devolver niveles más amplios. Un nombre guardado requiere refrescar geocodificación para reflejar cambios del proveedor.

Nominatim busca un objeto OSM próximo adecuado, no calcula una dirección exacta garantizada del punto; los campos dependen de la cartografía. Fuente: [documentación de reverse](https://nominatim.org/release-docs/latest/api/Reverse/). Una tolerancia existente de 50 km en la validación rechaza resultados muy lejanos, pero no certifica un barrio. No se agregó un polígono ni una regla «Cochapamba → Atucucho».

Para cerrar el diagnóstico en Android: comprobar fuente seleccionada, precisión/antigüedad, punto realmente enviado, campos devueltos y título tras refrescar. Comparar nombres en una captura local anonimizada; no subir domicilio, coordenadas ni respuesta íntegra a GitHub. Los tests usan puntos sintéticos, no el domicilio de referencia.

La etiqueta personalizada está accesible también para la ubicación actual aunque no haya preferencias adicionales de la fuente. Solo modifica `customName`: no altera GPS, consulta meteorológica ni dirección del proveedor. Es una etiqueta del usuario y puede quedar desactualizada al moverse.

## Nominatim: privacidad y límites

Se conserva User-Agent identificable, atribución OpenStreetMap/ODbL y la URL configurable. El cliente lee la instancia en cada solicitud, por lo que cambiarla no necesita reiniciar. La consulta inversa pide detalle `zoom=18`; eso no garantiza barrio ni mejora el GPS.

Se elimina la interfaz de búsqueda de Nominatim para no exponer autocompletado. Las consultas inversas se serializan entre instancias del servicio dentro del proceso; se guarda una caché exacta de hasta 32 resultados por coordenadas, idioma e instancia, durante un máximo de 24 horas en memoria. No se redondea el punto para aprovechar caché de otra calle. Las entradas no se escriben a disco y desaparecen al terminar el proceso.

Las solicitudes no cacheadas tienen un intervalo mínimo de 60 segundos; las llamadas demasiado frecuentes fallan sin quedar en cola y el refresco puede usar su fallback. También se limita tras errores. HTTP 429/503 aplican espera mínima de 15 minutos y `Retry-After` (segundos o fecha); HTTP 403 aplica al menos 24 horas. Estos controles y la caché se reinician con el proceso: no constituyen un coordinador global ni una garantía de disponibilidad.

**La [política pública de Nominatim](https://operations.osmfoundation.org/policies/nominatim/) exige máximo 1 solicitud/s agregado por aplicación, identificación, atribución y caché; prohíbe autocompletado y consultas sistemáticas y desaconseja consultas periódicas.** El límite local no garantiza el agregado de todos los teléfonos. Para distribución amplia se necesita una instancia/proveedor adecuado o proxy que controle tráfico global y retención. No se desplegó infraestructura ni se habilitó una fuente nueva. El seguimiento GPS opcional no llama directamente a Nominatim.

Las consultas remotas transmiten coordenadas al proveedor configurado; la etiqueta manual no se incluye. No se añadieron registros de coordenadas, telemetría, envío a contactos ni permisos. Las preferencias del GPS usan almacenamiento privado existente. La lectura guardada se borra mediante `clearCurrentLocation`; la caché inversa temporal tiene vida independiente dentro del proceso. No se consultó la posición real del dispositivo desde Termux.

## Fuentes y resolución para Ecuador

El código permite cambiar fuentes por ubicación. Los valores de respaldo de compilación son Android para posición, Natural Earth para geocodificación base y selección automática para pronóstico; `SourceManager` utiliza Open-Meteo como respaldo meteorológico y Nominatim como fuente de geocodificación recomendada. Esto no demuestra qué fuente eligió una instalación concreta. No se leyeron secretos ni se cambió su configuración. Open-Meteo solicita modelos configurados y series horarias/15 minutos; no se extrae actualmente una resolución espacial efectiva por variable de la respuesta.

| Fuente evaluada | Evidencia y utilidad | Límite / decisión |
| --- | --- | --- |
| [Open-Meteo](https://open-meteo.com/en/docs) | Modelos globales cubren Ecuador; el catálogo incluye IFS de 9 km y otras mallas. Permite comparar modelos. | La resolución depende del modelo/variable; no atribuir 9 km a todas las consultas. Fuera de Norteamérica y Europa central, 15 minutos se interpolan de datos horarios. No son radar ni observaciones de barrio. Se mantiene integración existente. |
| [NASA IMERG](https://gpm.nasa.gov/taxonomy/term/1357) | Estimación satelital global, incluida Ecuador, malla 0,1° (aprox. 11 km en el ecuador), cada 30 minutos. | Early tiene latencia mínima de unas 4 horas; sirve para contraste/acumulados, no aviso de llegada en 5 minutos. Revisar acceso, formatos y calidad orográfica antes de integrar; no integrado. |
| [INAMHI: visor de estaciones](https://inamhi.gob.ec/ddia/visor) | Existe un visor de observaciones meteorológicas e hidrológicas. | No se verificó una API pública estable, licencia de redistribución, frescura ni estación representativa de Atucucho. No se agregó scraping ni se presume cobertura por barrio. |
| [Red hidrometeorológica de Quito, 2026-10-01](https://quitoinforma.quito.gob.ec/2026/10/01/una-red-de-171-estaciones-hidrometeorologicas-monitorea-el-comportamiento-climatico-en-quito/) | El Municipio informa de 171 estaciones y mediciones que incluyen precipitación y viento. | La existencia de la red no acredita acceso automatizado público o datos actuales para cada punto. Evaluar convenio/API, sello horario, distancia, altitud y control de calidad; no integrada. |
| [Antecedente de radares INAMHI](https://www.inamhi.gob.ec/Publicaciones/inf%20gestion/Revista%20Inamhi%2053%20a.pdf) | Publicación histórica describe adquisición de radares de banda X para Quito. | No prueba operación actual ni cobertura de API accesible. No se confirma radar operativo para Atucucho ni se habilita una capa con ese nombre. |

No se añadió un proveedor de lluvia nuevo: no hay evidencia suficiente para prometer una mejora hiperlocal operativa. La siguiente integración útil requiere estaciones con acceso autorizado y datos frescos para validación; cualquier radar requiere comprobar cobertura, fecha, licencia y API. No se deduce trayectoria a partir del viento de superficie.

La tarjeta de viento ahora conserva texto de origen aun si muestra ráfagas; rechaza ángulos no finitos/fuera de rango y conserva dirección variable cuando el proveedor la indica. El adaptador y las notificaciones ya tenían dirección del viento en los cambios locales previos. No se añadió dirección de desplazamiento de lluvia, ETA ni cuenta regresiva.

## Verificación y pendientes

Comprobaciones locales ejecutadas correctamente: `git diff --check`, `python3 scripts/validate_alerta_ec.py`, revisión de referencias y cambios. No ejecutar Gradle ni compilar en Termux.

Pruebas Kotlin añadidas (pendientes de ejecución en entorno Android/JVM adecuado): conservación de decimales, hora real/altitud/precisión, compatibilidad con preferencias antiguas, borrado/metadatos ausentes y localidad frente a barrio confirmado. Se conservan los tests locales previos de prioridad y coordenadas.

Pruebas Android pendientes:

1. Permiso preciso/aproximado/denegado/revocado, proveedores apagados, sin internet, timeout y cancelación; comprobar retirada de listeners y consumo.
2. GPS reciente frente a última posición antigua; datos sin precisión/altitud, reloj incoherente y migración de preferencias Float.
3. Capturar en entorno de prueba la petición del clima y confirmar igualdad con las coordenadas originales, incluso ante fallo de Nominatim y fallback.
4. Respuestas sin barrio, con sector, con localidad Atucucho y distrito Cochapamba; sin sustituciones, sin centro administrativo y con etiqueta manual intacta.
5. Caché, llamadas concurrentes, cambio de idioma/instancia, 429/503/403 y Retry-After. Medir límites globales en infraestructura antes de distribución masiva.
6. Lectura accesible del panel GPS, fuentes grandes, pantalla bloqueada y viento con ráfagas/dirección ausente/variable. La precisión meteorológica efectiva sigue sin metadatos automáticos.
