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
- Quito, Atucucho, La Carolina, Valle de Los Chillos y otras zonas configurables.
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
- `.github/workflows/alerta-ec-release.yml` compila Release firmada desde `workflow_dispatch` o el tag `v0.1.4`, usa secretos de firma y publica la Release con nombre/tag de versión fijados en el workflow. No se ejecutó ni se publicó nada. Para futuras versiones debe revisarse ese versionado fijo antes de activar el workflow; no usarlo como prueba de compatibilidad.
- La documentación histórica registra una instalación Obtainium 0.1.3 / code 103 y una Release 0.1.4 / code 104. No se pudo consultar la APK instalada ni inspeccionar los secretos/certificado: la compatibilidad real de firma y actualización sigue sin verificar. Antes de distribuir hay que obtener de forma segura el SHA-256 del certificado de la APK instalada y compararlo con la Release firmada; comprobar mismo applicationId, `versionCode` superior, `versionName`, artefacto estable detectable por Obtainium e instalar encima sin desinstalar conservando datos cuando Android lo permita.
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
