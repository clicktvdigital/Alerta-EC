## Paso 632 — Correcciones de auditoría INAMHI (18-10-2026)

Estado: implementación local en `alerta-ec`, pendiente de pruebas unitarias y validación Android. Esta sección sustituye las limitaciones de la revisión INAMHI anterior; el resto del historial se conserva.

- Estilo base MapLibre: hasta dos intentos por vista, espera de 2 s y watchdog de 20 s por intento. Los callbacks tienen token e identidad de vista; se retiran listeners y temporizadores al terminar, pausar o destruir. Al agotar el presupuesto se informa al usuario; una vista nueva permite reintentar.
- Catálogo WMS: una descarga compartida por refresco, con OkHttp ya presente en el proyecto. Cancelar la coroutine llama a `Call.cancel()`; conexión y lectura de 10 s, plazo total HTTP de 30 s, máximo de 4 MiB reales (incluido contenido descomprimido y respuestas sin Content-Length). Se cierra la respuesta también ante errores. Estos límites corresponden a GetCapabilities; las teselas GetMap siguen gestionadas por MapLibre.
- XML: parser SAX real compartido por Android/JVM, sin dependencias nuevas; rechazo obligatorio de DTD y entidades externas mediante lexical handler/resolver. Límites de profundidad, eventos, texto y fechas. Fechas inválidas y capas duplicadas se marcan por capa; XML roto o respuesta de error invalidan el catálogo completo.
- Capas: se conservan fechas aún vigentes durante consulta/error y se informa de ello. Una capa se retira por desactivación, vencimiento o respuesta válida sin fecha seleccionable. Una fecha igual no reconstruye la fuente; una nueva se crea antes de retirar la anterior. Fallos parciales conservan registro de recursos para limpieza posterior. No se confirma descarga ni renderizado de teselas.
- Respaldos: `*.pre_*` excluidos en `.gitignore`, sin borrar ni mover archivos. No había respaldos de ese patrón ya rastreados. Los 263 respaldos registrados al inicio se comprobaron por SHA-256 y permanecen intactos.

Validaciones realizadas: `git diff --check` sin errores; revisión de archivos sin seguimiento relevantes y de cambios frente a la copia inicial; análisis sintáctico PSI de 17 archivos Kotlin sin errores. Esto no comprueba tipos ni compatibilidad de APIs. **Pruebas unitarias no ejecutadas; sin Gradle, compilación Android, APK ni consulta al servicio real.**

### Archivos del paso 632 y continuación

Modificados en `app/src/main/kotlin/org/breezyweather/ui/map/`: `MapFragment.kt`, `InamhiWmsTimes.kt`, `InamhiMapOverlay.kt`, `InamhiLayerStatus.kt`. Añadido `InamhiStyleState.kt`.

Pruebas en `app/src/test/kotlin/org/breezyweather/ui/map/`: reemplazado el recolector simulado de `InamhiWmsCapabilitiesTest.kt` por XML real; ampliado `InamhiLayerStatusTest.kt`; añadidos `InamhiStyleStateTest.kt` e `InamhiWmsDownloadTest.kt`. Cubren herencia, nombres de estilos, Extent/CDATA, fechas ausentes/inválidas, duplicados, XML truncado, excepciones WMS, DTD/entidades, límites de bytes, cancelación del parser y de Call, error HTTP/red, presupuesto de reintento y callbacks tardíos, y conservación/vencimiento de fechas.

También modificados `.gitignore`, `README.md` y este documento. Muchos archivos INAMHI y sus pruebas ya estaban sin seguimiento antes de este paso; siguen así porque no se ejecutó `git add`. Los archivos ajenos no se editaron. Copia de entrada y hashes en `/data/data/com.termux/files/usr/tmp/inamhi-audit-mfa9pyby/`; no incorporarla a Git.

Siguiente validación, en entorno autorizado:
1. Ejecutar todas las pruebas `Inamhi*` y compilar la variante correspondiente. Comprobar resolución de tipos/API de MapLibre y OkHttp; PSI solo valida sintaxis.
2. En Android, comprobar SAX/lexical handler (si no se admite la protección, falla de forma cerrada), error/timeout del estilo, éxito del segundo intento, dos fallos, pausa/retorno, recreación y callbacks tardíos. Pausar un intento consume su presupuesto; dos intentos interrumpidos requieren recrear la vista.
3. Simular HTTP lento, sin Content-Length, gzip, más de 4 MiB, HTTP 503 y desconexión durante lectura; medir que cancelación/call timeout cierran la conexión. Las pruebas con Call simulado no sustituyen esta integración.
4. Validar un catálogo con una capa inválida y dos válidas, refrescos repetidos, cambio rápido de preferencias, misma/nueva fecha, catálogo vacío válido, vencimiento durante consulta/pausa y fallo al añadir/retirar fuentes. La retirada ante excepciones nativas de MapLibre se intenta de nuevo; no se garantiza recuperación si el motor nativo deja de aceptar operaciones.
5. Inspeccionar orden, opacidad y continuidad visual de teselas. Crear una fuente nueva no confirma recepción de sus imágenes y puede producir transición visual mientras se descargan. Confirmar unidades, leyenda y disponibilidad oficial antes de afirmarlas.

No se hicieron add, commit, push, merge, tag ni Release. Se mantienen applicationId, firma, credenciales y todos los cambios locales previos. No continuar con publicación ni compilación pesada sin nueva autorización.

## Revisión local del módulo INAMHI — 18-10-2026

Esta sección actualiza únicamente el estado de mapas INAMHI y prevalece sobre sus notas históricas inferiores. Cambios locales en `alerta-ec`, sin compilación ni validación Android; no se modifica la estimación global del proyecto.

- Consultas vinculadas a la vista, el estilo y una instantánea de preferencias. Aplicación síncrona en el hilo principal después de consultar WMS; ya no quedan callbacks de aplicación fuera de la tarea. Invalidación antes de cancelar, agrupación de refrescos pendientes y limpieza al pausar/destruir la vista.
- Una consulta GetCapabilities por refresco para las tres capas. Lectura de dimensiones temporales heredadas, listas y rangos de duración fija con límites; rechazo de fechas ambiguas, intervalos inválidos y respuestas de error. Selección de fechas después de recibir la respuesta; retirada conservadora al superar la fecha seleccionada, sin inventar fecha de emisión ni periodo de validez.
- Capas activadas por defecto y desactivación persistente. La preferencia actual prevalece sobre la antigua. Botones «activada/desactivada» describen la elección; el panel distingue consulta, ausencia de fecha, error, vencimiento y teselas solicitadas sin confirmar su visualización.
- Funcionamiento sin GPS, dominio WRF continental separado del alcance nacional de ALERTA EC (incluido Galápagos). El rectángulo configurado no acredita disponibilidad por punto. Unidades y leyenda siguen sin verificar y no se inventan. No se equipara blanco con ausencia de lluvia ni pronóstico con alerta oficial.
- Se conservan PNG transparente y opacidades 0,15/0,10/0,10. Los controles se desplazan dentro de un máximo del 45 % del alto disponible para dejar espacio al mapa. No se alteran las imágenes del proveedor.
- Pruebas unitarias añadidas para invalidación/refrescos, estados, preferencias, dominio continental, URL WMS y lectura/selección temporal. **Pendientes de ejecución**. Revisión sintáctica PSI Kotlin y `git diff --check`: sin errores; no comprueban resolución de tipos, compatibilidad de APIs ni funcionamiento Android.

Pendientes: ejecutar pruebas y compilación en una etapa autorizada; validar en dispositivo rotación, salida/retorno durante consulta, alternancia rápida, cambio de preferencias, mapa sin GPS/red, fallos WMS/teselas, expiración, continente/Galápagos, texto grande y desplazamiento de paneles. Verificar metadatos oficiales, leyenda, unidades y representación de teselas antes de anunciarlos como confirmados. No se comprobó disponibilidad actual del servicio en esta sesión.


### Continuidad técnica de esta revisión INAMHI

Archivos de producción revisados y modificados: `MapFragment.kt`, `InamhiLayers.kt`, `InamhiMapOverlay.kt`, `InamhiWmsTimes.kt`, `InamhiMapPreferences.kt`, `InamhiLayerStatus.kt`; añadido `InamhiRequestState.kt`, todos en `app/src/main/kotlin/org/breezyweather/ui/map/`. `InamhiWmsTiles.kt` se revisó y se conservó sin cambios.

Pruebas en `app/src/test/kotlin/org/breezyweather/ui/map/`: ampliado `InamhiWmsTimesTest.kt`; añadidos `InamhiRequestStateTest.kt`, `InamhiLayerStatusTest.kt`, `InamhiMapPreferencesTest.kt`, `InamhiLayersTest.kt` e `InamhiWmsCapabilitiesTest.kt`. El último prueba el recolector con eventos simulados, no sustituye una prueba del parser XML Android con respuestas reales.

Límites pendientes: la cancelación puede esperar una lectura HTTP bloqueante (timeouts de conexión 10 s y lectura 20 s, sin plazo global de descarga); un mutex evita acumular conexiones al alternar capas. El estado nunca interpreta GetCapabilities ni la creación de una fuente MapLibre como confirmación de descarga/renderizado de teselas. Se retiran las capas al empezar un refresco para no mostrar datos anteriores bajo un estado nuevo; comprobar el efecto visual en dispositivo. Los rangos mensuales/anuales y `current` se rechazan, no se aproximan. Un catálogo temporal inválido produce error de consulta, sin conservar capas anteriores.

Se preservaron todos los archivos ajenos al módulo. Respaldo de entrada del módulo, pruebas y documentación en `/data/data/com.termux/files/usr/tmp/alerta-ec-inamhi-8oapihy0/` (temporal local, no incorporarlo al repositorio). Sin Gradle, APK, commit, push, tags, publicaciones ni cambios de secretos/firma.

<!-- ALERTA_EC_AUDITORIA_20261018 -->
## Estado auditado ALERTA EC v0.1.9 / 109 (18-10-2026)

Auditoria local de Codex, solo lectura, sin compilacion ni validacion Android.

- Avance global estimado: **46/100** (revision provisional del 2026-10-18; implementacion ponderada estimada 65,7 % x factor de validacion 0,70; no equivale a pruebas aprobadas).
- Revision posterior a los pasos 558-598: autosoporte reforzado (cola WorkManager, identificacion de solicitudes, contenido meteorologico, verificacion parcial/completa y cancelaciones). Las pruebas unitarias aun no se han ejecutado ni existe validacion de esta revision en Android. El aumento de implementacion de 62,7 % a 65,7 % es una estimacion de auditoria, no una medicion automatica.
- Implementacion ponderada estimada: **62,7 %**.
- Pendientes prioritarios: autosoporte y cola WorkManager; vigencia y cobertura INAMHI; pruebas meteorologicas; notificaciones multiciudad; intro; GPS/SOS; envio voluntario; accesibilidad y pruebas en Samsung.
- Las funciones futuras aplazadas siguen pendientes y no se declaran completas.
- Informe detallado: docs/ALERTA_EC_AUDITORIA_CODEX_20261018.txt.
- No publicar Release ni ejecutar compilacion integral sin autorizacion.

# ALERTA EC — CONTINUAR AQUÍ

## Revisión GPS/Nominatim/meteorología — 2026-10-17

Estado actualizado y límites en [ALERTA_EC_GPS_METEOROLOGIA.md](ALERTA_EC_GPS_METEOROLOGIA.md). Esta revisión sustituye la descripción anterior de GPS sin cambios: ahora se conserva Double, precisión/altitud/hora, se limita la edad del fallback y se mantienen coordenadas ante fallo de geocodificación. Hay caché y control de frecuencia de Nominatim, panel GPS en preferencias y dirección de viento junto a ráfagas. No hay nueva cobertura de radar ni proveedor de lluvia integrado. Los antecedentes territoriales que siguen abajo son históricos, no una consulta actual. Sin compilación, CI, commit, push ni Release; tests Kotlin/Android pendientes.


## Ubicación y notificaciones meteorológicas — 2026-10-17

Implementación local, sin compilación ni prueba Android. Nominatim y la conversión al modelo persistente priorizan `neighborhood`, `quarter`, parroquia/distrito y después ciudad. Se eliminó la inferencia que convertía una ciudad o distrito ambiguo en barrio. El nombre depende de los datos devueltos para las coordenadas: **no se ha confirmado ningún barrio concreto**, ni se añadió un nombre o polígono fijo. Android Geocoder solo proporciona `subLocality`, que no se presenta como un barrio certificado. Natural Earth y fuentes sin detalle pueden seguir mostrando una zona amplia. Las ubicaciones guardadas requieren nueva geocodificación para recibir el nombre actualizado.

En preferencias de cada ubicación se permite editar el nombre también para la ubicación actual. Es una etiqueta local manual, marcada «(manual)» en el resumen meteorológico; no verifica el barrio ni modifica latitud/longitud. Hay que revisarla al desplazarse. Se conserva la persistencia existente de `customName`, sin migración de base de datos.

Alertas horarias y notificación nativa: fenómeno y lugar en título, intervalo en subtítulo, probabilidad, acumulación en mm y temperatura en contenido compacto; detalles en `NotificationCompat.BigTextStyle`. Los estilos personalizados y multiciudad conservan su selección y muestran el resumen horario en su cabecera, con métricas en hasta dos líneas y descripción accesible. Sus vistas expandidas y apertura de la ubicación conservan el acceso a datos adicionales. Datos ausentes se muestran con raya, no con cero. La temperatura respeta la unidad configurada y la preferencia de sensación térmica en widgets. El icono térmico existente sigue representando las condiciones actuales. El intervalo iniciado dice «Desde»; uno vencido, «Intervalo pasado»; solo uno futuro dice «Prev.», con fecha si cambia de día. La recepción pertenece al detalle, no a la hora prevista.

Se conservan canales, permisos, deduplicación, preferencias y apertura de detalles. Referencia de API: [notificaciones expandibles de Android](https://developer.android.com/develop/ui/compose/notifications/expanded). Se usa visibilidad privada para los resúmenes modificados y se elimina la reflexión para cambiar iconos: se mantiene `setSmallIcon` oficial. No se añaden peticiones ni registros de coordenadas. Android puede truncar el contenido según dispositivo y tamaño de letra; falta comprobar la legibilidad real y TalkBack.

Auditoría GPS: `AndroidLocationService` entrega coordenadas y precisión, prioriza proveedor fusionado/red y conserva el fallback a última posición; no se alteraron permisos ni frecuencia. La comprobación de distancia de geocodificación ahora valida las coordenadas del proveedor antes de conservar las del pronóstico; el límite existente de 50 km es una comprobación de resultado distante, **no una certificación de barrio ni de precisión GPS**. `overwriteCoordinates = false` permanece. GPS aproximado, caché antigua o cartografía incompleta no permiten prometer identificación de barrio.

Verificación local: `git diff --check`, `python scripts/validate_alerta_ec.py` y parseo XML/unicidad de claves. Se añadieron pruebas Kotlin para prioridades, conservación de coordenadas/etiqueta, ausencia de barrio inventado y horas pasadas; **no ejecutadas** en Termux. Pendiente CI autorizado y pruebas Android: un caso de prueba anonimizado con coordenadas consentidas, geocodificación sin barrio/fallida, GPS aproximado, desplazamiento con etiqueta manual, datos horarios ausentes/antiguos, medianoche, unidades, todos los estilos, fuentes grandes, TalkBack y pantalla bloqueada. No se consultó una ubicación privada a servicios externos.

Archivos: `AlertaEcNotifier.kt`, `AlertaEcWeatherAdapter.kt`, nuevo `AlertaEcNotificationText.kt`; `EcuadorAddressMapper.kt`, `RefreshHelper.kt`, `NominatimService.kt`, modelo de dominio `Location.kt`, `LocationPreferences.kt`; los tres presentadores `*WidgetNotificationIMP.kt`, nuevo `NotificationForecastSummary.kt`, `notification_base.xml`, cadenas `values`/`values-es`; tres pruebas nuevas `EcuadorAddressMapperTest`, `LocationAddressPriorityTest`, `AlertaEcNotificationTextTest`; README y este documento.

Se conservan cambios locales previos en los dos workflows y `app/build.gradle.kts`, así como los archivos locales sin seguimiento. No se modificaron versiones, firma, secretos ni AGENTS.md. Sin Gradle, commit, push ni Release.


## Prueba previa a release posterior a v0.1.8 — 2026-10-17

**Estado actual: código preparado localmente; APK todavía no compilado ni probado.** Esta sección prevalece sobre los registros históricos inferiores. Rama `alerta-ec`, versión base comprobada en Gradle **0.1.8 / 108**; no se incrementó versión ni se publicó release o tag. Sin commit ni push.

- Notificaciones: actualizaciones, alertas graves, precipitaciones y pronósticos de hoy/mañana quedan habilitados si no existe una preferencia guardada. Se respetan los valores guardados, incluidos los desactivados. Los trabajos de pronóstico ya no guardan `false` al faltar permiso; al volver a la pantalla principal se reprograman si hay permiso y no están activos. El permiso Android, los horarios, la actualización en segundo plano y la disponibilidad de fuentes siguen siendo necesarios.
- Acerca de: logo conservado, marca, CLICK TV DIGITAL y versión en renglones separados y centrados. Una única ficha de Richard Ontaneda con el texto «Fundador de CLICK TV DIGITAL, creador y desarrollador principal de ALERTA EC. 🇪🇨». Atribución a Breezy Weather y LGPL-3.0 en el desplegable junto a los créditos originales; acceso a licencias conservado.
- Donativos: dimensiones de logos más equilibradas; los tres PNG antiguos eliminan en memoria el blanco exterior conectado a sus bordes. Se conservan los blancos interiores, recursos originales, enlaces, QR y comportamientos. Pendiente inspección visual de bordes y contraste en Android.
- Detalle horario: gráfica encima, encabezado de desplazamiento/hora destacado, datos meteorológicos en otra línea y recepción debajo con color secundario del tema; separación entre intervalos, altura flexible y sin truncar texto. Se conservan unidades, estados y datos ausentes.
- CI prepara `:app:assembleBasicDebug` y un artefacto `alerta-ec-prueba-post-v0.1.8-<SHA>` con retención de 7 días, además de las verificaciones existentes. Solo se ejecutará tras un push autorizado o PR; no crea Releases. Usa la variante `.debug` existente y firma de depuración: no sirve para comprobar actualización sobre producción. No se modificaron `applicationId`, keystore, firma de producción ni secretos.

Verificación local: `git diff --check`, `python scripts/validate_alerta_ec.py`, parseo de los XML modificados y unicidad de sus claves de cadenas. No se ejecutó Gradle en Termux ni CI remoto. No hay APK nuevo disponible todavía.

Prueba Android pendiente: instalación Debug limpia y valores predeterminados; desactivar opciones y comprobar persistencia tras reiniciar; denegar/conceder permiso y verificar planificación/entrega real de hoy y mañana; alertas y precipitación con fuente disponible; tema claro/oscuro, texto grande y TalkBack; logos sin halos o pérdida de detalle, apertura de enlaces y QR originales; datos horarios ausentes/antiguos, cambio de día y recepción. Comprobar también intro y regresiones registradas abajo. Antes de una release posterior hará falta un APK firmado de prueba y validar actualización conservando datos.

Los archivos locales sin seguimiento `AGENTS.md` y `ALERTA_EC_GITHUB_CI_RELEASE_2026-10-14.txt` se conservan intactos. Siguiente paso: revisar diff, autorizar commit/push, esperar CI y descargar el APK de prueba; no crear tag final ni publicar hasta completar pruebas y recibir autorización.


## Registro histórico: preparación anterior — 2026-10-17

Estado: **implementación local pendiente de CI y prueba Android**. Registro histórico supersedido por la sección de prueba posterior a v0.1.8. Rama `alerta-ec`; versión entonces registrada **0.1.6 / 106**, paquete `com.clicktvdigital.alertaec`, configuración de firma y certificado de producción sin cambios. No se ejecutaron Gradle, commit, push, tags ni publicación.

### Cambios implementados
- Intro (`AlertaEcVideoIntro.kt`): se retiró el fondo negro opaco del `VideoView`, causa probable de ocultación de su superficie. Una portada con el logo permanece hasta `MEDIA_INFO_VIDEO_RENDERING_START`; el audio se habilita solo entonces. Preparar el reproductor ya no cancela la protección: salida a la pantalla principal a los 4 s si no llega imagen y límite de 30 s después del primer fotograma. Error, omisión, pausa/destrucción y fin liberan reproducción y portada. Se revisaron `AlertaEcIntroController` y las llamadas existentes de `MainActivity`; no requieren cambios. Movimiento reducido omite el vídeo.
- Inspección binaria del MP4 existente: AVC/H.264 High, nivel 4.0, 720×1280, duración de contenedor 12,052 s; `moov` después de `mdat`. No se recodificó ni reemplazó. El orden del contenedor no demuestra un fallo al ser un recurso local. **No se ha confirmado visualmente el primer fotograma ni la corrección en hardware Android**: `ffprobe`/`ffmpeg` locales fallan por símbolo ausente en `libplacebo.so`.
- Precipitación: la pestaña horaria de lluvia habilitada y válida aparece primero; se conserva la selección posterior del usuario y las pestañas deshabilitadas. Gráfica sobre resumen y detalles horarios. Tarjeta cuadrada de precipitación movida después de los pronósticos junto a las mediciones. No se modificaron modelos, unidades, proveedores, valores ni reglas de ausencia de datos.
- Donativos (`SupportSection.kt`): PayPal actualizado a `https://www.paypal.com/ncp/payment/9XTVG6H4YU6JY`; PayPhone ya coincidía con `https://ppls.me/izAAuu8fptGHF6uZBV9L4Q` y se conserva. Nuevos logos locales de PayPal, PayPhone, Patreon y GitHub Sponsors; se mantienen Deuna, Binance y Banco Pichincha. Fuentes y condiciones en `artwork/alerta-ec/donaciones/MARCAS.md`. No se inventaron QR, cuentas ni enlaces; otros bancos requieren datos y recursos verificados.
- Revisión HTTP sin pagos: PayPal, PayPhone, Patreon y Sponsors respondieron 200; PayPhone llega a su formulario. Los dos enlaces Deuna redirigen a la ficha de su app (200), Binance a descarga (202). No demuestra vigencia del cobro, titularidad ni destinatario. Los enlaces existentes se conservan y deben comprobarse en la aplicación del servicio.
- Acerca de: ALERTA EC y CLICK TV DIGITAL encabezan la pantalla. Richard aparece una sola vez en colaboradores, sin repetir su nombre en la descripción. Colaboradores y traductores se despliegan desde un botón; sus datos y enlaces se conservan. Licencias, copyright y atribución a Breezy Weather permanecen. Dos textos visibles heredados en gallego y noruego ahora identifican ALERTA EC; los nombres técnicos y avisos legales se conservan.

### Validación y recuperación
- Comprobaciones locales: `git diff --check`, `python scripts/validate_alerta_ec.py`, parseo de XML modificados/nuevos y sintaxis Python del nuevo validador. Sin compilación Android local.
- `.github/workflows/alerta-ec-ci.yml` incorpora `scripts/validate_intro_media.py`: exige H.264/yuv420p, duración menor de 30 s, decodificación completa sin errores y señal visible durante el primer segundo; exporta fotogramas 0, 1, 15 y 30 y metadatos al artefacto `intro-review`. No demuestra compatibilidad de todos los decodificadores Android. Conserva pruebas `:app:testBasicDebugUnitTest` y compilación `:app:compileBasicReleaseKotlin`.
- Después de revisión y autorización de commit/push, comprobar CI en `alerta-ec`, descargar `intro-review` e inspeccionar primer fotograma y muestras. Si falla la decodificación, revisar el mensaje y exportar una copia compatible H.264 de 8 bits YUV 4:2:0 con audio AAC, conservando el original hasta validar; no reemplazar a ciegas. No reparar bibliotecas de Termux ni compilar Android para esta comprobación.
- Prueba Android pendiente: arranque frío/caliente, omitir antes/después del primer fotograma, ir al fondo y volver, giro, silencio, volumen cero y movimiento reducido; simular recurso inválido/bloqueo en una variante de prueba y confirmar retorno sin audio residual. Verificar gráfica/detalle con lluvia, cero y datos ausentes, tarjetas, texto grande, TalkBack, logos en temas claro/oscuro y créditos desplegables.
- Si la intro falla al usuario, puede omitirla; los límites automáticos vuelven a la pantalla principal sin borrar datos. Para recuperar un cambio de código, revisar el diff y revertir únicamente los fragmentos de esta sesión, conservando cambios posteriores y archivos locales. No usar `reset --hard` ni `clean`.
- Antes de una futura publicación: validar actualización sobre la versión instalada conservando datos y certificado. Incrementar versión y publicar solo con autorización posterior. `AGENTS.md` y `ALERTA_EC_GITHUB_CI_RELEASE_2026-10-14.txt` siguen sin seguimiento e intactos.


## Reparación integral v0.1.7 — avance parcial del 2026-10-16

Estado: cambios locales en código; NO compilados ni probados en Android. La orden integral NO está terminada. Esta sección es más reciente que los estados históricos inferiores. No se modificó versión, applicationId, firma, preferencias ni v0.1.6; no se ejecutaron commit, push, tags, Releases o Gradle.

### Hallazgos comprobados
- Rama `alerta-ec`; inicialmente solo estaban sin seguimiento `AGENTS.md` y `ALERTA_EC_GITHUB_CI_RELEASE_2026-10-14.txt`, conservados sin cambios.
- `OpenMeteoService.requestWeather` agrupa CURRENT, FORECAST y MINUTELY en `mForecastApi.getWeather`. Un error puede registrarse en las tres funcionalidades. No demuestra tres peticiones ni una caída del proveedor o problema de VPN.
- `RefreshHelper` procesa errores por funcionalidad y la adquisición horaria existe en `HourlyAcquisition`. Falta completar la auditoría de persistencia, cancelación, timeouts, datos vacíos y reintentos; no se cambiaron peticiones ni sus límites.
- La notificación persistente se construye en `remoteviews/presenters/notification/WidgetNotificationIMP.kt`, con variantes Native y MultiCity; no es `AlertaEcNotifier`. La ruta oficial es `remoteviews/Notifications`.
- `UV.getShortDescription` devuelve una cadena vacía si falta el índice. El consumidor añadía `UV:` incluso entonces. El layout compacto también conserva un texto de espera cuando no se asigna condición.
- `Base.refreshTime` es un intento de refresco; `currentUpdateTime` es la marca disponible para condiciones actuales. La variante Native usaba la primera.

### Cambios reales y archivos
- `WidgetNotificationIMP.kt`: condición ausente explícita, ubicación en la segunda línea compacta, AQI conservado en detalle expandido solo con índice válido, UV ausente/inválido omitido (cero válido), viento vacío omitido, etiquetas de humedad/viento/sensación/lluvia prevista traducibles y apertura de la ubicación concreta. La fecha de condiciones guardadas incluye día, hora y zona; marcas ausentes/futuras se indican como desconocidas. No se afirma actualidad usando la hora del intento.
- `NativeWidgetNotificationIMP.kt`: usa `currentUpdateTime`, muestra fecha y zona cuando no hay calendario alternativo, conserva temperatura en el título aunque también exista icono térmico y abre la ubicación concreta.
- Nuevo `NotificationWeatherText.kt`: validación de textos vacíos e índices UV finitos no negativos.
- Nuevo `app/src/test/kotlin/org/breezyweather/remoteviews/presenters/notification/NotificationWeatherTextTest.kt`: casos nulos, blancos, español/inglés, UV cero, negativo, NaN e infinitos. Pendientes de ejecutar en CI.
- `values/strings.xml` y `values-es/strings.xml`: cinco recursos nuevos para condición ausente, fecha desconocida, fecha guardada, sensación y lluvia prevista.
- Este documento registra el avance. README revisado: ya contiene logo propio, autoría, LGPL, Releases, Obtainium y aportes; sin cambios en esta fase.

### Verificación y límites
- Ejecutados correctamente: `git diff --check`, `python scripts/validate_alerta_ec.py`, parseo XML de ambos strings y comprobación de unicidad de las cinco claves nuevas; comprobaciones estáticas de integración para validación UV, ausencia de condición, fecha de condiciones y PendingIntent. Esto NO demuestra compilación o comportamiento Android.
- No ejecutados: pruebas Kotlin, Gradle, CI, APK ni validación visual. `.github/workflows/alerta-ec-ci.yml` ya declara `:app:testBasicDebugUnitTest` y `:app:compileBasicReleaseKotlin`; ejecutarlos en una etapa posterior autorizada, sin publicación.
- No existe garantía de que One UI no trunque. La variante MultiCity no se modificó. Quedan por completar la escala/origen del AQI, antigüedad independiente de sus componentes, ausencia completa de Current y coherencia de datos horarios/diarios guardados. La variante Native conserva su tratamiento del calendario alternativo.
- Prueba manual pendiente sobre v0.1.6, conservando firma y datos: comparar estilo nativo y horario/diario, expandir notificación, probar texto grande y TalkBack, condición ausente, viento vacío, UV nulo/cero, fecha antigua/futura, dos ubicaciones y pulsación de notificación. No existen capturas verificadas de este cambio.

### Pendientes de la orden integral
1. A: auditoría completa del refresco y caché; reintentos acotados solo transitorios; pruebas 200, timeout, cancelación, errores parciales/vacíos. Revisar específicamente los catch de Throwable de RefreshHelper antes de cambiar cancelación.
2. A4/C4: comprobar fuente, cobertura territorial verificable, vigencia, recepción y deduplicación de alertas. No se verificó el aviso del Litoral ni se declaró caducado o pertinente para Quito.
3. B: cuatro filas compactas +1/+2/+3/+6, adquisición sin repetición, acceso visible a reordenación persistente, medianoche y texto grande. Sin cambios en esta fase.
4. C: completar todas las variantes, fuente/escala AQI y antigüedad por dato; añadir pruebas de contenido renderizado y zona horaria.
5. D: resumen horario opt-in, planificación/cancelación, silencio y deduplicación. No implementado.
6. E: iconos, intro/insets, créditos y auditoría de PayPhone/QR/retorno bancario. No modificados ni validados en esta fase.
7. F: CI y pruebas en dispositivo antes de proponer v0.1.7. La versión permanece intacta; no publicar ni alterar historial durante la auditoría pendiente.


## Corrección de consentimiento de audio — v0.1.6 (2026-10-15)

- Corregida la lectura de `sound_enabled`: una preferencia ausente significa sonido apagado. Inicio, etiqueta del control y alternancia usan la misma lectura; solo el botón guarda la elección. Los valores `true` y `false` ya guardados se conservan, sin migración ni sobrescritura.
- El valor predeterminado anterior `true` podía permitir audio sin activación explícita cuando el timbre, volumen y foco lo permitían. Esas barreras no sustituyen el consentimiento.
- Revisión estática: omisión, final, pausa, destrucción y movimiento reducido detienen audio; pérdida de foco cancela pistas y libera reproductores. Silencio/vibración y volumen multimedia cero bloquean el inicio. No se cambió el reproductor ni la cronología.
- Añadidas pruebas unitarias para preferencia ausente, valores guardados y ausencia de escrituras al leer; ampliada la cobertura de la política al modo vibración. No ejecutadas: Gradle no se ejecuta en Termux. Pendientes CI y pruebas de audio/foco/ciclo de vida en dispositivo; los cambios de timbre durante la reproducción no se monitorizan continuamente.
- Se preservaron logo, CLICK TV DIGITAL, versión, applicationId, firma y cambios locales. Sin commit, push, tag ni Release.


## Avance de intro y alertas — 2026-10-14
- Rama `alerta-ec`, paquete de producción `com.clicktvdigital.alertaec`; no se modificaron versión, firma, publicación ni tags.
- Intro: se mantuvieron las siete pistas WAV originales y su cronología. La secuencia visual dura ahora 5,8 s y comienza el fundido a 5,3 s, para no truncar lluvia (hasta 5,15 s) ni cierre armónico (hasta 5,3 s). La política contempla animaciones deshabilitadas desde `ValueAnimator.areAnimatorsEnabled()` (API 26+) y escala de animador no finita/cero; reproducción requiere consentimiento, pista válida, timbre normal y volumen multimedia activo. La liberación de cada reproductor se protege para que un fallo no corte la limpieza del resto.
- Recursos existentes: viento, tormenta, sismo, volcán, lluvia, deslizamiento y cierre armónico. La duración/formato/picos fueron verificados por el script generador según el registro de README; `file` confirmó WAV PCM mono 16-bit/22.050 Hz en esta revisión.
- Riesgos/pendientes: faltan compilación CI, reproducción/foco real, TalkBack y validación visual en dispositivo. Volumen máximo configurado 0,12. No hay TTS en intro.
- Widgets/alertas: las notificaciones oficiales pasan por `remoteviews.Notifications`; AlertaEcNotifier omite `OFFICIAL_EVENT`, guarda IDs recientes para deduplicar, y la lluvia minutely sigue reservada al notificador original. AlertaEcWeatherAdapter solo construye el pronóstico del primer registro horario disponible. No se agregan sonidos/TTS, ETA ni avisos de anticipación con esa resolución; deben diseñarse con una fuente y deduplicación común antes de implementarse.
- No existe un widget horario específico de Alerta EC en el código encontrado: hay widgets meteorológicos generales y vistas de tendencia horaria. La lluvia minutely requiere verificar funcionalmente su proveedor y antigüedad antes de alertar.
- Verificaciones ligeras de esta revisión: `git diff --check` y `file app/src/main/res/raw/alertaec_intro_*.wav`. No se ejecutaron Gradle, CI ni pruebas instrumentadas. Siguiente: revisión manual del diff, CI de GitHub y reproducción/escucha + accesibilidad en dispositivo.

## Diagnóstico CI — commit 0c73a5d76abf10618cf029d32a782a7e223dc0ea
- `:app:compileBasicDebugUnitTestKotlin` fallaba porque `AlertaEcNotificationPolicyTest.kt` importaba `org.junit.Test` (JUnit 4), mientras `app/build.gradle.kts` incorpora JUnit 6/Jupiter mediante `libs.bundles.test` y los tests existentes usan `org.junit.jupiter.api.Test`.
- Se corrigió únicamente el import para usar Jupiter. Se conservaron todas las pruebas y su contenido; no se modificaron dependencias, CI, funciones de la app, logos, intro cinematográfica, firma ni `applicationId`.
- Verificación solicitada: `git diff --check`. No compilar Gradle en Termux.

<!-- ALERTA_EC_ESTADO_OPERATIVO_INICIO -->
## ESTADO OPERATIVO — LEER PRIMERO

### REGLA DE CONTINUIDAD
- Leer este archivo y README.md antes de cambiar codigo.
- GitHub origin/alerta-ec es el respaldo principal.
- Un comando Termux compatible con Fish por turno.
- Guardar informes extensos en Descargas.
- No publicar ubicaciones privadas, claves ni tokens.
- Validar antes de commit y push.
- Evitar compilaciones locales por almacenamiento limitado.

### ULTIMO ESTADO CONFIRMADO
- Instalación real verificada en el teléfono: 0.1.4 / versionCode 104, applicationId `com.clicktvdigital.alertaec`, certificado SHA-256 `06badd26afa4dfe2653209d6d4e9431ef4cffbd535c9d55297aa24f8df7a125e`.
- 0.1.5 / versionCode 105 es únicamente una propuesta futura; la versión configurada sigue en 0.1.4 / 104.
- Rama alerta-ec; ultimo commit de referencia 7531845b4.
- CI y panel local previamente verificados.
- GPS y geocodificacion Ecuador tienen implementacion inicial.
- Proxima prioridad: GPS 3D, direccion detallada y referencias comunitarias.
- No publicar una nueva Release sin pruebas funcionales.

### RED Y REPETIDOR
- Router/repetidor deben estudiarse tambien como posible origen del problema.
- Repetidor/AP funciona como puente, DHCP deshabilitado y administración en una red local de pruebas.
- Revisar DHCPv4, IPv6 RA/SLAAC, DNS y rutas cuando se pruebe desde el repetidor.
- No desactivar IPv6 como solucion.
- Una red IPv6-only necesita compatibilidad de red para destinos IPv4, por ejemplo NAT64/DNS64/464XLAT cuando corresponda.

### DISTRIBUCION Y POCO ALMACENAMIENTO DEL TELEFONO
- applicationId Release: com.clicktvdigital.alertaec.
- Registro histórico supersedido: la verificación actual confirma instalada 0.1.4 / versionCode 104; certificado SHA-256 `06badd26afa4dfe2653209d6d4e9431ef4cffbd535c9d55297aa24f8df7a125e`.
- GitHub Releases es la fuente permanente de APK firmadas.
- Obtainium consume las Releases oficiales de clicktvdigital/Alerta-EC.
- Mantener siempre el mismo certificado de firma para permitir actualizaciones.
- Las compilaciones de verificacion pasan a GitHub Actions con un workflow CI en cada push a alerta-ec.
- La Release firmada sigue siendo manual: no publicar una nueva version por cada cambio sin probar.
- 0.1.5 / versionCode 105 es una propuesta futura solamente, no está configurada ni publicada.
- No acumular APK, builds y logs innecesarios en el telefono.
### SIGUIENTE ACCION EXACTA
1. COMPLETADO: diagnostico integral de GPS, Nominatim, Release y panel local.
2. COMPLETADO: identificar qué campos administrativos devuelve Nominatim para un punto de prueba anonimizado, sin conservar calles ni nombres de lugares identificables.
3. EN ESTE CAMBIO: corregir la jerarquia de Ecuador para que quarter/sector no sustituya a la parroquia en el titulo principal.
4. EN ESTE CAMBIO: mapear direccion detallada como barrio/sector/parroquia/canton/provincia sin mezclar niveles.
5. EN ESTE CAMBIO: activar CI automatica de compilacion Kotlin Release en GitHub para cada push a alerta-ec.
6. EN ESTE CAMBIO: guardar el panel local dentro del repositorio y mantener una copia de ejecucion en ~/alerta-ec-panel.
7. SIGUIENTE: confirmar CI verde en GitHub.
8. SIGUIENTE: probar que la app separa barrio, parroquia y sector, conservando el sector únicamente cuando el proveedor lo devuelve como tal.
9. SIGUIENTE: resolver el código postal con estrategia multifuente; no sobrescribir un valor validado con otro proveedor sin validación.
10. SIGUIENTE: validar en dispositivo GPS, clima, minutely y FPAS; no crear una Release hasta completar CI y pruebas funcionales.
### PRIORIDADES QUE NO SE PUEDEN OLVIDAR
- GPS real y seguro.
- Lluvia hiperlocal y cuenta regresiva cuando los datos tengan precision suficiente.
- Movimiento/direccion de lluvia solamente con radar o campo espacial valido.
- Alertas Ecuador/Quito con fuentes verificables.
- Accesibilidad: texto grande, voz, sonidos, vibracion y alto contraste.
- Mapas offline.
- SOS: distinguir guardado, transmitido, recibido y confirmado.
- Bluetooth y Wi-Fi P2P para contingencia.
- LoRa/Meshtastic solamente con hardware compatible.
- Satelite/Starlink solamente con hardware y servicio compatibles.
- Historial, privacidad y moderacion de incidentes.
- No afirmar integracion oficial con ECU 911 sin que exista.

### DEFINICION DE TERMINADO
Una funcion no esta terminada solo porque exista codigo.
Cuando corresponda debe quedar:
IMPLEMENTADA -> COMPILADA -> PROBADA -> DOCUMENTADA -> COMMIT -> PUSH GITHUB.
Si se distribuye: RELEASE VERIFICADA.
<!-- ALERTA_EC_ESTADO_OPERATIVO_FIN -->


## Identificación
- Proyecto: Alerta EC, aplicación Android de alertas para Ecuador.
- Repositorio: https://github.com/clicktvdigital/Alerta-EC
- Rama de desarrollo: alerta-ec
- Código Android: com.clicktvdigital.alertaec
- Origen: adaptación de Breezy Weather, conservando sus atribuciones y licencia.

## Entorno de desarrollo
- Dispositivo: Samsung Galaxy S21 FE.
- Herramientas: Termux, Fish, Git, Gradle, Kotlin, Java y ADB.
- Carpeta habitual: ~/breezy-weather
- Compilación: alerta-build
- APK: app/build/outputs/apk/basic/debug/app-basic-arm64-v8a-debug.apk

## Estado actual
- Existe una introducción animada con fenómenos naturales, cóndor andino, logotipo y marca CLICK TV DIGITAL.
- Quedan por probar cuatro modificaciones recientes de la introducción y los colores de inicio.
- El último APK instalado no incluye necesariamente estas cuatro modificaciones.
- Las alertas meteorológicas automáticas y su precisión requieren investigación y pruebas.
- El panel local está en ~/alerta-ec-panel y utiliza http://127.0.0.1:8765.
- El panel todavía está en desarrollo; no todos los botones funcionan.

## Próximos pasos
1. Confirmar y subir los cambios pendientes de la introducción.
2. Compilar, instalar y probar la animación actualizada.
3. Revisar las notificaciones meteorológicas y sus fuentes verificables.
4. Completar el panel de desarrollo para administrar varios proyectos.
5. Preparar respaldos verificables antes de liberar almacenamiento.

## Recuperación
1. Acceder al repositorio GitHub y seleccionar la rama alerta-ec.
2. Leer este archivo antes de modificar el código.
3. Clonar el repositorio en un entorno Android/Termux compatible.
4. Revisar el historial Git y comprobar el estado de compilación.
5. No publicar contraseñas, tokens, archivos local.properties ni claves de firma.

## Instrucciones para otro asistente
Continuar desde el último estado confirmado. Trabajar en español. Dar un solo comando de Termux compatible con Fish por turno, esperar su resultado y no eliminar archivos sin verificar antes sus respaldos.


## PLAN MAESTRO ACTUALIZADO

### Vision
Alerta EC sera una aplicacion meteorologica profesional, inclusiva y accesible, desarrollada inicialmente para Ecuador y preparada para ampliarse a otras regiones. Debe explicar los fenomenos, su probabilidad, horarios, fuentes, riesgos y recomendaciones, sin presentar estimaciones como certezas.

### Avance
- Avance global estimado: 30 por ciento del alcance ampliado.
- Es una estimacion de planificacion, no una auditoria automatica.
- Base Android y compilacion Debug: funcionales.
- Intro completo: pendiente de compilar y verificar.
- Release firmada y Obtainium: en preparacion; no publicar todavia.
- Sistema meteorologico avanzado y accesibilidad: pendientes de implementar y probar.

### Prioridad 1: lluvia hiperlocal
- Pronostico del dia anterior y horarios probables.
- Avisos anticipados de 60, 30, 15 y 5 minutos cuando la precision de los datos lo permita.
- Aviso de 1 minuto y cuenta regresiva solamente con datos suficientemente fiables.
- Seguimiento durante la lluvia, intensidad y finalizacion estimada.
- Recordatorios espaciados y aviso cuando finalice.
- Pronostico de nuevos episodios de lluvia durante el mismo dia.
- Evitar falsas alarmas y notificaciones repetitivas.

### Prioridad 2: accesibilidad
- Configuracion inicial sencilla para adultos mayores.
- Sonidos diferentes segun fenomeno y gravedad.
- Vibracion, notificaciones visuales y voz mediante TTS de Android.
- Compatibilidad con lectores de pantalla, textos grandes y alto contraste.
- Modo avanzado para personalizar avisos.
- Apartado para restablecer configuraciones sin borrar datos importantes.
- Al tocar una alerta, abrir explicacion, fuente, hora, recomendaciones y estado.
- Historial de alertas consultable.

### Prioridad 3: informacion meteorologica completa
- Temperatura, sensacion termica, humedad, presion y visibilidad.
- Viento en metros por segundo y kilometros por hora, direccion y rafagas.
- Radiacion UV, calidad del aire y contaminantes cuando existan datos.
- Recomendaciones de vestimenta, actividades y horarios para salir o regresar.
- Comparar fuentes gratuitas fiables: INAMHI, Open-Meteo y otras fuentes compatibles.
- Evaluar cobertura, licencias, frecuencia y precision de satelites, radares y estaciones.
- Distinguir pronosticos de observaciones reales.

### Ubicacion
- Área de validación: un entorno urbano anonimizado de Ecuador. No publicar coordenadas GPS precisas ni intersecciones de domicilios en GitHub.
- La clasificación municipal debe conservarse como jerarquía administrativa verificada y no confundirse con parroquia, distrito o zona.
- La zona de planificación metropolitana no debe confundirse con una administración zonal ni con un distrito de salud.
- Los distritos de salud deben documentarse como nivel independiente de la parroquia y la administración zonal.
- El código postal debe tratarse como dato de fuente y validarse de forma independiente; no conservar códigos personales en el repositorio.
- Las referencias postales aportadas por usuarios deben permanecer fuera del repositorio y validarse por separado.
- Los proveedores pueden devolver códigos postales discrepantes; conservar la discrepancia como categoría y resolverla con validación multifuente, sin registrar valores concretos.
- Objetivo de presentación para el caso de prueba: mostrar barrio, sector, parroquia, cantón y provincia por separado, usando calle y código postal solo si la fuente los devuelve y la privacidad está garantizada.
- No hardcodear una direccion particular para todos los usuarios. La aplicacion debe resolver dinamicamente por GPS y conservar procedencia/confianza de cada dato.
- Fuentes de verificación: autoridades municipales y sanitarias, y una norma postal ecuatoriana; revisar cobertura, vigencia y términos.
### Fases posteriores
- Alertas verificadas de granizo, tormentas, rayos, deslizamientos, sismos, volcanes e incendios.
- Integracion de organismos oficiales y canales de difusion, incluido X cuando sea pertinente.
- No tratar publicaciones de redes sociales como confirmacion automatica de emergencias.
- No afirmar que se pueden predecir terremotos sin un sistema oficial de alerta temprana.

### Diagnosticos confirmados
- Android registra canal alert con importancia alta y vibracion inicialmente deshabilitada.
- El usuario activo manualmente vibracion, sonido y ventanas emergentes.
- Corregir configuracion predeterminada de canales nuevos y respetar ajustes del usuario.
- Existen modelos Minutely y proveedores Open-Meteo y Pirate Weather en el codigo.
- Existen PendingIntent para abrir notificaciones; falta verificar sus destinos.
- Existen archivos de intro y pantallas de alertas; falta probar su funcionamiento completo.

### Firma, respaldo y distribucion
- Paquete Debug: com.clicktvdigital.alertaec.debug.
- Paquete Release: com.clicktvdigital.alertaec.
- GitHub Actions tiene secretos de JKS Base64, contrasena de almacen y contrasena de clave.
- Existe workflow local .github/workflows/alerta-ec-release.yml aun sin validacion final.
- Verificar seguridad, firma, versionCode y funcionamiento antes de publicar Release.
- Conservar Debug hasta comprobar migracion de datos a Release.
- No publicar secretos, claves JKS, archivos privados ni datos personales.
- Conservar respaldos cifrados y verificar su disponibilidad fuera del telefono.

### Metodo de trabajo
- Mantener este archivo como unico documento maestro de continuidad de Alerta EC.
- Actualizar decisiones, avance, pruebas y siguiente paso en este mismo archivo.
- No crear documentos de continuidad duplicados.
- Un comando compatible con Fish por turno y esperar el resultado.
- Revisar cambios antes de confirmar y subir a GitHub.

### Punto de reanudacion
- Ultimo paso completado: 111, lectura del documento de continuidad.
- Paso 112: consolidar este plan maestro.
- Siguiente trabajo tecnico: verificar destino y contenido de notificaciones meteorologicas.
- Priorizar lluvia, alertas accesibles, ubicacion e intro antes de publicar Release.

### Sistema de actualizaciones
- Publicar APK Release firmadas y verificadas en GitHub Releases.
- Mantener compatibilidad con Obtainium para comprobaciones periodicas en segundo plano.
- Evaluar un actualizador integrado en Alerta EC para no exigir una segunda aplicacion.
- Permitir buscar actualizaciones manualmente y consultar la version instalada.
- Descargar actualizaciones desde fuentes oficiales mediante conexiones seguras.
- Respetar permisos de instalacion, restricciones de Android y preferencias del usuario.
- Conservar datos y configuraciones al actualizar; verificar versionCode y certificado de firma.
- No prometer instalaciones silenciosas ni comprobaciones continuas sin restricciones.
- Probar la primera Release antes de habilitar actualizaciones para usuarios finales.

## Avance del 8 de octubre de 2026 — GitHub Sponsors

- Cuenta Stripe Connect creada con Banco Pichincha; verificacion de pagos pendiente.
- Perfil GitHub Sponsors de clicktvdigital enviado para aprobacion.
- Tres aportes unicos publicados: $1 cafecito, $3 tamalito lojano con cafe y $5 encebollado.
- Tres patrocinios mensuales publicados: $2 Amigo, $5 Protector y $10 Aliado de Alerta EC.
- Se agrego al README.md la seccion de patrocinio voluntario.
- Enlace: https://github.com/sponsors/clicktvdigital
- Pendiente: aprobacion de GitHub, verificar cobros y preparar el boton de apoyo en la aplicacion.
- Mantener gratuitas las alertas y las futuras funciones SOS.


## Avance 8 de octubre de 2026 — conectividad IPv4/IPv6
- Investigado error "Red no disponible" de Open-Meteo y FPAS.
- OkHttp 5.5.0 y Retrofit 3.0.0 usan la red/DNS de Android; no se fuerza IPv4 ni IPv6.
- Open-Meteo respondió HTTP 200 mediante IPv4 durante las pruebas.
- Se corrigió isOnline() para usar NET_CAPABILITY_INTERNET + NET_CAPABILITY_VALIDATED en Android 10+.
- Objetivo: compatibilidad Wi-Fi, datos móviles, VPN, IPv4, IPv6 y dual-stack sin forzar una familia IP.
- Red IPv6-only: destinos IPv4 requieren compatibilidad de la propia red (NAT64/DNS64/464XLAT cuando corresponda).
- Pendiente probar router principal, repetidor/AP puente y datos móviles.
- Repetidor: DHCP deshabilitado y administrado dentro de la LAN; revisar posteriormente DHCPv4, RA/SLAAC, DNS y rutas sin alterar cámaras/dispositivos locales.
- Prioridades conservadas: GPS real y seguro; lluvia hiperlocal y cuenta regresiva; alertas Ecuador/Quito; mapas offline; SOS con estados guardado/transmitido/recibido/confirmado; Bluetooth/Wi-Fi P2P; LoRa solo con hardware; satélite solo con hardware/servicio compatible; historial de incidentes y privacidad.
- No afirmar dirección de lluvia sin radar/campo espacial suficiente.
- Próximo paso después del build: instalar y probar actualización meteorológica real.

<!-- ALERTA_EC_LLUVIAS_Y_ACCESIBILIDAD -->
## Lluvia hiperlocal, historial y accesibilidad — requisito funcional

### Caso real de prueba — 8 de octubre de 2026
- Zona de prueba: ubicación GPS actual de un usuario que haya dado consentimiento; NO publicar coordenadas precisas en GitHub.
- Primeras gotas observadas por el usuario: 13:40 hora local.
- Lluvia claramente observada: aproximadamente 13:45.
- Intensidad observada por el usuario: muy leve, descrita como tipo rocio.
- Alerta EC no genero una advertencia previa util para este evento.
- Este evento debe conservarse como caso de validacion para futuras versiones.

### Objetivo de alerta hiperlocal
Cuando las fuentes tengan resolucion suficiente, Alerta EC debe mostrar:
- precipitacion aproximandose al punto GPS;
- direccion cardinal de procedencia y desplazamiento;
- barrio/sector aproximado desde donde se acerca, solo cuando pueda determinarse con datos geoespaciales confiables;
- intensidad estimada;
- viento: direccion y velocidad;
- ETA aproximada hasta el punto GPS;
- cuenta regresiva actualizable;
- nivel de confianza/incertidumbre;
- fuente y hora de actualizacion;
- resumen de la situacion;
- recomendaciones preventivas apropiadas al evento.

### Regla de precision
- No inferir direccion/origen de lluvia a partir de un unico pronostico puntual.
- Para trayectoria/origen utilizar radar, satelite o campos espaciales suficientemente densos cuando esten disponibles.
- Si no existen datos suficientes, mostrar claramente que direccion/ETA no estan disponibles o que son estimaciones de baja confianza.
- No presentar predicciones como observaciones fisicas confirmadas.

### Historial de precipitacion
Registrar localmente, respetando privacidad:
- hora de emision de alerta;
- ETA pronosticada;
- intensidad prevista;
- direccion/origen estimados;
- viento;
- fuente utilizada;
- nivel de confianza;
- hora prevista de llegada;
- hora de primeras gotas confirmada por el usuario cuando exista;
- hora de lluvia observada;
- diferencia/error en minutos entre ETA y observacion;
- resultado del evento.
Las coordenadas GPS precisas no deben publicarse en GitHub.

### Accesibilidad y alerta vocal
- Integrar Android Text-to-Speech para lectura vocal de alertas.
- Compatibilidad con TalkBack y servicios de accesibilidad de Android.
- Notificacion visual + sonido + vibracion + opcion de voz.
- Mensaje hablado debe incluir peligro, ubicacion/sector cuando corresponda, direccion, intensidad, ETA y recomendacion.
- Permitir activar/desactivar voz y configurar repeticion/prioridad sin depender de audio pregrabado.
- Diseñar tambien para personas ciegas, baja vision y usuarios que no puedan mirar la pantalla.

### Recomendaciones
La alerta debe proporcionar recomendaciones breves y relacionadas con el riesgo real, evitando alarmismo. Para amenazas oficiales o emergencias, priorizar informacion de fuentes oficiales verificadas cuando este disponible.


<!-- ALERTA_EC_PERFILES_ACCESIBILIDAD_UNIDADES -->
## Perfiles de uso, tercera edad y unidades predeterminadas
- Alerta EC debe ser intuitiva para adultos mayores, personas con poca experiencia tecnológica y usuarios con discapacidad visual.
- Incorporar perfiles seleccionables: Novato/Fácil, Estándar y Avanzado. Deben funcionar como preajustes y el usuario podrá cambiar de perfil o modificar opciones individualmente.
- Novato/Fácil: interfaz simplificada, botones y texto grandes, lenguaje cotidiano, alertas prioritarias visibles, sonido, vibración, voz/TTS opcional y compatibilidad con TalkBack.
- Estándar: equilibrio entre simplicidad e información meteorológica y de emergencias.
- Avanzado: acceso a fuentes, precisión GPS, intervalos, datos técnicos y configuraciones detalladas.
- Las funciones esenciales de seguridad deben venir razonablemente configuradas desde la instalación y no depender de que un adulto mayor comprenda ajustes técnicos.
- Para Ecuador/español, usar sistema métrico como predeterminado: temperatura en grados Celsius (°C) antes que Fahrenheit (°F), viento en km/h, distancias en km/m, lluvia en mm, presión en hPa y horario de 24 horas. Fahrenheit y otras unidades seguirán disponibles como opciones manuales.
- En modo Fácil priorizar frases comprensibles como: Lluvia ligera, Viento de 12 km/h y Posible lluvia en 15 minutos, evitando tecnicismos innecesarios.
- Este requisito debe conservarse en futuras versiones y documentarse antes de la versión estable.

<!-- ALERTA_EC_FUENTES_Y_404_GITHUB -->
## Fuentes por perfil y diagnóstico HTTP 404
- Mantener tres perfiles seleccionables: Novato/Fácil, Estándar y Avanzado, conservando en todos configuraciones predeterminadas recomendadas y seguras.
- Novato/Fácil: mostrar principalmente fuentes recomendadas y explicaciones sencillas; evitar configuraciones técnicas innecesarias.
- Estándar: permitir elegir fuentes adicionales con una descripción clara de qué información aporta cada una.
- Avanzado: permitir selección y configuración detallada de fuentes, endpoints y parámetros técnicos cuando corresponda.
- Cada fuente debe poder mostrar descripción y estado comprensible: Disponible, Sin conexión, No disponible para Ecuador, Requiere configuración u otro estado verificable.
- El fallo de una fuente individual no debe presentarse como si toda la aplicación o toda la conexión a Internet estuviera caída. Mantener fuentes predeterminadas y permitir alternativas cuando sean compatibles.
- Diagnóstico 2026-10-08: Open-Meteo forecast respondió HTTP 200; FPAS area respondió HTTP 200 y sus alertas CAP/INAMHI para Ecuador terminaron en HTTP 200 después de redirección.
- El HTTP 404 observado en logcat quedó identificado con alta probabilidad como el comprobador de actualizaciones: GitHub API /repos/clicktvdigital/Alerta-EC/releases/latest devuelve HTTP 404 mientras v0.1.2 sea prerelease; /releases/tags/v0.1.2 devuelve HTTP 200. No confundir este 404 con un fallo meteorológico.
- Pendiente: corregir el manejo del comprobador de actualizaciones/prereleases y continuar validando por separado clima actual, pronóstico, precipitación de 15 minutos y alertas FPAS antes de declarar estable la versión.

<!-- ALERTA_EC_MAPAS_REFERENCIAS_SOS -->
## Mapas, referencias y futura integracion SOS
- Mantener MapLibre y mapas offline como base abierta cuando sea conveniente.
- Evaluar Google Maps y Street View como integraciones opcionales para cartografia, calles, POI, referencias visuales y apoyo al futuro boton de panico/SOS; revisar antes API, claves, costos, licencias, terminos y privacidad.
- Google Maps/Street View no sustituyen ni aumentan por si mismos la precision GNSS: las coordenadas reales deben proceder del servicio de ubicacion de Android/GPS y mostrar precision, antiguedad y hora de medicion.
- Mejorar la direccion dinamica separando calle/interseccion, barrio/sector, parroquia, ciudad, provincia, codigo postal y referencias cercanas; nunca concatenar dos barrios como si fueran un unico nombre.
- Usar POI/cartografia verificable para referencias cercanas. Permitir en el futuro referencias privadas aportadas por el usuario cuando un comercio o punto local no figure en mapas, sin publicarlas en GitHub.
- Para SOS, utilizar la posicion GPS real y permitir mapa, referencias cercanas y vista visual cuando esten disponibles, manteniendo los estados guardado/transmitido/recibido/confirmado.
- Radar meteorologico, satelite o campos espaciales validos se utilizaran para trayectoria/origen de precipitacion; no inferir movimiento de lluvia desde un unico punto de pronostico.

<!-- ALERTA_EC_SOS_DETECCION_RESCATE -->
## SOS, deteccion de riesgo y rescate comunitario
- El boton SOS debe iniciar una sesion de emergencia con GPS real, hora, precision, ultima ubicacion conocida y, cuando sea posible, actualizacion de posicion en tiempo real.
- Si la persona deja de transmitir, conservar claramente la ultima ubicacion recibida y su antiguedad; nunca presentarla como ubicacion actual.
- Evaluar deteccion automatica de posibles situaciones de riesgo mediante sensores y contexto disponibles en Android: impacto/caida, inmovilidad posterior, salida de una zona configurada o check-in vencido. Estas señales no prueban por si solas que ocurrio un accidente.
- Ante una deteccion automatica, usar una cuenta regresiva/confirmacion tipo "¿Estas bien?" con sonido, vibracion y voz accesible; si no hay respuesta, escalar segun las reglas SOS configuradas y las capacidades reales de comunicacion.
- Evitar falsos positivos por transporte, actividad normal, perdida temporal de GPS, bateria o falta de Internet.
- Durante una emergencia se podra mostrar trayectoria reciente, direccion de desplazamiento cuando los datos lo permitan, ultima posicion, precision, hora y estado de transmision para facilitar el rescate.
- No compartir por defecto la ubicacion GPS precisa con todos los usuarios de Alerta EC. Aplicar consentimiento, privacidad y roles; reservar la posicion precisa para contactos/rescatistas autorizados o sesiones SOS segun la configuracion y usar ubicacion aproximada en vistas comunitarias cuando corresponda.
- Mantener estados inequívocos: guardado, transmitido, recibido y confirmado. Sin conectividad, almacenar/encolar el SOS y no afirmar que fue recibido.
- Diseñar alternativas de comunicacion por Internet y, cuando sean tecnicamente posibles, Bluetooth/Wi-Fi P2P, LoRa con hardware externo y satelite solo con hardware/servicio compatible.
- La futura vista de rescate debe combinar mapa, referencias cercanas verificables y capas pertinentes; radar meteorologico es una capa para peligros meteorologicos, no un mecanismo para localizar personas.

<!-- ALERTA_EC_PANEL_DESARROLLO -->
## Panel local de desarrollo
- Codigo canonico del panel: tools/alerta-ec-panel dentro del repositorio.
- Copia de ejecucion local: ~/alerta-ec-panel.
- URL local: http://127.0.0.1:8765.
- El panel queda limitado a localhost y en esta fase es solo de lectura.
- Debe mostrar rama/commit/version, estado Git, GPS de Android, sesiones tmux y el final de CONTINUAR_AQUI.md.
- No exponer un endpoint de ejecucion arbitraria de comandos ni abrir el servidor a 0.0.0.0.
- Flujo recomendado: cambios locales pequenos -> commit/push -> CI automatica en GitHub -> pruebas -> Release firmada manual -> Obtainium.
- Futuro: agregar acciones autenticadas y acotadas para diagnosticos, documentacion, CI y preparacion de Release, sin almacenar tokens en el panel.

<!-- ALERTA_EC_PANEL_FIX_20261008 -->
## Correccion del panel local y CI — 8 de octubre de 2026
- Commit fba76e168 subio la mejora de geocodificacion Ecuador, el panel local y la CI inicial.
- Se detecto un error de generacion en tools/alerta-ec-panel/app.py: la cadena usada para unir las ultimas lineas de CONTINUAR_AQUI.md quedo partida y produjo SyntaxError.
- Corregir el panel y validar siempre su sintaxis con python -m py_compile.
- La CI de GitHub debe validar tambien tools/alerta-ec-panel/app.py antes de compilar Kotlin Release.
- El panel debe permanecer en 127.0.0.1:8765 y no exponerse a 0.0.0.0.
- Mantener flujo: cambios -> validaciones -> commit/push -> CI GitHub -> prueba funcional -> Release firmada -> Obtainium.
- No crear una Release nueva hasta confirmar CI verde y probar la geocodificacion corregida.
- Mantener los códigos postales únicamente como datos efímeros de validación fuera del repositorio; resolver discrepancias con estrategia multifuente.

<!-- ALERTA_EC_PANEL_FIX_DEFINITIVO_20261008 -->
## Panel local reparado — 8 de octubre de 2026
- El intento anterior de reparar app.py mediante sustitucion Perl no corrigio la cadena partida en la ruta /api/continuidad.
- Se reescribio el app.py canonico completo y se valido con python -m py_compile antes de commit/push.
- La CI ya exige validar la sintaxis de tools/alerta-ec-panel/app.py antes de la compilacion Kotlin Release.
- La copia de ejecucion local se sincroniza desde tools/alerta-ec-panel y se mantiene limitada a 127.0.0.1:8765.
- Mantener la regla: una validacion no puede imprimirse como OK si el comando anterior fallo.
- Despues de este commit: comprobar panel HTTP/API y estado de GitHub Actions.
- La siguiente Release candidata sigue siendo 0.1.4, unicamente despues de CI verde y pruebas funcionales.

<!-- ALERTA_EC_CI_PANEL_OK_20261008 -->
## CI y panel confirmados — 8 de octubre de 2026
- GitHub Actions run 37851821147 finalizo completed/success para el commit be4851f04.
- El panel local responde correctamente en 127.0.0.1:8765.
- /api/proyecto y /api/gps responden HTTP 200.
- Se excluyen __pycache__ y archivos *.pyc del repositorio.
- Siguiente validación: GPS real + Nominatim y comprobar la jerarquía barrio / sector / parroquia / cantón / provincia sin nombres identificables.
- No publicar 0.1.4 hasta completar esta validacion.

<!-- ALERTA_EC_GPS_VALIDADO_014 -->
## GPS/geocodificacion validada y candidata 0.1.4 — 8 de octubre de 2026
- Panel local operativo y arbol Git limpio.
- GPS real y Nominatim deben validarse para un caso de prueba anonimizado, comprobando que la calle, barrio, sector, parroquia, cantón y provincia se separan según los campos de la fuente.
- La jerarquía nueva produce un título compuesto solo con campos administrativos no identificables.
- Las fuentes postales oficiales deben compararse sin guardar códigos concretos.
- Los valores validados por usuarios deben permanecer fuera del repositorio y no hardcodearse.
- Las discrepancias de Nominatim/OSM deben conservarse solo como estado de validación sin valores personales.
- La administración municipal debe conservarse como nivel separado de parroquia y distrito.
- 0.1.4 sera prerelease de prueba para validar la jerarquia de ubicacion en la APK instalada.
- Antes de declarar estable el codigo postal se debe implementar validacion postal multifuente/geoespacial.

<!-- ALERTA_EC_RELEASE_014_CONFIRMADA -->
## Release 0.1.4 confirmada — 8 de octubre de 2026
- Version 0.1.4 / versionCode 104.
- CI previa del commit de Release termino completed/success.
- Tag v0.1.4 existe localmente y en GitHub y apunta al commit f3d2f7a15.
- GitHub Actions run 37853710866 de Release firmada termino completed/success.
- GitHub Release v0.1.4 fue verificada y contiene al menos una APK valida.
- Esta version es una prerelease de prueba para validar en el telefono la nueva jerarquia de geocodificacion de Ecuador.
- Caso de validación esperado: barrio y parroquia como título; sector separado; cantón y provincia en sus niveles administrativos.
- Mantener las referencias postales fuera del repositorio y resolver discrepancias de proveedores mediante validación postal multifuente.
- Siguiente paso: actualizar Alerta EC con Obtainium y verificar version instalada, certificado, direccion mostrada, GPS, clima, minutely y FPAS.

## PLAN GPS 3D Y REFERENCIAS COMUNITARIAS — 2026-10-08

### Objetivo
- Mostrar latitud, longitud, precision horizontal, altitud cuando exista, precision vertical cuando exista, fecha y hora de medicion.
- Distinguir altitud GNSS, elevacion ortometrica y elevacion del terreno; no confundir profundidad con altitud.
- Incorporar coordenadas UTM, pendiente, orientacion de ladera y relieve 3D cuando existan fuentes verificadas.
- Separar calle, interseccion, barrio, sector, parroquia, canton, provincia y codigo postal.
- Conservar procedencia, confianza y antiguedad de cada dato.
- No inventar coordenadas, distancias, alturas ni direcciones.

### Sectores de validación (sin datos identificables)
- Sectores comunitarios del caso de prueba: nombres y límites por verificar fuera del repositorio.
- Son nombres comunitarios por verificar y delimitar mediante cartografia.
- El sector debe determinarse dinámicamente; no fijar un nombre comunitario para todos.

### Calles y referencias comunitarias por verificar
- Calles y referencias residenciales: nombres y conexiones por contrastar fuera del repositorio.
- Referencias viarias del caso de prueba: contrastar externamente sin registrar nombres identificables.
- Referencias comerciales o comunitarias: validar externamente y no registrar ubicaciones privadas.
- Referencias comerciales o comunitarias: validar externamente y no registrar ubicaciones privadas.
- Referencias locales del caso de prueba: validar externamente y no registrar ubicaciones privadas.
- Referencias locales del caso de prueba: validar externamente y no registrar ubicaciones privadas.
- Referencias comerciales omitidas: validar externamente sin registrar nombres identificables.
- Faldas del Pichincha: referencia natural general.
- Las distancias son reportes comunitarios, NO mediciones GPS verificadas.
- No publicar puntos que permitan identificar un domicilio privado.

### Arquitectura y recursos
- GPS Android bajo demanda y actualizaciones adaptativas.
- Geocodificacion inversa y catalogo comunitario opcional.
- Datos locales pequenos; sin mapas 3D precargados.
- MapLibre y capas de elevacion descargables opcionales.
- Cache limitada y purga configurable.
- Referencias privadas almacenadas localmente y compartidas solo con consentimiento.
- No registrar seguimiento continuo sin activacion explicita.
- En SOS distinguir posicion actual, ultima conocida y precision.

### Siguiente trabajo tecnico
1. Revisar AdvancedLocationProvider, DetailedLocationProvider, EcuadorAddressMapper y LocationHumanFormatter.
2. Revisar modelos de altitud, sensores y mapa.
3. Implementar cambios incrementales con pruebas.
4. Ejecutar CI en GitHub, no compilaciones pesadas en el telefono.
5. Validar GPS y direccion real en el dispositivo.
6. Actualizar este documento y README.md despues de cada avance.

<!-- ALERTA_EC_REPARACION_NOTIFICACIONES_20261012 -->
## Reparación de notificaciones meteorológicas — 12 de octubre de 2026
- Corregido caso en que una notificación de precipitación anterior podía permanecer visible cuando el nuevo pronóstico minutely llegaba vacío o nulo: ahora se cancela ID_PRECIPITATION antes de retornar.
- Corregido WeatherUpdateJob para no absorber CancellationException dentro del bloque por ubicación; la cancelación se propaga al nivel exterior y permite ejecutar la limpieza final.
- Confirmado que doWork() dispone de finally que ejecuta cancelProgressNotification().
- Confirmado que el cliente meteorológico general usa timeout de conexión de 30 s, lectura de 30 s y escritura de 45 s.
- Estas correcciones están IMPLEMENTADAS pero todavía requieren COMPILACIÓN y PRUEBA FUNCIONAL antes de considerarse terminadas.
- Prueba pendiente: actualización manual normal, pérdida de red/cancelación y comprobar que «Actualizando datos climáticos…» desaparece correctamente.
- Prueba pendiente: confirmar que una respuesta minutely vacía elimina una notificación de precipitación anterior.
- No publicar una nueva Release hasta completar las pruebas funcionales.
- Próximo bloque: precisión de lluvia/Open-Meteo, cuenta regresiva estimada, widget con prioridad horaria y accesibilidad mediante sonido, vibración y TTS.



### Notificador propio: estados y precipitación (2026-10-13)

- La precipitación minutely continúa usando `Notifications.checkAndSendPrecipitation`, sin notificación paralela de Alerta EC.
- La conversión minutely en el adaptador Alerta EC queda inactiva y documentada; no se elimina ni se altera el mecanismo original.
- `OBSERVED` permanece sin generación/notificación mientras no haya una fuente observacional real. Los pronósticos no se elevan a emergencias; las alertas oficiales siguen en `remoteviews.Notifications`.
- La deduplicación elimina IDs ya notificados y repetidos dentro de una actualización; se añadieron pruebas para esos casos y para la política de estados.
- El workflow existente `alerta-ec-ci.yml` ejecuta `:app:testBasicDebugUnitTest` y compila Kotlin Release. Pendiente resultado de GitHub Actions y validación funcional en dispositivo. No se ejecutó Gradle en Termux.




## Acuerdo de producto y estado técnico consolidado — 2026-10-13

Esta sección complementa el historial de continuidad anterior; se conservan sus acuerdos, casos de prueba y notas históricas, aunque algunos pasos antiguos hayan quedado superados. El README describe la visión funcional. Este archivo conserva decisiones técnicas y pendientes.

### Regla permanente de documentación
Toda nueva función o decisión debe actualizar README.md y este documento según corresponda antes de cada commit. README registra la experiencia y capacidades previstas/implementadas; CONTINUAR_AQUI registra estado verificable, decisiones, pendientes, riesgos, pruebas y próximo paso. No etiquetar una función como implementada solo por tener un modelo o plan.

### Territorio, ubicación y unidades
- Desglosar ubicación sin mezclar niveles: calle/intersección, barrio, sector, parroquia civil, parroquia eclesiástica (solo con fuente verificable), distrito/cantón, provincia y código postal. Conservar proveedor, confianza y antigüedad. No publicar coordenadas de domicilios.
- Para el caso de referencia, los códigos postales confirmados por usuarios y fuentes oficiales deben permanecer fuera del repositorio. Las discrepancias de proveedores se resuelven con validación postal/geoespacial independiente, sin registrar valores concretos.
- Caso de geocodificación previo: separar barrio, sector, parroquia civil, cantón/distrito y provincia; no confundir niveles administrativos ni usar nombres de prueba identificables.
- Zona horaria objetivo Quito, UTC−05:00; preservar segundos en presentación/registro cuando la fuente los soporte y mostrar antigüedad. Tiempo exacto no significa precisión predictiva.
- Unidades predeterminadas: temperatura °C, lluvia acumulada mm y tasa mm/h. Expresar intervalo de acumulación y no mezclar profundidad acumulada con tasa. Pronóstico por horas prioritario y diario secundario.

### Datos, alertas y presentación
- Minutely de Open-Meteo llega como precipitación en mm por intervalo de 15 minutos; Breezy Weather multiplica por cuatro para normalizar a tasa equivalente mm/h usada en su nowcast/dBZ. Es dato de PRONÓSTICO, no observación pluviométrica. Minutely permanece bajo `Notifications.checkAndSendPrecipitation`; el adaptador Alerta EC no genera notificación paralela.
- Mejora de este cambio: si no hay un índice temporal minutely desde el corte permitido, el parser Open-Meteo devuelve null en vez de cortar un rango desde índice -1. Conversión mm/15 min a mm/h intacta. Prueba unitaria cubre intervalo encontrado, serie pasada y serie vacía.
- Distinguir siempre FORECAST, OBSERVED, OFFICIAL_EVENT y reporte ciudadano no verificado. No elevar un pronóstico a alerta oficial ni marcar OBSERVED sin fuente observacional. No predecir sismos ni inventar magnitud, hora o trayectoria.
- Seguimiento por barrios, dirección de precipitación y ETA requieren radar/campos espaciales reales con resolución suficiente; con punto único declarar indisponibilidad/incertidumbre.
- Radar y satélites deben ser observaciones/productos reales con proveedor, cobertura y fecha, diferenciados de modelos. Calidad del aire debe explicar índice/contaminantes, si son mediciones o estimaciones, fuente, hora y recomendación comprensible.
- Añadir recomendaciones solo con base en riesgo/fuente y sin alarmismo. Voz/TTS, vibración, sonidos configurables, contraste, textos grandes, lectores de pantalla y flujos fáciles permanecen objetivos; registrar qué está operativo al momento del cambio.
- Mapas offline/relieve, reportes ciudadanos moderados, historial privado, SOS con estados (guardado/transmitido/recibido/confirmado) y contactos siguen por fases; no afirmar contacto ECU 911 sin integración oficial autorizada.

### Fuentes, licencias y límites conocidos
- Inventario a evaluar, no declaración de integraciones: INAMHI, IG-EPN, SGR, COE, ECU 911, AMT, EPMMOP, MTOP, REMMAQ, GeoQuito, NOAA y canales oficiales en X, junto con proveedores ya presentes. Confirmar endpoint, cobertura, calidad, latencia, permiso, atribución y términos para cada uso.
- Redes sociales y reportes ciudadanos son pistas, no confirmación automática. Las publicaciones deben revisarse con fuente oficial cuando el riesgo lo requiera.
- No acceder a cámaras restringidas de ECU 911. Solo considerar transmisiones públicas autorizadas. Mantener explícitas restricciones de licencias de radar, satélite, mapas, cámaras y redistribución; no descargar, retransmitir ni usar comercialmente un producto sin autorización comprobada.
- Las fuentes de barrios/parroquia eclesiástica y cobertura real de radar/satélite no están comprobadas todavía. No simularlas.

### Estado actual de cambios y siguiente paso
- Se conserva el cambio local previo de `AlertaEcNotificationPolicyTest.kt`: usa `org.junit.jupiter.api.Test`, alineado con las dependencias Jupiter del proyecto. También se conserva el diagnóstico local inicial al comienzo de este archivo.
- En este cambio se actualizan ambos documentos y se agrega una guardia de índice para pronóstico minutely de Open-Meteo más pruebas unitarias. No se cambian applicationId, firma, versiones, logo ni intro.
- No se ejecuta Gradle local en Termux. Verificar `git diff --check` y solo verificaciones ligeras disponibles; push únicamente si no hay error verificable. CI remota/dispositivo quedan como validaciones posteriores si no pueden comprobarse ahora.
- Próximo paso: ejecutar CI en `alerta-ec`, revisar resultado; luego validar en dispositivo un ciclo con minutely futuro, vencido/vacío y confirmar que solo la notificación original informa precipitación. Después priorizar una mejora horaria accesible con fuente, hora y recomendación, sin duplicar alertas.

## Avance 2026-10-13 — widget meteorológico fase 1

- Se inspeccionaron presenters, layouts, proveedores, modelos y configuración de unidades existentes antes del cambio.
- Material You presenta condiciones actuales, lluvia minutely próxima y previsión horaria con acumulación por hora. La tasa mm/h se integra sobre los intervalos reales para obtener mm acumulados; la tasa máxima se mantiene claramente diferenciada. Minutely sigue siendo pronóstico, nunca lluvia observada.
- Hourly Trend muestra acumulación horaria por celda y texto accesible con hora, temperatura y acumulación. Temperatura automática en ubicaciones de Ecuador usa °C sin reemplazar una preferencia manual; las unidades de precipitación del widget son mm y mm/h. Los tiempos se formatean con la zona horaria de la ubicación y se incluye antigüedad.
- TalkBack recibe condiciones, resumen de lluvia, horas disponibles y actualización; los estados sin pronóstico y sin lluvia pronosticada son distintos.
- Pruebas unitarias añadidas para acumulación por intervalos, datos sin lluvia, selección de unidad y antigüedad; no ejecutadas porque no se debe ejecutar Gradle en Termux. Solo están escritas, no se declara compilación ni prueba exitosa.
- Pendiente: CI GitHub, validación física de los tamaños y accesibilidad, y evitar confundir el resumen del widget con la única ruta de notificación original. El widget Material You aún muestra seis horas en sus layouts; ampliar a 12–24 horas según tamaño y exponer el resumen minutely como texto visible en layouts compactos queda pendiente. No se cambió applicationId, versión, firma, logo o intro, ni se creó Release/tag.



## Diagnóstico CI — commit 550df16d51 (2026-10-14)
- Los runs 38490947969 (APK Debug) y 38490946868 (Verificación continua) fallaron durante `:app:compileBasicDebugKotlin` en `HourlyTrendWidgetIMP.kt:315`: `getPlace` no se resolvía sobre `Location` porque faltaba importar la extensión `org.breezyweather.domain.location.model.getPlace`.
- Se agregó ese import. El error era de compilación compartido, no de XML ni de las pruebas; las pruebas unitarias no llegaron a ejecutarse.
- Pendiente: confirmar compilación/pruebas en CI después del push y validar el widget en dispositivo. Gradle no se ejecuta en Termux.



## Fase de intro cinematográfica — 2026-10-14

- Se conservaron todas las escenas y marcas existentes. Se añadió una alternativa estática accesible de movimiento reducido, activable durante la intro y persistente entre lanzamientos; el destello intenso se eliminó en todos los modos; también sigue la escala de animación del sistema en cero.
- Se añadieron controles accesibles de movimiento y sonido. La opción de sonido es opt-in y se muestra como pendiente mientras no haya una pista aprobada.
- Infraestructura Android preparada con MediaPlayer/AudioFocus, volumen máximo moderado, fundido de entrada, respeto del modo de timbre silencioso/vibración y volumen multimedia cero, liberación al omitir, finalizar, pausar o destruir la actividad.
- Estado histórico previo a la actualización final: `res/raw` solo tenía JSON y la intro no reproducía. Las pistas originales están ahora incorporadas según el detalle final. Sin TTS automático.
- No se tocaron widgets, ubicaciones, datos de versión, applicationId, firma, secretos o workflows. Pruebas ligeras añadidas; no ejecutar Gradle localmente en Termux.
- La actualización final documenta recursos propios sintetizados y sincronizados; siguen pendientes CI y validación de escucha/foco en dispositivo.




## Intro cinematográfica: efectos originales (2026-10-14)

Estado: pistas procedurales implementadas e integradas; validación en dispositivo y CI pendiente. Esta actualización reemplaza el estado histórico inmediatamente anterior de “audio pendiente”.

- Se generaron localmente con síntesis digital determinista siete WAV originales, sin grabaciones descargadas: viento (5,0 s), tormenta distante (1,8 s), sismo (1,2 s), ambiente volcánico (1,5 s), lluvia (2,0 s), deslizamiento (1,4 s) y cierre armónico (1,4 s). El tamaño conjunto es aprox. 0,60 MiB (630.938 bytes).
- Archivos en `app/src/main/res/raw/alertaec_intro_{wind,storm,seismic,volcano,rain,landslide,closing}.wav`; mono PCM 16-bit, 22.050 Hz, picos limitados a 0,68 de escala digital (aprox. −3,35 dBFS), con entradas/salidas suaves. El guion reproducible es `scripts/generate_alerta_ec_intro_audio.py`; su autoría es propia y no tiene dependencias ni material de terceros.
- `IntroAudioPlayback` solicita Audio Focus transitorio y crea/reproduce/libera cada MediaPlayer por efecto. Límite por pista 0,12; no comienza sin activación explícita persistente, volumen multimedia disponible y modo de timbre normal. Omisión, movimiento reducido, pérdida de foco, pausa y destrucción cancelan tareas, detienen y liberan reproductores y abandonan foco. El sonido sigue apagado inicialmente; no hay TTS.
- Sincronía con el timeline existente: viento 0 ms; tormenta 250 ms; sismo 300 ms; volcán 1.250 ms; lluvia 3.150 ms; deslizamiento 3.350 ms; cierre 3.900 ms. No se alteraron escenas, logo o cóndor; el destello permanece desactivado y el modo de movimiento reducido sigue silenciando la intro.
- Verificación estática realizada por el generador: formato PCM, canales, frecuencia, duración exacta, integridad de lectura y pico de cada pista. No se ejecutó Gradle en Termux. Pendiente probar audición/foco en dispositivo y CI.




## Infraestructura de publicación estable — 2026-10-13
- Se eliminó el tag/versionado fijo y el disparador manual del workflow de Release: solo etiquetas explícitas `vMAJOR.MINOR.PATCH` pueden iniciar el flujo. La Release se publica estable después de validaciones de versión, paquete y firma.
- Se verifican de forma estática los secretos requeridos sin mostrar sus valores, la firma APK con el certificado SHA-256 confirmado, el `applicationId`, `versionName` y `versionCode`; se publica el APK universal con archivo de checksum SHA-256.
- Instalación real confirmada: Alerta EC 0.1.4 (104), paquete `com.clicktvdigital.alertaec`, certificado `06badd26afa4dfe2653209d6d4e9431ef4cffbd535c9d55297aa24f8df7a125e`. Propuesta futura únicamente: 0.1.5 (105).
- Versiones de acciones verificadas contra revisiones ya usadas por los workflows existentes: checkout v7.0.1, setup-java v6.0.1, setup-gradle v5.0.2 y gh-release v3.0.3.
- No se ejecutó Release ni Gradle en Termux; no se crearon tags/Releases ni se tocaron instalación, applicationId, keystore o secretos.




## Continuidad verificada — 2026-10-14

- PRIORIDAD: preservar avances y decisiones en GitHub.
- Intro cinematografica: composicion y siete pistas de audio procedurales documentadas; faltan CI y pruebas reales de audio en dispositivo.
- Widget meteorologico: fase 1 implementada en codigo; faltan CI, pruebas visuales, accesibilidad y ampliacion de horizonte.
- Alertas: adaptador meteorologico y modelos iniciales existentes; motor multiamenaza, avisos anticipados, sonidos por fenomeno, vibracion y voz TTS pendientes de integracion y validacion.
- Ubicaciones: GPS, calles, barrios, parroquias, altitud y referencias comunitarias pendientes de completar y verificar; no publicar domicilios privados.
- Donaciones: ui/support/SupportSection.kt creado y conectado localmente a AboutScreen.kt; faltan formulario, WhatsApp, pruebas y CI. No afirmar que esta publicado.
- Resenas: usar opiniones reales, moderadas, con posible lectura desde GitHub; no publicar opiniones ficticias.
- Proxima prioridad: completar aportes, revisar intro/widget/alertas, ejecutar CI y pruebas en dispositivo.
- Codex: utilizarlo para implementar tareas por modulos y documentar resultados.
- Version publica documentada: 0.1.4 / 104; mantener applicationId y certificado de produccion.
- No publicar Release, tag ni APK nueva sin pruebas, firma verificada y autorizacion.
- Actualizar README.md y docs/CONTINUAR_AQUI.md en cada avance; mantener cada proyecto en su propio repositorio.


## Requisitos SOS y búsqueda familiar inspirados en Cerberus — 2026-10-16

**Estado:** requisitos de producto y arquitectura para evaluación; **NO son funcionalidades terminadas ni verificadas en ALERTA EC v0.1.6**. Esta sección recoge la experiencia relatada por el usuario con la aplicación antirrobo Cerberus, no una comprobación independiente de las funciones de Cerberus ni de su disponibilidad actual. No altera el estado de releases, firma o permisos. Mantener este registro junto al historial de continuidad y trasladar los avances validados al README cuando se implementen.

### Objetivo y casos de uso

Ampliar ALERTA EC, de CLICK TV DIGITAL, con un sistema SOS de localización y coordinación de ayuda para desastres naturales, sismos, accidentes, caídas con incapacidad, personas desaparecidas y situaciones de secuestro o riesgo. Evitar confundir prevención comunitaria con una respuesta oficial garantizada. Mantener fuentes meteorológicas/sísmicas, comunicación de emergencia y privacidad como componentes separados pero coordinados.

### Funciones solicitadas y criterios de diseño

1. **Geolocalización real y trazable:** latitud y longitud precisas, precisión de la medición, altitud y precisión vertical cuando existan, fuente/sistema de posicionamiento, fecha, hora, minutos, segundos y zona horaria. Ofrecer coordenadas decimales y grados/minutos/segundos. No inventar altura, profundidad, mediciones, tiempos ni precisión. Diferenciar última ubicación conocida de posición actual.
2. **Botón SOS:** al activarlo, solicitar la mejor ubicación disponible e iniciar una sesión de seguimiento por 10 o 30 minutos (configurable), con fin anticipado y opción de reactivación. Actualizar, por ejemplo, cada 5 minutos, ajustando a movimiento, batería, red y precisión; informar claramente el momento de cada muestra. Cuando la señal no esté disponible, conservar y mostrar la última posición válida con su antigüedad.
3. **Hasta cinco contactos de emergencia:** alta y verificación individual, autorizaciones granulares, confirmación de recepción y opción de revocar. Enviar avisos con ubicación/mapa y, si se configura y la red lo permite, mensajes/correo electrónico a los cinco contactos. Las alertas deben distinguir envío, entrega y lectura; no prometer ninguna sin evidencia. Evitar incluir coordenadas sensibles en canales públicos o correos no protegidos sin elección informada.
4. **Solicitud remota de auxilio por un familiar:** contacto preautorizado puede solicitar localización de emergencia desde otro dispositivo, incluso si la persona no puede contestar. Exigir autenticación sólida, consentimientos previos revocables, avisos visibles y registro de accesos. Solo utilizar permisos y servicios de localización disponibles según Android. La aplicación **no puede forzar silenciosamente la activación del GPS del sistema si el usuario lo desactivó**, ni garantizar respuesta con teléfono apagado o sin conectividad. No permitir rastreo no consentido.
5. **Batería crítica (referencia Cerberus):** umbral configurable, con 5 % como caso solicitado; priorizar aviso de baja batería, coordenada más reciente, hora, precisión y último recorrido consentido antes que acciones de alto consumo. Registrar si el envío falló y reintentar cuando resulte viable. No prometer fotografías/audio/vídeo si Android, energía o permisos no lo permiten.
6. **Geovallas voluntarias:** entrada/salida de áreas elegidas, con controles anti-falsas alarmas, límites de precisión y horarios. No son equivalentes a detectar accidentes, terremotos o secuestros.
7. **Historial de recorridos y búsqueda:** sucesión de posiciones verdaderas con marcas de tiempo, precisión, estado de transmisión, inicio/fin de incidente, acciones tomadas, resultado y forma de localización/asistencia; estados no confirmado, en curso y cerrado. Control de acceso; borrado manual y retención configurable (por ejemplo 7 o 30 días) con purga automática, sujetos a validación de cachés, copias de Android y receptores externos.
8. **Detección de accidente/caída:** explorar sensores y algoritmos validados con umbrales prudentes, cuenta atrás cancelable y confirmación cuando sea posible. En caso de incapacidad, iniciar un protocolo previamente habilitado, no un diagnóstico infalible. Minimizar falsos positivos y consumo de batería.
9. **Evidencia multimedia de emergencia:** el usuario solicita audio de aproximadamente un minuto, vídeo de uno o cinco minutos e imágenes cada cinco minutos. Diseñar opciones de captura **iniciadas conscientemente en el dispositivo**, claramente señaladas y sujetas a permisos, indicadores de Android, consentimiento y restricciones de segundo plano. No implementar encendido remoto oculto del micrófono/cámara ni escucha clandestina de conversaciones; un pedido remoto de familiar podrá solicitar ayuda/localización autorizada, no activar sensores invasivos sin participación y controles del dueño.
10. **Mapa de proximidad / radar de búsqueda:** mostrar posición, última actualización, posibles ayudantes y zonas aproximadas; el radar comunitario es una presentación geográfica, no una medición satelital propia. Ofrecer nombre, iniciales o seudónimo según autorización. Las coordenadas exactas se reservarán preferentemente a contactos verificados y rescatistas legitimados, con controles especiales ante secuestro, acoso y violencia; la comunidad abierta recibirá un área aproximada de manera predeterminada y solo datos de incidentes moderados.
11. **Meteorología, sismos y peligros naturales:** integrar únicamente fuentes reales y verificables de lluvia, rayos, satélite y alertas INAMHI/Instituto Geofísico donde sea factible; distinguir pronóstico, observación y alerta oficial. Un movimiento detectado por el celular no prueba un terremoto; las notificaciones sísmicas exigen fuente confiable, sello temporal y límites geográficos claros.
12. **ECU 911 e infraestructura de cámaras:** ofrecer botón para llamar al 911 y funciones de compartir coordenadas/reportes, claramente separadas de una integración institucional no existente. Investigar app ECU911, acuerdos, protocolos y acceso a cámaras de tránsito/vecinos únicamente con autorización legal de sus propietarios u organismos; nunca acceso o activación de cámaras ajenas sin permiso.
13. **Conectividad degradada:** explorar GNSS/GPS, ubicación de red celular y Wi-Fi, Bluetooth de proximidad, radio y servicios satelitales (incluido Starlink) solo cuando hardware, sistema, permisos, operadores y APIs lo soporten. Recibir coordenadas GNSS no equivale a poder enviar por satélite. Nunca prometer comunicación sin cobertura real. Colas locales y reintentos sujetos a límites y consentimiento.
14. **Privacidad, seguridad y resistencia al abuso:** permisos graduados y revocables, cifrado en tránsito y controles de acceso, verificación de contactos, transparencia de solicitudes remotas, límite temporal de sesiones, protección de datos y auditoría de accesos. Considerar riesgos de violencia doméstica, coerción, secuestro, seguimiento indebido, filtraciones y avisos falsos. No difundir ubicación precisa de personas vulnerables públicamente por defecto. Definir mecanismo de cancelación segura.
15. **Validaciones obligatorias:** comprobar funcionamiento real en Android, restricciones de ubicación/cámara/micrófono en segundo plano, batería baja, teléfono bloqueado, GPS apagado, red ausente, retrasos, permisos denegados, actualización tras reinicio, accesibilidad y calidad del posicionamiento. Añadir pruebas y ejecutar compilaciones en GitHub Actions; evitar Gradle pesado en Termux. No presentar funciones como operativas hasta pasar validación funcional.

### Priorización recomendada

- **Núcleo:** botón SOS + GPS auténtico + última ubicación conocida + hasta cinco contactos verificados + seguimiento temporal + historial mínimo protegido + botón 911.
- **Resiliencia:** batería 5 %, red intermitente, geovallas voluntarias, detección de caídas, reintentos y gestión de falsas alarmas.
- **Multimedia segura:** capturas explícitas durante SOS y envío de evidencia con consentimiento y compatibilidad Android verificados.
- **Comunidad y coordinación:** mapa/radar, moderación y privacidad; evaluación de convenios ECU 911 y cámaras con autorización.

### Estado de proyecto / reglas de continuidad

- No confundir estos requisitos con capacidades ya presentes en la APK publicada; la última Release indicada previamente era v0.1.5 y v0.1.6 está en desarrollo local sin CI ni publicación confirmadas.
- No modificar applicationId `com.clicktvdigital.alertaec`, firma de producción, historial Git o Releases para aplicar esta especificación.
- Conservar `AGENTS.md`, `README.md`, `docs/CONTINUAR_AQUI.md`, los respaldos y las atribuciones/licencias a Breezy Weather. Registrar cada avance probado antes de anunciarlo como funcionalidad.
- La política de privacidad de ALERTA EC permanece en borrador hasta verificaciones técnicas y datos legales pendientes; actualizarla **antes** de habilitar nuevas transmisiones, multimedia, contactos o mecanismos SOS.

## Aclaración de requisitos: administración, búsquedas y batería — 2026-10-16

**Estado: REQUISITOS APROBADOS PARA ESTUDIO Y DISEÑO, NO FUNCIONES IMPLEMENTADAS.** Este anexo amplía y, en los puntos indicados, CORRIGE el anexo «Requisitos SOS y búsqueda familiar inspirados en Cerberus — 2026-10-16». Es la referencia más reciente para el diseño de este módulo; no cambia el código, las pruebas ni las Releases.

### 1. UNA sola aplicación; distintas funciones por rol

ALERTA EC — by CLICK TV DIGITAL conservará una sola aplicación Android y el identificador de producción existente. Los accesos se asignarán por cuentas autenticadas, permisos y autorización del servidor cuando exista infraestructura validada, no por una APK secreta ni por una simple pantalla oculta.

- **Usuario:** SOS propio, ubicación actual y última posición con fecha y precisión reales, permisos y contactos, geovallas consentidas, historial propio, privacidad y revocación.
- **Hasta cinco contactos familiares verificados:** recibir alertas; consultar la localización exacta durante sesiones autorizadas; solicitar una sesión de auxilio previamente consentida y temporal; reconocer recepción; no disponer de un mando permanente e invisible del teléfono.
- **Comunidad:** alertas de incidentes moderados, seudónimo/nombre consentido, bandera y mapa de proximidad; visualizar por defecto zona aproximada y hora de última actualización, nunca posiciones precisas de cualquier ciudadano a demanda.
- **Rescatista o colaborador verificado:** acceso por incidente y por tiempo limitado a los datos estrictamente necesarios para auxiliar, con autenticación, trazabilidad y validación de identidad/función.
- **Administrador CLICK TV DIGITAL:** consola especial en la MISMA aplicación para gestionar usuarios/roles autorizados, revisar reportes, evaluar evidencias de incidentes, coordinar búsqueda y rescatistas, auditar accesos y preparar notificaciones. La condición de administrador no concede GPS, cámara, micrófono ni historial permanente de todos los usuarios.
- **Autoridad pública enlazada (futura):** rol institucional independiente, creado únicamente tras convenio, validación legal/técnica y procedimiento formal. No simular que ALERTA EC es ECU 911, COE Nacional o un servicio oficial de despacho.

### 2. Inicio y ciclo de un incidente

1. Emergencia activada por el propio usuario, evento de caída potencial detectado con función previamente habilitada, o aviso presentado por familiar/administrador/comunidad.
2. Los avisos de terceros son **reportes por verificar**, no prueba de delito, secuestro, accidente o desaparición. Incorporar verificación, prevención de hostigamiento, denuncias falsas y eliminación de datos.
3. Para ubicación remota, utilizar únicamente permisos otorgados en Android y autorizaciones previas expresas, revocables y específicas de la persona. Una solicitud autenticada puede activar una sesión permitida cuando el dispositivo, la app y las reglas Android lo permitan; si servicios de localización están deshabilitados, el teléfono apagado/sin red o el sistema lo impide, mostrar el fallo y la última posición disponible claramente marcada con antigüedad.
4. Avisar a contactos autorizados y, conforme a reglas de privacidad del caso, a colaboradores verificados o a la comunidad con un aviso moderado. Mostrar bandera de incidente, estado «sin confirmar/en curso/cerrado», código de caso y seudónimo opcional.
5. Seguimiento temporal seleccionable: 10 o 30 minutos como propuestas iniciales, con posibilidad de cancelación, ampliación verificada y registro de todas las consultas; nunca convertirlo en rastreo indefinido. Durante una sesión, actualizar según capacidad real: p. ej. aproximadamente cada 5 minutos, o más frecuentemente si existe necesidad técnica y consentimiento.
6. Cierre, resumen de acciones y localización/asistencia, desactivación de enlaces y accesos temporales, y retención/borrado configurable como en el documento previo, teniendo en cuenta cachés y copias.

### 3. Identidad, autenticación y seguridad

- La app deberá exigir inicio de sesión fuerte, doble factor en administración y operaciones sensibles, comprobación del rol en servidor, controles de mínimo privilegio, permisos por incidente y caducidad.
- Nunca codificar un «usuario maestro» o contraseña secreta universal dentro de la APK. El modo de administrador no debe habilitar vigilancia discrecional de todos los usuarios.
- Registrar quién accedió, a qué incidente, cuándo, qué datos vio, justificación, autorización y caducidad. Permitir revisión de acciones sensibles.
- Vincular cada instalación a una cuenta y un identificador propio y rotativo/revocable de aplicación; no depender del IMEI para comandos remotos: en Android moderno su acceso está restringido a apps privilegiadas/casos especiales.
- Para terceros que reportan una persona desaparecida, exigir medidas contra suplantación, órdenes de acoso, violencia intrafamiliar y exposición del paradero de una víctima a su agresor.
- El usuario debe poder ver permisos y contactos autorizados, deshabilitar acceso de personas específicas y obtener un historial de solicitudes. Diseñar un procedimiento de excepción de emergencia únicamente si es legalmente aplicable, aprobado por el responsable y auditable, no un bypass oculto.

### 4. Batería: CORRECCIÓN EXPLÍCITA DEL 5 % ANTERIOR

**La referencia anterior al 5 % como umbral principal queda SUPERADA.** La solicitud más reciente fija una propuesta predeterminada de **25 % (aviso inicial)** y **20 % (aviso reforzado)**, ambos configurables, sujetos a pruebas de consumo, preferencias y permisos. A 25 %, avisar y ofrecer medidas preventivas; a 20 %, priorizar ahorro y transmitir última posición válida a destinatarios autorizados en una emergencia activa o mediante una opción expresamente habilitada. No declarar una desaparición pública, transmitir ubicación privada o grabar multimedia solo porque la batería esté al 25 %/20 %. Evitar grabaciones o sondeos intensivos que agoten la batería restante. El 5 % puede quedar como escenario de contingencia, no como activador principal.

### 5. Multimedia y comandos remotos

- Se conserva el requisito de explorar **audio ~1 minuto**, **video de 1 o 5 minutos** y **fotografías espaciadas** como evidencias de emergencia. Diseñar la captura iniciada con SOS en el dispositivo y con avisos/permisos de Android, y archivos cifrados/protegidos; validar límites y costes de red/batería.
- El usuario desea que familiares o administración pidan esa evidencia cuando la persona esté inconsciente. Se evaluará una **solicitud de auxilio remota**, pero no se implementará encendido oculto de cámara o micrófono, escucha clandestina, ni capturas remotas indiscriminadas. Android restringe cámara/micrófono desde segundo plano y requiere permisos/indicadores.
- Alternativa prioritaria para incapacidad: último recorrido consentido, última posición georreferenciada, estado de batería/red, alertas a cinco contactos, sesión de posición previamente consentida y notificaciones a voluntarios verificados.
- Nunca presentar un mensaje SMS, correo o push como una vía garantizada de encendido de GPS, sensor o cámara; requiere infraestructura, autorización, entrega de mensajes, red y permisos del sistema.

### 6. Mapa, radar y datos exactos

- GPS/GNSS y fuentes disponibles: latitud, longitud, altitud si está disponible, precisión, fuente y sello temporal con fecha/hora/minutos/segundos/zona horaria; distinguir ubicación actual de última conocida. No fabricar mediciones de profundidad ni calidad de señal.
- Mapa familiar con recorrido real, historial con caducidad y contador de última actualización; mapa comunitario con zona aproximada predeterminada, banderita/aviso, categorías moderadas, nombre o seudónimo según configuración.
- Acceso a coordenadas exactas para contactos verificados y personal de rescate autorizado por caso. Considerar rutas especiales de intervención cuando haya amenaza de secuestro, violencia o acoso: publicar ubicación exacta en un radar abierto puede poner en peligro a la víctima.
- El radar meteorológico de lluvia/rayos y el mapa comunitario de proximidad podrán compartir interfaz cartográfica, pero son capas con orígenes, exactitudes y permisos diferentes; no confundir satélite meteorológico con seguimiento de personas vía satélite.

### 7. ECU 911, COE Nacional y cámaras

- Primero habilitar teléfono 911 mediante acción explícita del usuario y exportación segura de datos para compartir; no afirmar transmisión electrónica al ECU 911 hasta verificar integración real.
- Explorar convenios de interoperabilidad del SIS ECU 911, procedimientos oficiales de privacidad y cadena de custodia. Que el desarrollador trabaje con una institución no concede por sí mismo acceso irrestricto a datos, sistemas o cámaras.
- Cámaras de tránsito, vecinos y comercios: uso solo con consentimiento de propietarios o habilitación legal/institucional. Evitar acceso o activación unilateral, especialmente para búsquedas de personas vulnerables.

### 8. Pendientes y verificación

- Diseñar servidor/API autenticada, canales de entrega confirmada, almacenamiento cifrado, registro de auditoría, controles de abuso y revisión legal de Ecuador; identificar responsables reales y destinatarios.
- Validar Android para GPS desactivado, segundo plano, suspensión de batería, sin internet, teléfono apagado, permisos revocados, geovallas, falso reporte, botón de emergencia, latencia y precisión.
- Revisar el alcance de comunicación por SMS/correo, satélite/Starlink, Wi-Fi y Bluetooth sin prometer capacidades no disponibles. Documentar claramente «solicitado», «diseñado», «implementado», «validado en CI» y «comprobado en dispositivo».
- Actualizar política de privacidad antes de habilitar localización compartida, roles, comunidad o multimedia; no publicar rastreo o grabación sin verificación y control de permisos.
- **Requisitos de proyecto:** preservar historial, README.md, AGENTS.md, docs/CONTINUAR_AQUI.md, firma, applicationId y versiones; no ejecutar builds pesados en Termux; no commit/push/tag/Release sin autorización. v0.1.6 sigue en desarrollo local.

### Fuentes técnicas y administrativas verificadas para el análisis (consultadas 2026-10-16)

Android — restricciones de servicios en segundo plano: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
Android — identificadores únicos / IMEI: https://developer.android.com/identity/user-data-ids
Android — ubicación en servicios en primer plano: https://developer.android.com/develop/background-work/services/fgs/service-types
ECU 911 — Resolución de interoperabilidad (PDF): https://www.ecu911.gob.ec/wp-content/uploads/2024/08/RESOLUCION-INTEROPERABILIDAD-ok-.-signed.pdf
ECU 911 — Registro de interoperabilidad privado: https://www.ecu911.gob.ec/registro-de-interoperabilidad-privado-local-seguro/
Superintendencia de Protección de Datos Personales (Ecuador): https://spdp.gob.ec/

<!-- ALERTA_EC_UNIVERSAL_SOS_2026_10_16_START -->

ALERTA EC — ANEXO DE CONTINUIDAD
ENFOQUE UNIVERSAL DE ACCESIBILIDAD, SOS Y COMPARTICIÓN FAMILIAR
Fecha: 2026-10-16
Estado: requisitos de producto y arquitectura propuestos; NO equivalen a funcionalidades compiladas, probadas o publicadas.
Marca: ALERTA EC — by CLICK TV DIGITAL

OBJETIVO GENERAL
ALERTA EC debe ser una aplicación inclusiva, útil e intuitiva para personas de cualquier edad y condición, tanto en Ecuador como en otros territorios donde las funciones y servicios estén disponibles. No limitar el diseño a perfiles infantiles o a usuarios familiarizados con la tecnología. El objetivo abarca emergencias, prevención de fenómenos naturales, seguridad personal, accesibilidad, ayuda comunitaria y coordinación autorizada de rescate.

USUARIOS Y NECESIDADES
- Adultos de todas las edades y personas mayores, incluidas quienes nunca han usado aplicaciones complejas.
- Personas ciegas o con baja visión: lector de pantalla, etiquetas claras, navegación lógica, avisos hablados opcionales y alternativas táctiles.
- Personas sordas o con hipoacusia: avisos por vibración, señales visuales, subtítulos y comunicación escrita sin depender del sonido.
- Personas que no pueden hablar: SOS silencioso y mensajes predefinidos, texto y pictogramas; no exigir una llamada de voz para solicitar ayuda.
- Personas con movilidad reducida, destreza limitada o discapacidad cognitiva: controles grandes, poco esfuerzo motor, navegación sencilla, ayuda guiada y recuperación de errores.
- Niñas, niños y adolescentes: modos de protección con padres o tutores acreditados, garantías reforzadas y privacidad ante la comunidad.
- Personas en accidentes, desorientación, desastres, riesgo de violencia o desaparición: protocolo SOS adaptable y última ubicación conocida.
- Cada persona elige las funciones que necesita; evitar forzar una clasificación por discapacidad o asumir incapacidad por la edad.

DISEÑO DE LA INTERFAZ
1. Misma aplicación y mismo identificador Android; accesibilidad universal desde la pantalla principal, no una app paralela.
2. Interfaz clara con opciones de "Modo sencillo" y "Modo avanzado" opcionales, reversibles y sin perder funcionalidades críticas.
3. Botón SOS grande, claramente identificado, accesible con TalkBack, buen contraste, texto escalable y áreas táctiles suficientes; estudiar accesos alternativos por botón físico o acciones del sistema cuando Android lo permita.
4. Instrucciones breves, lenguaje sencillo, iconos acompañados de texto, confirmaciones inequívocas y estados visibles: sin conexión, ubicación antigua, alerta enviada, pendiente y error.
5. Alertas multicanal: texto, vibración, sonido opcional, mensajes legibles, notificaciones compatibles con lector de pantalla. No depender de un único canal sensorial.
6. Aplicar pruebas reales con personas usuarias de tecnologías de asistencia, personas mayores y usuarios de poca experiencia tecnológica.

GPS Y COMPARTICIÓN DE UBICACIÓN (REQUISITOS)
- Tres modalidades: compartir durante 1 hora; compartir hasta que el titular lo desactive; compartir durante emergencia SOS con duración preconfigurada y posibilidad de ampliación segura.
- Hasta cinco contactos de confianza verificados, permisos explícitos y revocables; para menores o personas bajo representación legal, configuración apropiada por tutor acreditado y salvaguardas adicionales.
- Los adultos conservan su autonomía: parentesco, discapacidad o edad no conceden automáticamente acceso permanente.
- Ubicación actual precisa cuando el dispositivo la ofrece: latitud, longitud, altitud/precisión vertical si están disponibles, precisión horizontal, fuente y sello de fecha/hora completo con segundos y zona horaria; indicar antigüedad y última ubicación conocida cuando corresponda.
- Actualizaciones periódicas configurables según batería, señal, emergencia y restricciones Android; historial de recorrido con consentimiento y política de conservación configurable.
- GPS activo como capacidad PRIORITARIA al iniciar una emergencia, siempre sujeto a permisos y disponibilidad del sistema; Android no permite que una app ordinaria fuerce silenciosamente un servicio de ubicación deshabilitado por el usuario.
- Sin red: conservar localmente lo necesario y reintentar envíos; nunca afirmar que familiares o autoridades recibieron la alerta sin confirmación técnica.
- Geovallas optativas y notificaciones de llegada/salida configuradas por la persona autorizada.

SOS Y RESCATE INCLUSIVO
- SOS de un toque y mecanismo de activación por accidente/caída solo si se implementa, valida y configura de antemano; contemplar falsos positivos.
- Alternativas para personas que no pueden hablar u oír: solicitud SOS por texto, pictogramas y señales visuales/táctiles.
- Compartir con contactos autorizados el tipo de emergencia, coordenadas, precisión y actualizaciones, sin exigir que la persona pueda manejar el teléfono durante el incidente.
- El panel de comunidad puede mostrar reportes moderados y zona aproximada; la ubicación exacta de menores y personas vulnerables se reserva a contactos/rescatistas habilitados conforme a autorizaciones y protocolos de protección.
- Mapa de incidentes con marcador, alias/nombre configurable cuando corresponda, tiempo desde la última señal, estado del caso y capas meteorológicas reales separadas del "radar" de búsqueda.
- Avisos de batería tempranos configurables al 25 % y 20 %; priorizar posición y autonomía ante una emergencia activa, no publicar desapariciones solo por batería baja.
- Audio/video/fotografías SOS: evaluar únicamente flujos con inicio/consentimiento válido, indicadores y permisos exigidos por Android; no proponer activación remota clandestina de cámara o micrófono.
- Contacto al 911 mediante llamada/enlace según lo disponible, sin afirmar integración directa con ECU 911 o COE hasta tener convenio y pruebas.

ROLES EN UNA SOLA APLICACIÓN
- Persona usuaria: gestiona su ubicación y autorizaciones.
- Hasta cinco familiares/contactos verificados: acceso según consentimiento y ventana temporal autorizada.
- Comunidad: alertas moderadas con controles de privacidad y verificación.
- Rescatistas autorizados: gestión de incidentes conforme a autorización y protocolo.
- Administrador de CLICK TV DIGITAL: configura servicio, valida incidencias, modera y coordina; NO acceso irrestricto a sensores/ubicaciones. Autenticación fuerte, privilegios mínimos y trazabilidad de acceso.

VALIDACIONES Y GARANTÍAS PENDIENTES
- Auditar el comportamiento real de Android para ubicación en segundo plano, notificaciones, sensor de caída, accesibilidad y ahorro de batería.
- Documentar los tratamientos de datos y permisos en PRIVACY.md antes de publicar cambios.
- Definir límites y eliminación de historiales, registros, geovallas, sesiones SOS y respaldos; aplicar retención configurable cuando corresponda.
- Verificar seguridad, consentimiento, protección a menores y personas en riesgo, falsas alarmas, suplantación, abuso del panel administrativo y pruebas con usuarios reales.
- No atribuir capacidades a v0.1.6 sin pruebas ni despliegue.
- Mantener README.md y docs/CONTINUAR_AQUI.md actualizados al implementar y verificar; no alterar el historial previo.

NOTA DE CONTINUIDAD
Este anexo complementa los anexos GPS/SOS/Cerberus y roles/batería existentes. No los reemplaza. Prioridad de desarrollo: GPS real y fiabilidad de SOS, seguidos de accesibilidad, contactos de confianza y validación integral. La ejecución de esta especificación requiere planificación, implementación y pruebas antes de su distribución pública.

<!-- ALERTA_EC_UNIVERSAL_SOS_2026_10_16_END -->

<!-- ALERTA_EC_SIN_CUENTAS_2026_10_16_START -->

## Decisión vigente: publicación sin registro obligatorio — 2026-10-16

```text
ALERTA EC — PLAN PRIORITARIO DE PUBLICACIÓN SIN CUENTAS
Fecha: 16 de octubre de 2026
Estado: decisión de alcance y propuesta, NO implementada ni publicada por este archivo.
Proyecto: https://github.com/clicktvdigital/Alerta-EC
Rama: alerta-ec
Versión de desarrollo local reportada: 0.1.6 (versionCode 106)
Identificador de producción: com.clicktvdigital.alertaec
Desarrollador: CLICK TV DIGITAL

OBJETIVO
Entregar cuanto antes una aplicación gratuita, intuitiva y accesible sin demorar su distribución por un sistema de cuentas ni una infraestructura de pago. La urgencia no elimina la necesidad de comprobar compilación, firma, permisos y funcionamiento real antes de la Release estable.

DECISIÓN ACTUAL
1. No exigir registro, contraseñas ni identificación para consultar clima y alertas públicas y usar funciones locales disponibles.
2. Aplazar integración de Firebase, Supabase, Google Sign-In, Apple, Facebook y GitHub Login. No son requisitos de la siguiente Release estable.
3. Mantener ALERTA EC 100 % gratuita, sin anuncios invasivos y sin vender datos de ubicación.
4. Usar GitHub Releases estables y Obtainium como vía de distribución y actualización; no generar APK temporales para los usuarios finales.
5. Mantener una sola aplicación, el nombre ALERTA EC y la atribución by CLICK TV DIGITAL.
6. Proteger el historial del repositorio, las credenciales de firma y las versiones instaladas.

FUNCIONES QUE PUEDEN PLANIFICARSE SIN CUENTA NI SERVIDOR PROPIO
- Consulta de meteorología y alertas de fuentes existentes (validar cada ruta realmente funcional).
- Ubicación local con permisos Android, precisión y marca temporal reales; no inventar lecturas.
- Acceso directo al marcador 911 y/o aplicación ECU 911 como acceso externo sin afirmar integración institucional.
- Preparar mensaje SOS de texto con coordenadas y enlace de mapas en el selector de compartir de Android; su envío depende de la aplicación escogida, conexión y confirmación del usuario.
- Contactos de confianza guardados localmente, con controles claros de consentimiento y protección de datos.
- Accesibilidad desde el diseño para personas mayores, personas ciegas, sordas, sin habla, con movilidad reducida y usuarios con escasa experiencia tecnológica.

FUNCIONES FUTURAS QUE REQUIEREN COMUNICACIÓN ADICIONAL
- Seguimiento familiar EN VIVO desde otro teléfono, solicitudes remotas de ubicación y avisos automáticos a cinco contactos.
- Ubicación comunitaria sincronizada, moderación, autorización y validación de incidentes.
- Notificaciones push fiables, sincronización de rutas y evidencias entre dispositivos.
Estas funciones requieren evaluar un canal de datos seguro, servidor o servicio externo, SMS/canales compatibles y sus costos reales. No presentarlas como implementadas hasta demostrar el funcionamiento. Sin infraestructura, una app no puede garantizar ubicación remota continua ni envío automático a familiares cuando el titular esté inconsciente.
- Cámara y micrófono no podrán activarse clandestinamente a distancia. Cualquier captación SOS debe respetar permisos, restricciones del sistema, consentimiento y seguridad de víctimas.

OBTAINIUM — EXPERIENCIA DE INSTALACIÓN
Repositorio de ALERTA EC: https://github.com/clicktvdigital/Alerta-EC
Releases oficiales: https://github.com/clicktvdigital/Alerta-EC/releases
Obtainium oficial: https://github.com/ImranR98/Obtainium
Wiki de Obtainium: https://wiki.obtainium.imranr.dev/

- Añadir el repositorio propio a Obtainium; preferiblemente comenzar la instalación desde Obtainium para favorecer futuras actualizaciones automáticas.
- Activar comprobaciones periódicas y, cuando sea compatible, actualizaciones en segundo plano.
- Permitir a Obtainium instalar apps desde esa fuente en Ajustes de Android, cuando lo pida.
- No seleccionar versiones prerelease ni APK debug; una Release estable debería incluir una APK universal firmada y claramente identificable.
- El sistema puede solicitar confirmación para instalar: no prometer instalación silenciosa universal. El instalador que registró la instalación original, versión Android, configuración y permisos influyen.
- Las políticas de ahorro de batería y red pueden retrasar las comprobaciones; no asegurar que se actualice exactamente al publicarse una Release.
- Para futuras versiones conservar el mismo applicationId, misma firma de producción y versionCode ascendente; validar certificado SHA-256 de APK instalada y de la Release antes de publicar.

GOOGLE PLAY PROTECT Y VERIFICACIÓN DE DESARROLLADOR
- La APK distribuida fuera de Play puede generar solicitud de análisis porque es desconocida. No equivale por sí sola a una detección de malware.
- No desactivar Play Protect, no usar trucos para ocultar el sistema de seguridad ni pedir a usuarios que ignoren advertencias de riesgo elevado.
- Revisar permisos mínimos, funcionamiento honesto, procedencia del APK, firma, integridad, políticas de privacidad y vías oficiales de apelación si existe un falso positivo.
- Prever los cambios progresivos de Android Developer Verification; su alcance depende de país, fecha y tipo de instalación.

ANTES DE PUBLICAR v0.1.6
[ ] Resolver revisión pendiente del historial Git y protección de datos privados.
[ ] Validar CI/compilación mediante GitHub Actions, nunca Gradle pesado en Termux.
[ ] Revisar pruebas de funcionamiento y accesibilidad reales.
[ ] Confirmar certificado de firma de producción y compatibilidad de actualización desde v0.1.5.
[ ] Validar enlaces, README, INSTALACIÓN, política de privacidad y descripción honesta de funciones.
[ ] Probar Obtainium en al menos un teléfono real, incluido caso instalación inicial desde Descargas y caso desde Obtainium.
[ ] Publicar solo con autorización explícita; no hacer push, tags ni Releases de manera automática.

FUENTES PÚBLICAS DE REFERENCIA
- Obtainium: https://wiki.obtainium.imranr.dev/sources/
- Actualizaciones Android: https://developer.android.com/google/play/app-updates
- Instaladores Android: https://developer.android.com/reference/android/content/pm/PackageInstaller.SessionParams
- Play Protect: https://developers.google.com/android/play-protect/warning-dev-guidance
- Distribución alternativa: https://developer.android.com/distribute/marketing-tools/alternative-distribution
- Verificación: https://support.google.com/android/answer/17065026

ESTADO DE LA DOCUMENTACIÓN
Este TXT es un ANEXO nuevo de planificación que deja en pausa el plan anterior de cuentas. No borra los requisitos futuros de GPS, SOS, cinco familiares, inclusión universal y coordinación de rescates; los prioriza por fases. No contiene cambios de código ni da a entender que una función pendiente ya opera.
```

<!-- ALERTA_EC_SIN_CUENTAS_2026_10_16_END -->




<!-- ALERTA_EC_README_CAPTURAS_2026_10_16 -->
## Identidad visual, créditos y capturas oficiales — estado 2026-10-16

- Crédito de presentación pública: **Richard Ontaneda**, creador y desarrollador de ALERTA EC; fundador y creador de **CLICK TV DIGITAL**. No añadir el prefijo académico «Ing.» a su nombre en la documentación oficial.
- Logo principal del README: `artwork/alerta-ec/alerta-ec-master.png` (conservar este archivo original). Vista previa social GitHub configurada por separado.
- Retiradas del README las siete referencias gráficas históricas `fastlane/metadata/android/en-US/images/phoneScreenshots/` porque son del proyecto original y están en inglés. Los archivos históricos originales y las atribuciones/licencia **no se borraron**.
- No generar capturas ficticias. Pendiente obtener fotografías/capturas reales de la APK estable v0.1.6 instalada: intro, pantalla principal, pronóstico y Acerca de/apoyo; registrar versión, contexto público y fecha, y ocultar datos sensibles.
- Eliminar referencias a capturas solo del README no equivale a que la app esté validada ni a que la Release se haya publicado.
- La documentación heredada de Breezy Weather permanece en su bloque histórico desplegable, separada de la presentación de ALERTA EC.
<!-- /ALERTA_EC_README_CAPTURAS_2026_10_16 -->



<!-- ALERTA_EC_PUBLIC_README_VISUAL_V1 -->
## Presentación pública del README — revisión visual (2026-10-16)
- README principal: retirar texto histórico largo en inglés, conservando créditos/licencia de Breezy Weather y referencia a su repositorio original.
- Mostrar PayPhone, PayPal, Patreon y GitHub Sponsors como enlaces visuales con insignias grandes; **no afirmar que pagos estén probados**. El botón PayPhone lleva el nombre de marca, no un icono oficial no verificado.
- Conservar logo original de ALERTA EC `artwork/alerta-ec/alerta-ec-master.png` e incluir un logo de CLICK TV DIGITAL facilitado por el creador en `artwork/alerta-ec/click-tv-digital-logo.jpg`.
- Créditos oficiales: **Richard Ontaneda**, creador y desarrollador de ALERTA EC; fundador y creador de CLICK TV DIGITAL. No atribuir título académico «Ing.» en publicaciones formales.
- Las secciones técnicas y el bloque de sincronización se mantienen intactos pero desplegables; se debe conservar la sincronización automática y las referencias legales.
- El bloque Contributors de GitHub es controlado por GitHub y muestra avatares, no logos; el README incluye una sección propia de autoría.
- Alerta EC v0.1.6 aún debe superar CI, prueba Android y publicación de la APK firmada. Ningún enlace de pagos ni captura nueva ha sido validado por esta actualización.
<!-- /ALERTA_EC_PUBLIC_README_VISUAL_V1 -->


<!-- ALERTA_EC_QR_APORTES_V1 -->
## Métodos QR de aportes voluntarios (2026-10-16)
- PayPhone, Banco Pichincha vía Deuna, Deuna y Binance Pay: QR compartidos por el fundador de CLICK TV DIGITAL; incorporados como recursos visuales locales y como enlaces en README y SupportSection.
- Los QR se han reconocido técnicamente; antes de distribuir hay que confirmar destinatario, vigencia, usabilidad y pruebas Android/CI de la pantalla de aportes. Binance Pay requiere precaución por selección de red/moneda; la app no procesa pagos directamente.
- No obligatorios ni vinculados con prioridad en alertas; no afirmar transacciones verificadas. No se publica API ni credenciales bancarias.
- El diseñador de GitHub Contributors no puede sustituirse por logos empresariales; usar apartado propio de autoría.


## Aportes accesibles en Android (16/10/2026)

- Tarjetas con botones grandes para abrir el enlace HTTPS y consultar el QR; las cuentas Banco Pichincha y Deuna son distintas.
- Marcas visuales de Pichincha, Deuna y Binance tomadas de las capturas facilitadas por el responsable del proyecto; PayPhone figura por nombre hasta contar con logotipo oficial verificado.
- Al regresar de una aplicación externa, preguntar si el usuario pudo terminar; nunca considerar el regreso como comprobante de cobro.
- Agradecer solo tras confirmación voluntaria del usuario; compartir con Android ACTION_SEND y opinión libre 1-5 estrellas por correo, sin tienda de reseñas.
- Pendiente comprobar deep links de cada plataforma, tamaños de pantalla, lector de pantalla, navegación, Kotlin CI y APK Release antes de publicación.
- El almacenamiento/confirmación de transacciones reales requeriría integración del proveedor, no se declara implementada.
ALERTA_EC_APOYOS_ACCESIBLES_2026_10_16_V2



<!-- ALERTA_EC_COMPARTIR_CREDITOS_2026_10_16 -->
### Compartir y créditos — siguiente versión (v0.1.7)
- El texto para compartir da prioridad al enlace principal del repositorio y mantiene Releases y Obtainium. La vista previa en redes depende de metadatos y caché externos.
- Los créditos muestran «Richard Ontaneda — creador y desarrollador de ALERTA EC; fundador y creador de CLICK TV DIGITAL», sin título académico.
- Pendiente comprobar en dispositivo y CI antes de publicar una futura versión; la v0.1.6 publicada no se modifica.




## Notificaciones Android: iconos y vibración (2026-10-17)

Los iconos pequeños son vectores blancos sobre fondo transparente: lluvia, tormenta eléctrica, viento fuerte y advertencia meteorológica. Se seleccionan por el fenómeno disponible; una tormenta genérica no implica rayos. El logotipo ALERTA EC se conserva. Los pronósticos diarios mantienen los vectores mínimos nativos y ya no sustituyen el icono pequeño mediante paquetes externos.

Diagnóstico: faltaba `android.permission.VIBRATE` y el canal compartido `alert` no habilitaba vibración ni patrón. Además, Android conserva el comportamiento de los canales creados anteriormente; cambiar el código no restablece sus preferencias.

En instalaciones nuevas:

| Canal | Importancia | Patrón solicitado, en milisegundos |
| --- | --- | --- |
| `forecast` | Baja, discreta | Sin sonido ni vibración |
| `weather_precipitation` | Predeterminada | 0, 180 |
| `weather_severe` | Alta | 0, 350, 180, 350 |
| `weather_extreme` | Alta | 0, 500, 200, 500, 200, 500 |

El primer valor es la espera; los siguientes alternan vibración y pausa. Solo la severidad SEVERE/EXTREME del proveedor oficial selecciona canales graves. Los pronósticos horarios, incluso con lluvia abundante, usan el canal informativo. No hay llamadas directas al vibrador, evasión de No molestar ni cambios del modo silencio. POST_NOTIFICATIONS, bloqueo de la aplicación, ajustes del canal, No molestar y el dispositivo pueden impedir sonido o vibración; no se garantiza que vibre.

**Instalaciones existentes:** si ya existe `alert`, se conserva su ruta para los avisos que lo usaban y no se crean canales alternativos para eludir sus ajustes. Los canales existentes no se reconfiguran ni se eliminan. Mantener pulsada una notificación → Ajustes → categoría permite revisar importancia, sonido y vibración según el fabricante. El usuario puede habilitar vibración en el canal antiguo, pero sus categorías seguirán compartidas. Para probar la separación y patrones predeterminados se necesita una instalación limpia sin canales anteriores, preferiblemente en un dispositivo/perfil de pruebas. Antes de desinstalar, exportar los datos que se quieran conservar: se pueden perder ubicaciones y preferencias; una restauración de Android puede recuperar los ajustes antiguos. Eliminar y recrear el mismo ID no garantiza restablecerlo. La aplicación no realiza ese restablecimiento automáticamente.

La precipitación actualiza un ID estable, usa `setOnlyAlertOnce` y silencia reenvíos dentro de 30 minutos por ubicación. Los pronósticos conservan su deduplicación por evento. Las alertas oficiales descartan vencidas y menores, recuerdan hasta 200 claves por ubicación/ID/severidad/inicio y permiten cambios de severidad. El resumen de grupo es silencioso. La memoria es acotada: cambios de identificadores del proveedor o eventos desalojados pueden volver a avisar.

Los avisos horarios, oficiales y de precipitación usan `geographicName`: barrio, parroquia, ciudad, omitiendo datos ausentes y duplicados. Los nombres de barrio, parroquia y ciudad aparecen únicamente si esos datos están disponibles en la dirección resuelta o en una etiqueta manual; este cambio no agrega nombres ni modifica coordenadas.

Validación local: `python3 scripts/validate_alerta_ec.py`, `git diff --check` y análisis XML de manifiesto, traducciones y cuatro vectores monocromáticos. Se añadieron pruebas `AlertaEcNotificationAppearanceTest` para iconos, severidad oficial y tormenta genérica. No se ejecutaron pruebas Kotlin, compilación, instalación ni CI. Pendiente: ejecutar pruebas unitarias en un entorno Android preparado y comprobar en dispositivo instalación limpia/actualización, permiso denegado, canales bloqueados o personalizados, No molestar, silencio, vibración desactivada, repetición, escalamiento oficial y ubicación sin datos/manual. Conservar las pruebas previas de frescura, deduplicación y jerarquía geográfica.

Referencia: [canales de notificación de Android](https://developer.android.com/develop/ui/views/notifications/channels). No se modificaron versión, applicationId, firma, secretos ni logotipo en esta tarea; se preservaron los cambios locales anteriores. Sin compilación pesada, commit, push ni Release.



## Seguimiento motor de recomendaciones dinamicas - 2026-10-18
- Implementado localmente: consejos contextuales por lluvia, temperatura y viento.
- Integrado localmente: AlertaEcNotifier utiliza forEvent(event).
- Cinco pruebas unitarias escritas, pendientes de ejecucion en GitHub Actions.
- Comprobacion estructural aprobada; funcionalidad aun no certificada.
- Pendiente: evitar duplicacion de consejos, integrar pantalla principal y resumen diario.
- Pendiente: validar notificaciones inmediatas, urgentes, voz, vibracion, distritos y cobertura nacional.
- No publicar version estable hasta CI, prueba en telefono y autorizacion.

<!-- ALERTA_EC_AUTO_START -->

# ALERTA EC - ESTADO MAESTRO DEL PROYECTO

Actualizado: 2026-10-13
Proyecto: Alerta EC
Repositorio: clicktvdigital/Alerta-EC
Rama de desarrollo: alerta-ec

## OBJETIVO
Sistema ecuatoriano de vigilancia meteorologica, emergencias naturales, seguridad vial y avisos territoriales, con GPS, mapas, notificaciones, sonido, vibracion y voz.

## ESTADO REAL
- Base Android: existente.
- Modelos AlertaEcPhenomenon y AlertaEcEvent: creados; integracion pendiente.
- Correcciones de notificaciones y cancelacion: modificadas localmente; compilacion y pruebas pendientes.
- Preferencias de sonido, vibracion y voz: estructura inicial; integracion pendiente.
- Motor multiamenaza: pendiente.
- Motor de vigilancia vial: pendiente.
- Integracion de fuentes oficiales: pendiente.
- Camaras publicas autorizadas: pendiente.
- Radar y seguimiento geografico: pendiente.
- Notificaciones por voz y sonidos especificos: pendiente.
- Widget meteorológico fase 1: código local implementado con condiciones actuales, lluvia minutely pronosticada, acumulación horaria, antigüedad y descripción accesible; CI/pruebas/dispositivo pendientes. Cuenta regresiva: pendiente.
- Intro cinematográfica: composición y accesibilidad conservadas; pistas ambientales originales sintetizadas e integradas en `res/raw`, con reproducción opt-in y MediaPlayer/AudioFocus. Ver actualización detallada al final del documento; CI y validación en dispositivo pendientes.
- Resumen diario de las 09:00 dentro de Android: pendiente.

## FENOMENOS NATURALES
Lluvia, lluvia fuerte, tormenta electrica, granizo, viento, inundacion, deslizamiento, sismo, actividad volcanica, incendio, niebla, nieve, calor extremo, frio extremo, ciclon tropical y tsunami.

## VIGILANCIA VIAL
Accidentes, choques, volcamientos, cierres, baches, derrames, obstaculos, inundaciones viales, deslizamientos, fallas de semaforos y restricciones oficiales.

## FUENTES POR EVALUAR
INAMHI, Instituto Geofisico, ECU 911, Gestion de Riesgos, COE, AMT, EPMMOP, instituciones viales, medios de comunicacion, canales RSS y publicaciones oficiales en X.

Verificar disponibilidad, permisos, frecuencia de actualizacion, condiciones de uso y calidad de cada fuente antes de integrarla.

## VIGILANCIA GEOGRAFICA
- Ubicacion GPS actual.
- Sectores favoritos y zonas cercanas.
- Quito y otras zonas configurables.
- Distancia y relevancia respecto de la ubicacion o ruta.
- No confundir pronostico, observacion, reporte ciudadano y alerta oficial.

## NOTIFICACIONES
- Clasificacion por fenomeno, gravedad, fuente y ubicacion.
- Sonido diferenciado cuando corresponda.
- Vibracion y notificacion por voz.
- Evitar avisos repetidos y eventos caducados.
- Alertas de lluvia 10, 5 y 1 minuto antes solo si los datos permiten esa precision.
- Cuenta regresiva de segundos solo con respaldo tecnico suficiente.
- Nunca predecir terremotos ni inventar magnitudes, intensidades u horas exactas.

## CAMARAS Y MAPAS
- Reproductor de transmisiones publicas autorizadas.
- No acceder a camaras restringidas de ECU 911.
- Distinguir radar observacional de modelos y animaciones de pronostico.
- Reportes ciudadanos identificados como no verificados hasta su corroboracion.

## REGLAS OBLIGATORIAS
1. Leer README.md, docs/CONTINUAR_AQUI.md y este documento antes de modificar codigo.
2. Preservar cambios locales existentes.
3. No declarar implementado algo que solo esta planificado.
4. No inventar datos meteorologicos, sismicos ni viales.
5. No confundir modelos de datos con funciones operativas.
6. Mantener una sola fuente maestra de estado.
7. Sincronizar README y CONTINUAR_AQUI antes de cada commit.
8. Verificar diferencias antes de subir cambios.
9. Compilar preferentemente mediante GitHub Actions.
10. No publicar releases sin compilacion y pruebas funcionales.
11. Registrar cada avance y el siguiente paso pendiente.
12. Usar comandos compatibles con Fish y Termux.

## SIGUIENTE BLOQUE
Implementar el motor central de eventos, la clasificacion de fuentes y la conversion de datos meteorologicos existentes a eventos normalizados.

Posteriormente: notificaciones inteligentes, vigilancia territorial, seguridad vial, fuentes oficiales, mapas, camaras autorizadas y reportes diarios.

## CRITERIO DE FINALIZACION
IMPLEMENTADO -> COMPILADO -> PROBADO -> DOCUMENTADO -> COMMIT -> PUSH.
Para distribucion: RELEASE VERIFICADA.

## ENTORNO DE TERMINAL Y COMANDOS VALIDADOS

Entorno principal de desarrollo movil: Termux con shell Fish.

### METODOS QUE FUNCIONAN
- Un solo comando completo por turno.
- Encadenar operaciones con ; y and.
- printf "%s\n" para crear archivos pequenos o medianos.
- Crear scripts persistentes y luego ejecutarlos con python3.
- python3 -c para operaciones cortas sin cadenas anidadas complejas.
- git diff --check para validacion estatica.
- git status --short para comprobar cambios.
- grep y sed para inspecciones puntuales.
- scripts/sync_alerta_ec_docs.py para sincronizar documentacion.
- GitHub Actions para compilaciones pesadas.

### METODOS QUE NO DEBEN REPETIRSE
- No usar heredoc de Bash: python3 - <<PY, porque Fish no lo maneja como Bash.
- Evitar cat <<EOF para bloques grandes en Fish.
- No transportar scripts grandes mediante Base64 pegado en Termux: una cadena ya se corrompio durante el pegado.
- No generar scripts Python complejos dentro de otro python3 -c usando literales de salto de linea anidados: produjo SyntaxError por cadena sin terminar.
- No realizar compilaciones Android pesadas innecesarias en el telefono.

### REGLA DE DOCUMENTACION
- docs/ALERTA_EC_ESTADO.md es la fuente maestra del estado.
- README.md y docs/CONTINUAR_AQUI.md reciben exactamente un bloque automatico.
- Antes de commit: actualizar estado maestro, sincronizar, comprobar duplicados y ejecutar git diff --check.
- Una idea nueva se registra primero como PLANIFICADA; no debe aparecer como IMPLEMENTADA hasta existir codigo.
- Estados permitidos: PLANIFICADO, IMPLEMENTADO, COMPILADO, PROBADO, DOCUMENTADO, PUSH y RELEASE VERIFICADA.
- No borrar antecedentes tecnicos utiles al sincronizar.
- No versionar archivos .bak temporales.

## AUTOMATIZACION DEL REPOSITORIO
- .githooks/pre-commit sincroniza y valida la documentacion antes de cada commit.
- scripts/validate_alerta_ec.py realiza validaciones reutilizables.
- En un clon nuevo ejecutar: git config core.hooksPath .githooks
- Los hooks deben permanecer versionados en el repositorio.
- GitHub Actions debe convertirse progresivamente en la autoridad de compilacion y validacion pesada.

## COMUNIDAD, REPORTES Y FUNCIONES SOCIALES - PLANIFICADO

### COMPARTIR AMENAZAS E INCIDENTES
- Cada evento debe poder compartirse mediante Android Share.
- Incluir acceso rapido para compartir por WhatsApp cuando este disponible.
- El contenido compartido debe incluir fenomeno o incidente, ubicacion aproximada, hora, gravedad, fuente y enlace cuando corresponda.
- Diferenciar claramente alerta oficial, observacion, pronostico y reporte ciudadano.
- No compartir automaticamente ubicacion precisa privada sin consentimiento.

### COMPARTIR Y RECOMENDAR ALERTA EC
- Incorporar opcion Compartir aplicacion / Recomendar Alerta EC.
- Utilizar el enlace oficial de descarga cuando exista una publicacion estable.
- No insertar enlaces de descarga provisionales como si fueran oficiales.

### COMUNIDAD Y COMENTARIOS
- Incorporar un espacio de comunidad asociado a eventos y zonas.
- Permitir comentarios y actualizaciones de usuarios.
- Implementar mecanismos contra spam, abuso, informacion falsa y contenido peligroso antes de habilitar publicacion publica.
- Mostrar fecha y estado de verificacion de los reportes.

### REPORTE CIUDADANO MULTIMEDIA
- Boton Reportar para accidentes, inundaciones, granizo, derrumbes, incendios, baches, derrames, obstaculos y otras eventualidades.
- Permitir fotografia mediante camara con permiso explicito.
- Permitir video mediante camara con permiso explicito.
- Permitir audio mediante microfono con permiso explicito.
- Permitir seleccionar archivos existentes cuando corresponda.
- Adjuntar ubicacion GPS solamente con autorizacion del usuario.
- Registrar fecha y hora del reporte.
- Permitir descripcion escrita.
- Incorporar dictado de voz a texto para generar una descripcion editable antes de publicar.
- El usuario debe poder revisar y corregir el texto reconocido.
- Todo reporte ciudadano inicia con estado NO VERIFICADO.
- Nunca convertir automaticamente un reporte ciudadano en alerta oficial.

### SOS Y GPS - FASE POSTERIOR
- Boton SOS claramente diferenciado de un reporte normal.
- Activacion deliberada con proteccion contra pulsaciones accidentales.
- Compartir ubicacion GPS durante una emergencia solamente con consentimiento explicito.
- Permitir configurar hasta cinco contactos de confianza elegidos por el usuario.
- Posibilidad futura de enviar estado de emergencia y ubicacion a esos contactos.
- Posibilidad futura de publicar una solicitud de ayuda a la comunidad bajo controles de privacidad y seguridad.
- Permitir cancelar o finalizar el estado SOS.
- No afirmar que el SOS contacta ECU 911 mientras no exista una integracion oficial autorizada y comprobada.
- Evaluar funcionamiento con conectividad limitada y mecanismos de reintento sin prometer comunicacion cuando no exista red.

### PRIVACIDAD Y SEGURIDAD
- Solicitar permisos de camara, microfono y ubicacion solo cuando la funcion los necesite.
- Explicar al usuario por que se solicita cada permiso.
- Minimizar almacenamiento de ubicaciones precisas.
- No exponer publicamente domicilio o ubicacion sensible innecesaria.
- El usuario controla que contenido multimedia y ubicacion envia.
- Definir retencion y eliminacion de reportes antes de habilitar backend comunitario.

### ORDEN PROPUESTO DE IMPLEMENTACION
1. Motor central AlertaEcEvent y fuentes.
2. Alertas meteorologicas reales y notificaciones.
3. Compartir eventos mediante Android Share.
4. Reportes ciudadanos basicos con texto, foto y ubicacion opcional.
5. Video, audio y voz a texto.
6. Backend de comunidad, comentarios, moderacion y verificacion.
7. Vigilancia vial y mapa colaborativo.
8. Configuracion de contactos de confianza.
9. SOS/GPS y pruebas especificas de seguridad.

Estas funciones permanecen PLANIFICADAS hasta que exista codigo funcional, compilado y probado.
- rg/ripgrep NO esta instalado actualmente en Termux; no usarlo como dependencia obligatoria.
- Para busquedas de codigo usar grep -R -n -E, que ya esta disponible, o instalar ripgrep explicitamente antes de usar rg.

## Adaptador meteorológico Alerta EC
- Estado: IMPLEMENTADO / COMPILADO EN CI.
- Archivo: app/src/main/kotlin/org/breezyweather/alertaec/AlertaEcWeatherAdapter.kt
- Convierte alertas oficiales Breezy Weather a AlertaEcEvent con estado OFFICIAL_EVENT.
- Convierte precipitación minutely detectada por dBZ a evento FORECAST de lluvia.
- Convierte códigos meteorológicos horarios compatibles a fenómenos Alerta EC.
- Conserva fuente, ubicación, coordenadas, inicio/fin y severidad cuando existen.
- Los eventos minutely son FORECAST y no deben presentarse como lluvia observada.
- CI verificada: commit e9f5791e6, APK Debug y Verificacion continua completaron SUCCESS.
- Pendiente: pruebas funcionales en dispositivo, clasificación por intensidad, probabilidad, viento, deduplicación, notificaciones, sonido, vibración y TTS.
- Modelos canónicos confirmados en módulo domain: Weather, Alert, Minutely y Location.
- Regla de seguridad: Alerta EC ayuda a prevención, información y respuesta temprana; no afirmar que puede impedir desastres ni que un pronóstico es una observación.

## Politica de publicacion estable y Obtainium

- Alerta EC NO debe publicar betas, APK Debug ni builds temporales como actualizacion publica.
- La distribucion publica debe realizarse mediante GitHub Releases estables y ser compatible con Obtainium para todos los usuarios.
- Mantener SIEMPRE el applicationId de produccion: com.clicktvdigital.alertaec.
- Toda actualizacion publica debe estar firmada con EXACTAMENTE la misma clave/firma de produccion usada por la version actualmente instalada. Nunca generar o sustituir la clave de firma sin un proceso de migracion compatible.
- Incrementar versionCode y versionName en cada nueva version publica.
- Los APK Debug generados por CI sirven exclusivamente para comprobacion y pruebas; no son Releases publicas.
- GitHub Actions debe realizar las compilaciones pesadas para evitar consumir almacenamiento y recursos del telefono de desarrollo.
- Flujo obligatorio de una version publica: IMPLEMENTADO -> COMPILADO EN CI -> TESTEADO EN DISPOSITIVO -> DOCUMENTADO -> RELEASE ESTABLE.
- Antes de RELEASE debe verificarse instalacion/actualizacion sobre la version publica anterior SIN desinstalar la aplicacion y conservando datos/configuracion cuando Android lo permita.
- Obtainium debe poder detectar la nueva GitHub Release y descargar el artefacto estable correspondiente.
- No marcar una version como RELEASE solamente porque compile.
- Si la firma, applicationId, versionCode o mecanismo de Release no estan verificados, DETENER la publicacion.

### Adaptador meteorologico enriquecido

Estado: IMPLEMENTADO / COMPILADO EN CI.

- Commit verificado: e5d0a50e42bd39ad7847e0e85e4b525146fb4fbd.
- GitHub Actions: Alerta EC - Verificacion continua = SUCCESS.
- GitHub Actions: Alerta EC - APK Debug = SUCCESS.
- El adaptador horario incorpora precipitacion en mm/h, probabilidad de precipitacion en porcentaje, velocidad y rafagas de viento en km/h, direccion del viento, humedad y visibilidad.
- Clasifica lluvia fuerte utilizando los umbrales horarios existentes de Breezy Weather.
- Los eventos generados desde pronostico permanecen etiquetados como FORECAST; no deben presentarse como observaciones reales ni alertas oficiales.
- Pendiente: prueba funcional en dispositivo, deduplicacion, notificaciones Alerta EC, sonido, vibracion y TTS.
- Este hito NO constituye una Release publica ni una actualizacion de Obtainium.

### Widget meteorológico Alerta EC — fase 1 (2026-10-13)

Estado: IMPLEMENTADO EN CÓDIGO / COMPILACIÓN, PRUEBAS Y DISPOSITIVO PENDIENTES.

- Se revisaron los presenters HourlyTrendWidgetIMP y MaterialYouForecastWidgetIMP, sus layouts y proveedores, los modelos Weather/Current/Hourly/Minutely/Base y la resolución de unidades de SettingsManager.
- Material You prioriza condiciones actuales, muestra lluvia próxima cuando hay intervalos minutely futuros y deja explícito que es pronóstico. El total se calcula integrando la tasa mm/h por la duración real de cada intervalo y se presenta como mm acumulados, con tasa máxima aparte en mm/h.
- La previsión horaria de Material You agrega la acumulación horaria en mm junto a la temperatura. La descripción accesible incluye condiciones actuales, lluvia próxima, hasta 24 horas de datos disponibles y antigüedad de actualización.
- Hourly Trend incluye la acumulación horaria en mm por celda y un texto accesible para cada hora renderizada.
- En ubicaciones de Ecuador se usa Celsius cuando la preferencia está en automático; una unidad configurada manualmente conserva prioridad. La precipitación del widget se expresa en mm y mm/h, sin cambiar las preferencias globales.
- Los horarios usan la zona horaria de Location. No se inventan observaciones, radar, trayectoria, ETA ni cuenta regresiva. Sin datos minutely futuros se informa que falta pronóstico; una serie disponible sin lluvia se distingue de una serie ausente.
- Se añadieron pruebas unitarias para integración de mm/h por intervalo, ausencia de lluvia pronosticada, prioridad de unidad manual y antigüedad de datos. No se ejecutaron: no correr Gradle en Termux.
- Limitación de esta fase: las celdas visibles de Material You siguen mostrando hasta seis horas debido a los layouts actuales; las 12–24 horas solo quedan incluidas en la lectura accesible cuando los datos están disponibles. Extender visualmente el horizonte según tamaño sigue pendiente. Los layouts compactos sin campo secundario presentan el detalle de lluvia en TalkBack, no como texto visible.
- Pendiente: validar compilación y pruebas mediante CI de GitHub, probar en dispositivo todos los tamaños, TalkBack, texto grande, datos vacíos/vencidos y confirmar que la notificación de minutely continúa siendo únicamente la ruta original de Breezy Weather. No se modificaron applicationId, versión, firma, logo ni intro y no se publicó Release.


## Revisión de widgets, intro y distribución — 2026-10-13

### Fechas de actualización del widget
- Corregido `updateAgeMinutes`: compara primero la diferencia en milisegundos y rechaza timestamps futuros antes de convertir a minutos completos.
- Regresión conservada y ampliada en `AlertEcForecastWidgetDataTest`: timestamps a +1 ms y +59 999 ms se rechazan; un dato de 59 999 ms de antigüedad sigue representando cero minutos completos; se mantienen los casos de 5 y 120 minutos.
- Pruebas añadidas pero no ejecutadas localmente; no ejecutar Gradle en Termux.

### Widgets y nombres accesibles
- Los layouts Material You de pronóstico que muestran ciudad permiten hasta dos líneas y ya no truncan ese título con puntos suspensivos; 1x1 y 2x1 conservan su diseño compacto y anuncian la ubicación completa en la descripción TalkBack.
- La descripción del widget Hourly Trend también inicia con el nombre completo de la ubicación. Las tarjetas de ubicación permiten dos líneas para título y dirección/estado y TalkBack incluye ambos textos junto a las instrucciones de gesto.
- La temperatura principal de los widgets Material You de pronóstico se redujo de 42sp a 36sp; el formato 2x1 pasó de 38dp a 34sp para respetar escala tipográfica. Se conservan las dimensiones, variantes, datos y acciones existentes.
- Falta validar visualmente todos los tamaños, escalas de texto y servicios de accesibilidad en dispositivo.

### Intro cinematográfica
- Se conservaron animaciones, escenas, logo y marcas. Se agregó un control accesible para omitir y una descripción de la introducción; textos disponibles en inglés y español.
- Reducción de movimiento con una presentación alternativa que preserve el significado y la secuencia de escenas: pendiente de diseño y validación.
- **Los sonidos y la narración de la intro están pendientes. La intro actual no contiene audio ni narración.** No confundir esto con los planes de TTS para alertas de la app.

### Release, firma y Obtainium
- Inspección estática: `applicationId` configurado `com.clicktvdigital.alertaec`, `versionCode` 104 y `versionName` 0.1.4. No se modificaron esos valores ni la firma o secretos.
- `.github/workflows/alerta-ec-ci.yml` corre en push/PR de `alerta-ec`, ejecuta pruebas unitarias y compilación Kotlin Release sin publicar.
- Nota histórica superada: el workflow antes admitía `workflow_dispatch` y tenía `v0.1.4` fijado. La infraestructura actualizada se documenta en la sección final de este archivo.
- Nota histórica superada: este registro anterior indicaba que la instalación/certificado no se habían verificado; la verificación real actual confirma 0.1.4 / 104 y el SHA-256 registrado en la sección final.
- No se cambió versión, firma, secretos, tags o releases.

### Verificaciones de este avance
- Pasaron `git diff --check`, `scripts/validate_alerta_ec.py`, parseo XML de los 13 layouts/recursos modificados y `py_compile` de los dos scripts de validación/sincronización. Gradle local no se ejecutó.
- Si las verificaciones ligeras pasan, crear commit y hacer push únicamente a `origin/alerta-ec`, sin tag ni Release.

## Corrección CI — 2026-10-14
- Runs fallidos sobre `550df16d51`: APK Debug `38490947969` y Verificación continua `38490946868`.
- Causa raíz en ambos: `HourlyTrendWidgetIMP.kt:315` usaba `location.getPlace(context)` para la descripción accesible, pero faltaba importar la extensión `getPlace` del paquete `org.breezyweather.domain.location.model`; Kotlin informó `Unresolved reference`.
- Corrección: añadir el import en `HourlyTrendWidgetIMP.kt`. No fue necesario modificar XML, pruebas, workflows, datos de versión o firma.
- Las pruebas unitarias no llegaron a ejecutarse, pues falló antes la compilación Kotlin. Pendiente volver a comprobar ambas ejecuciones de CI y validar en dispositivo.


## Intro cinematográfica — fase de accesibilidad y audio seguro (2026-10-14)

Nota histórica: la infraestructura estuvo implementada antes de sintetizar los recursos descritos en la actualización final de este documento.

- Se preservaron el logo, cóndor, terreno y escenas existentes de volcán, incendio, inundación, deslizamiento, tormenta y sismo. Se mantuvieron las animaciones cinematográficas originales para el modo normal.
- La intro agrega un control accesible y persistente de movimiento reducido. Presenta las mismas escenas en una composición estática, reduce desplazamientos/zoom; los destellos intensos se eliminaron en todos los modos. También respeta la escala de animación del sistema cuando está en cero.
- Se agregó una preferencia explícita de sonido. El reproductor usa MediaPlayer, atributos de uso multimedia, solicitud/liberación de audio focus, límite de volumen 0.22 y entrada gradual. No inicia en modo silencio/vibración ni con el volumen multimedia a cero.
- Omisión, fin de la intro, pausa y destrucción de MainActivity detienen y liberan audio. No se genera voz TTS.
- Estado histórico previo: `res/raw` solo contenía JSON/datos. La actualización final documenta la síntesis original propia que resolvió esa carencia.
- Estado histórico previo a la actualización final: el controlador recibía recurso cero y no reproducía. Las pistas propias ahora están generadas e integradas; consultar la actualización final para su lista y sincronía.
- Pruebas unitarias ligeras añadidas para consentimiento/estado silencioso/recurso disponible y política de movimiento. No se ejecutó Gradle en Termux.
- No se alteraron widgets, ubicaciones, applicationId, versiones, firma, secretos ni workflows de Release. No se creó tag ni Release.
- Pendiente: obtener y verificar recursos ambientales redistribuibles, registrar cada archivo y licencia, asignar audio por escena y validar escucha/transiciones/accesibilidad en dispositivo; compilar y ejecutar pruebas en CI.


## Intro cinematográfica: efectos originales (2026-10-14)

Estado: pistas procedurales implementadas e integradas; validación en dispositivo y CI pendiente. Esta actualización reemplaza el estado histórico inmediatamente anterior de “audio pendiente”.

- Se generaron localmente con síntesis digital determinista siete WAV originales, sin grabaciones descargadas: viento (5,0 s), tormenta distante (1,8 s), sismo (1,2 s), ambiente volcánico (1,5 s), lluvia (2,0 s), deslizamiento (1,4 s) y cierre armónico (1,4 s). El tamaño conjunto es aprox. 0,60 MiB (630.938 bytes).
- Archivos en `app/src/main/res/raw/alertaec_intro_{wind,storm,seismic,volcano,rain,landslide,closing}.wav`; mono PCM 16-bit, 22.050 Hz, picos limitados a 0,68 de escala digital (aprox. −3,35 dBFS), con entradas/salidas suaves. El guion reproducible es `scripts/generate_alerta_ec_intro_audio.py`; su autoría es propia y no tiene dependencias ni material de terceros.
- `IntroAudioPlayback` solicita Audio Focus transitorio y crea/reproduce/libera cada MediaPlayer por efecto. Límite por pista 0,12; no comienza sin activación explícita persistente, volumen multimedia disponible y modo de timbre normal. Omisión, movimiento reducido, pérdida de foco, pausa y destrucción cancelan tareas, detienen y liberan reproductores y abandonan foco. El sonido sigue apagado inicialmente; no hay TTS.
- Sincronía con el timeline existente: viento 0 ms; tormenta 250 ms; sismo 300 ms; volcán 1.250 ms; lluvia 3.150 ms; deslizamiento 3.350 ms; cierre 3.900 ms. No se alteraron escenas, logo o cóndor; el destello permanece desactivado y el modo de movimiento reducido sigue silenciando la intro.
- Verificación estática realizada por el generador: formato PCM, canales, frecuencia, duración exacta, integridad de lectura y pico de cada pista. No se ejecutó Gradle en Termux. Pendiente probar audición/foco en dispositivo y CI.


## Infraestructura de publicación estable — 2026-10-13
- Instalación real verificada en teléfono: `com.clicktvdigital.alertaec`, versionName 0.1.4, versionCode 104 y certificado SHA-256 `06badd26afa4dfe2653209d6d4e9431ef4cffbd535c9d55297aa24f8df7a125e`.
- La configuración Gradle permanece en 0.1.4 / 104. 0.1.5 / 105 es solo una propuesta futura.
- El workflow de Release ya no fija tag/versión ni admite ejecución manual: se activa únicamente con etiqueta explícita estable `vMAJOR.MINOR.PATCH`. Comprueba coherencia de etiqueta, versionName y versionCode, paquete de producción, presencia de secretos sin mostrarlos, firma válida y certificado exacto. Genera checksum SHA-256 del APK universal y publica como Release estable.
- Acciones actualizadas a revisiones existentes en los workflows del repositorio. La compilación continúa en GitHub Actions.
- Validación local limitada a inspección estática y `git diff --check`; no se ejecutó el workflow de Release ni Gradle en Termux, no se crearon tags/Releases y no se tocó APK instalada, applicationId, keystore o secretos. Pendiente CI y prueba funcional en dispositivo antes de cualquier publicación.

## Estado NOAA e INAMHI — 2026-10-18

- Rama de trabajo: alerta-ec. Cambios locales pendientes; no se ha realizado commit, push, tag ni Release en esta fase.
- NOAA GOES-19: verificadas las fuentes de nubes GEOCOLOR, rayos GLM EXTENT3 y vista amplia GEOCOLOR. El 2026-10-18 a las 13:35 UTC, las imagenes mas recientes tenian unos 24-25 minutos de antiguedad; las tres respondieron HTTP 206 a solicitudes parciales y presentaron firma JPEG valida. Esto no sustituye las pruebas de visualizacion en Android.
- Reproductor NOAA: cambios locales en NoaaSatellitePlayer.kt y MapFragment.kt para reanudar consultas al volver a primer plano, esperar la finalizacion de la carga antes de avanzar y retrasar el primer avance 1200 ms. Pendientes compilacion Kotlin, pruebas de animacion, errores de red, memoria y ciclo de vida en dispositivo.
- INAMHI WRF: la prueba WMS en un punto de prueba urbano devolvió PNG completamente blanco y opaco para precipitacion 3h, incluso con TRANSPARENT=TRUE, BGCOLOR=0xFFFFFF e image/png8. Otras capas devolvieron colores opacos. No interpretar automaticamente blanco como ausencia de lluvia o NoData. Pendiente estrategia segura que no oculte el mapa.
- Mantener observaciones satelitales NOAA separadas de pronosticos WRF INAMHI y Open-Meteo, indicando fuente, fecha, vigencia y cobertura. La aplicacion debe funcionar en Ecuador continental y Galapagos; la cobertura WRF continental no equivale a cobertura nacional.
- Preservar GPS, alertas, SOS, accesibilidad, identidad ALERTA EC, licencias y funcionamiento gratuito sin registro.
- Estado Git previo a documentar: 20 archivos modificados, 19 archivos nuevos y 111 respaldos locales .pre_* sin seguimiento. No agregar respaldos ni archivos no revisados automaticamente.
- La documentacion historica de versiones anteriores se conserva. Verificar versionName/versionCode reales antes de cualquier publicacion. No cambiar applicationId ni firma. Flujo obligatorio: CI -> pruebas en dispositivo -> documentacion -> autorizacion expresa -> Release estable.

<!-- ALERTA_EC_AUTO_END -->


## Protocolo Termux y NOAA — 2026-10-18
- Todos los comandos entregados al usuario deben copiar su salida completa al portapapeles mediante: 2>&1 | string collect | tee /dev/stderr | termux-clipboard-set.
- Usar Fish, un comando por turno y esperar el resultado; salida vacía significa que el comando se ejecutó sin producir texto.
- Crear respaldo antes de cambios, evitar compilaciones locales y no hacer commit, push, tag ni release sin autorización expresa.
- Paso 294: NoaaSatellitePlayer.kt actualizado para consultar imágenes NOAA al reanudar cuando transcurran 15 minutos; respaldo NoaaSatellitePlayer.kt.pre_refresh_294; git diff --check sin errores visibles.
- Pendientes NOAA: recuperación de imágenes fallidas, comprobación real en Android y actualización fiable sin descargas innecesarias.


## NOAA — Correcciones verificadas 294-303 (2026-10-18)
- Paso 294/296: consulta de imágenes al reanudar el mapa si han transcurrido 15 minutos; verificación estática correcta.
- Paso 298/299: registro de imágenes fallidas, salto durante reproducción y detención si ninguna es utilizable; verificación estática correcta.
- Paso 300/301: al cambiar modo se detiene la animación, se cancela la consulta anterior y se ignoran respuestas obsoletas; verificación estática correcta.
- Paso 302/303: mensaje diferenciado para errores de conexión y reinicio del intervalo tras consulta fallida; verificación estática correcta.
- Respaldos: NoaaSatellitePlayer.kt.pre_refresh_294, .pre_failed_frames_298, .pre_mode_switch_300 y .pre_error_message_302.
- Pendiente: revisar cancelaciones, validar conectividad y reproducción real en Android, confirmar cobertura satelital de Ecuador continental y Galápagos.
- No compilar, hacer commit, push, tag ni publicar sin autorización expresa del usuario.


## NOAA — Cancelaciones verificadas (pasos 305-307)
- Se identificó que runCatching capturaba CancellationException en las consultas NOAA y en la carga de imágenes.
- Paso 306: se añadió propagación explícita de CancellationException en ambas operaciones.
- Paso 307: cinco verificaciones estáticas OK; git diff --check sin errores.
- Respaldo: NoaaSatellitePlayer.kt.pre_cancellation_306.
- Pendiente: comprobar comportamiento real en Android, errores de red y disponibilidad/cobertura de imágenes para Ecuador continental y Galápagos.
- Mantener un comando Fish por turno y copiar siempre la salida a Termux mediante string collect, tee y termux-clipboard-set.

## NOAA: avances locales pasos 309-326

- Verificadas por HTTP 200 las fuentes oficiales NOAA GOES-19: GEOCOLOR regional, GLM rayos y GEOCOLOR disco completo; sin descargar los JPEG al teléfono.
- Reproductor con tres modos, cronología, reproducción, pausa, zoom y hora de observación.
- Actualización normal cada 15 minutos y espera mínima de 60 segundos tras fallo; se evita iniciar otra consulta si existe una activa.
- Conservación de imágenes anteriores ante fallos, únicamente si siguen vigentes; caducidad de 6 horas y descarte de fotogramas vencidos.
- Mensaje visible cuando todas las imágenes caducan; se detiene la reproducción.
- Se preservan respaldos locales de los cambios; no se han compilado APK ni publicado versiones nuevas.
- PENDIENTE: revisar selección de zona horaria continental/Galápagos en el reproductor, validar casos de fallo y ciclo de vida en Android, ejecutar CI cuando se autorice y continuar otros pendientes de ALERTA EC.
- IMPORTANTE: las comprobaciones realizadas son estáticas; no equivalen a pruebas de funcionamiento en Android.


## NOAA: zona horaria y límites de Galápagos (pasos 330-340)

- NoaaSatellitePlayer admite y actualiza dinámicamente la hora de Galápagos (Pacific/Galapagos, UTC-6) y Ecuador continental (America/Guayaquil, UTC-5).
- MapFragment reutiliza el GPS existente, actualizado aproximadamente cada cinco segundos, sin iniciar otro servicio de ubicación ni descargar imágenes al cambiar de zona horaria.
- EcuadorTimeFormatter.isGalapagosCoordinates centraliza la detección aproximada. Sus límites son latitud -1.4016 a 1.6644 y longitud -92.0116 a -89.2548.
- Estos límites proceden del recurso existente breezytz_ec.json, que contiene un MultiPolygon de un solo rectángulo de cinco puntos para EC-W; NO representa las costas ni una frontera administrativa precisa.
- BreezyTimeZoneService y sus avisos legales permanecen intactos. Su recurso geográfico tampoco proporciona contornos insulares precisos.
- Pendiente: validar los límites y las islas periféricas, el funcionamiento en Android, la cobertura real de las imágenes NOAA y la presentación de fechas durante cambios de ubicación.
- Estado: cambios locales sin compilación, sin commit, sin push y sin publicación. No considerar la integración validada en ejecución.

## Introduccion vertical: pasos 368-373
- Se incorporo el logotipo vertical original proporcionado por el usuario (864x1536) a los recursos alertaec_logo_acerca.png y alerta_ec_intro_logo.png, con respaldos .pre_vertical_368.
- El XML de introduccion utiliza el logotipo vertical con contenedor 220x390 dp.
- Los posters inicial y final del reproductor utilizan FIT_CENTER.
- Al terminar el video, el logo final permanece 3000 ms y luego se desvanece durante 600 ms.
- stop() cancela la animacion de salida, restablece alpha=1 y oculta la capa.
- Validacion estatica: git diff --check sin errores.
- PENDIENTE: compilacion autorizada y prueba visual en Android; no afirmar que la reproduccion esta validada.
- No se realizo commit, push, compilacion ni publicacion.
