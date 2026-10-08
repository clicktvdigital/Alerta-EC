from flask import Flask, jsonify, render_template
from pathlib import Path
import re
import subprocess

app = Flask(__name__)
PROJECT = Path.home() / "breezy-weather"


def run(command, timeout=12):
    result = subprocess.run(
        command,
        capture_output=True,
        text=True,
        timeout=timeout,
    )
    return {
        "ok": result.returncode == 0,
        "code": result.returncode,
        "stdout": result.stdout.strip(),
        "stderr": result.stderr.strip(),
    }


@app.get("/")
def inicio():
    return render_template("index.html")


@app.get("/api/estado")
def estado():
    return jsonify(
        proyecto="Alerta EC",
        estado="activo",
        idioma="es",
        modo="solo-lectura",
        host="127.0.0.1",
        puerto=8765,
    )


@app.get("/api/proyecto")
def proyecto():
    branch = run(["git", "-C", str(PROJECT), "branch", "--show-current"])
    commit = run(["git", "-C", str(PROJECT), "log", "-1", "--oneline"])
    status = run(["git", "-C", str(PROJECT), "status", "--short"])

    gradle = (PROJECT / "app" / "build.gradle.kts").read_text(
        encoding="utf-8",
        errors="replace",
    )
    version_name = re.search(r'versionName\s*=\s*"([^"]+)"', gradle)
    version_code = re.search(r"versionCode\s*=\s*(\d+)", gradle)

    return jsonify(
        rama=branch["stdout"],
        commit=commit["stdout"],
        cambios=status["stdout"].splitlines() if status["stdout"] else [],
        limpio=not bool(status["stdout"]),
        versionName=version_name.group(1) if version_name else None,
        versionCode=int(version_code.group(1)) if version_code else None,
    )


@app.get("/api/gps")
def gps():
    result = run(["adb", "shell", "dumpsys", "location"], timeout=20)
    if not result["ok"]:
        return jsonify(ok=False, error=result["stderr"] or "ADB no disponible"), 503

    candidates = []
    pattern = re.compile(
        r"last location=Location\[(gps|fused|network) "
        r"(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)"
        r".*?hAcc=([0-9.]+)"
        r"(?:.*?alt=([-0-9.]+))?"
    )

    for match in pattern.finditer(result["stdout"]):
        candidates.append(
            {
                "provider": match.group(1),
                "latitude": float(match.group(2)),
                "longitude": float(match.group(3)),
                "accuracyMeters": float(match.group(4)),
                "altitudeMeters": float(match.group(5)) if match.group(5) else None,
            }
        )

    priority = {"gps": 0, "fused": 1, "network": 2}
    candidates.sort(key=lambda item: (priority.get(item["provider"], 9), item["accuracyMeters"]))
    return jsonify(ok=True, locations=candidates[:6], best=candidates[0] if candidates else None)


@app.get("/api/continuidad")
def continuidad():
    path = PROJECT / "docs" / "CONTINUAR_AQUI.md"
    text = path.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    return jsonify(
        path=str(path),
        totalLineas=len(lines),
        final="
".join(lines[-80:]),
    )


@app.get("/api/sesiones")
def sesiones():
    result = run(["tmux", "list-sessions", "-F", "#{session_name}"], timeout=5)
    if not result["ok"]:
        return jsonify(sesiones=[])
    return jsonify(sesiones=result["stdout"].splitlines())


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=8765, debug=False)
