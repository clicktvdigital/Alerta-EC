"""Comprobación de decodificación para CI; no requiere Gradle ni modifica el MP4."""
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parent.parent
VIDEO = ROOT / "app/src/main/res/raw/alertaec_cinematic_intro.mp4"
OUTPUT = ROOT / "build/intro-review"


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    probe = subprocess.check_output([
        "ffprobe", "-v", "error", "-show_streams", "-show_format", "-of", "json", str(VIDEO)
    ], text=True)
    (OUTPUT / "metadata.json").write_text(probe)
    metadata = json.loads(probe)
    video = next(s for s in metadata["streams"] if s["codec_type"] == "video")
    assert video["codec_name"] == "h264", "Revisar compatibilidad del codec"
    assert video["pix_fmt"] == "yuv420p", "Se requiere vídeo de 8 bits YUV 4:2:0"
    assert 0 < float(metadata["format"]["duration"]) < 30, "Revisar límite de reproducción"
    subprocess.run([
        "ffmpeg", "-v", "error", "-xerror", "-i", str(VIDEO), "-f", "null", "-"
    ], check=True)
    # Primer fotograma exacto y muestras tempranas para revisión visual.
    subprocess.run([
        "ffmpeg", "-v", "error", "-y", "-i", str(VIDEO), "-vf",
        "select='eq(n,0)+eq(n,1)+eq(n,15)+eq(n,30)'", "-vsync", "0",
        "-frames:v", "4", str(OUTPUT / "frame-%02d.png")
    ], check=True)
    pixels = subprocess.check_output([
        "ffmpeg", "-v", "error", "-i", str(VIDEO), "-t", "1", "-vf",
        "scale=32:32,format=gray", "-f", "rawvideo", "-"
    ])
    assert pixels and max(pixels) > 32, "Primer segundo negro: revisar/exportar vídeo"
    print("MP4 decodificable; revisar build/intro-review y probar hardware Android")


if __name__ == "__main__":
    main()
