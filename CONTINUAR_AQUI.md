# Continuar aquí — Alerta EC (Android TV)

## 1. Estado actual

- **Repositorio:** `clicktvdigital/alerta-ec`, rama de trabajo `tv-control-remoto`.
- **Plataforma:** Android nativo (Kotlin, Views + Compose). No es Flutter.
- **Último commit publicado:** ver `git log -1` en la rama `tv-control-remoto`.
- **Verificación:** compilación con Gradle y 276 pruebas unitarias sin fallos.
- **Compilación:** no se compila APK localmente. El build se hace en GitHub Actions (`alerta-ec.yml`, manual).

## 2. Qué se reparó en esta etapa

- **Navegación con control remoto en Home:** flechas entre tarjetas y filas; la barra superior se alcanza desde la primera fila; izquierda y derecha se mueven dentro de la barra; OK activa la tarjeta marcada.
- **Tecla Menú:** abre Ajustes desde Home.
- **Actualizar Home:** mantener ABAJO unos 2 segundos (reemplaza el botón amarillo, que se retiró).
- **Intro de TV:** imagen de inicio 5 s, video original de 12 s, imagen final 5 s. Se intenta reproducir al reanudar la actividad, también al reingresar. **Pendiente de validar en el TV.**
- **Ubicación actual:** al agregarla se pide el clima de inmediato, sin esperar a refrescar.
- **Botón Omitir:** anillo de foco visible.
- **Diagnóstico:** registros `AlertaEcStartup` (arranque) y `AlertaEcKeys` (teclas) temporales, para investigar el arranque.

## 3. Herramientas, workflows y skills (para Claude Code)

**Skills y plugins activos en el entorno de trabajo**
- **Superpowers** (obra): `systematic-debugging` (causa raíz antes de parchar), `verification-before-completion` (evidencia antes de declarar terminado), `writing-plans` (planificar antes de editar), además de TDD y revisión de código.
- **Claude Memory** (`structured-memory`): memoria entre sesiones.
- **Code Graph** (`code-graph`): mapa de subsistemas en HTML.
- **Emil Kowalski** (`emil-design-eng`), **Impeccable** y **Taste** (`learn-taste`): criterios de diseño y microinteracciones. Adaptar a TV: alto contraste, texto legible a 3 metros, sin efectos pesados.
- **Skills de Flutter** (`flutter/skills`): solo aplican al launcher.

**No disponible**
- `claude-mem` y `mem-search`: no están en el catálogo de la cuenta.

**Graphify (CLI)**
- Instalado con `pip install graphifyy`. Uso: `graphify . --code-only` (sin clave de API) y `graphify cluster-only <ruta>`.
- El grafo de Alerta-EC está **desactualizado** (commit `48921a75`). Para actualizarlo: `graphify . --code-only --force`.

**Dónde vive la configuración**
- `.claude/skills/` existe solo en el launcher (`CLICK-TV-LAUNCHER`). Alerta-EC no tiene carpeta `.claude`.
- Las skills globales están en `~/.claude/skills`. Solo existen en el equipo donde se instalaron; en otro equipo hay que instalarlas de nuevo.

**Workflow de compilación y publicación**
- Alerta-EC: `alerta-ec.yml` (APK de depuración, manual, sube artefacto `Alerta-EC-Debug`); `release.yml` solo con tags `v*`.
- Launcher: `.github/workflows/` con el release por tags.
- Antes de subir: compilar y correr pruebas (Gradle en Alerta-EC; `flutter analyze` en el launcher). Sin `git add` de `graphify-out/` (ya está en `.gitignore`).

## 4. Próximos pasos (siguiente sesión)

1. **Validar en el TV** el build más reciente: intro en frío y al reingresar; ubicación actual al agregarla; mantener ABAJO 2 s para actualizar.
2. **Ajustes:** flechas arriba y abajo no funcionan. Revisar el foco de las pantallas Compose de `SettingsActivity` y las vistas de Settings.
3. **Home:** scroll hasta los bloques inferiores (precipitación, visibilidad, presión, luna, mapas). Revisar si la lista recibe foco en esos bloques.
4. **Arranque en frío:** unos 14 s de hilo principal ocupado después de `onCreate`. Usar el log `AlertaEcStartup` de `HomeFragment` para ubicar el bloqueo.
5. **Pantallas de detalle** (pronóstico por horas y por días): izquierda y derecha entre bloques.
6. **Decisión pendiente:** ocultar por completo "Ubicación actual" en TV, o mantenerla como ruta alternativa.
7. **Rediseño visual para TV:** tipografía para lectura a 3 metros, contraste y foco. Hacerlo por partes y validar cada una en el TV.
8. **Registros de diagnóstico:** quitar `AlertaEcStartup` y `AlertaEcKeys` cuando el bloqueo esté resuelto.
9. **Grafo:** regenerar con `--force` para que refleje los commits actuales.
10. **README:** reescribir cuando se defina el nombre del producto y el flujo de descarga de releases.

## 5. Cómo retomar

```fish
cd ~/alerta-ec   # o la ruta donde esté clonado
git checkout tv-control-remoto && git pull
```

Compilar y probar:
```bash
./gradlew :app:testBasicDebugUnitTest --no-daemon
```
