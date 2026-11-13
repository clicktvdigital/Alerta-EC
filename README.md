# Alerta EC

Alerta EC es una aplicación para Android que reúne información meteorológica para Ecuador, con ubicación GPS, mapas, widgets y notificaciones. Su objetivo es ayudar a la prevención y a la respuesta temprana ante fenómenos naturales.

> Alerta EC informa, pero no reemplaza a las autoridades. En una emergencia, llame al **911**.

Es una versión modificada de [Breezy Weather](https://github.com/breezy-weather/breezy-weather). Consulte la sección [Créditos y licencia](#créditos-y-licencia).

## Funciones

**Disponibles**

- Pronóstico diario y horario, condiciones actuales y calidad del aire.
- Ubicación actual por GPS y lista de ubicaciones.
- Widgets para la pantalla de inicio.
- Uso gratuito y sin registro de cuenta.

**En desarrollo**

- Visor de imágenes del satélite NOAA GOES-19 (nubes y rayos).
- Capas del modelo WRF del INAMHI, en validación.
- Widgets con lluvia próxima y acumulación en milímetros.
- Introducción con sonido opcional, desactivado por defecto.

**Planificadas (todavía no disponibles)**

- Motor de alertas por varias amenazas, con notificaciones de sonido, vibración y voz.
- Vigilancia vial y reportes ciudadanos. Un reporte ciudadano nunca se presentará como alerta oficial.
- Botón SOS. Mientras no exista una integración oficial verificada, la aplicación no contactará al ECU 911.

## Fuentes de datos

Cada dato muestra su fuente, fecha y vigencia. Las observaciones satelitales, los pronósticos y las alertas oficiales se presentan por separado para evitar confundirlos.

## Instalación

1. Descargue el APK más reciente desde la sección [Releases](https://github.com/clicktvdigital/Alerta-EC/releases).
2. Verifique el archivo con el checksum SHA-256 que acompaña a la versión.
3. Instale el APK. Si Android lo pide, permita la instalación desde esa fuente.

## Privacidad

La aplicación solicita la ubicación solo cuando la necesita y explica para qué se usa cada permiso. Consulte la [política de privacidad](PRIVACY.md).

## Desarrollo

Requisitos: JDK 21 y Android SDK.

```bash
git clone https://github.com/clicktvdigital/Alerta-EC.git
cd Alerta-EC
git checkout alerta-ec
./gradlew :app:assembleBasicRelease
```

Generar el APK de release requiere la clave de firma. Sin ella, el build de release no se completa. Las contribuciones se describen en [CONTRIBUTING.md](CONTRIBUTING.md).

## Créditos y licencia

Alerta EC es una versión modificada de [Breezy Weather](https://github.com/breezy-weather/breezy-weather), desarrollada por su equipo y colaboradores. Se distribuye bajo la [GNU Lesser General Public License v3.0](LICENSE).

Según la [licencia adicional](LICENSE_ADDITIONAL), esta licencia no concede derechos sobre marcas, nombres ni logotipos de los colaboradores. Esta versión modificada no es la aplicación oficial de Breezy Weather. Las aplicaciones de terceros que usan datos de Breezy Weather están listadas en [3RD_PARTY.md](3RD_PARTY.md).

<!-- ALERTA_EC_AUTO_START -->
**Estado:** versión en desarrollo. El seguimiento interno del proyecto está en [docs/CONTINUAR_AQUI.md](docs/CONTINUAR_AQUI.md).
<!-- ALERTA_EC_AUTO_END -->
