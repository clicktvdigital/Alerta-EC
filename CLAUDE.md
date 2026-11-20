# Reglas del Proyecto: Alerta EC

## 1. Objetivo y enfoque de la app
- **Alerta EC** es una app **Android nativa en Kotlin** (Views + Jetpack Compose; Gradle con módulos `app`, `data`, `domain`), basada en Breezy Weather. **No es Flutter.** (Flutter solo aplica al Click TV Launcher, en otro repo.)
- Enfocada en alertas tempranas, notificaciones críticas y monitoreo en tiempo real para Ecuador, en celular (vertical, táctil) y Android TV / TV Box (horizontal, D-pad). Un solo APK que se adapta por calificadores de recursos (`-television`, `-w640dp`) y detección de TV en tiempo de ejecución.
- **Reglas técnicas obligatorias:**
  - Bajo consumo de batería y RAM (equipos de 1 GB).
  - Entrega confiable de alertas y navegación fluida con control remoto (D-pad + OK). Cada hora y cada día deben abrir su detalle con OK igual que al tocar en el celular.
  - Interfaz clara, de lectura rápida, con jerarquía visual fuerte y foco de alto contraste; sin animaciones pesadas.
  - En TV no depender de la tecla MENÚ: muchos televisores (MStar) la capturan.

## 2. Flujo de trabajo y verificación
- Toma la mejor decisión técnica y ejecuta; pregunta solo ante acciones destructivas o irreversibles.
- **Causa raíz antes de parchar:** usa `systematic-debugging`.
- **Verificación antes de subir** (`verification-before-completion`):
  - `python3 scripts/validate_alerta_ec.py` (local, sin toolchain Android).
  - Pruebas en el TV: una hipótesis por corrida, registrada con `bash scripts/tv_bitacora.sh` (`docs/PROTOCOLO_EXPERIMENTOS.md`). La evidencia queda fuera de git.
  - `./gradlew :app:testBasicDebugUnitTest` y `:app:compileBasicReleaseKotlin` donde haya toolchain. En Termux no hay Gradle/aapt2: la compilación se verifica en GitHub Actions (`alerta-ec-ci.yml` en push/PR a `alerta-ec`; `alerta-ec.yml` manual para el APK de prueba).
- **Commits:** agrega rutas explícitas (`git add <archivos>`), nunca `git add .`: hay logs y skills locales ignorados. Push de la rama de trabajo para que compile CI.
- **Release:** tag `vX.Y.Z` → `alerta-ec-release.yml` publica un APK firmado por ABI (`armeabi-v7a`, `arm64-v8a`). versionName/versionCode en `app/build.gradle.kts`.
- No usar SSH ni `setprop persist.sys.*`; no tocar `~/pq_backup_tv11.txt` ni los keystores (`~/ALERTA_EC_RESGUARDO_CRITICO`).

## 3. Diseño y memoria
- Criterios de **Emil Kowalski** (`emil-design-eng`, `ask-emil`), **Impeccable** (`impeccable`) y **Taste** (`learn-taste`, `design-taste-frontend`): claridad, accesibilidad, contraste alto, microinteracciones rápidas. Adaptar a TV: legible a 3 m.
- Memoria entre sesiones: plugin `claude-memory` (`structured-memory`) y la memoria de Claude Code. Estado y próximos pasos en `CONTINUAR_AQUI.md`.
- Las skills viven en `.claude/skills/` **solo en local** (ignoradas por git; se instalan por equipo).

## 4. Reporte al terminar
Al cerrar cada tarea entrega: tabla antes/después (UI, alertas o rendimiento), release notes listas para GitHub y estado real del push/CI (sin afirmar compilación que no se haya verificado).

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- When graphify-out/graph.json exists, run `graphify query "<question>"` before your first grep, search or source-file read for a codebase question, and put this rule in every subagent prompt that explores code. Read raw files only for lines the graph or an earlier read already located. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
- A committed graphify-out/graph.json is rewritten whole on every build, so a large diff to it in a pull request is expected. Do not flag it as churn or suggest skipping the graph rebuild or its hooks.
