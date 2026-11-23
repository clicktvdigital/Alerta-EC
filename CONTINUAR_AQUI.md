# Continuar aquí — Alerta EC

Actualizado: 2026-11-22 · Versión publicada: **v0.1.16 (versionCode 117)** · Tag: `v0.1.16` · Commit: `c2e172a3d`

## Estado actual

- **Proyecto unificado:** un solo repositorio (`clicktvdigital/Alerta-EC`) y un solo código base Android (Kotlin + Views/Compose) para teléfono y Android TV / TV Box, con APKs firmados separados por arquitectura (`arm64-v8a` y `armeabi-v7a`) y misma firma.
- **Release v0.1.16 publicado:** [https://github.com/clicktvdigital/Alerta-EC/releases/tag/v0.1.16](https://github.com/clicktvdigital/Alerta-EC/releases/tag/v0.1.16).
- **Verificado en dispositivos:** probado en teléfono Android (`arm64-v8a`) y Android TV / TV Box con `0.1.16` (`versionCode 117`), 10 canales de audio `*_v4`, sin crashes ni ANR, intro operativa y navegación por control remoto estable.

## Cambios clave en v0.1.16

- **Actualizador in-app por ABI y variante:** selecciona el APK compatible (`arm64-v8a` o `armeabi-v7a`) y respeta el flavor (`basic` / `freenet`), descargando en caché interna y abriendo el instalador sobre la firma existente sin pedir permisos adicionales.
- **Audio y notificaciones:** canales `*_v4` con `alertaec_tono_lluvia_informativa`, tonos por nombre de recurso (`/raw/<tono>`), retiro de canales `*_v3` obsoletos, enfriamiento de 2 horas y deduplicación ordenada de 48 horas.
- **Pantalla principal en Ecuador (`EC`):** oculta las tarjetas vacías de Polen y Visibilidad cuando la ubicación corresponde a Ecuador.

## Próximos pasos al preparar la siguiente versión (`v0.1.17` / `versionCode 118`)

1. Mantener el entorno de trabajo limpio sin archivos debug o temporales dentro del repositorio.
2. Incrementar `versionName` a `"0.1.17"` y `versionCode` a `118` en `app/build.gradle.kts`, validar con `python3 scripts/validate_alerta_ec.py` y publicar el tag `v0.1.17` para probar de punta a punta la actualización automática desde `0.1.16`.
