# ALERTA EC — DOCUMENTO MAESTRO DE CONTINUIDAD

## Identidad
- App: Alerta EC
- Marca: CLICK TV DIGITAL
- Autor: Ing. Richard Ontaneda
- Base técnica: fork de Breezy Weather
- Licencia heredada: LGPL-3.0; conservar atribución correspondiente.
- Repositorio: clicktvdigital/Alerta-EC
- Rama de desarrollo: alerta-ec
- Ruta Termux: ~/breezy-weather
- Application ID estable: com.clicktvdigital.alertaec
- Application ID debug: com.clicktvdigital.alertaec.debug
- Namespace heredado: org.breezyweather

## Forma de trabajo
El desarrollo se realiza principalmente desde Termux en Android.
Dar UN SOLO COMANDO por turno y esperar el resultado antes de continuar.
El shell es Fish: evitar sintaxis exclusiva de Bash y heredocs.
No realizar clean builds salvo que sea estrictamente necesario.
No cambiar configuraciones de memoria innecesariamente.
No afirmar que una función está terminada hasta compilarla/probarla.

## Entorno
- Java 21
- Gradle wrapper 9.7.1
- compileSdk/targetSdk 37
- minSdk 24 actualmente
- Arquitecturas de teléfono objetivo: arm64-v8a y armeabi-v7a.
- x86/x86_64 pueden conservarse para desarrollo/emulación.
- ARM64 es la arquitectura principal.
- ARMv7 se mantiene para dispositivos compatibles.
- La compatibilidad no debe decidirse únicamente por número de núcleos o año del teléfono.

## Versión propia
Se inició migración desde la numeración heredada de Breezy:
- versionCode: 100
- versionName: 0.1.0
Debug añade sufijo -r<commitCount>.
Antes de distribución estable debe existir una clave Release permanente y protegida.
No usar firma debug como canal definitivo de actualizaciones.

## Compilación
Compilación Kotlin:
./gradlew :app:compileBasicDebugKotlin --no-daemon --max-workers=1

APK completo en Termux:
./gradlew :app:assembleBasicDebug --no-daemon --max-workers=1 --console=plain -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2

Existe comando auxiliar:
alerta-build

APK ARM64 de prueba:
~/storage/downloads/Alerta-EC/APK/Alerta-EC-arm64-v8a-debug.apk

Carpetas visibles:
~/storage/downloads/Alerta-EC/APK
~/storage/downloads/Alerta-EC/Builds
~/storage/downloads/Alerta-EC/Logs
~/storage/downloads/Alerta-EC/Documentacion
~/storage/downloads/Alerta-EC/Mapas
~/storage/downloads/Alerta-EC/Backups
~/storage/downloads/Alerta-EC/Pruebas

## ADB
ADB está disponible desde Termux.
Dispositivo usado durante desarrollo: emulator-5554 cuando está conectado.
Antes de instalar comprobar siempre:
adb devices

Instalación/actualización debug prevista:
adb -s emulator-5554 install -r ~/storage/downloads/Alerta-EC/APK/Alerta-EC-arm64-v8a-debug.apk

No ejecutar instalación hasta comprobar que el dispositivo correcto está conectado y que la firma instalada es compatible.

## Distribución
Objetivo: GitHub Releases + Obtainium.
Obtainium debe poder detectar nuevas versiones publicadas.
Android normalmente requiere confirmación del usuario para instalar APK externos; no prometer actualización silenciosa.
Mantener versionCode siempre creciente.
Mantener applicationId y firma Release permanentes.

## Ubicación
Implementado:
- ActiveLocationService
- ActiveLocationController
- LocationSharingSessionManager
- sensores del dispositivo
- GPS + altitud + precisión + rumbo
- geocodificación detallada con Nominatim como componente online
- MapLibre como base de mapas.

La ubicación visible heredada de Breezy todavía puede mostrar una zona antigua/general como Cotocollao. NO afirmar que esto está solucionado hasta conectar/probar el GPS nuevo con la interfaz visible.

## Mapas offline
MapLibre 13.6.1.
PMTiles v3 administrado por la aplicación.
Implementados:
- OfflineMapStorage
- PmTilesValidator
- cliente HTTP dedicado
- OfflineMapDownloader
- descargas reanudables
- control de espacio
- cancelación segura
- instalación del archivo descargado.

Pendiente importante: fortalecer reanudación con ETag/Last-Modified/If-Range para evitar mezclar versiones diferentes de un archivo PMTiles.

## Concepto nacional
Alerta EC está orientada a Ecuador.
Debe admitir:
- provincias
- cantones
- parroquias
- sectores/barrios cuando exista cartografía fiable
- búsqueda por nombre
- ubicación GPS
- comparación entre lugares.

En Quito se desea una vista detallada DMQ por zonas (Atucucho, Roldós, Pisulí, Pomasqui, San Antonio/Mitad del Mundo, Calderón, Comité del Pueblo, La Bota, Guayllabamba, Solanda, Chillogallo, Quitumbe y otros), sin asumir que toda la ciudad tiene el mismo clima.

## Meteorología y recomendaciones
Objetivo de UX:
1. ¿Qué hago hoy?
2. ¿Cómo me preparo mañana?
3. Preparar mi viaje.

Traducir datos técnicos a lenguaje cotidiano.
Ejemplos condicionales:
- lluvia: paraguas/impermeable
- frío: abrigo/capas
- frío + viento: bufanda/protección adicional
- UV elevado: sombra, sombrero/gorra, protector solar e hidratación
- calor: hidratación/sombra
- mala calidad del aire: reducir exposición/actividad intensa y recomendaciones apropiadas según riesgo
- tormenta eléctrica: refugio cerrado y evitar zonas expuestas.

Las recomendaciones pueden combinarse.
Nunca inventar condiciones que la fuente no proporcione.

## Datos horarios confirmados
El modelo Hourly dispone o puede completarse con:
- temperatura
- sensación térmica
- precipitación
- probabilidad de precipitación
- tormenta
- lluvia
- nieve/hielo cuando aplique
- viento/ráfagas
- humedad
- punto de rocío
- presión
- nubosidad
- visibilidad
- UV
- calidad del aire.

Esto permitirá evaluar franjas como mañana 06:00–08:00 para correr, viajar o realizar actividades.

## Calidad del aire
Breezy ya dispone de:
- AQI
- PM2.5
- PM10
- O3
- NO2
- SO2
- CO
- contaminante principal
- seis niveles de riesgo
- descripciones sanitarias.

Umbrales actuales de PollutantIndex:
0, 20, 50, 100, 150, 250.

No presentar AQI como porcentaje de contaminación.
Si se usa porcentaje, etiquetarlo únicamente como indicador visual de riesgo.
UX objetivo: primero explicar qué significa y qué hacer; después mostrar AQI/contaminantes/datos técnicos.

## Alertas
Actualmente se observó en la app instalada:
OMM Centro de Información de Tiempo Severo (Alertas): Solicitud caducada.

Debe reemplazarse la exposición de errores técnicos por mensajes comprensibles y conservar la última información válida cuando corresponda.
Las alertas oficiales importantes deben tener prioridad sobre consejos cotidianos.

## Sismos y volcanes
Fuentes previstas:
- IG-EPN principal para Ecuador
- USGS como complemento para sismos cuando corresponda.

NO predecir terremotos.
Nunca decir que ocurrirá un sismo en una fecha/hora futura.
Mostrar actividad reciente, información oficial y preparación preventiva.
Para volcanes, utilizar estado/alertas oficiales cuando se integre la fuente.

## Viajes
Futuro módulo Preparar mi viaje:
- destino
- fechas
- franjas horarias
- actividades
- recomendaciones de equipaje/ropa/protección
- pronóstico
- alertas disponibles.

Ejemplos: Costa/playa, Cayambe, Cotopaxi, Chimborazo y cualquier lugar de Ecuador.
No llamar segura a una ruta o actividad sin datos suficientes y actuales.

## SOS/comunidad
Diseño previsto:
- SOS con pulsación prolongada
- 911 mediante flujo permitido por Android
- compartir ubicación con contactos autorizados
- sesiones temporales/revocables
- comunidad con reportes corroborables.

Nunca publicar GPS exacto de una persona como dato comunitario.
Nunca afirmar que un reporte ciudadano es una alerta oficial.
No crear seguimiento secreto.

## Trabajo actual
Se creó:
app/src/main/kotlin/org/breezyweather/domain/weather/advice/

SIGUIENTE OBJETIVO:
Crear el motor propio de recomendaciones de Alerta EC y conectarlo posteriormente a datos Hourly/Daily.
Debe producir consejos para hoy, mañana y viajes sin sustituir los datos técnicos.

## Git — commits relevantes
ca4b87c90 Add MapLibre map foundation
105bd53c6 Add offline PMTiles storage
3a32005b2 Add atomic offline map installation
1a3d5e706 Add dedicated offline map download client
6e5c9407e Validate offline PMTiles downloads
01b854233 Add offline PMTiles downloader
ebb1e1f77 Check storage before offline map downloads
dd512f083 Protect storage during offline map downloads
7fd0f6f3f Cancel offline map downloads safely
304443e14 Track partial offline map downloads
8d6cc4e74 Add resumable offline map downloads

## Regla para otra IA
Antes de modificar el proyecto:
1. Leer completamente este documento.
2. Ejecutar git status --short --branch.
3. Ejecutar git log -5 --oneline.
4. No borrar cambios existentes.
5. Verificar el código antes de afirmar que una función existe.
6. Dar al usuario un solo comando Termux por turno.
7. Compilar después de cambios funcionales.
8. Mantener atribución/licencia de Breezy.
9. No inventar fuentes, APIs, alertas ni capacidades.
10. Actualizar este documento cuando cambie significativamente el estado del proyecto.
