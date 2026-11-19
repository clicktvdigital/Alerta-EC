# Continuar aquí — Alerta EC

Actualizado: 2026-11-19 · Versión en preparación: **v0.1.15 (versionCode 116)**

## 1. Estado actual

- **Repositorio:** `clicktvdigital/Alerta-EC`. Android nativo en Kotlin (Views + Compose), no Flutter.
- **Rama de release:** `release/v0.1.15`. Unifica la línea publicada `v0.1.14` (code 114) con la rama de TV `test/alerta-ec-tv-panoramico` (0.1.13-tv, code 115). PR #2 hacia `alerta-ec`, en borrador.
- **Release publicado en GitHub:** `v0.1.14`. El tag `v0.1.15` **todavía no existe**: se crea tras validar en el TV y fusionar el PR #2.
- **Verificación local:** `python3 scripts/validate_alerta_ec.py`. En Termux no hay Gradle/aapt2; la compilación y las pruebas corren en GitHub Actions.

## 2. Qué se hizo en v0.1.15

- **Fusión:** 13 conflictos resueltos. Detalle en el commit de merge `a298afa`.
- **TV — horas y días:**
  - Las celdas del pronóstico por horas y por días son destinos del D-pad; IZQ/DER las recorren con scroll horizontal automático.
  - OK abre el detalle de esa hora o día, igual que tocar en el celular.
  - ABAJO ya no queda atrapado en la lista de tendencias.
- **TV — foco:** anillo azul marino + ámbar. El blanco anterior no se veía sobre tarjetas blancas.
- **TV — Ajustes:** botón en la barra superior (ARRIBA desde la primera fila). En paneles MStar la tecla MENÚ la captura el televisor y nunca llega a la app.
- **Alertas:** `FLAG_KEEP_SCREEN_ON` solo mientras haya una alerta vigente, para que el salvapantallas (5 min en el TV de prueba) no la tape.
- **Gráficos:**
  - Splash, inicio y cierre del intro de TV usan un solo JPG mediante alias (−432 KB).
  - Se retiraron 6 PNG de marcas sin uso.
  - Banner de TV de 640×360. El de v0.1.14 (1280×720) quedó archivado fuera del repo.
- **CI:** `alerta-ec-release.yml` y `alerta-ec-release-test.yml` publican y verifican un APK por ABI (`armeabi-v7a`, `arm64-v8a`). Antes esperaban un APK universal que ya no se genera.
- **Repo:**
  - `ultimo_log_tv.txt` y `.claude/skills/` salen de git (siguen en disco, ignorados).
  - `CLAUDE.md` corregido.
  - README público; la bitácora pasa a `docs/HISTORIAL_README.md`.

## 2b. Segunda etapa (misma fecha)

- **Primer uso en TV:**
  - con la lista vacía, el foco va a «Añadir lugar nuevo»;
  - ABAJO lleva de la búsqueda a los resultados;
  - el panel de Lugares se pliega tras agregar el primer lugar.

  Antes, en un TV recién instalado no se podía agregar ningún lugar con el control.
- **Barra en TV:** `menu-w640dp/activity_main.xml` (heredado de Breezy) reemplazaba al menú en pantallas anchas. Ahora incluye Ajustes y Centro de Emergencias.
- **Mapa / satélite NOAA:**
  - Nubes (GeoColor), Rayos (GLM) y Lluvia (Sandwich: nubes de tormenta, no mm) **encuadrados en Ecuador continental**: se decodifica solo esa ventana de la imagen de 3600×2160 (≈0,5 MP).
  - Mapa completo = sector norte de Sudamérica.
  - Galápagos queda fuera del sector.
- **Home:** ícono grande de cielo (128 dp) según día/noche y condición: sol, sol con nubes, luna, lluvia o tormenta. Animación corta, no continua.
- **Intro por perfil** (`IntroPlaybackPolicy.videoProfile`):
  - TV con ≥ 1,5 GiB → 1920×1080 (`alertaec_cinematic_intro_tv_fhd.mp4`, 6,4 MB);
  - TV Box de ~1 GB o `isLowRamDevice` → 720p;
  - celular → vertical 720×1280. **Falta un master vertical 1080×1920**: no se escala para no inflar el APK sin ganar detalle.
- **Alertas por defecto en Ecuador:** fuente WMO Severe Weather, donde INAMHI publica sus avisos CAP (verificado: `ec-inamhi-es` con avisos vigentes). Antes el preset usaba AccuWeather sin clave y quedaba «Ninguna». Las notificaciones de alertas ya estaban activas por defecto.
- **Pantalla encendida:** con eventos Alerta EC de nivel WARNING o superior (oficiales y de pronóstico).
- **Código postal: investigación con la fuente oficial (2026-11-19).**
  - Fuente: WFS público de la Agencia de Regulación y Control Postal, `https://www.codigopostal.gob.ec/geoserver/arcp_cp/ows`, capa `arcp_cp:zona_postal`, geometría `shape`, EPSG:4326.
  - **170528** = Pichincha (DPA 17), Quito (DPA 1701), distrito de planificación **17D05**, que agrupa Concepción, Mariscal Sucre, Belisario Quevedo, San Isidro del Inca, Rumipamba, Kennedy, Nayón, Iñaquito, Cochapamba, Jipijapa y Zámbiza. 27,44 km² y 59.852 habitantes; caja lat −0,165…−0,117, lon −78,568…−78,496.
  - Puntos de Atucucho (centroide del barrio y calle Ángel Araujo) → **170528** según la consulta punto-en-zona.
  - **170318 no existe** en el catálogo oficial. Es un código que figura solo en OpenStreetMap, y Nominatim lo devuelve para Atucucho por error.
  - Nominatim ubica el barrio Atucucho en la parroquia **Cochapamba** (−0,1290, −78,5137), zona horaria `America/Guayaquil` (UTC−5).
  - Los sectores **La Unión** y **La Paz** de Atucucho no están en OSM: solo se pueden registrar como referencia local aportada por el usuario.
- **Por qué un código postal no basta:** la zona 170528 mide 27 km² y tiene ~60.000 habitantes; en Quito puede llover en una ladera y hacer sol a 2 km.
  - El pronóstico de la app ya se pide por **coordenadas** (Open-Meteo por punto), nunca por código postal.
  - La dirección se afina con capas: GPS → Nominatim (calle, barrio, parroquia) → zona postal oficial → corrección local del usuario (sector, calle transversal, referencia; solo a menos de 75 m del punto) → posición polar en español.
- **Implementado:**
  - `EcuadorPostalZoneService`: punto → zona oficial, con reintentos y caché.
  - `EcuadorPolarReference`: distancia y rumbo de 8 puntos en español desde la capital provincial más cercana (Atucucho: ~10 km al Norte de Quito centro).
  - Nominatim con `es` dentro de Ecuador.
  - Formulario «Corregir mi zona» con calle transversal y referencia local. El código oficial prevalece si el aportado es de otra provincia.
- **Privacidad:** el repo es **público**. La dirección exacta del domicilio (calles) se configura solo en el dispositivo, nunca en el código ni en la documentación.
- **«Ubicación actual» en TV:** el TV no tiene GPS y su ubicación por red no da posición (sin «last known»). Si GPS/red fallan o están apagados, se usa IP (ip.sb), marcada como proveedor «ip» con radio de 5 km (nivel ciudad). El barrio exacto en TV requiere «Introducir coordenadas GPS».
- **ECUWATER:** no aparece en el código, la documentación ni en fuentes públicas (búsqueda web 2026-11-19). Respaldo vigente:
  - alertas oficiales INAMHI vía WMO;
  - capas WRF de INAMHI;
  - pronóstico Open-Meteo por coordenadas;
  - eventos Alerta EC derivados del pronóstico.
- **Capas INAMHI:** opacidad 0,30/0,20 → 0,70/0,65/0,55 y leyenda oficial (GetLegendGraphic JSON) con valores sobre su color. INAMHI no publica capa de granizo; el granizo llega como código de tiempo y alerta.
- **Recomendaciones:** `AlertaEcRecommendations` ya era dinámico (lluvia/intensidad, probabilidad, sensación térmica y hora, UV, ráfagas, granizo, tormenta). Nuevo `AlertaEcSeismicProtocol`: base de sismos antes/durante/después según magnitud, distancia y profundidad, con paso de tsunami. Pendiente conectar una fuente sísmica (IG-EPN).
- **Aviso sobre otras apps (YouTube):** factible. El TV tiene «Acceso especial › Mostrar sobre otras apps» (`SystemAlertActivity`). Requiere declarar `SYSTEM_ALERT_WINDOW` y que el usuario lo active.
  - Recomendado solo como opción, solo para WARNING/EMERGENCY, banner breve que no tape el video entero, con cierre por Atrás/OK.
  - No se implementó todavía.
- **Amaru («YAMARU»):** sin repositorio ni API públicos. Solo ideas de producto (índice de riesgo por colores, aviso solo cuando sube el riesgo).
- **INAMHI WMS:** de 276 capas, las útiles siguen siendo WRF de precipitación 3 h/24 h (ya integradas). No hay radar ni rayos.

## 3. Celular vs TV / TV Box

Se mantiene **un solo módulo y un solo APK por ABI**. La lógica (alertas, datos, mapas) es común; la presentación se separa con calificadores de recursos (`-television`, `-w640dp`) y con la detección de TV en tiempo de ejecución.

| | Celular | Android TV / TV Box |
|---|---|---|
| Orientación | Vertical | Horizontal (720p/1080p) |
| Entrada | Táctil / deslizar | D-pad + OK |
| Intro | `raw/alertaec_cinematic_intro.mp4` (720×1280) | `raw/alertaec_cinematic_intro_tv.mp4` (1280×720) |

Mejora posible: mover cada video a `raw/` y `raw-television/` con el mismo nombre para quitar el `if` del código. Ambos seguirían en el APK; solo un flavor separado ahorraría ~3 MB.

## 4. Herramientas y skills

- **Grafo:** `graphify` (CLI y skill). Usar `graphify query "<pregunta>"` antes de buscar en el código y `graphify update .` después de editar (solo AST, sin costo de API). `.graphifyignore` excluye `.claude/skills/`.
- **Memoria:**
  - plugin `claude-memory` (`structured-memory`);
  - memoria de Claude Code en `~/.claude/projects/.../memory/` (entrada `alerta-ec-tv-entorno`);
  - este archivo.
- **Diseño/UI (local en `.claude/skills/`, ignorado por git):** `emil-design-eng`, `ask-emil`, `learn-taste`, `design-taste-frontend`, `apple-design` e `impeccable` (enlace a la copia del launcher).
- **Proceso:** superpowers (`systematic-debugging`, `verification-before-completion`, `writing-plans`).

## 5. Ramas

| Rama | Estado |
|---|---|
| `release/v0.1.15` | Release unificado (PR #2) |
| `alerta-ec` | Rama por defecto; recibe el PR #2 |
| `test/alerta-ec-tv-panoramico` | Rama de TV previa; ya incluida en v0.1.15 |
| `chore/tv-graficos-consolidados` | Commit `fd61b14`; ya incluido en v0.1.15 |
| `respaldo/breezy-wip-20261119` | Trabajo sin commit que estaba en `~/breezy-weather` (reseñas, ubicación detallada, mapa). **No validado.** Revisar e integrar o descartar |

## 6. Prueba en TV (SWTV-20SA, Android 11, MStar, `armeabi-v7a`)

- ADB: `adb connect 192.168.100.11:5555`.
- `screencap` antepone texto al PNG: recortar hasta la firma `\x89PNG`.
- Las capturas y grabaciones no reflejan el post-procesado del panel MStar.
- Evidencias en `/sdcard/Download/alerta_ec_tv*` (TV y teléfono) y en `~/ALERTA_EC_ARCHIVO_20261119/`.

<!-- RESULTADOS_TV_V0115 -->

## 7. Próximos pasos

1. Validar en el TV el release firmado de v0.1.15: horas/días con OK, foco visible, Ajustes desde la barra, intro y Atrás.
2. Fusionar el PR #2 y crear el tag `v0.1.15`. El workflow publica los dos APK firmados.
3. Retirar los logs de diagnóstico temporales (`AlertaEcStartup`, `AlertaEcKeys`, `AlertaEcFocus`) antes del release definitivo, o dejarlos solo en debug.
4. Arranque en frío: ~0,9 s en `super.onCreate` y ~2 s hasta "adapter attached" en el TV. Diferir trabajo fuera del hilo principal. Valorar mostrar el intro solo en el primer arranque en equipos de poca RAM.
5. Revisar la rama `respaldo/breezy-wip-20261119` y archivar `~/breezy-weather` cuando se integre.
6. Ejecutar `~/ALERTA_EC_ARCHIVO_20261119/PLAN_BORRADO.sh --si` (libera ~758 MB). El `.tar` de datos en claro solo se borra si el `.enc` descifra al mismo SHA-256 (pide la contraseña).
