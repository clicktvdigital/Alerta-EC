# ALERTA EC — Sistema de aportes voluntarios

Proyecto: ALERTA EC
Interfaz: Jetpack Compose / Material 3
Aportes con enlaces en la interfaz: PayPhone, PayPal, Patreon y GitHub Sponsors.
Métodos pendientes de verificación: Pichincha, Guayaquil, Pacífico, Produbanco, Deuna, Binance Pay, Airtm, Global66 y Remitly.
Contacto: dos numeros de WhatsApp
Regla: no publicar datos bancarios privados
Estado general del plan de transferencias/datos bancarios: pendiente; ver el estado actual de los aportes en la sección inferior.

## Estado de la sección de aportes de la aplicación

- PayPhone, PayPal, Patreon y GitHub Sponsors aparecen como aportes opcionales. No hay funciones meteorológicas esenciales de pago.
- PayPhone usa el enlace HTTPS original autorizado: https://ppls.me/izAAuu8fptGHF6uZBV9L4Q. El propietario confirma que funciona desde otra cuenta, acepta cualquier monto y abre el formulario de pago. No se completó una transacción de prueba. La verificación HTTP HEAD devuelve 404; ese método no demuestra que el flujo GET confirmado por el propietario esté inactivo.
- PayPal (https://www.paypal.com/ncp/payment/NKS3NQA6Q2Q5N), Patreon (https://www.patreon.com/clicktvdigital) y GitHub Sponsors (https://github.com/sponsors/clicktvdigital) responden en sus dominios oficiales. La revisión solo comprobó disponibilidad de página/enlace, no completó pagos ni verificó una transacción.
- El enlace de Pichincha proporcionado para la auditoría (https://envios.pichincha.com/l/Cb3V5Ja7) responde en el dominio oficial, pero el contenido recuperado no permitió confirmar destinatario ni monto. No activarlo hasta confirmar esos datos con el propietario.
- No se encontraron enlaces ni imágenes QR en los archivos rastreados del repositorio para Banco Guayaquil, Banco del Pacífico, Produbanco, Deuna, Binance Pay, Airtm, Global66 o Remitly. Mantenerlos pendientes y no inventar datos.
- No se agregó Google Play Billing ni Google Pay.
- PayPhone se abre mediante Intent ACTION_VIEW/BROWSABLE con el enlace HTTPS original; la aplicación no captura datos financieros, credenciales ni parámetros de redirección.
