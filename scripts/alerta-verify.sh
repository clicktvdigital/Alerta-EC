#!/data/data/com.termux/files/usr/bin/bash
# Verificación de Alerta EC en Termux: compila, opcionalmente genera APK e
# instala en un dispositivo ADB, guarda un log y copia el resumen al portapapeles.
#
# Uso:
#   bash ~/breezy-weather/scripts/alerta-verify.sh            # solo compilación Kotlin
#   bash ~/breezy-weather/scripts/alerta-verify.sh --apk      # compila APK debug arm64
#   bash ~/breezy-weather/scripts/alerta-verify.sh --apk --install

set -u

REPO="$HOME/breezy-weather"
LOG_DIR="$HOME/storage/downloads/Alerta-EC/Logs"
STAMP="$(date +%Y%m%d-%H%M%S)"
LOG="$LOG_DIR/verify-$STAMP.txt"
AAPT2="/data/data/com.termux/files/usr/bin/aapt2"
BUILD_APK=0
INSTALL=0

for arg in "$@"; do
  case "$arg" in
    --apk) BUILD_APK=1 ;;
    --install) BUILD_APK=1; INSTALL=1 ;;
    *) echo "Argumento desconocido: $arg"; exit 2 ;;
  esac
done

mkdir -p "$LOG_DIR" 2>/dev/null || { LOG_DIR="$HOME"; LOG="$HOME/verify-$STAMP.txt"; }

cd "$REPO" || { echo "No existe $REPO"; exit 2; }

{
  echo "== Alerta EC verificación $STAMP =="
  echo "== Git =="
  git status --short --branch
  git log -1 --oneline
  echo

  echo "== Compilación Kotlin =="
  ./gradlew :app:compileBasicDebugKotlin --no-daemon --max-workers=1 --console=plain
  COMPILE_RC=$?
  echo "compileBasicDebugKotlin rc=$COMPILE_RC"
  echo

  APK_RC=0
  if [ "$BUILD_APK" -eq 1 ] && [ "$COMPILE_RC" -eq 0 ]; then
    echo "== APK debug =="
    ./gradlew :app:assembleBasicDebug --no-daemon --max-workers=1 --console=plain \
      -Pandroid.aapt2FromMavenOverride="$AAPT2"
    APK_RC=$?
    echo "assembleBasicDebug rc=$APK_RC"
    echo
  fi

  INSTALL_RC=0
  if [ "$INSTALL" -eq 1 ] && [ "$APK_RC" -eq 0 ] && [ "$COMPILE_RC" -eq 0 ]; then
    echo "== Dispositivos ADB =="
    adb devices
    DEVICES="$(adb devices | awk 'NR>1 && $2=="device" {print $1}')"
    COUNT="$(printf '%s\n' "$DEVICES" | grep -c .)"
    if [ "$COUNT" -eq 1 ]; then
      APK="$(ls -t "$REPO"/app/build/outputs/apk/basic/debug/*arm64*.apk 2>/dev/null | head -1)"
      echo "Instalando: $APK"
      adb -s "$DEVICES" install -r "$APK"
      INSTALL_RC=$?
    else
      echo "No se instala: se requiere exactamente 1 dispositivo conectado (hay $COUNT)."
      INSTALL_RC=3
    fi
    echo "install rc=$INSTALL_RC"
  fi
} 2>&1 | tee "$LOG"

RESULT="OK"
[ "${COMPILE_RC:-1}" -ne 0 ] && RESULT="FALLO compilación"
[ "${APK_RC:-0}" -ne 0 ] && RESULT="FALLO APK"
[ "${INSTALL_RC:-0}" -ne 0 ] && RESULT="FALLO instalación"

SUMMARY="Alerta EC $STAMP: $RESULT (log: $LOG)"
echo "$SUMMARY"
if command -v termux-clipboard-set >/dev/null 2>&1; then
  tail -n 80 "$LOG" | termux-clipboard-set
  echo "Últimas 80 líneas copiadas al portapapeles."
else
  echo "termux-clipboard-set no disponible: instala el paquete termux-api."
fi

[ "$RESULT" = "OK" ]
