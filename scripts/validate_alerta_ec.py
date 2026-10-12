from pathlib import Path
import subprocess
import sys

root = Path(__file__).resolve().parent.parent
required = [
    root / "README.md",
    root / "docs/CONTINUAR_AQUI.md",
    root / "docs/ALERTA_EC_ESTADO.md",
    root / "scripts/sync_alerta_ec_docs.py",
    root / "app/src/main/kotlin/org/breezyweather/alertaec/AlertaEcPhenomenon.kt",
    root / "app/src/main/kotlin/org/breezyweather/alertaec/AlertaEcEvent.kt",
]

missing = [str(p.relative_to(root)) for p in required if not p.exists()]
if missing:
    print("ERROR: faltan archivos requeridos:")
    for item in missing:
        print(" -", item)
    sys.exit(1)

for name in ("README.md", "docs/CONTINUAR_AQUI.md"):
    text = (root / name).read_text(encoding="utf-8")
    if text.count("<!-- ALERTA_EC_AUTO_START -->") != 1:
        raise SystemExit("ERROR: bloque automatico incorrecto en " + name)
    if text.count("<!-- ALERTA_EC_AUTO_END -->") != 1:
        raise SystemExit("ERROR: cierre automatico incorrecto en " + name)

subprocess.run(["git", "diff", "--check"], cwd=root, check=True)
print("VALIDACION ALERTA EC: OK")
