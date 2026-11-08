#!/usr/bin/env python3
"""TV Kat Rumah: periksa, backup, pasang dan tetapkan, dari Windows, Mac atau Linux.

Sediakan sekali (dari folder repo):
    python3 -m venv tools/venv                         (Windows: py -3 -m venv tools\\venv)
    tools/venv/bin/pip install -r tools/requirements.txt   (Windows: tools\\venv\\Scripts\\pip ...)

Kemudian, dengan TV_HOST ditetapkan kepada IP TV (atau --tv <ip>):
    python tools/pasang.py periksa            model TV, versi Android, sesuai atau tidak (baca sahaja)
    python tools/pasang.py backup             simpan keadaan TV dalam backup/ (baca sahaja)
    python tools/pasang.py pasang             pasang app dan jadikan Home (dengan perlindungan)
        --matikan-launcher-asal               jika Home asal tetap menang (cth. Xiaomi), matikan ia (pulih menghidupkannya)
    python tools/pasang.py azan               muat turun audio azan (dari repo Clear TV Islamic) ke media/
    python tools/pasang.py tetapan --zon WLY01   hantar media/ (gambar, suara, tetapan) ke TV
        Android 11 ke atas: dihantar melalui halaman tetapan TV; taip PIN yang keluar di TV
    python tools/pasang.py pulih              pulangkan Home dan screensaver asal (app tidak dibuang)

Setiap arahan yang mengubah TV dicatat dalam backup/log.txt.
"""
import argparse
import base64
import datetime
import hashlib
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)

PKG = "com.rumahhakim.tv"
FILES = f"/sdcard/Android/data/{PKG}/files"
APK = os.path.join(ROOT, "dist", "tv-kat-rumah.apk")
MEDIA = os.path.join(ROOT, "media")
BACKUP = os.path.join(ROOT, "backup")
DISABLED = os.path.join(BACKUP, "launcher-dimatikan.txt")
AZAN_URL = "https://raw.githubusercontent.com/Pokka99/cleartv-Islamic/main/themes/islamik/"
HOME_QUERY = "cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN"
NOT_SUPPORTED = ("Amazon",)


def dev():
    import tv   # needs TV_HOST, set from --tv in main()
    return tv.connect()


def sh(d, cmd, timeout=120.0):
    return d.shell(cmd, read_timeout_s=timeout)


def log(text):
    os.makedirs(BACKUP, exist_ok=True)
    with open(os.path.join(BACKUP, "log.txt"), "a", encoding="utf-8") as f:
        f.write(f"{datetime.datetime.now().isoformat(timespec='seconds')}  {text}\n")


def push(d, local, remote):
    """Sends a file in base64 chunks over plain shell commands and checks its md5 (from b64push.py)."""
    data = open(local, "rb").read()
    want = hashlib.md5(data).hexdigest()
    chunk = min(48 * 1024, d.max_chunk_size * 3 // 4 - 64)
    chunk -= chunk % 3
    tmp = remote + ".b64"
    sh(d, f"rm -f '{remote}' '{tmp}'")
    n = (len(data) + chunk - 1) // chunk
    for i in range(n):
        part = base64.b64encode(data[i * chunk:(i + 1) * chunk]).decode()
        if f"ok{i}" not in sh(d, f"echo '{part}' >> '{tmp}' && echo ok{i}", 60.0):
            raise SystemExit(f"gagal menghantar {local}")
    got = sh(d, f"base64 -d '{tmp}' > '{remote}' && rm '{tmp}' && md5sum '{remote}'", 600.0).split()
    if not got or got[0] != want:
        raise SystemExit(f"fail rosak semasa dihantar: {local}")


def home(d):
    return sh(d, HOME_QUERY).strip().splitlines()[-1].strip()


# ---- arahan ------------------------------------------------------------------------------------

def periksa(a):
    d = dev()
    props = {p: sh(d, f"getprop {p}").strip() for p in (
        "ro.product.manufacturer", "ro.product.model", "ro.product.device", "ro.build.version.release",
        "ro.build.version.sdk")}
    for k, v in props.items():
        print(f"{k} = {v}")
    sdk = int(props["ro.build.version.sdk"] or 0)
    print("Home sekarang:", home(d))
    if props["ro.product.manufacturer"] in NOT_SUPPORTED:
        print("KEPUTUSAN: TIDAK SESUAI. Fire TV mengambil semula butang Home daripada launcher lain.")
    elif sdk < 26:
        print(f"KEPUTUSAN: TIDAK SESUAI. Android {props['ro.build.version.release']} (API {sdk}); perlu Android 8 (API 26) atau lebih baru.")
    else:
        print("KEPUTUSAN: SESUAI. Android TV / Google TV, Android 8 atau lebih baru.")
    d.close()


def backup(a):
    d = dev()
    os.makedirs(BACKUP, exist_ok=True)
    cmds = {
        "pakej-semua.txt": "pm list packages -f",
        "pakej-dimatikan.txt": "pm list packages -d",
        "getprop.txt": "getprop",
        "settings-system.txt": "settings list system",
        "settings-secure.txt": "settings list secure",
        "settings-global.txt": "settings list global",
        "home-asal.txt": HOME_QUERY,
        "screensaver-asal.txt": "settings get secure screensaver_components",
    }
    for name, cmd in cmds.items():
        out = sh(d, cmd)
        with open(os.path.join(BACKUP, name), "w", encoding="utf-8") as f:
            f.write(out)
        print(f"{name}: {len(out)} bait")
    d.close()
    print(f"Backup dalam {BACKUP}")


def healthy(d):
    out = sh(d, f"sleep 4; a=$(pidof {PKG}); sleep 3; b=$(pidof {PKG}); "
                "f=$(dumpsys window 2>/dev/null | grep -m1 mCurrentFocus); echo \"$a|$b|$f\"")
    p = out.strip().splitlines()[-1].split("|", 2) if out.strip() else []
    return len(p) == 3 and p[0] and p[0] == p[1] and f"{PKG}/{PKG}.MainActivity" in p[2]


def pasang(a):
    apk = a.apk or APK
    if not os.path.isfile(apk):
        raise SystemExit(f"tiada APK: {apk}")
    if not os.path.isfile(os.path.join(BACKUP, "home-asal.txt")):
        raise SystemExit("Jalankan 'backup' dahulu, supaya Home asal boleh dipulangkan.")
    d = dev()
    sdk = int(sh(d, "getprop ro.build.version.sdk").strip() or 0)
    if sdk < 26 or sh(d, "getprop ro.product.manufacturer").strip() in NOT_SUPPORTED:
        raise SystemExit("TV ini tidak sesuai (jalankan 'periksa'). Tiada apa-apa diubah.")
    orig = home(d)
    print("Home sekarang:", orig)
    print("Menghantar APK...")
    push(d, apk, "/data/local/tmp/tv-kat-rumah.apk")
    out = sh(d, "pm install -r /data/local/tmp/tv-kat-rumah.apk; rm -f /data/local/tmp/tv-kat-rumah.apk", 300.0)
    print(out.strip())
    if "Success" not in out:
        raise SystemExit("pm install gagal; tiada apa-apa lain diubah.")
    log(f"pm install {PKG} (undo: tools/pasang.py pulih; pm uninstall {PKG})")
    for c in (f"appops set {PKG} SYSTEM_ALERT_WINDOW allow",
              f"pm grant {PKG} android.permission.ACCESS_FINE_LOCATION",
              f"cmd package compile -m speed -f {PKG}"):
        sh(d, c + " 2>/dev/null; true", 300.0)
    try:
        # Must run on this TV before it becomes Home.
        sh(d, f"am start -W -n {PKG}/.MainActivity")
        if not healthy(d):
            raise RuntimeError("app tidak berjalan dengan betul pada TV ini")
        sh(d, f"cmd package set-home-activity --user 0 {PKG}/.MainActivity")
        now = home(d)
        if not now.startswith(PKG + "/") and "/" in now and a.matikan_launcher_asal:
            # Some TVs (Xiaomi, Google TV) keep their own launcher above any default; switched off, not removed.
            other = now.split("/")[0]
            sh(d, f"pm disable-user --user 0 {other}")
            os.makedirs(BACKUP, exist_ok=True)
            with open(DISABLED, "a", encoding="utf-8") as f:
                f.write(other + "\n")
            log(f"pm disable-user {other} (undo: pm enable {other}; tools/pasang.py pulih)")
            print("Launcher asal dimatikan (tidak dibuang):", other)
            sh(d, f"cmd package set-home-activity --user 0 {PKG}/.MainActivity")
            now = home(d)
        if not now.startswith(PKG + "/"):
            hint = "" if a.matikan_launcher_asal else " (cuba --matikan-launcher-asal, dengan izin pemilik TV)"
            raise RuntimeError(f"Home menghala ke {now}{hint}")
        sh(d, "input keyevent KEYCODE_HOME")
        if not healthy(d):
            raise RuntimeError("butang Home tidak membuka TV Kat Rumah yang berfungsi")
        log(f"set-home-activity {PKG}/.MainActivity (undo: set-home-activity {orig})")
        if not a.tanpa_screensaver:
            sh(d, f"settings put secure screensaver_components {PKG}/.ClockDream")
            log(f"screensaver -> {PKG}/.ClockDream (undo: tools/pasang.py pulih)")
    except RuntimeError as e:
        print("RALAT:", e, "- memulangkan Home asal...")
        enable_launchers(d)
        if "/" in orig and not orig.startswith(PKG):
            sh(d, f"cmd package set-home-activity --user 0 {orig}")
        sh(d, "input keyevent KEYCODE_HOME")
        raise SystemExit("Pemasangan dihentikan; Home asal dipulangkan. App masih terpasang tetapi bukan Home.")
    d.close()
    print("Siap: TV Kat Rumah kini Home. Seterusnya: tools/pasang.py tetapan --zon <ZON>")


def azan(a):
    for f in ("azan.mp3", "azan-subuh.mp3"):
        out = os.path.join(MEDIA, f)
        if os.path.exists(out) and not a.semula:
            print(f"{f} sudah ada")
            continue
        print(f"Memuat turun {f} dari Clear TV Islamic...")
        req = urllib.request.Request(AZAN_URL + f, headers={"User-Agent": "tv-kat-rumah"})
        with urllib.request.urlopen(req, timeout=120) as r, open(out, "wb") as w:
            w.write(r.read())
        print(f"{f}: {os.path.getsize(out) // 1024} KB")


def enable_launchers(d):
    """Switches back on the launchers --matikan-launcher-asal switched off."""
    if not os.path.isfile(DISABLED):
        return
    for p in sorted({x.strip() for x in open(DISABLED, encoding="utf-8") if x.strip()}):
        sh(d, f"pm enable {p}")
        print("Launcher asal dihidupkan semula:", p)
        log(f"pm enable {p}")
    os.remove(DISABLED)


def tetapan(a):
    folder = os.path.abspath(a.folder or MEDIA)
    cfg_path = os.path.join(folder, "config.json")
    cfg = json.load(open(cfg_path, encoding="utf-8")) if os.path.isfile(cfg_path) else {}
    if a.zon:
        cfg["prayer_zone"] = a.zon.strip().upper()
    if not cfg.get("prayer_zone"):
        print("AMARAN: tiada zon solat (--zon). Waktu solat dan azan tidak akan keluar.")
    d = dev()
    sh(d, f"mkdir -p {FILES}/zikir {FILES}/surah {FILES}/masjid")
    # Android 11 and later keep the computer out of the app's folder (mkdir says nothing, writes fail):
    # send everything through the app's own settings page instead, with the PIN on the TV.
    if "boleh" not in sh(d, f"echo x > {FILES}/.tulis 2>/dev/null && rm {FILES}/.tulis && echo boleh"):
        print("Android ini tidak membenarkan komputer menulis ke folder app; fail dihantar melalui halaman tetapan TV.")
        tetapan_web(os.environ["TV_HOST"].split(":")[0], folder, cfg)
        restart(d)
        d.close()
        return
    tmp = os.path.join(BACKUP, "config-dihantar.json")
    os.makedirs(BACKUP, exist_ok=True)
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(cfg, f, ensure_ascii=False, indent=2)
    push(d, tmp, f"{FILES}/config.json")
    print("config.json dihantar")
    for name in ("azan.jpg", "peringatan.jpg", "azan.mp3", "azan-subuh.mp3"):
        p = os.path.join(folder, name)
        if os.path.isfile(p):
            if name.endswith(".jpg"):
                stem = name[:-4]
                sh(d, f"rm -f {FILES}/{stem}.jpg {FILES}/{stem}.jpeg {FILES}/{stem}.png {FILES}/{stem}.webp")
            push(d, p, f"{FILES}/{name}")
            print(name, "dihantar")
    masjid = os.path.join(folder, "masjid")
    if os.path.isdir(masjid):
        sh(d, f"rm -rf {FILES}/masjid; mkdir -p {FILES}/masjid")
        for n in sorted(os.listdir(masjid)):
            if n.endswith(".jpg") or n == "captions.json":
                push(d, os.path.join(masjid, n), f"{FILES}/masjid/{n}")
        print("gambar latar dihantar")
    for sub, types in (("zikir", (".mp3", ".m4a", ".ogg", ".wav")), ("surah", (".mp3", ".m4a", ".mp4"))):
        src = os.path.join(folder, sub)
        if not os.path.isdir(src):
            continue
        on_tv = sh(d, f"ls -l {FILES}/{sub}/ 2>/dev/null")
        for n in sorted(os.listdir(src)):
            p = os.path.join(src, n)
            if not n.lower().endswith(types):
                continue
            if f" {os.path.getsize(p)} " in on_tv and n in on_tv:
                continue   # same size already there
            push(d, p, f"{FILES}/{sub}/{n}")
            print(f"{sub}/{n} dihantar")
    restart(d)
    log(f"tetapan dihantar dari {folder}")
    d.close()
    print("Siap. Tetapan dari telefon: buka http://<IP TV>:8080 di Wi-Fi rumah.")


def restart(d):
    """The settings screen sets the alarms again and fetches the prayer times; Home restarts with it all."""
    sh(d, f"am start -W -n {PKG}/.TetapanActivity; sleep 4; am force-stop {PKG}; input keyevent KEYCODE_HOME")


def tetapan_web(host, folder, cfg):
    """The same as tetapan, through http://<TV>:8080: the TV shows a PIN, the person types it here."""
    base, cookie = f"http://{host}:8080", [""]

    def call(path, data=b"", ctype="application/octet-stream", timeout=60):
        req = urllib.request.Request(base + path, data=data, method="POST")
        req.add_header("Content-Type", ctype)
        if cookie[0]:
            req.add_header("Cookie", cookie[0])
        try:
            with urllib.request.urlopen(req, timeout=timeout) as r:
                if r.headers.get("Set-Cookie"):
                    cookie[0] = r.headers["Set-Cookie"].split(";")[0]
                return json.loads(r.read() or b"{}")
        except urllib.error.HTTPError as e:
            return json.loads(e.read() or b"{}")
        except OSError as e:
            raise SystemExit(f"Halaman tetapan TV tidak dapat dihubungi ({e}). Pastikan TV Kat Rumah ialah Home (langkah pasang).")

    def upload(jenis, path, nama=None):
        nama = nama or os.path.basename(path)
        q = urllib.parse.urlencode({"jenis": jenis, "nama": nama})
        with open(path, "rb") as f:
            data = f.read()
        res = call(f"/api/naik?{q}", data, timeout=900)
        print(f"  {jenis:18} {nama:22} {len(data) // 1024:>7} KB  {'OK' if res.get('ok') else 'GAGAL: ' + str(res.get('ralat'))}")
        return bool(res.get("ok"))

    call("/api/pin")
    print("PIN 4 angka kini keluar di penjuru kanan atas skrin TV (sah 2 minit).")
    try:
        pin = input("Taip PIN itu: ").strip() if sys.stdin and sys.stdin.isatty() else ""
    except EOFError:
        pin = ""
    if not pin:
        # The PIN is the person's to type (AGENTS.md, ground rule 4), so an AI running this stops here.
        raise SystemExit("Perlu PIN dari skrin TV. Pemilik TV perlu menjalankan arahan ini sendiri di terminal "
                         "mereka dan menaip PIN itu (AGENTS.md, langkah 7).")
    res = call("/api/masuk", urllib.parse.urlencode({"pin": pin}).encode(), "application/x-www-form-urlencoded")
    if not res.get("ok"):
        raise SystemExit(f"Log masuk gagal: {res.get('ralat')}")
    res = call("/api/tetapan", json.dumps(cfg).encode(), "application/json")
    print(f"config.json (zon {cfg.get('prayer_zone')}): {'OK' if res.get('ok') else 'GAGAL: ' + str(res.get('ralat'))}")
    failed = 0 if res.get("ok") else 1
    on_tv = call("/api/status").get("fail", {})
    for name, jenis in (("azan.jpg", "azan-gambar"), ("peringatan.jpg", "peringatan-gambar"),
                        ("azan.mp3", "azan-bunyi"), ("azan-subuh.mp3", "azan-bunyi")):
        p = os.path.join(folder, name)
        if os.path.isfile(p):
            failed += not upload(jenis, p)
    for sub, exts in (("masjid", (".jpg", "captions.json")), ("zikir", (".mp3", ".m4a", ".ogg", ".wav")),
                      ("surah", (".mp3", ".m4a", ".mp4"))):
        src = os.path.join(folder, sub)
        for n in sorted(os.listdir(src)) if os.path.isdir(src) else []:
            if not n.lower().endswith(exts):
                continue
            if sub == "surah" and n.lower() in on_tv.get("surah", []):
                print(f"  {sub:18} {n:22}  sudah ada di TV (padam dari halaman telefon untuk menggantinya)")
                continue
            failed += not upload(sub, os.path.join(src, n))
    log(f"tetapan dihantar dari {folder} melalui halaman tetapan TV")
    if failed:
        raise SystemExit(f"{failed} fail gagal; jalankan sekali lagi untuk cuba semula.")
    print("Siap. Tetapan dari telefon: buka http://<IP TV>:8080 di Wi-Fi rumah.")


def pulih(a):
    d = dev()
    orig = ""
    p = os.path.join(BACKUP, "home-asal.txt")
    if os.path.isfile(p):
        lines = [x.strip() for x in open(p, encoding="utf-8").read().splitlines() if x.strip()]
        orig = lines[-1] if lines else ""
    if "/" in orig and not orig.startswith(PKG):
        sh(d, f"cmd package set-home-activity --user 0 {orig}")
        print("Home dipulangkan kepada", orig)
    else:
        print("Tiada Home asal dalam backup/; pilih launcher di Tetapan TV, atau 'pm uninstall' app ini.")
    enable_launchers(d)
    s = os.path.join(BACKUP, "screensaver-asal.txt")
    if os.path.isfile(s):
        comp = open(s, encoding="utf-8").read().strip()
        if comp and comp != "null":
            sh(d, f"settings put secure screensaver_components {comp}")
            print("Screensaver dipulangkan kepada", comp)
    sh(d, "input keyevent KEYCODE_HOME")
    log("pulih: Home dan screensaver asal dipulangkan")
    d.close()


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--tv", help="IP TV (atau tetapkan TV_HOST)")
    sub = ap.add_subparsers(dest="arahan", required=True)
    sub.add_parser("periksa")
    sub.add_parser("backup")
    p = sub.add_parser("pasang")
    p.add_argument("--apk")
    p.add_argument("--tanpa-screensaver", action="store_true")
    p.add_argument("--matikan-launcher-asal", action="store_true",
                   help="matikan (bukan buang) launcher asal jika ia tetap menjadi Home; pulih menghidupkannya")
    z = sub.add_parser("azan")
    z.add_argument("--semula", action="store_true")
    t = sub.add_parser("tetapan")
    t.add_argument("--folder")
    t.add_argument("--zon", help="zon JAKIM, contoh WLY01")
    sub.add_parser("pulih")
    a = ap.parse_args()
    if a.tv:
        os.environ["TV_HOST"] = a.tv
    if a.arahan != "azan" and not os.environ.get("TV_HOST"):
        raise SystemExit("Beri --tv <IP TV> atau tetapkan TV_HOST")
    {"periksa": periksa, "backup": backup, "pasang": pasang, "azan": azan, "tetapan": tetapan, "pulih": pulih}[a.arahan](a)


if __name__ == "__main__":
    main()
