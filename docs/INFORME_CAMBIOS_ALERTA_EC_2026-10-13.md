# Informe de cambios Alerta EC — 2026-10-13

## Alcance y documentos preservados

Se leyeron README.md, docs/CONTINUAR_AQUI.md y el historial reciente de la rama alerta-ec. Se preservó el contenido histórico existente: la visión y los acuerdos nuevos se anexaron, sin sustituir ninguno de los dos documentos. Se mantuvieron los cambios locales previos de la corrección JUnit Jupiter y su diagnóstico.

README.md ahora registra la visión funcional, ubicación administrativa, código postal [código postal residencial omitido] confirmado por el usuario y discrepancia Nominatim 170318, unidades, zona horaria, prioridades horarias, radar/satélites reales, vigilancia territorial, calidad del aire, accesibilidad, fuentes, límites de licencia y funciones pendientes. CONTINUAR_AQUI.md agrega estado técnico, regla de sincronización antes de cada commit, decisiones, riesgos y siguiente fase.

## Archivos cambiados

- README.md — visión funcional de Alerta EC y regla permanente documental.
- docs/CONTINUAR_AQUI.md — acuerdos técnicos consolidados y estado de la mejora.
- app/src/main/kotlin/org/breezyweather/sources/openmeteo/OpenMeteoService.kt — evita un índice inválido cuando no existe intervalo minutely futuro utilizable; conserva mm/15 min × 4 como tasa equivalente mm/h de pronóstico.
- app/src/test/kotlin/org/breezyweather/sources/openmeteo/MinutelyForecastIndexTest.kt — casos de intervalo encontrado, serie vencida y vacía.
- app/src/test/kotlin/org/breezyweather/alertaec/AlertaEcNotificationPolicyTest.kt — se conserva la corrección local de JUnit 4 a Jupiter.
- Este informe.

No se modificaron applicationId, firma, versiones, logo ni intro. No se activó una segunda ruta de notificación minutely ni se presentó el pronóstico como observación.

## Verificaciones

- `git diff --check`: correcto.
- Comprobación estática ligera de guardia de índice, conversión de unidades, casos de prueba y regla documental: correcta.
- Pruebas unitarias/Gradle: no ejecutadas; el encargo excluye Gradle local. La prueba nueva queda pendiente de CI.
- Validación funcional en dispositivo: pendiente.

## Commit y push

- Commit de implementación: `597f3205c` (`feat(alerta-ec): guard minutely forecast parsing`).
- Push: correcto a `origin/alerta-ec` (`0c73a5d76..597f3205c`). No se publicó a `main` ni se crearon tags o releases.
- Este informe se completa en un commit documental posterior para registrar el resultado del push.
- Rama objetivo: `alerta-ec`; no se usa `main`, tags, releases ni repositorios nuevos.

## Próximo paso

Confirmar CI de `alerta-ec`, luego validar en dispositivo los casos minutely futuro, vencido y vacío y que solo la notificación original de Breezy Weather avise precipitación. Mantener sincronizados README.md y CONTINUAR_AQUI.md antes de cada commit.
