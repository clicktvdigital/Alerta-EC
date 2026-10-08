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


## PLAN MAESTRO ACTUALIZADO

### Vision
Alerta EC sera una aplicacion meteorologica profesional, inclusiva y accesible, desarrollada inicialmente para Ecuador y preparada para ampliarse a otras regiones. Debe explicar los fenomenos, su probabilidad, horarios, fuentes, riesgos y recomendaciones, sin presentar estimaciones como certezas.

### Avance
- Avance global estimado: 30 por ciento del alcance ampliado.
- Es una estimacion de planificacion, no una auditoria automatica.
- Base Android y compilacion Debug: funcionales.
- Intro completo: pendiente de compilar y verificar.
- Release firmada y Obtainium: en preparacion; no publicar todavia.
- Sistema meteorologico avanzado y accesibilidad: pendientes de implementar y probar.

### Prioridad 1: lluvia hiperlocal
- Pronostico del dia anterior y horarios probables.
- Avisos anticipados de 60, 30, 15 y 5 minutos cuando la precision de los datos lo permita.
- Aviso de 1 minuto y cuenta regresiva solamente con datos suficientemente fiables.
- Seguimiento durante la lluvia, intensidad y finalizacion estimada.
- Recordatorios espaciados y aviso cuando finalice.
- Pronostico de nuevos episodios de lluvia durante el mismo dia.
- Evitar falsas alarmas y notificaciones repetitivas.

### Prioridad 2: accesibilidad
- Configuracion inicial sencilla para adultos mayores.
- Sonidos diferentes segun fenomeno y gravedad.
- Vibracion, notificaciones visuales y voz mediante TTS de Android.
- Compatibilidad con lectores de pantalla, textos grandes y alto contraste.
- Modo avanzado para personalizar avisos.
- Apartado para restablecer configuraciones sin borrar datos importantes.
- Al tocar una alerta, abrir explicacion, fuente, hora, recomendaciones y estado.
- Historial de alertas consultable.

### Prioridad 3: informacion meteorologica completa
- Temperatura, sensacion termica, humedad, presion y visibilidad.
- Viento en metros por segundo y kilometros por hora, direccion y rafagas.
- Radiacion UV, calidad del aire y contaminantes cuando existan datos.
- Recomendaciones de vestimenta, actividades y horarios para salir o regresar.
- Comparar fuentes gratuitas fiables: INAMHI, Open-Meteo y otras fuentes compatibles.
- Evaluar cobertura, licencias, frecuencia y precision de satelites, radares y estaciones.
- Distinguir pronosticos de observaciones reales.

### Ubicacion
- Revisar la etiqueta incorrecta Atucucho San Jose.
- Mejorar geocodificacion de calles, cuando el proveedor tenga esos datos.
- Referencia local aportada: sector Atucucho, calles Angel Araujo y N56D, codigo postal 170528.
- No inventar direcciones ni confundir coordenadas GPS con nombres de calles.

### Fases posteriores
- Alertas verificadas de granizo, tormentas, rayos, deslizamientos, sismos, volcanes e incendios.
- Integracion de organismos oficiales y canales de difusion, incluido X cuando sea pertinente.
- No tratar publicaciones de redes sociales como confirmacion automatica de emergencias.
- No afirmar que se pueden predecir terremotos sin un sistema oficial de alerta temprana.

### Diagnosticos confirmados
- Android registra canal alert con importancia alta y vibracion inicialmente deshabilitada.
- El usuario activo manualmente vibracion, sonido y ventanas emergentes.
- Corregir configuracion predeterminada de canales nuevos y respetar ajustes del usuario.
- Existen modelos Minutely y proveedores Open-Meteo y Pirate Weather en el codigo.
- Existen PendingIntent para abrir notificaciones; falta verificar sus destinos.
- Existen archivos de intro y pantallas de alertas; falta probar su funcionamiento completo.

### Firma, respaldo y distribucion
- Paquete Debug: com.clicktvdigital.alertaec.debug.
- Paquete Release: com.clicktvdigital.alertaec.
- GitHub Actions tiene secretos de JKS Base64, contrasena de almacen y contrasena de clave.
- Existe workflow local .github/workflows/alerta-ec-release.yml aun sin validacion final.
- Verificar seguridad, firma, versionCode y funcionamiento antes de publicar Release.
- Conservar Debug hasta comprobar migracion de datos a Release.
- No publicar secretos, claves JKS, archivos privados ni datos personales.
- Conservar respaldos cifrados y verificar su disponibilidad fuera del telefono.

### Metodo de trabajo
- Mantener este archivo como unico documento maestro de continuidad de Alerta EC.
- Actualizar decisiones, avance, pruebas y siguiente paso en este mismo archivo.
- No crear documentos de continuidad duplicados.
- Un comando compatible con Fish por turno y esperar el resultado.
- Revisar cambios antes de confirmar y subir a GitHub.

### Punto de reanudacion
- Ultimo paso completado: 111, lectura del documento de continuidad.
- Paso 112: consolidar este plan maestro.
- Siguiente trabajo tecnico: verificar destino y contenido de notificaciones meteorologicas.
- Priorizar lluvia, alertas accesibles, ubicacion e intro antes de publicar Release.
