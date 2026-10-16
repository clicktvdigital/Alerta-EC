<!-- ALERTA_EC_INSTALL_GUIDE_V1 -->
# Instalar ALERTA EC 🇪🇨

**Desarrollado por CLICK TV DIGITAL**
Aplicación Android gratuita para información meteorológica, prevención y alertas.
**Sin cuenta obligatoria para las funciones disponibles.**

> **Atención:** ALERTA EC es un proyecto independiente basado en Breezy Weather; sus APK, firmas y canales de actualización son distintos. Para instalar ALERTA EC, utiliza exclusivamente los enlaces de esta sección. Las funciones futuras de seguimiento familiar, rescate comunitario y SOS remoto siguen en desarrollo y no deben considerarse servicios disponibles ni un sustituto del ECU 911.

## Opción recomendada: instalar y actualizar con Obtainium

1. Instala **Obtainium** desde su proyecto oficial: https://github.com/ImranR98/Obtainium/releases
2. Abre Obtainium y selecciona **Añadir aplicación / Add App**.
3. Introduce la URL de origen de ALERTA EC:

   `https://github.com/clicktvdigital/Alerta-EC`

4. Selecciona **GitHub** como origen si Obtainium no lo detecta automáticamente.
5. **No actives «Incluir versiones preliminares / Include prereleases»**: usamos Releases estables para el público.
6. Si se ofrecen varias APK, elige la APK **universal y estable de producción** de ALERTA EC. No utilices APK Debug, archivos `.sha256`, ni APK de otro proyecto. La convención prevista para futuras publicaciones automatizadas es `Alerta-EC-vX.Y.Z.apk`; confirma el nombre disponible en cada Release.
7. Añade la aplicación y sigue las indicaciones de Android para instalarla. No es necesario iniciar sesión en GitHub.

### Actualizaciones automáticas en Obtainium

- En **Ajustes de Obtainium**, revisa el **intervalo de comprobación en segundo plano** y habilita las actualizaciones automáticas según las opciones de tu versión.
- Comprueba en la ficha de **ALERTA EC** que no esté excluida de las actualizaciones en segundo plano.
- Autoriza a Obtainium para **instalar aplicaciones** cuando Android lo solicite.
- En Android 12 o posterior, Obtainium puede instalar ciertas actualizaciones en segundo plano cuando se cumplen las condiciones del sistema: versión actual instalada por Obtainium, APK seleccionada sin ambigüedad, versión objetivo compatible y permisos/configuración adecuados. No está garantizado en todos los dispositivos; Android podría solicitar confirmación.
- Las comprobaciones son periódicas y Android puede retrasarlas por ahorro de batería o restricciones del fabricante. No significa que se actualice en el mismo segundo en que aparece una Release.

Documentación oficial: https://wiki.obtainium.imranr.dev/app_tracking/

## Alternativa: instalación manual desde GitHub

1. Abre la página oficial de Releases: https://github.com/clicktvdigital/Alerta-EC/releases
2. Elige una Release **estable** y comprueba el nombre de su APK de producción. No instales una compilación Debug, preliminar ni un artefacto de GitHub Actions.
3. Descarga el archivo `.apk`, ábrelo y acepta únicamente los permisos de instalación legítimamente solicitados por Android.
4. Para gestionar futuras actualizaciones, añade después el repositorio a Obtainium. **Las instalaciones realizadas desde Descargas pueden requerir confirmación para actualizarse**. No desinstales una instalación existente sin respaldar primero los datos: podrías perderlos.

## Avisos de seguridad de Android y Play Protect

Android puede pedir analizar o verificar una APK instalada fuera de Google Play. Este aviso por sí solo no demuestra que la aplicación sea peligrosa, pero **no es recomendable desactivar Play Protect ni ignorar una advertencia de riesgo específico**. Comprueba que la APK proceda de la Release oficial y verifica los hashes cuando estén publicados. Si un aviso indica bloqueo o riesgo, detén la instalación y solicita ayuda antes de continuar.

## Compatibilidad de futuras actualizaciones

Para actualizar la app existente sin perder la instalación, la nueva versión debe mantener el **applicationId** de producción `com.clicktvdigital.alertaec`, una **firma de producción compatible** y un **versionCode** adecuado. La verificación criptográfica de la firma y las pruebas de actualización entre versiones están pendientes; esta guía no las declara completadas.

## Ayuda y reporte de fallos

- Problemas técnicos y errores: https://github.com/clicktvdigital/Alerta-EC/issues
- Consultas privadas, especialmente sobre datos personales: `clicktvdigital@gmail.com`
- No compartas públicamente coordenadas exactas, información de menores, credenciales o grabaciones sensibles en GitHub Issues.
- GitHub Discussions solo se anunciará después de comprobar que esté habilitado.

## Procedencia, licencias y créditos

ALERTA EC es un proyecto derivado no oficial de **Breezy Weather**: https://github.com/breezy-weather/breezy-weather. Conserva las atribuciones a autores y colaboradores y las licencias correspondientes, incluida LGPL-3.0. El proyecto original y sus repositorios F-Droid **no distribuyen necesariamente las APK de ALERTA EC**.

---

## Instrucciones históricas de Breezy Weather (solo referencia de procedencia)

<details>
<summary>Mostrar documentación histórica del proyecto original (NO instala ALERTA EC)</summary>

> **Aviso:** todos los enlaces de esta sección corresponden a Breezy Weather, no a ALERTA EC. No los uses para instalar nuestra aplicación.

# Simple instructions

Go to [Releases page](https://github.com/breezy-weather/breezy-weather/releases) and download the file with the following format `breezy-weather-vX.Y.Z_standard.apk`.

Install it and you’re done!

After adding your first location, you will be asked if you want to be notified of app updates. We highly recommend you enable it.


# Detailed instructions

## Flavors

The recommended flavor of **Breezy Weather** is the standard version. It is fully open source and contains no proprietary components.

For specific needs, we also offer a flavor with only free-network sources (libre and self-hostable): Open-Meteo, Pirate Weather, Bright Sky (DWD), Recosanté and ClimWeb (used by many African countries).

Both flavors are signed with the same signature, so you can easily try/switch between both.


## Sources to get Breezy Weather from

**Breezy Weather** releases are available from the following sources:
- **[GitHub releases](https://github.com/breezy-weather/breezy-weather/releases)** is where releases built by GitHub are published under APK format. Any Android device can install APK files without needing any particular app. If you have a GitHub account, you can subscribe to be notified of updates, however it’s more convenient to use a store app to track updates. Due to technical limitations, this is also the only source to provide architecture-specific APKs, although the difference between them and the universal APK is of a negligible 2 MB, so it should not be a criteria of choice.
- **[Breezy Weather’s F-Droid repositories](https://github.com/breezy-weather/fdroid-repo/blob/main/README.md)** are maintained by Breezy Weather developers and get updates from a F-Droid client that doesn’t support receiving updates from GitHub.
- **[Izzy F-Droid repository](https://apt.izzysoft.de/fdroid/index/info)** offers the standard flavor which is our recommended choice if you would like someone to independently review the app before it gets published. Updates are fast (less than 24 hours).
- **[F-Droid default repository](https://f-droid.org/packages/org.breezyweather/)** offers the flavor with only free-network sources. Updates require someone on F-Droid team to manually add new versions, as autoupdates cannot be enabled for technical reasons. Since November 2025, after this manual action, updates are made available faster than it used to be in the past. If you decide to use this source and you want to report an issue, you will be asked to update to the latest version before making the report if it happens to be lagging behind.

| Differences                | GitHub releases | [F-Droid repo] Breezy Weather | [F-Droid repo] Izzy    | [F-Droid repo] Default     |
|----------------------------|-----------------|-------------------------------|------------------------|----------------------------|
| Available flavors          | All             | All                           | Standard               | Free network sources-only  |
| Pre-releases               | Optional        | Optional                      | ❌                      | ❌                          |
| Delay for updates          | Immediate       | Immediate                     | Every day at 18:00 UTC | Depends on a manual action |
| APK matches source code    | ✅               | ✅                             | ✅                      | ✅                          |
| Independently reviewed     | ❌               | ❌                             | ✅                      | ✅                          |
| Architecture-specific APKs | ✅               | ❌                             | ❌                      | ❌                          |


### Other not supported well-known sources

- Google Play Store:
  - Costs money
  - Is privacy invasive for the developer (requires sending your ID and giving your phone number)
  - We don’t [comply with Google Play policy](https://github.com/breezy-weather/breezy-weather/issues/31)
- Accrescent: waiting for it to become stable (no ETA announced by upstream)


## Client configuration instructions

### Obtainium

[Link to Obtainium page](https://github.com/ImranR98/Obtainium/blob/main/README.md)

#### Getting updates from GitHub releases

In the “Add App” screen:
1. Add the following URL: `https://github.com/breezy-weather/breezy-weather`
2. To receive updates for prereleases, enable “Include prereleases”
3. (Optional) If you want the flavor with only free network sources, add `freenet` in the “Filter APKs by Regular Expression”
4. Tap the “Add” button at the very top, and you’re done!

#### Getting updates from a F-Droid repository

In the “Add App” screen, just add as App Source URL the following URL depending on the repository you want to use:
   - Standard flavor from Izzy repo: `https://apt.izzysoft.de/fdroid/index/apk/org.breezyweather`
   - Standard flavor from Breezy Weather repo: configure Obtainium to use GitHub releases instead (see previous section)
   - Free-net flavor from Breezy Weather repo: configure Obtainium to use GitHub releases instead (see previous section)
   - Free-net flavor from default F-Droid repo: `https://f-droid.org/packages/org.breezyweather/`

Tap the “Add” button at the very top, and you’re done!


### F-Droid client

1) Look for the Repositories option from your F-Droid client and add a new repository depending on the source you want to use:
   - Standard flavor from Izzy repo: `https://apt.izzysoft.de/fdroid/repo`
   - Standard flavor from Breezy Weather repo: `https://breezy-weather.github.io/fdroid-repo/fdroid/repo`
   - Free-net flavor from Breezy Weather repo: `https://breezy-weather.github.io/fdroid-repo/fdroid-version/fdroid/repo`
   - Free-net flavor from default F-Droid repo: should already be enabled by default on your F-Droid client

2) After adding the repository, go to the app details and make sure to select which repo you want to get updates from to avoid cross-updates between flavors and repos. This is how you do it:
![F-Droid preferred repo feature](docs/fdroid_client_config.png)

</details>
