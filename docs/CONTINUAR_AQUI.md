# ALERTA EC — CONTINUAR AQUÍ

<!-- ALERTA_EC_ESTADO_OPERATIVO_INICIO -->
## ESTADO OPERATIVO — LEER PRIMERO

### REGLA DE CONTINUIDAD
- Este archivo es la fuente maestra de continuidad de Alerta EC.
- Al reanudar el proyecto, LEER ESTE BLOQUE ANTES DE MODIFICAR CODIGO.
- No depender de la memoria del usuario ni de una conversacion.
- GitHub `origin/alerta-ec` es el respaldo principal; el telefono es solamente entorno temporal.
- Despues de cada avance importante: actualizar este estado, commit y push a GitHub.
- Evitar compilaciones pesadas en el telefono cuando GitHub Actions pueda realizarlas.
- Salidas grandes: guardarlas temporalmente en Descargas y subir el archivo.
- Nunca subir claves JKS, passwords, tokens, local.properties ni informacion privada.

### ULTIMO ESTADO CONFIRMADO
- Rama: alerta-ec.
- Respaldo confirmado en GitHub: ba61c479b.
- Reparada deteccion de red con NET_CAPABILITY_INTERNET + NET_CAPABILITY_VALIDATED.
- No forzar IPv4 ni IPv6.
- Mantener compatibilidad IPv4, IPv6 y dual-stack.
- Open-Meteo respondio HTTP 200 mediante IPv4 durante las pruebas.
- Cambios pendientes anteriores de Notifications.kt y activity_main.xml fueron incluidos en ba61c479b.
- Falta comprobar la reparacion dentro de una nueva APK Release.

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
