# ALERTA EC — CONTINUAR AQUÍ

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
- Release 0.1.4 / versionCode 104 publicada y firmada.
- Rama alerta-ec; ultimo commit de referencia 7531845b4.
- CI y panel local previamente verificados.
- GPS y geocodificacion Ecuador tienen implementacion inicial.
- Proxima prioridad: GPS 3D, direccion detallada y referencias comunitarias.
- No publicar una nueva Release sin pruebas funcionales.

### RED Y REPETIDOR
- Router/repetidor deben estudiarse tambien como posible origen del problema.
- Repetidor/AP funciona como puente, DHCP deshabilitado y administracion 192.168.100.2.
- Revisar DHCPv4, IPv6 RA/SLAAC, DNS y rutas cuando se pruebe desde el repetidor.
- No desactivar IPv6 como solucion.
- Una red IPv6-only necesita compatibilidad de red para destinos IPv4, por ejemplo NAT64/DNS64/464XLAT cuando corresponda.

### DISTRIBUCION Y POCO ALMACENAMIENTO DEL TELEFONO
- applicationId Release: com.clicktvdigital.alertaec.
- Release instalada y comprobada en el telefono al iniciar este avance: 0.1.3 / versionCode 103, instalada por Obtainium.
- GitHub Releases es la fuente permanente de APK firmadas.
- Obtainium consume las Releases oficiales de clicktvdigital/Alerta-EC.
- Mantener siempre el mismo certificado de firma para permitir actualizaciones.
- Las compilaciones de verificacion pasan a GitHub Actions con un workflow CI en cada push a alerta-ec.
- La Release firmada sigue siendo manual: no publicar una nueva version por cada cambio sin probar.
- Proxima version candidata para distribuir estos cambios: 0.1.4, solo despues de CI verde y pruebas funcionales.
- No acumular APK, builds y logs innecesarios en el telefono.
### SIGUIENTE ACCION EXACTA
1. COMPLETADO: diagnostico integral de GPS, Nominatim, Release y panel local.
2. COMPLETADO: identificar que Nominatim entrega para el punto de prueba road=Angel Araujo, quarter=San Jose, city_district=Cochapamba, village=Atucucho, county=Quito y state=Pichincha.
3. EN ESTE CAMBIO: corregir la jerarquia de Ecuador para que quarter/sector no sustituya a la parroquia en el titulo principal.
4. EN ESTE CAMBIO: mapear direccion detallada como barrio/sector/parroquia/canton/provincia sin mezclar niveles.
5. EN ESTE CAMBIO: activar CI automatica de compilacion Kotlin Release en GitHub para cada push a alerta-ec.
6. EN ESTE CAMBIO: guardar el panel local dentro del repositorio y mantener una copia de ejecucion en ~/alerta-ec-panel.
7. SIGUIENTE: confirmar CI verde en GitHub.
8. SIGUIENTE: probar que la app muestra Atucucho, Cochapamba y que la direccion detallada conserva San Jose solamente como sector si el proveedor lo devuelve.
9. SIGUIENTE: resolver el codigo postal con estrategia multifuente; no sobreescribir 170528 por el 170318 de OSM sin validacion.
10. SIGUIENTE: preparar 0.1.4 prerelease, instalar mediante Obtainium y validar GPS, clima, minutely y FPAS.
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
- Area de validacion: Atucucho, parroquia Cochapamba, Quito, Pichincha. No publicar coordenadas GPS precisas ni la interseccion exacta del domicilio en GitHub.
- La clasificacion municipal vigente consultada en fuentes del Municipio de Quito ubica Atucucho y la parroquia Cochapamba en la Administracion Zonal Eugenio Espejo (Zona Norte), no en la Administracion Zonal La Delicia.
- Zona de Planificacion 9 significa Distrito Metropolitano de Quito; no debe confundirse con una administracion zonal ni con un distrito de salud.
- El MSP ubica Atucucho/Cochapamba en el Distrito de Salud 17D05, denominado La Concepcion a Zambiza.
- El codigo postal 170528 es un codigo postal valido del Distrito Metropolitano de Quito. Su estructura normativa es 17-05-28: provincia Pichincha, distrito postal/administrativo 05, zona postal 28.
- Referencia validada por el usuario en Google Maps y en una aplicacion de Codigo Postal Ecuador: 170528. Ademas existen referencias publicas de servicios y direcciones cercanas que usan 170528.
- Nominatim/OpenStreetMap devolvio 170318 para la coordenada GPS de prueba, por lo que existe una discrepancia de fuente. No asumir que el codigo de OSM es correcto solo por venir del geocodificador.
- Objetivo de presentacion para el caso de prueba: barrio Atucucho; sector San Jose si la fuente lo devuelve como quarter; parroquia Cochapamba; canton Quito; provincia Pichincha; calle segun el proveedor; codigo postal validado por una fuente postal independiente.
- No hardcodear una direccion particular para todos los usuarios. La aplicacion debe resolver dinamicamente por GPS y conservar procedencia/confianza de cada dato.
- Fuentes de verificacion: Municipio de Quito/Zonales (Atucucho y Cochapamba bajo Eugenio Espejo), MSP Distrito 17D05, y Norma Tecnica del Codigo Postal Ecuatoriano.
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
- Zona de prueba: ubicacion GPS actual del usuario en Quito; NO publicar coordenadas precisas en GitHub.
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
- Mantener 170528 como referencia postal validada independientemente por el usuario; Nominatim/OSM devolvio 170318 y debe conservarse como discrepancia de fuente hasta resolverla con estrategia multifuente.

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
- Siguiente validacion: GPS real + Nominatim y comprobar jerarquia Atucucho / Cochapamba / Quito / Pichincha.
- No publicar 0.1.4 hasta completar esta validacion.

<!-- ALERTA_EC_GPS_VALIDADO_014 -->
## GPS/geocodificacion validada y candidata 0.1.4 — 8 de octubre de 2026
- Panel local operativo y arbol Git limpio.
- GPS real y Nominatim validaron para el caso de prueba: calle Angel Araujo; village/barrio Atucucho; quarter/sector San Jose; city_district/parroquia Cochapamba; county Distrito Metropolitano de Quito; provincia Pichincha.
- La jerarquia nueva produce como titulo esperado: Atucucho, Cochapamba.
- El Vademecum oficial del sector postal de MINTEL incluye Quito | 17D05 | 170528 | Cochapamba; tambien existen 170529 y 170530 dentro de Cochapamba.
- 170528 queda registrado como referencia postal validada por el usuario y respaldada por fuentes postales; no hardcodearlo para todos los usuarios.
- Nominatim/OpenStreetMap devolvio 170318 para el punto GPS y se mantiene registrado como discrepancia de fuente.
- Municipio de Quito ubica Atucucho, parroquia Cochapamba, dentro de la Administracion Zonal Eugenio Espejo.
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
- Caso de validacion esperado: Atucucho, Cochapamba como titulo; San Jose separado como sector; Quito y Pichincha en sus niveles administrativos.
- Mantener 170528 como referencia postal validada para Cochapamba/17D05 y 170318 como discrepancia devuelta por Nominatim/OpenStreetMap hasta implementar validacion postal multifuente.
- Siguiente paso: actualizar Alerta EC con Obtainium y verificar version instalada, certificado, direccion mostrada, GPS, clima, minutely y FPAS.

## PLAN GPS 3D Y REFERENCIAS COMUNITARIAS — 2026-10-08

### Objetivo
- Mostrar latitud, longitud, precision horizontal, altitud cuando exista, precision vertical cuando exista, fecha y hora de medicion.
- Distinguir altitud GNSS, elevacion ortometrica y elevacion del terreno; no confundir profundidad con altitud.
- Incorporar coordenadas UTM, pendiente, orientacion de ladera y relieve 3D cuando existan fuentes verificadas.
- Separar calle, interseccion, barrio, sector, parroquia, canton, provincia y codigo postal.
- Conservar procedencia, confianza y antiguedad de cada dato.
- No inventar coordenadas, distancias, alturas ni direcciones.

### Sectores de validacion en Atucucho
- Union La Paz, sector Escuela, El Cisne, Ladera del Cisne, Corazon de Jesus, Condor Mirador, La Campina y San Jacinto.
- Son nombres comunitarios por verificar y delimitar mediante cartografia.
- El sector debe determinarse dinamicamente; no fijar Union La Paz para todos.

### Calles y referencias comunitarias por verificar
- Angel Leonidas Araujo Chiriboga, tambien conocida como Angel Araujo.
- Calle N56D y Zulema Julia Blacio Galarza: nombres y conexiones por contrastar.
- Carpinteria Don Pedro Sarango: referencia aproximada a 10 metros.
- Viveres Solange: referencia aproximada a 20 metros.
- Mecanica de Edison Guallichico: hacia la parte alta.
- Fundacion Pan de Vida: referencia por N56D.
- Viveres Damasio: referencia hacia Zulema Blacio.
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
- Para el caso de referencia, 170528 está confirmado por el usuario y respaldado por fuentes postales oficiales consultadas (incluido vademécum MINTEL: Quito, 17D05, Cochapamba). No generalizarlo ni hardcodearlo. Nominatim/OSM entregó 170318 para la coordenada de prueba; mantener ambos valores y resolver con validación postal/geoespacial independiente.
- Caso de geocodificación previo: barrio Atucucho, sector San José si lo informa el proveedor, parroquia civil Cochapamba, cantón/distrito Quito y provincia Pichincha; la zona municipal indicada en antecedentes es Eugenio Espejo. No confundir parroquia civil con eclesiástica, distrito postal, distrito de salud ni administración zonal.
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
- Intro animada con sonido: pendiente.
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

<!-- ALERTA_EC_AUTO_END -->
