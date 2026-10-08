# ALERTA EC — CONTINUAR AQUÍ

## Identificación
- Proyecto: Alerta EC, aplicación Android de alertas para Ecuador.
- Repositorio: https://github.com/clicktvdigital/Alerta-EC
- Rama de desarrollo: alerta-ec
- Código Android: com.clicktvdigital.alertaec
- Origen: adaptación de Breezy Weather, conservando sus atribuciones y licencia.

## Entorno de desarrollo
- Dispositivo: Samsung Galaxy S21 FE.
- Herramientas: Termux, Fish, Git, Gradle, Kotlin, Java y ADB.
- Carpeta habitual: ~/breezy-weather
- Compilación: alerta-build
- APK: app/build/outputs/apk/basic/debug/app-basic-arm64-v8a-debug.apk

## Estado actual
- Existe una introducción animada con fenómenos naturales, cóndor andino, logotipo y marca CLICK TV DIGITAL.
- Quedan por probar cuatro modificaciones recientes de la introducción y los colores de inicio.
- El último APK instalado no incluye necesariamente estas cuatro modificaciones.
- Las alertas meteorológicas automáticas y su precisión requieren investigación y pruebas.
- El panel local está en ~/alerta-ec-panel y utiliza http://127.0.0.1:8765.
- El panel todavía está en desarrollo; no todos los botones funcionan.

## Próximos pasos
1. Confirmar y subir los cambios pendientes de la introducción.
2. Compilar, instalar y probar la animación actualizada.
3. Revisar las notificaciones meteorológicas y sus fuentes verificables.
4. Completar el panel de desarrollo para administrar varios proyectos.
5. Preparar respaldos verificables antes de liberar almacenamiento.

## Recuperación
1. Acceder al repositorio GitHub y seleccionar la rama alerta-ec.
2. Leer este archivo antes de modificar el código.
3. Clonar el repositorio en un entorno Android/Termux compatible.
4. Revisar el historial Git y comprobar el estado de compilación.
5. No publicar contraseñas, tokens, archivos local.properties ni claves de firma.

## Instrucciones para otro asistente
Continuar desde el último estado confirmado. Trabajar en español. Dar un solo comando de Termux compatible con Fish por turno, esperar su resultado y no eliminar archivos sin verificar antes sus respaldos.
