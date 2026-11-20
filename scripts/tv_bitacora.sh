#!/usr/bin/env bash
# Bitácora de experimentos en el TV (ver docs/PROTOCOLO_EXPERIMENTOS.md).
# Uso:
#   bash scripts/tv_bitacora.sh base   "<nota>"         línea base, solo observa
#   bash scripts/tv_bitacora.sh run    "<hipótesis>"    arranque en frío + evidencia
#   bash scripts/tv_bitacora.sh decide PASA|FALLA|DUDOSO "<nota>"
# La evidencia queda fuera de git, en $ALERTA_BITACORA_DIR (~/ALERTA_EC_BITACORA).
set -u

TV="${ALERTA_TV:-192.168.100.11:5555}"
DIR="${ALERTA_BITACORA_DIR:-$HOME/ALERTA_EC_BITACORA}"
TSV="$DIR/bitacora.tsv"
MODE="${1:-}"; shift || true
NOTE="${*:-}"

mkdir -p "$DIR"
[ -f "$TSV" ] || printf 'fecha\tmodo\tcommit\tpaquete\tversion\tnota\tarranque_ms\tevidencia\tdecision\n' > "$TSV"

case "$MODE" in
  decide)
    verdict="${1:-}"; shift || true
    case "$verdict" in PASA|FALLA|DUDOSO) ;; *) echo "decide PASA|FALLA|DUDOSO \"nota\""; exit 2 ;; esac
    tmp="$(mktemp)"
    awk -F'\t' -v OFS='\t' -v d="$verdict: ${*:-}" 'NR>1{last=NR} {l[NR]=$0} END{for(i=1;i<=NR;i++){if(i==last){split(l[i],f,"\t");f[9]=d;s=f[1];for(j=2;j<=9;j++)s=s OFS f[j];print s}else print l[i]}}' "$TSV" > "$tmp" && mv "$tmp" "$TSV"
    tail -n 1 "$TSV"; exit 0 ;;
  base|run) ;;
  *) sed -n '2,7p' "$0"; exit 2 ;;
esac

A=(adb -s "$TV")
timeout 15 adb connect "$TV" >/dev/null || { echo "Sin conexión con $TV"; exit 1; }
"${A[@]}" shell input keyevent KEYCODE_WAKEUP

PKG=""
for p in com.clicktvdigital.alertaec com.clicktvdigital.alertaec.debug; do
  "${A[@]}" shell pm path "$p" >/dev/null 2>&1 && { PKG="$p"; break; }
done
[ -n "$PKG" ] || { echo "Alerta EC no está instalada en $TV"; exit 1; }
VER="$("${A[@]}" shell dumpsys package "$PKG" 2>/dev/null | grep -m1 -oE 'versionName=[^ ]+' | cut -d= -f2)"
COMMIT="$(git -C "$(dirname "$0")/.." rev-parse --short HEAD 2>/dev/null || echo '?')"
STAMP="$(date +%Y%m%d-%H%M%S)"
EVD="$DIR/$STAMP-$MODE"; mkdir -p "$EVD"
MS="-"

if [ "$MODE" = run ]; then
  "${A[@]}" shell am force-stop "$PKG"
  "${A[@]}" logcat -c
  ACT="$("${A[@]}" shell cmd package resolve-activity --brief -c android.intent.category.LEANBACK_LAUNCHER "$PKG" | tail -n 1)"
  case "$ACT" in */*) ;; *) ACT="$("${A[@]}" shell cmd package resolve-activity --brief "$PKG" | tail -n 1)" ;; esac
  "${A[@]}" shell am start -W -n "$ACT" > "$EVD/am_start.txt" 2>&1
  MS="$(grep -oE 'TotalTime: [0-9]+' "$EVD/am_start.txt" | grep -oE '[0-9]+' || echo '-')"
  sleep 4
fi

"${A[@]}" logcat -d -v time -s AlertaEcStartup AlertaEcKeys AlertaEcFocus AndroidRuntime:E > "$EVD/logcat.txt" 2>&1
"${A[@]}" exec-out screencap -p | python3 -c 'import sys;d=sys.stdin.buffer.read();i=d.find(b"\x89PNG");sys.stdout.buffer.write(d[i:] if i>=0 else b"")' > "$EVD/pantalla.png"
[ -s "$EVD/pantalla.png" ] || rm -f "$EVD/pantalla.png"
printf '%s\n' "$NOTE" > "$EVD/nota.txt"

printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$STAMP" "$MODE" "$COMMIT" "$PKG" "$VER" "$NOTE" "$MS" "$EVD" "pendiente" >> "$TSV"
echo "Registrado: $MODE $PKG $VER arranque=${MS}ms → $EVD"
grep -c . "$EVD/logcat.txt" | xargs -I{} echo "Líneas de log filtradas: {}"
