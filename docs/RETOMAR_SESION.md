# Retomar la sesión: ALERTA EC (TV y teléfono)

Este archivo es el punto de entrada para cualquier sesión nueva, con Claude u otra IA. Léelo antes que el resto de la documentación.

## Estado actual

- Repositorio: `clicktvdigital/Alerta-EC`.
- Rama de trabajo: `claude/clima-launcher-por-defecto`.
- Versión: `0.1.12` (`versionCode` 112).
- Último commit en la rama: `b7f4357` (botones de Compose navegables y la flecha arriba llega a la barra superior).
- Último build instalado: run 37 (`43319124501`, commit `48da2e3`). Los commits posteriores (`41c11a0`, `b7f4357`) no tienen build verde todavía.

## Un solo APK para teléfono y TV box

El build firma un APK **universal** que incluye `arm64-v8a` (teléfonos y TV box modernos), `armeabi-v7a` (TV box de 32 bits), `x86` y `x86_64`. No hacen falta APK separados. Configurado en `app/build.gradle.kts` (bloque `splits.abi`, `isUniversalApk = true`).

Artefacto del workflow: `ALERTA-EC-v0.1.12-RELEASE-PRUEBA`, con el archivo `Alerta-EC-v0.1.12.apk` y su `.sha256`.

## Historial de commits

Los fallos del workflow son ejecuciones del CI, no commits rotos. El historial de la rama se conserva tal cual: no se reescribe ni se hace `force push` sin autorización expresa. Para una rama limpia, lo más seguro es un merge o squash a una rama nueva, decidido por el responsable del repositorio.

## Qué falta verificar en dispositivo

- **TV:** el mando debe bajar sin pulsar de más por la pantalla principal y por Configuración; el halo neón debe marcar la opción enfocada; «Omitir introducción» debe responder a OK al empezar la intro.
- **Mapa de satélite y rayos (NOAA) e INAMHI:** siguen con "Imagen no disponible" y las capas no cargan. Falta la salida de `logcat -d -s AlertaEC-Map:V` tras abrir el mapa.
- **Teléfono:** la versión vertical del logo ya mejoró según la prueba del usuario. Pendiente revisar la pantalla principal y los mapas, que no aparecen en el teléfono.
- **Teléfono:** pantallas heredadas (GPS, seguridad, enlaces externos de emergencia) sin decidir qué se conserva.

## Cómo instalar (Termux con fish, en el teléfono o la TV)

```fish
set RUN_ID 43286895270
```

```fish
gh run download $RUN_ID -R clicktvdigital/Alerta-EC -n ALERTA-EC-v0.1.12-RELEASE-PRUEBA
```

```fish
set APKA (find . -name 'Alerta-EC-v0.1.12.apk' | head -1)
adb -s 192.168.100.11:5555 install -r $APKA
```

Para el teléfono, copia el APK a `/storage/emulated/0/Download/` y ábrelo con el gestor de archivos.

## Reglas del proyecto

Léase `AGENTS.md`: responder en español; no hacer commit, push, merge, tag ni Release sin autorización expresa; no cambiar `applicationId`, la firma ni las credenciales sin autorización explícita.

## Repositorio del launcher

El launcher de CLICK TV (`clicktvdigital/click-tv-launcher`, en otro directorio) tiene su propia documentación en `docs/ESTADO_PROYECTO.md`. Cada app se instala y se versiona por separado.

## Pendiente antes del siguiente build

- Intro: que «Omitir introducción» reciba el foco y OK lo active.
- Acerca de y Configuración: que la flecha abajo recorra las opciones.
- Mapa meteorológico: capas distinguibles y modo noche.
- Satélite y rayos: confirmar con `logcat -d -s AlertaEC-Map:V` tras pulsar Reproducir.
- Versión de Alerta EC: pendiente de subir a 0.1.12 en el build de prueba.
