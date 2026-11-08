# TV Kat Rumah

**Skrin Home Islamik untuk Android TV, diurus dari telefon.** Waktu solat JAKIM di bawah jam, azan
skrin penuh pada setiap waktu, peringatan 5 minit sebelum azan, zikir berbeza dengan suara setiap jam,
bacaan surah, Doa Pagi dan video Asmaul Husna mengikut jadual, dan screensaver gambar masjid atau video
(MP4 tanpa bunyi di belakang jam, dimuat naik dari telefon, dengan butang Uji screensaver). TV boleh hidup sendiri untuk azan dan zikir,
kemudian tidur semula jika tiada orang. Semua tetapan diubah dari pelayar telefon, tanpa komputer.

Berasaskan launcher [Clear TV Islamic](https://github.com/Pokka99/cleartv-Islamic) (MIT), yang pula
berasaskan [Clean Your TV](https://github.com/Gathin23/cleanyourtv).

## Cara paling mudah: minta AI pasangkan

Buka Claude Code, Codex atau mana-mana AI yang boleh menjalankan arahan di komputer anda, dan tampal:

> Clone https://github.com/imuhakim/tvkatrumah dan ikut AGENTS.md dalam repo itu untuk pasang TV Kat Rumah di TV saya.
> Model TV: (contoh Sony KD-55X7500F). IP TV: (contoh 192.168.0.10). Zon JAKIM: (contoh WLY01).
> Semak model TV dulu, buat backup sebelum apa-apa perubahan, dan tanya saya sebelum setiap langkah yang mengubah TV.

[AGENTS.md](AGENTS.md) ialah buku panduan langkah demi langkah untuk AI: semak model TV, backup, pasang
dengan perlindungan, hantar tetapan, uji, dan serahkan kepada anda. AI akan bertanya sebelum setiap
perubahan pada TV.

## Kelebihan

**Berbanding skrin Home asal TV:** tiada iklan atau baris cadangan, waktu solat sentiasa di depan mata,
dan TV menjadi peringatan solat untuk seisi rumah.

**Berbanding Clear TV Islamic** (yang menjadi asasnya; semua cirinya dikekalkan):

| | Clear TV Islamic | TV Kat Rumah |
|---|---|---|
| Waktu solat JAKIM, azan, screensaver Islamik | Ada | Ada |
| Azan semasa menonton YouTube, Netflix, siaran TV | Jalur di bawah skrin | **Skrin penuh** dengan gambar pilihan sendiri |
| Peringatan sebelum azan | Tiada | **"Lagi 5 minit nak azan"**, skrin penuh dan 3 bunyi beep |
| Zikir | Tiada | **24 zikir berbeza, satu setiap jam**, dengan suara, tulisan Arab, rumi dan maksud |
| Bacaan surah | Tiada | **Mengikut jadual, hingga ke minit** (cth. al-Waqiah 4 pagi, Doa Pagi 6:30, Asmaul Husna 7 pagi, al-Kahfi pagi Jumaat, al-Mulk 10 malam) |
| Video | Tiada | **Skrin penuh** dari jadual (`.mp4`) |
| TV hidup sendiri | Tiada | **Hidup untuk azan, zikir dan surah**, tidur semula jika tiada siapa tekan remote |
| Ubah tetapan | Komputer sahaja | **Dari telefon** (http://IP-TV:8080, dilindungi PIN) |
| Tukar gambar dan rakaman | Komputer sahaja | **Dari galeri telefon**; gambar dikecilkan sendiri |
| Cahaya warna app di belakang Home | Sentiasa | Boleh dimatikan (`ambient_glow`), mati secara lalai |
| Pemasang | Skrip shell / PowerShell | **Satu skrip Python** untuk Windows, Mac dan Linux |

## TV yang sesuai

| TV atau kotak | Sesuai? |
|---|---|
| **Android TV / Google TV, Android 8 ke atas**: Sony BRAVIA (2016+), TCL, Xiaomi Mi TV dan Mi Box, Philips Android TV, Sharp, Hisense dan Toshiba (model Android), Chromecast with Google TV, NVIDIA Shield, projektor Android | **Ya** |
| Android TV 5, 6 atau 7 | Tidak (perlu Android 8) |
| Amazon Fire TV | Tidak (Fire OS mengambil semula butang Home) |
| Samsung (Tizen), LG (webOS), Hisense/Toshiba VIDAA, Roku, Apple TV, dekoder lama | Tidak |

Yang penting ialah sistem TV, bukan jenamanya. Semak di **Settings → About**: Android TV menunjukkan
"Android TV OS" atau versi Android. Untuk TV yang tidak sesuai, **Google TV / Android TV box** (contohnya
Chromecast with Google TV atau Xiaomi TV Stick) di port HDMI memberikan semua ciri ini.

Sudah diuji: **Sony KD-55X7500F** (BRAVIA 4K, Android 9) dan **Xiaomi Mi TV** (Android TV 11, dengan dua
langkah tambahan di bawah).

## Pasang sendiri (tanpa AI)

Ringkasan; setiap langkah dan semakannya ada dalam [AGENTS.md](AGENTS.md).

1. Di TV: hidupkan **Developer options → USB debugging** (dan **Network debugging** jika ada), dan catat IP TV.
2. Di komputer (Python 3):
   ```
   git clone https://github.com/imuhakim/tvkatrumah
   cd tvkatrumah
   python3 -m venv tools/venv
   tools/venv/bin/pip install -r tools/requirements.txt
   ```
   Windows: `py -3 -m venv tools\venv` dan `tools\venv\Scripts\pip install -r tools\requirements.txt`;
   di bawah, gantikan `tools/venv/bin/python` dengan `tools\venv\Scripts\python.exe`.
3. ```
   tools/venv/bin/python tools/pasang.py --tv 192.168.0.10 periksa     # sesuai atau tidak (tekan "Always allow" di TV)
   tools/venv/bin/python tools/pasang.py --tv 192.168.0.10 backup      # simpan keadaan TV
   tools/venv/bin/python tools/pasang.py azan                          # audio azan, ~9 MB (pilihan)
   tools/venv/bin/python tools/pasang.py --tv 192.168.0.10 pasang      # pasang dan jadikan Home
   tools/venv/bin/python tools/pasang.py --tv 192.168.0.10 tetapan --zon WLY01
   ```
   Android 11 ke atas: `tetapan` menghantar melalui halaman tetapan TV, dan meminta PIN yang keluar di TV.
   Jika launcher asal tetap menjadi Home (contohnya Xiaomi), `pasang --matikan-launcher-asal` mematikannya
   (tidak membuang); `pulih` menghidupkannya semula.
4. Buka **http://192.168.0.10:8080** di telefon (Wi-Fi yang sama) dan tekan **"Tunjuk PIN di TV"**.

Untuk memulangkan Home dan screensaver asal: `tools/pasang.py --tv <ip> pulih`.

## Tetapan dari telefon

Buka **http://IP-TV:8080** di telefon yang menggunakan Wi-Fi rumah, tekan "Tunjuk PIN di TV" dan
masukkan PIN 4 digit yang keluar di skrin TV. Telefon itu kekal log masuk selepas itu. Dari situ:

- hidup/matikan zikir, azan dan TV hidup sendiri; julat jam zikir; zon solat; minit peringatan
- jadual surah: jam, hari, nama, fail MP3, dan "zikir dahulu"
- muat naik dari galeri: gambar azan, gambar peringatan, gambar latar Home, suara zikir, MP3 atau video
  (`.mp4`) bacaan, dan azan
- butang **Uji zikir**, **Uji azan** dan **Uji surah**

5 PIN salah mengunci halaman selama 5 minit; fail hanya boleh ditulis dalam folder app di TV.

## Tetapan (`media/config.json`)

| Kunci | Maksud | Dalam `media/` |
|---|---|---|
| `prayer_zone` | Zon JAKIM (cth. `WLY01`); kosong = tiada waktu solat dan azan | kosong: `--zon` mengisinya |
| `azan` | Skrin azan pada setiap waktu solat | `true` |
| `peringatan_minit` | Peringatan sebelum azan, dalam minit; 0 = tiada | `5` |
| `zikir`, `jam_mula`, `jam_akhir` | Zikir setiap jam, dan julat jamnya | `true`, 0 hingga 23 |
| `jarak_solat_minit` | Langkau zikir jika waktu solat dalam sekian minit | `10` |
| `auto_hidup` | Hidupkan TV untuk azan, zikir dan surah; tidurkan semula jika tiada orang | `true` |
| `surah` | Jadual bacaan: `jam`, `minit` (pilihan), `hari` (pilihan, cth. `"jumaat"`), `nama`, `fail` (`.mp3`, `.m4a`, `.mp4`), `zikir_dulu` | 8 bacaan |
| `dock_apps` | Empat app di dock, mengikut susunan | Netflix, YouTube, unifi TV, RTMKlik |
| `wallpaper_minutes` | Gambar latar bertukar setiap sekian minit | `1` |
| `ambient_glow` | Cahaya warna app terpilih di belakang Home | `false` |
| `tetapan_telefon` | Halaman tetapan telefon di port 8080 | `true` |

Semua kunci tema Clear TV Islamic juga diterima; lihat
[themes/README](https://github.com/Pokka99/cleartv-Islamic/blob/main/themes/README.md).

## Media dalam repo ini

- `media/masjid/`: 13 gambar masjid dari Wikimedia Commons (Masjidil Haram, Masjid Nabawi, Masjid Kristal,
  Masjid Putra dan lain-lain), dengan nama dan kredit jurugambar dalam `captions.json`.
  `media/azan.jpg` dan `media/peringatan.jpg` ialah dua daripadanya. Ganti dengan gambar sendiri bila-bila
  masa dari telefon.
- `media/zikir/`: 24 zikir dalam **suara pemilik repo ini sendiri**, diklon dengan ElevenLabs. Suara ini
  **tidak** berlesen MIT: ia hanya untuk dimainkan bersama TV Kat Rumah, dan tidak boleh diklon atau digunakan
  untuk tujuan lain ([syarat penuh](CREDITS.md#suara-zikir-mediazikir)). Senarai zikir dan cara merakam
  sendiri: [ZIKIR.md](ZIKIR.md).
- `media/surah/`: Doa Pagi (jam 6:30) dan video Asmaul Husna (jam 7:00, 75 MB). Kedua-duanya **tidak** berlesen MIT,
  dan asal serta lesennya belum disahkan ([lihat CREDITS.md](CREDITS.md#doa-pagi-dan-asmaul-husna-mediasurah)).
- Audio azan dan MP3 surah lain **tidak** disertakan. `tools/pasang.py azan` memuat turun azan dari
  Clear TV Islamic, dan MP3 surah anda sendiri diletakkan dalam `media/surah/`.

## Membina sendiri

JDK 17 dan Android SDK (platform 34, build-tools 34.0.0):
`./gradlew testReleaseUnitTest assembleRelease`. APK yang ditandatangani dengan kunci lain tidak boleh
mengemas kini APK dari `dist/` tanpa uninstall dahulu.

## Kredit dan lesen

Kod: MIT ([LICENSE](LICENSE)). Gambar dan fon: lesen masing-masing dalam [CREDITS.md](CREDITS.md). Suara
zikir: bukan MIT, ikut syarat dalam [CREDITS.md](CREDITS.md#suara-zikir-mediazikir).
