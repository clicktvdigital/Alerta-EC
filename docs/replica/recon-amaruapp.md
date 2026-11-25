# Recon (clean-room): patrones de UX de amaruapp.com/app aplicados a ALERTA EC

Fuente pública única: https://amaruapp.com/app (página web, leída el 2026-11-25 con `graphify add`).
Regla: se estudian **funciones y flujos**, no se copia código, textos, marca, iconos ni datos.
Todo lo implementado en ALERTA EC está escrito desde cero con nombres, colores y textos propios.

## Pantallas / bloques observados (S##)

| ID | Bloque | Propósito |
| --- | --- | --- |
| S01 | Mapa principal con capas | lluvia, satélite, rayos, reportes, ciudades, ríos |
| S02 | Panel "dónde hay problemas" | lista de zonas con incidencias, filtro por país |
| S03 | Ficha de punto del mapa | seguir zona, reportar aquí, perfil de ciudad |
| S04 | Formulario de reporte | foto opcional, gravedad en 4 niveles, guía de privacidad, normas |
| S05 | Denuncia de reporte | motivos, ocultación inmediata y revisión humana |
| S06 | Leyenda de niveles | 4 niveles de color + índice 0-100 que combina varias fuentes y explica el porqué |
| S07 | Cuenta opcional | la app funciona sin cuenta |
| S08 | Asistente de preguntas | consultas en lenguaje natural |

## Matriz de funciones → decisión para ALERTA EC

| Función | Prioridad | Estado en ALERTA EC |
| --- | --- | --- |
| Índice de riesgo de lluvia 0-100 con 4 niveles y "por qué" | must | **Nuevo**: `AlertaEcRainRisk` + tarjeta "Lluvia ahora" (MetNo, Open-Meteo, EPMAPS, INAMHI, reportes) |
| Gravedad en el reporte comunitario | must | **Nuevo**: Leve / Moderada / Alta / Crítica en el formulario y en Supabase |
| Guía de privacidad al adjuntar evidencia | must | **Nuevo**: aviso propio (sin rostros, placas ni documentos) |
| Denunciar reporte + ocultación automática | must | **Nuevo**: tabla `report_flags` + trigger que oculta con 3 denuncias |
| Capas de satélite, rayos y lluvia | should | Ya existía (GOES-19 Nubes/Lluvia/Rayos/Amplia, capas INAMHI) |
| App usable sin cuenta | should | Ya existía (sin cuentas; Supabase anónimo con RLS) |
| Seguir zonas y avisos | should | Ya existía (ubicaciones + notificaciones de lluvia/alertas) |
| Ríos, mareas, embalses, apagones | could | No adoptado: sin fuente libre verificada para Ecuador en la app |
| Asistente conversacional | could | No adoptado: requiere un servicio de IA de pago y moderación |
