# Reglas del Proyecto: Alerta EC

## 1. Objetivo y Enfoque de la App
- Aplicación Flutter **Alerta EC** enfocada en alertas tempranas, notificaciones críticas y monitoreo en tiempo real para Ecuador (incluyendo versión panorámica para Android TV y TV Box).
- **Reglas técnicas obligatorias:**
  - Priorizar bajo consumo de batería y RAM (compatible con dispositivos de 1 GB de RAM).
  - Garantizar entrega confiable de alertas y navegación fluida con control remoto (D-pad).
  - Interfaz clara, de lectura rápida, con jerarquía visual fuerte y sin recargar la UI con animaciones pesadas.

## 2. Protocolo Anti-Daños y Flujo Autónomo (Cero Preguntas)
- **No pidas confirmación ni hagas preguntas** antes de editar código. Toma la mejor decisión técnica basada en este archivo y ejecuta los cambios.
- **Prohibido parchar a ciegas:** Usa siempre `systematic-debugging` para encontrar la causa raíz completa antes de tocar código.
- **Verificación obligatoria antes de subir:** Ejecuta `flutter analyze` y las pruebas afectadas (`verification-before-completion`) antes de hacer commit.
- **Compilación exclusiva en GitHub:** Nunca ejecutes `flutter build apk` localmente. Al terminar cualquier mejora, ejecuta `git add .`, `git commit -m "feat/fix: descripción"` y `git push` para disparar la compilación en la nube.

## 3. Estándares de Diseño y Memoria
- Aplica los criterios de **Emil Kowalski**, **Impeccable** y **Taste Skill** enfocados en claridad, accesibilidad, contraste alto y microinteracciones rápidas.
- Usa las skills de `claude-mem` (`mem-search`) para mantener contexto de decisiones previas.

## 4. Reporte Visual Obligatorio al Terminar
Al terminar cada tarea y subirla a GitHub, entrega SIEMPRE este resumen sin que te lo pida:
1. **Tabla Visual (Antes vs. Después):** Qué cambió en la interfaz, alertas o rendimiento.
2. **Release Notes listas para GitHub:** Lista limpia con viñetas de mejoras y correcciones para la próxima versión.
3. **Estado del Push:** Confirmación de que los cambios ya están en GitHub compilándose.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- When graphify-out/graph.json exists, run `graphify query "<question>"` before your first grep, search or source-file read for a codebase question, and put this rule in every subagent prompt that explores code. Read raw files only for lines the graph or an earlier read already located. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
- A committed graphify-out/graph.json is rewritten whole on every build, so a large diff to it in a pull request is expected. Do not flag it as churn or suggest skipping the graph rebuild or its hooks.
