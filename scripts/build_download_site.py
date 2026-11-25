#!/usr/bin/env python3
"""Genera el sitio de descargas directas de ALERTA EC (GitHub Pages).

Uso: python3 -I scripts/build_download_site.py <carpeta_salida> <version> <commit> [version_code]

La carpeta de salida debe contener ya apk/ con los 4 perfiles. Cada perfil tiene una
página corta (p. ej. /celular/) con metadatos Open Graph para la vista previa de WhatsApp
y redes, que inicia la descarga de su APK automáticamente.
"""
import base64
import hashlib
import html
import json
import os
import shutil
import sys
from pathlib import Path

BASE_URL = "https://clicktvdigital.github.io/Alerta-EC"
SUPABASE_URL = "https://bnnpdwrduktchmgbwlha.supabase.co"
WEB_IPHONE_DIR = Path(__file__).resolve().parent.parent / "docs" / "web-iphone"

PROFILES = [
    ("celular", "alertaec-phone-arm64-v8a.apk", "Celular (recomendado)",
     "Para la mayoría de celulares Android modernos (64 bits, arm64-v8a)."),
    ("celular-v7", "alertaec-phone-armeabi-v7a.apk", "Celular antiguo",
     "Para celulares Android de 32 bits (armeabi-v7a) o de gama de entrada."),
    ("tv", "alertaec-tv-fhd-armeabi-v7a.apk", "Smart TV Full HD",
     "Para televisores Android TV / Google TV 1080p (armeabi-v7a)."),
    ("tvbox", "alertaec-tvbox-hd-armeabi-v7a.apk", "TV Box HD",
     "Para TV Box Android 720p y equipos con poca memoria (armeabi-v7a)."),
]

STYLE = """
:root{color-scheme:dark}
body{margin:0;font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;background:#07131f;color:#e8f1f8}
main{max-width:640px;margin:0 auto;padding:24px 18px 40px}
img.hero{width:100%;border-radius:14px;display:block}
h1{font-size:1.5rem;margin:20px 0 6px}
h2{font-size:1.15rem;margin:18px 0 6px}
p{line-height:1.5;color:#b9c9d6}
a.btn{display:block;text-align:center;margin:14px 0;padding:16px;border-radius:12px;border:2px solid #00e5ff;
color:#e8f1f8;text-decoration:none;font-weight:700;font-size:1.1rem;box-shadow:0 0 14px rgba(0,229,255,.35)}
a.btn:focus,a.btn:hover{background:rgba(0,229,255,.12)}
.card{border:1px solid #1f3a50;border-radius:12px;padding:14px;margin:12px 0}
.card h2{font-size:1.1rem;margin:0 0 4px}
small{color:#7f97aa;word-break:break-all}
ul,ol{color:#b9c9d6;line-height:1.6;padding-left:20px}
"""


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def is_public_supabase_key(value: str) -> bool:
    """Igual que AlertaEcCommunity.isPublicKey: solo anon JWT o sb_publishable_, nunca service_role/sb_secret_."""
    if not value or value.startswith("sb_secret_"):
        return False
    if value.startswith("sb_publishable_"):
        return True
    parts = value.split(".")
    if len(parts) != 3:
        return False
    try:
        payload = json.loads(base64.urlsafe_b64decode(parts[1] + "=" * (-len(parts[1]) % 4)))
    except (ValueError, TypeError):
        return False
    return isinstance(payload, dict) and payload.get("role") == "anon"


def build_web_iphone(out: Path, version: str) -> bool:
    """Copia la Web App para iPhone (PWA) en <out>/iphone/ con la versión y la clave pública de Supabase."""
    if not (WEB_IPHONE_DIR / "index.html").is_file():
        return False
    dest = out / "iphone"
    if dest.exists():
        shutil.rmtree(dest)
    shutil.copytree(WEB_IPHONE_DIR, dest)
    for name in ("index.html", "service-worker.js"):
        target = dest / name
        target.write_text(target.read_text(encoding="utf-8").replace("__ALERTAEC_VERSION__", version), encoding="utf-8")
    key = os.environ.get("ALERTAEC_SUPABASE_ANON_KEY", "").strip()
    if not is_public_supabase_key(key):
        if key:
            print("AVISO: la clave de Supabase no es pública (anon/publishable); la Web App queda sin reportes.")
        key = ""
    config = {"version": version, "supabaseUrl": SUPABASE_URL if key else "", "supabaseKey": key}
    (dest / "config.js").write_text(
        "window.ALERTA_EC_CONFIG = " + json.dumps(config, ensure_ascii=False) + ";\n", encoding="utf-8"
    )
    print(f"Web App iPhone: {dest} (comunidad {'activa' if key else 'sin clave'})")
    return True


def head(title: str, description: str, url: str, extra: str = "") -> str:
    t, d = html.escape(title), html.escape(description)
    return f"""<!DOCTYPE html>
<html lang="es"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>{t}</title>
<meta name="description" content="{d}">
<meta property="og:type" content="website">
<meta property="og:site_name" content="ALERTA EC">
<meta property="og:title" content="{t}">
<meta property="og:description" content="{d}">
<meta property="og:url" content="{url}">
<meta property="og:image" content="{BASE_URL}/og-alertaec.jpg">
<meta property="og:image:width" content="1200">
<meta property="og:image:height" content="630">
<meta property="og:locale" content="es_EC">
<meta name="twitter:card" content="summary_large_image">
<meta name="robots" content="noindex">
{extra}<style>{STYLE}</style></head><body><main>
"""


STEPS = """<ol>
<li>Si Android lo pide, permite «Instalar apps desconocidas» para tu navegador o WhatsApp.</li>
<li>Abre el archivo descargado y toca «Instalar» (o «Actualizar» si ya tienes ALERTA EC).</li>
<li>Al abrirla, concede ubicación y notificaciones para recibir alertas de tu zona.</li>
</ol>"""


def main() -> None:
    out, version, commit = Path(sys.argv[1]), sys.argv[2], sys.argv[3][:9]
    version_code = int(sys.argv[4]) if len(sys.argv) > 4 and sys.argv[4].isdigit() else None
    notes_file = Path(__file__).resolve().parent.parent / "docs" / "descargas" / "novedades.txt"
    notes = [line.strip() for line in notes_file.read_text(encoding="utf-8").splitlines() if line.strip()] \
        if notes_file.is_file() else []
    news = ("<h2>Novedades de esta versión</h2><ul>" +
            "".join(f"<li>{html.escape(n)}</li>" for n in notes) + "</ul>") if notes else ""
    apk_dir = out / "apk"
    manifest = {"version": version, "version_code": version_code, "commit": commit, "perfiles": []}
    sums = []
    for slug, filename, label, about in PROFILES:
        apk = apk_dir / filename
        if not apk.is_file() or apk.stat().st_size < 1_000_000:
            raise SystemExit(f"Falta o está vacío: {apk}")
        digest = sha256(apk)
        size_mb = apk.stat().st_size / 1_048_576
        sums.append(f"{digest}  {filename}")
        page_url = f"{BASE_URL}/{slug}/"
        apk_url = f"{BASE_URL}/apk/{filename}"
        manifest["perfiles"].append(
            {"perfil": slug, "archivo": filename, "pagina": page_url, "apk": apk_url,
             "sha256": digest, "bytes": apk.stat().st_size}
        )
        title = f"Descargar ALERTA EC {version} · {label}"
        description = f"{about} Alertas de clima, lluvia y riesgos de Ecuador. APK {size_mb:.0f} MB."
        # Se descarga sola al abrir el enlace; el botón queda por si el navegador la bloquea.
        refresh = f'<meta http-equiv="refresh" content="1;url=../apk/{filename}">\n'
        body = head(title, description, page_url, refresh) + f"""
<img class="hero" src="../og-alertaec.jpg" alt="ALERTA EC">
<h1>ALERTA EC · {html.escape(label)}</h1>
<p>{html.escape(about)} La descarga empieza sola en un momento.</p>
{news}
<a class="btn" href="../apk/{filename}" download>⬇ Descargar APK ({size_mb:.0f} MB)</a>
{STEPS}
<p><small>Versión {html.escape(version)} · commit {html.escape(commit)}<br>SHA-256: {digest}</small></p>
<p><a href="../">¿Otro equipo? Ver las 4 versiones</a></p>
</main></body></html>
"""
        (out / slug).mkdir(parents=True, exist_ok=True)
        (out / slug / "index.html").write_text(body, encoding="utf-8")

    cards = "".join(
        f'<div class="card"><h2>{html.escape(label)}</h2><p>{html.escape(about)}</p>'
        f'<a class="btn" href="{slug}/">Descargar</a><small>{BASE_URL}/{slug}/</small></div>'
        for slug, _, label, about in PROFILES
    )
    if build_web_iphone(out, version):
        cards += (
            '<div class="card"><h2>iPhone · Web App oficial</h2><p>Para iPhone y iPad: clima, índice de riesgo de lluvia, '
            'mapa y reportes de la comunidad desde Safari. Se instala con Compartir → «Agregar a inicio».</p>'
            f'<a class="btn" href="iphone/">Abrir en iPhone</a><small>{BASE_URL}/iphone/</small></div>'
        )
    index = head(
        f"ALERTA EC {version} · Descargas",
        "Descarga ALERTA EC para celular, Smart TV o TV Box, o úsala en iPhone. Alertas de clima y riesgos de Ecuador.",
        f"{BASE_URL}/",
    ) + f"""
<img class="hero" src="og-alertaec.jpg" alt="ALERTA EC">
<h1>Descargar ALERTA EC {html.escape(version)}</h1>
<p>Elige tu equipo. Si no sabes cuál, usa «Celular (recomendado)». En iPhone, abre la Web App.</p>
{cards}
{news}
{STEPS}
<p><small>Firmado con la clave oficial de ALERTA EC · commit {html.escape(commit)} ·
<a href="apk/SHA256SUMS.txt">SHA256SUMS.txt</a></small></p>
</main></body></html>
"""
    (out / "index.html").write_text(index, encoding="utf-8")
    (apk_dir / "SHA256SUMS.txt").write_text("\n".join(sums) + "\n", encoding="utf-8")
    (out / "version.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    (out / ".nojekyll").write_text("", encoding="utf-8")
    print(json.dumps(manifest, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
