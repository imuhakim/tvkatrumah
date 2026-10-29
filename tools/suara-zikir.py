#!/usr/bin/env python3
"""Jana suara 24 zikir dengan ElevenLabs, ke media/zikir/01.mp3 hingga 24.mp3 (menggantikan yang ada).

Kunci API anda sendiri, dalam pemboleh ubah persekitaran (jangan tulis dalam fail):
    $env:ELEVENLABS_API_KEY = "..."            (PowerShell, tetingkap itu sahaja)

Guna:
    py tools/suara-zikir.py --senarai-suara      senarai suara dalam akaun anda (nama dan voice_id)
    py tools/suara-zikir.py --suara <voice_id>   jana semua yang belum ada
    py tools/suara-zikir.py --suara <voice_id> --zikir 1,5 --semula   jana semula zikir 1 dan 5 sahaja

Teks diambil daripada Zikir.java (tulisan Arab berbaris). Setiap zikir diulang (paling banyak 7 kali)
supaya bacaannya sekitar 10 hingga 12 saat; app menghentikan bunyi pada saat ke-15. Dengar setiap fail sebelum dihantar ke TV:
sebutan Arab mesin tidak selalu tepat.
"""
import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ZIKIR_JAVA = os.path.join(ROOT, "app", "src", "main", "java", "com", "rumahhakim", "tv", "Zikir.java")
OUT = os.path.join(ROOT, "media", "zikir")
API = "https://api.elevenlabs.io/v1"
MODEL = "eleven_multilingual_v2"
# Reading speed measured on a Malay ElevenLabs voice: about 16 letters (with their marks) a second, and a pause of
# about 0.8 s between repeats. A zikir is repeated as often as fits in LONGEST seconds (at most
# seven times); the app cuts the sound at 15 s.
LETTERS_PER_SECOND = 16
PAUSE_SECONDS = 0.8
LONGEST = 13
MAX_REPEATS = 7


def seconds(letters, repeats):
    return repeats * letters / LETTERS_PER_SECOND + PAUSE_SECONDS * (repeats - 1)


def repeats_for(letters):
    return max([r for r in range(1, MAX_REPEATS + 1) if seconds(letters, r) <= LONGEST] or [1])


def zikir():
    src = open(ZIKIR_JAVA, encoding="utf-8").read()
    return [(int(n), ar) for n, ar in re.findall(r'new Zikir\((\d+),\s*"([^"]+)"', src)]


def call(path, key, body=None):
    req = urllib.request.Request(API + path, headers={"xi-api-key": key, "Content-Type": "application/json"},
                                 data=json.dumps(body).encode("utf-8") if body is not None else None,
                                 method="POST" if body is not None else "GET")
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            return r.read()
    except urllib.error.HTTPError as e:
        sys.exit(f"ElevenLabs {e.code}: {e.read().decode('utf-8', 'replace')[:300]}")


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--suara", help="voice_id ElevenLabs")
    p.add_argument("--senarai-suara", action="store_true")
    p.add_argument("--zikir", help="nombor zikir, contoh 1,5,24 (lalai: semua)")
    p.add_argument("--semula", action="store_true", help="jana semula walaupun fail sudah ada")
    p.add_argument("--contoh", action="store_true",
                   help="contoh untuk didengar dahulu: zikir 1 dan 15 ke media/contoh-suara/, bukan media/zikir/")
    a = p.parse_args()
    key = os.environ.get("ELEVENLABS_API_KEY", "").strip()
    if not key:
        sys.exit('Tetapkan dahulu: $env:ELEVENLABS_API_KEY = "kunci anda" (dalam tetingkap PowerShell ini)')
    if a.senarai_suara:
        for v in json.loads(call("/voices", key))["voices"]:
            print(f'{v["voice_id"]}  {v["name"]}  ({v.get("labels", {}).get("accent", "")})')
        return
    if not a.suara:
        sys.exit("Beri --suara <voice_id> (lihat --senarai-suara)")
    only = {int(x) for x in a.zikir.split(",")} if a.zikir else None
    folder = OUT
    if a.contoh:
        only = only or {1, 15}
        folder = os.path.join(ROOT, "media", "contoh-suara")
    os.makedirs(folder, exist_ok=True)
    for n, arabic in zikir():
        if only and n not in only:
            continue
        out = os.path.join(folder, f"{a.suara}-{n:02d}.mp3" if a.contoh else f"{n:02d}.mp3")
        if os.path.exists(out) and not a.semula:
            print(f"{n:02d} sudah ada, dilangkau")
            continue
        repeats = repeats_for(len(arabic))
        text = " ... ".join([arabic] * repeats)
        audio = call(f"/text-to-speech/{a.suara}?output_format=mp3_44100_128", key,
                     {"text": text, "model_id": MODEL,
                      "voice_settings": {"stability": 0.6, "similarity_boost": 0.8}})
        with open(out, "wb") as f:
            f.write(audio)
        # mp3 at 128 kbit/s: 16 KB a second
        print(f"{n:02d} -> {out} ({len(audio) // 1024} KB, ~{len(audio) // 16000} saat, {repeats}x)")
    if a.contoh:
        print(f"Siap. Dengar contoh dalam {folder}")
    else:
        print("Siap. Dengar fail-fail itu, kemudian jalankan tools/pasang.py tetapan untuk menghantarnya ke TV.")


if __name__ == "__main__":
    main()
