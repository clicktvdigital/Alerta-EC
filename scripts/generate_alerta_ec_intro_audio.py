#!/usr/bin/env python3
"""Deterministic, original procedural sound design for the Alerta EC intro."""
from __future__ import annotations
import hashlib
import math
import random
import struct
import wave
from pathlib import Path

RATE = 22050
OUT = Path(__file__).resolve().parents[1] / "app/src/main/res/raw"


def render(name: str, duration: float, seed: int, synth) -> tuple[Path, float, str]:
    rng = random.Random(seed)
    count = round(RATE * duration)
    values = []
    previous = 0.0
    for i in range(count):
        t = i / RATE
        raw, previous = synth(t, rng, previous)
        # Smooth 100 ms edges prevent clicks and keep the authored peaks gentle.
        edge = min(1.0, t / 0.10, (duration - t) / 0.20)
        values.append(raw * max(0.0, edge))
    peak = max(abs(v) for v in values) or 1.0
    gain = 0.68 / peak
    pcm = b"".join(struct.pack("<h", round(max(-0.68, min(0.68, v * gain)) * 32767)) for v in values)
    path = OUT / f"{name}.wav"
    with wave.open(str(path), "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(RATE)
        wav.writeframes(pcm)
    with wave.open(str(path), "rb") as wav:
        assert wav.getnchannels() == 1 and wav.getsampwidth() == 2 and wav.getframerate() == RATE
        assert wav.getnframes() == count
        samples = struct.unpack("<" + "h" * count, wav.readframes(count))
        actual_peak = max(abs(s) for s in samples) / 32768
        assert actual_peak <= 0.681
    return path, duration, hashlib.sha256(pcm).hexdigest()


def wind(t, rng, prev):
    n = rng.uniform(-1, 1)
    low = prev * .992 + n * .008
    swell = .55 + .25 * math.sin(2 * math.pi * .19 * t) + .12 * math.sin(2 * math.pi * .37 * t)
    return low * swell, low


def rain(t, rng, prev):
    n = rng.uniform(-1, 1)
    low = prev * .35 + n * .65
    patter = (rng.random() ** 5) * 1.7
    return low * .18 + patter * (0.45 + 0.25 * math.sin(2 * math.pi * .7 * t)), low


def volcano(t, rng, prev):
    n = rng.uniform(-1, 1)
    low = prev * .997 + n * .003
    pulse = .55 + .45 * math.sin(2 * math.pi * .48 * t) ** 2
    return .68 * low + .20 * math.sin(2 * math.pi * (43 + 3 * math.sin(t)) * t) + .12 * math.sin(2 * math.pi * 67 * t), low


def storm(t, rng, prev):
    n = rng.uniform(-1, 1)
    low = prev * .994 + n * .006
    swell = math.sin(math.pi * t / 1.8) ** 2
    distant_roll = math.sin(2 * math.pi * (39 + 2 * t) * t) * .34
    return swell * (low * .72 + distant_roll), low


def seismic(t, rng, prev):
    n = rng.uniform(-1, 1)
    low = prev * .98 + n * .02
    envelope = math.exp(-3.0 * t) * (0.75 + .25 * math.sin(2 * math.pi * 8 * t))
    return envelope * (.65 * math.sin(2 * math.pi * 48 * t) + .25 * low), low


def landslide(t, rng, prev):
    n = rng.uniform(-1, 1)
    low = prev * .96 + n * .04
    rumble = math.sin(2 * math.pi * (37 + 7 * t) * t) * .45
    grit = (rng.random() ** 3) * .5
    return (rumble + low * .3 + grit) * min(1.0, t * 1.2) * max(.2, 1 - t / 1.55), low


def closing(t, rng, prev):
    fade = max(0.0, 1 - t / 1.35)
    chord = (math.sin(2 * math.pi * 220 * t) + .55 * math.sin(2 * math.pi * 277.18 * t) + .4 * math.sin(2 * math.pi * 329.63 * t)) / 1.95
    return chord * fade * .7, prev


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    tracks = [
        ("alertaec_intro_wind", 5.0, 1101, wind),
        ("alertaec_intro_rain", 2.0, 1102, rain),
        ("alertaec_intro_storm", 1.8, 1107, storm),
        ("alertaec_intro_volcano", 1.5, 1103, volcano),
        ("alertaec_intro_seismic", 1.2, 1104, seismic),
        ("alertaec_intro_landslide", 1.4, 1105, landslide),
        ("alertaec_intro_closing", 1.4, 1106, closing),
    ]
    for name, duration, seed, synth in tracks:
        path, seconds, digest = render(name, duration, seed, synth)
        print(f"{path.name}: {seconds:.1f}s, {path.stat().st_size} bytes, sha256(pcm)={digest}")
