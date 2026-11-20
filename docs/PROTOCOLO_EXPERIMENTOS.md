# Protocolo de experimentos y bitácora

Patrón inspirado en el método de trabajo de Andrej Karpathy (*autoresearch*, «recipe for training neural nets»): **una hipótesis, un cambio, una medida, una decisión**, todo trazable. Es solo un protocolo y un script; no hay dependencias nuevas ni plugins.

## Reglas

1. **Línea base primero.** Antes de tocar código, registra el estado actual del TV con `bash scripts/tv_bitacora.sh base`.
2. **Un cambio por corrida.** Si cambias dos cosas a la vez, no sabrás cuál funcionó.
3. **Hipótesis falsable.** Escríbela antes de probar: «OK sobre una hora abre su detalle», no «mejorar la navegación».
4. **Evidencia o no pasó.** Cada resultado necesita un log, una captura o la salida de un comando. Si solo lo viste en pantalla, márcalo como *observado*.
5. **Decidir y anotar:** `PASA`, `FALLA` o `DUDOSO`. Un resultado `DUDOSO` se repite en una corrida controlada antes de concluir nada.
6. **Fuera de git lo pesado.** Logs, capturas y videos van a `~/ALERTA_EC_BITACORA/` (configurable con `ALERTA_BITACORA_DIR`). En el repo solo entra la conclusión: una fila en `CONTINUAR_AQUI.md`, bajo `RESULTADOS_TV_V0115`.

## Corrida controlada en el TV (SWTV-20SA, MStar)

Las trampas conocidas del TV contaminan las pruebas si no se controlan:

- el salvapantallas se activa a los 5 min;
- MENÚ lo captura el firmware;
- `screencap` antepone texto al PNG;
- alguien puede estar usando el control físico al mismo tiempo.

El script hace, por defecto en `192.168.100.11:5555`:

1. `adb connect` y `KEYCODE_WAKEUP`;
2. detecta el paquete instalado: el release (`com.clicktvdigital.alertaec`) o, si no está, el debug (`….debug`), y anota su versión;
3. en el modo `run`: `force-stop`, borra logcat y abre la app en frío. Mide el arranque con `am start -W`;
4. guarda el logcat filtrado (`AlertaEcStartup`, `AlertaEcKeys`, `AlertaEcFocus`, `AndroidRuntime`) y una captura PNG ya recortada;
5. añade una línea a `bitacora.tsv` con fecha, commit, paquete, versión, hipótesis, `TotalTime` y la ruta de la evidencia.

```sh
bash scripts/tv_bitacora.sh base "estado antes de cambios"           # solo observa
bash scripts/tv_bitacora.sh run  "OK en hora 14:00 abre su detalle"   # arranque en frío + evidencia
bash scripts/tv_bitacora.sh decide PASA "detalle abre en 1 pulsación"  # cierra la última entrada
```

Las teclas de la prueba (D-pad, OK) se envían a mano o con `adb shell input keyevent`. Antes de concluir, contrasta las teclas enviadas con el log `AlertaEcFocus`.

## Cierre de una versión

- [ ] La línea base y una corrida `run` por cada punto del checklist de la versión, todos en `PASA`.
- [ ] Resumen de resultados en `CONTINUAR_AQUI.md` (tabla corta con la ruta de la evidencia, no la evidencia).
- [ ] `python3 scripts/validate_alerta_ec.py` en verde y CI en verde. No se da por compilado nada que CI no haya compilado.
