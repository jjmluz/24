#!/usr/bin/env python3
"""Synthesises the game's sound effects into app/src/main/res/raw. Run from the repo root."""
import math
import struct
import wave

RATE = 44100
OUT = "app/src/main/res/raw"


def tone(freq, length, decay, volume=1.0, glide=1.0):
    """A soft mallet note: a sine with a quiet octave, a short attack and an exponential decay."""
    samples = []
    phase = 0.0
    for i in range(int(length * RATE)):
        t = i / RATE
        phase += 2 * math.pi * freq * glide ** (t / length) / RATE
        attack = min(1.0, t / 0.003)
        samples.append(volume * attack * math.exp(-t / decay) * (math.sin(phase) + 0.25 * math.sin(2 * phase)))
    return samples


def mix(*notes):
    """Layers (start seconds, samples) pairs."""
    out = [0.0] * max(int(start * RATE) + len(s) for start, s in notes)
    for start, s in notes:
        offset = int(start * RATE)
        for i, v in enumerate(s):
            out[offset + i] += v
    return out


def save(name, samples, peak=0.7):
    # A short fade keeps the end of each sound from clicking.
    fade = min(len(samples), int(0.01 * RATE))
    for i in range(fade):
        samples[-1 - i] *= i / fade
    scale = peak / max(abs(v) for v in samples) * 32767
    with wave.open(f"{OUT}/{name}.wav", "wb") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(RATE)
        f.writeframes(b"".join(struct.pack("<h", int(v * scale)) for v in samples))


save("pick_number", tone(660, 0.10, 0.03), peak=0.45)
save("pick_op", tone(880, 0.10, 0.03), peak=0.45)
save("merge", mix((0, tone(659, 0.16, 0.06)), (0.08, tone(880, 0.22, 0.08))), peak=0.55)
save("undo", tone(520, 0.12, 0.05, glide=0.7), peak=0.4)
save("blocked", tone(160, 0.16, 0.05), peak=0.6)
save("miss", mix((0, tone(330, 0.25, 0.1)), (0.16, tone(247, 0.4, 0.16))), peak=0.55)
save(
    "solved",
    mix(
        (0, tone(523, 0.3, 0.14)),
        (0.1, tone(659, 0.3, 0.14)),
        (0.2, tone(784, 0.3, 0.14)),
        (0.3, tone(1047, 0.7, 0.28)),
    ),
)
save(
    "deal",
    mix(
        (0, tone(392, 0.7, 0.3)),
        (0.09, tone(494, 0.7, 0.3)),
        (0.18, tone(587, 0.8, 0.34)),
    ),
    peak=0.4,
)
