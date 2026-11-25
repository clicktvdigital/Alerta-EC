# Cambios de ALERTA EC

Registro en español de las compilaciones de ALERTA EC (rama `alerta-ec`). Lo más reciente va arriba.
Los 4 perfiles de APK de cada compilación se describen en [README.md](../README.md#descargas-directas).

## v0.1.22 · compilaciones del 24 y 25-11-2026

### Descargas y compilación
- Descargas directas en GitHub Pages con una página corta por perfil (vista previa para WhatsApp/redes y descarga automática). Los 4 APK van firmados con la clave de producción y se verifica el certificado SHA-256 antes de publicar. No se crean etiquetas ni Releases (`.github/workflows/alerta-ec-descargas.yml`, `scripts/build_download_site.py`).
- La clave pública de Supabase (`ALERTAEC_SUPABASE_ANON_KEY`) se inyecta desde GitHub Secrets en CI, Release y Descargas. Gradle rechaza claves `service_role`/`sb_secret_` y avisa si la clave falta.

### Notificaciones, sonido y voz
- La notificación fija del clima usa un canal silencioso nuevo (`widget_silent_v2`) y el canal antiguo con tono genérico de Android se elimina.
- Al actualizar con novedad (niebla, lluvia, tormenta, granizo, viento o nieve) suena el tono propio de ALERTA EC del fenómeno y luego la voz, p. ej. «Estamos con niebla ahorita en Quito. 12 grados. Usa luces bajas.» Una sola vez por cambio (no se repite en 2 horas).
- Cada notificación o evento se dice en voz alta una sola vez.
- Vienen activados por defecto: notificación del clima por horas, temperatura en la barra de estado, sensación térmica, sonido y vibración de lluvia e iconos monocromos.

### Inicio y pronóstico
- Tarjeta «Lluvia ahora» con un índice de riesgo de 0 a 100 (Tranquilo, Atento, Precaución o Peligro) y el botón «¿Por qué este nivel?» con sus razones: probabilidad, milímetros previstos, minuto a minuto, pluviómetros EPMAPS, INAMHI WRF y reportes de la comunidad.
- Botones de estilo neón transparentes con foco visible. Los de «Abrir mapa» y «Colaborar» se apilan en celulares para que no se corten.
- Pronóstico diario extendido a 16 días con Open-Meteo después del último día de MET Norway.
- Bloque de reloj por defecto, recomendaciones de abrigo, calor y conducción, y guía de intensidad alineada.
- Unidades de Ecuador por defecto (°C, mm, km/h, km, hPa), con una explicación y un ejemplo práctico en cada bloque de «Apariencia → Unidades».

### Mapa y satélite
- Mapa centrado en Ecuador, con flechas, zoom con OK, retroceso por capas y foco neón para el control remoto.
- Capas INAMHI de lluvia 3 h, humedad 1 h, temperatura 1 h y 3 h y viento, con reproducción y pausa, leyenda de colores y «Capa zona».
- Satélite GOES-19 (NOAA) en modos Nubes, Lluvia, Rayos y Amplia, con tamaño compacto en TV y equipos con poca memoria.

### Comunidad (Supabase)
- Reportes comunitarios con severidad (Leve, Moderada, Alta o Crítica), lista de reportes cercanos y denuncias. Tres denuncias ocultan un reporte.
- Solo usa la clave pública. Las reglas RLS y los límites anti-abuso están en `docs/supabase/alerta_ec_comunidad.sql`.
- Las coordenadas se redondean a unos 1 km y solo se envían si el usuario lo marca.

### Autosoporte
- Si un dato no trae fecha propia, se usa la fecha real de la última actualización que lo guardó, así que ya no aparece «No consta fecha de adquisición verificable». Si los datos están viejos, el autosoporte pide una actualización segura por sí mismo.
- En Android TV, la falta de GPS ya no cuenta como fallo.

### Televisión y arranque
- Intro adaptada a cada perfil (TV FHD, TV Box HD y celular vertical), banner 16:9 de TV y navegación completa con el control remoto (D-pad).
- Fuentes de Ecuador (EPMAPS e INAMHI) en la cabecera y actualización inmediata al elegir una ubicación.
