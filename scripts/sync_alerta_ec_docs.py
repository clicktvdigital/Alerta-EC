from pathlib import Path
import re

root = Path(__file__).resolve().parent.parent
master = root / "docs/ALERTA_EC_ESTADO.md"
start = "<!-- ALERTA_EC_AUTO_START -->"
end = "<!-- ALERTA_EC_AUTO_END -->"

if not master.exists():
    raise SystemExit("ERROR: falta docs/ALERTA_EC_ESTADO.md")

source = master.read_text(encoding="utf-8").strip()
block = start + chr(10) + chr(10) + source + chr(10) + chr(10) + end

for name in ("README.md", "docs/CONTINUAR_AQUI.md"):
    path = root / name
    previous = path.read_text(encoding="utf-8") if path.exists() else ""
    backup = Path(str(path) + ".bak")
    if path.exists():
        backup.write_text(previous, encoding="utf-8")
    pattern = re.escape(start) + r".*?" + re.escape(end)
    matches = list(re.finditer(pattern, previous, flags=re.S))
    if matches:
        cleaned = re.sub(pattern, "", previous, flags=re.S).rstrip()
        updated = cleaned + chr(10) + chr(10) + block + chr(10)
    else:
        updated = previous.rstrip() + chr(10) + chr(10) + block + chr(10)
    path.parent.mkdir(parents=True, exist_ok=True)
    if updated != previous:
        path.write_text(updated, encoding="utf-8")
        print("ACTUALIZADO:", name)
    else:
        print("SIN CAMBIOS:", name)
    if backup.exists():
        backup.unlink()
    final_text = path.read_text(encoding="utf-8")
    if final_text.count(start) != 1 or final_text.count(end) != 1:
        raise SystemExit("ERROR: bloque automatico duplicado o incompleto en " + name)

print("SINCRONIZACION COMPLETADA SIN DUPLICADOS")
